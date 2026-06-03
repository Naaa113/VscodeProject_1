$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$requiredPaths = @(
  "README.md",
  "CONTRIBUTING.md",
  ".gitignore",
  ".editorconfig",
  ".env.example",
  "docs/development/README.md",
  "docs/development/local-dev.md",
  "docs/adr/0001-frontend-stack.md",
  "docs/adr/0002-message-queue.md",
  "docs/adr/0003-relational-database.md",
  "docs/adr/0004-vector-search.md",
  "docs/adr/0005-realtime-channel.md",
  "apps/web-console/README.md",
  "apps/api-gateway/README.md",
  "services/identity-service/README.md",
  "services/ticket-service/README.md",
  "services/workflow-service/README.md",
  "services/notification-service/README.md",
  "ai-services/agent-service/README.md",
  "ai-services/rag-service/README.md",
  "ai-services/analytics-worker/README.md",
  "packages/shared-contracts/README.md",
  "packages/shared-contracts/openapi/.gitkeep",
  "packages/shared-contracts/events/.gitkeep",
  "packages/shared-contracts/tools/.gitkeep",
  "packages/shared-contracts/examples/.gitkeep",
  "packages/shared-contracts/errors/.gitkeep",
  "packages/prompt-templates/README.md",
  "packages/test-fixtures/README.md",
  "deploy/README.md",
  "tests/README.md",
  "tests/contract/README.md",
  "tests/integration/README.md",
  "tests/e2e/README.md",
  "tests/smoke/README.md"
)

$missing = @()
foreach ($path in $requiredPaths) {
  if (-not (Test-Path -LiteralPath $path)) {
    $missing += $path
  }
}

$scanRoots = @("apps", "services", "ai-services", "packages", "deploy", "tests")
$forbidden = Get-ChildItem -Recurse -Force $scanRoots |
  Where-Object {
    $_.Name -match "^(pom.xml|build.gradle|settings.gradle|package.json|vite.config.*|next.config.*|vue.config.*|pyproject.toml|requirements.txt|Dockerfile)$" -or
    $_.FullName -match "\\src\\|\\node_modules\\|\\migrations\\|\\alembic\\|\\routes\\|\\controllers\\|\\components\\"
  }

if ($missing.Count -gt 0) {
  Write-Host "Missing required Phase 001 skeleton paths:"
  $missing | ForEach-Object { Write-Host " - $_" }
}

if ($forbidden.Count -gt 0) {
  Write-Host "Forbidden Phase 001 implementation artifacts found:"
  $forbidden | ForEach-Object { Write-Host " - $($_.FullName)" }
}

if ($missing.Count -gt 0 -or $forbidden.Count -gt 0) {
  exit 1
}

Write-Host "Phase 001 skeleton validation passed."
