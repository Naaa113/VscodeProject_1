# Phase 004 架构交接

## 启动确认

- 最新 steering decision：`docs/harness/handoffs/steering-decision-phase-004.md`。
- 用户批准状态：该文件记录“已由用户在 2026-06-01 批准”。
- 当前状态来源：`docs/harness/state/current-state.md` 记录 Phase 003 已完成，Steering 状态为 `handoff_done`。
- 重复启动检查：`docs/harness/handoffs/phase-004-architect.md` 在本窗口写入前不存在。
- 冲突判断：`current-state.md` 尚未把“最新 Steering 决策”更新到 Phase 004，但其“下一步建议”和 Phase 004 steering decision 一致，均指向 `ticket-service` 最小工单模型；本窗口不视为冲突。

## 本阶段目标

Phase 004 的目标是落地 `ticket-service` 最小工单事实源，让平台拥有第一个可被前端和未来 Agent 工具消费的业务对象域。

本阶段只覆盖：

- 客户和工单基础模型。
- 工单创建、列表查询、详情读取。
- 工单状态、优先级、分类、SLA 截止时间。
- 租户隔离、当前用户归因和工单审计字段。
- `ticket.search.v1` 所需的稳定检索语义。
- 单元测试、集成测试、契约校验和本地验证脚本。

## 允许修改的文件范围

Window 2 只允许修改以下范围：

- `services/ticket-service/**`
- `packages/shared-contracts/openapi/agentops-api.v1.yaml`
- `packages/shared-contracts/tools/ticket.search.v1.schema.json`
- `packages/shared-contracts/examples/openapi/**`
- `packages/shared-contracts/examples/tools/ticket.search.*.json`
- `packages/shared-contracts/manifest.v1.json`
- `scripts/validate-contracts.ps1`
- `scripts/validate-ticket-service.ps1`
- `docs/development/**`
- `docs/harness/handoffs/phase-004-implementation.md`

仅当新增错误码确实无法复用现有错误码时，才允许修改：

- `packages/shared-contracts/errors/error-codes.v1.json`

## 禁止修改的文件范围

Window 2 不得修改以下范围，除非先停止并请求 Window 1 或 Window 0 重新决策：

- `services/identity-service/**`
- `services/workflow-service/**`
- `services/notification-service/**`
- `ai-services/**`
- `apps/**`
- `deploy/**`
- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/10-steering-state-machine.md`
- `packages/shared-contracts/events/**`
- `packages/shared-contracts/tools/ticket.create_followup.v1.schema.json`
- `packages/shared-contracts/tools/knowledge.search.v1.schema.json`
- `packages/shared-contracts/tools/report.save.v1.schema.json`

不得清理、回滚或重写本阶段无关的既有 dirty 变更。

## 受影响 host 和数据所有权

| Host | 本阶段状态 | 数据所有权 |
|---|---|---|
| `services/ticket-service` | 新增最小 Java 运行时 | 拥有 `customer`、`ticket`、工单状态、优先级、分类、SLA 字段和工单审计事实 |
| `services/identity-service` | 只作为身份上下文来源，不修改代码 | 拥有租户、用户、角色、权限、JWT 和身份审计事实 |
| `packages/shared-contracts` | 允许补齐工单 REST 与 `ticket.search.v1` 契约 | 拥有跨 host 字段、错误形状、示例和工具协议 |
| `ai-services/agent-service` | 不实现 | 未来只能通过 `ticket.search.v1` 或公开 API 查询工单 |
| `services/workflow-service` | 不实现 | 未来拥有审批实例和动作命令，不由 `ticket-service` 提前承担 |

`ticket-service` 可以保存 `created_by`、`updated_by` 等用户 ID 引用，但不得复制用户名、角色、权限、密码、token 或身份表。

## 必须保持稳定的契约和行为

### URL 与 API

- `GET /api/tickets`
- `POST /api/tickets`
- `GET /api/tickets/{id}`
- `Authorization: Bearer <token>`
- `X-Tenant-Id`
- `X-Trace-Id`
- 统一错误响应：`error_code`、`message`、`trace_id`、`retryable`，可带 `details`

### 状态与枚举

- 工单优先级固定为：`low`、`normal`、`high`、`urgent`。
- 工单最小状态固定为：`open`、`in_progress`、`waiting_customer`、`resolved`、`closed`。
- 新建工单默认状态必须是 `open`。
- 本阶段不提供状态变更 API；除创建时写入 `open` 外，不实现状态迁移。

### 租户隔离

- 所有工单 API 必须要求 `Authorization`、`X-Tenant-Id` 和 `X-Trace-Id`。
- token 中的 `tenant_id` 必须与 `X-Tenant-Id` 一致。
- 列表查询只能返回当前租户数据。
- 详情查询遇到跨租户工单时必须返回 `RESOURCE_NOT_FOUND`，不得泄露该工单是否存在于其他租户。
- 创建工单时，服务端必须从身份上下文写入 `tenant_id` 和 `created_by`，不得信任请求体中的租户或创建人。

## 允许新增的目录、文件、class、method 或 schema 类型

### `services/ticket-service`

允许创建与 Phase 003 身份服务风格一致的最小 Java 服务结构：

- `services/ticket-service/ticket-service.maven.xml`
- `services/ticket-service/.mvn/maven.config`
- `services/ticket-service/app/main/java/com/agentops/ticket/**`
- `services/ticket-service/app/main/resources/schema.sql`
- `services/ticket-service/app/main/resources/application.example.properties`
- `services/ticket-service/app/test/java/com/agentops/ticket/**`

允许新增的核心类型：

- `TicketApplication`
- `TicketHttpServer`
- `TicketService`
- `TicketRepository`
- `H2TicketRepository`
- `TicketAuthClient` 或等价身份上下文校验组件
- `TicketRequestHandler`
- `Ticket`, `Customer`, `TicketPriority`, `TicketStatus`, `AuditFields`
- `CreateTicketRequest`, `TicketSummaryResponse`, `TicketDetailResponse`, `TicketListResponse`
- `ErrorResponse`, `RequestContext`

允许新增的数据库对象：

- `customer`
- `ticket`
- `ticket_audit_log` 或等价工单审计表

### `packages/shared-contracts`

允许补齐以下 schema 或参数：

- `TicketStatus`
- `TicketPriority`
- `CustomerSummary`
- `CreateCustomerInput`
- 工单列表过滤参数：`status`、`priority`、`category`、`customer_id`、`created_from`、`created_to`、`sla_due_before`、`query`
- `TicketSummary` 与 `TicketDetail` 中的客户、分类和 SLA 字段
- `ticket.search.v1` 输入中的 `category`、`customer_id`、`sla_due_before`
- `ticket.search.v1` 输出中的 `tenant_id`、`customer_id`、`category`、`sla_due_at`

这些变更必须先写入契约、示例和校验脚本，再写业务实现。

## 不允许新增的 helper、adapter、fallback、bridge

本阶段不得新增：

- `agent-service` 到 `ticket-service` 的真实桥接调用。
- `workflow-service` 或审批 adapter。
- `ticket.create_followup.v1` 的执行 helper。
- RAG、LLM、Spring AI、LangGraph 或模型调用 helper。
- 绕过 `Authorization` 的本地开发后门。
- 将身份用户复制到工单库的同步 adapter。
- 自动派单、自动升级、自动关闭或高风险动作 fallback。
- 前端 mock bridge、网关代理或 SSE 推送通道。

## 需要新增或冻结的契约

### OpenAPI

必须先补齐并冻结：

- 三个工单 API 的 `bearerAuth` 安全声明。
- 三个工单 API 的 `401`、`403` 和统一错误响应。
- `GET /api/tickets` 过滤参数：`status`、`priority`、`category`、`customer_id`、`created_from`、`created_to`、`sla_due_before`、`query`。
- `CreateTicketRequest` 必须能表达最小客户输入或已有客户引用；实现验收时请求必须携带 `customer_id` 或 `customer` 之一。
- `TicketSummary` 和 `TicketDetail` 必须能表达 `customer`、`category`、`sla_due_at` 和 `audit`。
- `GET /api/tickets/{id}` 的跨租户不可见行为用 `RESOURCE_NOT_FOUND` 示例表达。

### 工具契约

必须冻结 `ticket.search.v1` 的只读语义：

- 宿主仍为 `ticket-service`。
- 权限仍为 `ticket:read`。
- `requires_approval` 仍为 `false`。
- 不要求幂等键。
- 必须携带 `tenant_id`、`requested_by`、`trace_id`。
- 查询范围必须只来自当前租户。
- 失败响应必须使用统一错误形状。

### 事件契约

本阶段不新增事件契约。不得发布 `ticket.created.v1`、`ticket.updated.v1` 或任何 Agent / workflow 事件。若实现窗口认为需要事件，必须停止并回到 Window 1。

## 状态生命周期和失败处理

### 工单创建

```text
request_received -> auth_checked -> tenant_checked -> validated -> persisted(open) -> response_created
```

要求：

- `auth_checked` 必须校验 bearer token。
- `tenant_checked` 必须校验 token 租户与 `X-Tenant-Id`。
- `validated` 必须校验标题、描述、优先级、客户输入和 SLA 时间。
- `persisted(open)` 必须写入 `tenant_id`、`created_by`、`created_at`。
- 创建成功返回 `201` 和 `TicketDetail`。

失败处理：

- 缺少租户：`TENANT_REQUIRED`。
- token 过期：`AUTH_TOKEN_EXPIRED`。
- token 无效、租户不匹配、权限不足：`AUTH_FORBIDDEN`。
- 请求体验证失败：`VALIDATION_FAILED`。
- 客户引用不存在且未提供可创建客户输入：`VALIDATION_FAILED`。
- 数据冲突：`CONFLICT`。
- 未预期错误：`INTERNAL_ERROR`。

### 工单查询

```text
request_received -> auth_checked -> tenant_checked -> filters_validated -> tenant_scoped_query -> response_created
```

要求：

- 分页参数沿用 `page` 和 `page_size`。
- 支持按时间范围、分类、优先级、状态查询。
- `query` 只能作为标题、描述或客户显示名的普通搜索条件，不得引入搜索引擎。
- 返回顺序默认按 `created_at` 倒序。

失败处理：

- 非法分页、时间范围或枚举值返回 `VALIDATION_FAILED`。
- 工具或 API 查询失败必须带 `trace_id`。

### 工单详情

```text
request_received -> auth_checked -> tenant_checked -> tenant_scoped_lookup -> response_created
```

要求：

- 只能按当前租户查询。
- 当前租户不存在该工单时返回 `RESOURCE_NOT_FOUND`。
- 跨租户工单也返回 `RESOURCE_NOT_FOUND`。

## 验收条件

Window 2 完成后必须满足：

- `services/ticket-service` 存在最小 Java 运行时和测试闭环。
- 可通过 API 创建带客户信息的样例工单。
- 可按时间范围、分类、优先级、状态查询工单。
- 可读取工单详情。
- 工单响应包含稳定的 `tenant_id`、客户摘要、状态、优先级、分类、SLA 截止时间和审计字段。
- `ticket.search.v1` 契约与工单查询语义一致。
- 租户隔离测试覆盖列表、详情和创建。
- 跨租户详情访问不泄露资源存在性。
- 认证失败、租户缺失、租户不匹配、权限不足和验证失败均返回统一错误响应。
- 默认运行入口不写入演示客户或演示工单；样例数据只能由测试 fixture 或明确的测试初始化创建。
- 未新增 Agent、RAG、workflow、前端、网关、通知或高风险动作执行能力。

## 必须运行的验证命令

Window 2 至少运行并记录结果：

```powershell
.\scripts\validate-contracts.ps1
```

```powershell
.\scripts\validate-ticket-service.ps1
```

```powershell
Push-Location services\ticket-service
mvn test
Pop-Location
```

如果 `validate-ticket-service.ps1` 内部已经执行 `mvn test`，仍需在 handoff 中明确说明。

建议同时运行：

```powershell
.\scripts\validate-identity-service.ps1
```

该命令用于确认 Phase 004 没有破坏身份上下文契约；如果因本地环境无法运行，必须记录原因。

## 实现窗口发现 blocker 时如何停止

Window 2 遇到以下情况必须停止，不得自行扩大范围：

- OpenAPI 需要破坏性修改才能表达客户或查询语义。
- `ticket.search.v1` 与 REST 工单查询语义无法保持一致。
- 必须修改 `identity-service` 才能完成身份校验。
- 需要新增审批、动作命令、Agent 调用、RAG 检索、前端页面或网关能力。
- 无法在不泄露跨租户资源存在性的情况下实现详情查询。
- 需要新增事件契约。
- 需要引入 Spring Boot、PostgreSQL 迁移、Docker 或生产级框架，且这不是最小运行时所必需。
- 测试必须依赖真实密钥、真实客户数据或外部网络。

停止时应写入 `docs/harness/handoffs/phase-004-implementation.md` 的阻断摘要，说明：

- 已完成的契约或代码变更。
- 阻断点。
- 已验证命令及结果。
- 需要 Window 1、Window 0 或用户裁决的问题。

## Window 2 交接要求

实现窗口完成后必须写入：

- `docs/harness/handoffs/phase-004-implementation.md`

交接内容必须包含：

- 变更摘要。
- 契约补齐清单。
- 运行时文件清单。
- 测试和验证命令结果。
- 未完成项。
- 新增债务建议。
- 是否建议进入 Window 3。

Window 1 到此停止。请用户批准后再进入 Window 2 实现。
