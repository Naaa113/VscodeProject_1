package com.agentops.workflow;

import java.util.List;

final class WorkflowAuthService {
  private final JwtVerifier jwtVerifier;

  WorkflowAuthService(JwtVerifier jwtVerifier) {
    this.jwtVerifier = jwtVerifier;
  }

  RequestContext authenticate(
      String authorizationHeader, String tenantHeader, String traceId, List<String> requiredPermissions) {
    if (isBlank(tenantHeader)) {
      throw new WorkflowException(ErrorCode.TENANT_REQUIRED);
    }
    if (isBlank(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
      throw new WorkflowException(ErrorCode.AUTH_FORBIDDEN);
    }
    JwtClaims claims = jwtVerifier.verify(authorizationHeader.substring("Bearer ".length()).trim());
    if (!tenantHeader.equals(claims.tenantId())) {
      throw new WorkflowException(ErrorCode.AUTH_FORBIDDEN);
    }
    RequestContext context =
        new RequestContext(claims.tenantId(), claims.subject(), claims.roles(), claims.permissions(), traceId);
    if (requiredPermissions != null
        && !requiredPermissions.isEmpty()
        && requiredPermissions.stream().noneMatch(context::hasPermission)) {
      throw new WorkflowException(ErrorCode.AUTH_FORBIDDEN);
    }
    return context;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
