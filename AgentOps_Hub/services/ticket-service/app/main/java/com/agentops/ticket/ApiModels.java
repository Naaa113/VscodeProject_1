package com.agentops.ticket;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

enum TicketPriority {
  low,
  normal,
  high,
  urgent
}

enum TicketStatus {
  open,
  in_progress,
  waiting_customer,
  resolved,
  closed
}

record CreateCustomerInput(String display_name, String external_ref) {}

record CreateTicketRequest(
    String title,
    String description,
    TicketPriority priority,
    String category,
    String customer_id,
    CreateCustomerInput customer,
    Instant sla_due_at) {}

record CustomerSummary(String customer_id, String display_name, String external_ref) {}

record AuditFields(String created_by, Instant created_at, String updated_by, Instant updated_at) {}

record TicketSummaryResponse(
    String ticket_id,
    String tenant_id,
    String title,
    TicketStatus status,
    TicketPriority priority,
    String category,
    CustomerSummary customer,
    Instant sla_due_at,
    AuditFields audit) {
  static TicketSummaryResponse from(TicketRecord ticket) {
    return new TicketSummaryResponse(
        ticket.id(),
        ticket.tenantId(),
        ticket.title(),
        ticket.status(),
        ticket.priority(),
        ticket.category(),
        new CustomerSummary(ticket.customerId(), ticket.customerDisplayName(), ticket.customerExternalRef()),
        ticket.slaDueAt(),
        new AuditFields(ticket.createdBy(), ticket.createdAt(), ticket.updatedBy(), ticket.updatedAt()));
  }
}

record TicketDetailResponse(
    String ticket_id,
    String tenant_id,
    String title,
    TicketStatus status,
    TicketPriority priority,
    String category,
    CustomerSummary customer,
    Instant sla_due_at,
    AuditFields audit,
    String description) {
  static TicketDetailResponse from(TicketRecord ticket) {
    return new TicketDetailResponse(
        ticket.id(),
        ticket.tenantId(),
        ticket.title(),
        ticket.status(),
        ticket.priority(),
        ticket.category(),
        new CustomerSummary(ticket.customerId(), ticket.customerDisplayName(), ticket.customerExternalRef()),
        ticket.slaDueAt(),
        new AuditFields(ticket.createdBy(), ticket.createdAt(), ticket.updatedBy(), ticket.updatedAt()),
        ticket.description());
  }
}

record PageMeta(int page, int page_size, int total) {}

record TicketListResponse(List<TicketSummaryResponse> items, PageMeta page) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record ErrorResponse(
    String error_code,
    String message,
    String trace_id,
    boolean retryable,
    Map<String, Object> details) {
  static ErrorResponse from(ErrorCode code, String traceId) {
    return new ErrorResponse(code.name(), code.defaultMessage, traceId, code.retryable, null);
  }
}

record RequestContext(
    String tenantId,
    String userId,
    List<String> roles,
    List<String> permissions,
    String traceId) {
  boolean hasPermission(String permission) {
    return permissions != null && permissions.contains(permission);
  }
}

record JwtClaims(
    String subject,
    String tenantId,
    List<String> roles,
    List<String> permissions,
    Instant issuedAt,
    Instant expiresAt,
    String jwtId) {}

record TicketRecord(
    String id,
    String tenantId,
    String customerId,
    String customerDisplayName,
    String customerExternalRef,
    String title,
    String description,
    TicketStatus status,
    TicketPriority priority,
    String category,
    Instant slaDueAt,
    String createdBy,
    Instant createdAt,
    String updatedBy,
    Instant updatedAt) {}

record CustomerRecord(
    String id,
    String tenantId,
    String displayName,
    String externalRef,
    String createdBy,
    Instant createdAt,
    String updatedBy,
    Instant updatedAt) {}

record TicketSearchFilter(
    TicketStatus status,
    TicketPriority priority,
    String category,
    String customerId,
    Instant createdFrom,
    Instant createdTo,
    Instant slaDueBefore,
    String query,
    int page,
    int pageSize) {}
