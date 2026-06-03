$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$serviceDir = Join-Path $root "ai-services\rag-service"
$requiredFiles = @(
  "ai-services/rag-service/README.md",
  "ai-services/rag-service/pyproject.toml",
  "ai-services/rag-service/src/rag_service/__init__.py",
  "ai-services/rag-service/src/rag_service/models.py",
  "ai-services/rag-service/src/rag_service/service.py",
  "ai-services/rag-service/src/rag_service/eval_runner.py",
  "ai-services/rag-service/src/rag_service/cli.py",
  "ai-services/rag-service/tests/test_rag_service.py",
  "ai-services/rag-service/fixtures/billing_complaint_sop.txt",
  "ai-services/rag-service/fixtures/refund_policy_faq.txt",
  "ai-services/rag-service/evals/rag-eval.phase-007.json"
)

$errors = New-Object System.Collections.Generic.List[string]

foreach ($file in $requiredFiles) {
  if (-not (Test-Path -LiteralPath $file)) {
    $errors.Add("Missing required rag-service file: $file")
  }
}

$forbiddenPatterns = @(
  "pgvector",
  "Milvus",
  "RabbitMQ",
  "FastAPI",
  "approval_instance",
  "ActionCommand",
  "ticket.create_followup"
)

if (Test-Path -LiteralPath $serviceDir) {
  $scanFiles = Get-ChildItem -Path $serviceDir -Recurse -File -Include "*.py"
  foreach ($file in $scanFiles) {
    $raw = Get-Content -Raw -Encoding UTF8 $file.FullName
    foreach ($pattern in $forbiddenPatterns) {
      if ($raw -match [regex]::Escape($pattern)) {
        $errors.Add("Forbidden Phase 007 RAG capability marker '$pattern' found in $($file.FullName)")
      }
    }
  }
}

if ($errors.Count -gt 0) {
  Write-Host "Phase 007 rag-service validation failed:"
  foreach ($errorMessage in $errors) {
    Write-Host " - $errorMessage"
  }
  exit 1
}

Push-Location $serviceDir
try {
  $env:PYTHONPATH = (Join-Path $serviceDir "src")
  python -m unittest discover -s tests
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
  python -m rag_service.cli --scenario eval
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
} finally {
  Pop-Location
}

Write-Host "Phase 007 rag-service validation passed."
