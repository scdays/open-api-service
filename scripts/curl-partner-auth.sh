#!/usr/bin/env bash
# Partner 身份与 Token 验收脚本（直连 open-api-service，默认 35780）
set -euo pipefail

BASE="${BASE:-http://127.0.0.1:35780}"
ADMIN_KEY="${ADMIN_KEY:-dev-internal-admin-key-change-in-prod}"
PARTNER_ID="${PARTNER_ID:-partner-curl-$(date +%s)}"

echo "=== 1. 创建 Partner ==="
CREATE_RESP=$(curl -s -X POST "${BASE}/internal/admin/partners" \
  -H "Content-Type: application/json" \
  -H "X-Internal-Admin-Key: ${ADMIN_KEY}" \
  -d "{
    \"partnerId\": \"${PARTNER_ID}\",
    \"partnerName\": \"curl 验收 Partner\",
    \"partnerType\": \"SIEM\",
    \"capabilities\": [\"TASK_READ\", \"TASK_WRITE\"],
    \"rateLimitQps\": 100
  }")
echo "${CREATE_RESP}"

echo ""
echo "=== 2. 创建凭证（clientSecret 仅返回一次）==="
CRED_RESP=$(curl -s -X POST "${BASE}/internal/admin/partners/${PARTNER_ID}/credentials" \
  -H "X-Internal-Admin-Key: ${ADMIN_KEY}")
echo "${CRED_RESP}"
CLIENT_ID=$(echo "${CRED_RESP}" | sed -n 's/.*"clientId"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
CLIENT_SECRET=$(echo "${CRED_RESP}" | sed -n 's/.*"clientSecret"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)

echo ""
echo "=== 3. 换取 Token（/oauth/token）==="
TOKEN_RESP=$(curl -s -X POST "${BASE}/oauth/token" \
  -H "Content-Type: application/json" \
  -d "{
    \"grantType\": \"client_credentials\",
    \"clientId\": \"${CLIENT_ID}\",
    \"clientSecret\": \"${CLIENT_SECRET}\"
  }")
echo "${TOKEN_RESP}"
ACCESS_TOKEN=$(echo "${TOKEN_RESP}" | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)

echo ""
echo "=== 4. 更新 Partner capabilities ==="
curl -s -X PUT "${BASE}/internal/admin/partners/${PARTNER_ID}" \
  -H "Content-Type: application/json" \
  -H "X-Internal-Admin-Key: ${ADMIN_KEY}" \
  -d '{"capabilities":["TASK_READ","TASK_WRITE","INSTANCE_READ"]}'
echo ""

echo ""
echo "=== 5. 凭证列表（不含 secret）==="
curl -s "${BASE}/internal/admin/partners/${PARTNER_ID}/credentials" \
  -H "X-Internal-Admin-Key: ${ADMIN_KEY}"
echo ""

echo ""
echo "=== 6. introspect（partner-gateway 降级用）==="
curl -s -X POST "${BASE}/internal/token/introspect" \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"${ACCESS_TOKEN}\"}"
echo ""

echo ""
echo "=== 7. 校验 Redis 键（需本机 redis-cli）==="
if command -v redis-cli >/dev/null 2>&1 && [ -n "${ACCESS_TOKEN}" ]; then
  HASH=$(printf '%s' "${ACCESS_TOKEN}" | sha256sum | awk '{print $1}')
  echo "KEY=partner:token:${HASH}"
  redis-cli GET "partner:token:${HASH}"
  echo ""
else
  echo "跳过 redis-cli（未安装或无 accessToken）"
fi
