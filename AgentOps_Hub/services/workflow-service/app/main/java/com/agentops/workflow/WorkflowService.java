package com.agentops.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class WorkflowService {
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final H2WorkflowRepository repository;
  private final Clock clock;

  WorkflowService(H2WorkflowRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  ApprovalInstance createApproval(RequestContext context, CreateApprovalRequest request) {
    validateCreateRequest(context, request);
    String fingerprint = fingerprint(request);
    Optional<ApprovalInstanceRecord> existing =
        repository.findApprovalByIdempotency(context.tenantId(), request.action_type(), request.idempotency_key());
    if (existing.isPresent()) {
      ApprovalInstanceRecord record = existing.get();
      if (!record.requestFingerprint().equals(fingerprint)) {
        throw new WorkflowException(
            ErrorCode.CONFLICT,
            "Idempotency key already exists with a different payload.");
      }
      return toApproval(record);
    }

    Instant now = clock.instant();
    ApprovalInstanceRecord record =
        new ApprovalInstanceRecord(
            newId("approval"),
            context.tenantId(),
            ApprovalStatus.pending,
            request.requested_by(),
            request.source_type().trim(),
            request.source_task_id().trim(),
            request.target_type().trim(),
            request.target_id().trim(),
            request.action_type(),
            request.risk_reason().trim(),
            request.idempotency_key().trim(),
            fingerprint,
            request.action_input(),
            request.citations() == null ? List.of() : request.citations(),
            null,
            context.traceId(),
            now.plusSeconds(24 * 60 * 60),
            now,
            now);
    repository.createApproval(record);
    return toApproval(record);
  }

  ApprovalInstance getApproval(RequestContext context, String approvalId) {
    return toApproval(expireIfNeeded(loadApproval(context.tenantId(), approvalId), "system", context.traceId()));
  }

  ApprovalInstance approveApproval(RequestContext context, String approvalId, ApprovalDecisionRequest request) {
    if (!context.hasPermission("approval:decide") || isSystemLikeUser(context.userId())) {
      throw new WorkflowException(ErrorCode.AUTH_FORBIDDEN);
    }
    ApprovalInstanceRecord current = expireIfNeeded(loadApproval(context.tenantId(), approvalId), "system", context.traceId());
    if (current.status() == ApprovalStatus.approved) {
      return toApproval(current);
    }
    if (current.status() != ApprovalStatus.pending) {
      throw new WorkflowException(ErrorCode.CONFLICT, "Approval is no longer pending.");
    }
    String reason = requireText(request.reason(), "Approval reason is required.");
    repository.createApprovalRecord(
        new ApprovalRecordRecord(
            newId("approval_record"),
            current.id(),
            current.tenantId(),
            ApprovalDecision.approved,
            context.userId(),
            reason,
            "Create one workflow-owned follow-up action command.",
            context.traceId(),
            clock.instant()));

    ActionCommandRecord action =
        repository.findActionCommandByApproval(current.tenantId(), current.id()).orElseGet(() -> createPendingAction(current, context));
    ApprovalInstanceRecord updated =
        repository.updateApproval(current.tenantId(), current.id(), ApprovalStatus.approved, action.id(), clock.instant());
    executeAction(updated, action, context);
    return toApproval(updated);
  }

  ApprovalInstance rejectApproval(RequestContext context, String approvalId, ApprovalDecisionRequest request) {
    if (!context.hasPermission("approval:decide") || isSystemLikeUser(context.userId())) {
      throw new WorkflowException(ErrorCode.AUTH_FORBIDDEN);
    }
    ApprovalInstanceRecord current = expireIfNeeded(loadApproval(context.tenantId(), approvalId), "system", context.traceId());
    if (current.status() == ApprovalStatus.rejected) {
      return toApproval(current);
    }
    if (current.status() != ApprovalStatus.pending) {
      throw new WorkflowException(ErrorCode.CONFLICT, "Approval is no longer pending.");
    }
    repository.createApprovalRecord(
        new ApprovalRecordRecord(
            newId("approval_record"),
            current.id(),
            current.tenantId(),
            ApprovalDecision.rejected,
            context.userId(),
            requireText(request.reason(), "Rejection reason is required."),
            "No action command created.",
            context.traceId(),
            clock.instant()));
    ApprovalInstanceRecord updated =
        repository.updateApproval(current.tenantId(), current.id(), ApprovalStatus.rejected, null, clock.instant());
    return toApproval(updated);
  }

  ApprovalInstance cancelApproval(RequestContext context, String approvalId, ApprovalDecisionRequest request) {
    ApprovalInstanceRecord current = expireIfNeeded(loadApproval(context.tenantId(), approvalId), "system", context.traceId());
    boolean canDecide = context.hasPermission("approval:decide") && !isSystemLikeUser(context.userId());
    boolean isRequester = current.requestedBy().equals(context.userId());
    if (!canDecide && !isRequester) {
      throw new WorkflowException(ErrorCode.AUTH_FORBIDDEN);
    }
    if (current.status() == ApprovalStatus.cancelled) {
      return toApproval(current);
    }
    if (current.status() != ApprovalStatus.pending) {
      throw new WorkflowException(ErrorCode.CONFLICT, "Approval is no longer pending.");
    }
    repository.createApprovalRecord(
        new ApprovalRecordRecord(
            newId("approval_record"),
            current.id(),
            current.tenantId(),
            ApprovalDecision.cancelled,
            context.userId(),
            requireText(request.reason(), "Cancellation reason is required."),
            "No action command created.",
            context.traceId(),
            clock.instant()));
    ApprovalInstanceRecord updated =
        repository.updateApproval(current.tenantId(), current.id(), ApprovalStatus.cancelled, null, clock.instant());
    return toApproval(updated);
  }

  ActionCommand getActionCommand(RequestContext context, String actionCommandId) {
    ActionCommandRecord record =
        repository.findActionCommand(context.tenantId(), actionCommandId)
            .orElseThrow(() -> new WorkflowException(ErrorCode.RESOURCE_NOT_FOUND));
    return toActionCommand(record);
  }

  private ApprovalInstanceRecord loadApproval(String tenantId, String approvalId) {
    return repository.findApproval(tenantId, approvalId)
        .orElseThrow(() -> new WorkflowException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private ApprovalInstanceRecord expireIfNeeded(ApprovalInstanceRecord record, String decidedBy, String traceId) {
    if (record.status() != ApprovalStatus.pending || record.expiresAt() == null || record.expiresAt().isAfter(clock.instant())) {
      return record;
    }
    repository.createApprovalRecord(
        new ApprovalRecordRecord(
            newId("approval_record"),
            record.id(),
            record.tenantId(),
            ApprovalDecision.expired,
            decidedBy,
            "Approval expired before a human decision was submitted.",
            "No action command created.",
            traceId,
            clock.instant()));
    return repository.updateApproval(record.tenantId(), record.id(), ApprovalStatus.expired, null, clock.instant());
  }

  private ActionCommandRecord createPendingAction(ApprovalInstanceRecord approval, RequestContext context) {
    ActionCommandRecord action =
        new ActionCommandRecord(
            newId("action"),
            approval.id(),
            approval.tenantId(),
            approval.actionType(),
            approval.targetType(),
            approval.targetId(),
            approval.idempotencyKey(),
            ActionCommandStatus.pending,
            context.userId(),
            context.traceId(),
            clock.instant(),
            null,
            null,
            null);
    return repository.createActionCommand(action);
  }

  private void executeAction(ApprovalInstanceRecord approval, ActionCommandRecord action, RequestContext context) {
    if (action.status() != ActionCommandStatus.pending) {
      return;
    }
    if (approval.status() != ApprovalStatus.approved) {
      throw new WorkflowException(ErrorCode.CONFLICT, "Action command cannot execute before approval.");
    }
    TicketFollowupActionInput input = approval.actionInput();
    if (input.title().contains("[force-fail]") || input.reason().contains("[force-fail]")) {
      repository.updateActionCommandResult(
          approval.tenantId(),
          action.id(),
          ActionCommandStatus.failed,
          clock.instant(),
          null,
          ErrorResponse.from(
              ErrorCode.DOWNSTREAM_UNAVAILABLE,
              "Follow-up action executor is temporarily unavailable.",
              context.traceId(),
              Map.of("target_id", approval.targetId())));
      return;
    }
    repository.updateActionCommandResult(
        approval.tenantId(),
        action.id(),
        ActionCommandStatus.success,
        clock.instant(),
        new TicketFollowupActionOutput(
            newId("followup_ref"),
            input.ticket_id(),
            clock.instant(),
            context.userId()),
        null);
  }

  private ApprovalInstance toApproval(ApprovalInstanceRecord record) {
    ApprovalRecordRecord latestRecord = repository.findLatestApprovalRecord(record.id()).orElse(null);
    return new ApprovalInstance(
        record.id(),
        record.tenantId(),
        record.status(),
        record.requestedBy(),
        record.sourceType(),
        record.sourceId(),
        record.targetType(),
        record.targetId(),
        record.actionType(),
        record.riskReason(),
        record.idempotencyKey(),
        record.createdAt(),
        record.updatedAt(),
        record.expiresAt(),
        record.actionCommandId(),
        record.actionInput(),
        record.citations() == null ? List.of() : record.citations(),
        latestRecord == null ? null : toApprovalRecord(latestRecord));
  }

  private static ApprovalRecord toApprovalRecord(ApprovalRecordRecord record) {
    return new ApprovalRecord(
        record.id(),
        record.approvalInstanceId(),
        record.tenantId(),
        record.decision(),
        record.decidedBy(),
        record.reason(),
        record.actionSummary(),
        record.traceId(),
        record.createdAt());
  }

  private static ActionCommand toActionCommand(ActionCommandRecord record) {
    return new ActionCommand(
        record.id(),
        record.approvalInstanceId(),
        record.tenantId(),
        record.actionType(),
        record.targetType(),
        record.targetId(),
        record.idempotencyKey(),
        record.status(),
        record.createdBy(),
        record.createdAt(),
        record.executedAt(),
        record.resultPayload(),
        record.error());
  }

  private static void validateCreateRequest(RequestContext context, CreateApprovalRequest request) {
    if (request == null) {
      throw new WorkflowException(ErrorCode.VALIDATION_FAILED);
    }
    requireText(request.source_type(), "source_type is required.");
    requireText(request.source_task_id(), "source_task_id is required.");
    requireText(request.target_type(), "target_type is required.");
    requireText(request.target_id(), "target_id is required.");
    requireText(request.requested_by(), "requested_by is required.");
    requireText(request.risk_reason(), "risk_reason is required.");
    requireText(request.idempotency_key(), "idempotency_key is required.");
    if (!context.userId().equals(request.requested_by())) {
      throw new WorkflowException(ErrorCode.AUTH_FORBIDDEN);
    }
    if (request.action_type() != ActionType.ticket_create_followup) {
      throw new WorkflowException(ErrorCode.VALIDATION_FAILED);
    }
    if (!"ticket".equals(request.target_type().trim())) {
      throw new WorkflowException(ErrorCode.VALIDATION_FAILED);
    }
    if (request.action_input() == null) {
      throw new WorkflowException(ErrorCode.VALIDATION_FAILED);
    }
    requireText(request.action_input().ticket_id(), "action_input.ticket_id is required.");
    requireText(request.action_input().title(), "action_input.title is required.");
    requireText(request.action_input().reason(), "action_input.reason is required.");
    if (request.action_input().approval_policy() == null) {
      throw new WorkflowException(ErrorCode.VALIDATION_FAILED);
    }
    if (!request.target_id().trim().equals(request.action_input().ticket_id().trim())) {
      throw new WorkflowException(ErrorCode.VALIDATION_FAILED);
    }
  }

  private static boolean isSystemLikeUser(String userId) {
    return userId != null && userId.startsWith("agent_");
  }

  private static String requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new WorkflowException(ErrorCode.VALIDATION_FAILED, message);
    }
    return value.trim();
  }

  private static String fingerprint(CreateApprovalRequest request) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(MAPPER.writeValueAsBytes(request));
      return HexFormat.of().formatHex(digest);
    } catch (Exception ex) {
      throw new WorkflowException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  private static String newId(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }
}
