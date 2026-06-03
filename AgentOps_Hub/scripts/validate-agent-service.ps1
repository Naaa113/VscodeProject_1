$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$serviceDir = Join-Path $root "ai-services\agent-service"
$ragServiceDir = Join-Path $root "ai-services\rag-service"
$requiredFiles = @(
  "ai-services/agent-service/README.md",
  "ai-services/agent-service/pyproject.toml",
  "ai-services/agent-service/src/agent_service/models.py",
  "ai-services/agent-service/src/agent_service/graph.py",
  "ai-services/agent-service/src/agent_service/tools.py",
  "ai-services/agent-service/src/agent_service/store.py",
  "ai-services/agent-service/src/agent_service/eval_runner.py",
  "ai-services/agent-service/tests/test_agent_graph.py",
  "ai-services/agent-service/evals/complaint-analysis.phase-005.json",
  "ai-services/agent-service/evals/complaint-analysis.phase-007.json",
  "ai-services/agent-service/evals/complaint-analysis.phase-008.json",
  "ai-services/rag-service/src/rag_service/service.py"
)

$errors = New-Object System.Collections.Generic.List[string]

foreach ($file in $requiredFiles) {
  if (-not (Test-Path -LiteralPath $file)) {
    $errors.Add("Missing required agent-service file: $file")
  }
}

$forbiddenPatterns = @(
  "FastAPI",
  "RabbitMQ",
  "Embedding",
  "pgvector",
  "Milvus"
)

if (Test-Path -LiteralPath $serviceDir) {
  $pythonFiles = Get-ChildItem -Path $serviceDir -Recurse -File -Include "*.py"
  foreach ($file in $pythonFiles) {
    $raw = Get-Content -Raw -Encoding UTF8 $file.FullName
    foreach ($pattern in $forbiddenPatterns) {
      if ($raw -match [regex]::Escape($pattern)) {
        $errors.Add("Forbidden Phase 005 capability marker '$pattern' found in $($file.FullName)")
      }
    }
  }
}

$graph = Get-Content -Raw -Encoding UTF8 "ai-services/agent-service/src/agent_service/graph.py"
foreach ($needle in @("ticket.create_followup.v1", "approval_instance_id", "mock_workflow_contract")) {
  if ($graph -notmatch [regex]::Escape($needle)) {
    $errors.Add("Phase 008 approval integration missing marker '$needle' in graph.py")
  }
}

$tooling = Get-Content -Raw -Encoding UTF8 "ai-services/agent-service/src/agent_service/tools.py"
foreach ($needle in @("TicketCreateFollowupToolClient", "\"waiting_approval\"", "\"approval_instance_id\"")) {
  if ($tooling -notmatch [regex]::Escape($needle)) {
    $errors.Add("Phase 008 follow-up tool integration missing marker '$needle' in tools.py")
  }
}

if ($errors.Count -gt 0) {
  Write-Host "Phase 005 agent-service validation failed:"
  foreach ($errorMessage in $errors) {
    Write-Host " - $errorMessage"
  }
  exit 1
}

Push-Location $serviceDir
try {
  $agentSrc = Join-Path $serviceDir "src"
  $ragSrc = Join-Path $ragServiceDir "src"
  $env:PYTHONPATH = $agentSrc + [System.IO.Path]::PathSeparator + $ragSrc
  python -m unittest discover -s tests
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
  python -m agent_service.cli --scenario eval
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
} finally {
  Pop-Location
}

Write-Host "Phase 007 agent-service validation passed."
