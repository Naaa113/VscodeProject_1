package com.agentops.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class WorkflowHttpServer {
  private final WorkflowAuthService authService;
  private final WorkflowService workflowService;
  private final ObjectMapper mapper;
  private HttpServer server;

  WorkflowHttpServer(WorkflowAuthService authService, WorkflowService workflowService) {
    this.authService = authService;
    this.workflowService = workflowService;
    this.mapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  int start(int port) {
    try {
      server = HttpServer.create(new InetSocketAddress(port), 0);
      server.createContext("/api/approvals", this::handleApprovals);
      server.createContext("/api/action-commands", this::handleActionCommands);
      server.createContext("/actuator/health", this::handleHealth);
      server.start();
      return server.getAddress().getPort();
    } catch (IOException ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  private void handleApprovals(HttpExchange exchange) throws IOException {
    String traceId = traceId(exchange);
    if (missingTrace(exchange)) {
      send(exchange, ErrorCode.VALIDATION_FAILED.httpStatus, ErrorResponse.from(ErrorCode.VALIDATION_FAILED, traceId));
      return;
    }
    try {
      String path = exchange.getRequestURI().getPath();
      if ("/api/approvals".equals(path)) {
        if (!"POST".equals(exchange.getRequestMethod())) {
          send(exchange, 405, ErrorResponse.from(ErrorCode.AUTH_FORBIDDEN, traceId));
          return;
        }
        RequestContext context = authenticate(exchange, traceId, List.of("ticket:followup:request"));
        CreateApprovalRequest request = mapper.readValue(exchange.getRequestBody(), CreateApprovalRequest.class);
        send(exchange, 201, workflowService.createApproval(context, request));
        return;
      }
      if (!path.startsWith("/api/approvals/")) {
        send(exchange, 404, ErrorResponse.from(ErrorCode.RESOURCE_NOT_FOUND, traceId));
        return;
      }

      String suffix = path.substring("/api/approvals/".length());
      if ("GET".equals(exchange.getRequestMethod()) && !suffix.contains("/")) {
        RequestContext context = authenticate(exchange, traceId, List.of("ticket:followup:request", "approval:decide"));
        send(exchange, 200, workflowService.getApproval(context, suffix));
        return;
      }

      int slash = suffix.indexOf('/');
      if (slash < 0 || !"POST".equals(exchange.getRequestMethod())) {
        send(exchange, 404, ErrorResponse.from(ErrorCode.RESOURCE_NOT_FOUND, traceId));
        return;
      }
      String approvalId = suffix.substring(0, slash);
      String operation = suffix.substring(slash + 1);
      ApprovalDecisionRequest request = mapper.readValue(exchange.getRequestBody(), ApprovalDecisionRequest.class);
      if ("approve".equals(operation)) {
        RequestContext context = authenticate(exchange, traceId, List.of("approval:decide"));
        send(exchange, 200, workflowService.approveApproval(context, approvalId, request));
        return;
      }
      if ("reject".equals(operation)) {
        RequestContext context = authenticate(exchange, traceId, List.of("approval:decide"));
        send(exchange, 200, workflowService.rejectApproval(context, approvalId, request));
        return;
      }
      if ("cancel".equals(operation)) {
        RequestContext context = authenticate(exchange, traceId, List.of("ticket:followup:request", "approval:decide"));
        send(exchange, 200, workflowService.cancelApproval(context, approvalId, request));
        return;
      }
      send(exchange, 404, ErrorResponse.from(ErrorCode.RESOURCE_NOT_FOUND, traceId));
    } catch (WorkflowException ex) {
      send(exchange, ex.code().httpStatus, ErrorResponse.from(ex.code(), ex.getMessage(), traceId, null));
    } catch (Exception ex) {
      send(exchange, ErrorCode.VALIDATION_FAILED.httpStatus, ErrorResponse.from(ErrorCode.VALIDATION_FAILED, traceId));
    }
  }

  private void handleActionCommands(HttpExchange exchange) throws IOException {
    String traceId = traceId(exchange);
    if (missingTrace(exchange)) {
      send(exchange, ErrorCode.VALIDATION_FAILED.httpStatus, ErrorResponse.from(ErrorCode.VALIDATION_FAILED, traceId));
      return;
    }
    try {
      String path = exchange.getRequestURI().getPath();
      if (!path.startsWith("/api/action-commands/") || !"GET".equals(exchange.getRequestMethod())) {
        send(exchange, 404, ErrorResponse.from(ErrorCode.RESOURCE_NOT_FOUND, traceId));
        return;
      }
      RequestContext context = authenticate(exchange, traceId, List.of("ticket:followup:request", "approval:decide"));
      String actionCommandId = path.substring("/api/action-commands/".length());
      send(exchange, 200, workflowService.getActionCommand(context, actionCommandId));
    } catch (WorkflowException ex) {
      send(exchange, ex.code().httpStatus, ErrorResponse.from(ex.code(), ex.getMessage(), traceId, null));
    }
  }

  private RequestContext authenticate(HttpExchange exchange, String traceId, List<String> requiredPermissions) {
    return authService.authenticate(
        exchange.getRequestHeaders().getFirst("Authorization"),
        exchange.getRequestHeaders().getFirst("X-Tenant-Id"),
        traceId,
        requiredPermissions);
  }

  private void handleHealth(HttpExchange exchange) throws IOException {
    send(exchange, 200, Map.of("status", "UP", "checked_at", Instant.now().toString()));
  }

  private static boolean missingTrace(HttpExchange exchange) {
    String header = exchange.getRequestHeaders().getFirst("X-Trace-Id");
    return header == null || header.isBlank();
  }

  private static String traceId(HttpExchange exchange) {
    String header = exchange.getRequestHeaders().getFirst("X-Trace-Id");
    if (header == null || header.isBlank()) {
      return UUID.randomUUID().toString();
    }
    return header;
  }

  private void send(HttpExchange exchange, int status, Object body) throws IOException {
    byte[] payload = mapper.writeValueAsBytes(body);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(status, payload.length);
    exchange.getResponseBody().write(payload);
    exchange.close();
  }
}
