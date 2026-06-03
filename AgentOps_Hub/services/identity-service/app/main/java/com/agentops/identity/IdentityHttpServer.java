package com.agentops.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

final class IdentityHttpServer {
  private final AuthService authService;
  private final ObjectMapper mapper;
  private HttpServer server;

  IdentityHttpServer(AuthService authService) {
    this.authService = authService;
    this.mapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  int start(int port) {
    try {
      server = HttpServer.create(new InetSocketAddress(port), 0);
      server.createContext("/api/auth/login", this::handleLogin);
      server.createContext("/api/auth/me", this::handleCurrentUser);
      server.createContext("/actuator/health", this::handleHealth);
      server.start();
      return server.getAddress().getPort();
    } catch (IOException ex) {
      throw new IdentityException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  private void handleLogin(HttpExchange exchange) throws IOException {
    String traceId = traceId(exchange);
    if (!"POST".equals(exchange.getRequestMethod())) {
      send(exchange, 405, ErrorResponse.from(ErrorCode.AUTH_FORBIDDEN, traceId));
      return;
    }
    try {
      LoginRequest request = mapper.readValue(exchange.getRequestBody(), LoginRequest.class);
      send(exchange, 200, authService.login(request, traceId));
    } catch (IdentityException ex) {
      send(exchange, ex.code().httpStatus, ErrorResponse.from(ex.code(), traceId));
    } catch (Exception ex) {
      send(exchange, ErrorCode.VALIDATION_FAILED.httpStatus, ErrorResponse.from(ErrorCode.VALIDATION_FAILED, traceId));
    }
  }

  private void handleCurrentUser(HttpExchange exchange) throws IOException {
    String traceId = traceId(exchange);
    if (!"GET".equals(exchange.getRequestMethod())) {
      send(exchange, 405, ErrorResponse.from(ErrorCode.AUTH_FORBIDDEN, traceId));
      return;
    }
    try {
      String authorization = exchange.getRequestHeaders().getFirst("Authorization");
      String tenantId = exchange.getRequestHeaders().getFirst("X-Tenant-Id");
      send(exchange, 200, authService.currentUser(authorization, tenantId, traceId));
    } catch (IdentityException ex) {
      send(exchange, ex.code().httpStatus, ErrorResponse.from(ex.code(), traceId));
    }
  }

  private void handleHealth(HttpExchange exchange) throws IOException {
    send(exchange, 200, Map.of("status", "UP", "checked_at", Instant.now().toString()));
  }

  private String traceId(HttpExchange exchange) {
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
