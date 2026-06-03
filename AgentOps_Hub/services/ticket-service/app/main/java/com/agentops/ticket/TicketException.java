package com.agentops.ticket;

final class TicketException extends RuntimeException {
  private final ErrorCode code;

  TicketException(ErrorCode code) {
    super(code.defaultMessage);
    this.code = code;
  }

  TicketException(ErrorCode code, Throwable cause) {
    super(code.defaultMessage, cause);
    this.code = code;
  }

  ErrorCode code() {
    return code;
  }
}
