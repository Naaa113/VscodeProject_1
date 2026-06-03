$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$appRoot = Join-Path $repoRoot "apps/web-console"

if (-not (Test-Path (Join-Path $appRoot "package.json"))) {
    throw "apps/web-console/package.json not found."
}

& npm.cmd --prefix $appRoot run typecheck
& npm.cmd --prefix $appRoot run test
& npm.cmd --prefix $appRoot run build

