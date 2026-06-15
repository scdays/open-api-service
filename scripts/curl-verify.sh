#!/usr/bin/env bash
# open-api-service 任务 API 验证脚本（F0 契约：POST /tasks/vul）
# 用法：
#   1) 经网关：BASE=http://127.0.0.1:35770 TOKEN=<Bearer>
#   2) 直连本服务（测试头）：BASE=http://127.0.0.1:35780
#
# 取 Token（直连 open-api-service，字段为 camelCase）：
#   curl -s -X POST http://127.0.0.1:35780/oauth/token \
#     -H "Content-Type: application/json" \
#     -d '{"grantType":"client_credentials","clientId":"<clientId>","clientSecret":"<secret>"}'

set -euo pipefail

BASE="${BASE:-http://127.0.0.1:35780}"
PARTNER_A="${PARTNER_A:-partner-demo-01}"
PARTNER_B="${PARTNER_B:-partner-demo-02}"
EXT_ID="EXT-TASK-$(date +%s)"
AUTH_HEADER=()
if [ -n "${TOKEN:-}" ]; then
  AUTH_HEADER=(-H "Authorization: Bearer ${TOKEN}")
fi

JSON_BODY="{\"extTaskId\":\"${EXT_ID}\",\"taskName\":\"curl-test\",\"type\":1,\"targets\":{\"hosts\":\"10.0.0.1\"}}"

echo "=== Partner A 创建任务 POST /tasks/vul ==="
CREATE_A=$(curl -s "${AUTH_HEADER[@]}" \
  -H "X-Partner-Id: ${PARTNER_A}" \
  -H "Content-Type: application/json" \
  -d "${JSON_BODY}" \
  "${BASE}/api/open/v1/tasks/vul")
echo "${CREATE_A}"
TASK_ID=$(echo "${CREATE_A}" | sed -n 's/.*"taskId"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)

echo ""
echo "=== Partner A 幂等重复（期望 code=40901）==="
curl -s "${AUTH_HEADER[@]}" \
  -H "X-Partner-Id: ${PARTNER_A}" \
  -H "Content-Type: application/json" \
  -d "${JSON_BODY}" \
  "${BASE}/api/open/v1/tasks/vul"
echo ""

echo ""
echo "=== Partner B 访问 A 的 taskId（期望 code=40003）==="
curl -s "${AUTH_HEADER[@]}" \
  -H "X-Partner-Id: ${PARTNER_B}" \
  "${BASE}/api/open/v1/tasks/${TASK_ID}"
echo ""

echo ""
echo "=== Partner A 查进度 ==="
curl -s "${AUTH_HEADER[@]}" \
  -H "X-Partner-Id: ${PARTNER_A}" \
  "${BASE}/api/open/v1/tasks/${TASK_ID}"
echo ""

echo ""
echo "=== Partner A 列表（Partner B 列表应互不可见）==="
curl -s "${AUTH_HEADER[@]}" \
  -H "X-Partner-Id: ${PARTNER_A}" \
  "${BASE}/api/open/v1/tasks?page=1&size=10"
echo ""
curl -s "${AUTH_HEADER[@]}" \
  -H "X-Partner-Id: ${PARTNER_B}" \
  "${BASE}/api/open/v1/tasks?page=1&size=10"
echo ""
