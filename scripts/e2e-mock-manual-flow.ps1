# open-api-service Mock manual ingest E2E (Windows PowerShell)
#
# Prerequisites:
#   - open-api-service with spring.profiles.active=mock-manual (or mock,mock-manual)
#   - Python on PATH (for XML -> instances.json)
#   - import-script-path resolvable (default: svmp/docs/internal/scripts/...)
#
# Usage:
#   powershell -File e2e-mock-manual-flow.ps1
#   powershell -File e2e-mock-manual-flow.ps1 -XmlPath "..\..\svmp\docs\standards\mock-data\report_by_vul.xml"

param(
    [string]$Base = "http://127.0.0.1:35780",
    [string]$AdminKey = "dev-internal-admin-key-change-in-prod",
    [string]$XmlPath = "",
    [int]$ScanTemplateId = 1002,
    [int]$MinInstances = 1
)

$ErrorActionPreference = "Stop"

function Write-Step($msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }
function Assert-Code($resp, $step) {
    if ($null -eq $resp -or $resp.code -ne 0) {
        throw "$step failed: $($resp | ConvertTo-Json -Compress)"
    }
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..\..\..\..")).Path

if (-not $XmlPath) {
    $XmlPath = Join-Path $repoRoot "svmp\docs\standards\mock-data\report_by_vul.xml"
}
if (-not (Test-Path $XmlPath)) {
    throw "XML not found: $XmlPath"
}

$adminHeaders = @{ "X-Internal-Admin-Key" = $AdminKey }
$partnerId = "partner-manual-" + (Get-Date -Format "yyyyMMddHHmmss")
$extTaskId = "EXT-MANUAL-" + (Get-Date -Format "yyyyMMddHHmmss")

Write-Step "1. Create Partner"
$createPartner = Invoke-RestMethod -Method Post -Uri "$Base/internal/admin/partners" `
    -Headers $adminHeaders -ContentType "application/json" `
    -Body (@{
        partnerId    = $partnerId
        partnerName  = "E2E Manual Mock Partner"
        partnerType  = "SIEM"
        capabilities = @("TASK_READ", "TASK_WRITE", "INSTANCE_READ")
        rateLimitQps = 100
    } | ConvertTo-Json)
Assert-Code $createPartner "create partner"

Write-Step "2. Create credential"
$cred = Invoke-RestMethod -Method Post -Uri "$Base/internal/admin/partners/$partnerId/credentials" `
    -Headers $adminHeaders
Assert-Code $cred "create credential"
$clientId = $cred.data.clientId
$clientSecret = $cred.data.clientSecret

Write-Step "3. OAuth token"
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
    "Authorization" = "Bearer $accessToken"
    "X-Partner-Id"  = $partnerId
    "Content-Type"  = "application/json"
}

Write-Step "4. Create task (manual mode should stay RUNNING)"
$createTask = Invoke-RestMethod -Method Post -Uri "$Base/api/open/v1/tasks/vul" `
    -Headers $openHeaders `
    -Body (@{
        extTaskId        = $extTaskId
        taskName         = "e2e-manual-mock"
        type             = 1
        scanTemplateId   = $ScanTemplateId
        reportTemplateId = 2001
        targets          = @{ hosts = "10.0.0.1" }
    } | ConvertTo-Json -Depth 3)
Assert-Code $createTask "create task"
$taskId = $createTask.data.taskId
Write-Host "taskId=$taskId"

Start-Sleep -Seconds 3
$prog = Invoke-RestMethod -Uri "$Base/api/open/v1/tasks/$taskId" -Headers $openHeaders
Write-Host "status after 3s: $($prog.data.status) (expect RUNNING in manual mode)"
if ($prog.data.status -eq "FINISHED") {
    Write-Warning "Task already FINISHED ¡ª service may not be on mock-manual profile"
}

Write-Step "5. GET dispatch-packet"
$packet = Invoke-RestMethod -Uri "$Base/internal/admin/mock-tasks/$taskId/dispatch-packet" `
    -Headers $adminHeaders
Assert-Code $packet "dispatch-packet"
Write-Host "ingestMode=$($packet.data.ingestMode) bundleDir=$($packet.data.taskBundleDir)"

Write-Step "6. POST import-report (XML upload)"
$importUri = "$Base/internal/admin/mock-tasks/$taskId/import-report"
$import = Invoke-RestMethod -Method Post -Uri $importUri `
    -Headers $adminHeaders `
    -Form @{ file = Get-Item -LiteralPath $XmlPath }
Assert-Code $import "import-report"
Write-Host "instances=$($import.data.instanceCount) ingested=$($import.data.instancesIngested)"

if ($import.data.instanceCount -lt $MinInstances) {
    throw "import instanceCount=$($import.data.instanceCount) < min=$MinInstances"
}
if (-not $import.data.instancesIngested) {
    throw "instances not ingested: $($import.data.ingestError)"
}

Write-Step "7. Verify task FINISHED + instances searchable"
$after = Invoke-RestMethod -Uri "$Base/api/open/v1/tasks/$taskId" -Headers $openHeaders
if ($after.data.status -ne "FINISHED") {
    throw "task status not FINISHED: $($after.data.status)"
}

$search = Invoke-RestMethod -Method Post -Uri "$Base/api/open/v1/instances/search" `
    -Headers $openHeaders `
    -Body (@{ taskId = $taskId; page = 1; size = 10 } | ConvertTo-Json)
Assert-Code $search "search instances"
$total = [int64]$search.data.total
Write-Host "instance total=$total"
if ($total -lt $MinInstances) {
    throw "search total=$total < min=$MinInstances"
}

Write-Step "DONE"
Write-Host @"

Manual mock E2E OK
  partnerId  : $partnerId
  taskId     : $taskId
  xml        : $XmlPath
  instances  : $total

"@ -ForegroundColor Green
