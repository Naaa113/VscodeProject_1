import { ApiError, type AiTaskClient, type AuthClient, type SessionContext, type TicketClient, type WorkflowClient } from "../api/clients";
import type { ActionCommand, ApprovalDecisionRequest, ApprovalInstance, CreateAiTaskRequest, LoginRequest } from "../api/contracts";
import {
  mockActionCommandSuccess,
  mockAgentSteps,
  mockApprovalPending,
  mockCurrentUser,
  mockLoginResponse,
  mockReport,
  mockTicketDetails,
  mockTickets,
  mockWaitingApprovalTask
} from "./mockFixtures";

const delay = (milliseconds = 180) => new Promise((resolve) => globalThis.setTimeout(resolve, milliseconds));
const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T;

const requireValidTenant = (session: SessionContext) => {
  if (!session.tenantId) {
    throw new ApiError("TENANT_REQUIRED", "Tenant header is required.", session.traceId, false);
  }

  if (session.tenantId !== mockLoginResponse.tenant_id) {
    throw new ApiError("AUTH_FORBIDDEN", "Tenant does not match the current session.", session.traceId, false);
  }
};

class MockWorkflowState {
  task = clone(mockWaitingApprovalTask);
  approval = clone(mockApprovalPending);
  action: ActionCommand | null = null;
}

export class MockAuthClient implements AuthClient {
  async login(request: LoginRequest) {
    await delay();

    if (request.tenant_id !== "tenant_demo" || request.username !== "demo.user" || !request.password) {
      throw new ApiError("AUTH_INVALID_CREDENTIALS", "Tenant, username, or password is invalid.", "trace_mock_login", false);
    }

    return mockLoginResponse;
  }

  async currentUser(session: SessionContext) {
    await delay(120);
    requireValidTenant(session);
    return mockCurrentUser;
  }
}

export class MockTicketClient implements TicketClient {
  async listTickets(session: SessionContext, query = "") {
    await delay();
    requireValidTenant(session);

    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) {
      return mockTickets;
    }

    const items = mockTickets.items.filter((ticket) =>
      [ticket.title, ticket.category, ticket.customer.display_name, ticket.priority, ticket.status]
        .join(" ")
        .toLowerCase()
        .includes(normalizedQuery)
    );

    return {
      items,
      page: {
        page: 1,
        page_size: 20,
        total: items.length
      }
    };
  }

  async getTicket(session: SessionContext, ticketId: string) {
    await delay(150);
    requireValidTenant(session);

    const ticket = mockTicketDetails[ticketId];
    if (!ticket) {
      throw new ApiError("RESOURCE_NOT_FOUND", "Ticket was not found or is not accessible.", session.traceId, false);
    }

    return ticket;
  }
}

export class MockAiTaskClient implements AiTaskClient {
  constructor(private readonly state: MockWorkflowState = new MockWorkflowState()) {}

  async createTask(session: SessionContext, request: CreateAiTaskRequest) {
    await delay(260);
    requireValidTenant(session);

    if (request.task_type !== "complaint_analysis") {
      throw new ApiError("AGENT_TASK_FAILED", "Only complaint analysis is available in the Phase 006 mock.", session.traceId, true);
    }

    return {
      ...this.state.task,
      prompt: request.prompt,
      created_at: new Date("2026-06-01T08:10:00Z").toISOString()
    };
  }

  async getTask(session: SessionContext, taskId: string) {
    await delay(120);
    requireValidTenant(session);

    if (taskId !== this.state.task.task_id) {
      throw new ApiError("RESOURCE_NOT_FOUND", "AI task was not found or is not accessible.", session.traceId, false);
    }

    return clone(this.state.task);
  }

  async listSteps(session: SessionContext, taskId: string) {
    await delay(140);
    await this.getTask(session, taskId);
    return {
      items: mockAgentSteps
    };
  }

  async getReport(session: SessionContext, taskId: string) {
    await delay(140);
    await this.getTask(session, taskId);
    return mockReport;
  }
}

export class MockWorkflowClient implements WorkflowClient {
  constructor(private readonly state: MockWorkflowState = new MockWorkflowState()) {}

  async getApproval(session: SessionContext, approvalId: string) {
    await delay(120);
    requireValidTenant(session);
    if (approvalId !== this.state.approval.approval_instance_id) {
      throw new ApiError("RESOURCE_NOT_FOUND", "Approval was not found or is not accessible.", session.traceId, false);
    }
    return clone(this.state.approval);
  }

  async approveApproval(session: SessionContext, approvalId: string, request: ApprovalDecisionRequest) {
    await delay(160);
    const approval = await this.getApproval(session, approvalId);
    if (!request.reason.trim()) {
      throw new ApiError("VALIDATION_FAILED", "Approval reason is required.", session.traceId, false);
    }
    const action = clone(mockActionCommandSuccess);
    action.approval_instance_id = approval.approval_instance_id;
    this.state.action = action;
    this.state.approval = {
      ...approval,
      status: "approved",
      updated_at: "2026-06-01T08:15:00Z",
      action_command_id: action.action_command_id,
      latest_record: {
        approval_record_id: "approval_record_mock_approve",
        approval_instance_id: approval.approval_instance_id,
        tenant_id: approval.tenant_id,
        decision: "approved",
        decided_by: mockCurrentUser.user_id,
        reason: request.reason,
        action_summary: "Create one workflow-owned follow-up action reference.",
        trace_id: session.traceId,
        created_at: "2026-06-01T08:15:00Z"
      }
    };
    this.state.task = {
      ...this.state.task,
      status: "success",
      blocking_reason: null
    };
    return clone(this.state.approval);
  }

  async rejectApproval(session: SessionContext, approvalId: string, request: ApprovalDecisionRequest) {
    await delay(160);
    const approval = await this.getApproval(session, approvalId);
    this.state.approval = {
      ...approval,
      status: "rejected",
      updated_at: "2026-06-01T08:16:00Z",
      latest_record: {
        approval_record_id: "approval_record_mock_reject",
        approval_instance_id: approval.approval_instance_id,
        tenant_id: approval.tenant_id,
        decision: "rejected",
        decided_by: mockCurrentUser.user_id,
        reason: request.reason,
        action_summary: "No action command created.",
        trace_id: session.traceId,
        created_at: "2026-06-01T08:16:00Z"
      }
    };
    this.state.task = {
      ...this.state.task,
      status: "failed",
      blocking_reason: "审批已拒绝，未创建跟进动作。"
    };
    return clone(this.state.approval);
  }

  async cancelApproval(session: SessionContext, approvalId: string, request: ApprovalDecisionRequest) {
    await delay(160);
    const approval = await this.getApproval(session, approvalId);
    this.state.approval = {
      ...approval,
      status: "cancelled",
      updated_at: "2026-06-01T08:17:00Z",
      latest_record: {
        approval_record_id: "approval_record_mock_cancel",
        approval_instance_id: approval.approval_instance_id,
        tenant_id: approval.tenant_id,
        decision: "cancelled",
        decided_by: mockCurrentUser.user_id,
        reason: request.reason,
        action_summary: "No action command created.",
        trace_id: session.traceId,
        created_at: "2026-06-01T08:17:00Z"
      }
    };
    this.state.task = {
      ...this.state.task,
      status: "cancelled",
      blocking_reason: "审批请求已取消。"
    };
    return clone(this.state.approval);
  }

  async getActionCommand(session: SessionContext, actionCommandId: string) {
    await delay(100);
    requireValidTenant(session);
    if (!this.state.action || this.state.action.action_command_id !== actionCommandId) {
      throw new ApiError("RESOURCE_NOT_FOUND", "Action command was not found or is not accessible.", session.traceId, false);
    }
    return clone(this.state.action);
  }
}

const workflowState = new MockWorkflowState();

export const mockApiClients = {
  auth: new MockAuthClient(),
  tickets: new MockTicketClient(),
  aiTasks: new MockAiTaskClient(workflowState),
  workflow: new MockWorkflowClient(workflowState)
};
