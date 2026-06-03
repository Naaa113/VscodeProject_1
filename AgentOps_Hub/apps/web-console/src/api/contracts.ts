export type ErrorResponse = {
  error_code: string;
  message: string;
  details?: Record<string, unknown>;
  trace_id: string;
  retryable: boolean;
};

export type LoginRequest = {
  tenant_id: string;
  username: string;
  password: string;
};

export type LoginResponse = {
  tenant_id: string;
  user_id: string;
  access_token: string;
  expires_at: string;
};

export type CurrentUserResponse = {
  tenant_id: string;
  user_id: string;
  username: string;
  roles: string[];
  permissions: string[];
};

export type TicketStatus = "open" | "in_progress" | "waiting_customer" | "resolved" | "closed";
export type TicketPriority = "low" | "normal" | "high" | "urgent";

export type AuditFields = {
  created_by: string;
  created_at: string;
  updated_by?: string | null;
  updated_at?: string | null;
};

export type PageMeta = {
  page: number;
  page_size: number;
  total: number;
};

export type CustomerSummary = {
  customer_id: string;
  display_name: string;
  external_ref?: string | null;
};

export type TicketSummary = {
  ticket_id: string;
  tenant_id: string;
  title: string;
  status: TicketStatus;
  priority: TicketPriority;
  category: string;
  customer: CustomerSummary;
  sla_due_at?: string | null;
  audit: AuditFields;
};

export type TicketDetail = TicketSummary & {
  description: string;
};

export type TicketListResponse = {
  items: TicketSummary[];
  page: PageMeta;
};

export type ApprovalStatus = "pending" | "approved" | "rejected" | "cancelled" | "expired";
export type ActionCommandStatus = "pending" | "success" | "failed" | "cancelled";

export type ApprovalCitationSummary = {
  document_id: string;
  chunk_id: string;
  source_title: string;
  source_uri?: string | null;
};

export type TicketFollowupActionInput = {
  ticket_id: string;
  title: string;
  reason: string;
  due_at?: string | null;
  approval_policy: "required" | "policy_check_required";
};

export type TicketFollowupActionOutput = {
  followup_task_ref: string;
  ticket_id: string;
  created_at: string;
  created_by: string;
};

export type ApprovalRecord = {
  approval_record_id: string;
  approval_instance_id: string;
  tenant_id: string;
  decision: "approved" | "rejected" | "cancelled" | "expired";
  decided_by: string;
  reason: string;
  action_summary: string;
  trace_id: string;
  created_at: string;
};

export type ActionCommand = {
  action_command_id: string;
  approval_instance_id: string;
  tenant_id: string;
  action_type: "ticket_create_followup";
  target_type: string;
  target_id: string;
  idempotency_key: string;
  status: ActionCommandStatus;
  created_by: string;
  created_at: string;
  executed_at?: string | null;
  result_payload?: TicketFollowupActionOutput | null;
  error?: ErrorResponse;
};

export type ApprovalInstance = {
  approval_instance_id: string;
  tenant_id: string;
  status: ApprovalStatus;
  requested_by: string;
  source_type: string;
  source_id: string;
  target_type: string;
  target_id: string;
  action_type: "ticket_create_followup";
  risk_reason: string;
  idempotency_key: string;
  created_at: string;
  updated_at: string;
  expires_at?: string | null;
  action_command_id?: string | null;
  action_input: TicketFollowupActionInput;
  citations: ApprovalCitationSummary[];
  latest_record?: ApprovalRecord | null;
};

export type ApprovalDecisionRequest = {
  reason: string;
};

export type AiTaskStatus = "pending" | "running" | "waiting_approval" | "success" | "failed" | "cancelled";

export type AiTask = {
  task_id: string;
  tenant_id: string;
  task_type: string;
  prompt: string;
  status: AiTaskStatus;
  run_id?: string | null;
  approval_instance_id?: string | null;
  blocking_reason?: string | null;
  report_id?: string | null;
  error?: ErrorResponse;
  retryable?: boolean | null;
  cancelled_by?: string | null;
  created_by: string;
  created_at: string;
};

export type CreateAiTaskRequest = {
  task_type: string;
  prompt: string;
  priority: "low" | "normal" | "high";
};

export type Citation = {
  document_id: string;
  chunk_id: string;
  source_title: string;
  source_uri?: string | null;
};

export type AgentStepStatus = "pending" | "running" | "success" | "failed" | "skipped" | "waiting_human";

export type AgentStep = {
  task_id: string;
  run_id: string;
  step_id: string;
  agent_name: string;
  step_name: string;
  status: AgentStepStatus;
  started_at: string;
  finished_at?: string | null;
  output_summary?: string | null;
  citations?: Citation[];
  error?: ErrorResponse;
};

export type AgentStepListResponse = {
  items: AgentStep[];
};

export type ReportSummary = {
  report_id: string;
  task_id: string;
  tenant_id: string;
  title: string;
  facts: string[];
  inferences: string[];
  recommendations: string[];
  citations: Citation[];
};
