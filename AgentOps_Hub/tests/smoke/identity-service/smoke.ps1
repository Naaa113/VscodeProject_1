$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
Set-Location $root
powershell -ExecutionPolicy Bypass -File scripts/validate-identity-service.ps1
