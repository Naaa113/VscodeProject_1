# Phase 004 最终交接

## phase status

completed

Phase 004 `ticket-service` 最小工单模型已完成 Window 4 收尾。最新评审文件为 `docs/harness/handoffs/phase-004-review.md`，结论为 `approve`，允许进入 Window 4。本阶段不存在 Fix Pass，也不存在用户接受 residual risk 后强行交接的情况。

## completed scope

- 已补齐工单 OpenAPI 契约，覆盖 `GET /api/tickets`、`POST /api/tickets`、`GET /api/tickets/{id}`、bearer token 安全声明、租户 header、trace header、过滤参数、客户摘要、分类、SLA 截止时间、审计字段和统一错误响应。
- 已补齐 `ticket.search.v1` 只读工具契约，保持 `requires_approval: false`，供后续 Agent 阶段按契约检索工单。
- 已新增 `services/ticket-service` 最小 Java + H2 运行时。
- 已实现客户与工单基础模型、工单创建、列表查询、详情读取、工单状态、优先级、分类、SLA 截止时间、租户隔离和工单审计字段。
- 已实现 `ticket:read` / `ticket:write` 权限校验、token 租户与 `X-Tenant-Id` 一致性校验、跨租户详情不可见行为和统一错误响应。
- 已新增 `scripts/validate-ticket-service.ps1` 作为 Phase 004 本地校验入口。

## unchanged contracts

- 认证契约保持不变：`POST /api/auth/login`、`GET /api/auth/me`、`Authorization: Bearer <token>`、`X-Tenant-Id`、`X-Trace-Id` 和统一错误响应语义继续沿用 Phase 003。
- 事件契约保持不变：本阶段未新增 `ticket.created.v1`、`ticket.updated.v1`、Agent 事件、workflow 事件或通知事件。
- `ticket.create_followup.v1`、`knowledge.search.v1`、`report.save.v1` 未被实现为运行时能力。
- 高风险动作审批边界未改变，仍不得由 Agent 或工单服务绕过审批执行。
- `identity-service` 代码和身份事实所有权未由本阶段改变。

## changed contracts / authority / transition

- Contract：`packages/shared-contracts/openapi/agentops-api.v1.yaml` 已补齐工单 REST 契约、过滤参数、客户输入、客户摘要、工单状态、工单优先级、SLA 字段、审计字段和工单错误响应示例。
- Contract：`packages/shared-contracts/tools/ticket.search.v1.schema.json` 已补齐客户、分类、SLA、租户和只读检索字段。
- Authority：`services/ticket-service` 成为客户、工单、工单状态、优先级、分类、SLA 字段和工单审计事实 owner。
- Authority：`services/identity-service` 仍是租户、用户、角色、权限、JWT 和身份审计事实 owner；`ticket-service` 只保存用户 ID 引用和租户 ID，不复制身份事实。
- Transition：工单创建生命周期已落地为请求接收、认证校验、租户校验、权限校验、请求体验证、客户解析或创建、工单持久化为 `open`、工单审计和响应创建。
- Transition：工单详情按当前租户查找；跨租户资源和不存在资源均返回 `RESOURCE_NOT_FOUND`。
- Behavior：新建工单默认状态为 `open`，本阶段不提供工单状态变更 API。

## validation summary

Window 3 已记录并通过以下验证：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1
```

结果：通过，输出 `Phase 002 contract validation passed.`。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-ticket-service.ps1
```

结果：通过；脚本内部运行 `mvn test`，`ticket-service` 共 7 个测试，0 失败，0 错误，并输出 `Phase 004 ticket-service validation passed.`。

```powershell
mvn test
```

运行目录：`services/ticket-service`。结果：通过，`ticket-service` 共 7 个测试，0 失败，0 错误。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-identity-service.ps1
```

结果：通过，`identity-service` 共 8 个测试，0 失败，0 错误，并输出 `Phase 003 identity-service validation passed.`。

## remaining debt

- DEBT-004：pgvector/Milvus 尚未选择，仍影响 RAG 最小闭环架构。
- DEBT-006：高风险动作审批边界未实现，仍为 D0 债务；Phase 004 未新增高风险动作执行能力，也未关闭该债务。
- DEBT-007：AI 质量指标只有概念，没有基准集，仍需在 Agent / RAG 阶段收敛。
- DEBT-008：观测栈尚未落地。
- DEBT-009：多租户深度隔离策略需要从身份、工单和后续服务持续验证。
- DEBT-010：报告存储格式和导出格式未冻结。
- DEBT-011：`identity-service` 仍缺少生产级数据初始化、PostgreSQL 迁移、refresh token、集中式 token 撤销和完整 RBAC 管理 API。
- DEBT-012：`ticket-service` 仍缺少生产级 Spring Boot、PostgreSQL 迁移、服务间鉴权集成、受控样例数据初始化、数据库分页索引、工单状态变更审计和完整 SLA 引擎。

## latest state for Window 0

Window 0 下一次启动时应自动发现：

- 最新 final handoff 是 `docs/harness/handoffs/phase-004-final.md`。
- 当前 Steering 状态为 `handoff_done`。
- Phase 004 `ticket-service` 最小工单模型已完成。
- 最新 review 文件是 `docs/harness/handoffs/phase-004-review.md`，结论为 `approve`。
- `identity-service` 和 `ticket-service` 均为最小 Java + H2 本地运行时，不是生产级 Spring Boot / PostgreSQL 服务。
- `packages/shared-contracts` 已包含认证和工单相关契约；工单事件、Agent 运行时、RAG、审批、前端、网关和通知仍未实现。
- 工作区仍保持多阶段 dirty / untracked 状态；Window 4 未 stage、未提交、未清理既有变更。

## recommended candidate inputs for Window 0

推荐 primary candidate：Phase 005 `agent-service` 最小 LangGraph 闭环。

推荐输入：

- Phase 003 已提供身份上下文、租户校验、权限摘要和身份审计基础。
- Phase 004 已提供客户与工单事实源、工单查询 API、工单详情 API、`ticket.search.v1` 只读契约、租户隔离和工单审计字段。
- Phase 005 可限定为最小 LangGraph 状态机、Planner / Retriever / Data Analyst / Risk / Supervisor / Report 简化节点、契约化工单检索、Agent run / step 状态、失败路径和最小 AI eval 样例。
- Phase 005 不应实现审批动作执行、RAG 真实检索、前端页面、网关生产鉴权、工单状态变更或高风险动作执行。
- 如果用户明确优先处理 DEBT-006，可由 Window 0 评估 Phase 008 作为 fallback，但不得绕过身份、租户、权限、工单事实源和审计上下文。

## files changed in this handoff

- `docs/harness/state/current-state.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-004-final.md`
