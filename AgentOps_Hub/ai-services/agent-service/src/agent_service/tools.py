from __future__ import annotations

from typing import Any

from agent_service.models import Citation, ErrorInfo, ToolCallRecord
from rag_service.service import KnowledgeSearchEngine, RagServiceError, create_demo_engine


class ToolContractError(Exception):
    def __init__(self, error: ErrorInfo, record: ToolCallRecord | None = None):
        super().__init__(error.message)
        self.error = error
        self.record = record


class TicketSearchToolClient:
    def __init__(self, fail: bool = False) -> None:
        self.fail = fail

    def search(
        self,
        *,
        tenant_id: str,
        requested_by: str,
        trace_id: str,
        run_id: str,
        input_payload: dict[str, Any],
    ) -> tuple[ToolCallRecord, list[dict[str, Any]]]:
        request = {
            "tool_name": "ticket.search.v1",
            "tool_version": "v1",
            "tenant_id": tenant_id,
            "requested_by": requested_by,
            "trace_id": trace_id,
            "input": {"page": 1, "page_size": 20, **input_payload},
        }
        if self.fail:
            error = ErrorInfo(
                error_code="DOWNSTREAM_UNAVAILABLE",
                message="Mock ticket.search.v1 failure.",
                trace_id=trace_id,
                retryable=True,
                details={"source_type": "mock_contract"},
            )
            record = ToolCallRecord(
                tool_name="ticket.search.v1",
                tool_version="v1",
                tenant_id=tenant_id,
                requested_by=requested_by,
                trace_id=trace_id,
                run_id=run_id,
                status="failed",
                source_type="mock_contract",
                request=request,
                response=None,
                error=error,
                attempts=2,
            )
            raise ToolContractError(error, record) from None

        items = [
            {
                "ticket_id": "ticket_1001",
                "tenant_id": tenant_id,
                "title": "VIP customer reports repeated billing complaint",
                "status": "open",
                "priority": "urgent",
                "customer_id": "customer_vip_001",
                "category": "billing",
                "sla_due_at": "2026-06-01T10:00:00Z",
            },
            {
                "ticket_id": "ticket_1002",
                "tenant_id": tenant_id,
                "title": "Customer asks for refund policy clarification",
                "status": "open",
                "priority": "normal",
                "customer_id": "customer_std_019",
                "category": "policy",
                "sla_due_at": "2026-06-02T10:00:00Z",
            },
        ]
        query = (input_payload.get("query") or "").lower()
        if query:
            items = [item for item in items if query in item["title"].lower() or query in item["category"]]
        response = {
            "tool_name": "ticket.search.v1",
            "tool_version": "v1",
            "tenant_id": tenant_id,
            "trace_id": trace_id,
            "status": "success",
            "output": {"items": items, "page": 1, "page_size": 20, "total": len(items)},
            "error": None,
        }
        record = ToolCallRecord(
            tool_name="ticket.search.v1",
            tool_version="v1",
            tenant_id=tenant_id,
            requested_by=requested_by,
            trace_id=trace_id,
            run_id=run_id,
            status="success",
            source_type="mock_contract",
            request=request,
            response=response,
        )
        return record, items


class MockKnowledgeSearchTool:
    def search(self, *, tenant_id: str, requested_by: str, trace_id: str, run_id: str, query: str) -> tuple[ToolCallRecord, list[dict[str, Any]]]:
        request = {
            "tool_name": "knowledge.search.v1",
            "tool_version": "v1",
            "tenant_id": tenant_id,
            "requested_by": requested_by,
            "trace_id": trace_id,
            "input": {"query": query, "top_k": 3, "filters": {"phase": "005-mock"}},
        }
        items = [
            {
                "document_id": "mock_doc_sop_billing",
                "chunk_id": "mock_chunk_001",
                "snippet": "Escalate urgent billing complaints to supervisor review before customer-facing action.",
                "score": 0.91,
                "mock_source": True,
                "citation": {
                    "document_id": "mock_doc_sop_billing",
                    "chunk_id": "mock_chunk_001",
                    "source_title": "Mock Billing Complaint SOP",
                    "mock_source": True,
                },
            }
        ]
        response = {
            "tool_name": "knowledge.search.v1",
            "tool_version": "v1",
            "tenant_id": tenant_id,
            "trace_id": trace_id,
            "status": "success",
            "output": {"items": items, "mock_source": True},
            "error": None,
        }
        record = ToolCallRecord(
            tool_name="knowledge.search.v1",
            tool_version="v1",
            tenant_id=tenant_id,
            requested_by=requested_by,
            trace_id=trace_id,
            run_id=run_id,
            status="success",
            source_type="mock_knowledge",
            request=request,
            response=response,
        )
        return record, items


class RagKnowledgeSearchTool:
    def __init__(self, engine: KnowledgeSearchEngine | None = None) -> None:
        self.engine = engine or create_demo_engine()

    def search(self, *, tenant_id: str, requested_by: str, trace_id: str, run_id: str, query: str) -> tuple[ToolCallRecord, list[dict[str, Any]]]:
        input_payload = {
            "query": query,
            "top_k": 3,
            "filters": {"knowledge_base_id": "kb_support_policy"},
        }
        request = {
            "tool_name": "knowledge.search.v1",
            "tool_version": "v1",
            "tenant_id": tenant_id,
            "requested_by": requested_by,
            "trace_id": trace_id,
            "input": input_payload,
        }
        try:
            response = self.engine.search(
                tenant_id=tenant_id,
                requested_by=requested_by,
                trace_id=trace_id,
                query=query,
                top_k=input_payload["top_k"],
                filters=input_payload["filters"],
            )
        except RagServiceError as exc:
            error = ErrorInfo(
                error_code=exc.error.error_code,
                message=exc.error.message,
                trace_id=trace_id,
                retryable=exc.error.retryable,
                details=exc.error.details,
            )
            record = ToolCallRecord(
                tool_name="knowledge.search.v1",
                tool_version="v1",
                tenant_id=tenant_id,
                requested_by=requested_by,
                trace_id=trace_id,
                run_id=run_id,
                status="failed",
                source_type="local_rag",
                request=request,
                response=None,
                error=error,
                attempts=1,
            )
            raise ToolContractError(error, record) from None

        record = ToolCallRecord(
            tool_name="knowledge.search.v1",
            tool_version="v1",
            tenant_id=tenant_id,
            requested_by=requested_by,
            trace_id=trace_id,
            run_id=run_id,
            status="success",
            source_type="local_rag",
            request=request,
            response=response,
        )
        return record, response["output"]["items"]


class MockReportSaveTool:
    def save(
        self,
        *,
        tenant_id: str,
        requested_by: str,
        trace_id: str,
        run_id: str,
        report_payload: dict[str, Any],
    ) -> tuple[ToolCallRecord, str]:
        report_id = f"report_{report_payload['task_id']}"
        request = {
            "tool_name": "report.save.v1",
            "tool_version": "v1",
            "tenant_id": tenant_id,
            "requested_by": requested_by,
            "trace_id": trace_id,
            "idempotency_key": f"{report_payload['task_id']}:report.save.v1",
            "input": report_payload,
        }
        response = {
            "tool_name": "report.save.v1",
            "tool_version": "v1",
            "tenant_id": tenant_id,
            "trace_id": trace_id,
            "status": "success",
            "output": {"report_id": report_id, "storage_mode": "mock_local"},
            "error": None,
        }
        record = ToolCallRecord(
            tool_name="report.save.v1",
            tool_version="v1",
            tenant_id=tenant_id,
            requested_by=requested_by,
            trace_id=trace_id,
            run_id=run_id,
            status="success",
            source_type="mock_report_store",
            request=request,
            response=response,
        )
        return record, report_id


class TicketCreateFollowupToolClient:
    def request_followup(
        self,
        *,
        tenant_id: str,
        requested_by: str,
        trace_id: str,
        run_id: str,
        source_task_id: str,
        risk_reason: str,
        input_payload: dict[str, Any],
        citations: list[dict[str, Any]],
    ) -> tuple[ToolCallRecord, str]:
        approval_instance_id = f"approval_{source_task_id}"
        idempotency_key = f"followup:{input_payload['ticket_id']}:{source_task_id}"
        request = {
            "tool_name": "ticket.create_followup.v1",
            "tool_version": "v1",
            "tenant_id": tenant_id,
            "requested_by": requested_by,
            "trace_id": trace_id,
            "idempotency_key": idempotency_key,
            "input": input_payload,
            "source_task_id": source_task_id,
            "risk_reason": risk_reason,
            "citations": citations,
        }
        response = {
            "tool_name": "ticket.create_followup.v1",
            "tool_version": "v1",
            "tenant_id": tenant_id,
            "trace_id": trace_id,
            "status": "waiting_approval",
            "output": {"approval_instance_id": approval_instance_id},
            "error": None,
        }
        record = ToolCallRecord(
            tool_name="ticket.create_followup.v1",
            tool_version="v1",
            tenant_id=tenant_id,
            requested_by=requested_by,
            trace_id=trace_id,
            run_id=run_id,
            status="waiting_approval",
            source_type="mock_workflow_contract",
            request=request,
            response=response,
        )
        return record, approval_instance_id


def citations_from_knowledge(items: list[dict[str, Any]]) -> list[Citation]:
    citations: list[Citation] = []
    for item in items:
        citation = item["citation"]
        citations.append(
            Citation(
                document_id=citation["document_id"],
                chunk_id=citation["chunk_id"],
                source_title=citation["source_title"],
                source_uri=citation.get("source_uri"),
                mock_source=bool(citation.get("mock_source")),
            )
        )
    return citations
