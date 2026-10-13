$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path $PSScriptRoot -Parent
$directory = Join-Path $projectRoot "server\requests\fixtures"
New-Item -ItemType Directory -Path $directory -Force | Out-Null

$bitmap = New-Object System.Drawing.Bitmap(8, 6)
try {
    $bitmap.SetPixel(0, 0, [System.Drawing.Color]::Teal)
    $bitmap.Save(
        (Join-Path $directory "valid.png"),
        [System.Drawing.Imaging.ImageFormat]::Png
    )
    $bitmap.Save(
        (Join-Path $directory "valid.jpg"),
        [System.Drawing.Imaging.ImageFormat]::Jpeg
    )
} finally {
    $bitmap.Dispose()
}

[System.IO.File]::WriteAllText(
    (Join-Path $directory "invalid.png"),
    "This is not an image.",
    [System.Text.Encoding]::UTF8
)

[System.IO.File]::WriteAllBytes(
    (Join-Path $directory "large.png"),
    (New-Object byte[] (10 * 1024 * 1024 + 1))
)

Get-ChildItem $directory | Select-Object Name, Length