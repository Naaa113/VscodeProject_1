# ADR 0001: Frontend Stack

## Status

Accepted

## Context

The platform needs a typed frontend stack for a future operations console. Phase 001 may record the direction but must not create a frontend project, routes, components, stores, or API clients.

## Decision

Use React + TypeScript as the MVP frontend direction for future `apps/web-console` work.

## Consequences

- Future frontend implementation should remain behind the gateway and consume published contracts only.
- Phase 001 creates no React application or UI code.
- Contract and API client details remain out of scope until a later phase freezes them.
