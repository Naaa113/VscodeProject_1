package com.agentops.identity;

final class IdentityException extends RuntimeException {
  private final ErrorCode code;

  IdentityException(ErrorCode code) {
    super(code.defaultMessage);
    this.code = code;
  }

  IdentityException(ErrorCode code, Throwable cause) {
    super(code.defaultMessage, cause);
    this.code = code;
  }

  ErrorCode code() {
    return code;
  }
}
