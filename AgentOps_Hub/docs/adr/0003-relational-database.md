# ADR 0003: Relational Database

## Status

Accepted

## Context

The MVP needs a primary relational database direction for future business services. Phase 001 may record the direction but must not add migrations, ORM setup, database clients, or seed data.

## Decision

Use PostgreSQL as the MVP relational database direction.

## Consequences

- Future service phases can design migrations after contracts and host boundaries are frozen.
- Phase 001 adds no database runtime files, schemas, or business tables.
