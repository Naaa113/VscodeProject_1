package com.agentops.workflow;

import java.util.Map;

record WorkflowConfig(int port, String dbUrl, String jwtSecret) {
  static WorkflowConfig fromEnvironment() {
    Map<String, String> env = System.getenv();
    String secret = env.get("WORKFLOW_JWT_SECRET");
    if (isBlank(secret) || secret.length() < 32) {
      throw new IllegalStateException("WORKFLOW_JWT_SECRET must be provided and at least 32 characters.");
    }
    return new WorkflowConfig(
        parseInt(env.get("WORKFLOW_PORT"), 8083),
        env.getOrDefault("WORKFLOW_DB_URL", "jdbc:h2:file:./data/workflow-service"),
        secret);
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
