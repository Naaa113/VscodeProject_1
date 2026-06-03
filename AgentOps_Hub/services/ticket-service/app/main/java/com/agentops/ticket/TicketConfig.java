package com.agentops.ticket;

import java.util.Map;

record TicketConfig(int port, String dbUrl, String jwtSecret) {
  static TicketConfig fromEnvironment() {
    Map<String, String> env = System.getenv();
    String secret = env.get("TICKET_JWT_SECRET");
    if (isBlank(secret) || secret.length() < 32) {
      throw new IllegalStateException("TICKET_JWT_SECRET must be provided and at least 32 characters.");
    }
    return new TicketConfig(
        parseInt(env.get("TICKET_PORT"), 8082),
        env.getOrDefault("TICKET_DB_URL", "jdbc:h2:file:./data/ticket-service"),
        secret);
  }

  static TicketConfig forTest(String dbUrl, String jwtSecret) {
    return new TicketConfig(0, dbUrl, jwtSecret);
  }

  private static int parseInt(String raw, int fallback) {
    if (isBlank(raw)) {
      return fallback;
    }
    return Integer.parseInt(raw);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
