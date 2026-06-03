from __future__ import annotations

from dataclasses import asdict, dataclass, field
from datetime import UTC, datetime
from typing import Any
from uuid import uuid4


AI_TASK_PENDING = "pending"
AI_TASK_RUNNING = "running"
AI_TASK_WAITING_APPROVAL = "waiting_approval"
AI_TASK_SUCCESS = "success"
AI_TASK_FAILED = "failed"

RUN_CREATED = "created"
RUN_STARTED = "started"
RUN_STEP_RUNNING = "step_running"
RUN_COMPLETED = "completed"
RUN_FAILED = "failed"

STEP_PENDING = "pending"
STEP_RUNNING = "running"
STEP_SUCCESS = "success"
STEP_FAILED = "failed"
STEP_WAITING_HUMAN = "waiting_human"


def utc_now() -> str:
    return datetime.now(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def new_id(prefix: str) -> str:
    return f"{prefix}_{uuid4().hex[:12]}"


@dataclass
class ErrorInfo:
    error_code: str
    message: str
    trace_id: str
    retryable: bool
    details: dict[str, Any] = field(default_factory=dict)


@dataclass
class Citation:
    document_id: str
    chunk_id: str
    source_title: str
    source_uri: str | None = None
    mock_source: bool = False


@dataclass
class AgentTask:
    task_id: str
    tenant_id: str
    created_by: str
    task_type: str
    prompt: str
    priority: str
    trace_id: str
    status: str = AI_TASK_PENDING
    run_id: str | None = None
    approval_instance_id: str | None = None
    report_id: str | None = None
    blocking_reason: str | None = None
    error: ErrorInfo | None = None
    retryable: bool | None = None
    created_at: str = field(default_factory=utc_now)


@dataclass
class AgentRun:
    run_id: str
    task_id: str
    tenant_id: str
    trace_id: str
    graph_name: str
    graph_version: str
    status: str = RUN_CREATED
    current_step: str | None = None
    started_at: str | None = None
    finished_at: str | None = None
    report_id: str | None = None
    error: ErrorInfo | None = None


@dataclass
class AgentStep:
    task_id: str
    run_id: str
    step_id: str
    agent_name: str
    step_name: str
    status: str = STEP_PENDING
    input_summary: str | None = None
    output_summary: str | None = None
    citations: list[Citation] = field(default_factory=list)
    error: ErrorInfo | None = None
    started_at: str | None = None
    finished_at: str | None = None


@dataclass
class ToolCallRecord:
    tool_name: str
    tool_version: str
    tenant_id: str
    requested_by: str
    trace_id: str
    run_id: str
    status: str
    source_type: str
    request: dict[str, Any]
    response: dict[str, Any] | None = None
    error: ErrorInfo | None = None
    attempts: int = 1
    started_at: str = field(default_factory=utc_now)
    finished_at: str | None = None


@dataclass
class RiskAssessment:
    level: str
    confidence: float
    requires_human: bool
    reasons: list[str]


@dataclass
class ReportDraft:
    task_id: str
    tenant_id: str
    title: str
    facts: list[str]
    inferences: list[str]
    risks: list[str]
    recommendations: list[str]
    citations: list[Citation]
    report_id: str | None = None
    storage_mode: str = "mock_local"


@dataclass
class EvalCase:
    case_id: str
    task: AgentTask
    expect_status: str
    expect_waiting_human: bool
    expect_tool_success: bool
    expect_approval_requested: bool


@dataclass
class EvalResult:
    case_id: str
    passed: bool
    checks: dict[str, bool]
    task_status: str
    report_id: str | None


def to_dict(value: Any) -> Any:
    if hasattr(value, "__dataclass_fields__"):
        return {key: to_dict(item) for key, item in asdict(value).items()}
    if isinstance(value, list):
        return [to_dict(item) for item in value]
    if isinstance(value, dict):
        return {key: to_dict(item) for key, item in value.items()}
    return value
