# Phase 004 实现交接

## Phase

Phase 004：`ticket-service` 最小工单模型。

## Scope

本次实现严格限定在 `phase-004-architect.md` 批准范围内：

- 补齐工单 OpenAPI 和 `ticket.search.v1` 工具契约。
- 落地 `services/ticket-service` 最小 Java + H2 本地运行时。
- 实现工单创建、列表查询和详情读取。
- 实现客户基础模型、工单状态、优先级、分类、SLA 截止时间、租户隔离和审计字段。
- 新增本地验证脚本和开发说明。

## Completed

### 契约补齐

- `packages/shared-contracts/openapi/agentops-api.v1.yaml`
  - 为 `GET /api/tickets`、`POST /api/tickets`、`GET /api/tickets/{id}` 补齐 `bearerAuth`。
  - 增加 `401`、`403` 和统一错误响应。
  - 增加查询过滤参数：`status`、`priority`、`category`、`customer_id`、`created_from`、`created_to`、`sla_due_before`、`query`。
  - 增加 `TicketStatus`、`TicketPriority`、`CustomerSummary`、`CreateCustomerInput`。
  - 让 `CreateTicketRequest` 表达 `customer_id` 或 `customer` 二选一。
  - 让 `TicketSummary` / `TicketDetail` 表达客户、分类、SLA 和审计字段。
- `packages/shared-contracts/tools/ticket.search.v1.schema.json`
  - 保持只读工具语义和 `requires_approval: false`。
  - 增加客户、分类、SLA 和租户输出字段。
- 新增和更新工单 OpenAPI / 工具示例：
  - `tickets-list.success.v1.json`
  - `tickets-create.success.v1.json`
  - `tickets-detail.success.v1.json`
  - `tickets-detail.not-found.v1.json`
  - `ticket.search.success.v1.json`
  - `ticket.search.failure.v1.json`
- 更新 `manifest.v1.json` 和 `validate-contracts.ps1`。

### 运行时实现

- 新增 `services/ticket-service/ticket-service.maven.xml` 和 `.mvn/maven.config`。
- 新增 `services/ticket-service/app/main/resources/schema.sql`，包含：
  - `customer`
  - `ticket`
  - `ticket_audit_log`
- 新增最小 Java 运行时：
  - `TicketApplication`
  - `TicketHttpServer`
  - `TicketService`
  - `TicketAuthService`
  - `JwtVerifier`
  - `H2TicketRepository`
  - API model、错误码和异常类型。
- API 已实现：
  - `GET /api/tickets`
  - `POST /api/tickets`
  - `GET /api/tickets/{id}`
  - `GET /actuator/health`

### 行为实现

- 新建工单默认状态为 `open`。
- 工单优先级固定为 `low`、`normal`、`high`、`urgent`。
- 工单状态固定为 `open`、`in_progress`、`waiting_customer`、`resolved`、`closed`。
- 创建工单时从 token 和 header 写入 `tenant_id` 与 `created_by`。
- 列表查询支持时间范围、分类、优先级、状态、客户和普通文本查询。
- 详情查询按租户隔离，跨租户访问返回 `RESOURCE_NOT_FOUND`。
- 认证、租户缺失、权限不足、验证失败和未找到路径使用统一错误响应。
- 默认运行入口不写入演示客户或演示工单。

### 测试覆盖

- `TicketServiceTest`
  - 创建工单持久化客户、租户、审计和默认状态。
  - 按状态、优先级、分类、时间范围和查询词过滤。
  - 跨租户详情查询返回 `RESOURCE_NOT_FOUND`。
  - 创建请求必须提供且只能提供一个客户来源。
- `TicketHttpServerTest`
  - HTTP 创建、列表和详情流程符合契约形状。
  - 跨租户详情查询不泄露资源存在性。
  - 缺少 `X-Trace-Id` 和缺少权限时返回统一错误。

## Validation

已运行：

```powershell
mvn test
```

结果：通过。`ticket-service` 共 7 个测试，0 失败，0 错误。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1
```

结果：通过，输出 `Phase 002 contract validation passed.`。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-ticket-service.ps1
```

结果：通过，脚本内部再次运行 `mvn test`，输出 `Phase 004 ticket-service validation passed.`。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-identity-service.ps1
```

结果：通过，`identity-service` 共 8 个测试，0 失败，0 错误。

说明：直接运行 `.\scripts\validate-contracts.ps1` 时被本机 PowerShell Execution Policy 拦截；使用 `powershell -ExecutionPolicy Bypass -File ...` 后成功，符合 `docs/development/local-dev.md` 的推荐运行方式。

## Not Done

本阶段未实现，且按架构要求不应实现：

- Spring Boot、PostgreSQL 迁移、Docker、Kubernetes 或生产级部署。
- `identity-service` 代码修改。
- `workflow-service`、审批实例、动作命令或 `ticket.create_followup.v1` 执行。
- `agent-service`、RAG、LLM、前端、网关、通知或 SSE。
- 工单状态变更 API、自动派单、自动升级、自动关闭、完整 SLA 引擎。
- 工单事件契约。

## Debt Added/Closed

未关闭既有债务：

- DEBT-006：高风险动作审批边界未实现，仍需后续 workflow / approval 阶段处理。
- DEBT-011：身份服务生产化增强仍未处理。

建议新增 D2 债务：

- `ticket-service` 当前仍是最小本地 H2 闭环，生产化前需要补齐 PostgreSQL 迁移、受控样例数据初始化、服务间鉴权集成和状态变更审计。

## Risks

- `ticket-service` 通过本地 JWT 校验消费 Phase 003 token 载荷；后续引入网关或集中鉴权后，需要保持 `Authorization`、`X-Tenant-Id`、权限和 trace 语义不漂移。
- `CreateTicketRequest` 对客户输入做了 Phase 004 收敛；若未来前端需要独立客户管理 API，应在新阶段先冻结客户契约。
- 查询当前使用 H2 和内存过滤，适合最小闭环；数据量扩大后需要数据库分页和索引策略。

## Next Recommended Phase

建议进入 Window 3 Review/Eval，对 Phase 004 进行代码评审、安全检查、契约一致性复核和验收验证。
