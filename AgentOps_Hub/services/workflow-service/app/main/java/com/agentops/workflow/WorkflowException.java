package com.agentops.workflow;

final class WorkflowException extends RuntimeException {
  private final ErrorCode code;

  WorkflowException(ErrorCode code) {
    super(code.defaultMessage);
    this.code = code;
  }

  WorkflowException(ErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  WorkflowException(ErrorCode code, Throwable cause) {
    super(code.defaultMessage, cause);
    this.code = code;
  }

  ErrorCode code() {
    return code;
  }
}
