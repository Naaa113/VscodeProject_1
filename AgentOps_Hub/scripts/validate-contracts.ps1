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
  "packages/shared-contracts/examples/openapi/auth-me.success.v1.json",
  "packages/shared-contracts/examples/openapi/auth-me.token-expired.v1.json",
  "packages/shared-contracts/examples/openapi/auth-me.forbidden.v1.json",
  "packages/shared-contracts/examples/openapi/approvals-create.success.v1.json",
  "packages/shared-contracts/examples/openapi/approvals-detail.pending.v1.json",
  "packages/shared-contracts/examples/openapi/approvals-approve.success.v1.json",
  "packages/shared-contracts/examples/openapi/approvals-reject.success.v1.json",
  "packages/shared-contracts/examples/openapi/approvals-cancel.success.v1.json",
  "packages/shared-contracts/examples/openapi/action-commands.success.v1.json",
  "packages/shared-contracts/examples/openapi/action-commands.failed.v1.json",
  "packages/shared-contracts/examples/openapi/tickets-list.success.v1.json",
  "packages/shared-contracts/examples/openapi/tickets-create.success.v1.json",
  "packages/shared-contracts/examples/openapi/tickets-detail.success.v1.json",
  "packages/shared-contracts/examples/openapi/tickets-detail.not-found.v1.json",
  "packages/shared-contracts/examples/openapi/knowledge-documents-list.success.v1.json",
  "packages/shared-contracts/examples/openapi/knowledge-document-register.success.v1.json",
  "packages/shared-contracts/examples/openapi/knowledge-search.success.v1.json",
  "packages/shared-contracts/examples/events/ai.task.created.v1.json",
  "packages/shared-contracts/examples/events/agent.run.started.v1.json",
  "packages/shared-contracts/examples/events/agent.step.completed.v1.json",
  "packages/shared-contracts/examples/events/agent.run.completed.v1.json",
  "packages/shared-contracts/examples/events/agent.run.failed.v1.json",
  "packages/shared-contracts/examples/events/document.uploaded.v1.json",
  "packages/shared-contracts/examples/events/document.indexed.v1.json",
  "packages/shared-contracts/examples/tools/ticket.search.success.v1.json",
  "packages/shared-contracts/examples/tools/ticket.search.failure.v1.json",
  "packages/shared-contracts/examples/tools/ticket.create_followup.waiting_approval.v1.json",
  "packages/shared-contracts/examples/tools/ticket.create_followup.failure.v1.json",
  "packages/shared-contracts/examples/tools/knowledge.search.success.v1.json",
  "packages/shared-contracts/examples/tools/knowledge.search.failure.v1.json",
  "packages/shared-contracts/examples/tools/report.save.failure.v1.json",
  "scripts/validate-agent-service.ps1",
  "scripts/validate-rag-service.ps1"
)

$requiredOpenApiPaths = @(
  "/api/auth/login",
  "/api/auth/me",
  "/api/tickets",
  "/api/tickets/{id}",
  "/api/approvals",
  "/api/approvals/{id}",
  "/api/approvals/{id}/approve",
  "/api/approvals/{id}/reject",
  "/api/approvals/{id}/cancel",
  "/api/action-commands/{id}",
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
  foreach ($needle in @("TicketStatus", "TicketPriority", "CustomerSummary", "CreateCustomerInput", "customer_id", "category", "created_from", "created_to", "sla_due_before", "query", "bearerAuth")) {
    if ($openApi -notmatch $needle) {
      $errors.Add("OpenAPI ticket contract missing Phase 004 field: $needle")
    }
  }
  foreach ($needle in @("ApprovalInstance", "ApprovalRecord", "ActionCommand", "TicketFollowupActionInput", "TicketFollowupActionOutput")) {
    if ($openApi -notmatch $needle) {
      $errors.Add("OpenAPI approval contract missing Phase 008 schema: $needle")
    }
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
  foreach ($needle in @("error_code", "message", "trace_id", "retryable", "allOf")) {
    if ($raw -notmatch $needle) {
      $errors.Add("Tool schema $($file.Name) does not force unified failed error field: $needle")
    }
  }
  if ($raw -match '"error":\s*\{"type":\s*\["object",\s*"null"\]\}') {
    $errors.Add("Tool schema $($file.Name) still uses broad nullable object error shape.")
  }
}

$followup = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/tools/ticket.create_followup.v1.schema.json"
if ($followup -notmatch '"requires_approval": true') {
  $errors.Add("ticket.create_followup.v1 must require approval or policy check.")
}
foreach ($needle in @('"approval_instance_id"', '"waiting_approval"')) {
  if ($followup -notmatch $needle) {
    $errors.Add("ticket.create_followup.v1 missing Phase 008 approval field: $needle")
  }
}
if ($followup -match 'action_command_status') {
  $errors.Add("ticket.create_followup.v1 must not expose action_command_status before approval.")
}

$ticketSearch = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/tools/ticket.search.v1.schema.json"
foreach ($needle in @('"requires_approval": false', '"customer_id"', '"category"', '"sla_due_before"', '"tenant_id"', '"ticket:read"')) {
  if ($ticketSearch -notmatch $needle) {
    $errors.Add("ticket.search.v1 missing Phase 004 contract field: $needle")
  }
}

$knowledge = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/tools/knowledge.search.v1.schema.json"
if ($knowledge -match "pgvector|Milvus") {
  $errors.Add("knowledge.search.v1 must remain vector-storage neutral.")
}
foreach ($needle in @("mock_source", "Phase 005 local Agent runtime")) {
  if ($knowledge -notmatch $needle) {
    $errors.Add("knowledge.search.v1 missing Phase 005 mock marker: $needle")
  }
}
foreach ($needle in @("source_uri", "Storage-neutral citation URI")) {
  if ($knowledge -notmatch $needle) {
    $errors.Add("knowledge.search.v1 missing Phase 007 citation field: $needle")
  }
}

$errorCatalog = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/errors/error-codes.v1.json"
foreach ($code in @("AGENT_TOOL_FORBIDDEN", "APPROVAL_REQUIRED", "VALIDATION_FAILED", "DOWNSTREAM_UNAVAILABLE", "INTERNAL_ERROR")) {
  if ($errorCatalog -notmatch $code) {
    $errors.Add("Error catalog missing tool failure category: $code")
  }
}

$agentStep = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/events/agent.step.completed.v1.schema.json"
foreach ($needle in @('"status": {"const": "failed"}', '"required": ["error_code", "message", "retryable"]', '"message"', '"retryable"')) {
  if ($agentStep -notmatch [regex]::Escape($needle)) {
    $errors.Add("agent.step.completed.v1 failed-status constraint missing: $needle")
  }
}

$unapprovedImplementationRoots = @(
  "apps\api-gateway",
  "services\notification-service",
  "ai-services\analytics-worker"
)

foreach ($scanRoot in $unapprovedImplementationRoots) {
  if (-not (Test-Path -LiteralPath $scanRoot)) {
    continue
  }
  $forbidden = Get-ChildItem -Recurse -Force $scanRoot |
    Where-Object {
      $_.Name -match "^(pom.xml|build.gradle|settings.gradle|package.json|vite.config.*|next.config.*|vue.config.*|pyproject.toml|requirements.txt|Dockerfile)$" -or
      $_.FullName -match "\\src\\|\\node_modules\\|\\migrations\\|\\alembic\\|\\routes\\|\\controllers\\|\\components\\"
    }
  foreach ($item in $forbidden) {
    $errors.Add("Forbidden implementation artifact found in unapproved host: $($item.FullName)")
  }
}

if ($errors.Count -gt 0) {
  Write-Host "Phase 002 contract validation failed:"
  foreach ($errorMessage in $errors) {
    Write-Host " - $errorMessage"
  }
  exit 1
}

Write-Host "Phase 002 contract validation passed."
