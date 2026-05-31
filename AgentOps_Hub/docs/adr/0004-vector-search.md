# ADR 0004: Vector Search

## Status

Deferred

## Context

The MVP will need vector search for RAG, but the choice depends on contract shape, document scale, local development constraints, and the later RAG phase.

## Decision

Defer the vector search choice until a later contract or RAG phase. Candidate directions remain pgvector, Milvus, or a temporary development substitute approved by that later phase.

## Consequences

- Phase 001 does not choose a vector runtime.
- No embedding index configuration, schema, fallback, or adapter is created in this phase.
- Phase 002 keeps knowledge retrieval contracts storage-neutral: `index_ref`, `document_id`, and `chunk_id` are contract fields, but no pgvector, Milvus, or temporary vector runtime is selected.
