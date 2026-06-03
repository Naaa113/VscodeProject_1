package com.agentops.identity;

import java.util.List;

final class AuthService {
  private final H2IdentityRepository repository;
  private final PasswordHasher passwordHasher;
  private final JwtService jwtService;

  AuthService(H2IdentityRepository repository, PasswordHasher passwordHasher, JwtService jwtService) {
    this.repository = repository;
    this.passwordHasher = passwordHasher;
    this.jwtService = jwtService;
  }

  LoginResponse login(LoginRequest request, String traceId) {
    if (request == null || isBlank(request.tenant_id())) {
      auditLoginFailure(request == null ? null : request.tenant_id(), traceId, ErrorCode.TENANT_REQUIRED);
      throw new IdentityException(ErrorCode.TENANT_REQUIRED);
    }
    if (isBlank(request.username()) || isBlank(request.password())) {
      auditLoginFailure(request.tenant_id(), traceId, ErrorCode.VALIDATION_FAILED);
      throw new IdentityException(ErrorCode.VALIDATION_FAILED);
    }
    if (!repository.tenantEnabled(request.tenant_id())) {
      auditLoginFailure(request.tenant_id(), traceId, ErrorCode.AUTH_INVALID_CREDENTIALS);
      throw new IdentityException(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }
    UserRecord user =
        repository
            .findUserForLogin(request.tenant_id(), request.username())
            .orElseThrow(
                () -> {
                  auditLoginFailure(request.tenant_id(), traceId, ErrorCode.AUTH_INVALID_CREDENTIALS);
                  return new IdentityException(ErrorCode.AUTH_INVALID_CREDENTIALS);
                });
    if (!user.enabled() || !passwordHasher.verify(request.password().toCharArray(), user.passwordHash())) {
      auditLoginFailure(request.tenant_id(), traceId, ErrorCode.AUTH_INVALID_CREDENTIALS);
      throw new IdentityException(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }
    List<String> roles = repository.rolesForUser(user.id());
    List<String> permissions = repository.permissionsForUser(user.id());
    TokenIssue token;
    try {
      token = jwtService.issue(user, roles, permissions);
    } catch (IdentityException ex) {
      repository.audit(user.tenantId(), user.id(), "LOGIN", false, ex.code(), traceId);
      throw ex;
    } catch (RuntimeException ex) {
      repository.audit(user.tenantId(), user.id(), "LOGIN", false, ErrorCode.INTERNAL_ERROR, traceId);
      throw new IdentityException(ErrorCode.INTERNAL_ERROR, ex);
    }
    repository.audit(user.tenantId(), user.id(), "LOGIN", true, null, traceId);
    return new LoginResponse(user.tenantId(), user.id(), token.accessToken(), token.expiresAt());
  }

  CurrentUserResponse currentUser(String authorizationHeader, String tenantHeader, String traceId) {
    if (isBlank(tenantHeader)) {
      repository.audit(null, null, "CURRENT_USER", false, ErrorCode.TENANT_REQUIRED, traceId);
      throw new IdentityException(ErrorCode.TENANT_REQUIRED);
    }
    if (isBlank(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
      repository.audit(tenantHeader, null, "CURRENT_USER", false, ErrorCode.AUTH_FORBIDDEN, traceId);
      throw new IdentityException(ErrorCode.AUTH_FORBIDDEN);
    }
    JwtClaims claims;
    try {
      claims = jwtService.verify(authorizationHeader.substring("Bearer ".length()).trim());
    } catch (IdentityException ex) {
      repository.audit(tenantHeader, null, "CURRENT_USER", false, ex.code(), traceId);
      throw ex;
    }
    if (!tenantHeader.equals(claims.tenantId())) {
      repository.audit(tenantHeader, claims.subject(), "CURRENT_USER", false, ErrorCode.AUTH_FORBIDDEN, traceId);
      throw new IdentityException(ErrorCode.AUTH_FORBIDDEN);
    }
    UserRecord user =
        repository
            .findUserById(claims.tenantId(), claims.subject())
            .orElseThrow(
                () -> {
                  repository.audit(
                      claims.tenantId(), claims.subject(), "CURRENT_USER", false, ErrorCode.AUTH_FORBIDDEN, traceId);
                  return new IdentityException(ErrorCode.AUTH_FORBIDDEN);
                });
    if (!user.enabled()) {
      repository.audit(user.tenantId(), user.id(), "CURRENT_USER", false, ErrorCode.AUTH_FORBIDDEN, traceId);
      throw new IdentityException(ErrorCode.AUTH_FORBIDDEN);
    }
    List<String> roles = repository.rolesForUser(user.id());
    List<String> permissions = repository.permissionsForUser(user.id());
    repository.audit(user.tenantId(), user.id(), "CURRENT_USER", true, null, traceId);
    return new CurrentUserResponse(user.tenantId(), user.id(), user.username(), roles, permissions);
  }

  private void auditLoginFailure(String tenantId, String traceId, ErrorCode errorCode) {
    repository.audit(tenantId, null, "LOGIN", false, errorCode, traceId);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
