/**
 * Verify open-api-service sync: v1 API + main-line orchestration.
 * Usage: node scripts/verify-sync.mjs
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.join(__dirname, '..')
let fail = 0

function assertExists (rel) {
  if (!fs.existsSync(path.join(root, rel))) {
    console.error('[FAIL] missing:', rel)
    fail++
  }
}

function assertContains (rel, needle) {
  const text = fs.readFileSync(path.join(root, rel), 'utf8')
  if (!text.includes(needle)) {
    console.error('[FAIL]', rel, 'missing:', needle)
    fail++
  }
}

const mainOnly = [
  'src/main/java/com/vtc/openapi/domain/instance/gateway/IInstanceLifecycleGateway.java',
  'src/main/java/com/vtc/openapi/infra/adapter/InstanceLifecycleGatewayImpl.java',
  'src/main/java/com/vtc/openapi/infra/adapter/InstanceLifecycleGatewayNoOpImpl.java',
  'src/main/java/com/vtc/openapi/infra/adapter/SvmpOpenOrchestrationEngineAdapterImpl.java',
  'src/main/java/com/vtc/openapi/app/service/IVerifyFixNotifyAppService.java',
  'src/main/java/com/vtc/openapi/app/service/impl/VerifyFixNotifyAppServiceImpl.java',
  'src/main/java/com/vtc/openapi/domain/instance/service/business/impl/VerifyFixCompletionDomainServiceImpl.java',
  'src/main/java/com/vtc/openapi/ui/internal/SvmpVerifyFixNotifyUI.java',
  'src/main/java/com/vtc/openapi/infra/feign/IVulPassOpenInstanceFeign.java'
]
mainOnly.forEach(assertExists)

assertContains(
  'src/main/java/com/vtc/openapi/domain/instance/service/business/impl/InstanceDomainServiceImpl.java',
  'lifecycleGateway.isAsyncVerifyFixEnabled'
)
assertContains(
  'src/main/java/com/vtc/openapi/domain/instance/service/business/impl/InstanceDomainServiceImpl.java',
  'VERIFY_FIX_STATUS_PENDING'
)
assertContains(
  'src/main/java/com/vtc/openapi/ui/dto/admin/PartnerWebhookSecretDTO.java',
  'webhookSecret'
)

function walk (dir, out = []) {
  for (const name of fs.readdirSync(dir)) {
    const p = path.join(dir, name)
    const st = fs.statSync(p)
    if (st.isDirectory()) walk(p, out)
    else if (/\.(java|yml|xml)$/.test(name)) out.push(p)
  }
  return out
}

for (const file of walk(path.join(root, 'src'))) {
  const text = fs.readFileSync(file, 'utf8')
  if (text.includes('<<<<<<<')) {
    console.error('[FAIL] conflict marker in', path.relative(root, file))
    fail++
  }
}

if (fail === 0) {
  console.log('[verify-sync] OK')
  process.exit(0)
}
console.error('[verify-sync] FAILED', fail, 'checks')
process.exit(1)
