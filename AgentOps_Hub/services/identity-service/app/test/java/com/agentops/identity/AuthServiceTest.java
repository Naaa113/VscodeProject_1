package com.agentops.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class AuthServiceTest {
  @Test
  void loginIssuesTokenAndCurrentUserReturnsTenantRolesAndPermissions() {
    Fixture fixture = Fixture.create(Duration.ofMinutes(30));
    LoginResponse login =
        fixture.service.login(
            new LoginRequest("tenant_demo", "demo.user", fixture.loginSecret), "trace-login-success");

    assertEquals("tenant_demo", login.tenant_id());
    assertEquals("user_demo", login.user_id());
    assertNotNull(login.access_token());
    assertFalse(login.access_token().contains("password"));

    CurrentUserResponse current =
        fixture.service.currentUser(
            "Bearer " + login.access_token(), "tenant_demo", "trace-current-user-success");

    assertEquals("demo.user", current.username());
    assertEquals("tenant_demo", current.tenant_id());
    assertEquals("user_demo", current.user_id());
    assertEquals(1, current.roles().size());
    assertTrue(current.permissions().contains("auth:me"));
    assertTrue(current.permissions().contains("ticket:read"));
    assertEquals(1, fixture.repository.auditCount("LOGIN", true));
    assertEquals(1, fixture.repository.auditCount("CURRENT_USER", true));
  }

  @Test
  void loginFailureDoesNotRevealMissingTenantUserPasswordOrDisabledStatus() {
    Fixture fixture = Fixture.create(Duration.ofMinutes(30));

    assertCode(
        ErrorCode.AUTH_INVALID_CREDENTIALS,
        () -> fixture.service.login(new LoginRequest("missing", "demo.user", fixture.loginSecret), "trace-a"));
    assertCode(
        ErrorCode.AUTH_INVALID_CREDENTIALS,
        () ->
            fixture.service.login(
                new LoginRequest("tenant_demo", "missing.user", fixture.loginSecret), "trace-b"));
    assertCode(
        ErrorCode.AUTH_INVALID_CREDENTIALS,
        () ->
            fixture.service.login(
                new LoginRequest("tenant_demo", "demo.user", UUID.randomUUID().toString()), "trace-c"));
    assertCode(
        ErrorCode.AUTH_INVALID_CREDENTIALS,
        () ->
            fixture.service.login(
                new LoginRequest("tenant_demo", "disabled.user", fixture.loginSecret), "trace-d"));
    assertEquals(4, fixture.repository.auditCount("LOGIN", false));
  }

  @Test
  void loginValidationAndTenantRequiredUseContractErrors() {
    Fixture fixture = Fixture.create(Duration.ofMinutes(30));

    assertCode(
        ErrorCode.TENANT_REQUIRED,
        () -> fixture.service.login(new LoginRequest("", "demo.user", fixture.loginSecret), "trace-tenant"));
    assertCode(
        ErrorCode.VALIDATION_FAILED,
        () -> fixture.service.login(new LoginRequest("tenant_demo", "", fixture.loginSecret), "trace-validation"));
    assertEquals(2, fixture.repository.auditCount("LOGIN", false));
  }

  @Test
  void loginTokenIssueFailureIsAuditedAsInternalError() {
    Fixture fixture = Fixture.create(null);

    assertCode(
        ErrorCode.INTERNAL_ERROR,
        () ->
            fixture.service.login(
                new LoginRequest("tenant_demo", "demo.user", fixture.loginSecret), "trace-token-failure"));
    assertEquals(1, fixture.repository.auditCount("LOGIN", false));
  }

  @Test
  void currentUserRejectsMissingTenantTenantMismatchExpiredTokenAndDisabledUser() {
    Fixture fixture = Fixture.create(Duration.ofMinutes(30));
    LoginResponse login =
        fixture.service.login(new LoginRequest("tenant_demo", "demo.user", fixture.loginSecret), "trace-login");

    assertCode(
        ErrorCode.TENANT_REQUIRED,
        () -> fixture.service.currentUser("Bearer " + login.access_token(), "", "trace-missing-tenant"));
    assertCode(
        ErrorCode.AUTH_FORBIDDEN,
        () ->
            fixture.service.currentUser(
                "Bearer " + login.access_token(), "tenant_other", "trace-tenant-mismatch"));

    Fixture expired = Fixture.create(Duration.ofSeconds(-1));
    LoginResponse expiredLogin =
        expired.service.login(
            new LoginRequest("tenant_demo", "demo.user", expired.loginSecret), "trace-expired-login");
    assertCode(
        ErrorCode.AUTH_TOKEN_EXPIRED,
        () ->
            expired.service.currentUser(
                "Bearer " + expiredLogin.access_token(), "tenant_demo", "trace-expired"));

    String disabledToken =
        fixture.jwtService
            .issue(
                new UserRecord("user_disabled", "tenant_demo", "disabled.user", "not-used", true),
                java.util.List.of(),
                java.util.List.of())
            .accessToken();
    assertCode(
        ErrorCode.AUTH_FORBIDDEN,
        () -> fixture.service.currentUser("Bearer " + disabledToken, "tenant_demo", "trace-disabled"));
  }

  private static void assertCode(ErrorCode expected, ThrowingAction action) {
    IdentityException exception = assertThrows(IdentityException.class, action::run);
    assertEquals(expected, exception.code());
  }

  private interface ThrowingAction {
    void run();
  }

  private record Fixture(
      AuthService service,
      H2IdentityRepository repository,
      JwtService jwtService,
      String loginSecret) {
    static Fixture create(Duration tokenTtl) {
      String loginSecret = UUID.randomUUID().toString();
      PasswordHasher passwordHasher = new PasswordHasher();
      IdentityConfig config =
          IdentityConfig.forTest(
              "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
              "local-test-jwt-secret-at-least-thirty-two-chars",
              tokenTtl);
      H2IdentityRepository repository = new H2IdentityRepository(config.dbUrl());
      repository.initialize();
      TestIdentityData.seedDemoData(config.dbUrl(), passwordHasher, loginSecret);
      JwtService jwtService =
          new JwtService(
              config, Clock.fixed(Instant.parse("2026-05-31T00:00:00Z"), ZoneOffset.UTC));
      return new Fixture(new AuthService(repository, passwordHasher, jwtService), repository, jwtService, loginSecret);
    }
  }
}
