package com.agentops.identity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

final class TestIdentityData {
  private TestIdentityData() {}

  static void seedDemoData(String jdbcUrl, PasswordHasher passwordHasher, String loginSecret) {
    Instant now = Instant.now();
    String activeHash = passwordHasher.hash(loginSecret.toCharArray());
    String disabledHash = passwordHasher.hash(UUID.randomUUID().toString().toCharArray());
    try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
      upsertTenant(connection, "tenant_demo", "Demo Tenant", true, now);
      upsertTenant(connection, "tenant_other", "Other Tenant", true, now);
      upsertUser(connection, "user_demo", "tenant_demo", "demo.user", activeHash, true, now);
      upsertUser(connection, "user_disabled", "tenant_demo", "disabled.user", disabledHash, false, now);
      upsertUser(connection, "user_other", "tenant_other", "demo.user", activeHash, true, now);
      upsertRole(connection, "role_ops", "tenant_demo", "ops", "Operator", now);
      upsertRole(connection, "role_other", "tenant_other", "ops", "Operator", now);
      upsertPermission(connection, "perm_ticket_read", "ticket:read", "Read tickets", now);
      upsertPermission(connection, "perm_auth_me", "auth:me", "Read current user", now);
      linkUserRole(connection, "user_demo", "role_ops");
      linkUserRole(connection, "user_other", "role_other");
      linkRolePermission(connection, "role_ops", "perm_ticket_read");
      linkRolePermission(connection, "role_ops", "perm_auth_me");
      linkRolePermission(connection, "role_other", "perm_auth_me");
    } catch (SQLException ex) {
      throw new IdentityException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  private static void upsertTenant(Connection connection, String id, String name, boolean enabled, Instant now)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("MERGE INTO tenant KEY(id) VALUES (?, ?, ?, ?, NULL)")) {
      statement.setString(1, id);
      statement.setString(2, name);
      statement.setBoolean(3, enabled);
      statement.setObject(4, now);
      statement.executeUpdate();
    }
  }

  private static void upsertUser(
      Connection connection,
      String id,
      String tenantId,
      String username,
      String passwordHash,
      boolean enabled,
      Instant now)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "MERGE INTO sys_user KEY(id) VALUES (?, ?, ?, ?, ?, ?, NULL)")) {
      statement.setString(1, id);
      statement.setString(2, tenantId);
      statement.setString(3, username);
      statement.setString(4, passwordHash);
      statement.setBoolean(5, enabled);
      statement.setObject(6, now);
      statement.executeUpdate();
    }
  }

  private static void upsertRole(Connection connection, String id, String tenantId, String code, String name, Instant now)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("MERGE INTO sys_role KEY(id) VALUES (?, ?, ?, ?, ?)")) {
      statement.setString(1, id);
      statement.setString(2, tenantId);
      statement.setString(3, code);
      statement.setString(4, name);
      statement.setObject(5, now);
      statement.executeUpdate();
    }
  }

  private static void upsertPermission(Connection connection, String id, String code, String name, Instant now)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("MERGE INTO sys_permission KEY(id) VALUES (?, ?, ?, ?)")) {
      statement.setString(1, id);
      statement.setString(2, code);
      statement.setString(3, name);
      statement.setObject(4, now);
      statement.executeUpdate();
    }
  }

  private static void linkUserRole(Connection connection, String userId, String roleId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("MERGE INTO sys_user_role KEY(user_id, role_id) VALUES (?, ?)")) {
      statement.setString(1, userId);
      statement.setString(2, roleId);
      statement.executeUpdate();
    }
  }

  private static void linkRolePermission(Connection connection, String roleId, String permissionId)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "MERGE INTO sys_role_permission KEY(role_id, permission_id) VALUES (?, ?)")) {
      statement.setString(1, roleId);
      statement.setString(2, permissionId);
      statement.executeUpdate();
    }
  }
}
