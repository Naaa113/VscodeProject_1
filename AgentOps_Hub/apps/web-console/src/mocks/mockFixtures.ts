import type {
  ActionCommand,
  AgentStep,
  ApprovalInstance,
  AiTask,
  CurrentUserResponse,
  LoginResponse,
  ReportSummary,
  TicketDetail,
  TicketListResponse
} from "../api/contracts";

export const mockLoginResponse: LoginResponse = {
  tenant_id: "tenant_demo",
  user_id: "user_demo",
  access_token: "mock-web-console-token",
  expires_at: "2026-06-01T12:00:00Z"
};

export const mockCurrentUser: CurrentUserResponse = {
  tenant_id: "tenant_demo",
  user_id: "user_demo",
  username: "demo.user",
  roles: ["ops"],
  permissions: ["auth:me", "ticket:read", "ai_task:create", "ticket:followup:request", "approval:decide"]
};

export const mockTickets: TicketListResponse = {
  items: [
    {
      ticket_id: "ticket_demo",
      tenant_id: "tenant_demo",
      title: "Example customer complaint",
      status: "open",
      priority: "high",
      category: "complaint",
      customer: {
        customer_id: "customer_demo",
        display_name: "Example Customer",
        external_ref: "crm-1001"
      },
      sla_due_at: "2026-06-02T08:00:00Z",
      audit: {
        created_by: "user_demo",
        created_at: "2026-06-01T08:00:00Z"
      }
    },
    {
      ticket_id: "ticket_latency",
      tenant_id: "tenant_demo",
      title: "Delayed response escalation",
      status: "in_progress",
      priority: "urgent",
      category: "sla",
      customer: {
        customer_id: "customer_north",
        display_name: "Northwind Retail",
        external_ref: "crm-1420"
      },
      sla_due_at: "2026-06-01T10:30:00Z",
      audit: {
        created_by: "user_demo",
        created_at: "2026-06-01T07:25:00Z"
      }
    },
    {
      ticket_id: "ticket_empty_check",
      tenant_id: "tenant_demo",
      title: "Billing wording clarification",
      status: "waiting_customer",
      priority: "normal",
      category: "billing",
      customer: {
        customer_id: "customer_south",
        display_name: "South Harbor Group",
        external_ref: "crm-1288"
      },
      sla_due_at: "2026-06-03T09:00:00Z",
      audit: {
        created_by: "user_demo",
        created_at: "2026-05-31T15:45:00Z"
      }
    }
  ],
  page: {
    page: 1,
    page_size: 20,
    total: 3
  }
};

export const mockTicketDetails: Record<string, TicketDetail> = {
  ticket_demo: {
    ...mockTickets.items[0],
    description:
      "Customer reported repeated delayed responses across the last week and asked for a clear follow-up owner."
  },
  ticket_latency: {
    ...mockTickets.items[1],
    description:
      "Escalation mentions a missed SLA and asks whether the customer should receive a supervisor review."
  },
  ticket_empty_check: {
    ...mockTickets.items[2],
    description:
      "Customer is asking for clarification on billing wording. No action should be taken until the policy citation is checked."
  }
};

export const mockWaitingApprovalTask: AiTask = {
  task_id: "task_complaint_demo",
  tenant_id: "tenant_demo",
  task_type: "complaint_analysis",
  prompt: "Analyze recent complaint tickets and prepare a risk-aware handling summary.",
  status: "waiting_approval",
  run_id: "run_mock_complaint",
  approval_instance_id: "approval_mock_complaint",
  blocking_reason: "Mock risk policy requires human confirmation before any follow-up action.",
  report_id: "report_mock_complaint",
  retryable: false,
  created_by: "user_demo",
  created_at: "2026-06-01T08:10:00Z"
};

export const mockApprovalPending: ApprovalInstance = {
  approval_instance_id: "approval_mock_complaint",
  tenant_id: "tenant_demo",
  status: "pending",
  requested_by: "user_demo",
  source_type: "ai_task",
  source_id: "task_complaint_demo",
  target_type: "ticket",
  target_id: "ticket_latency",
  action_type: "ticket_create_followup",
  risk_reason: "Mock risk policy requires human confirmation before any follow-up action.",
  idempotency_key: "followup:ticket_latency:task_complaint_demo",
  created_at: "2026-06-01T08:10:09Z",
  updated_at: "2026-06-01T08:10:09Z",
  expires_at: "2026-06-02T08:10:09Z",
  action_command_id: null,
  action_input: {
    ticket_id: "ticket_latency",
    title: "Supervisor follow-up for delayed response escalation",
    reason: "High-risk complaint handling needs a human-approved follow-up task.",
    due_at: "2026-06-01T12:00:00Z",
    approval_policy: "required"
  },
  citations: [
    {
      document_id: "doc_sop_mock",
      chunk_id: "chunk_mock_01",
      source_title: "Mock complaint SOP",
      source_uri: "mock://knowledge/doc_sop_mock#chunk_mock_01"
    }
  ],
  latest_record: null
};

export const mockActionCommandSuccess: ActionCommand = {
  action_command_id: "action_mock_complaint",
  approval_instance_id: "approval_mock_complaint",
  tenant_id: "tenant_demo",
  action_type: "ticket_create_followup",
  target_type: "ticket",
  target_id: "ticket_latency",
  idempotency_key: "followup:ticket_latency:task_complaint_demo",
  status: "success",
  created_by: "user_supervisor",
  created_at: "2026-06-01T08:15:00Z",
  executed_at: "2026-06-01T08:15:01Z",
  result_payload: {
    followup_task_ref: "followup_ref_mock_001",
    ticket_id: "ticket_latency",
    created_at: "2026-06-01T08:15:01Z",
    created_by: "user_supervisor"
  }
};

export const mockActionCommandFailed: ActionCommand = {
  action_command_id: "action_mock_failed",
  approval_instance_id: "approval_mock_failed",
  tenant_id: "tenant_demo",
  action_type: "ticket_create_followup",
  target_type: "ticket",
  target_id: "ticket_latency",
  idempotency_key: "followup:ticket_latency:task_failed",
  status: "failed",
  created_by: "user_supervisor",
  created_at: "2026-06-01T08:20:00Z",
  executed_at: "2026-06-01T08:20:01Z",
  result_payload: null,
  error: {
    error_code: "DOWNSTREAM_UNAVAILABLE",
    message: "Mock follow-up executor is temporarily unavailable.",
    trace_id: "trace_action_failed",
    retryable: true
  }
};

export const mockAgentSteps: AgentStep[] = [
  {
    task_id: "task_complaint_demo",
    run_id: "run_mock_complaint",
    step_id: "step_plan",
    agent_name: "Planner",
    step_name: "Plan complaint analysis",
    status: "success",
    started_at: "2026-06-01T08:10:01Z",
    finished_at: "2026-06-01T08:10:03Z",
    output_summary: "Selected complaint trend review, SLA scan, and risk summary steps."
  },
  {
    task_id: "task_complaint_demo",
    run_id: "run_mock_complaint",
    step_id: "step_retrieve",
    agent_name: "Retriever",
    step_name: "Retrieve tickets and knowledge",
    status: "success",
    started_at: "2026-06-01T08:10:04Z",
    finished_at: "2026-06-01T08:10:08Z",
    output_summary: "Used Mock ticket.search.v1 and Mock knowledge.search.v1 sources.",
    citations: [
      {
        document_id: "doc_sop_mock",
        chunk_id: "chunk_mock_01",
        source_title: "Mock complaint SOP",
        source_uri: "mock://knowledge/doc_sop_mock#chunk_mock_01"
      }
    ]
  },
  {
    task_id: "task_complaint_demo",
    run_id: "run_mock_complaint",
    step_id: "step_risk",
    agent_name: "Risk",
    step_name: "Check high-risk conditions",
    status: "waiting_human",
    started_at: "2026-06-01T08:10:09Z",
    output_summary: "Detected urgent SLA and customer-impact risk. No action command was created."
  },
  {
    task_id: "task_complaint_demo",
    run_id: "run_mock_complaint",
    step_id: "step_report",
    agent_name: "Report",
    step_name: "Prepare report preview",
    status: "success",
    started_at: "2026-06-01T08:10:11Z",
    finished_at: "2026-06-01T08:10:14Z",
    output_summary: "Saved a local Mock report preview with facts, inferences, recommendations, and citations."
  }
];

export const mockReport: ReportSummary = {
  report_id: "report_mock_complaint",
  task_id: "task_complaint_demo",
  tenant_id: "tenant_demo",
  title: "Complaint trend preview",
  facts: [
    "Two complaint-related tickets are currently open or in progress for tenant_demo.",
    "One urgent ticket has an SLA deadline on 2026-06-01T10:30:00Z."
  ],
  inferences: [
    "Recent complaints are concentrated around delayed response handling.",
    "The urgent SLA ticket may need supervisor review before any customer-facing follow-up."
  ],
  recommendations: [
    "Ask an authorized operator to confirm the high-risk handling path.",
    "Review the Mock complaint SOP citation before sending any external response."
  ],
  citations: [
    {
      document_id: "doc_sop_mock",
      chunk_id: "chunk_mock_01",
      source_title: "Mock complaint SOP",
      source_uri: "mock://knowledge/doc_sop_mock#chunk_mock_01"
    },
    {
      document_id: "ticket_latency",
      chunk_id: "ticket_latency_summary",
      source_title: "Mock ticket.search.v1 result",
      source_uri: "mock://ticket.search.v1/ticket_latency_summary"
    }
  ]
};
