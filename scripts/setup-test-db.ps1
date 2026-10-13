$ErrorActionPreference = "Stop"

Set-Location (Split-Path -Parent $PSScriptRoot)

docker compose up -d --wait postgres
if ($LASTEXITCODE -ne 0) {
    throw "PostgreSQL ishga tushmadi."
}

$exists = docker compose exec -T postgres psql `
    -U medvision -d postgres -tAc `
    "SELECT 1 FROM pg_database WHERE datname = 'medvision_test';"

if ($LASTEXITCODE -ne 0) {
    throw "Database tekshiruvi bajarilmadi."
}

if (($exists -join "").Trim() -ne "1") {
    docker compose exec -T postgres psql `
        -U medvision -d postgres -v ON_ERROR_STOP=1 `
        -c "CREATE DATABASE medvision_test OWNER medvision;"

    if ($LASTEXITCODE -ne 0) {
        throw "Test database yaratilmadi."
    }
}

Write-Output "medvision_test tayyor."