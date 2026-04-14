# Deployment Guide

## Overview

This repository uses three GitHub Actions workflows:

- `Backend CI` (`.github/workflows/backend-ci.yml`)
- `Build Docker Image` (`.github/workflows/build-image.yml`)
- `Deploy HML` (`.github/workflows/deploy-hml.yml`)

The deployment strategy is **pipeline image build + server-side image pull** with Docker Compose on a DigitalOcean Droplet.

## Workflow Details

### Backend CI

- **Triggers**
  - Pull requests targeting `main`
  - Pushes to `main`
- **Steps**
  - Checkout repository
  - Setup Java 17 with Gradle cache
  - Run tests: `./gradlew test`
  - Run coverage gate: `./gradlew koverVerify`
  - Run build: `./gradlew build`
- **Failure behavior**
  - Any failing command stops the workflow and marks it as failed.

### Deploy HML

- **Triggers**
  - Successful `Build Docker Image` runs from `develop`
  - Manual run via `workflow_dispatch`
- **Strategy**
  - Run `Diagnose SSH secrets` step first (validates `SSH_HOST`, `SSH_PORT`, `SSH_USER`, `SSH_PRIVATE_KEY` without printing secrets; detects a key pasted into `SSH_USER`; prints a **public key fingerprint** for `SSH_PRIVATE_KEY` so you can match it on the Droplet)
  - Connect to the DigitalOcean Droplet over SSH
  - Execute `scripts/deploy-hml.sh` on the server
  - Update local clone to `origin/develop`
  - Pull image from GHCR
  - Run `docker compose up -d --no-build --remove-orphans`
  - Validate app health through HTTP endpoint
- **Failure behavior**
  - `set -euo pipefail` and `script_stop: true` ensure immediate failure on errors.

### Build Docker Image

- **Triggers**
  - Pushes to `develop` and `main`
  - Manual run via `workflow_dispatch`
- **Strategy**
  - Build Docker image from `Dockerfile`
  - Push image to GHCR with tags:
    - branch tag (`develop` or `main`)
    - commit tag (`sha-<7 chars>`)

## Required GitHub Secrets

The `Deploy HML` workflow requires:

- `SSH_HOST`: Droplet public host or IP
- `SSH_PORT`: SSH port (usually `22`)
- `SSH_USER`: SSH user with Docker/Compose permissions
- `SSH_PRIVATE_KEY`: Private key for SSH auth
- `HML_DEPLOY_PATH`: Absolute path to the repository on the Droplet (example: `/srv/daily-back`)
- `HML_HEALTHCHECK_URL`: Health endpoint used after deploy (example: `http://127.0.0.1:8081/health`)
- `GHCR_USERNAME`: GitHub username for GHCR pull on server
- `GHCR_TOKEN`: GitHub token/PAT with package read permissions

## Required Server Setup

On the DigitalOcean Droplet:

1. Docker and Docker Compose must be installed.
2. Repository must be cloned at `HML_DEPLOY_PATH`.
3. A valid `.env` file must exist in the project root with runtime values, for example:
   - `APP_HOST`
   - `APP_PORT`
   - `APP_EXTERNAL_PORT`
   - `DB_HOST`
   - `DB_PORT`
   - `DB_NAME`
   - `DB_USER`
   - `DB_PASSWORD`
   - `DB_SCHEMA`
   - `DB_SSL`
   - `FLYWAY_ENABLED`
   - `FLYWAY_LOCATION`
   - `SEED_ENABLED`
   - `SEED_SCENARIO_ENABLED`
   - `RECURRENCE_MAINTENANCE_ENABLED`
   - `RECURRENCE_MAINTENANCE_INTERVAL_HOURS`

## How Deploy Works Internally

The `scripts/deploy-hml.sh` script performs:

1. `git fetch origin`
2. `git checkout develop`
3. `git reset --hard origin/develop`
4. `docker login ghcr.io`
5. `docker compose -f docker-compose.yml -f docker-compose.hml.yml pull app`
6. `docker compose -f docker-compose.yml -f docker-compose.hml.yml up -d --no-build --remove-orphans`
7. container status print (`docker compose ps`)
8. app logs tail for debugging (`docker compose logs --tail 150 app`)
9. HTTP health check retry loop

## Debugging Failures

### `workflow_run` uses the default branch workflow file

GitHub runs workflows triggered by `workflow_run` using the workflow definition from the repository **default branch** (usually `main`), not from the branch that triggered the upstream workflow.

If you update `.github/workflows/deploy-hml.yml` only on `develop`, you may still execute an **older** `deploy-hml.yml` from `main` until you merge those workflow changes into the default branch.

Symptoms:

- Logs do not include newer steps such as `Diagnose SSH secrets` or `Materialize SSH private key for ssh-action`
- SSH errors persist even after fixing secrets

### `ssh.ParsePrivateKey: ssh: no key found`

Common causes:

- `SSH_PRIVATE_KEY` is empty in the environment that runs the job (wrong secret scope, wrong repository fork, or secret not available to Actions)
- Private key was pasted into `SSH_USER` instead of `SSH_PRIVATE_KEY`
- Key is **encrypted with a passphrase** (CI typically needs an unencrypted key, or configure `passphrase` in `appleboy/ssh-action`)
- Multiline key formatting issues when passing `key:` inline (this repo writes a temp file and uses `key_path` to reduce that class of failures)

If deployment fails:

1. Check GitHub Actions logs for the failed step.
2. SSH into the Droplet and inspect:
   - `docker compose ps`
   - `docker compose logs --tail 200 app`
3. Verify `.env` values in `HML_DEPLOY_PATH`.
4. Run manually in `HML_DEPLOY_PATH`:

```bash
bash scripts/deploy-hml.sh
```

5. Confirm health endpoint response:

```bash
curl -i http://127.0.0.1:8081/health
```
