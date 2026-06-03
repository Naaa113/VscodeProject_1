$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$errors = New-Object System.Collections.Generic.List[string]

$requiredFiles = @(
  "services/identity-service/identity-service.maven.xml",
  "services/identity-service/.mvn/maven.config",
  "services/identity-service/app/main/java/com/agentops/identity/IdentityApplication.java",
  "services/identity-service/app/main/java/com/agentops/identity/AuthService.java",
  "services/identity-service/app/main/java/com/agentops/identity/JwtService.java",
  "services/identity-service/app/main/java/com/agentops/identity/H2IdentityRepository.java",
  "services/identity-service/app/main/resources/schema.sql",
  "services/identity-service/app/test/java/com/agentops/identity/AuthServiceTest.java",
  "services/identity-service/app/test/java/com/agentops/identity/IdentityHttpServerTest.java",
  "packages/shared-contracts/examples/openapi/auth-me.success.v1.json",
  "packages/shared-contracts/examples/openapi/auth-me.token-expired.v1.json",
  "packages/shared-contracts/examples/openapi/auth-me.forbidden.v1.json"
)

foreach ($file in $requiredFiles) {
  if (-not (Test-Path -LiteralPath $file)) {
    $errors.Add("Missing identity-service file: $file")
  }
}

$openApi = Get-Content -Raw -Encoding UTF8 "packages/shared-contracts/openapi/agentops-api.v1.yaml"
foreach ($needle in @("bearerAuth", "'403':", "/api/auth/me", "/api/auth/login")) {
  if ($openApi -notmatch $needle) {
    $errors.Add("OpenAPI auth contract missing $needle")
  }
}

$schema = Get-Content -Raw -Encoding UTF8 "services/identity-service/app/main/resources/schema.sql"
foreach ($needle in @("tenant", "sys_user", "sys_role", "sys_permission", "identity_audit_log")) {
  if ($schema -notmatch $needle) {
    $errors.Add("Identity schema missing $needle")
  }
}

$jwtService = Get-Content -Raw -Encoding UTF8 "services/identity-service/app/main/java/com/agentops/identity/JwtService.java"
foreach ($needle in @('"sub"', '"tenant_id"', '"roles"', '"permissions"', '"iat"', '"exp"', '"jti"')) {
  if ($jwtService -notmatch [regex]::Escape($needle)) {
    $errors.Add("JWT payload missing $needle")
  }
}

Push-Location "services/identity-service"
try {
  mvn test
} finally {
  Pop-Location
}

if ($errors.Count -gt 0) {
  Write-Host "Phase 003 identity-service validation failed:"
  foreach ($errorMessage in $errors) {
    Write-Host " - $errorMessage"
  }
  exit 1
}

Write-Host "Phase 003 identity-service validation passed."
