$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$requiredFiles = @(
  "packages/shared-contracts/README.md",
  "packages/shared-contracts/manifest.v1.json",
  "packages/shared-contracts/openapi/agentops-api.v1.yaml",
  "packages/shared-contracts/schemas/common.v1.schema.json",
  "packages/shared-contracts/schemas/status.v1.schema.json",
  "packages/shared-contracts/errors/error-codes.v1.json",
  "packages/shared-contracts/events/ai.task.created.v1.schema.json",
  "packages/shared-contracts/events/agent.run.started.v1.schema.json",
  "packages/shared-contracts/events/agent.step.completed.v1.schema.json",
  "packages/shared-contracts/events/agent.run.completed.v1.schema.json",
  "packages/shared-contracts/events/agent.run.failed.v1.schema.json",
  "packages/shared-contracts/events/document.uploaded.v1.schema.json",
  "packages/shared-contracts/events/document.indexed.v1.schema.json",
  "packages/shared-contracts/tools/ticket.search.v1.schema.json",
  "packages/shared-contracts/tools/ticket.create_followup.v1.schema.json",
  "packages/shared-contracts/tools/knowledge.search.v1.schema.json",
  "packages/shared-contracts/tools/report.save.v1.schema.json",
  "packages/shared-contracts/examples/openapi/auth-login.success.v1.json",
  "packages/shared-contracts/examples/openapi/auth-login.failure.v1.json",
  "packages/shared-contracts/examples/events/ai.task.created.v1.json",
  "packages/shared-contracts/examples/events/agent.run.started.v1.json",
  "packages/shared-contracts/examples/events/agent.step.completed.v1.json",
  "packages/shared-contracts/examples/events/agent.run.completed.v1.json",
  "packages/shared-contracts/examples/events/agent.run.failed.v1.json",
  "packages/shared-contracts/examples/events/document.uploaded.v1.json",
  "packages/shared-contracts/examples/events/document.indexed.v1.json",
  "packages/shared-contracts/examples/tools/ticket.search.success.v1.json",
  "packages/shared-contracts/examples/tools/ticket.create_followup.waiting_approval.v1.json",
  "packages/shared-contracts/examples/tools/knowledge.search.success.v1.json",
  "packages/shared-contracts/examples/tools/report.save.failure.v1.json"
)

$requiredOpenApiPaths = @(
  "/api/auth/login",
  "/api/auth/me",
  "/api/tickets",
  "/api/tickets/{id}",
  "/api/ai/tasks",
  "/api/ai/tasks/{id}",
  "/api/ai/tasks/{id}/steps",
  "/api/ai/tasks/{id}/report",
  "/api/knowledge/documents",
  "/api/knowledge/search",
  "/api/stream/tasks/{id}"
)

$errors = New-Object System.Collections.Generic.List[string]

foreach ($file in $requiredFiles) {
  if (-not (Test-Path -LiteralPath $file)) {
    $errors.Add("Missing required contract file: $file")
  }
}

if (Test-Path -LiteralPath "packages/shared-contracts/openapi/agentops-api.v1.yaml") {
  $openApi = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/openapi/agentops-api.v1.yaml"
  foreach ($path in $requiredOpenApiPaths) {
    if ($openApi -notmatch [regex]::Escape($path)) {
      $errors.Add("OpenAPI contract missing path: $path")
    }
  }
  if ($openApi -notmatch "text/event-stream") {
    $errors.Add("OpenAPI contract missing SSE text/event-stream response.")
  }
  if ($openApi -notmatch "ErrorResponse") {
    $errors.Add("OpenAPI contract missing unified ErrorResponse reference.")
  }
}

$jsonFiles = Get-ChildItem -Recurse -Force "packages/shared-contracts" -Filter "*.json"
foreach ($file in $jsonFiles) {
  try {
    Get-Content -Raw -Encoding UTF8 $file.FullName | ConvertFrom-Json | Out-Null
  } catch {
    $errors.Add("Invalid JSON: $($file.FullName) :: $($_.Exception.Message)")
  }
}

$eventSchemas = Get-ChildItem -Force "packages/shared-contracts/events" -Filter "*.schema.json"
foreach ($file in $eventSchemas) {
  $raw = Get-Content -Raw -Encoding UTF8 $file.FullName
  foreach ($needle in @("event_id", "event_name", "event_version", "occurred_at", "tenant_id", "trace_id", "payload", "x_producer", "x_consumers", "x_compatibility")) {
    if ($raw -notmatch $needle) {
      $errors.Add("Event schema $($file.Name) missing $needle")
    }
  }
}

$toolSchemas = Get-ChildItem -Force "packages/shared-contracts/tools" -Filter "*.schema.json"
foreach ($file in $toolSchemas) {
  $raw = Get-Content -Raw -Encoding UTF8 $file.FullName
  foreach ($needle in @("permission", "requires_approval", "idempotency", "timeout_ms", "retry", "audit_fields", "request", "response")) {
    if ($raw -notmatch $needle) {
      $errors.Add("Tool schema $($file.Name) missing $needle")
    }
  }
}

$followup = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/tools/ticket.create_followup.v1.schema.json"
if ($followup -notmatch '"requires_approval": true') {
  $errors.Add("ticket.create_followup.v1 must require approval or policy check.")
}

$knowledge = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/tools/knowledge.search.v1.schema.json"
if ($knowledge -match "pgvector|Milvus") {
  $errors.Add("knowledge.search.v1 must remain vector-storage neutral.")
}

$scanRoots = @("apps", "services", "ai-services", "packages", "tests")
$forbidden = Get-ChildItem -Recurse -Force $scanRoots |
  Where-Object {
    $_.Name -match "^(pom.xml|build.gradle|settings.gradle|package.json|vite.config.*|next.config.*|vue.config.*|pyproject.toml|requirements.txt|Dockerfile)$" -or
    $_.FullName -match "\\src\\|\\node_modules\\|\\migrations\\|\\alembic\\|\\routes\\|\\controllers\\|\\components\\"
  }

foreach ($item in $forbidden) {
  $errors.Add("Forbidden implementation artifact found: $($item.FullName)")
}

if ($errors.Count -gt 0) {
  Write-Host "Phase 002 contract validation failed:"
  foreach ($errorMessage in $errors) {
    Write-Host " - $errorMessage"
  }
  exit 1
}

Write-Host "Phase 002 contract validation passed."
