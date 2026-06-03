from __future__ import annotations

from agent_service.graph import AgentGraphRunner
from agent_service.models import (
    AI_TASK_FAILED,
    AgentTask,
    EvalCase,
    EvalResult,
    new_id,
)
from agent_service.store import LocalRunStore
from agent_service.tools import TicketSearchToolClient


def default_eval_cases() -> list[EvalCase]:
    return [
        EvalCase(
            case_id="workflow_completed_with_human_boundary",
            task=AgentTask(
                task_id=new_id("task"),
                tenant_id="tenant_demo",
                created_by="user_ops_demo",
                task_type="complaint_analysis",
                prompt="Analyze billing complaint growth and identify high-risk customers.",
                priority="high",
                trace_id="trace_eval_success",
            ),
            expect_status="waiting_approval",
            expect_waiting_human=True,
            expect_tool_success=True,
            expect_approval_requested=True,
        ),
        EvalCase(
            case_id="ticket_tool_failure_path",
            task=AgentTask(
                task_id=new_id("task"),
                tenant_id="tenant_demo",
                created_by="user_ops_demo",
                task_type="complaint_analysis",
                prompt="Analyze complaint trend with unavailable ticket tool.",
                priority="normal",
                trace_id="trace_eval_failure",
            ),
            expect_status=AI_TASK_FAILED,
            expect_waiting_human=False,
            expect_tool_success=False,
            expect_approval_requested=False,
        ),
    ]


def run_eval_case(case: EvalCase) -> EvalResult:
    fail_ticket_tool = not case.expect_tool_success
    store = LocalRunStore()
    runner = AgentGraphRunner(store=store, ticket_tool=TicketSearchToolClient(fail=fail_ticket_tool))
    task = runner.run(case.task)
    checks = {
        "task_status": task.status == case.expect_status,
        "trace_id_propagated": all(record.trace_id == task.trace_id for record in store.tool_calls),
        "tenant_propagated": all(record.tenant_id == task.tenant_id for record in store.tool_calls),
        "waiting_human": any(step.status == "waiting_human" for step in store.steps) == case.expect_waiting_human,
        "source_boundaries_visible": all(record.source_type in {"mock_contract", "local_rag", "mock_report_store", "mock_workflow_contract"} for record in store.tool_calls),
        "approval_requested": (task.approval_instance_id is not None) == case.expect_approval_requested,
    }
    if case.expect_tool_success:
        checks["report_generated"] = task.report_id is not None
        checks["knowledge_search_uses_local_rag"] = any(
            record.tool_name == "knowledge.search.v1" and record.source_type == "local_rag"
            for record in store.tool_calls
        )
        checks["report_has_non_mock_rag_citation"] = any(
            not citation.mock_source and citation.source_uri
            for report in store.reports.values()
            for citation in report.citations
        )
    else:
        checks["failure_has_error"] = task.error is not None and task.error.trace_id == task.trace_id and task.error.retryable
    return EvalResult(
        case_id=case.case_id,
        passed=all(checks.values()),
        checks=checks,
        task_status=task.status,
        report_id=task.report_id,
    )


def run_default_evals() -> list[EvalResult]:
    return [run_eval_case(case) for case in default_eval_cases()]
