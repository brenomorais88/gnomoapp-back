#!/usr/bin/env bash

set -euo pipefail

DEPLOY_PATH="${DEPLOY_PATH:-/srv/daily-back}"
DEPLOY_BRANCH="${DEPLOY_BRANCH:-develop}"
APP_SERVICE_NAME="${APP_SERVICE_NAME:-app}"
APP_HEALTHCHECK_URL="${APP_HEALTHCHECK_URL:-http://127.0.0.1:8081/health}"
DOCKER_IMAGE_TAG="${DOCKER_IMAGE_TAG:-develop}"
GHCR_IMAGE_NAME="${GHCR_IMAGE_NAME:-ghcr.io/owner/daily-back}"
GHCR_USERNAME="${GHCR_USERNAME:-}"
GHCR_TOKEN="${GHCR_TOKEN:-}"

echo "==> Deploy path: ${DEPLOY_PATH}"
echo "==> Deploy branch: ${DEPLOY_BRANCH}"
echo "==> Commit SHA: ${GITHUB_SHA:-unknown}"
echo "==> Docker image: ${GHCR_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"

cd "${DEPLOY_PATH}"

if [[ ! -f "docker-compose.yml" ]]; then
  echo "ERROR: docker-compose.yml not found in ${DEPLOY_PATH}"
  exit 1
fi

if [[ ! -f "docker-compose.hml.yml" ]]; then
  echo "ERROR: docker-compose.hml.yml not found in ${DEPLOY_PATH}"
  exit 1
fi

echo "==> Syncing repository"
git fetch origin
git checkout -f "${DEPLOY_BRANCH}" || git checkout -B "${DEPLOY_BRANCH}" "origin/${DEPLOY_BRANCH}"
git reset --hard "origin/${DEPLOY_BRANCH}"
git clean -fd

if [[ -n "${GHCR_USERNAME}" && -n "${GHCR_TOKEN}" ]]; then
  echo "==> Logging in to GHCR"
  echo "${GHCR_TOKEN}" | docker login ghcr.io -u "${GHCR_USERNAME}" --password-stdin
else
  echo "ERROR: GHCR credentials were not provided"
  exit 1
fi

echo "==> Docker status before deploy"
docker compose -f docker-compose.yml -f docker-compose.hml.yml ps || true

echo "==> Pulling updated app image"
IMAGE_TAG="${DOCKER_IMAGE_TAG}" docker compose -f docker-compose.yml -f docker-compose.hml.yml pull "${APP_SERVICE_NAME}"

echo "==> Starting services without build"
IMAGE_TAG="${DOCKER_IMAGE_TAG}" docker compose -f docker-compose.yml -f docker-compose.hml.yml up -d --no-build --remove-orphans

echo "==> Waiting for application startup"
sleep 15

echo "==> Docker status after deploy"
IMAGE_TAG="${DOCKER_IMAGE_TAG}" docker compose -f docker-compose.yml -f docker-compose.hml.yml ps

echo "==> Last logs from service ${APP_SERVICE_NAME}"
IMAGE_TAG="${DOCKER_IMAGE_TAG}" docker compose -f docker-compose.yml -f docker-compose.hml.yml logs --tail 150 "${APP_SERVICE_NAME}" || true

echo "==> HTTP healthcheck: ${APP_HEALTHCHECK_URL}"
for attempt in $(seq 1 12); do
  status_code="$(curl -s -o /dev/null -w "%{http_code}" "${APP_HEALTHCHECK_URL}" || true)"
  if [[ "${status_code}" == "200" ]]; then
    echo "Healthcheck succeeded"
    exit 0
  fi

  echo "Attempt ${attempt}/12 returned status ${status_code}"
  sleep 5
done

echo "ERROR: healthcheck failed"
IMAGE_TAG="${DOCKER_IMAGE_TAG}" docker compose -f docker-compose.yml -f docker-compose.hml.yml ps
IMAGE_TAG="${DOCKER_IMAGE_TAG}" docker compose -f docker-compose.yml -f docker-compose.hml.yml logs --tail 200 "${APP_SERVICE_NAME}" || true
exit 1
