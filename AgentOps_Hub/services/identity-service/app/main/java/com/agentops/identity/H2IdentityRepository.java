package com.agentops.identity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class H2IdentityRepository {
  private final String jdbcUrl;

  H2IdentityRepository(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
  }

  void initialize() {
    try (Connection connection = open()) {
      String schema;
      try (var stream = H2IdentityRepository.class.getResourceAsStream("/schema.sql")) {
        if (stream == null) {
          throw new IllegalStateException("schema.sql not found.");
        }
        schema = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      }
      for (String statement : schema.split(";")) {
        if (!statement.isBlank()) {
          connection.createStatement().execute(statement);
        }
      }
    } catch (SQLException | IOException ex) {
      throw new IdentityException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  Optional<UserRecord> findUserForLogin(String tenantId, String username) {
    return findUser(
        "SELECT id, tenant_id, username, password_hash, enabled FROM sys_user WHERE tenant_id = ? AND username = ?",
        tenantId,
        username);
  }

  Optional<UserRecord> findUserById(String tenantId, String userId) {
    return findUser(
        "SELECT id, tenant_id, username, password_hash, enabled FROM sys_user WHERE tenant_id = ? AND id = ?",
        tenantId,
        userId);
  }

  boolean tenantEnabled(String tenantId) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement("SELECT enabled FROM tenant WHERE id = ?")) {
      statement.setString(1, tenantId);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() && result.getBoolean("enabled");
      }
    } catch (SQLException ex) {
      throw new IdentityException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  List<String> rolesForUser(String userId) {
    return listCodes(
        """
        SELECT r.code
        FROM sys_role r
        JOIN sys_user_role ur ON ur.role_id = r.id
        WHERE ur.user_id = ?
        ORDER BY r.code
        """,
        userId);
  }

  List<String> permissionsForUser(String userId) {
    return listCodes(
        """
        SELECT DISTINCT p.code
        FROM sys_permission p
        JOIN sys_role_permission rp ON rp.permission_id = p.id
        JOIN sys_user_role ur ON ur.role_id = rp.role_id
        WHERE ur.user_id = ?
        ORDER BY p.code
        """,
        userId);
  }

  void audit(
      String tenantId, String userId, String action, boolean success, ErrorCode errorCode, String traceId) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                INSERT INTO identity_audit_log
                  (id, tenant_id, user_id, action, success, error_code, trace_id, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
      statement.setString(1, UUID.randomUUID().toString());
      statement.setString(2, tenantId);
      statement.setString(3, userId);
      statement.setString(4, action);
      statement.setBoolean(5, success);
      statement.setString(6, errorCode == null ? null : errorCode.name());
      statement.setString(7, traceId);
      statement.setObject(8, Instant.now());
      statement.executeUpdate();
    } catch (SQLException ex) {
      throw new IdentityException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  int auditCount(String action, boolean success) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT COUNT(*) FROM identity_audit_log WHERE action = ? AND success = ?")) {
      statement.setString(1, action);
      statement.setBoolean(2, success);
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        return result.getInt(1);
      }
    } catch (SQLException ex) {
      throw new IdentityException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  private Optional<UserRecord> findUser(String sql, String first, String second) {
    try (Connection connection = open();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, first);
      statement.setString(2, second);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return Optional.empty();
        }
        return Optional.of(
            new UserRecord(
                result.getString("id"),
                result.getString("tenant_id"),
                result.getString("username"),
                result.getString("password_hash"),
                result.getBoolean("enabled")));
      }
    } catch (SQLException ex) {
      throw new IdentityException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  private List<String> listCodes(String sql, String userId) {
    List<String> codes = new ArrayList<>();
    try (Connection connection = open();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, userId);
      try (ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          codes.add(result.getString(1));
        }
      }
      return codes;
    } catch (SQLException ex) {
      throw new IdentityException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  private Connection open() throws SQLException {
    return DriverManager.getConnection(jdbcUrl);
  }
}
