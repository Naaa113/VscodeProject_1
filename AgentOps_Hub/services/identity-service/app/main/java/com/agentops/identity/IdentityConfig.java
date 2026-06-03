package com.agentops.identity;

import java.time.Duration;
import java.util.Map;

record IdentityConfig(int port, String dbUrl, String jwtSecret, Duration tokenTtl) {
  static IdentityConfig fromEnvironment() {
    Map<String, String> env = System.getenv();
    String secret = env.get("IDENTITY_JWT_SECRET");
    if (isBlank(secret) || secret.length() < 32) {
      throw new IllegalStateException("IDENTITY_JWT_SECRET must be provided and at least 32 characters.");
    }
    return new IdentityConfig(
        parseInt(env.get("IDENTITY_PORT"), 8081),
        env.getOrDefault("IDENTITY_DB_URL", "jdbc:h2:file:./data/identity-service"),
        secret,
        Duration.ofSeconds(parseLong(env.get("IDENTITY_TOKEN_TTL_SECONDS"), 1800L)));
  }

  static IdentityConfig forTest(String dbUrl, String jwtSecret, Duration tokenTtl) {
    return new IdentityConfig(0, dbUrl, jwtSecret, tokenTtl);
  }

  private static int parseInt(String raw, int fallback) {
    if (isBlank(raw)) {
      return fallback;
    }
    return Integer.parseInt(raw);
  }

  private static long parseLong(String raw, long fallback) {
    if (isBlank(raw)) {
      return fallback;
    }
    return Long.parseLong(raw);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
