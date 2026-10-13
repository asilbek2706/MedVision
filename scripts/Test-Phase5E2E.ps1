param(
    [string]$BaseUrl = "http://localhost:18080"
)

$ErrorActionPreference = "Stop"
$api = "$BaseUrl/api"
$passed = 0
$failed = 0

function Check {
    param(
        [bool]$Condition,
        [string]$Name
    )

    if ($Condition) {
        $script:passed++
        Write-Host "[PASS] $Name" -ForegroundColor Green
    }
    else {
        $script:failed++
        Write-Host "[FAIL] $Name" -ForegroundColor Red
    }
}

function New-TestSession {
    return New-Object Microsoft.PowerShell.Commands.WebRequestSession
}

function Get-Csrf {
    param($Session)

    $response = Invoke-RestMethod `
        -Uri "$api/auth/csrf" `
        -Method Get `
        -WebSession $Session

    return $response.token
}

function Post-Json {
    param(
        [string]$Url,
        $Session,
        [string]$Csrf,
        $Body
    )

    return Invoke-RestMethod `
        -Uri $Url `
        -Method Post `
        -WebSession $Session `
        -Headers @{ "X-CSRF-TOKEN" = $Csrf } `
        -ContentType "application/json" `
        -Body ($Body | ConvertTo-Json -Depth 10)
}

function Upload-Image {
    param(
        $Session,
        [string]$Csrf,
        [string]$FilePath,
        [string]$MimeType
    )

    $fileName = [IO.Path]::GetFileName($FilePath)
    $bytes = [IO.File]::ReadAllBytes($FilePath)

    $boundary = "MedVisionBoundary" + [guid]::NewGuid().ToString("N")
    $crlf = "`r`n"

    $prefix = (
        "--$boundary$crlf" +
        "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"$crlf" +
        "Content-Type: $MimeType$crlf$crlf"
    )

    $suffix = "$crlf--$boundary--$crlf"

    $prefixBytes = [Text.Encoding]::UTF8.GetBytes($prefix)
    $suffixBytes = [Text.Encoding]::UTF8.GetBytes($suffix)

    $body = New-Object byte[] (
        $prefixBytes.Length + $bytes.Length + $suffixBytes.Length
    )

    [Array]::Copy($prefixBytes, 0, $body, 0, $prefixBytes.Length)
    [Array]::Copy($bytes, 0, $body, $prefixBytes.Length, $bytes.Length)
    [Array]::Copy(
        $suffixBytes, 0, $body,
        $prefixBytes.Length + $bytes.Length,
        $suffixBytes.Length
    )

    return Invoke-RestMethod `
        -Uri "$api/images" `
        -Method Post `
        -WebSession $Session `
        -Headers @{ "X-CSRF-TOKEN" = $Csrf } `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body $body
}

try {
    Write-Host "`n=== MEDVISION PHASE 5 E2E ===" -ForegroundColor Cyan

    $root = Split-Path $PSScriptRoot -Parent
    $fixtures = Join-Path $root "server\requests\fixtures"

    $png = Join-Path $fixtures "valid.png"
    $jpg = Join-Path $fixtures "valid.jpg"

    if (-not (Test-Path $png) -or -not (Test-Path $jpg)) {
        throw "PNG/JPEG fixtures missing."
    }

    # Health
    $health = Invoke-RestMethod "$api/health"
    Check ($health.status -eq "running") "Backend health"

    # Owner A
    $sessionA = New-TestSession
    $csrfA = Get-Csrf $sessionA

    $tag = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $password = "MedVisionTest2026!"

    $emailA = "phase5-a-$tag@example.test"
    $emailB = "phase5-b-$tag@example.test"

    $registerA = Post-Json "$api/auth/register" $sessionA $csrfA @{
        name = "Phase 5 Owner A"
        email = $emailA
        password = $password
    }

    Check ($null -ne $registerA.id) "Register A"

    $loginA = Post-Json "$api/auth/login" $sessionA $csrfA @{
        email = $emailA
        password = $password
    }

    Check ($null -ne $loginA.id) "Login A"

    # Login rotates CSRF
    $csrfA = Get-Csrf $sessionA

    # PNG
    $uploadedPng = Upload-Image $sessionA $csrfA $png "image/png"
    $pngId = $uploadedPng.id

    Check (
        $uploadedPng.width -eq 8 -and
        $uploadedPng.height -eq 6 -and
        $uploadedPng.mimeType -eq "image/png"
    ) "Upload PNG"

    # JPEG
    $uploadedJpg = Upload-Image $sessionA $csrfA $jpg "image/jpeg"
    $jpgId = $uploadedJpg.id

    Check (
        $null -ne $jpgId -and
        $uploadedJpg.mimeType -eq "image/jpeg"
    ) "Upload JPEG"

    # List
    $list = Invoke-RestMethod `
        -Uri "$api/images" `
        -WebSession $sessionA

    Check (
        @($list.items | Where-Object { $_.id -eq $pngId }).Count -gt 0
    ) "Owner image list"

    # Detail
    $detail = Invoke-RestMethod `
        -Uri "$api/images/$pngId" `
        -WebSession $sessionA

    Check ($detail.id -eq $pngId) "Image detail"

    # Owner B
    $sessionB = New-TestSession
    $csrfB = Get-Csrf $sessionB

    $registerB = Post-Json "$api/auth/register" $sessionB $csrfB @{
        name = "Phase 5 Owner B"
        email = $emailB
        password = $password
    }

    Check ($null -ne $registerB.id) "Register B"

    $loginB = Post-Json "$api/auth/login" $sessionB $csrfB @{
        email = $emailB
        password = $password
    }

    Check ($null -ne $loginB.id) "Login B"

    $csrfB = Get-Csrf $sessionB

    # B cannot read A image
    $status = 0

    try {
        Invoke-WebRequest -UseBasicParsing `
            -Uri "$api/images/$pngId/content" `
            -WebSession $sessionB `
            -ErrorAction Stop | Out-Null

        $status = 200
    }
    catch {
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        }
    }

    Check ($status -eq 404) "Owner isolation"

    # Delete PNG
    $delete = Invoke-WebRequest -UseBasicParsing `
        -Uri "$api/images/$pngId" `
        -Method Delete `
        -WebSession $sessionA `
        -Headers @{ "X-CSRF-TOKEN" = $csrfA }

    Check ($delete.StatusCode -eq 204) "Delete PNG"

    Write-Host "`n=== SUMMARY ===" -ForegroundColor Cyan
    Write-Host "PASSED: $passed"
    Write-Host "FAILED: $failed"

    if ($failed -gt 0) {
        exit 1
    }
}
catch {
    Write-Host "`n[E2E ERROR] $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "PASSED: $passed"
    Write-Host "FAILED: $failed"
    exit 1
}