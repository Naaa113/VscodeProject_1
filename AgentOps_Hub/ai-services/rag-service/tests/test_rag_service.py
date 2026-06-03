import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))

from rag_service.eval_runner import run_default_evals
from rag_service.models import DOCUMENT_FAILED, DOCUMENT_INDEXED
from rag_service.service import KnowledgeSearchEngine, RagServiceError, create_demo_engine


class RagServiceTest(unittest.TestCase):
    def test_demo_document_search_returns_traceable_non_mock_citation(self):
        engine = create_demo_engine()

        response = engine.search(
            tenant_id="tenant_demo",
            requested_by="user_ops_demo",
            trace_id="trace_test_rag_search",
            query="billing complaint supervisor escalation",
            top_k=3,
            filters={"knowledge_base_id": "kb_support_policy"},
        )

        self.assertEqual("success", response["status"])
        self.assertFalse(response["output"]["mock_source"])
        self.assertTrue(response["output"]["items"])
        first = response["output"]["items"][0]
        self.assertEqual("doc_billing_complaint_sop", first["document_id"])
        self.assertFalse(first["mock_source"])
        self.assertEqual(first["document_id"], first["citation"]["document_id"])
        self.assertEqual(first["chunk_id"], first["citation"]["chunk_id"])
        self.assertIn("local-index://tenant/tenant_demo", first["citation"]["source_uri"])

    def test_parse_failure_lands_failed_and_can_retry(self):
        engine = KnowledgeSearchEngine()
        document = engine.register_document(
            tenant_id="tenant_demo",
            requested_by="user_ops_demo",
            trace_id="trace_test_parse_failure",
            knowledge_base_id="kb_support_policy",
            filename="empty.txt",
            object_key="fixtures/missing.txt",
            source_text="",
            document_id="doc_empty",
        )

        failed = engine.parse_document(
            document_id=document.document_id,
            tenant_id="tenant_demo",
            requested_by="user_ops_demo",
            trace_id="trace_test_parse_failure",
        )

        self.assertEqual(DOCUMENT_FAILED, failed.parse_status)
        self.assertIsNotNone(failed.error)
        self.assertEqual("DOCUMENT_PARSE_FAILED", failed.error.error_code)
        self.assertTrue(failed.error.retryable)

        retried = engine.retry_parse(
            document_id=document.document_id,
            tenant_id="tenant_demo",
            requested_by="user_ops_demo",
            trace_id="trace_test_parse_retry",
            source_text="Refund policy exceptions require documented supervisor approval before customer communication.",
        )

        self.assertEqual(DOCUMENT_INDEXED, retried.parse_status)
        self.assertGreater(retried.chunk_count, 0)
        self.assertIsNone(retried.error)

    def test_parse_retry_rejects_cross_tenant_context(self):
        engine = KnowledgeSearchEngine()
        document = engine.register_document(
            tenant_id="tenant_a",
            requested_by="user_a",
            trace_id="trace_tenant_a_register",
            knowledge_base_id="kb_support_policy",
            filename="empty.txt",
            object_key="fixtures/missing.txt",
            source_text="",
            document_id="doc_shared",
        )
        failed = engine.parse_document(
            document_id=document.document_id,
            tenant_id="tenant_a",
            requested_by="user_a",
            trace_id="trace_tenant_a_parse",
        )

        self.assertEqual(DOCUMENT_FAILED, failed.parse_status)

        with self.assertRaises(RagServiceError) as parse_error:
            engine.parse_document(
                document_id=document.document_id,
                tenant_id="tenant_b",
                requested_by="user_b",
                trace_id="trace_tenant_b_parse",
            )
        self.assertEqual("RESOURCE_NOT_FOUND", parse_error.exception.error.error_code)
        self.assertEqual("trace_tenant_b_parse", parse_error.exception.error.trace_id)

        with self.assertRaises(RagServiceError) as retry_error:
            engine.retry_parse(
                document_id=document.document_id,
                tenant_id="tenant_b",
                requested_by="user_b",
                trace_id="trace_tenant_b_retry",
                source_text="Tenant B must not repair tenant A content.",
            )
        self.assertEqual("RESOURCE_NOT_FOUND", retry_error.exception.error.error_code)
        self.assertEqual("trace_tenant_b_retry", retry_error.exception.error.trace_id)
        self.assertEqual(DOCUMENT_FAILED, document.parse_status)
        self.assertEqual("", document.source_text)

    def test_tenant_isolation_filters_search_results(self):
        engine = create_demo_engine()

        response = engine.search(
            tenant_id="tenant_other",
            requested_by="user_other",
            trace_id="trace_test_tenant_other",
            query="billing complaint supervisor",
            top_k=3,
        )

        self.assertEqual([], response["output"]["items"])

    def test_default_eval_cases_calculate_metrics(self):
        results = run_default_evals()

        self.assertTrue(results)
        self.assertTrue(all(result.passed for result in results), [result.checks for result in results])
        self.assertTrue(all(result.hit_rate == 1.0 for result in results))
        self.assertTrue(all(result.citation_accuracy == 1.0 for result in results))


if __name__ == "__main__":
    unittest.main()
