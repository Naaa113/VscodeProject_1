package com.agentops.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class TicketHttpServerTest {
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private static final String SECRET = "local-test-jwt-secret-at-least-thirty-two-chars";
  private TicketHttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  void createListAndGetHttpFlowMatchesContractShape() throws Exception {
    int port = startServer();
    HttpClient client = HttpClient.newHttpClient();
    String token = token("tenant_demo", "user_demo", List.of("ticket:read", "ticket:create"));

    HttpResponse<String> createResponse =
        client.send(
            requestBuilder(port, "/api/tickets", token, "tenant_demo")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        """
                        {
                          "title": "Delayed response",
                          "description": "Customer reported a delayed response.",
                          "priority": "normal",
                          "category": "complaint",
                          "customer": {
                            "display_name": "Example Customer",
                            "external_ref": "crm-1001"
                          },
                          "sla_due_at": "2026-06-02T08:00:00Z"
                        }
                        """))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(201, createResponse.statusCode());
    JsonNode created = MAPPER.readTree(createResponse.body());
    assertEquals("tenant_demo", created.get("tenant_id").asText());
    assertEquals("open", created.get("status").asText());
    assertEquals("complaint", created.get("category").asText());
    assertTrue(created.hasNonNull("customer"));

    HttpResponse<String> listResponse =
        client.send(
            requestBuilder(port, "/api/tickets?status=open&priority=normal&category=complaint&query=delayed", token, "tenant_demo")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(200, listResponse.statusCode());
    JsonNode list = MAPPER.readTree(listResponse.body());
    assertEquals(1, list.get("page").get("total").asInt());
    assertEquals("complaint", list.get("items").get(0).get("category").asText());

    String ticketId = created.get("ticket_id").asText();
    HttpResponse<String> getResponse =
        client.send(
            requestBuilder(port, "/api/tickets/" + ticketId, token, "tenant_demo").GET().build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(200, getResponse.statusCode());
    JsonNode detail = MAPPER.readTree(getResponse.body());
    assertEquals(ticketId, detail.get("ticket_id").asText());
    assertTrue(detail.hasNonNull("description"));
  }

  @Test
  void crossTenantDetailDoesNotRevealResourceExistence() throws Exception {
    int port = startServer();
    HttpClient client = HttpClient.newHttpClient();
    String tenantAToken = token("tenant_a", "user_a", List.of("ticket:read", "ticket:create"));
    String tenantBToken = token("tenant_b", "user_b", List.of("ticket:read"));

    HttpResponse<String> createResponse =
        client.send(
            requestBuilder(port, "/api/tickets", tenantAToken, "tenant_a")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        """
                        {
                          "title": "Delayed response",
                          "description": "Customer reported a delayed response.",
                          "priority": "urgent",
                          "category": "complaint",
                          "customer": {"display_name": "Tenant A Customer"}
                        }
                        """))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    String ticketId = MAPPER.readTree(createResponse.body()).get("ticket_id").asText();

    HttpResponse<String> forbiddenLookup =
        client.send(
            requestBuilder(port, "/api/tickets/" + ticketId, tenantBToken, "tenant_b").GET().build(),
            HttpResponse.BodyHandlers.ofString());
    JsonNode error = MAPPER.readTree(forbiddenLookup.body());

    assertEquals(404, forbiddenLookup.statusCode());
    assertEquals("RESOURCE_NOT_FOUND", error.get("error_code").asText());
    assertFalse(error.has("details"));
  }

  @Test
  void missingTraceAndMissingPermissionUseUnifiedErrors() throws Exception {
    int port = startServer();
    HttpClient client = HttpClient.newHttpClient();
    String token = token("tenant_demo", "user_demo", List.of("ticket:read"));

    HttpResponse<String> missingTrace =
        client.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/tickets"))
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-Id", "tenant_demo")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(400, missingTrace.statusCode());
    assertEquals("VALIDATION_FAILED", MAPPER.readTree(missingTrace.body()).get("error_code").asText());

    HttpResponse<String> missingPermission =
        client.send(
            requestBuilder(port, "/api/tickets", token, "tenant_demo")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        """
                        {
                          "title": "Delayed response",
                          "description": "Customer reported a delayed response.",
                          "priority": "normal",
                          "category": "complaint",
                          "customer": {"display_name": "Example Customer"}
                        }
                        """))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(403, missingPermission.statusCode());
    assertEquals("AUTH_FORBIDDEN", MAPPER.readTree(missingPermission.body()).get("error_code").asText());
  }

  private int startServer() {
    H2TicketRepository repository =
        new H2TicketRepository("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    repository.initialize();
    TicketAuthService authService =
        new TicketAuthService(new JwtVerifier(SECRET, Clock.fixed(Instant.parse("2026-06-01T08:00:00Z"), ZoneOffset.UTC)));
    TicketService ticketService =
        new TicketService(repository, Clock.fixed(Instant.parse("2026-06-01T08:00:00Z"), ZoneOffset.UTC));
    server = new TicketHttpServer(authService, ticketService);
    return server.start(0);
  }

  private static String token(String tenantId, String userId, List<String> permissions) {
    return TestJwtTokens.issue(SECRET, tenantId, userId, permissions, Instant.parse("2026-06-01T09:00:00Z"));
  }

  private static HttpRequest.Builder requestBuilder(int port, String path, String token, String tenantId) {
    return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + token)
        .header("X-Tenant-Id", tenantId)
        .header("X-Trace-Id", "trace-http-test");
  }
}
