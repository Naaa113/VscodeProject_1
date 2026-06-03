$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$errors = New-Object System.Collections.Generic.List[string]

$requiredFiles = @(
  "services/workflow-service/workflow-service.maven.xml",
  "services/workflow-service/.mvn/maven.config",
  "services/workflow-service/README.md",
  "services/workflow-service/app/main/java/com/agentops/workflow/WorkflowApplication.java",
  "services/workflow-service/app/main/java/com/agentops/workflow/WorkflowHttpServer.java",
  "services/workflow-service/app/main/java/com/agentops/workflow/WorkflowService.java",
  "services/workflow-service/app/main/java/com/agentops/workflow/WorkflowAuthService.java",
  "services/workflow-service/app/main/java/com/agentops/workflow/JwtVerifier.java",
  "services/workflow-service/app/main/java/com/agentops/workflow/H2WorkflowRepository.java",
  "services/workflow-service/app/main/resources/schema.sql",
  "services/workflow-service/app/test/java/com/agentops/workflow/WorkflowHttpServerTest.java",
  "services/workflow-service/app/test/java/com/agentops/workflow/WorkflowServiceTest.java",
  "packages/shared-contracts/tools/ticket.create_followup.v1.schema.json",
  "packages/shared-contracts/examples/openapi/approvals-create.success.v1.json",
  "packages/shared-contracts/examples/openapi/action-commands.success.v1.json"
)

foreach ($file in $requiredFiles) {
  if (-not (Test-Path -LiteralPath $file)) {
    $errors.Add("Missing workflow-service file: $file")
  }
}

$openApi = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/openapi/agentops-api.v1.yaml"
foreach ($needle in @("/api/approvals", "/api/approvals/{id}/approve", "/api/approvals/{id}/reject", "/api/approvals/{id}/cancel", "/api/action-commands/{id}", "ApprovalInstance", "ActionCommand")) {
  if ($openApi -notmatch [regex]::Escape($needle)) {
    $errors.Add("OpenAPI workflow contract missing $needle")
  }
}

$workflowSchema = Get-Content -Raw -Encoding UTF8 "services/workflow-service/app/main/resources/schema.sql"
foreach ($needle in @("approval_instance", "approval_record", "action_command", "idempotency_key", "request_fingerprint")) {
  if ($workflowSchema -notmatch $needle) {
    $errors.Add("workflow-service schema missing $needle")
  }
}

$toolSchema = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/tools/ticket.create_followup.v1.schema.json"
foreach ($needle in @('"requires_approval": true', '"approval_instance_id"', '"idempotency_key"')) {
  if ($toolSchema -notmatch $needle) {
    $errors.Add("ticket.create_followup.v1 schema missing $needle")
  }
}
if ($toolSchema -match 'action_command_status') {
  $errors.Add("ticket.create_followup.v1 must not expose action_command_status before approval.")
}

Push-Location "services/workflow-service"
try {
  mvn test
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
} finally {
  Pop-Location
}

if ($errors.Count -gt 0) {
  Write-Host "Phase 008 workflow-service validation failed:"
  foreach ($errorMessage in $errors) {
    Write-Host " - $errorMessage"
  }
  exit 1
}

Write-Host "Phase 008 workflow-service validation passed."
