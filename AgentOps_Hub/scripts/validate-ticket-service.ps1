$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$errors = New-Object System.Collections.Generic.List[string]

$requiredFiles = @(
  "services/ticket-service/ticket-service.maven.xml",
  "services/ticket-service/.mvn/maven.config",
  "services/ticket-service/app/main/java/com/agentops/ticket/TicketApplication.java",
  "services/ticket-service/app/main/java/com/agentops/ticket/TicketHttpServer.java",
  "services/ticket-service/app/main/java/com/agentops/ticket/TicketService.java",
  "services/ticket-service/app/main/java/com/agentops/ticket/TicketAuthService.java",
  "services/ticket-service/app/main/java/com/agentops/ticket/JwtVerifier.java",
  "services/ticket-service/app/main/java/com/agentops/ticket/H2TicketRepository.java",
  "services/ticket-service/app/main/resources/schema.sql",
  "services/ticket-service/app/test/java/com/agentops/ticket/TicketServiceTest.java",
  "services/ticket-service/app/test/java/com/agentops/ticket/TicketHttpServerTest.java",
  "packages/shared-contracts/examples/openapi/tickets-list.success.v1.json",
  "packages/shared-contracts/examples/openapi/tickets-create.success.v1.json",
  "packages/shared-contracts/examples/openapi/tickets-detail.success.v1.json",
  "packages/shared-contracts/examples/openapi/tickets-detail.not-found.v1.json",
  "packages/shared-contracts/examples/tools/ticket.search.success.v1.json",
  "packages/shared-contracts/examples/tools/ticket.search.failure.v1.json"
)

foreach ($file in $requiredFiles) {
  if (-not (Test-Path -LiteralPath $file)) {
    $errors.Add("Missing ticket-service file: $file")
  }
}

$openApi = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/openapi/agentops-api.v1.yaml"
foreach ($needle in @("TicketStatus", "TicketPriority", "CustomerSummary", "CreateCustomerInput", "customer_id", "category", "created_from", "created_to", "sla_due_before", "query", "bearerAuth")) {
  if ($openApi -notmatch $needle) {
    $errors.Add("OpenAPI ticket contract missing $needle")
  }
}

$ticketTool = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/tools/ticket.search.v1.schema.json"
foreach ($needle in @('"requires_approval": false', '"customer_id"', '"category"', '"sla_due_before"', '"tenant_id"', '"ticket:read"')) {
  if ($ticketTool -notmatch $needle) {
    $errors.Add("ticket.search.v1 schema missing $needle")
  }
}

$schema = Get-Content -Raw -Encoding UTF8 "services/ticket-service/app/main/resources/schema.sql"
foreach ($needle in @("customer", "ticket", "ticket_audit_log", "tenant_id", "created_by", "sla_due_at")) {
  if ($schema -notmatch $needle) {
    $errors.Add("Ticket schema missing $needle")
  }
}

$serviceCode = (Get-Content -Raw -Encoding UTF8 "services/ticket-service/app/main/java/com/agentops/ticket/TicketService.java") +
  (Get-Content -Raw -Encoding UTF8 "services/ticket-service/app/main/java/com/agentops/ticket/H2TicketRepository.java")
foreach ($needle in @("TicketStatus.open", "TICKET_CREATE", "TICKET_LIST", "TICKET_GET", "RESOURCE_NOT_FOUND")) {
  if ($serviceCode -notmatch $needle) {
    $errors.Add("Ticket service behavior missing $needle")
  }
}

Push-Location "services/ticket-service"
try {
  mvn test
} finally {
  Pop-Location
}

if ($errors.Count -gt 0) {
  Write-Host "Phase 004 ticket-service validation failed:"
  foreach ($errorMessage in $errors) {
    Write-Host " - $errorMessage"
  }
  exit 1
}

Write-Host "Phase 004 ticket-service validation passed."
