#!/usr/bin/env bash
# DailyBack backend - all implemented route curls
# Requires: BASE_URL and TOKEN (for authenticated endpoints)
# Example:
#   BASE_URL=http://localhost:8080
#   TOKEN="<jwt>"

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-REPLACE_WITH_JWT}"

ACCOUNT_ID="${ACCOUNT_ID:-REPLACE_ACCOUNT_ID}"
CATEGORY_ID="${CATEGORY_ID:-REPLACE_CATEGORY_ID}"
OCCURRENCE_ID="${OCCURRENCE_ID:-REPLACE_OCCURRENCE_ID}"
FAMILY_MEMBER_ID="${FAMILY_MEMBER_ID:-REPLACE_MEMBER_ID}"

echo "=== Auth ==="
cat <<'EOF'
curl -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName":"Breno",
    "lastName":"Morais",
    "document":"12345678901",
    "birthDate":"1990-01-10",
    "password":"StrongPass#123",
    "email":"breno@example.com",
    "phone":"+5511999999999"
  }'

curl -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "login":"breno@example.com",
    "password":"StrongPass#123"
  }'
EOF

echo "=== Users ==="
cat <<'EOF'
curl -X GET "$BASE_URL/auth/me" \
  -H "Authorization: Bearer $TOKEN"
EOF

echo "=== Families ==="
cat <<'EOF'
curl -X POST "$BASE_URL/families" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Família Morais"}'

curl -X GET "$BASE_URL/families/me" \
  -H "Authorization: Bearer $TOKEN"

curl -X GET "$BASE_URL/families/me/members" \
  -H "Authorization: Bearer $TOKEN"
EOF

echo "=== Family Members ==="
cat <<'EOF'
curl -X POST "$BASE_URL/families/current/members" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "displayName":"João",
    "document":"98765432100",
    "email":"joao@example.com",
    "phone":"+5511988888888"
  }'

curl -X PATCH "$BASE_URL/families/current/members/$FAMILY_MEMBER_ID/role" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"role":"ADMIN"}'

curl -X DELETE "$BASE_URL/families/current/members/$FAMILY_MEMBER_ID" \
  -H "Authorization: Bearer $TOKEN"
EOF

echo "=== Permissions ==="
cat <<'EOF'
curl -X GET "$BASE_URL/families/current/members/$FAMILY_MEMBER_ID/permissions" \
  -H "Authorization: Bearer $TOKEN"

curl -X PUT "$BASE_URL/families/current/members/$FAMILY_MEMBER_ID/permissions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "canViewFamilyAccounts": true,
    "canCreateFamilyAccounts": false,
    "canEditFamilyAccounts": false,
    "canDeleteFamilyAccounts": false,
    "canMarkFamilyAccountsPaid": false,
    "canManageCategories": false,
    "canInviteMembers": false,
    "canManageMembers": false,
    "canViewOtherPersonalAccounts": false,
    "canEditOtherPersonalAccounts": false
  }'
EOF

echo "=== Categories ==="
cat <<'EOF'
curl -X GET "$BASE_URL/categories" \
  -H "Authorization: Bearer $TOKEN"

curl -X GET "$BASE_URL/categories/$CATEGORY_ID" \
  -H "Authorization: Bearer $TOKEN"

curl -X POST "$BASE_URL/categories" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Alimentação","color":"#22AA66"}'

curl -X PUT "$BASE_URL/categories/$CATEGORY_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Alimentação Casa","color":"#2288FF"}'

curl -X DELETE "$BASE_URL/categories/$CATEGORY_ID" \
  -H "Authorization: Bearer $TOKEN"
EOF

echo "=== Accounts ==="
cat <<'EOF'
curl -X GET "$BASE_URL/accounts?scope=VISIBLE_TO_ME" \
  -H "Authorization: Bearer $TOKEN"

curl -X GET "$BASE_URL/accounts/$ACCOUNT_ID" \
  -H "Authorization: Bearer $TOKEN"

curl -X POST "$BASE_URL/accounts" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "ownershipType":"PERSONAL",
    "title":"Internet",
    "baseAmount":"120.50",
    "startDate":"2026-05-01",
    "endDate":null,
    "recurrenceType":"MONTHLY",
    "categoryId":"'"$CATEGORY_ID"'",
    "notes":"Plano fibra",
    "active":true
  }'

curl -X POST "$BASE_URL/accounts" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "ownershipType":"FAMILY",
    "responsibleMemberId":"'"$FAMILY_MEMBER_ID"'",
    "title":"Aluguel",
    "baseAmount":"1500.00",
    "startDate":"2026-05-01",
    "endDate":null,
    "recurrenceType":"MONTHLY",
    "categoryId":"'"$CATEGORY_ID"'",
    "notes":"Casa",
    "active":true
  }'

curl -X PUT "$BASE_URL/accounts/$ACCOUNT_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Internet Casa",
    "baseAmount":"130.90",
    "startDate":"2026-05-01",
    "endDate":null,
    "recurrenceType":"MONTHLY",
    "categoryId":"'"$CATEGORY_ID"'",
    "notes":"Atualizado",
    "active":true
  }'

curl -X PATCH "$BASE_URL/accounts/$ACCOUNT_ID/activate" \
  -H "Authorization: Bearer $TOKEN"

curl -X PATCH "$BASE_URL/accounts/$ACCOUNT_ID/deactivate" \
  -H "Authorization: Bearer $TOKEN"

curl -X DELETE "$BASE_URL/accounts/$ACCOUNT_ID" \
  -H "Authorization: Bearer $TOKEN"
EOF

echo "=== Occurrences ==="
cat <<'EOF'
curl -X GET "$BASE_URL/occurrences?scope=VISIBLE_TO_ME&month=2026-05" \
  -H "Authorization: Bearer $TOKEN"

curl -X GET "$BASE_URL/occurrences/$OCCURRENCE_ID" \
  -H "Authorization: Bearer $TOKEN"

curl -X PATCH "$BASE_URL/occurrences/$OCCURRENCE_ID/mark-paid" \
  -H "Authorization: Bearer $TOKEN"

curl -X PATCH "$BASE_URL/occurrences/$OCCURRENCE_ID/unmark-paid" \
  -H "Authorization: Bearer $TOKEN"

curl -X PATCH "$BASE_URL/occurrences/$OCCURRENCE_ID/override-amount" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":"999.99"}'
EOF

echo "=== Dashboard ==="
cat <<'EOF'
curl -X GET "$BASE_URL/dashboard/home?month=2026-05" \
  -H "Authorization: Bearer $TOKEN"

curl -X GET "$BASE_URL/dashboard/day?date=2026-05-10" \
  -H "Authorization: Bearer $TOKEN"

curl -X GET "$BASE_URL/dashboard/next-12-months?includeDetails=true" \
  -H "Authorization: Bearer $TOKEN"

curl -X GET "$BASE_URL/dashboard/category-summary?month=2026-05" \
  -H "Authorization: Bearer $TOKEN"
EOF

echo "=== System ==="
cat <<'EOF'
curl -X GET "$BASE_URL/health"
curl -X GET "$BASE_URL/"
curl -X GET "$BASE_URL/swagger"
EOF
