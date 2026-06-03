# ticket-service

Phase 004 minimal ticket-domain runtime.

Ownership boundary:

- Owns customer, ticket, ticket status, priority, category, SLA due time, and ticket audit facts.
- Consumes JWT tenant/user context through the Phase 003 token shape.
- Does not own identity facts, Agent runs, approvals, RAG, notifications, gateway behavior, or high-risk action execution.

Implemented API surface:

- `GET /api/tickets`
- `POST /api/tickets`
- `GET /api/tickets/{id}`
- `GET /actuator/health`

Local validation:

```powershell
mvn test
```

From repository root:

```powershell
.\scripts\validate-ticket-service.ps1
```
