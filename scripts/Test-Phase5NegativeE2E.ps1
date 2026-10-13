param(
    [string]$BaseUrl = "http://localhost:18080"
)

$ErrorActionPreference = "Stop"
$api = "$BaseUrl/api"
$fixtures = Join-Path (Split-Path $PSScriptRoot -Parent) "server\requests\fixtures"
$passed = 0
$failed = 0

function Check {
    param([bool]$Condition, [string]$Name, [string]$Details)

    if ($Condition) {
        $script:passed++
        Write-Host "[PASS] $Name $Details" -ForegroundColor Green
    }
    else {
        $script:failed++
        Write-Host "[FAIL] $Name $Details" -ForegroundColor Red
    }
}

function Get-Status {
    param([scriptblock]$Request)

    try {
        $response = & $Request
        return [int]$response.StatusCode
    }
    catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

function Upload {
    param(
        $Session,
        [string]$Csrf,
        [string]$FilePath,
        [string]$Mime
    )

    $boundary = "MedVisionBoundary" + [guid]::NewGuid().ToString("N")
    $crlf = "`r`n"
    $name = [IO.Path]::GetFileName($FilePath)
    $bytes = [IO.File]::ReadAllBytes($FilePath)

    $prefix = "--$boundary$crlf" +
        "Content-Disposition: form-data; name=`"file`"; filename=`"$name`"$crlf" +
        "Content-Type: $Mime$crlf$crlf"

    $suffix = "$crlf--$boundary--$crlf"

    $p = [Text.Encoding]::UTF8.GetBytes($prefix)
    $s = [Text.Encoding]::UTF8.GetBytes($suffix)
    $body = New-Object byte[] ($p.Length + $bytes.Length + $s.Length)

    [Array]::Copy($p, 0, $body, 0, $p.Length)
    [Array]::Copy($bytes, 0, $body, $p.Length, $bytes.Length)
    [Array]::Copy($s, 0, $body, $p.Length + $bytes.Length, $s.Length)

    Invoke-WebRequest -UseBasicParsing `
        -Uri "$api/images" `
        -Method Post `
        -WebSession $Session `
        -Headers @{ "X-CSRF-TOKEN" = $Csrf } `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body $body `
        -ErrorAction Stop
}

try {
    Write-Host "`n=== PHASE 5 NEGATIVE E2E ===" -ForegroundColor Cyan

    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

    $csrf = (Invoke-RestMethod "$api/auth/csrf" -WebSession $session).token

    $tag = [guid]::NewGuid().ToString("N").Substring(0, 12)
    $email = "negative-$tag@example.test"
    $password = "MedVisionTest2026!"

    $headers = @{ "X-CSRF-TOKEN" = $csrf }

    $register = Invoke-RestMethod `
        -Uri "$api/auth/register" `
        -Method Post `
        -WebSession $session `
        -Headers $headers `
        -ContentType "application/json" `
        -Body (@{
            name = "Negative Test User"
            email = $email
            password = $password
        } | ConvertTo-Json)

    Check ($null -ne $register.id) "Register" ""

    $login = Invoke-RestMethod `
        -Uri "$api/auth/login" `
        -Method Post `
        -WebSession $session `
        -Headers $headers `
        -ContentType "application/json" `
        -Body (@{
            email = $email
            password = $password
        } | ConvertTo-Json)

    Check ($null -ne $login.id) "Login" ""

    $csrf = (Invoke-RestMethod "$api/auth/csrf" -WebSession $session).token

    $fake = Join-Path $fixtures "fake.png"
    $large = Join-Path $fixtures "large.png"
    $valid = Join-Path $fixtures "valid.png"

    $status = Get-Status {
        Upload $session $csrf $fake "image/png"
    }
    Check ($status -eq 415) "Fake PNG rejected" "(HTTP $status, expected 415)"

    $status = Get-Status {
        Upload $session $csrf $large "image/png"
    }
    Check ($status -eq 413) "Oversized file rejected" "(HTTP $status, expected 413)"

    $status = Get-Status {
        Upload $session $csrf $valid "image/jpeg"
    }
    Check ($status -eq 415) "MIME mismatch rejected" "(HTTP $status, expected 415)"

    $status = Get-Status {
        Upload $session "" $valid "image/png"
    }
    Check ($status -eq 403) "Missing CSRF rejected" "(HTTP $status, expected 403)"

    Write-Host "`n=== SUMMARY ===" -ForegroundColor Cyan
    Write-Host "PASSED: $passed"
    Write-Host "FAILED: $failed"

    if ($failed -gt 0) { exit 1 }
}
catch {
    Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
