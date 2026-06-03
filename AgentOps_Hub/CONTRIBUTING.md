# Contributing

This repository is governed by the phased harness in `docs/harness`.

## Phase Discipline

- Follow the current phase architect handoff before changing files.
- Do not add business APIs, runtime services, database migrations, frontend pages, Agent workflows, helpers, adapters, fallbacks, or bridges in Phase 001.
- Keep host ownership aligned with `docs/harness/03-host-ownership.md`.
- Put cross-service contract work in a future contract phase, not in service placeholders.

## Local Checks

Before handing work to review, run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-skeleton.ps1
```

Also inspect:

```powershell
git status --short --untracked-files=all
```

Only stage files that belong to the current implementation window.
