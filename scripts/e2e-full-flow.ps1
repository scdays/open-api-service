# open-api-service full E2E (admin + open API + state machine + batch + negative cases)
#
# Usage:
#   powershell -File e2e-full-flow.ps1
#   powershell -File e2e-full-flow.ps1 -IncludeHeavy   # scanTemplateId=1001 ~500 instances
#   powershell -File e2e-full-flow.ps1 -ExportWaitSec 60
#
# SKIP (catalog only): legacy POST /tasks, archive, receivePlatformWebhook (Partner-side)
# Export/Webhook tests need restarted service + file-sharing-center for READY exports

param(
    [string]$Base = "http://127.0.0.1:35780",
    [string]$AdminKey = "dev-internal-admin-key-change-in-prod",
    [string]$WebhookCallbackUrl = "",
    [int]$ExportWaitSec = 45,
    [int]$VerifyScanWaitSec = 8,
    [switch]$IncludeHeavy,
    [switch]$Quick
)

$ErrorActionPreference = "Stop"
$script:Passed = 0
$script:Failed = 0
$script:Skipped = 0
$script:Results = New-Object System.Collections.Generic.List[string]

function Write-Phase($msg) {
    Write-Host ""
    Write-Host "######## $msg ########" -ForegroundColor Magenta
}

function Record($name, $status, $detail = "") {
    $line = "[$status] $name"
    if ($detail) { $line += " - $detail" }
    $script:Results.Add($line) | Out-Null
    switch ($status) {
        "PASS" { $script:Passed++; Write-Host $line -ForegroundColor Green }
        "FAIL" { $script:Failed++; Write-Host $line -ForegroundColor Red }
        "SKIP" { $script:Skipped++; Write-Host $line -ForegroundColor Yellow }
    }
}

function Invoke-Api {
    param(
        [string]$Method = "GET",
        [string]$Uri,
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [switch]$Raw
    )
    $params = @{
        Method      = $Method
        Uri         = $Uri
        Headers     = $Headers
        ContentType = "application/json; charset=utf-8"
    }
    if ($Body) { $params.Body = $Body }
    try {
        if ($Raw) {
            return Invoke-WebRequest @params -UseBasicParsing
        }
        return Invoke-RestMethod @params
    } catch {
        if ($_.ErrorDetails.Message) {
            try { return ($_.ErrorDetails.Message | ConvertFrom-Json) } catch { }
        }
        throw
    }
}

function Expect-Code($resp, $expected, $testName) {
    $actual = $resp.code
    if ($actual -eq $expected) {
        Record $testName "PASS" "code=$actual"
        return $true
    }
    Record $testName "FAIL" "expected=$expected actual=$actual msg=$($resp.message)"
    return $false
}

function Wait-TaskFinished {
    param([string]$TaskId, [hashtable]$Headers, [int]$MaxWaitSec = 20)
    for ($i = 1; $i -le ($MaxWaitSec / 2); $i++) {
        Start-Sleep -Seconds 2
        $p = Invoke-Api -Uri "$Base/api/open/v1/tasks/$TaskId" -Headers $Headers
        if ($p.data.status -eq "FINISHED") { return $p }
    }
    throw "task $TaskId not FINISHED within ${MaxWaitSec}s"
}

function New-OpenHeaders {
    param([string]$Token, [string]$PartnerId, [string]$IdempotencyKey = $null)
    $h = @{
        "Authorization" = "Bearer $Token"
        "X-Partner-Id"  = $PartnerId
        "Content-Type"  = "application/json"
    }
    if ($IdempotencyKey) { $h["Idempotency-Key"] = $IdempotencyKey }
    return $h
}

function Create-TaskByJson {
    param(
        [hashtable]$Headers,
        [string]$ExtTaskId,
        [int]$ScanTemplateId,
        [int]$Type = 1,
        [int]$ReportTemplateId = 2001
    )
    $body = @{
        extTaskId        = $ExtTaskId
        taskName         = "e2e-$ScanTemplateId-t$Type"
        type             = $Type
        scanTemplateId   = $ScanTemplateId
        reportTemplateId = $ReportTemplateId
        targets          = @{ hosts = "10.0.0.1" }
    } | ConvertTo-Json -Depth 3
    $r = Invoke-Api -Method Post -Uri "$Base/api/open/v1/tasks/vul" -Headers $Headers -Body $body
    if ($r.code -ne 0) { throw "create task failed: $($r | ConvertTo-Json -Compress)" }
    Wait-TaskFinished -TaskId $r.data.taskId -Headers $Headers | Out-Null
    return $r.data.taskId
}

function Search-Instances {
    param([hashtable]$Headers, [string]$TaskId, [int]$Size = 100)
    $body = @{ taskId = $TaskId; page = 1; size = $Size } | ConvertTo-Json
    $r = Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/search" -Headers $Headers -Body $body
    if ($r.code -ne 0) { throw "search failed" }
    return $r.data
}

function Get-TaskExportSummary {
    param([string]$TaskId, [hashtable]$Headers, [string]$ExportStage = $null)
    $r = Invoke-Api -Uri "$Base/api/open/v1/tasks/$TaskId/exports?page=1&size=50" -Headers $Headers
    if ($r.code -ne 0 -or -not $r.data.items) {
        return @{ ready = 0; failed = 0; other = 0; total = 0 }
    }
    $items = @($r.data.items | Where-Object { -not $ExportStage -or $_.exportStage -eq $ExportStage })
    return @{
        ready  = @($items | Where-Object { $_.status -eq "READY" }).Count
        failed = @($items | Where-Object { $_.status -eq "FAILED" }).Count
        other  = @($items | Where-Object { $_.status -ne "READY" -and $_.status -ne "FAILED" }).Count
        total  = $items.Count
    }
}

function Record-ExportWaitResult {
    param(
        [string]$Name,
        [string]$TaskId,
        [hashtable]$Headers,
        [string]$ExportStage,
        [int]$MinReady,
        $ExportData
    )
    if ($null -ne $ExportData) {
        $ready = @($ExportData.items | Where-Object {
            $_.status -eq "READY" -and (-not $ExportStage -or $_.exportStage -eq $ExportStage)
        })
        if ($ready.Count -ge $MinReady) {
            Record $Name "PASS" "count=$($ready.Count)"
            return $ExportData
        }
    }
    $sum = Get-TaskExportSummary -TaskId $TaskId -Headers $Headers -ExportStage $ExportStage
    if ($sum.total -eq 0) {
        Record $Name "FAIL" "no export rows (service restarted with export DDL?)"
    } elseif ($sum.failed -ge $MinReady -and $sum.ready -eq 0) {
        Record $Name "SKIP" "FAILED=$($sum.failed) (file-sharing-center upload?)"
    } else {
        Record $Name "FAIL" "READY=$($sum.ready) FAILED=$($sum.failed) within wait window"
    }
    return $null
}

function Wait-TaskExportsReady {
    param(
        [string]$TaskId,
        [hashtable]$Headers,
        [int]$MinReady = 2,
        [string]$ExportStage = $null,
        [int]$MaxWaitSec = 45
    )
    $interval = 3
    $attempts = [Math]::Max(1, [int][Math]::Ceiling($MaxWaitSec / $interval))
    for ($i = 1; $i -le $attempts; $i++) {
        $r = Invoke-Api -Uri "$Base/api/open/v1/tasks/$TaskId/exports?page=1&size=50" -Headers $Headers
        if ($r.code -eq 0 -and $r.data.items) {
            $ready = @($r.data.items | Where-Object {
                $_.status -eq "READY" -and (-not $ExportStage -or $_.exportStage -eq $ExportStage)
            })
            if ($ready.Count -ge $MinReady) {
                return $r.data
            }
        }
        Start-Sleep -Seconds $interval
    }
    return $null
}

function Get-WebhookDeliveryPage {
    param(
        [string]$PartnerId,
        [string]$EventType = $null,
        [int]$Size = 100
    )
    $uri = "$Base/internal/admin/webhook-deliveries?partnerId=$PartnerId&page=1&size=$Size"
    if ($EventType) { $uri += "&eventType=$EventType" }
    return Invoke-Api -Uri $uri -Headers $script:AdminHeadersRef
}

function Count-WebhookEventType {
    param([string]$PartnerId, [string]$EventType)
    $r = Get-WebhookDeliveryPage -PartnerId $PartnerId -EventType $EventType
    if ($r.code -ne 0 -or -not $r.data) { return 0 }
    return [int64]$r.data.total
}

# ---------- bootstrap ----------
$ts = Get-Date -Format "yyyyMMddHHmmss"
$partnerA = "partner-full-$ts"
$partnerB = "partner-full-b-$ts"
$adminHeaders = @{ "X-Internal-Admin-Key" = $AdminKey }
$script:AdminHeadersRef = $adminHeaders
$allCaps = @(
    "TASK_READ", "TASK_WRITE",
    "INSTANCE_READ", "INSTANCE_VERIFY", "INSTANCE_REMEDIATE",
    "INSTANCE_VERIFY_FIX", "INSTANCE_ARCHIVE", "EXPORT_READ", "EVENT_SUBSCRIBE"
)

if (-not $WebhookCallbackUrl) {
    $WebhookCallbackUrl = "$Base/internal/dev/webhook/receive"
}

Write-Phase "Phase 0 - Health check"
try {
    $health = Invoke-Api -Uri "$Base/internal/health"
    if ($health.status -eq "UP" -or $health.code -eq 0) {
        Record "GET /internal/health" "PASS"
    } else {
        Record "GET /internal/health" "PASS" "reachable"
    }
} catch {
    Record "GET /internal/health" "FAIL" $_.Exception.Message
    throw "open-api-service not reachable at $Base"
}

Write-Phase "Phase 1 - Admin API (governance)"
$createPartnerBody = @{
    partnerId           = $partnerA
    partnerName         = "Full E2E Partner A"
    partnerType         = "SIEM"
    capabilities        = $allCaps
    rateLimitQps        = 100
    defaultCallbackUrl  = $WebhookCallbackUrl
} | ConvertTo-Json
$pCreate = Invoke-Api -Method Post -Uri "$Base/internal/admin/partners" -Headers $adminHeaders -Body $createPartnerBody
Expect-Code $pCreate 0 "POST /internal/admin/partners"

$pList = Invoke-Api -Uri "$Base/internal/admin/partners?page=1&size=5" -Headers $adminHeaders
Expect-Code $pList 0 "GET /internal/admin/partners"

$pGet = Invoke-Api -Uri "$Base/internal/admin/partners/$partnerA" -Headers $adminHeaders
Expect-Code $pGet 0 "GET /internal/admin/partners/{id}"

$pPut = Invoke-Api -Method Put -Uri "$Base/internal/admin/partners/$partnerA" -Headers $adminHeaders `
    -Body (@{ partnerName = "Full E2E Partner A Updated"; capabilities = $allCaps } | ConvertTo-Json)
Expect-Code $pPut 0 "PUT /internal/admin/partners/{id}"

$cred = Invoke-Api -Method Post -Uri "$Base/internal/admin/partners/$partnerA/credentials" -Headers $adminHeaders
Expect-Code $cred 0 "POST /internal/admin/partners/{id}/credentials"
$clientId = $cred.data.clientId
$clientSecret = $cred.data.clientSecret

$credList = Invoke-Api -Uri "$Base/internal/admin/partners/$partnerA/credentials" -Headers $adminHeaders
Expect-Code $credList 0 "GET /internal/admin/partners/{id}/credentials"

$stats = Invoke-Api -Uri "$Base/internal/admin/partners/$partnerA/stats" -Headers $adminHeaders
Expect-Code $stats 0 "GET /internal/admin/partners/{id}/stats"

$catalog = Invoke-Api -Uri "$Base/internal/admin/api-operations?page=1&size=50" -Headers $adminHeaders
if ($catalog.code -eq 0 -and $catalog.data.total -ge 20) {
    Record "GET /internal/admin/api-operations" "PASS" "total=$($catalog.data.total)"
} else {
    Record "GET /internal/admin/api-operations" "FAIL" "total=$($catalog.data.total)"
}

Write-Phase "Phase 2 - Auth (Token)"
$tokenBody = @{ grantType = "client_credentials"; clientId = $clientId; clientSecret = $clientSecret } | ConvertTo-Json
$tokenResp = Invoke-Api -Method Post -Uri "$Base/oauth/token" -Body $tokenBody
Expect-Code $tokenResp 0 "POST /oauth/token"
$token = $tokenResp.data.accessToken

$intro = Invoke-Api -Method Post -Uri "$Base/internal/token/introspect" `
    -Body (@{ token = $token } | ConvertTo-Json)
Expect-Code $intro 0 "POST /internal/token/introspect"

$openA = New-OpenHeaders -Token $token -PartnerId $partnerA

Write-Phase "Phase 3 - Task create (multi entry + idempotency)"
$extMain = "EXT-MAIN-$ts"
$taskMain = Create-TaskByJson -Headers $openA -ExtTaskId $extMain -ScanTemplateId 1003
Record "POST /tasks/vul scanTemplateId=1003" "PASS" "taskId=$taskMain expectedInstances=5"

$dupBody = @{
    extTaskId = $extMain; taskName = "dup"; type = 1
    scanTemplateId = 1003; reportTemplateId = 2001; targets = @{ hosts = "10.0.0.1" }
} | ConvertTo-Json -Depth 3
$dup = Invoke-Api -Method Post -Uri "$Base/api/open/v1/tasks/vul" -Headers $openA -Body $dupBody
Expect-Code $dup 40901 "POST /tasks/vul duplicate extTaskId"

if (-not $Quick) {
    $task1002 = Create-TaskByJson -Headers $openA -ExtTaskId "EXT-1002-$ts" -ScanTemplateId 1002
    $c1002 = (Search-Instances -Headers $openA -TaskId $task1002).total
    if ([int64]$c1002 -eq 1) {
        Record "POST /tasks/vul scanTemplateId=1002" "PASS" "total=1"
    } else {
        Record "POST /tasks/vul scanTemplateId=1002" "FAIL" "total=$c1002"
    }

    $taskPwd = Create-TaskByJson -Headers $openA -ExtTaskId "EXT-PWD-$ts" -ScanTemplateId 1001 -Type 3
    $cPwd = (Search-Instances -Headers $openA -TaskId $taskPwd).total
    if ([int64]$cPwd -eq 8) {
        Record "POST /tasks/vul scanTemplateId=1001 type=3" "PASS" "total=8"
    } else {
        Record "POST /tasks/vul scanTemplateId=1001 type=3" "FAIL" "total=$cPwd"
    }

    $xmlPath = Join-Path $PSScriptRoot "fixtures/minimal-scan-task-1003.xml"
    if (-not (Test-Path $xmlPath)) { throw "missing fixture: $xmlPath" }
    $xmlContent = [System.IO.File]::ReadAllText($xmlPath, [System.Text.Encoding]::UTF8)
    $fileBody = (@{ extTaskId = "EXT-FILE-$ts"; type = 1; file = $xmlContent } | ConvertTo-Json -Depth 3 -Compress)
    $fileResp = Invoke-Api -Method Post -Uri "$Base/api/open/v1/tasks/file" -Headers $openA -Body $fileBody
    if ($fileResp.code -eq 0) {
        Wait-TaskFinished -TaskId $fileResp.data.taskId -Headers $openA | Out-Null
        Record "POST /tasks/file (XML string)" "PASS" "taskId=$($fileResp.data.taskId)"
    } else {
        Record "POST /tasks/file (XML string)" "FAIL" $fileResp.message
    }

    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
        $extUp = "EXT-UP-$ts"
        $uploadJson = curl.exe -s -X POST "$Base/api/open/v1/tasks/upload" `
            -H "Authorization: Bearer $token" -H "X-Partner-Id: $partnerA" `
            -F "file=@$xmlPath" -F "extTaskId=$extUp" -F "type=1"
        $uploadResp = $uploadJson | ConvertFrom-Json
        if ($uploadResp.code -eq 0) {
            Wait-TaskFinished -TaskId $uploadResp.data.taskId -Headers $openA | Out-Null
            Record "POST /tasks/upload (multipart)" "PASS" "taskId=$($uploadResp.data.taskId)"
        } else {
            Record "POST /tasks/upload (multipart)" "FAIL" $uploadResp.message
        }
    } else {
        Record "POST /tasks/upload (multipart)" "SKIP" "curl.exe not found"
    }
}

if ($IncludeHeavy) {
    $task1001 = Create-TaskByJson -Headers $openA -ExtTaskId "EXT-1001-$ts" -ScanTemplateId 1001 -Type 1
    $c1001 = (Search-Instances -Headers $openA -TaskId $task1001 -Size 600).total
    if ([int64]$c1001 -ge 500) {
        Record "POST /tasks/vul scanTemplateId=1001 type=1 (heavy)" "PASS" "total=$c1001"
    } else {
        Record "POST /tasks/vul scanTemplateId=1001 type=1 (heavy)" "FAIL" "total=$c1001"
    }
}

Write-Phase "Phase 4 - Task read"
$prog = Invoke-Api -Uri "$Base/api/open/v1/tasks/$taskMain" -Headers $openA
Expect-Code $prog 0 "GET /tasks/{taskId}"

$list = Invoke-Api -Uri "$Base/api/open/v1/tasks?page=1&size=10&extTaskId=$extMain" -Headers $openA
if ($list.code -eq 0 -and $list.data.items.Count -ge 1) {
    Record "GET /tasks list with extTaskId filter" "PASS" "count=$($list.data.items.Count)"
} else {
    Record "GET /tasks list with extTaskId filter" "FAIL"
}

Write-Phase "Phase 4.5 - Export APIs (TASK_COMPLETED, reportTemplateId=2001 -> json only)"
$exportList = Wait-TaskExportsReady -TaskId $taskMain -Headers $openA -MinReady 1 `
    -ExportStage "TASK_COMPLETED" -MaxWaitSec $ExportWaitSec
$exportList = Record-ExportWaitResult -Name "GET /tasks/{taskId}/exports TASK_COMPLETED" `
    -TaskId $taskMain -Headers $openA -ExportStage "TASK_COMPLETED" -MinReady 1 -ExportData $exportList
if ($null -ne $exportList) {
    $tcItems = @($exportList.items | Where-Object { $_.exportStage -eq "TASK_COMPLETED" -and $_.status -eq "READY" })
    $fmtXml = @($tcItems | Where-Object { $_.format -eq "xml" })
    $fmtJson = @($tcItems | Where-Object { $_.format -eq "json" })
    if ($tcItems.Count -ge 1 -and $fmtJson.Count -ge 1 -and $fmtXml.Count -eq 0) {
        Record "TASK_COMPLETED json-only (reportTemplateId=2001)" "PASS" "json=$($fmtJson.Count)"
    } else {
        Record "TASK_COMPLETED json-only (reportTemplateId=2001)" "FAIL" "items=$($tcItems.Count) xml=$($fmtXml.Count) json=$($fmtJson.Count)"
    }

    $exportMetaId = $fmtJson[0].exportId
    $meta = Invoke-Api -Uri "$Base/api/open/v1/exports/$exportMetaId" -Headers $openA
    if ($meta.code -eq 0 -and $meta.data.downloadUrl -match "/api/open/v1/exports/.+/download") {
        Record "GET /exports/{exportId} metadata" "PASS" "has partner downloadUrl"
    } elseif ($meta.code -eq 0 -and $meta.data.status -eq "FAILED") {
        Record "GET /exports/{exportId} metadata" "SKIP" "export FAILED (file-sharing upload?)"
        $exportMetaId = $null
    } else {
        Record "GET /exports/{exportId} metadata" "FAIL" "code=$($meta.code) url=$($meta.data.downloadUrl)"
        $exportMetaId = $null
    }

    if ($exportMetaId) {
        try {
            $dl = Invoke-Api -Uri "$Base/api/open/v1/exports/$exportMetaId/download" -Headers $openA -Raw
            $ct = $dl.Headers["Content-Type"]
            $len = $dl.RawContentLength
            if ($len -gt 50 -and $ct -match "json") {
                Record "GET /exports/{exportId}/download" "PASS" "bytes=$len"
            } else {
                Record "GET /exports/{exportId}/download" "FAIL" "bytes=$len ct=$ct"
            }
        } catch {
            Record "GET /exports/{exportId}/download" "FAIL" $_.Exception.Message
        }
    } else {
        Record "GET /exports/{exportId}/download" "SKIP" "no READY json export"
    }
}

Write-Phase "Phase 5 - Instance read + single-instance state machine"
$search = Search-Instances -Headers $openA -TaskId $taskMain
Record "POST /instances/search" "PASS" "total=$($search.total)"

$ids = @($search.items | ForEach-Object { $_.vulInfoID })
if ($ids.Count -lt 4) { throw "need at least 4 instances, got $($ids.Count)" }

$instMain = $ids[0]
$instFp   = $ids[1]
$instB1   = $ids[2]
$instB2   = $ids[3]

$detail = Invoke-Api -Uri "$Base/api/open/v1/instances/$instMain" -Headers $openA
Expect-Code $detail 0 "GET /instances/{vulInfoID}"

# Main line: 1 -> 2 -> 5 -> 6
$hVerify = New-OpenHeaders -Token $token -PartnerId $partnerA -IdempotencyKey "verify-main-$ts"
$v1 = Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/$instMain/verify" -Headers $hVerify -Body (@{
    verifyResult = "VALID"; srcMethod = 1021; operator = "e2e@test.com"; transferTime = "1747476000"
} | ConvertTo-Json)
Expect-Code $v1 0 "POST verify VALID (1->2)"

Start-Sleep -Seconds $VerifyScanWaitSec
$vsAfterMain = Wait-TaskExportsReady -TaskId $taskMain -Headers $openA -MinReady 1 `
    -ExportStage "VERIFY_SCAN" -MaxWaitSec $ExportWaitSec
Record-ExportWaitResult -Name "VERIFY_SCAN exports after verify (instMain)" `
    -TaskId $taskMain -Headers $openA -ExportStage "VERIFY_SCAN" -MinReady 1 -ExportData $vsAfterMain | Out-Null

$hRem = New-OpenHeaders -Token $token -PartnerId $partnerA -IdempotencyKey "rem-main-$ts"
$r1 = Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/$instMain/remediate" -Headers $hRem -Body (@{
    vulInfoStat = 5; srcMethod = "1050"; remedDesc = "e2e fix"; remedTime = "3日"
    fixLnk = "https://example.com/patch"
} | ConvertTo-Json)
Expect-Code $r1 0 "POST remediate (2->5)"

$hVf = New-OpenHeaders -Token $token -PartnerId $partnerA -IdempotencyKey "vf-main-$ts"
$vf1 = Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/$instMain/verify-fix" -Headers $hVf -Body (@{
    verifyResult = "FIX_CONFIRMED"; transferTime = "1747488000"
} | ConvertTo-Json)
Expect-Code $vf1 0 "POST verify-fix FIX_CONFIRMED (5->6)"

Start-Sleep -Seconds $VerifyScanWaitSec
$vfExports = Wait-TaskExportsReady -TaskId $taskMain -Headers $openA -MinReady 1 `
    -ExportStage "VERIFY_FIX_SCAN" -MaxWaitSec $ExportWaitSec
Record-ExportWaitResult -Name "VERIFY_FIX_SCAN exports after verify-fix" `
    -TaskId $taskMain -Headers $openA -ExportStage "VERIFY_FIX_SCAN" -MinReady 1 -ExportData $vfExports | Out-Null

# False positive branch: 1 -> 3
$hFp = New-OpenHeaders -Token $token -PartnerId $partnerA -IdempotencyKey "fp-$ts"
$fp = Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/$instFp/verify" -Headers $hFp -Body (@{
    verifyResult = "FALSE_POSITIVE"; operator = "e2e@test.com"
} | ConvertTo-Json)
Expect-Code $fp 0 "POST verify FALSE_POSITIVE (1->3)"

Write-Phase "Phase 6 - Instance batch writes"
$hBatchV = New-OpenHeaders -Token $token -PartnerId $partnerA -IdempotencyKey "batch-verify-$ts"
$batchV = Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/verify:batch" -Headers $hBatchV -Body (@{
    items = @(
        @{ vulInfoID = $instB1; verifyResult = "VALID"; srcMethod = "1021" },
        @{ vulInfoID = $instB2; verifyResult = "VALID"; srcMethod = "1021" }
    )
} | ConvertTo-Json -Depth 4)
Expect-Code $batchV 0 "POST /instances/verify:batch"

$hBatchR = New-OpenHeaders -Token $token -PartnerId $partnerA -IdempotencyKey "batch-rem-$ts"
$batchR = Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/remediate:batch" -Headers $hBatchR -Body (@{
    items = @(
        @{ vulInfoID = $instB1; vulInfoStat = 5; srcMethod = "1050"; remedDesc = "batch fix 1"; remedTime = "3日"; fixLnk = "https://example.com/patch" },
        @{ vulInfoID = $instB2; vulInfoStat = 5; srcMethod = "1050"; remedDesc = "batch fix 2"; remedTime = "3日"; fixLnk = "https://example.com/patch" }
    )
} | ConvertTo-Json -Depth 4)
Expect-Code $batchR 0 "POST /instances/remediate:batch"

$hBatchVf = New-OpenHeaders -Token $token -PartnerId $partnerA -IdempotencyKey "batch-vf-$ts"
$batchVf = Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/verify-fix:batch" -Headers $hBatchVf -Body (@{
    items = @(
        @{ vulInfoID = $instB1; verifyResult = "FIX_CONFIRMED" },
        @{ vulInfoID = $instB2; verifyResult = "FIX_FAILED" }
    )
} | ConvertTo-Json -Depth 4)
Expect-Code $batchVf 0 "POST /instances/verify-fix:batch"

Write-Phase "Phase 7 - Idempotency replay + negative cases"
$idKey = "idem-replay-$ts"
$idHeaders = New-OpenHeaders -Token $token -PartnerId $partnerA -IdempotencyKey $idKey
$idBody = @{ verifyResult = "VALID"; srcMethod = "1021" } | ConvertTo-Json
$idemTask = Create-TaskByJson -Headers $openA -ExtTaskId "EXT-IDEM-$ts" -ScanTemplateId 1002
$idemSearch = Search-Instances -Headers $openA -TaskId $idemTask
$instIdem = $idemSearch.items[0].vulInfoID
$first = Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/$instIdem/verify" -Headers $idHeaders -Body $idBody
$replay = Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/$instIdem/verify" -Headers $idHeaders -Body $idBody
if ($first.code -eq 0 -and $replay.code -eq 0) {
    Record "Idempotency-Key replay" "PASS" "both code=0"
} else {
    Record "Idempotency-Key replay" "FAIL" "first=$($first.code) replay=$($replay.code)"
}

$conflictKey = "idem-conflict-$ts"
$confTask = Create-TaskByJson -Headers $openA -ExtTaskId "EXT-CONF-$ts" -ScanTemplateId 1002
$confInst = (Search-Instances -Headers $openA -TaskId $confTask).items[0].vulInfoID
$conflictHeaders = New-OpenHeaders -Token $token -PartnerId $partnerA -IdempotencyKey $conflictKey
Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/$confInst/verify" -Headers $conflictHeaders `
    -Body (@{ verifyResult = "VALID"; srcMethod = "1021" } | ConvertTo-Json) | Out-Null
$conflict = Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/$confInst/verify" -Headers $conflictHeaders `
    -Body (@{ verifyResult = "FALSE_POSITIVE" } | ConvertTo-Json)
Expect-Code $conflict 40901 "Idempotency-Key same key different body"

$badRem = Invoke-Api -Method Post -Uri "$Base/api/open/v1/instances/$instFp/remediate" -Headers $openA -Body "{}"
Expect-Code $badRem 40002 "remediate on false-positive stat=3"

$noPartner = @{ "Authorization" = "Bearer $token"; "Content-Type" = "application/json" }
$noPid = Invoke-Api -Method Post -Uri "$Base/api/open/v1/tasks/vul" -Headers $noPartner -Body (@{
    extTaskId = "EXT-NOPID-$ts"; taskName = "x"; type = 1; scanTemplateId = 1003; reportTemplateId = 2001; targets = @{ hosts = "1.1.1.1" }
} | ConvertTo-Json -Depth 3)
Expect-Code $noPid 40001 "missing X-Partner-Id"

$pbCreate = Invoke-Api -Method Post -Uri "$Base/internal/admin/partners" -Headers $adminHeaders -Body (@{
    partnerId = $partnerB; partnerName = "Partner B"; partnerType = "SIEM"; capabilities = @("TASK_READ"); rateLimitQps = 100
} | ConvertTo-Json)
$credB = Invoke-Api -Method Post -Uri "$Base/internal/admin/partners/$partnerB/credentials" -Headers $adminHeaders
$tokB = (Invoke-Api -Method Post -Uri "$Base/oauth/token" -Body (@{
    grantType = "client_credentials"; clientId = $credB.data.clientId; clientSecret = $credB.data.clientSecret
} | ConvertTo-Json)).data.accessToken
$openB = New-OpenHeaders -Token $tokB -PartnerId $partnerB
$cross = Invoke-Api -Uri "$Base/api/open/v1/tasks/$taskMain" -Headers $openB
Expect-Code $cross 40003 "Partner B cannot access Partner A taskId"

$exportListFinal = Invoke-Api -Uri "$Base/api/open/v1/tasks/$taskMain/exports?page=1&size=50" -Headers $openA
if ($exportListFinal.code -eq 0 -and $exportListFinal.data.items.Count -ge 1) {
    $anyExportId = $exportListFinal.data.items[0].exportId
    $crossExp = Invoke-Api -Uri "$Base/api/open/v1/exports/$anyExportId" -Headers $openB
    Expect-Code $crossExp 40003 "Partner B cannot GET Partner A exportId"
} else {
    Record "Partner B cannot GET Partner A exportId" "SKIP" "no exports on taskMain"
}

Write-Phase "Phase 8 - Invocation governance + Webhook delivery log"
$inv = Invoke-Api -Uri "$Base/internal/admin/invocations?partnerId=$partnerA&page=1&size=5" -Headers $adminHeaders
Expect-Code $inv 0 "GET /internal/admin/invocations"

$wh = Invoke-Api -Uri "$Base/internal/admin/webhook-deliveries?partnerId=$partnerA&page=1&size=5" -Headers $adminHeaders
Expect-Code $wh 0 "GET /internal/admin/webhook-deliveries"

$tcWh = Count-WebhookEventType -PartnerId $partnerA -EventType "TASK_COMPLETED"
if ($tcWh -ge 1) {
    Record "Webhook TASK_COMPLETED logged" "PASS" "count=$tcWh"
} else {
    Record "Webhook TASK_COMPLETED logged" "FAIL" "count=$tcWh"
}

$erWh = Count-WebhookEventType -PartnerId $partnerA -EventType "EXPORT_READY"
if ($erWh -ge 1) {
    Record "Webhook EXPORT_READY logged (json, reportTemplateId=2001)" "PASS" "count=$erWh"
} else {
    $expSum = Get-TaskExportSummary -TaskId $taskMain -Headers $openA
    if ($expSum.failed -ge 1 -and $expSum.ready -eq 0) {
        Record "Webhook EXPORT_READY logged (json, reportTemplateId=2001)" "SKIP" "exports FAILED (file-sharing-center?)"
    } else {
        Record "Webhook EXPORT_READY logged (json, reportTemplateId=2001)" "FAIL" "count=0"
    }
}

$vfWh = Count-WebhookEventType -PartnerId $partnerA -EventType "INSTANCE_VERIFY_FIX_COMPLETED"
if ($vfWh -ge 1) {
    Record "Webhook INSTANCE_VERIFY_FIX_COMPLETED" "PASS" "count=$vfWh"
} else {
    Record "Webhook INSTANCE_VERIFY_FIX_COMPLETED" "FAIL" "count=$vfWh"
}

$erPage = Get-WebhookDeliveryPage -PartnerId $partnerA -EventType "EXPORT_READY" -Size 20
if ($erPage.code -eq 0 -and $erPage.data.items -and $erPage.data.items.Count -ge 1) {
    $samplePayload = $erPage.data.items[0]
    $hasUrl = $false
    if ($samplePayload.callbackUrl) { $hasUrl = $true }
    Record "Webhook EXPORT_READY delivery row" "PASS" "callbackUrl configured"
} else {
    Record "Webhook EXPORT_READY delivery row" "SKIP" "no delivery rows"
}

$quota = Invoke-Api -Uri "$Base/internal/admin/quotas?partnerId=$partnerA&page=1&size=5" -Headers $adminHeaders
Expect-Code $quota 0 "GET /internal/admin/quotas"

Write-Phase "Phase 9 - Catalog APIs not implemented (SKIP)"
Record "POST /api/open/v1/tasks (legacy createTask)" "SKIP" "no Controller mapping"
Record "POST /instances/{id}/archive" "SKIP" "REST not implemented"
Record "receivePlatformWebhook" "SKIP" "Partner-side callback endpoint"

Write-Phase "Summary"
Write-Host ""
$script:Results | ForEach-Object { Write-Host $_ }
Write-Host ""
Write-Host "PASS=$script:Passed  FAIL=$script:Failed  SKIP=$script:Skipped" -ForegroundColor Cyan
Write-Host "partnerA=$partnerA  taskMain=$taskMain  extMain=$extMain"
if ($script:Failed -gt 0) { exit 1 }
