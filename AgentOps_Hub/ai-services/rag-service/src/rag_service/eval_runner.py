from __future__ import annotations

from rag_service.models import RagEvalCase, RagEvalResult
from rag_service.service import KnowledgeSearchEngine, create_demo_engine


def default_eval_cases() -> list[RagEvalCase]:
    return [
        RagEvalCase(
            case_id="billing_escalation_policy_hit",
            tenant_id="tenant_demo",
            query="billing complaint escalation supervisor review",
            expected_document_id="doc_billing_complaint_sop",
            expected_terms=["billing", "supervisor", "complaint"],
        ),
        RagEvalCase(
            case_id="refund_policy_hit",
            tenant_id="tenant_demo",
            query="refund policy exception documentation",
            expected_document_id="doc_refund_policy_faq",
            expected_terms=["refund", "policy"],
        ),
    ]


def run_eval_case(case: RagEvalCase, engine: KnowledgeSearchEngine) -> RagEvalResult:
    response = engine.search(
        tenant_id=case.tenant_id,
        requested_by="eval_runner",
        trace_id=f"trace_eval_{case.case_id}",
        query=case.query,
        top_k=3,
        filters={"knowledge_base_id": "kb_support_policy"},
    )
    items = response["output"]["items"]
    expected_hits = [item for item in items if item["document_id"] == case.expected_document_id]
    term_hits = [
        term
        for term in case.expected_terms
        if any(term.lower() in item["snippet"].lower() for item in items)
    ]
    citation_hits = [
        item
        for item in items
        if item["citation"]["document_id"] == item["document_id"]
        and item["citation"]["chunk_id"] == item["chunk_id"]
        and item["citation"].get("source_uri")
        and not item["citation"].get("mock_source", False)
    ]
    hit_rate = 1.0 if expected_hits and len(term_hits) == len(case.expected_terms) else 0.0
    citation_accuracy = len(citation_hits) / len(items) if items else 0.0
    checks = {
        "expected_document_hit": bool(expected_hits),
        "expected_terms_hit": len(term_hits) == len(case.expected_terms),
        "citations_traceable": citation_accuracy == 1.0,
        "non_mock_source": all(not item.get("mock_source", False) for item in items),
    }
    return RagEvalResult(
        case_id=case.case_id,
        passed=all(checks.values()),
        hit_rate=hit_rate,
        citation_accuracy=citation_accuracy,
        checks=checks,
    )


def run_default_evals() -> list[RagEvalResult]:
    engine = create_demo_engine()
    return [run_eval_case(case, engine) for case in default_eval_cases()]
