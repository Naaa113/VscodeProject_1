package com.agentops.ticket;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class H2TicketRepository {
  private final String jdbcUrl;

  H2TicketRepository(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
  }

  void initialize() {
    try (Connection connection = open()) {
      String schema;
      try (var stream = H2TicketRepository.class.getResourceAsStream("/schema.sql")) {
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
      throw new TicketException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  CustomerRecord createCustomer(
      String tenantId, CreateCustomerInput input, String createdBy, Instant createdAt) {
    CustomerRecord customer =
        new CustomerRecord(
            "customer_" + UUID.randomUUID(),
            tenantId,
            input.display_name().trim(),
            blankToNull(input.external_ref()),
            createdBy,
            createdAt,
            null,
            null);
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                INSERT INTO customer
                  (id, tenant_id, display_name, external_ref, created_by, created_at, updated_by, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
      statement.setString(1, customer.id());
      statement.setString(2, customer.tenantId());
      statement.setString(3, customer.displayName());
      statement.setString(4, customer.externalRef());
      statement.setString(5, customer.createdBy());
      statement.setObject(6, customer.createdAt());
      statement.setString(7, customer.updatedBy());
      statement.setObject(8, customer.updatedAt());
      statement.executeUpdate();
      return customer;
    } catch (SQLException ex) {
      throw new TicketException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  Optional<CustomerRecord> findCustomer(String tenantId, String customerId) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                SELECT id, tenant_id, display_name, external_ref, created_by, created_at, updated_by, updated_at
                FROM customer
                WHERE tenant_id = ? AND id = ?
                """)) {
      statement.setString(1, tenantId);
      statement.setString(2, customerId);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return Optional.empty();
        }
        return Optional.of(mapCustomer(result));
      }
    } catch (SQLException ex) {
      throw new TicketException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  TicketRecord createTicket(
      String tenantId,
      CustomerRecord customer,
      CreateTicketRequest request,
      String createdBy,
      Instant createdAt) {
    String ticketId = "ticket_" + UUID.randomUUID();
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                INSERT INTO ticket
                  (id, tenant_id, customer_id, title, description, status, priority, category,
                   sla_due_at, created_by, created_at, updated_by, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
      statement.setString(1, ticketId);
      statement.setString(2, tenantId);
      statement.setString(3, customer.id());
      statement.setString(4, request.title().trim());
      statement.setString(5, request.description().trim());
      statement.setString(6, TicketStatus.open.name());
      statement.setString(7, request.priority().name());
      statement.setString(8, request.category().trim());
      statement.setObject(9, request.sla_due_at());
      statement.setString(10, createdBy);
      statement.setObject(11, createdAt);
      statement.setString(12, null);
      statement.setObject(13, null);
      statement.executeUpdate();
      return findTicket(tenantId, ticketId).orElseThrow(() -> new TicketException(ErrorCode.INTERNAL_ERROR));
    } catch (SQLException ex) {
      throw new TicketException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  Optional<TicketRecord> findTicket(String tenantId, String ticketId) {
    try (Connection connection = open();
        PreparedStatement statement = connection.prepareStatement(ticketSelect() + " WHERE t.tenant_id = ? AND t.id = ?")) {
      statement.setString(1, tenantId);
      statement.setString(2, ticketId);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return Optional.empty();
        }
        return Optional.of(mapTicket(result));
      }
    } catch (SQLException ex) {
      throw new TicketException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  List<TicketRecord> searchTickets(String tenantId, TicketSearchFilter filter) {
    List<TicketRecord> tenantTickets = new ArrayList<>();
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(ticketSelect() + " WHERE t.tenant_id = ?")) {
      statement.setString(1, tenantId);
      try (ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          tenantTickets.add(mapTicket(result));
        }
      }
    } catch (SQLException ex) {
      throw new TicketException(ErrorCode.INTERNAL_ERROR, ex);
    }
    return tenantTickets.stream()
        .filter(ticket -> filter.status() == null || ticket.status() == filter.status())
        .filter(ticket -> filter.priority() == null || ticket.priority() == filter.priority())
        .filter(ticket -> isBlank(filter.category()) || ticket.category().equals(filter.category()))
        .filter(ticket -> isBlank(filter.customerId()) || ticket.customerId().equals(filter.customerId()))
        .filter(ticket -> filter.createdFrom() == null || !ticket.createdAt().isBefore(filter.createdFrom()))
        .filter(ticket -> filter.createdTo() == null || !ticket.createdAt().isAfter(filter.createdTo()))
        .filter(ticket -> filter.slaDueBefore() == null || (ticket.slaDueAt() != null && !ticket.slaDueAt().isAfter(filter.slaDueBefore())))
        .filter(ticket -> matchesQuery(ticket, filter.query()))
        .sorted(Comparator.comparing(TicketRecord::createdAt).reversed())
        .toList();
  }

  void audit(String tenantId, String userId, String ticketId, String action, boolean success, ErrorCode errorCode, String traceId) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                INSERT INTO ticket_audit_log
                  (id, tenant_id, user_id, ticket_id, action, success, error_code, trace_id, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
      statement.setString(1, UUID.randomUUID().toString());
      statement.setString(2, tenantId);
      statement.setString(3, userId);
      statement.setString(4, ticketId);
      statement.setString(5, action);
      statement.setBoolean(6, success);
      statement.setString(7, errorCode == null ? null : errorCode.name());
      statement.setString(8, traceId);
      statement.setObject(9, Instant.now());
      statement.executeUpdate();
    } catch (SQLException ex) {
      throw new TicketException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  int auditCount(String action, boolean success) {
    try (Connection connection = open();
        PreparedStatement statement =
            connection.prepareStatement("SELECT COUNT(*) FROM ticket_audit_log WHERE action = ? AND success = ?")) {
      statement.setString(1, action);
      statement.setBoolean(2, success);
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        return result.getInt(1);
      }
    } catch (SQLException ex) {
      throw new TicketException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  private static boolean matchesQuery(TicketRecord ticket, String query) {
    if (isBlank(query)) {
      return true;
    }
    String normalized = query.toLowerCase();
    return ticket.title().toLowerCase().contains(normalized)
        || ticket.description().toLowerCase().contains(normalized)
        || ticket.customerDisplayName().toLowerCase().contains(normalized);
  }

  private static String ticketSelect() {
    return """
        SELECT t.id, t.tenant_id, t.customer_id, c.display_name AS customer_display_name,
               c.external_ref AS customer_external_ref, t.title, t.description, t.status, t.priority,
               t.category, t.sla_due_at, t.created_by, t.created_at, t.updated_by, t.updated_at
        FROM ticket t
        JOIN customer c ON c.id = t.customer_id AND c.tenant_id = t.tenant_id
        """;
  }

  private static CustomerRecord mapCustomer(ResultSet result) throws SQLException {
    return new CustomerRecord(
        result.getString("id"),
        result.getString("tenant_id"),
        result.getString("display_name"),
        result.getString("external_ref"),
        result.getString("created_by"),
        instant(result, "created_at"),
        result.getString("updated_by"),
        instant(result, "updated_at"));
  }

  private static TicketRecord mapTicket(ResultSet result) throws SQLException {
    return new TicketRecord(
        result.getString("id"),
        result.getString("tenant_id"),
        result.getString("customer_id"),
        result.getString("customer_display_name"),
        result.getString("customer_external_ref"),
        result.getString("title"),
        result.getString("description"),
        TicketStatus.valueOf(result.getString("status")),
        TicketPriority.valueOf(result.getString("priority")),
        result.getString("category"),
        instant(result, "sla_due_at"),
        result.getString("created_by"),
        instant(result, "created_at"),
        result.getString("updated_by"),
        instant(result, "updated_at"));
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

  private static String blankToNull(String value) {
    return isBlank(value) ? null : value.trim();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private Connection open() throws SQLException {
    return DriverManager.getConnection(jdbcUrl);
  }
}
