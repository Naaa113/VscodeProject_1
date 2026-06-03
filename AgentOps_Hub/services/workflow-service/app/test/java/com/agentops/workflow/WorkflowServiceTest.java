package com.agentops.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class WorkflowServiceTest {
  @Test
  void expiredApprovalCannotBeApprovedLater() {
    String dbUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    H2WorkflowRepository repository = new H2WorkflowRepository(dbUrl);
    repository.initialize();

    WorkflowService createService =
        new WorkflowService(repository, Clock.fixed(Instant.parse("2026-06-03T08:00:00Z"), ZoneOffset.UTC));
    RequestContext requester =
        new RequestContext(
            "tenant_demo",
            "user_demo",
            List.of("operator"),
            List.of("ticket:followup:request"),
            "trace-create");

    ApprovalInstance created =
        createService.createApproval(
            requester,
            new CreateApprovalRequest(
                "ai_task",
                "task_demo",
                "ticket",
                "ticket_demo",
                ActionType.ticket_create_followup,
                "user_demo",
                "Urgent complaint needs human-approved follow-up.",
                "followup:ticket_demo:expired-demo",
                List.of(),
                new TicketFollowupActionInput(
                    "ticket_demo",
                    "Supervisor follow-up",
                    "Human-approved follow-up task is required.",
                    Instant.parse("2026-06-03T12:00:00Z"),
                    ApprovalPolicy.required)));

    WorkflowService laterService =
        new WorkflowService(repository, Clock.fixed(Instant.parse("2026-06-05T08:00:00Z"), ZoneOffset.UTC));
    ApprovalInstance expired = laterService.getApproval(requester, created.approval_instance_id());
    assertEquals(ApprovalStatus.expired, expired.status());
    assertEquals(ApprovalDecision.expired, expired.latest_record().decision());

    RequestContext approver =
        new RequestContext(
            "tenant_demo",
            "user_supervisor",
            List.of("supervisor"),
            List.of("approval:decide"),
            "trace-approve");
    WorkflowException exception =
        assertThrows(
            WorkflowException.class,
            () -> laterService.approveApproval(approver, created.approval_instance_id(), new ApprovalDecisionRequest("Too late.")));
    assertEquals(ErrorCode.CONFLICT, exception.code());
  }
}
