import {
  CheckCheck,
  AlertTriangle,
  ArrowLeft,
  Bot,
  CheckCircle2,
  Clock3,
  FileText,
  LogOut,
  Search,
  ShieldAlert,
  Ticket,
  UserRound,
  Zap
} from "lucide-react";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { ApiError, type ApiClients, type SessionContext } from "../api/clients";
import type {
  ActionCommand,
  AgentStep,
  AiTask,
  ApprovalInstance,
  Citation,
  CurrentUserResponse,
  ErrorResponse,
  ReportSummary,
  TicketDetail,
  TicketSummary
} from "../api/contracts";
import { mockApiClients } from "../mocks/mockClients";

type Route =
  | { name: "login" }
  | { name: "tickets" }
  | { name: "ticket-detail"; ticketId: string }
  | { name: "task-detail"; taskId: string };

type LoadState<T> =
  | { status: "idle" | "loading" }
  | { status: "empty" }
  | { status: "loaded"; data: T }
  | { status: "error"; error: ErrorResponse };

const traceId = "trace_web_console_mock";

type DisplayCitation = Citation & {
  isMockSource: boolean;
};

type ReportPreviewModel = {
  facts: string[];
  inferences: string[];
  risks: string[];
  recommendations: string[];
  citations: DisplayCitation[];
};

type ReportPanelState =
  | { status: "loading" }
  | { status: "available"; report: ReportSummary }
  | { status: "waiting_approval"; message: string }
  | { status: "not_ready"; message: string }
  | { status: "not_found"; error: ErrorResponse }
  | { status: "error"; error: ErrorResponse };

const toErrorResponse = (error: unknown): ErrorResponse => {
  if (error instanceof ApiError) {
    return {
      error_code: error.error_code,
      message: error.message,
      trace_id: error.trace_id,
      retryable: error.retryable
    };
  }

  return {
    error_code: "INTERNAL_ERROR",
    message: "Unexpected frontend error.",
    trace_id: traceId,
    retryable: false
  };
};

const readRoute = (): Route => {
  const path = window.location.pathname;
  const taskMatch = path.match(/^\/tasks\/([^/]+)$/);
  if (taskMatch) {
    return { name: "task-detail", taskId: taskMatch[1] };
  }

  const ticketMatch = path.match(/^\/tickets\/([^/]+)$/);
  if (ticketMatch) {
    return { name: "ticket-detail", ticketId: ticketMatch[1] };
  }

  if (path === "/tickets" || path === "/") {
    return { name: "tickets" };
  }

  return { name: "login" };
};

const navigate = (route: Route) => {
  const path =
    route.name === "tickets"
      ? "/tickets"
      : route.name === "login"
        ? "/login"
        : route.name === "ticket-detail"
          ? `/tickets/${route.ticketId}`
          : `/tasks/${route.taskId}`;
  window.history.pushState(null, "", path);
  window.dispatchEvent(new PopStateEvent("popstate"));
};

const isMockSourceUri = (sourceUri?: string | null) => Boolean(sourceUri?.startsWith("mock://"));

const toDisplayCitation = (citation: Citation): DisplayCitation => ({
  ...citation,
  isMockSource: isMockSourceUri(citation.source_uri)
});

export const buildRiskItems = (task: AiTask | null, steps: AgentStep[]) => {
  const items = new Set<string>();

  if (task?.blocking_reason) {
    items.add(task.blocking_reason);
  }

  for (const step of steps) {
    if (step.status === "waiting_human" && step.output_summary) {
      items.add(step.output_summary);
    }

    if (step.error) {
      items.add(`${step.agent_name}: ${step.error.message}`);
    }
  }

  return [...items];
};

const approvalStatusLabel = (status: ApprovalInstance["status"]) =>
  ({
    pending: "等待审批",
    approved: "审批通过",
    rejected: "审批拒绝",
    cancelled: "审批取消",
    expired: "审批过期"
  })[status];

const actionStatusLabel = (status: ActionCommand["status"]) =>
  ({
    pending: "动作处理中",
    success: "动作成功",
    failed: "动作失败",
    cancelled: "动作取消"
  })[status];

export const buildReportPreviewModel = (report: ReportSummary, task: AiTask | null, steps: AgentStep[]): ReportPreviewModel => ({
  facts: report.facts,
  inferences: report.inferences,
  risks: buildRiskItems(task, steps),
  recommendations: report.recommendations,
  citations: report.citations.map(toDisplayCitation)
});

export const buildReportPanelState = (task: AiTask | null, reportState: LoadState<ReportSummary>): ReportPanelState => {
  if (reportState.status === "loading") {
    return { status: "loading" };
  }

  if (reportState.status === "loaded") {
    return { status: "available", report: reportState.data };
  }

  if (reportState.status === "error") {
    if (reportState.error.error_code === "RESOURCE_NOT_FOUND") {
      if (task?.status === "waiting_approval") {
        return {
          status: "waiting_approval",
          message: task.blocking_reason ?? "任务正在等待人工确认，报告将在确认后生成。"
        };
      }

      if (task?.status === "pending" || task?.status === "running") {
        return {
          status: "not_ready",
          message: "任务仍在处理中，报告尚未生成。"
        };
      }

      return {
        status: "not_found",
        error: reportState.error
      };
    }

    return { status: "error", error: reportState.error };
  }

  if (task?.status === "waiting_approval") {
    return {
      status: "waiting_approval",
      message: task.blocking_reason ?? "任务正在等待人工确认，报告将在确认后生成。"
    };
  }

  return {
    status: "not_ready",
    message: "报告尚未生成。"
  };
};

export function App({ clients = mockApiClients }: { clients?: ApiClients }) {
  const [route, setRoute] = useState<Route>(readRoute);
  const [session, setSession] = useState<SessionContext | null>(null);
  const [user, setUser] = useState<CurrentUserResponse | null>(null);

  useEffect(() => {
    const onRouteChange = () => setRoute(readRoute());
    window.addEventListener("popstate", onRouteChange);
    return () => window.removeEventListener("popstate", onRouteChange);
  }, []);

  const handleAuthenticated = (nextSession: SessionContext, nextUser: CurrentUserResponse) => {
    setSession(nextSession);
    setUser(nextUser);
    navigate({ name: "tickets" });
  };

  const logout = () => {
    setSession(null);
    setUser(null);
    navigate({ name: "login" });
  };

  if (!session || !user || route.name === "login") {
    return <LoginPage clients={clients} onAuthenticated={handleAuthenticated} />;
  }

  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="Primary">
        <div className="brand">
          <Zap aria-hidden="true" />
          <div>
            <strong>AgentOps Hub</strong>
            <span>tenant_demo</span>
          </div>
        </div>
        <button className="nav-button active" type="button" onClick={() => navigate({ name: "tickets" })}>
          <Ticket aria-hidden="true" />
          工单
        </button>
        <button className="nav-button muted" type="button" disabled>
          <Bot aria-hidden="true" />
          Agent
        </button>
        <button className="nav-button muted" type="button" disabled>
          <FileText aria-hidden="true" />
          报告
        </button>
        <div className="mock-boundary">Mock adapter: Phase 006</div>
      </aside>
      <main className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">运营工作台</p>
            <h1>投诉处理闭环</h1>
          </div>
          <div className="user-strip">
            <UserRound aria-hidden="true" />
            <span>{user.username}</span>
            <button className="icon-button" type="button" onClick={logout} aria-label="退出登录" title="退出登录">
              <LogOut aria-hidden="true" />
            </button>
          </div>
        </header>
        {route.name === "tickets" ? (
          <TicketsPage clients={clients} session={session} />
        ) : route.name === "ticket-detail" ? (
          <TicketDetailPage clients={clients} session={session} ticketId={route.ticketId} />
        ) : (
          <TaskPage clients={clients} session={session} taskId={route.taskId} />
        )}
      </main>
    </div>
  );
}

function LoginPage({
  clients,
  onAuthenticated
}: {
  clients: ApiClients;
  onAuthenticated: (session: SessionContext, user: CurrentUserResponse) => void;
}) {
  const [tenantId, setTenantId] = useState("tenant_demo");
  const [username, setUsername] = useState("demo.user");
  const [password, setPassword] = useState("demo-password");
  const [state, setState] = useState<LoadState<CurrentUserResponse>>({ status: "idle" });

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setState({ status: "loading" });
    try {
      const login = await clients.auth.login({ tenant_id: tenantId, username, password });
      const nextSession = {
        accessToken: login.access_token,
        tenantId: login.tenant_id,
        traceId
      };
      const currentUser = await clients.auth.currentUser(nextSession);
      setState({ status: "loaded", data: currentUser });
      onAuthenticated(nextSession, currentUser);
    } catch (error) {
      setState({ status: "error", error: toErrorResponse(error) });
    }
  };

  return (
    <main className="login-screen">
      <section className="login-panel" aria-label="Login">
        <div className="login-copy">
          <p className="eyebrow">AgentOps Hub</p>
          <h1>企业智能运营协同平台</h1>
          <p>Mock 登录联调</p>
        </div>
        <form className="login-form" onSubmit={submit}>
          <label>
            租户
            <input value={tenantId} onChange={(event) => setTenantId(event.target.value)} />
          </label>
          <label>
            用户名
            <input value={username} onChange={(event) => setUsername(event.target.value)} />
          </label>
          <label>
            密码
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
          </label>
          <button className="primary-button" type="submit" disabled={state.status === "loading"}>
            <CheckCircle2 aria-hidden="true" />
            {state.status === "loading" ? "登录中" : "进入工作台"}
          </button>
          {state.status === "error" ? <ErrorPanel error={state.error} /> : null}
        </form>
      </section>
    </main>
  );
}

function TicketsPage({ clients, session }: { clients: ApiClients; session: SessionContext }) {
  const [query, setQuery] = useState("");
  const [tickets, setTickets] = useState<LoadState<TicketSummary[]>>({ status: "loading" });

  useEffect(() => {
    let alive = true;
    setTickets({ status: "loading" });
    clients.tickets
      .listTickets(session, query)
      .then((response) => {
        if (!alive) return;
        setTickets(response.items.length === 0 ? { status: "empty" } : { status: "loaded", data: response.items });
      })
      .catch((error) => alive && setTickets({ status: "error", error: toErrorResponse(error) }));
    return () => {
      alive = false;
    };
  }, [clients.tickets, query, session]);

  return (
    <section className="content-grid two-columns">
      <div className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Tickets</p>
            <h2>工单列表</h2>
          </div>
          <div className="search-box">
            <Search aria-hidden="true" />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索客户、分类或状态" />
          </div>
        </div>
        {tickets.status === "loading" ? <SkeletonRows /> : null}
        {tickets.status === "empty" ? <EmptyState text="没有匹配的工单" /> : null}
        {tickets.status === "error" ? <ErrorPanel error={tickets.error} /> : null}
        {tickets.status === "loaded" ? (
          <div className="ticket-list">
            {tickets.data.map((ticket) => (
              <button className="ticket-row" key={ticket.ticket_id} type="button" onClick={() => navigate({ name: "ticket-detail", ticketId: ticket.ticket_id })}>
                <span className={`priority ${ticket.priority}`}>{ticket.priority}</span>
                <span>
                  <strong>{ticket.title}</strong>
                  <small>{ticket.customer.display_name}</small>
                </span>
                <span className="status-pill">{ticket.status}</span>
              </button>
            ))}
          </div>
        ) : null}
      </div>
      <div className="panel accent-panel">
        <p className="eyebrow">Agent Entry</p>
        <h2>投诉分析任务</h2>
        <p className="muted-text">基于公开契约和 Mock fixture 生成可审计预览。</p>
        <button className="primary-button" type="button" onClick={() => navigate({ name: "task-detail", taskId: "task_complaint_demo" })}>
          <Bot aria-hidden="true" />
          查看样例任务
        </button>
      </div>
    </section>
  );
}

function TicketDetailPage({ clients, session, ticketId }: { clients: ApiClients; session: SessionContext; ticketId: string }) {
  const [ticket, setTicket] = useState<LoadState<TicketDetail>>({ status: "loading" });
  const [taskState, setTaskState] = useState<LoadState<AiTask>>({ status: "idle" });

  useEffect(() => {
    let alive = true;
    setTicket({ status: "loading" });
    clients.tickets
      .getTicket(session, ticketId)
      .then((data) => alive && setTicket({ status: "loaded", data }))
      .catch((error) => alive && setTicket({ status: "error", error: toErrorResponse(error) }));
    return () => {
      alive = false;
    };
  }, [clients.tickets, session, ticketId]);

  const createTask = async () => {
    setTaskState({ status: "loading" });
    try {
      const task = await clients.aiTasks.createTask(session, {
        task_type: "complaint_analysis",
        prompt: `Analyze ticket ${ticketId} and related complaint context.`,
        priority: "high"
      });
      setTaskState({ status: "loaded", data: task });
      navigate({ name: "task-detail", taskId: task.task_id });
    } catch (error) {
      setTaskState({ status: "error", error: toErrorResponse(error) });
    }
  };

  return (
    <section className="panel wide-panel">
      <button className="ghost-button" type="button" onClick={() => navigate({ name: "tickets" })}>
        <ArrowLeft aria-hidden="true" />
        返回
      </button>
      {ticket.status === "loading" ? <SkeletonRows /> : null}
      {ticket.status === "error" ? <ErrorPanel error={ticket.error} /> : null}
      {ticket.status === "loaded" ? (
        <div className="detail-layout">
          <div>
            <p className="eyebrow">{ticket.data.category}</p>
            <h2>{ticket.data.title}</h2>
            <p>{ticket.data.description}</p>
            <dl className="meta-grid">
              <div>
                <dt>客户</dt>
                <dd>{ticket.data.customer.display_name}</dd>
              </div>
              <div>
                <dt>状态</dt>
                <dd>{ticket.data.status}</dd>
              </div>
              <div>
                <dt>优先级</dt>
                <dd>{ticket.data.priority}</dd>
              </div>
              <div>
                <dt>SLA</dt>
                <dd>{formatDate(ticket.data.sla_due_at)}</dd>
              </div>
            </dl>
          </div>
          <div className="action-panel">
            <ShieldAlert aria-hidden="true" />
            <h3>风险边界</h3>
            <p>任务可以进入等待人工确认；Phase 008 会创建审批实例，但不会在审批通过前执行跟进动作。</p>
            <button className="primary-button" type="button" onClick={createTask} disabled={taskState.status === "loading"}>
              <Bot aria-hidden="true" />
              {taskState.status === "loading" ? "发起中" : "发起投诉分析"}
            </button>
            {taskState.status === "error" ? <ErrorPanel error={taskState.error} /> : null}
          </div>
        </div>
      ) : null}
    </section>
  );
}

function TaskPage({ clients, session, taskId }: { clients: ApiClients; session: SessionContext; taskId: string }) {
  const [task, setTask] = useState<LoadState<AiTask>>({ status: "loading" });
  const [steps, setSteps] = useState<LoadState<AgentStep[]>>({ status: "loading" });
  const [report, setReport] = useState<LoadState<ReportSummary>>({ status: "loading" });
  const [approval, setApproval] = useState<LoadState<ApprovalInstance>>({ status: "idle" });
  const [action, setAction] = useState<LoadState<ActionCommand>>({ status: "idle" });

  useEffect(() => {
    let alive = true;
    setTask({ status: "loading" });
    setSteps({ status: "loading" });
    setReport({ status: "loading" });

    clients.aiTasks
      .getTask(session, taskId)
      .then((data) => alive && setTask({ status: "loaded", data }))
      .catch((error) => alive && setTask({ status: "error", error: toErrorResponse(error) }));
    clients.aiTasks
      .listSteps(session, taskId)
      .then((data) => alive && setSteps(data.items.length === 0 ? { status: "empty" } : { status: "loaded", data: data.items }))
      .catch((error) => alive && setSteps({ status: "error", error: toErrorResponse(error) }));
    clients.aiTasks
      .getReport(session, taskId)
      .then((data) => alive && setReport({ status: "loaded", data }))
      .catch((error) => alive && setReport({ status: "error", error: toErrorResponse(error) }));

    return () => {
      alive = false;
    };
  }, [clients.aiTasks, session, taskId]);

  const taskData = task.status === "loaded" ? task.data : null;

  useEffect(() => {
    let alive = true;
    if (!taskData?.approval_instance_id) {
      setApproval({ status: "idle" });
      setAction({ status: "idle" });
      return () => {
        alive = false;
      };
    }

    setApproval({ status: "loading" });
    setAction({ status: "idle" });
    clients.workflow
      .getApproval(session, taskData.approval_instance_id)
      .then((data) => {
        if (!alive) return;
        setApproval({ status: "loaded", data });
        if (data.action_command_id) {
          setAction({ status: "loading" });
          clients.workflow
            .getActionCommand(session, data.action_command_id)
            .then((actionData) => alive && setAction({ status: "loaded", data: actionData }))
            .catch((error) => alive && setAction({ status: "error", error: toErrorResponse(error) }));
        } else {
          setAction({ status: "idle" });
        }
      })
      .catch((error) => alive && setApproval({ status: "error", error: toErrorResponse(error) }));

    return () => {
      alive = false;
    };
  }, [clients.workflow, session, taskData?.approval_instance_id]);

  const applyApprovalTaskProjection = (nextApproval: ApprovalInstance) => {
    if (task.status !== "loaded") {
      return;
    }

    const nextTask: AiTask =
      nextApproval.status === "approved"
        ? { ...task.data, status: "success", blocking_reason: null }
        : nextApproval.status === "rejected"
          ? { ...task.data, status: "failed", blocking_reason: "审批已拒绝，未创建跟进动作。" }
          : nextApproval.status === "cancelled"
            ? { ...task.data, status: "cancelled", blocking_reason: "审批请求已取消。" }
            : task.data;
    setTask({ status: "loaded", data: nextTask });
  };

  const submitDecision = async (decision: "approve" | "reject" | "cancel") => {
    if (approval.status !== "loaded") {
      return;
    }

    const request = {
      reason:
        decision === "approve"
          ? "运营负责人确认可以创建跟进动作。"
          : decision === "reject"
            ? "运营负责人拒绝该跟进动作。"
            : "申请人取消该审批请求。"
    };

    setApproval({ status: "loading" });
    try {
      const nextApproval =
        decision === "approve"
          ? await clients.workflow.approveApproval(session, approval.data.approval_instance_id, request)
          : decision === "reject"
            ? await clients.workflow.rejectApproval(session, approval.data.approval_instance_id, request)
            : await clients.workflow.cancelApproval(session, approval.data.approval_instance_id, request);
      setApproval({ status: "loaded", data: nextApproval });
      applyApprovalTaskProjection(nextApproval);

      if (nextApproval.action_command_id) {
        setAction({ status: "loading" });
        const nextAction = await clients.workflow.getActionCommand(session, nextApproval.action_command_id);
        setAction({ status: "loaded", data: nextAction });
      } else {
        setAction({ status: "idle" });
      }
    } catch (error) {
      setApproval({ status: "error", error: toErrorResponse(error) });
    }
  };

  return (
    <section className="content-grid task-grid">
      <div className="panel">
        <button className="ghost-button" type="button" onClick={() => navigate({ name: "tickets" })}>
          <ArrowLeft aria-hidden="true" />
          返回
        </button>
        <div className="panel-heading compact">
          <div>
            <p className="eyebrow">AI Task</p>
            <h2>{taskData?.task_type ?? "任务"}</h2>
          </div>
          {taskData ? <span className={`task-status ${taskData.status}`}>{taskData.status}</span> : null}
        </div>
        {task.status === "loading" ? <SkeletonRows /> : null}
        {task.status === "error" ? <ErrorPanel error={task.error} /> : null}
        {taskData?.blocking_reason ? (
          <div className="risk-banner">
            <AlertTriangle aria-hidden="true" />
            <span>{taskData.blocking_reason}</span>
          </div>
        ) : null}
        <h3>Agent 步骤</h3>
        {steps.status === "loading" ? <SkeletonRows /> : null}
        {steps.status === "empty" ? <EmptyState text="暂无步骤" /> : null}
        {steps.status === "error" ? <ErrorPanel error={steps.error} /> : null}
        {steps.status === "loaded" ? <StepTimeline steps={steps.data} /> : null}
        <ApprovalPanel approval={approval} action={action} onDecision={submitDecision} />
      </div>
      <div className="panel">
        <p className="eyebrow">Report Preview</p>
        <h2>报告预览</h2>
        <ReportPanel task={taskData} steps={steps.status === "loaded" ? steps.data : []} reportState={report} />
      </div>
    </section>
  );
}

export function StepTimeline({ steps }: { steps: AgentStep[] }) {
  return (
    <ol className="step-list">
      {steps.map((step) => (
        <li key={step.step_id} className={`step-item ${step.status}`}>
          <div className="step-icon">
            {step.status === "failed" ? (
              <AlertTriangle aria-hidden="true" />
            ) : step.status === "waiting_human" ? (
              <Clock3 aria-hidden="true" />
            ) : (
              <CheckCircle2 aria-hidden="true" />
            )}
          </div>
          <div>
            <strong>{step.agent_name}</strong>
            <span>{step.step_name}</span>
            {step.output_summary ? <p>{step.output_summary}</p> : null}
            {step.error ? (
              <div className="step-error" role="alert">
                <strong>{step.error.error_code}</strong>
                <span>{step.error.message}</span>
                <small>{step.error.retryable ? "可重试" : "不可重试"}</small>
              </div>
            ) : null}
            {step.citations?.length ? <CitationList citations={step.citations.map(toDisplayCitation)} /> : null}
          </div>
          <small>{step.status}</small>
        </li>
      ))}
    </ol>
  );
}

function ApprovalPanel({
  approval,
  action,
  onDecision
}: {
  approval: LoadState<ApprovalInstance>;
  action: LoadState<ActionCommand>;
  onDecision: (decision: "approve" | "reject" | "cancel") => void;
}) {
  if (approval.status === "idle") {
    return null;
  }

  if (approval.status === "loading") {
    return (
      <section className="approval-panel">
        <p className="eyebrow">Approval</p>
        <h3>审批状态</h3>
        <SkeletonRows />
      </section>
    );
  }

  if (approval.status === "error") {
    return (
      <section className="approval-panel">
        <p className="eyebrow">Approval</p>
        <h3>审批状态</h3>
        <ErrorPanel error={approval.error} />
      </section>
    );
  }

  return (
    <section className="approval-panel">
      <div className="panel-heading compact">
        <div>
          <p className="eyebrow">Approval</p>
          <h3>审批状态</h3>
        </div>
        <span className={`task-status ${approval.data.status}`}>{approvalStatusLabel(approval.data.status)}</span>
      </div>
      <div className="approval-card">
        <strong>{approval.data.action_input.title}</strong>
        <span>{approval.data.risk_reason}</span>
        <small>approval_id: {approval.data.approval_instance_id}</small>
        {approval.data.latest_record ? (
          <small>
            最新记录: {approval.data.latest_record.decision} · {approval.data.latest_record.reason}
          </small>
        ) : null}
        {approval.data.citations.length ? (
          <CitationList citations={approval.data.citations.map(toDisplayCitation)} />
        ) : null}
      </div>
      {approval.data.status === "pending" ? (
        <div className="approval-actions">
          <button className="primary-button" type="button" onClick={() => onDecision("approve")}>
            <CheckCheck aria-hidden="true" />
            批准
          </button>
          <button className="ghost-button" type="button" onClick={() => onDecision("reject")}>
            拒绝
          </button>
          <button className="ghost-button" type="button" onClick={() => onDecision("cancel")}>
            取消
          </button>
        </div>
      ) : null}
      {action.status === "loading" ? <SkeletonRows /> : null}
      {action.status === "error" ? <ErrorPanel error={action.error} /> : null}
      {action.status === "loaded" ? (
        <div className={`approval-card action ${action.data.status}`}>
          <strong>{actionStatusLabel(action.data.status)}</strong>
          {action.data.result_payload ? (
            <span>followup_ref: {action.data.result_payload.followup_task_ref}</span>
          ) : null}
          {action.data.error ? (
            <small>
              {action.data.error.error_code} · {action.data.error.message}
            </small>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}

export function ReportPreview({ report, task, steps }: { report: ReportSummary; task: AiTask | null; steps: AgentStep[] }) {
  const preview = useMemo(() => buildReportPreviewModel(report, task, steps), [report, steps, task]);
  const sections = useMemo<Array<[string, string[]]>>(
    () => [
      ["事实", preview.facts],
      ["推断", preview.inferences],
      ["风险", preview.risks],
      ["建议", preview.recommendations]
    ],
    [preview]
  );

  return (
    <div className="report">
      {sections.map(([title, items]) => (
        <section key={title}>
          <h3>{title}</h3>
          <ul>
            {items.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </section>
      ))}
      <section>
        <h3>引用</h3>
        <CitationList citations={preview.citations} />
      </section>
    </div>
  );
}

export function ReportPanel({
  task,
  steps,
  reportState
}: {
  task: AiTask | null;
  steps: AgentStep[];
  reportState: LoadState<ReportSummary>;
}) {
  const state = useMemo(() => buildReportPanelState(task, reportState), [reportState, task]);

  if (state.status === "loading") {
    return <SkeletonRows />;
  }

  if (state.status === "available") {
    return <ReportPreview report={state.report} task={task} steps={steps} />;
  }

  if (state.status === "waiting_approval") {
    return (
      <ReportStatePanel
        title="等待人工确认"
        tone="warning"
        icon={<Clock3 aria-hidden="true" />}
        message={state.message}
        detail="当前任务已进入 waiting_approval，审批结果和动作结果会在审批面板中展示。"
      />
    );
  }

  if (state.status === "not_ready") {
    return (
      <ReportStatePanel
        title="报告尚未生成"
        tone="neutral"
        icon={<Clock3 aria-hidden="true" />}
        message={state.message}
        detail="请等待任务完成后再查看结构化报告预览。"
      />
    );
  }

  if (state.status === "not_found") {
    return (
      <ReportStatePanel
        title="报告不存在或不可访问"
        tone="neutral"
        icon={<FileText aria-hidden="true" />}
        message="当前租户下没有可显示的报告，或该报告尚未对当前会话开放。"
        detail={`trace: ${state.error.trace_id} · ${state.error.error_code}`}
      />
    );
  }

  return <ErrorPanel error={state.error} />;
}

function CitationList({ citations }: { citations: DisplayCitation[] }) {
  return (
    <ul className="citation-list">
      {citations.map((citation) => (
        <li key={`${citation.document_id}-${citation.chunk_id}`}>
          <FileText aria-hidden="true" />
          <div className="citation-copy">
            <span>{citation.source_title}</span>
            <small>{citation.chunk_id}</small>
          </div>
          {citation.isMockSource ? <em>Mock</em> : null}
        </li>
      ))}
    </ul>
  );
}

function ReportStatePanel({
  title,
  message,
  detail,
  tone,
  icon
}: {
  title: string;
  message: string;
  detail: string;
  tone: "warning" | "neutral";
  icon: JSX.Element;
}) {
  return (
    <div className={`report-state-panel ${tone}`} role="status">
      {icon}
      <div>
        <strong>{title}</strong>
        <span>{message}</span>
        <small>{detail}</small>
      </div>
    </div>
  );
}

function ErrorPanel({ error }: { error: ErrorResponse }) {
  return (
    <div className="error-panel" role="alert">
      <AlertTriangle aria-hidden="true" />
      <div>
        <strong>{error.error_code}</strong>
        <span>{error.message}</span>
        <small>
          trace: {error.trace_id} · {error.retryable ? "可重试" : "不可重试"}
        </small>
      </div>
    </div>
  );
}

function EmptyState({ text }: { text: string }) {
  return <div className="empty-state">{text}</div>;
}

function SkeletonRows() {
  return (
    <div className="skeleton-stack" aria-label="加载中">
      <span />
      <span />
      <span />
    </div>
  );
}

function formatDate(value?: string | null) {
  if (!value) {
    return "未设置";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}
