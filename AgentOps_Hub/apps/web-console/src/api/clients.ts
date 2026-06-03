import type {
  ActionCommand,
  AgentStepListResponse,
  ApprovalDecisionRequest,
  ApprovalInstance,
  AiTask,
  CreateAiTaskRequest,
  CurrentUserResponse,
  LoginRequest,
  LoginResponse,
  ReportSummary,
  TicketDetail,
  TicketListResponse
} from "./contracts";

export type SessionContext = {
  accessToken: string;
  tenantId: string;
  traceId: string;
};

export interface AuthClient {
  login(request: LoginRequest): Promise<LoginResponse>;
  currentUser(session: SessionContext): Promise<CurrentUserResponse>;
}

export interface TicketClient {
  listTickets(session: SessionContext, query?: string): Promise<TicketListResponse>;
  getTicket(session: SessionContext, ticketId: string): Promise<TicketDetail>;
}

export interface AiTaskClient {
  createTask(session: SessionContext, request: CreateAiTaskRequest): Promise<AiTask>;
  getTask(session: SessionContext, taskId: string): Promise<AiTask>;
  listSteps(session: SessionContext, taskId: string): Promise<AgentStepListResponse>;
  getReport(session: SessionContext, taskId: string): Promise<ReportSummary>;
}

export interface WorkflowClient {
  getApproval(session: SessionContext, approvalId: string): Promise<ApprovalInstance>;
  approveApproval(session: SessionContext, approvalId: string, request: ApprovalDecisionRequest): Promise<ApprovalInstance>;
  rejectApproval(session: SessionContext, approvalId: string, request: ApprovalDecisionRequest): Promise<ApprovalInstance>;
  cancelApproval(session: SessionContext, approvalId: string, request: ApprovalDecisionRequest): Promise<ApprovalInstance>;
  getActionCommand(session: SessionContext, actionCommandId: string): Promise<ActionCommand>;
}

export type ApiClients = {
  auth: AuthClient;
  tickets: TicketClient;
  aiTasks: AiTaskClient;
  workflow: WorkflowClient;
};

export class ApiError extends Error {
  constructor(
    public readonly error_code: string,
    message: string,
    public readonly trace_id: string,
    public readonly retryable: boolean
  ) {
    super(message);
  }
}
