#!/usr/bin/env bash
# open-api-service 任务 API 验证脚本
# 用法：
#   1) 经网关（需 morningglory + clover Token）：BASE=http://127.0.0.1:7000 TOKEN=<Bearer>
#   2) 直连本服务（跳过网关鉴权，仅带头测试）：BASE=http://127.0.0.1:35780
#
# 取 Token（clover 经 morningglory，OAuth snake_case）：
#   curl -s -X POST http://127.0.0.1:7000/oauth/token \
#     -H "Content-Type: application/json" \
#     -d '{"grant_type":"client_credentials","client_id":"<clientId>","client_secret":"<secret>"}'
#   响应字段：access_token / token_type / expires_in / partner_id

set -euo pipefail

BASE="${BASE:-http://127.0.0.1:35780}"
PARTNER_A="${PARTNER_A:-partner-demo-01}"
PARTNER_B="${PARTNER_B:-partner-demo-02}"
EXT_ID="EXT-TASK-$(date +%s)"
AUTH_HEADER=()
if [ -n "${TOKEN:-}" ]; then
  AUTH_HEADER=(-H "Authorization: Bearer ${TOKEN}")
fi

echo "=== Partner A 创建任务 ==="
CREATE_A=$(curl -s "${AUTH_HEADER[@]}" \
  -H "X-Partner-Id: ${PARTNER_A}" \
  -H "Content-Type: application/json" \
  -d "{\"extTaskId\":\"${EXT_ID}\",\"taskName\":\"curl-test\",\"targets\":[\"10.0.0.1\"],\"targetType\":\"IPV4\",\"vulnType\":1}" \
  "${BASE}/api/open/v1/tasks")
echo "${CREATE_A}"
TASK_ID=$(echo "${CREATE_A}" | sed -n 's/.*"taskId"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)

echo ""
echo "=== Partner A 幂等重复（期望 code=40901）==="
curl -s "${AUTH_HEADER[@]}" \
  -H "X-Partner-Id: ${PARTNER_A}" \
  -H "Content-Type: application/json" \
  -d "{\"extTaskId\":\"${EXT_ID}\",\"taskName\":\"curl-test\",\"targets\":[\"10.0.0.1\"],\"targetType\":\"IPV4\",\"vulnType\":1}" \
  "${BASE}/api/open/v1/tasks"
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
