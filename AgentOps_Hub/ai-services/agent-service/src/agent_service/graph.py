from __future__ import annotations

from typing import Any

from agent_service.models import (
    AI_TASK_FAILED,
    AI_TASK_RUNNING,
    AI_TASK_SUCCESS,
    AI_TASK_WAITING_APPROVAL,
    STEP_SUCCESS,
    STEP_WAITING_HUMAN,
    AgentTask,
    ErrorInfo,
    ReportDraft,
    RiskAssessment,
    new_id,
)
from agent_service.store import LocalRunStore
from agent_service.tools import (
    MockKnowledgeSearchTool,
    MockReportSaveTool,
    RagKnowledgeSearchTool,
    TicketCreateFollowupToolClient,
    TicketSearchToolClient,
    ToolContractError,
    citations_from_knowledge,
)


class AgentGraphRunner:
    graph_name = "complaint-analysis-minimal"
    graph_version = "phase-007.v1"

    def __init__(
        self,
        store: LocalRunStore | None = None,
        ticket_tool: TicketSearchToolClient | None = None,
        knowledge_tool: MockKnowledgeSearchTool | RagKnowledgeSearchTool | None = None,
        followup_tool: TicketCreateFollowupToolClient | None = None,
        report_tool: MockReportSaveTool | None = None,
    ) -> None:
        self.store = store or LocalRunStore()
        self.ticket_tool = ticket_tool or TicketSearchToolClient()
        self.knowledge_tool = knowledge_tool or RagKnowledgeSearchTool()
        self.followup_tool = followup_tool or TicketCreateFollowupToolClient()
        self.report_tool = report_tool or MockReportSaveTool()

    def run(self, task: AgentTask) -> AgentTask:
        self.store.add_task(task)
        run = self.store.create_run(task, self.graph_name, self.graph_version)
        task.status = AI_TASK_RUNNING
        self.store.start_run(run)
        context: dict[str, Any] = {"task": task, "tickets": [], "knowledge": [], "citations": []}

        try:
            self._planner(task, run, context)
            self._retriever(task, run, context)
            self._data_analyst(task, run, context)
            self._risk(task, run, context)
            self._supervisor(task, run, context)
            self._report(task, run, context)
        except ToolContractError as exc:
            if exc.record is not None:
                self.store.add_tool_call(exc.record)
            task.status = AI_TASK_FAILED
            task.error = exc.error
            task.retryable = exc.error.retryable
            self.store.fail_run(run, exc.error)
            return task
        except Exception as exc:  # noqa: BLE001 - local runner must translate unknown errors.
            error = ErrorInfo(
                error_code="INTERNAL_ERROR",
                message=str(exc),
                trace_id=task.trace_id,
                retryable=False,
            )
            task.status = AI_TASK_FAILED
            task.error = error
            task.retryable = False
            self.store.fail_run(run, error)
            return task

        if context["risk"].requires_human:
            task.status = AI_TASK_WAITING_APPROVAL
            task.blocking_reason = "High risk or low confidence recommendation requires human confirmation."
        else:
            task.status = AI_TASK_SUCCESS
        self.store.finish_run(run, task.report_id)
        return task

    def _planner(self, task: AgentTask, run, context: dict[str, Any]) -> None:
        step = self.store.start_step(run, task, "Planner", "plan_complaint_analysis", "complaint prompt")
        context["plan"] = {
            "ticket_query": "billing" if "billing" in task.prompt.lower() else "complaint",
            "knowledge_query": "billing complaint escalation supervisor review",
        }
        self.store.finish_step(step, STEP_SUCCESS, "Planned ticket search and local RAG knowledge retrieval.")

    def _retriever(self, task: AgentTask, run, context: dict[str, Any]) -> None:
        step = self.store.start_step(run, task, "Retriever", "contract_tool_retrieval", "ticket.search.v1 and knowledge.search.v1")
        ticket_record, tickets = self.ticket_tool.search(
            tenant_id=task.tenant_id,
            requested_by=task.created_by,
            trace_id=task.trace_id,
            run_id=run.run_id,
            input_payload={"query": context["plan"]["ticket_query"], "status": "open"},
        )
        self.store.add_tool_call(ticket_record)
        knowledge_record, knowledge_items = self.knowledge_tool.search(
            tenant_id=task.tenant_id,
            requested_by=task.created_by,
            trace_id=task.trace_id,
            run_id=run.run_id,
            query=context["plan"]["knowledge_query"],
        )
        self.store.add_tool_call(knowledge_record)
        context["tickets"] = tickets
        context["knowledge"] = knowledge_items
        context["citations"] = citations_from_knowledge(knowledge_items)
        self.store.finish_step(
            step,
            STEP_SUCCESS,
            f"Retrieved {len(tickets)} tickets and {len(knowledge_items)} local RAG knowledge snippets.",
            citations=context["citations"],
        )

    def _data_analyst(self, task: AgentTask, run, context: dict[str, Any]) -> None:
        step = self.store.start_step(run, task, "DataAnalyst", "summarize_ticket_patterns", "retrieved ticket summaries")
        tickets = context["tickets"]
        urgent_count = sum(1 for item in tickets if item["priority"] == "urgent")
        categories = sorted({item["category"] for item in tickets})
        context["analysis"] = {
            "ticket_count": len(tickets),
            "urgent_count": urgent_count,
            "categories": categories,
        }
        self.store.finish_step(step, STEP_SUCCESS, f"Found {len(tickets)} tickets, {urgent_count} urgent.")

    def _risk(self, task: AgentTask, run, context: dict[str, Any]) -> None:
        step = self.store.start_step(run, task, "Risk", "assess_customer_risk", "ticket pattern and mock SOP")
        analysis = context["analysis"]
        high_risk = analysis["urgent_count"] > 0 or task.priority == "high"
        confidence = 0.86 if high_risk else 0.74
        context["risk"] = RiskAssessment(
            level="high" if high_risk else "medium",
            confidence=confidence,
            requires_human=high_risk or confidence < 0.8,
            reasons=["Urgent open complaint found."] if high_risk else ["Local RAG evidence still requires manual confirmation at low confidence."],
        )
        self.store.finish_step(step, STEP_SUCCESS, f"Risk level {context['risk'].level}; confidence {confidence}.")

    def _supervisor(self, task: AgentTask, run, context: dict[str, Any]) -> None:
        step = self.store.start_step(run, task, "Supervisor", "apply_human_boundary", "risk assessment")
        risk: RiskAssessment = context["risk"]
        if risk.requires_human:
            followup_record, approval_instance_id = self.followup_tool.request_followup(
                tenant_id=task.tenant_id,
                requested_by=task.created_by,
                trace_id=task.trace_id,
                run_id=run.run_id,
                source_task_id=task.task_id,
                risk_reason="; ".join(risk.reasons),
                input_payload={
                    "ticket_id": context["tickets"][0]["ticket_id"] if context["tickets"] else "ticket_unknown",
                    "title": "Supervisor follow-up for high-risk complaint",
                    "reason": "High-risk complaint handling needs a human-approved follow-up task.",
                    "due_at": "2026-06-03T12:00:00Z",
                    "approval_policy": "required",
                },
                citations=[citation.__dict__ for citation in context["citations"]],
            )
            self.store.add_tool_call(followup_record)
            task.approval_instance_id = approval_instance_id
            self.store.finish_step(
                step,
                STEP_WAITING_HUMAN,
                f"Recommendation held for human confirmation; approval request {approval_instance_id} created and no action command generated.",
            )
            return
        self.store.finish_step(step, STEP_SUCCESS, "No human confirmation required for report-only output.")

    def _report(self, task: AgentTask, run, context: dict[str, Any]) -> None:
        step = self.store.start_step(run, task, "Report", "generate_structured_report", "analysis and risk assessment")
        analysis = context["analysis"]
        risk: RiskAssessment = context["risk"]
        report = ReportDraft(
            task_id=task.task_id,
            tenant_id=task.tenant_id,
            title="Complaint Analysis Report",
            facts=[
                f"{analysis['ticket_count']} open complaint tickets matched the contract search.",
                f"{analysis['urgent_count']} matched tickets are urgent.",
                "Knowledge snippets are local RAG document sources returned through knowledge.search.v1.",
            ],
            inferences=[
                f"Primary categories: {', '.join(analysis['categories']) or 'none'}.",
                f"Risk level is {risk.level} with confidence {risk.confidence}.",
            ],
            risks=risk.reasons,
            recommendations=[
                "Route high-risk customer handling to a human supervisor before any business action.",
                "Use ticket.create_followup.v1 only to create an approval request; do not execute follow-up actions before approval.",
            ],
            citations=context["citations"],
        )
        report_payload = {
            "task_id": report.task_id,
            "title": report.title,
            "facts": report.facts,
            "inferences": report.inferences,
            "recommendations": report.recommendations,
            "citations": [citation.__dict__ for citation in report.citations],
        }
        report_record, report_id = self.report_tool.save(
            tenant_id=task.tenant_id,
            requested_by=task.created_by,
            trace_id=task.trace_id,
            run_id=run.run_id,
            report_payload=report_payload,
        )
        self.store.add_tool_call(report_record)
        report.report_id = report_id or new_id("report")
        self.store.save_report(report)
        task.report_id = report.report_id
        self.store.finish_step(step, STEP_SUCCESS, f"Generated mock-local report {report.report_id}.", citations=report.citations)
