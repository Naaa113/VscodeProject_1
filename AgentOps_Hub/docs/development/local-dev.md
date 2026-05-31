# Local Development Baseline

## Purpose

Phase 001 establishes a stable monorepo layout and validation command for later phases. The local baseline proves that the repository structure exists and that no business implementation has been introduced early.

## Skeleton Check

Run from the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-skeleton.ps1
```

The script is read-only. It checks required placeholders and rejects framework project files, source directories, migrations, route folders, controller folders, component folders, dependency directories, and Dockerfiles under the Phase 001 skeleton roots.

## Contract Check

Phase 002 adds a read-only contract validation command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1
```

The script checks required shared-contract files, parses JSON schemas and examples, verifies required OpenAPI paths by text, and rejects runtime implementation artifacts under skeleton roots.

## Moving Into Later Phases

Later phases should use the host directories created here, but must wait for their own architect handoff before adding contracts, services, pages, tests, or runtime configuration.

## Secret Handling

- Use `.env.example` for non-secret variable names only.
- Keep real keys, tokens, model credentials, production data, customer data, and sensitive test material outside the repository.
- Do not place generated credentials in placeholder directories.
