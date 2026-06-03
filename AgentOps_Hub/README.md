# AgentOps Hub

AgentOps Hub is an enterprise intelligent operations collaboration platform. This repository is currently in Phase 001: monorepo skeleton and local development baseline.

This phase creates repository structure, development conventions, ADR records, and read-only validation only. It does not implement business APIs, frontend pages, database migrations, Agent workflows, model access, or runtime services.

## Repository Layout

```text
apps/                 User-facing and platform entrypoint applications
services/             Java business service placeholders
ai-services/          Python AI service placeholders
packages/             Shared contracts, prompts, and test assets
deploy/               Deployment asset placeholder
tests/                Cross-host test placeholder
docs/development/     Local development guidance
docs/adr/             Architecture decision records
scripts/              Local validation scripts
```

## Phase 001 Validation

Run the skeleton validation from PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-skeleton.ps1
```

The validation checks that required placeholder directories and documents exist, and that this phase has not introduced framework projects, business code, migrations, API routes, UI components, or runtime adapters.

## Current Boundaries

- `packages/shared-contracts` is only a location and rule set in Phase 001. It contains no business schemas.
- Host directories contain README placeholders only.
- Real credentials, production data, customer data, and model keys must never be committed.
