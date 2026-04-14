# Postman Files

This folder contains ready-to-import Postman assets for the currently implemented backend endpoints.

## Files

- `BillManager-API.postman_collection.json`: master collection with all implemented modules.
- `BillManager-Local.postman_environment.json`: local environment variables.

## How to import

1. Open Postman.
2. Click **Import**.
3. Import:
   - `postman/BillManager-API.postman_collection.json`
   - `postman/BillManager-Local.postman_environment.json`
4. Select the **BillManager Local** environment before running requests.

## Variables and manual values

Environment variables:

- `baseUrl` (default: `http://localhost:8080`)
- `categoryId`
- `accountId`
- `occurrenceId`
- `month` (default: `2026-04`)
- `date` (default: `2026-04-14`)

Notes:

- `categoryId`, `accountId`, and `occurrenceId` can be populated automatically by collection test scripts after running create/list requests.
- If your app is running via Docker Compose in this repository, you may need to set `baseUrl` to `http://localhost:8081`.

## Recommended execution order

1. **Health** -> `Get Health`
2. **Categories** -> `List Categories`
3. **Categories** -> `Create Category` (stores `categoryId`)
4. **Accounts** -> `Create Account` (stores `accountId`)
5. **Occurrences** -> `List Occurrences` (stores `occurrenceId` from first result)
6. **Occurrences** -> `Mark Occurrence Paid`
7. **Occurrences** -> `Unmark Occurrence Paid`
8. **Occurrences** -> `Override Occurrence Amount`
9. **Dashboard** endpoints:
   - `Get Home Summary`
   - `Get Day Details`
   - `Get Next 12 Months Projection`
   - `Get Category Summary`

## Coverage of implemented API

The collection includes all currently implemented routes:

- Health
- Categories
- Accounts
- Occurrences
- Dashboard
