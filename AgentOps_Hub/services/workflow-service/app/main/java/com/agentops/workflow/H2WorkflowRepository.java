package com.agentops.workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

final class H2WorkflowRepository {
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final String jdbcUrl;

  H2WorkflowRepository(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
  }

  void initialize() {
    try (Connection connection = open()) {
      String schema;
      try (var stream = H2WorkflowRepository.class.getResourceAsStream("/schema.sql")) {
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
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  Optional<ApprovalInstanceRecord> findApproval(String tenantId, String approvalId) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                SELECT *
                FROM approval_instance
                WHERE tenant_id = ? AND id = ?
                """)) {
      statement.setString(1, tenantId);
      statement.setString(2, approvalId);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return Optional.empty();
        }
        return Optional.of(mapApproval(result));
      }
    } catch (SQLException ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  Optional<ApprovalInstanceRecord> findApprovalByIdempotency(
      String tenantId, ActionType actionType, String idempotencyKey) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                SELECT *
                FROM approval_instance
                WHERE tenant_id = ? AND action_type = ? AND idempotency_key = ?
                """)) {
      statement.setString(1, tenantId);
      statement.setString(2, actionType.name());
      statement.setString(3, idempotencyKey);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return Optional.empty();
        }
        return Optional.of(mapApproval(result));
      }
    } catch (SQLException ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  ApprovalInstanceRecord createApproval(ApprovalInstanceRecord record) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                INSERT INTO approval_instance
                  (id, tenant_id, status, requested_by, source_type, source_id, target_type, target_id,
                   action_type, risk_reason, idempotency_key, request_fingerprint, action_input_json,
                   citations_json, action_command_id, trace_id, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
      statement.setString(1, record.id());
      statement.setString(2, record.tenantId());
      statement.setString(3, record.status().name());
      statement.setString(4, record.requestedBy());
      statement.setString(5, record.sourceType());
      statement.setString(6, record.sourceId());
      statement.setString(7, record.targetType());
      statement.setString(8, record.targetId());
      statement.setString(9, record.actionType().name());
      statement.setString(10, record.riskReason());
      statement.setString(11, record.idempotencyKey());
      statement.setString(12, record.requestFingerprint());
      statement.setString(13, writeJson(record.actionInput()));
      statement.setString(14, writeJson(record.citations()));
      statement.setString(15, record.actionCommandId());
      statement.setString(16, record.traceId());
      statement.setObject(17, record.expiresAt());
      statement.setObject(18, record.createdAt());
      statement.setObject(19, record.updatedAt());
      statement.executeUpdate();
      return record;
    } catch (SQLException ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  ApprovalInstanceRecord updateApproval(
      String tenantId, String approvalId, ApprovalStatus status, String actionCommandId, Instant updatedAt) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                UPDATE approval_instance
                SET status = ?, action_command_id = ?, updated_at = ?
                WHERE tenant_id = ? AND id = ?
                """)) {
      statement.setString(1, status.name());
      statement.setString(2, actionCommandId);
      statement.setObject(3, updatedAt);
      statement.setString(4, tenantId);
      statement.setString(5, approvalId);
      if (statement.executeUpdate() != 1) {
        throw new WorkflowException(ErrorCode.RESOURCE_NOT_FOUND);
      }
      return findApproval(tenantId, approvalId).orElseThrow(() -> new WorkflowException(ErrorCode.RESOURCE_NOT_FOUND));
    } catch (SQLException ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  ApprovalRecordRecord createApprovalRecord(ApprovalRecordRecord record) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                INSERT INTO approval_record
                  (id, approval_instance_id, tenant_id, decision, decided_by, reason, action_summary, trace_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
      statement.setString(1, record.id());
      statement.setString(2, record.approvalInstanceId());
      statement.setString(3, record.tenantId());
      statement.setString(4, record.decision().name());
      statement.setString(5, record.decidedBy());
      statement.setString(6, record.reason());
      statement.setString(7, record.actionSummary());
      statement.setString(8, record.traceId());
      statement.setObject(9, record.createdAt());
      statement.executeUpdate();
      return record;
    } catch (SQLException ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  Optional<ApprovalRecordRecord> findLatestApprovalRecord(String approvalId) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                SELECT *
                FROM approval_record
                WHERE approval_instance_id = ?
                ORDER BY created_at DESC
                LIMIT 1
                """)) {
      statement.setString(1, approvalId);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return Optional.empty();
        }
        return Optional.of(mapApprovalRecord(result));
      }
    } catch (SQLException ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  Optional<ActionCommandRecord> findActionCommand(String tenantId, String actionCommandId) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                SELECT *
                FROM action_command
                WHERE tenant_id = ? AND id = ?
                """)) {
      statement.setString(1, tenantId);
      statement.setString(2, actionCommandId);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return Optional.empty();
        }
        return Optional.of(mapActionCommand(result));
      }
    } catch (SQLException ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  Optional<ActionCommandRecord> findActionCommandByApproval(String tenantId, String approvalId) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                SELECT *
                FROM action_command
                WHERE tenant_id = ? AND approval_instance_id = ?
                """)) {
      statement.setString(1, tenantId);
      statement.setString(2, approvalId);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return Optional.empty();
        }
        return Optional.of(mapActionCommand(result));
      }
    } catch (SQLException ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  ActionCommandRecord createActionCommand(ActionCommandRecord record) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                INSERT INTO action_command
                  (id, approval_instance_id, tenant_id, action_type, target_type, target_id, idempotency_key,
                   status, created_by, trace_id, created_at, executed_at, result_payload_json, error_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
      statement.setString(1, record.id());
      statement.setString(2, record.approvalInstanceId());
      statement.setString(3, record.tenantId());
      statement.setString(4, record.actionType().name());
      statement.setString(5, record.targetType());
      statement.setString(6, record.targetId());
      statement.setString(7, record.idempotencyKey());
      statement.setString(8, record.status().name());
      statement.setString(9, record.createdBy());
      statement.setString(10, record.traceId());
      statement.setObject(11, record.createdAt());
      statement.setObject(12, record.executedAt());
      statement.setString(13, writeJson(record.resultPayload()));
      statement.setString(14, writeJson(record.error()));
      statement.executeUpdate();
      return record;
    } catch (SQLException ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  ActionCommandRecord updateActionCommandResult(
      String tenantId,
      String actionCommandId,
      ActionCommandStatus status,
      Instant executedAt,
      TicketFollowupActionOutput resultPayload,
      ErrorResponse error) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                UPDATE action_command
                SET status = ?, executed_at = ?, result_payload_json = ?, error_json = ?
                WHERE tenant_id = ? AND id = ?
                """)) {
      statement.setString(1, status.name());
      statement.setObject(2, executedAt);
      statement.setString(3, writeJson(resultPayload));
      statement.setString(4, writeJson(error));
      statement.setString(5, tenantId);
      statement.setString(6, actionCommandId);
      if (statement.executeUpdate() != 1) {
        throw new WorkflowException(ErrorCode.RESOURCE_NOT_FOUND);
      }
      return findActionCommand(tenantId, actionCommandId)
          .orElseThrow(() -> new WorkflowException(ErrorCode.RESOURCE_NOT_FOUND));
    } catch (SQLException ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  private ApprovalInstanceRecord mapApproval(ResultSet result) throws SQLException {
    return new ApprovalInstanceRecord(
        result.getString("id"),
        result.getString("tenant_id"),
        ApprovalStatus.valueOf(result.getString("status")),
        result.getString("requested_by"),
        result.getString("source_type"),
        result.getString("source_id"),
        result.getString("target_type"),
        result.getString("target_id"),
        ActionType.valueOf(result.getString("action_type")),
        result.getString("risk_reason"),
        result.getString("idempotency_key"),
        result.getString("request_fingerprint"),
        readJson(result.getString("action_input_json"), TicketFollowupActionInput.class),
        readJsonList(result.getString("citations_json"), new TypeReference<List<ApprovalCitationSummary>>() {}),
        result.getString("action_command_id"),
        result.getString("trace_id"),
        instant(result, "expires_at"),
        instant(result, "created_at"),
        instant(result, "updated_at"));
  }

  private static ApprovalRecordRecord mapApprovalRecord(ResultSet result) throws SQLException {
    return new ApprovalRecordRecord(
        result.getString("id"),
        result.getString("approval_instance_id"),
        result.getString("tenant_id"),
        ApprovalDecision.valueOf(result.getString("decision")),
        result.getString("decided_by"),
        result.getString("reason"),
        result.getString("action_summary"),
        result.getString("trace_id"),
        instant(result, "created_at"));
  }

  private ActionCommandRecord mapActionCommand(ResultSet result) throws SQLException {
    return new ActionCommandRecord(
        result.getString("id"),
        result.getString("approval_instance_id"),
        result.getString("tenant_id"),
        ActionType.valueOf(result.getString("action_type")),
        result.getString("target_type"),
        result.getString("target_id"),
        result.getString("idempotency_key"),
        ActionCommandStatus.valueOf(result.getString("status")),
        result.getString("created_by"),
        result.getString("trace_id"),
        instant(result, "created_at"),
        instant(result, "executed_at"),
        readJson(result.getString("result_payload_json"), TicketFollowupActionOutput.class),
        readJson(result.getString("error_json"), ErrorResponse.class));
  }

  private static Instant instant(ResultSet result, String column) throws SQLException {
    Object value = result.getObject(column);
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime.toInstant();
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant();
    }
    throw new SQLException("Unsupported timestamp value for " + column + ": " + value.getClass());
  }

  private static String writeJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(value);
    } catch (Exception ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  private static <T> T readJson(String raw, Class<T> type) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return MAPPER.readValue(raw, type);
    } catch (Exception ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  private static <T> T readJsonList(String raw, TypeReference<T> type) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return MAPPER.readValue(raw, type);
    } catch (Exception ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  private Connection open() throws SQLException {
    return DriverManager.getConnection(jdbcUrl);
  }
}
