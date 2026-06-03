package com.agentops.workflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

enum ApprovalStatus {
  pending,
  approved,
  rejected,
  cancelled,
  expired
}

enum ApprovalDecision {
  approved,
  rejected,
  cancelled,
  expired
}

enum ActionCommandStatus {
  pending,
  success,
  failed,
  cancelled
}

enum ActionType {
  ticket_create_followup
}

enum ApprovalPolicy {
  required,
  policy_check_required
}

record ApprovalCitationSummary(String document_id, String chunk_id, String source_title, String source_uri) {}

record TicketFollowupActionInput(
    String ticket_id,
    String title,
    String reason,
    Instant due_at,
    ApprovalPolicy approval_policy) {}

record TicketFollowupActionOutput(
    String followup_task_ref, String ticket_id, Instant created_at, String created_by) {}

record CreateApprovalRequest(
    String source_type,
    String source_task_id,
    String target_type,
    String target_id,
    ActionType action_type,
    String requested_by,
    String risk_reason,
    String idempotency_key,
    List<ApprovalCitationSummary> citations,
    TicketFollowupActionInput action_input) {}

record ApprovalDecisionRequest(String reason) {}

record ApprovalRecord(
    String approval_record_id,
    String approval_instance_id,
    String tenant_id,
    ApprovalDecision decision,
    String decided_by,
    String reason,
    String action_summary,
    String trace_id,
    Instant created_at) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record ActionCommand(
    String action_command_id,
    String approval_instance_id,
    String tenant_id,
    ActionType action_type,
    String target_type,
    String target_id,
    String idempotency_key,
    ActionCommandStatus status,
    String created_by,
    Instant created_at,
    Instant executed_at,
    TicketFollowupActionOutput result_payload,
    ErrorResponse error) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record ApprovalInstance(
    String approval_instance_id,
    String tenant_id,
    ApprovalStatus status,
    String requested_by,
    String source_type,
    String source_id,
    String target_type,
    String target_id,
    ActionType action_type,
    String risk_reason,
    String idempotency_key,
    Instant created_at,
    Instant updated_at,
    Instant expires_at,
    String action_command_id,
    TicketFollowupActionInput action_input,
    List<ApprovalCitationSummary> citations,
    ApprovalRecord latest_record) {}

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

  static ErrorResponse from(ErrorCode code, String traceId, Map<String, Object> details) {
    return new ErrorResponse(code.name(), code.defaultMessage, traceId, code.retryable, details);
  }

  static ErrorResponse from(ErrorCode code, String message, String traceId, Map<String, Object> details) {
    return new ErrorResponse(code.name(), message, traceId, code.retryable, details);
  }
}

record RequestContext(String tenantId, String userId, List<String> roles, List<String> permissions, String traceId) {
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

record ApprovalInstanceRecord(
    String id,
    String tenantId,
    ApprovalStatus status,
    String requestedBy,
    String sourceType,
    String sourceId,
    String targetType,
    String targetId,
    ActionType actionType,
    String riskReason,
    String idempotencyKey,
    String requestFingerprint,
    TicketFollowupActionInput actionInput,
    List<ApprovalCitationSummary> citations,
    String actionCommandId,
    String traceId,
    Instant expiresAt,
    Instant createdAt,
    Instant updatedAt) {}

record ApprovalRecordRecord(
    String id,
    String approvalInstanceId,
    String tenantId,
    ApprovalDecision decision,
    String decidedBy,
    String reason,
    String actionSummary,
    String traceId,
    Instant createdAt) {}

record ActionCommandRecord(
    String id,
    String approvalInstanceId,
    String tenantId,
    ActionType actionType,
    String targetType,
    String targetId,
    String idempotencyKey,
    ActionCommandStatus status,
    String createdBy,
    String traceId,
    Instant createdAt,
    Instant executedAt,
    TicketFollowupActionOutput resultPayload,
    ErrorResponse error) {}
