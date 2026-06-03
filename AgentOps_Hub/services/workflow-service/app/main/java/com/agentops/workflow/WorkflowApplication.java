package com.agentops.workflow;

import java.time.Clock;

public final class WorkflowApplication {
  private WorkflowApplication() {}

  public static void main(String[] args) {
    WorkflowConfig config = WorkflowConfig.fromEnvironment();
    H2WorkflowRepository repository = new H2WorkflowRepository(config.dbUrl());
    repository.initialize();
    WorkflowAuthService authService = new WorkflowAuthService(new JwtVerifier(config.jwtSecret(), Clock.systemUTC()));
    WorkflowService workflowService = new WorkflowService(repository, Clock.systemUTC());
    int port = new WorkflowHttpServer(authService, workflowService).start(config.port());
    System.out.println("workflow-service listening on " + port);
  }
}
