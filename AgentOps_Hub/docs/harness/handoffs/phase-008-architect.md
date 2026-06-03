# Phase 008 架构交接

## 启动确认

- 本窗口已列出 `docs/harness/handoffs`，最新 steering decision 为 `docs/harness/handoffs/steering-decision-phase-008.md`。
- `steering-decision-phase-008.md` 明确记录：Phase 008 已由用户在 2026-06-03 批准，Window 0 已停止，允许 Window 1 拆解架构。
- `docs/harness/handoffs/phase-008-architect.md` 在本次启动前不存在，因此本次不是重复启动。
- `docs/harness/state/current-state.md` 仍记录 Phase 007 RAG 最小知识库闭环为 `completed`，Steering 状态为 `handoff_done`。这与 Phase 008 steering decision 的“在 Phase 007 完成后选择下一阶段”一致，不构成冲突。
- 本文件只冻结 Phase 008 的 contract、边界、验收条件和禁止事项；不代表实现已完成。

## 本阶段目标

Phase 008 的目标是补齐高风险动作的人机协同闭环，处理 DEBT-006。

本阶段必须形成以下最小闭环：

1. 建立最小审批实例，记录申请人、租户、来源任务、风险原因、引用摘要、目标业务对象、状态和审计字段。
2. 建立最小审批记录，记录审批人、审批结论、理由、时间、trace、动作摘要和租户。
3. 建立最小动作命令，记录动作类型、目标对象、幂等键、状态、结果、错误和审计字段。
4. 将高风险或低置信度 Agent 建议从 `waiting_human` / `waiting_approval` 占位推进到真实 `approval_instance`，但不得自动执行动作。
5. 审批通过后才能创建并执行 `action_command`；审批拒绝、取消或过期后不得执行对应动作。
6. 将 `ticket.create_followup.v1` 纳入本阶段，限定为一个最小、可审计、可回放的跟进动作命令；它不得扩展为自动派单、通知发送、工单复杂状态机或完整流程引擎。
7. 前端只展示和提交审批意图，不能绕过后端直接构造审批结果或动作结果。

## Belongs：归属边界

### 受影响 host

| Host | Phase 008 归属 | 本阶段允许承担 | 本阶段不得承担 |
|---|---|---|---|
| `services/workflow-service` | 主实现 host | 审批实例、审批记录、动作命令、幂等键、最小跟进动作结果、审计记录 | 通知发送、完整流程引擎、复杂工单状态机、AI 推理、RAG 检索、无审计动作 |
| `packages/shared-contracts` | 契约权威 | 审批 API、动作命令 API、`ticket.create_followup.v1`、状态枚举、错误示例和示例 payload 的补齐或冻结 | 跟随某个服务私自漂移，破坏认证、工单、AI 任务和 RAG 既有契约 |
| `ai-services/agent-service` | 审批请求来源 | 在高风险或低置信度建议中按契约请求审批，记录 `approval_instance_id`、风险原因、引用和工具调用 | 直接写审批表、直接创建动作命令、绕过审批执行动作 |
| `apps/web-console` | 审批承载面 | 展示等待审批、审批通过、审批拒绝、动作成功和动作失败，提交批准 / 拒绝 / 取消意图 | 在浏览器侧构造内部数据库字段，直接执行动作，伪造审批人或动作结果 |
| `services/ticket-service` | 工单事实源 | 本阶段默认不修改；只作为 `ticket_id` 目标对象的事实归属 | 被 `workflow-service` 复制工单事实；被 Agent 直接写入；新增复杂状态机 |
| `services/identity-service` | 身份事实源 | 本阶段默认不修改；既有 token、租户、权限摘要作为审批上下文来源 | 被 `workflow-service` 复制用户、角色或权限事实 |
| `scripts` | 验证入口 | 新增 `validate-workflow-service.ps1`，必要时纳入 Agent / 前端验证入口 | 安装依赖、启动生产服务、迁移数据库或生成代码 |

### 数据所有权

| 数据 / 概念 | Owner | Phase 008 要求 |
|---|---|---|
| `approval_instance` | `workflow-service` | 拥有审批对象、申请人、租户、来源任务、目标对象、风险原因、引用摘要、状态、幂等键和审计字段 |
| `approval_record` | `workflow-service` | 拥有审批结论、审批人、理由、动作摘要、时间、trace 和租户 |
| `action_command` | `workflow-service` | 拥有动作类型、目标对象、幂等键、状态、结果、错误、审批关联和审计字段 |
| `idempotency_key` | `workflow-service` | 在同一 `tenant_id` 与动作语义内唯一；重复请求不得重复创建或执行动作 |
| `ticket.create_followup.v1` 结果 | `workflow-service` | 本阶段只记录最小跟进动作结果和目标 `ticket_id` 引用，不写入 `ticket-service` 工单事实表 |
| 工单、客户、SLA | `ticket-service` | Phase 008 不迁移、不复制；如必须新增工单事实写入，Window 2 必须停止 |
| 用户、角色、权限、租户 | `identity-service` | Phase 008 只消费 token / fixture 摘要，不复制身份事实表 |
| Agent run / step / report | `agent-service` | 只保存审批来源引用、风险理由和工具调用记录，不拥有审批结果事实 |
| 文档、切片、citation | `rag-service` | 只作为审批风险理由的引用摘要来源，不复制完整 RAG 内部事实 |

## Authority：允许范围与禁止范围

### 允许修改的文件范围

Window 2 只能在以下范围内实现：

- `services/workflow-service/**`
- `packages/shared-contracts/openapi/agentops-api.v1.yaml`
- `packages/shared-contracts/schemas/status.v1.schema.json`
- `packages/shared-contracts/schemas/common.v1.schema.json`
- `packages/shared-contracts/tools/ticket.create_followup.v1.schema.json`
- `packages/shared-contracts/examples/**`，仅限 Phase 008 审批、动作命令和跟进动作示例。
- `packages/shared-contracts/manifest.v1.json`
- `packages/shared-contracts/errors/error-codes.v1.json`，仅在现有错误码无法表达审批 / 动作失败时非破坏性新增。
- `ai-services/agent-service/**`，仅限高风险建议生成审批请求、消费 `ticket.create_followup.v1`、记录审批引用和测试 / eval。
- `apps/web-console/src/**`，仅限最小审批状态、审批决策和动作结果展示。
- `scripts/validate-contracts.ps1`
- `scripts/validate-workflow-service.ps1`
- `scripts/validate-agent-service.ps1`，仅在 Agent 验证入口需要纳入审批场景时允许修改。
- `scripts/validate-web-console.ps1`，仅在前端确实新增 Phase 008 UI 时允许修改。
- `docs/harness/handoffs/phase-008-implementation.md`，作为 Window 2 实现交接。

### 禁止修改的文件范围

Window 2 不得修改：

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/state/current-state.md`
- `services/identity-service/**`
- `services/ticket-service/**`
- `services/notification-service/**`
- `apps/api-gateway/**`
- `ai-services/rag-service/**`
- `deploy/**`
- 根目录构建、部署、容器、数据库迁移或生产配置文件，除非 Window 0 后续明确改判。

Window 2 如发现必须修改上述文件才能完成 Phase 008，必须停止并写入 blocker，不得自行扩大范围。

### 允许新增的目录、文件、class、method 或 schema 类型

允许 Window 2 新增以下类型的实现资产：

- `services/workflow-service/workflow-service.maven.xml`
- `services/workflow-service/.mvn/maven.config`
- `services/workflow-service/app/main/java/**`
- `services/workflow-service/app/main/resources/**`
- `services/workflow-service/app/test/java/**`
- `services/workflow-service/README.md` 的 Phase 008 更新。
- `services/workflow-service/app/main/resources/schema.sql`，仅限本地 H2 最小表结构，不代表生产 PostgreSQL 迁移。
- `scripts/validate-workflow-service.ps1`
- 审批内部类型：`ApprovalInstance`、`ApprovalRecord`、`ApprovalDecision`、`ApprovalStatus`、`ApprovalTarget`、`ApprovalCitationSummary`。
- 动作内部类型：`ActionCommand`、`ActionCommandStatus`、`ActionCommandResult`、`ActionType`、`IdempotencyRecord`。
- 最小服务 / repository / controller：`ApprovalController`、`ApprovalService`、`ActionCommandService`、`IdempotencyService`、`WorkflowAuditRepository` 或等价名称。
- 最小 method：`createApproval`、`getApproval`、`approveApproval`、`rejectApproval`、`cancelApproval`、`expirePendingApproval`、`createActionCommandAfterApproval`、`executeTicketFollowupCommand`、`getActionCommand`。
- 契约 schema：`ApprovalInstance`、`ApprovalRecord`、`CreateApprovalRequest`、`ApprovalDecisionRequest`、`ActionCommand`、`ActionCommandResult`、`TicketFollowupActionInput`、`TicketFollowupActionOutput`。
- 示例 payload：审批创建成功、审批详情、审批通过、审批拒绝、动作成功、动作失败、重复幂等请求、跨租户不可访问。

### 不允许新增的 helper / adapter / fallback / bridge

以下类型不得新增：

- 不得新增让 Agent 绕过 `workflow-service` 直接写审批或动作结果的 helper。
- 不得新增把审批失败静默当作成功的 fallback。
- 不得新增把动作执行失败静默替换为 Mock 成功结果的 fallback。
- 不得新增隐藏 Mock 来源或本地最小运行时边界的 adapter。
- 不得新增 `agent-service -> ticket-service` 直接写动作 bridge。
- 不得新增 `web-console -> ticket-service` 直接执行动作 bridge。
- 不得新增 `workflow-service -> notification-service` 通知发送 bridge。
- 不得新增真实 `api-gateway` 代理、真实 SSE、RabbitMQ、Kafka、异步任务平台、生产数据库迁移、完整工作流引擎或规则引擎。
- 不得新增工单状态机、自动派单策略、SLA 引擎、邮件、站内信、Webhook 或第三方触达。
- 不得新增 RAG 生产化、pgvector、Milvus、对象存储或真实上传相关 bridge。

## Contract：契约冻结与变更边界

### 必须保持稳定的 URL / API / 事件 / 状态 / 行为

必须保持稳定：

- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/tickets`
- `POST /api/tickets`
- `GET /api/tickets/{id}`
- `POST /api/ai/tasks`
- `GET /api/ai/tasks/{id}`
- `GET /api/ai/tasks/{id}/steps`
- `GET /api/ai/tasks/{id}/report`
- `GET /api/knowledge/documents`
- `POST /api/knowledge/documents`
- `POST /api/knowledge/search`
- `document.uploaded.v1`
- `document.indexed.v1`
- `agent.run.started.v1`
- `agent.step.completed.v1`
- `agent.run.completed.v1`
- `agent.run.failed.v1`
- `ticket.search.v1`
- `knowledge.search.v1`
- `report.save.v1`
- `ai_task.status`: `pending`、`running`、`waiting_approval`、`success`、`failed`、`cancelled`
- `agent_step.status`: `pending`、`running`、`success`、`failed`、`skipped`、`waiting_human`
- `approval_instance.status`: `pending`、`approved`、`rejected`、`cancelled`、`expired`
- `action_command.status`: `pending`、`success`、`failed`、`cancelled`
- `ErrorResponse` 统一错误形状：`error_code`、`message`、`trace_id`、`retryable`，允许 `details`。

必须保持的行为：

- `ticket.search.v1` 仍为只读。
- `knowledge.search.v1` 仍为只读，`requires_approval` 仍为 `false`。
- `ticket.create_followup.v1` 必须 `requires_approval: true`，且必须带 `idempotency_key`。
- 高风险或低置信度 Agent 输出不得自动执行业务动作。
- Agent 不得直接写业务事实表，不得直接创建已执行动作。
- 租户隔离必须贯穿审批创建、审批详情、审批决策、动作命令和前端展示。
- 跨租户审批或动作访问返回 `RESOURCE_NOT_FOUND`，不得泄露资源是否存在。

### 需要新增或冻结的 OpenAPI 契约

Window 2 必须在 `packages/shared-contracts/openapi/agentops-api.v1.yaml` 中新增或冻结以下最小 API。路径命名可在实现时保持一致性微调，但语义不得改变：

- `POST /api/approvals`：创建审批实例。请求必须包含 `source_task_id` 或等价来源引用、`target_type`、`target_id`、`action_type`、`risk_reason`、`requested_by`、`idempotency_key` 和可选 `citations` 摘要。
- `GET /api/approvals/{id}`：读取审批实例详情。只能返回当前租户可访问的数据。
- `POST /api/approvals/{id}/approve`：审批通过。只能由具备审批权限的用户调用；成功后创建并执行一个动作命令。
- `POST /api/approvals/{id}/reject`：审批拒绝。必须记录理由；不得创建或执行动作命令。
- `POST /api/approvals/{id}/cancel`：取消待审批实例。只能取消 `pending` 审批；不得执行动作命令。
- `GET /api/action-commands/{id}`：读取动作命令详情和结果。只能返回当前租户可访问的数据。

最小 schema 要求：

- `ApprovalInstance` 必须包含 `approval_instance_id`、`tenant_id`、`status`、`requested_by`、`source_type`、`source_id`、`target_type`、`target_id`、`action_type`、`risk_reason`、`idempotency_key`、`created_at`、`updated_at`、可选 `expires_at`、可选 `action_command_id` 和可选 `citations` 摘要。
- `ApprovalRecord` 必须包含 `approval_record_id`、`approval_instance_id`、`tenant_id`、`decision`、`decided_by`、`reason`、`action_summary`、`trace_id` 和 `created_at`。
- `ActionCommand` 必须包含 `action_command_id`、`approval_instance_id`、`tenant_id`、`action_type`、`target_type`、`target_id`、`idempotency_key`、`status`、`created_by`、`created_at`、可选 `executed_at`、可选 `result_payload` 和可选 `error`。
- `TicketFollowupActionInput` 必须沿用 `ticket.create_followup.v1` 的 `ticket_id`、`title`、`reason`、可选 `due_at` 和 `approval_policy`。
- `TicketFollowupActionOutput` 必须只表达最小动作结果，例如 `followup_task_ref`、`ticket_id`、`created_at` 和 `created_by`；不得写入或伪造 `ticket-service` 内部工单状态。

### 需要新增或冻结的工具契约

`ticket.create_followup.v1` 纳入 Phase 008，要求如下：

- `x_tool.host` 保持 `workflow-service`。
- `x_tool.requires_approval` 保持 `true`。
- `x_tool.permission` 至少表达 `ticket:followup:request` 或等价权限。
- request 必须包含 `tenant_id`、`requested_by`、`trace_id`、`idempotency_key` 和 `input`。
- response 的 `waiting_approval` 必须返回 `approval_instance_id`。
- 在审批通过前，运行时不得创建真实可执行的 `action_command`。如果旧示例保留 `action_command_status: pending`，Window 2 必须将其解释为兼容占位，或者更新示例使动作命令只在审批通过后出现。
- 失败必须返回统一 `error` 形状。
- 重复请求必须复用同一 `idempotency_key`；相同 payload 返回同一审批实例，不同 payload 返回 `CONFLICT`。

### 事件契约要求

Phase 008 默认不新增 RabbitMQ、Kafka 或真实事件发布。

允许 Window 2 新增本地 schema 草案或示例，用于描述未来事件，但不得把事件发布作为本阶段验收前置条件。若实现必须依赖以下事件才能完成闭环，必须停止并交回 Window 0：

- `approval.instance.created.v1`
- `approval.instance.decided.v1`
- `action.command.completed.v1`

本阶段真实验收以 OpenAPI、工具契约、本地状态和审计记录为准。

### 错误码要求

优先复用现有错误码：

- `APPROVAL_REQUIRED`
- `AGENT_TOOL_FORBIDDEN`
- `VALIDATION_FAILED`
- `RESOURCE_NOT_FOUND`
- `CONFLICT`
- `AUTH_FORBIDDEN`
- `DOWNSTREAM_UNAVAILABLE`
- `INTERNAL_ERROR`

只有现有错误码无法表达审批生命周期或动作执行失败时，才允许非破坏性新增错误码。新增错误码必须登记 owner、HTTP 状态和 `retryable`。

## Transition：状态生命周期和失败处理

### 审批生命周期

```text
pending -> approved
pending -> rejected
pending -> cancelled
pending -> expired
```

| 状态 | 创建者 / 迁移者 | 必须记录 | 用户可见性 | 失败处理 |
|---|---|---|---|---|
| `pending` | `workflow-service` 根据 Agent / 前端 / 工具请求创建 | 申请人、租户、来源任务、目标对象、风险原因、引用摘要、幂等键、trace | 前端报告区或审批详情可见 | 创建失败返回统一错误；重复幂等请求返回原审批 |
| `approved` | 具备审批权限的人工用户 | 审批人、理由、时间、动作摘要、trace | 前端显示审批通过和动作状态 | 通过后创建并执行一个动作命令；重复通过不得重复执行 |
| `rejected` | 具备审批权限的人工用户 | 审批人、拒绝理由、时间、trace | 前端显示审批拒绝 | 不得创建或执行动作命令 |
| `cancelled` | 申请人或具备审批权限的用户 | 取消人、原因、时间、trace | 前端显示已取消 | 不得创建或执行动作命令 |
| `expired` | `workflow-service` 本地生命周期逻辑 | 过期时间、系统原因、trace | 前端显示已过期 | 不得创建或执行动作命令 |

### 动作命令生命周期

```text
pending -> success
pending -> failed
pending -> cancelled
```

| 状态 | 创建者 / 迁移者 | 必须记录 | 用户可见性 | 失败处理 |
|---|---|---|---|---|
| `pending` | `workflow-service` 只能在审批 `approved` 后创建 | `approval_instance_id`、动作类型、目标对象、幂等键、创建人、trace | 前端可见动作处理中 | 若审批不是 `approved`，必须拒绝创建 |
| `success` | `workflow-service` 动作执行器 | 结果 payload、执行时间、trace | 前端可见动作成功 | 重复执行返回同一结果，不得二次生效 |
| `failed` | `workflow-service` 动作执行器 | 错误码、错误信息、`retryable`、trace | 前端可见动作失败 | 可重试时必须复用同一幂等键 |
| `cancelled` | `workflow-service` | 取消原因、时间、trace | 前端可见动作取消 | 不得继续执行 |

### 幂等要求

- 审批创建幂等范围为 `tenant_id + idempotency_key + action_type`。
- 相同幂等键和相同 payload 必须返回同一 `approval_instance_id`。
- 相同幂等键但 payload 不一致必须返回 `CONFLICT`。
- 审批通过重复提交必须返回同一 `action_command_id` 和同一执行结果，不得创建第二条动作命令。
- 动作执行重试必须复用同一 `idempotency_key`。
- 审批拒绝、取消或过期后，再次尝试执行动作必须返回 `CONFLICT` 或等价不可执行错误。

### 权限与租户要求

- 所有审批和动作 API 必须要求 `Authorization: Bearer <token>`、`X-Tenant-Id` 和 `X-Trace-Id`。
- token 中的 `tenant_id` 必须与 `X-Tenant-Id` 一致。
- 创建审批至少需要 `ticket:followup:request` 或等价权限。
- 审批通过、拒绝、取消至少需要 `approval:decide` 或等价权限；如果实现选择允许申请人取消自己的审批，必须在测试中明确覆盖。
- Agent 或系统用户不得自行审批。
- 跨租户读取、审批或动作访问必须返回 `RESOURCE_NOT_FOUND`，不得泄露资源是否存在。

### Agent 生命周期影响

- 高风险或低置信度建议必须继续进入 `waiting_human` / `waiting_approval`。
- Agent 可以通过 `ticket.create_followup.v1` 请求创建审批实例，但响应只能代表等待审批或失败。
- Agent 工具调用记录必须包含 `approval_instance_id`、`idempotency_key`、`risk_reason`、`citations` 摘要和 `trace_id`。
- Agent 不得在审批通过前创建 `action_command`。
- 审批失败或动作失败必须让 Agent / 前端可解释，不得静默降级为成功。

### 前端生命周期影响

- 报告区必须能区分等待审批、审批通过、审批拒绝、审批取消、审批过期、动作成功和动作失败。
- 前端只能发送 approve / reject / cancel 意图，不得构造内部 `approval_record`、`action_command` 或 `result_payload`。
- 前端可以继续使用显式 Mock fixture 展示审批链路，但 Mock 边界必须可识别。
- 前端不得把 `RESOURCE_NOT_FOUND` 折叠为成功空态。

## Behavior：行为边界、验收与停止条件

### 验收条件

Window 2 完成后必须满足：

- `services/workflow-service` 有可运行的最小本地审批与动作命令闭环。
- 可创建 `approval_instance`，并能按租户读取详情。
- 审批通过后只创建并执行一次 `action_command`。
- 审批拒绝、取消或过期后不会创建或执行动作命令。
- `ticket.create_followup.v1` 只能返回等待审批或失败；审批通过后的动作结果通过审批 / 动作 API 可追踪。
- 相同 `idempotency_key` 的重复请求不会重复创建审批或重复执行动作。
- 跨租户审批和动作读取不可泄露资源是否存在。
- 审批记录包含审批人、理由、时间、动作摘要、来源任务、trace、tenant 和引用摘要。
- Agent 高风险建议不会自动执行，且会记录 `approval_instance_id`。
- 前端能展示等待审批、审批结果和动作结果的最小状态。
- 失败响应保持统一错误形状。
- 未新增通知发送、真实网关、RabbitMQ、真实 SSE、复杂流程引擎、完整工单状态机、生产数据库迁移、生产级 RAG 或真实对象存储。

### 必须运行的验证命令

Window 1 不运行以下命令。Window 2 实现完成后必须运行并记录结果：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-contracts.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-workflow-service.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-agent-service.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-web-console.ps1
```

如果 Window 2 修改了认证、工单或知识库相关契约字段，还必须运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-identity-service.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-ticket-service.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-rag-service.ps1
```

如果 Window 2 没有修改 `apps/web-console/src/**`，必须在实现 handoff 中说明为什么前端验证可以不运行；否则 `validate-web-console.ps1` 为必跑项。

### 实现窗口发现 blocker 时的停止规则

Window 2 遇到以下任一情况必须停止，不得绕过：

- 当前实现需要修改禁止范围内的文件。
- 必须引入 Spring Boot、真实网关、RabbitMQ、Kafka、真实 SSE、通知发送、复杂流程引擎、生产数据库迁移、完整工单状态机或生产对象存储才能完成闭环。
- 必须修改 `ticket-service` 才能表达跟进任务事实。
- Agent 只能通过直接写数据库或调用未冻结内部接口才能创建审批或动作。
- 前端必须绕过 `workflow-service` 才能展示或提交审批。
- 无法保证审批、动作命令或动作结果按 `tenant_id` 隔离。
- 无法实现幂等键重复请求不重复执行。
- 审批失败或动作失败只能靠 Mock 成功结果掩盖。
- 需要新增真实事件发布、消息队列消费者或异步任务平台才能表达生命周期。
- 验证命令失败且无法在当前冻结范围内修复。

停止时必须写入 `docs/harness/handoffs/phase-008-implementation.md`，说明 blocker、已完成内容、未完成内容、失败命令和建议交回 Window 0 或用户确认的决策点。

## Window 2 任务拆分建议

1. 先补齐审批、动作命令和 `ticket.create_followup.v1` 契约，修正等待审批时动作命令不可执行的语义。
2. 在 `services/workflow-service` 中创建最小本地 Java 运行时、H2 schema、审批 / 动作领域模型和测试。
3. 实现审批创建、详情、通过、拒绝、取消和动作命令详情的最小 API。
4. 实现幂等键处理和跨租户不可泄露错误语义。
5. 让 `agent-service` 在高风险跟进建议中通过契约请求审批，并记录 `approval_instance_id`。
6. 让 `apps/web-console` 展示最小审批状态、审批决策和动作结果。
7. 新增 `scripts/validate-workflow-service.ps1`，并运行所有必需验证命令。
