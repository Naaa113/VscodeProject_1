package com.agentops.ticket;

final class TicketAuthService {
  private final JwtVerifier jwtVerifier;

  TicketAuthService(JwtVerifier jwtVerifier) {
    this.jwtVerifier = jwtVerifier;
  }

  RequestContext authenticate(
      String authorizationHeader, String tenantHeader, String traceId, String requiredPermission) {
    if (isBlank(tenantHeader)) {
      throw new TicketException(ErrorCode.TENANT_REQUIRED);
    }
    if (isBlank(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
      throw new TicketException(ErrorCode.AUTH_FORBIDDEN);
    }
    JwtClaims claims = jwtVerifier.verify(authorizationHeader.substring("Bearer ".length()).trim());
    if (!tenantHeader.equals(claims.tenantId())) {
      throw new TicketException(ErrorCode.AUTH_FORBIDDEN);
    }
    RequestContext context =
        new RequestContext(claims.tenantId(), claims.subject(), claims.roles(), claims.permissions(), traceId);
    if (!context.hasPermission(requiredPermission)) {
      throw new TicketException(ErrorCode.AUTH_FORBIDDEN);
    }
    return context;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
