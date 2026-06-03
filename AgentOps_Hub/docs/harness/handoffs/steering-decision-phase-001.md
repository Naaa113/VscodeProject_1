# Steering Decision: Phase 001

## Decision Status

Approved by user on 2026-05-31. Window 0 selected the next phase and stops before Window 1.

## Current State Summary

- Current state source: `docs/harness/state/current-state.md`
- Current phase: Phase 000 Harness Baseline
- Steering state: `handoff_done`
- Architecture status: 文档阶段 / 未落地代码
- Business code status: 未创建
- Handoffs listed: `phase-000-harness-baseline.md`
- Latest completed fact source: `phase-000-harness-baseline.md`
- No `phase-001-final.md` or higher final handoff exists, so bootstrap recommendation is still eligible for evaluation.

## Latest Phase / Handoff Recovery

Latest completed phase:

- Phase 000: Harness Baseline
- Read handoff: `docs/harness/handoffs/phase-000-harness-baseline.md`

Missing files for latest/current phase recovery:

- `docs/harness/handoffs/steering-decision-phase-000.md`
- `docs/harness/handoffs/phase-000-architect.md`
- `docs/harness/handoffs/phase-000-implementation.md`
- `docs/harness/handoffs/phase-000-review.md`
- `docs/harness/handoffs/phase-000-final.md`

Block assessment:

- Not blocked. `current-state.md` explicitly records Phase 000 as `handoff_done`, and `phase-000-harness-baseline.md` provides bootstrap facts and next recommended phase.
- The missing files are recorded as recovery gaps, not blockers, because Phase 000 was a special bootstrap harness phase.

## Candidate Scoring

Scoring scale: 1 = weak fit, 5 = strong fit.

| Candidate | Backlog next step | Debt impact | Uncertainty reduction | Verifiable loop | Avoids premature business implementation | Total |
|---|---:|---:|---:|---:|---:|---:|
| Phase 001: Monorepo 骨架与本地开发基线 | 5 | 4 | 5 | 5 | 5 | 24 |
| Phase 002: `shared-contracts` / OpenAPI / 事件契约 | 4 | 5 | 4 | 4 | 4 | 21 |
| Phase 003: `identity-service` 最小认证与租户模型 | 2 | 3 | 3 | 4 | 2 | 14 |
| Phase 005: `agent-service` 最小 LangGraph 闭环 | 1 | 3 | 3 | 4 | 1 | 12 |

## Primary Candidate

Phase 001: Monorepo 骨架与本地开发基线.

Why this is the next step:

- It is the explicit next phase in `current-state.md`, `phase-000-harness-baseline.md`, and `07-phase-backlog.md`.
- It addresses Phase 001 D1 debts directly: DEBT-001 frontend choice, DEBT-002 MQ choice, and DEBT-003 relational database choice.
- It reduces the highest immediate uncertainty before contract files or services are created: repository layout, host ownership mapping, local commands, placeholder validation strategy, and MVP technical decisions.
- It creates a verifiable non-business baseline: repository structure, README or equivalent developer guide, ADR records, and placeholder lint/test strategy.
- It avoids jumping into API, UI, Agent, or database implementation before Window 1 freezes boundaries and acceptance conditions.

Frozen intent for Window 1 to refine:

- Create only minimal monorepo structure and development baseline.
- Align directories with `03-host-ownership.md`.
- Record MVP decisions for frontend, MQ, relational database, vector-search direction, and realtime channel.
- Define local development command conventions and validation placeholders.
- Do not create Spring, Vue, React, FastAPI, database migrations, business APIs, business pages, or real model integration unless Window 1 explicitly proves that a non-business placeholder is necessary and keeps it within Phase 001.

## Fallback Candidate

Phase 002: `shared-contracts` / OpenAPI / 事件契约.

Fallback conditions:

- Use only if the user explicitly rejects Phase 001 sequencing and asks to prioritize D0 contract debt first.
- Window 1 must then explain how contract artifacts will live without a monorepo/package baseline, or scope Phase 002 to documentation-only contract preparation.

Why it is not primary:

- It addresses DEBT-005 more directly, but it depends on repository/package conventions that Phase 001 is meant to establish.
- Starting with Phase 002 risks freezing contract file layout, schema commands, and generation conventions before local development baseline decisions exist.

## Why Not Other Phases

- Phase 003 identity service is too far because `shared-contracts` does not exist and tech choices are still open.
- Phase 004 ticket service is too far because identity, contracts, tenancy, and local service conventions are not established.
- Phase 005 agent-service is too far because Agent tool contracts, approval boundaries, and business fact sources are not implemented.
- Phase 006 web-console is too far because frontend stack is still undecided and API contracts do not exist.
- Phase 007 RAG is too far because vector strategy is still open and knowledge contracts are not established.
- Phase 008 approval/action commands are too far for implementation, though its boundary must remain visible as a non-bypassable constraint.
- Phase 009 and Phase 010 are post-MVP hardening/expansion and should not be pulled into the baseline phase.

## Window 1 Task Boundary

Window 1 must produce a Phase 001 architecture plan before any implementation window starts.

Required clarifications:

- Contracts: confirm whether Phase 001 creates only contract locations and README conventions, or also placeholder schema validation commands; no business API contract content should be implemented unless explicitly scoped as a placeholder.
- Host boundaries: map `apps/`, `services/`, `ai-services/`, `packages/`, `deploy/`, `tests/`, and `docs/` to the ownership table in `03-host-ownership.md`.
- State lifecycle: define what "local development baseline ready" means, including setup, validation, failure reporting, and handoff state; do not introduce business runtime state.
- Debt handling: resolve or plan DEBT-001, DEBT-002, DEBT-003; record DEBT-004 deferral or MVP decision; keep DEBT-005 and DEBT-006 visible for later phases.
- Acceptance: define exact file/tree expectations, README/ADR requirements, validation commands, and checks proving no business implementation was added.
- Rollback/degrade: describe how a minimal skeleton can be reverted or adjusted if Window 2 finds a tooling choice incompatible.

## Required User Approval

Approve Window 0 recommendation to move into:

Phase 001: Monorepo 骨架与本地开发基线

If approved, Window 1 should start from this decision and produce the Phase 001 architecture, contract-impact, ownership, lifecycle, and acceptance plan. Window 0 stops here.
