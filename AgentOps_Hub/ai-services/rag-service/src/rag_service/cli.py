from __future__ import annotations

import argparse
import json

from rag_service.eval_runner import run_default_evals
from rag_service.models import to_dict
from rag_service.service import create_demo_engine


def run_demo() -> dict:
    engine = create_demo_engine()
    response = engine.search(
        tenant_id="tenant_demo",
        requested_by="user_ops_demo",
        trace_id="trace_rag_demo",
        query="billing complaint escalation supervisor review",
        top_k=3,
        filters={"knowledge_base_id": "kb_support_policy"},
    )
    return {
        "documents": engine.list_documents("tenant_demo"),
        "search": response,
        "events": engine.registry.events,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--scenario", choices=["demo", "eval"], default="demo")
    args = parser.parse_args()
    if args.scenario == "eval":
        results = run_default_evals()
        print(json.dumps({"results": [to_dict(result) for result in results]}, indent=2, sort_keys=True))
        return 0 if all(result.passed for result in results) else 1
    print(json.dumps(run_demo(), indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
