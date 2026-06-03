package com.agentops.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class IdentityHttpServerTest {
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private IdentityHttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  void loginAndCurrentUserHttpFlowMatchesContractShape() throws Exception {
    String loginSecret = UUID.randomUUID().toString();
    PasswordHasher passwordHasher = new PasswordHasher();
    IdentityConfig config =
        IdentityConfig.forTest(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
            "local-test-jwt-secret-at-least-thirty-two-chars",
            Duration.ofMinutes(30));
    H2IdentityRepository repository = new H2IdentityRepository(config.dbUrl());
    repository.initialize();
    TestIdentityData.seedDemoData(config.dbUrl(), passwordHasher, loginSecret);
    AuthService authService =
        new AuthService(repository, passwordHasher, new JwtService(config, Clock.systemUTC()));
    server = new IdentityHttpServer(authService);
    int port = server.start(0);
    HttpClient client = HttpClient.newHttpClient();

    HttpRequest loginRequest =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/login"))
            .header("Content-Type", "application/json")
            .header("X-Trace-Id", "trace-http-login")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    MAPPER.writeValueAsString(
                        new LoginRequest("tenant_demo", "demo.user", loginSecret))))
            .build();
    HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, loginResponse.statusCode());
    JsonNode loginJson = MAPPER.readTree(loginResponse.body());
    assertEquals("tenant_demo", loginJson.get("tenant_id").asText());
    assertTrue(loginJson.hasNonNull("access_token"));

    HttpRequest meRequest =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/me"))
            .header("Authorization", "Bearer " + loginJson.get("access_token").asText())
            .header("X-Tenant-Id", "tenant_demo")
            .header("X-Trace-Id", "trace-http-me")
            .GET()
            .build();
    HttpResponse<String> meResponse = client.send(meRequest, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, meResponse.statusCode());
    JsonNode meJson = MAPPER.readTree(meResponse.body());
    assertEquals("user_demo", meJson.get("user_id").asText());
    assertTrue(meJson.get("roles").isArray());
    assertTrue(meJson.get("permissions").isArray());
  }

  @Test
  void currentUserHttpFailureUsesUnifiedErrorShape() throws Exception {
    PasswordHasher passwordHasher = new PasswordHasher();
    IdentityConfig config =
        IdentityConfig.forTest(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
            "local-test-jwt-secret-at-least-thirty-two-chars",
            Duration.ofMinutes(30));
    H2IdentityRepository repository = new H2IdentityRepository(config.dbUrl());
    repository.initialize();
    server =
        new IdentityHttpServer(
            new AuthService(repository, passwordHasher, new JwtService(config, Clock.systemUTC())));
    int port = server.start(0);

    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/me"))
            .header("X-Trace-Id", "trace-http-tenant")
            .GET()
            .build();
    HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode json = MAPPER.readTree(response.body());

    assertEquals(400, response.statusCode());
    assertEquals("TENANT_REQUIRED", json.get("error_code").asText());
    assertEquals("trace-http-tenant", json.get("trace_id").asText());
    assertTrue(json.has("retryable"));
  }

  @Test
  void authMethodMismatchStillUsesUnifiedErrorShape() throws Exception {
    PasswordHasher passwordHasher = new PasswordHasher();
    IdentityConfig config =
        IdentityConfig.forTest(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
            "local-test-jwt-secret-at-least-thirty-two-chars",
            Duration.ofMinutes(30));
    H2IdentityRepository repository = new H2IdentityRepository(config.dbUrl());
    repository.initialize();
    server =
        new IdentityHttpServer(
            new AuthService(repository, passwordHasher, new JwtService(config, Clock.systemUTC())));
    int port = server.start(0);

    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/login"))
            .header("X-Trace-Id", "trace-http-method")
            .GET()
            .build();
    HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode json = MAPPER.readTree(response.body());

    assertEquals(405, response.statusCode());
    assertTrue(json.hasNonNull("error_code"));
    assertEquals("trace-http-method", json.get("trace_id").asText());
    assertTrue(json.has("retryable"));
  }
}
