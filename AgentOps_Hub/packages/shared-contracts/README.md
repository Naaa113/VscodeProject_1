# shared-contracts

This package is the cross-host contract authority for AgentOps Hub. It contains versioned OpenAPI, event, tool, error, state, example, and validation assets only.

Phase 002 freezes contract drafts. It does not implement services, clients, adapters, mock servers, database migrations, Agent workflows, RAG retrieval, or model access.

## Contents

- `openapi/agentops-api.v1.yaml`: MVP REST and SSE contract draft.
- `schemas/common.v1.schema.json`: shared tenant context, audit fields, pagination, errors, event envelopes, and tool envelopes.
- `schemas/status.v1.schema.json`: lifecycle status enums aligned to `docs/harness/05-transition-lifetime.md`.
- `events/*.v1.schema.json`: event contract drafts.
- `tools/*.v1.schema.json`: Agent tool contract drafts.
- `errors/error-codes.v1.json`: shared error code catalog.
- `examples/openapi`, `examples/events`, `examples/tools`: parseable success and failure payload examples.
- `manifest.v1.json`: contract inventory and compatibility policy.

## Compatibility

- File names include `.v1` for the first contract version.
- Additive fields may be introduced in later compatible versions when consumers can ignore unknown fields.
- Removing required fields, changing enum values, or changing error semantics requires a new version and review.
- Contracts describe future producer and consumer obligations; they do not claim any runtime service already exists.
