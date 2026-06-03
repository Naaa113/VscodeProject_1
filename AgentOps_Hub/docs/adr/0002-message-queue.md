# ADR 0002: Message Queue

## Status

Accepted

## Context

The MVP needs an asynchronous messaging direction for long-running AI tasks, document processing, and notification handoff in later phases. Phase 001 may record the direction but must not add queue configuration, consumers, producers, adapters, or event payload schemas.

## Decision

Use RabbitMQ as the MVP message queue direction.

## Consequences

- Later phases can define event contracts before runtime producers or consumers are implemented.
- Phase 001 adds no queue runtime, no event payload schema, and no bridge code.
