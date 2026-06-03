import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))

from agent_service.eval_runner import run_default_evals
from agent_service.graph import AgentGraphRunner
from agent_service.models import AgentTask, new_id
from agent_service.store import LocalRunStore
from agent_service.tools import TicketSearchToolClient


class AgentGraphRunnerTest(unittest.TestCase):
    def assert_event_envelope(self, event, event_name):
        self.assertEqual(event_name, event["event_name"])
        self.assertEqual("v1", event["event_version"])
        self.assertTrue(event["event_id"].startswith("event_"))
        self.assertTrue(event["occurred_at"].endswith("Z"))
        self.assertEqual("agent-service", event["producer"])
        self.assertEqual(["java-task-entrypoint", "web-console-stream"], event["consumers"])
        self.assertTrue(event["tenant_id"])
        self.assertTrue(event["trace_id"])
        self.assertIn("payload", event)

    def events_named(self, store, event_name):
        return [event for event in store.events if event["event_name"] == event_name]

    def test_complaint_flow_generates_report_and_waits_for_human(self):
        store = LocalRunStore()
        runner = AgentGraphRunner(store=store)
        task = AgentTask(
            task_id=new_id("task"),
            tenant_id="tenant_demo",
            created_by="user_ops_demo",
            task_type="complaint_analysis",
            prompt="Analyze billing complaint growth.",
            priority="high",
            trace_id="trace_test_success",
        )

        result = runner.run(task)

        self.assertEqual("waiting_approval", result.status)
        self.assertIsNotNone(result.approval_instance_id)
        self.assertIsNotNone(result.report_id)
        self.assertTrue(any(step.status == "waiting_human" for step in store.steps))
        self.assertEqual(["Planner", "Retriever", "DataAnalyst", "Risk", "Supervisor", "Report"], [step.agent_name for step in store.steps])
        self.assertTrue(all(record.tenant_id == "tenant_demo" for record in store.tool_calls))
        self.assertTrue(all(record.trace_id == "trace_test_success" for record in store.tool_calls))
        self.assertTrue(any(record.source_type == "local_rag" for record in store.tool_calls))
        self.assertTrue(any(record.tool_name == "ticket.create_followup.v1" for record in store.tool_calls))
        report = next(iter(store.reports.values()))
        self.assertTrue(report.citations)
        self.assertTrue(all(not citation.mock_source for citation in report.citations))
        self.assertTrue(all(citation.source_uri for citation in report.citations))
        started_events = self.events_named(store, "agent.run.started.v1")
        self.assertEqual(1, len(started_events))
        self.assert_event_envelope(started_events[0], "agent.run.started.v1")
        self.assertEqual("started", started_events[0]["payload"]["status"])
        self.assertEqual(result.run_id, started_events[0]["payload"]["run_id"])
        completed_events = self.events_named(store, "agent.run.completed.v1")
        self.assertEqual(1, len(completed_events))
        self.assert_event_envelope(completed_events[0], "agent.run.completed.v1")
        self.assertEqual("completed", completed_events[0]["payload"]["status"])
        self.assertEqual(result.report_id, completed_events[0]["payload"]["report_id"])
        step_events = self.events_named(store, "agent.step.completed.v1")
        self.assertEqual(6, len(step_events))
        for event in step_events:
            self.assert_event_envelope(event, "agent.step.completed.v1")
            self.assertIn(event["payload"]["status"], ["success", "failed", "skipped", "waiting_human"])
            self.assertTrue(event["payload"]["started_at"])
            self.assertTrue(event["payload"]["finished_at"])

    def test_ticket_tool_failure_records_retryable_error(self):
        store = LocalRunStore()
        runner = AgentGraphRunner(store=store, ticket_tool=TicketSearchToolClient(fail=True))
        task = AgentTask(
            task_id=new_id("task"),
            tenant_id="tenant_demo",
            created_by="user_ops_demo",
            task_type="complaint_analysis",
            prompt="Analyze unavailable ticket data.",
            priority="normal",
            trace_id="trace_test_failure",
        )

        result = runner.run(task)

        self.assertEqual("failed", result.status)
        self.assertIsNotNone(result.error)
        self.assertEqual("DOWNSTREAM_UNAVAILABLE", result.error.error_code)
        self.assertTrue(result.error.retryable)
        self.assertTrue(any(record.status == "failed" for record in store.tool_calls))
        self.assertTrue(any(event["event_name"] == "agent.run.failed.v1" for event in store.events))
        failed_events = self.events_named(store, "agent.run.failed.v1")
        self.assertEqual(1, len(failed_events))
        self.assert_event_envelope(failed_events[0], "agent.run.failed.v1")
        self.assertEqual("DOWNSTREAM_UNAVAILABLE", failed_events[0]["payload"]["error_code"])
        self.assertEqual("trace_test_failure", failed_events[0]["trace_id"])
        self.assertTrue(failed_events[0]["payload"]["retryable"])

    def test_default_eval_cases_pass(self):
        results = run_default_evals()
        self.assertTrue(results)
        self.assertTrue(all(result.passed for result in results), [result.checks for result in results])


if __name__ == "__main__":
    unittest.main()
