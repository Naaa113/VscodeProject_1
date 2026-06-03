from __future__ import annotations

from agent_service.models import (
    RUN_COMPLETED,
    RUN_FAILED,
    RUN_STARTED,
    RUN_STEP_RUNNING,
    STEP_FAILED,
    STEP_RUNNING,
    AgentRun,
    AgentStep,
    AgentTask,
    ErrorInfo,
    ReportDraft,
    ToolCallRecord,
    new_id,
    utc_now,
)


class LocalRunStore:
    def __init__(self) -> None:
        self.tasks: dict[str, AgentTask] = {}
        self.runs: dict[str, AgentRun] = {}
        self.steps: list[AgentStep] = []
        self.tool_calls: list[ToolCallRecord] = []
        self.reports: dict[str, ReportDraft] = {}
        self.events: list[dict] = []

    def _record_event(self, event_name: str, tenant_id: str, trace_id: str, payload: dict) -> None:
        self.events.append(
            {
                "event_id": new_id("event"),
                "event_name": event_name,
                "event_version": "v1",
                "occurred_at": utc_now(),
                "tenant_id": tenant_id,
                "trace_id": trace_id,
                "producer": "agent-service",
                "consumers": ["java-task-entrypoint", "web-console-stream"],
                "payload": payload,
            }
        )

    def add_task(self, task: AgentTask) -> AgentTask:
        self.tasks[task.task_id] = task
        return task

    def create_run(self, task: AgentTask, graph_name: str, graph_version: str) -> AgentRun:
        run = AgentRun(
            run_id=new_id("run"),
            task_id=task.task_id,
            tenant_id=task.tenant_id,
            trace_id=task.trace_id,
            graph_name=graph_name,
            graph_version=graph_version,
        )
        self.runs[run.run_id] = run
        task.run_id = run.run_id
        return run

    def start_run(self, run: AgentRun) -> None:
        run.status = RUN_STARTED
        run.started_at = utc_now()
        self._record_event(
            "agent.run.started.v1",
            run.tenant_id,
            run.trace_id,
            {
                "task_id": run.task_id,
                "run_id": run.run_id,
                "graph_name": run.graph_name,
                "graph_version": run.graph_version,
                "status": "started",
                "started_at": run.started_at,
            },
        )

    def finish_run(self, run: AgentRun, report_id: str | None) -> None:
        run.status = RUN_COMPLETED
        run.report_id = report_id
        run.finished_at = utc_now()
        self._record_event(
            "agent.run.completed.v1",
            run.tenant_id,
            run.trace_id,
            {
                "task_id": run.task_id,
                "run_id": run.run_id,
                "report_id": report_id,
                "status": "completed",
                "finished_at": run.finished_at,
            },
        )

    def fail_run(self, run: AgentRun, error: ErrorInfo) -> None:
        run.status = RUN_FAILED
        run.error = error
        run.finished_at = utc_now()
        self._record_event(
            "agent.run.failed.v1",
            run.tenant_id,
            run.trace_id,
            {
                "task_id": run.task_id,
                "run_id": run.run_id,
                "error_code": error.error_code,
                "message": error.message,
                "retryable": error.retryable,
                "failed_at": run.finished_at,
            },
        )

    def start_step(
        self,
        run: AgentRun,
        task: AgentTask,
        agent_name: str,
        step_name: str,
        input_summary: str,
    ) -> AgentStep:
        run.status = RUN_STEP_RUNNING
        run.current_step = step_name
        step = AgentStep(
            task_id=task.task_id,
            run_id=run.run_id,
            step_id=new_id("step"),
            agent_name=agent_name,
            step_name=step_name,
            status=STEP_RUNNING,
            input_summary=input_summary,
            started_at=utc_now(),
        )
        self.steps.append(step)
        return step

    def finish_step(self, step: AgentStep, status: str, output_summary: str, citations=None, error=None) -> None:
        step.status = status
        step.output_summary = output_summary
        step.finished_at = utc_now()
        step.citations = citations or []
        step.error = error
        run = self.runs[step.run_id]
        self._record_event(
            "agent.step.completed.v1",
            run.tenant_id,
            run.trace_id,
            {
                "task_id": step.task_id,
                "run_id": step.run_id,
                "step_id": step.step_id,
                "agent_name": step.agent_name,
                "step_name": step.step_name,
                "status": step.status,
                "summary": step.output_summary,
                "citations": [citation.__dict__ for citation in step.citations],
                "error_code": error.error_code if error else None,
                "message": error.message if error else None,
                "retryable": error.retryable if error else None,
                "started_at": step.started_at,
                "finished_at": step.finished_at,
            },
        )

    def fail_step(self, step: AgentStep, error: ErrorInfo) -> None:
        self.finish_step(step, STEP_FAILED, error.message, error=error)

    def add_tool_call(self, record: ToolCallRecord) -> ToolCallRecord:
        record.finished_at = utc_now()
        self.tool_calls.append(record)
        return record

    def save_report(self, report: ReportDraft) -> ReportDraft:
        report.report_id = report.report_id or new_id("report")
        self.reports[report.report_id] = report
        return report
