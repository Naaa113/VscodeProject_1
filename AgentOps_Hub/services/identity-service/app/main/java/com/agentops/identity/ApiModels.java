package com.agentops.identity;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

record LoginRequest(String tenant_id, String username, String password) {}

record LoginResponse(String tenant_id, String user_id, String access_token, Instant expires_at) {}

record CurrentUserResponse(
    String tenant_id, String user_id, String username, List<String> roles, List<String> permissions) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record ErrorResponse(
    String error_code,
    String message,
    String trace_id,
    boolean retryable,
    Map<String, Object> details) {
  static ErrorResponse from(ErrorCode code, String traceId) {
    return new ErrorResponse(code.name(), code.defaultMessage, traceId, code.retryable, null);
  }
}

record UserRecord(String id, String tenantId, String username, String passwordHash, boolean enabled) {}

record JwtClaims(
    String subject,
    String tenantId,
    List<String> roles,
    List<String> permissions,
    Instant issuedAt,
    Instant expiresAt,
    String jwtId) {}
