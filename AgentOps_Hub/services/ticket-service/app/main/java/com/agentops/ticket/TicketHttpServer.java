package com.agentops.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class TicketHttpServer {
  private final TicketAuthService authService;
  private final TicketService ticketService;
  private final ObjectMapper mapper;
  private HttpServer server;

  TicketHttpServer(TicketAuthService authService, TicketService ticketService) {
    this.authService = authService;
    this.ticketService = ticketService;
    this.mapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  int start(int port) {
    try {
      server = HttpServer.create(new InetSocketAddress(port), 0);
      server.createContext("/api/tickets", this::handleTickets);
      server.createContext("/actuator/health", this::handleHealth);
      server.start();
      return server.getAddress().getPort();
    } catch (IOException ex) {
      throw new TicketException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  private void handleTickets(HttpExchange exchange) throws IOException {
    String traceId = traceId(exchange);
    if (missingTrace(exchange)) {
      send(exchange, ErrorCode.VALIDATION_FAILED.httpStatus, ErrorResponse.from(ErrorCode.VALIDATION_FAILED, traceId));
      return;
    }
    try {
      String path = exchange.getRequestURI().getPath();
      if ("/api/tickets".equals(path)) {
        if ("GET".equals(exchange.getRequestMethod())) {
          RequestContext context = authenticate(exchange, traceId, "ticket:read");
          send(exchange, 200, ticketService.list(context, filter(exchange.getRequestURI().getRawQuery())));
          return;
        }
        if ("POST".equals(exchange.getRequestMethod())) {
          RequestContext context = authenticate(exchange, traceId, "ticket:create");
          CreateTicketRequest request = mapper.readValue(exchange.getRequestBody(), CreateTicketRequest.class);
          send(exchange, 201, ticketService.create(context, request));
          return;
        }
        send(exchange, 405, ErrorResponse.from(ErrorCode.AUTH_FORBIDDEN, traceId));
        return;
      }
      if (path.startsWith("/api/tickets/") && "GET".equals(exchange.getRequestMethod())) {
        RequestContext context = authenticate(exchange, traceId, "ticket:read");
        String ticketId = decode(path.substring("/api/tickets/".length()));
        send(exchange, 200, ticketService.get(context, ticketId));
        return;
      }
      send(exchange, 404, ErrorResponse.from(ErrorCode.RESOURCE_NOT_FOUND, traceId));
    } catch (TicketException ex) {
      send(exchange, ex.code().httpStatus, ErrorResponse.from(ex.code(), traceId));
    } catch (Exception ex) {
      send(exchange, ErrorCode.VALIDATION_FAILED.httpStatus, ErrorResponse.from(ErrorCode.VALIDATION_FAILED, traceId));
    }
  }

  private RequestContext authenticate(HttpExchange exchange, String traceId, String permission) {
    return authService.authenticate(
        exchange.getRequestHeaders().getFirst("Authorization"),
        exchange.getRequestHeaders().getFirst("X-Tenant-Id"),
        traceId,
        permission);
  }

  private TicketSearchFilter filter(String rawQuery) {
    Map<String, String> query = queryParams(rawQuery);
    return TicketService.filter(
        enumValue(TicketStatus.class, query.get("status")),
        enumValue(TicketPriority.class, query.get("priority")),
        query.get("category"),
        query.get("customer_id"),
        instant(query.get("created_from")),
        instant(query.get("created_to")),
        instant(query.get("sla_due_before")),
        query.get("query"),
        integer(query.get("page")),
        integer(query.get("page_size")));
  }

  private static Map<String, String> queryParams(String rawQuery) {
    Map<String, String> result = new HashMap<>();
    if (rawQuery == null || rawQuery.isBlank()) {
      return result;
    }
    for (String pair : rawQuery.split("&")) {
      if (pair.isBlank()) {
        continue;
      }
      String[] parts = pair.split("=", 2);
      result.put(decode(parts[0]), parts.length == 2 ? decode(parts[1]) : "");
    }
    return result;
  }

  private static <E extends Enum<E>> E enumValue(Class<E> type, String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(type, raw);
    } catch (IllegalArgumentException ex) {
      throw new TicketException(ErrorCode.VALIDATION_FAILED, ex);
    }
  }

  private static Instant instant(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(raw);
    } catch (RuntimeException ex) {
      throw new TicketException(ErrorCode.VALIDATION_FAILED, ex);
    }
  }

  private static Integer integer(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(raw);
    } catch (NumberFormatException ex) {
      throw new TicketException(ErrorCode.VALIDATION_FAILED, ex);
    }
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

  private static String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  private void send(HttpExchange exchange, int status, Object body) throws IOException {
    byte[] payload = mapper.writeValueAsBytes(body);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(status, payload.length);
    exchange.getResponseBody().write(payload);
    exchange.close();
  }
}
