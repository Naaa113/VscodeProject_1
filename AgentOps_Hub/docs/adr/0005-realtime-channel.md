# ADR 0005: Realtime Channel

## Status

Accepted

## Context

The future console needs task status updates. Phase 001 may record a direction but must not implement a realtime endpoint, client, bridge, event stream, or business status payload.

## Decision

Use SSE as the preferred MVP realtime channel direction.

## Consequences

- Future phases must define contracts and gateway behavior before implementation.
- Phase 001 creates no SSE route, client, bridge, or event payload.
