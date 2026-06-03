package com.agentops.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

final class WorkflowHttpServerTest {
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private static final String SECRET = "workflow-local-test-secret-at-least-thirty-two";
  private WorkflowHttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  void createApproveAndReadActionCommandFlow() throws Exception {
    int port = startServer();
    HttpClient client = HttpClient.newHttpClient();
    String requesterToken = token("tenant_demo", "user_demo", List.of("ticket:followup:request"));
    String approverToken = token("tenant_demo", "user_supervisor", List.of("approval:decide"));

    HttpResponse<String> createResponse =
        client.send(
            requestBuilder(port, "/api/approvals", requesterToken, "tenant_demo")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        """
                        {
                          "source_type": "ai_task",
                          "source_task_id": "task_demo",
                          "target_type": "ticket",
                          "target_id": "ticket_demo",
                          "action_type": "ticket_create_followup",
                          "requested_by": "user_demo",
                          "risk_reason": "Urgent complaint needs human-approved follow-up.",
                          "idempotency_key": "followup:ticket_demo:task_demo",
                          "citations": [
                            {
                              "document_id": "doc_policy",
                              "chunk_id": "chunk_001",
                              "source_title": "Policy",
                              "source_uri": "file://policy#chunk_001"
                            }
                          ],
                          "action_input": {
                            "ticket_id": "ticket_demo",
                            "title": "Supervisor follow-up",
                            "reason": "Human-approved follow-up task is required.",
                            "due_at": "2026-06-03T12:00:00Z",
                            "approval_policy": "required"
                          }
                        }
                        """))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(201, createResponse.statusCode());
    JsonNode created = MAPPER.readTree(createResponse.body());
    assertEquals("pending", created.get("status").asText());
    assertEquals("tenant_demo", created.get("tenant_id").asText());
    assertTrue(created.get("approval_instance_id").asText().startsWith("approval_"));

    String approvalId = created.get("approval_instance_id").asText();
    HttpResponse<String> approveResponse =
        client.send(
            requestBuilder(port, "/api/approvals/" + approvalId + "/approve", approverToken, "tenant_demo")
                .POST(HttpRequest.BodyPublishers.ofString("{\"reason\":\"Supervisor approved the follow-up.\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(200, approveResponse.statusCode());
    JsonNode approved = MAPPER.readTree(approveResponse.body());
    assertEquals("approved", approved.get("status").asText());
    assertTrue(approved.hasNonNull("action_command_id"));

    String actionCommandId = approved.get("action_command_id").asText();
    HttpResponse<String> actionResponse =
        client.send(
            requestBuilder(port, "/api/action-commands/" + actionCommandId, requesterToken, "tenant_demo").GET().build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(200, actionResponse.statusCode());
    JsonNode action = MAPPER.readTree(actionResponse.body());
    assertEquals("success", action.get("status").asText());
    assertEquals("ticket_demo", action.get("result_payload").get("ticket_id").asText());
    assertTrue(action.get("result_payload").get("followup_task_ref").asText().startsWith("followup_ref_"));

    HttpResponse<String> approveAgain =
        client.send(
            requestBuilder(port, "/api/approvals/" + approvalId + "/approve", approverToken, "tenant_demo")
                .POST(HttpRequest.BodyPublishers.ofString("{\"reason\":\"Repeated approve should be idempotent.\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, approveAgain.statusCode());
    JsonNode approvedAgain = MAPPER.readTree(approveAgain.body());
    assertEquals(actionCommandId, approvedAgain.get("action_command_id").asText());
  }

  @Test
  void idempotentCreateReusesApprovalAndCrossTenantDoesNotReveal() throws Exception {
    int port = startServer();
    HttpClient client = HttpClient.newHttpClient();
    String tenantAToken = token("tenant_a", "user_a", List.of("ticket:followup:request"));
    String tenantBToken = token("tenant_b", "user_b", List.of("approval:decide"));

    String requestBody =
        """
        {
          "source_type": "ai_task",
          "source_task_id": "task_demo",
          "target_type": "ticket",
          "target_id": "ticket_demo",
          "action_type": "ticket_create_followup",
          "requested_by": "user_a",
          "risk_reason": "Urgent complaint needs human-approved follow-up.",
          "idempotency_key": "followup:ticket_demo:task_demo",
          "action_input": {
            "ticket_id": "ticket_demo",
            "title": "Supervisor follow-up",
            "reason": "Human-approved follow-up task is required.",
            "due_at": "2026-06-03T12:00:00Z",
            "approval_policy": "required"
          }
        }
        """;

    HttpResponse<String> firstCreate =
        client.send(
            requestBuilder(port, "/api/approvals", tenantAToken, "tenant_a")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    HttpResponse<String> secondCreate =
        client.send(
            requestBuilder(port, "/api/approvals", tenantAToken, "tenant_a")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    String approvalId = MAPPER.readTree(firstCreate.body()).get("approval_instance_id").asText();
    assertEquals(approvalId, MAPPER.readTree(secondCreate.body()).get("approval_instance_id").asText());

    HttpResponse<String> conflictingCreate =
        client.send(
            requestBuilder(port, "/api/approvals", tenantAToken, "tenant_a")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        requestBody.replace("Supervisor follow-up", "Supervisor follow-up changed")))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(409, conflictingCreate.statusCode());
    assertEquals("CONFLICT", MAPPER.readTree(conflictingCreate.body()).get("error_code").asText());

    HttpResponse<String> tenantBLookup =
        client.send(
            requestBuilder(port, "/api/approvals/" + approvalId, tenantBToken, "tenant_b").GET().build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(404, tenantBLookup.statusCode());
    JsonNode error = MAPPER.readTree(tenantBLookup.body());
    assertEquals("RESOURCE_NOT_FOUND", error.get("error_code").asText());
    assertFalse(error.has("details"));
  }

  @Test
  void rejectAndCancelDoNotCreateActions() throws Exception {
    int port = startServer();
    HttpClient client = HttpClient.newHttpClient();
    String requesterToken = token("tenant_demo", "user_demo", List.of("ticket:followup:request"));
    String approverToken = token("tenant_demo", "user_supervisor", List.of("approval:decide"));

    String approvalId = createApproval(client, port, requesterToken, "reject-demo", "Supervisor follow-up").get("approval_instance_id").asText();
    HttpResponse<String> rejectResponse =
        client.send(
            requestBuilder(port, "/api/approvals/" + approvalId + "/reject", approverToken, "tenant_demo")
                .POST(HttpRequest.BodyPublishers.ofString("{\"reason\":\"No follow-up task is needed.\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    JsonNode rejected = MAPPER.readTree(rejectResponse.body());
    assertEquals(200, rejectResponse.statusCode());
    assertEquals("rejected", rejected.get("status").asText());
    assertTrue(rejected.get("action_command_id").isNull());

    HttpResponse<String> approveRejected =
        client.send(
            requestBuilder(port, "/api/approvals/" + approvalId + "/approve", approverToken, "tenant_demo")
                .POST(HttpRequest.BodyPublishers.ofString("{\"reason\":\"Retry approve should fail.\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(409, approveRejected.statusCode());

    String cancelApprovalId = createApproval(client, port, requesterToken, "cancel-demo", "Supervisor follow-up").get("approval_instance_id").asText();
    HttpResponse<String> cancelResponse =
        client.send(
            requestBuilder(port, "/api/approvals/" + cancelApprovalId + "/cancel", requesterToken, "tenant_demo")
                .POST(HttpRequest.BodyPublishers.ofString("{\"reason\":\"Requester cancelled the pending request.\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    JsonNode cancelled = MAPPER.readTree(cancelResponse.body());
    assertEquals(200, cancelResponse.statusCode());
    assertEquals("cancelled", cancelled.get("status").asText());
    assertTrue(cancelled.get("action_command_id").isNull());
  }

  @Test
  void actionFailureIsVisible() throws Exception {
    int port = startServer();
    HttpClient client = HttpClient.newHttpClient();
    String requesterToken = token("tenant_demo", "user_demo", List.of("ticket:followup:request"));
    String approverToken = token("tenant_demo", "user_supervisor", List.of("approval:decide"));

    JsonNode created = createApproval(client, port, requesterToken, "failure-demo", "Supervisor follow-up [force-fail]");
    String approvalId = created.get("approval_instance_id").asText();
    HttpResponse<String> approveResponse =
        client.send(
            requestBuilder(port, "/api/approvals/" + approvalId + "/approve", approverToken, "tenant_demo")
                .POST(HttpRequest.BodyPublishers.ofString("{\"reason\":\"Approve even though executor will fail.\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    JsonNode approved = MAPPER.readTree(approveResponse.body());
    String actionCommandId = approved.get("action_command_id").asText();

    HttpResponse<String> actionResponse =
        client.send(
            requestBuilder(port, "/api/action-commands/" + actionCommandId, requesterToken, "tenant_demo").GET().build(),
            HttpResponse.BodyHandlers.ofString());
    JsonNode action = MAPPER.readTree(actionResponse.body());
    assertEquals("failed", action.get("status").asText());
    assertEquals("DOWNSTREAM_UNAVAILABLE", action.get("error").get("error_code").asText());
    assertNull(action.get("result_payload").textValue());
  }

  private JsonNode createApproval(HttpClient client, int port, String token, String suffix, String title) throws Exception {
    HttpResponse<String> response =
        client.send(
            requestBuilder(port, "/api/approvals", token, "tenant_demo")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        """
                        {
                          "source_type": "ai_task",
                          "source_task_id": "task_%s",
                          "target_type": "ticket",
                          "target_id": "ticket_demo",
                          "action_type": "ticket_create_followup",
                          "requested_by": "user_demo",
                          "risk_reason": "Urgent complaint needs human-approved follow-up.",
                          "idempotency_key": "followup:ticket_demo:%s",
                          "action_input": {
                            "ticket_id": "ticket_demo",
                            "title": "%s",
                            "reason": "Human-approved follow-up task is required.",
                            "due_at": "2026-06-03T12:00:00Z",
                            "approval_policy": "required"
                          }
                        }
                        """.formatted(suffix, suffix, title)))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(201, response.statusCode());
    return MAPPER.readTree(response.body());
  }

  private int startServer() {
    H2WorkflowRepository repository =
        new H2WorkflowRepository("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    repository.initialize();
    WorkflowAuthService authService =
        new WorkflowAuthService(new JwtVerifier(SECRET, Clock.fixed(Instant.parse("2026-06-03T08:00:00Z"), ZoneOffset.UTC)));
    WorkflowService service =
        new WorkflowService(repository, Clock.fixed(Instant.parse("2026-06-03T08:00:00Z"), ZoneOffset.UTC));
    server = new WorkflowHttpServer(authService, service);
    return server.start(0);
  }

  private static String token(String tenantId, String userId, List<String> permissions) {
    return TestJwtTokens.issue(SECRET, tenantId, userId, permissions, Instant.parse("2026-06-03T09:00:00Z"));
  }

  private static HttpRequest.Builder requestBuilder(int port, String path, String token, String tenantId) {
    return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + token)
        .header("X-Tenant-Id", tenantId)
        .header("X-Trace-Id", "trace-workflow-http-test");
  }
}
