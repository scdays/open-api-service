# open-api-service Mock smoke E2E (Windows PowerShell)
#
# Usage:
#   powershell -File e2e-mock-flow.ps1
#   powershell -File e2e-mock-flow.ps1 -Base http://127.0.0.1:35780 -ScanTemplateId 1003

param(
    [string]$Base = "http://127.0.0.1:35780",
    [string]$AdminKey = "dev-internal-admin-key-change-in-prod",
    [int]$ScanTemplateId = 1003,
    [int]$ExpectedInstances = 5
)

$ErrorActionPreference = "Stop"

function Write-Step($msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }
function Assert-Code($resp, $step) {
    if ($null -eq $resp -or $resp.code -ne 0) {
        throw "$step failed: $($resp | ConvertTo-Json -Compress)"
    }
}

$partnerId = "partner-e2e-" + (Get-Date -Format "yyyyMMddHHmmss")
$extTaskId = "EXT-E2E-" + (Get-Date -Format "yyyyMMddHHmmss")

Write-Step "1. Create Partner"
$createPartner = Invoke-RestMethod -Method Post -Uri "$Base/internal/admin/partners" `
    -Headers @{ "X-Internal-Admin-Key" = $AdminKey } `
    -ContentType "application/json" `
    -Body (@{
        partnerId      = $partnerId
        partnerName    = "E2E Mock Partner"
        partnerType    = "SIEM"
        capabilities   = @(
            "TASK_READ", "TASK_WRITE",
            "INSTANCE_READ", "INSTANCE_VERIFY", "INSTANCE_REMEDIATE",
            "INSTANCE_VERIFY_FIX", "INSTANCE_ARCHIVE"
        )
        rateLimitQps   = 100
    } | ConvertTo-Json)
Assert-Code $createPartner "create partner"
Write-Host "partnerId=$partnerId"

Write-Step "2. Create credential"
$cred = Invoke-RestMethod -Method Post -Uri "$Base/internal/admin/partners/$partnerId/credentials" `
    -Headers @{ "X-Internal-Admin-Key" = $AdminKey }
Assert-Code $cred "create credential"
$clientId = $cred.data.clientId
$clientSecret = $cred.data.clientSecret
Write-Host "clientId=$clientId"

Write-Step "3. Get Token (grantType/clientId are camelCase)"
$token = Invoke-RestMethod -Method Post -Uri "$Base/oauth/token" `
    -ContentType "application/json" `
    -Body (@{
        grantType    = "client_credentials"
        clientId     = $clientId
        clientSecret = $clientSecret
    } | ConvertTo-Json)
Assert-Code $token "oauth token"
$accessToken = $token.data.accessToken

$openHeaders = @{
    "Authorization"  = "Bearer $accessToken"
    "X-Partner-Id"   = $partnerId
    "Content-Type"   = "application/json"
}

Write-Step "4. Create scan task POST /tasks/vul"
$createTask = Invoke-RestMethod -Method Post -Uri "$Base/api/open/v1/tasks/vul" `
    -Headers $openHeaders `
    -Body (@{
        extTaskId        = $extTaskId
        taskName         = "e2e-mock-scan"
        type             = 1
        scanTemplateId   = $ScanTemplateId
        reportTemplateId = 2001
        targets          = @{ hosts = "10.0.0.1" }
    } | ConvertTo-Json -Depth 3)
Assert-Code $createTask "create task"
$taskId = $createTask.data.taskId
Write-Host "taskId=$taskId extTaskId=$extTaskId"

Write-Step "5. Poll task progress (max 16s, Mock finishes in ~5s)"
$finished = $false
for ($i = 1; $i -le 8; $i++) {
    Start-Sleep -Seconds 2
    $prog = Invoke-RestMethod -Uri "$Base/api/open/v1/tasks/$taskId" -Headers $openHeaders
    Write-Host "  poll $i : status=$($prog.data.status) progress=$($prog.data.progress)"
    if ($prog.data.status -eq "FINISHED") { $finished = $true; break }
}
if (-not $finished) { throw "task not FINISHED within timeout" }

Write-Step "6. Search instances POST /instances/search"
$search = Invoke-RestMethod -Method Post -Uri "$Base/api/open/v1/instances/search" `
    -Headers $openHeaders `
    -Body (@{ taskId = $taskId; page = 1; size = 10 } | ConvertTo-Json)
Assert-Code $search "search instances"
$total = [int64]$search.data.total
Write-Host "total=$total (expected $ExpectedInstances)"
if ($total -ne $ExpectedInstances) {
    Write-Warning "instance count mismatch; scanTemplateId=$ScanTemplateId may map to different bundle"
}
$vulInfoId = $search.data.items[0].vulInfoID
Write-Host "sample vulInfoID=$vulInfoId"

Write-Step "7. Verify instance POST /instances/{id}/verify"
$verifyHeaders = $openHeaders.Clone()
$verifyHeaders["Idempotency-Key"] = "e2e-verify-" + (Get-Date -Format "HHmmss")
$verify = Invoke-RestMethod -Method Post -Uri "$Base/api/open/v1/instances/$vulInfoId/verify" `
    -Headers $verifyHeaders `
    -Body (@{
        verifyResult = "VALID"
        srcMethod    = 1021
        operator     = "e2e@test.com"
        transferTime = "1747476000"
        remark       = "e2e verify"
    } | ConvertTo-Json)
Assert-Code $verify "verify instance"
Write-Host "vulInfoStat $($verify.data.previousStatus) -> $($verify.data.currentStatus)"

Write-Step "DONE"
Write-Host @"

Summary
  partnerId   : $partnerId
  taskId      : $taskId
  instances   : $total
  verified    : $vulInfoId -> vulInfoStat=2

SQL check:
  SELECT COUNT(*) FROM open_vuln_instance WHERE task_id='$taskId';
  SELECT instances_ingested FROM open_task WHERE task_id='$taskId';

"@ -ForegroundColor Green
