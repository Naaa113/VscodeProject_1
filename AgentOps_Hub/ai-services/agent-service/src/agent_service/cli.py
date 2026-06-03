from __future__ import annotations

import argparse
import json

from agent_service.eval_runner import run_default_evals
from agent_service.graph import AgentGraphRunner
from agent_service.models import AgentTask, new_id, to_dict


def run_scenario() -> dict:
    task = AgentTask(
        task_id=new_id("task"),
        tenant_id="tenant_demo",
        created_by="user_ops_demo",
        task_type="complaint_analysis",
        prompt="Analyze billing complaints and identify high-risk customers.",
        priority="high",
        trace_id="trace_cli_demo",
    )
    runner = AgentGraphRunner()
    result = runner.run(task)
    return {
        "task": to_dict(result),
        "steps": [to_dict(step) for step in runner.store.steps],
        "tool_calls": [to_dict(record) for record in runner.store.tool_calls],
        "reports": [to_dict(report) for report in runner.store.reports.values()],
        "events": runner.store.events,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--scenario", choices=["complaint", "eval"], default="complaint")
    args = parser.parse_args()
    if args.scenario == "eval":
        results = run_default_evals()
        payload = {"results": [to_dict(result) for result in results]}
        print(json.dumps(payload, indent=2, sort_keys=True))
        return 0 if all(result.passed for result in results) else 1
    print(json.dumps(run_scenario(), indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
