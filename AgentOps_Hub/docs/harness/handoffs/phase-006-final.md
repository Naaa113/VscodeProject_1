# Phase 006 最终交接

## phase status（阶段状态）

`completed`。

Phase 006 `web-console` 最小工作台已完成，并已通过 Window 3 Fix Pass 2 复审。最新 review 文件为 `docs/harness/handoffs/phase-006-review-fix-2.md`，结论为 `approve`。本阶段没有用户接受 residual risk 的例外裁决；剩余问题按债务登记保留，不视为本阶段阻断项。

## completed scope（完成范围）

- 新增 `apps/web-console` 最小 React + TypeScript + Vite 工作台运行时。
- 落地 Mock 登录联调、工单列表、工单详情、投诉分析任务入口、Agent 步骤链路和报告预览主链路。
- 新增前端类型镜像，覆盖认证、当前用户、工单、AI 任务、Agent step、报告、引用和统一错误响应。
- 新增显式 `MockAuthClient`、`MockTicketClient`、`MockAiTaskClient` 和对应 fixture，保持 Mock 边界清晰可识别。
- 报告预览展示事实、推断、风险、建议和引用来源；Agent 步骤展示节点名、状态、摘要、错误和引用。
- 报告区实现 `waiting_approval`、`not_ready`、`not_found` 和通用 `error` 的独立状态分支。
- 高风险或低置信度结果只停留在 `waiting_human` / `waiting_approval` 展示态，不提供审批通过、动作执行、自动派单、工单状态变更或通知入口。
- 新增 `scripts/validate-web-console.ps1`，统一运行前端类型检查、测试和构建验证。

## unchanged contracts（未改变契约）

- 未修改 `packages/shared-contracts/**`。
- `POST /api/auth/login`、`GET /api/auth/me`、`GET /api/tickets`、`GET /api/tickets/{id}`、`POST /api/ai/tasks`、`GET /api/ai/tasks/{id}`、`GET /api/ai/tasks/{id}/steps`、`GET /api/ai/tasks/{id}/report` 的契约语义保持不变。
- `Authorization: Bearer <token>`、`X-Tenant-Id`、`X-Trace-Id` 和 `ErrorResponse` 形状保持不变。
- `AiTaskStatus`、`AgentStep.status`、`TicketStatus`、`TicketPriority` 枚举保持不变。
- `ticket.search.v1` 仍为只读工具，`ticket.create_followup.v1` 仍未实现执行 bridge。
- 未新增真实 RAG、真实网关、真实 SSE、审批实例、动作命令、自动派单或通知能力。

## changed contracts / authority / transition（契约、权威与生命周期变化）

- 契约消费：前端在 `apps/web-console/src/api/contracts.ts` 中新增与当前 OpenAPI 对齐的类型镜像，用于消费已冻结的认证、工单、AI 任务、Agent step、报告和错误响应契约。
- 契约消费：前端私有 Mock 标记、风险派生和报告面板状态只保留在视图模型层，没有写回共享契约。
- 权威：`apps/web-console` 当前只拥有页面状态、会话摘要、视图模型和显式 Mock adapter；不拥有身份、工单、Agent、知识库、审批或通知事实源。
- 生命周期：浏览器侧已落地登录、工单列表 / 详情、AI 任务详情、Agent step 时间线和报告预览状态分支。
- 行为：高风险结果在前端可见但不可执行，保持“展示风险边界，不提供动作入口”的策略。
- 验证入口：新增 `scripts/validate-web-console.ps1` 作为前端阶段本地验证脚本。

## validation summary（验证摘要）

Window 2 初次实现验证：

- `npm.cmd --prefix apps/web-console run typecheck`：通过。
- `npm.cmd --prefix apps/web-console run test`：通过，`src/mocks/mockClients.test.ts` 共 4 个测试通过。
- `npm.cmd --prefix apps/web-console run build`：通过。
- `powershell -ExecutionPolicy Bypass -File scripts\validate-web-console.ps1`：通过。
- `powershell -ExecutionPolicy Bypass -File scripts\validate-contracts.ps1`：失败。失败原因不是 Phase 006 功能错误，而是脚本仍按 Phase 002 的“禁止实现产物”规则把 `apps/web-console` 运行时识别为 forbidden implementation artifact。

Window 3 初次 review 结论为 `require fixes`，发现：

- `Citation` 契约镜像漂移。
- 报告预览缺少“风险”分区。
- 步骤时间线未展示错误信息。

Fix Pass 1 后复审仍为 `require fixes`，继续发现：

- `AiTask.error`、`AgentStep.error` 和 `Citation.source_uri` 的类型镜像仍有 nullable 放宽问题。
- 报告区没有独立的 `waiting_approval` / `not_found` / “尚未生成”状态分支。

Fix Pass 2 修复后复审通过：

- `npm.cmd --prefix apps/web-console run typecheck`：通过。
- `npm.cmd --prefix apps/web-console run test`：通过，`src/app/App.test.tsx` 5 个测试通过，`src/mocks/mockClients.test.ts` 4 个测试通过，共 9 个测试通过。
- `npm.cmd --prefix apps/web-console run build`：通过。
- `powershell -ExecutionPolicy Bypass -File scripts\validate-web-console.ps1`：通过。
- `docs/harness/handoffs/phase-006-review-fix-2.md`：结论为 `approve`。

补充说明：

- in-app Browser 的本地访问曾出现 `net::ERR_BLOCKED_BY_CLIENT`，因此浏览器内点击冒烟不是本阶段 approve 的前置条件。
- `scripts/validate-contracts.ps1` 的失败已转入治理债务，不视为 Phase 006 阻断。

## remaining debt（剩余债务）

- DEBT-004：向量检索方案仍 deferred，进入 RAG 阶段前必须收敛。
- DEBT-006：高风险动作审批边界仍为 D0；Phase 006 只展示 `waiting_human` / `waiting_approval`，未实现审批实例或动作命令。
- DEBT-007：已有最小 eval 样例，但仍缺真实 RAG / Agent 基准集。
- DEBT-010：前端已有报告预览，但报告存储格式和导出格式仍未冻结。
- DEBT-011：`identity-service` 仍缺生产级数据初始化、PostgreSQL 迁移、refresh token、集中式 token 撤销和完整 RBAC 管理 API。
- DEBT-012：`ticket-service` 仍缺生产级 Spring Boot、PostgreSQL 迁移、服务间鉴权、受控样例数据初始化、数据库分页索引、工单状态变更审计和完整 SLA 引擎。
- DEBT-013：`agent-service` 仍缺生产级 LangGraph / FastAPI、真实模型适配、真实 HTTP 工具调用、消息队列 / SSE 集成、持久化存储和完整 JSON Schema 事件校验。
- DEBT-014：`scripts/validate-contracts.ps1` 仍按早期“禁止实现产物”规则扫描前端运行时，对 Phase 006 产生假阳性失败。

## latest state for Window 0（Window 0 最新状态）

Window 0 下一次启动时应自动发现：

- 最新 final handoff：`docs/harness/handoffs/phase-006-final.md`。
- 当前 Steering 状态：`handoff_done`。
- 最新 Review 结论：`docs/harness/handoffs/phase-006-review-fix-2.md`，结论为 `approve`。
- `current-state.md` 已更新为 Phase 006 完成态。
- 当前仓库已具备身份域、工单域、Agent 编排域和前端工作台的最小运行时。
- 下一阶段尚未冻结；不得绕过 Window 0 直接进入实现。

## recommended candidate inputs for Window 0（推荐候选输入）

推荐 primary candidate：Phase 007 RAG 最小知识库闭环。

候选输入：

- Phase 003 已提供身份上下文、租户校验、权限摘要和身份审计基础。
- Phase 004 已提供客户与工单事实源、工单查询 API、详情 API、`ticket.search.v1` 只读契约、租户隔离和工单审计字段。
- Phase 005 已提供 Agent 本地闭环、Agent run / step 状态、显式 Mock 工具与知识来源、结构化报告、人工确认占位和最小 eval 样例。
- Phase 006 已提供工单、任务、步骤、引用和报告预览入口，为真实知识引用和文档追踪提供前端承载面。
- Phase 007 应限定为文档上传登记、解析状态、最小检索闭环、引用溯源和最小评测样例，不应顺手实现审批动作执行、生产网关鉴权、消息队列、自动通知或工单状态变更。
- `scripts/validate-contracts.ps1` 的阶段感知冲突应在后续阶段尽快收敛，避免继续影响契约校验可信度。

Fallback candidate：Phase 008 人工审批与动作命令。只有用户明确要求优先收敛 DEBT-006 时，Window 0 才应评估 fallback；即便选择 fallback，也不得绕过身份、租户、权限、工单事实源、Agent 风险标记和审计上下文。

## files changed in this handoff（本窗口修改文件）

- `docs/harness/state/current-state.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-006-final.md`

## 提交状态

用户未明确要求 commit。本窗口未 stage、未提交、未清理既有 dirty / untracked 变更。
