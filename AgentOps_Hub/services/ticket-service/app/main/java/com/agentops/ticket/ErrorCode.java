package com.agentops.ticket;

enum ErrorCode {
  AUTH_TOKEN_EXPIRED(401, true, "Authentication token expired."),
  AUTH_FORBIDDEN(403, false, "Forbidden."),
  TENANT_REQUIRED(400, false, "Tenant context is required."),
  VALIDATION_FAILED(400, false, "Request validation failed."),
  RESOURCE_NOT_FOUND(404, false, "Resource not found."),
  CONFLICT(409, false, "Conflict."),
  INTERNAL_ERROR(500, true, "Internal error.");

  final int httpStatus;
  final boolean retryable;
  final String defaultMessage;

  ErrorCode(int httpStatus, boolean retryable, String defaultMessage) {
    this.httpStatus = httpStatus;
    this.retryable = retryable;
    this.defaultMessage = defaultMessage;
  }
}
