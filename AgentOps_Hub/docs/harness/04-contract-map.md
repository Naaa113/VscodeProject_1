# Contract Map

## 契约原则

- 契约先于实现。
- 契约必须可版本化、可验证、可生成示例。
- REST API、事件消息、实时推送、工具调用和数据库状态枚举必须保持一致。
- Agent 工具调用不是“内部随意调用”，而是受权限、幂等、审计和审批约束的契约。

## Phase 002 契约落地状态

Phase 002 已在 `packages/shared-contracts` 中创建 v1 契约草案。该草案是后续服务、前端和 Agent 实现的共同语言，但不表示任何运行时服务已经存在。

权威入口：

- OpenAPI：`packages/shared-contracts/openapi/agentops-api.v1.yaml`
- 通用 schema：`packages/shared-contracts/schemas/common.v1.schema.json`
- 状态枚举：`packages/shared-contracts/schemas/status.v1.schema.json`
- 错误码：`packages/shared-contracts/errors/error-codes.v1.json`
- 事件：`packages/shared-contracts/events/*.v1.schema.json`
- 工具：`packages/shared-contracts/tools/*.v1.schema.json`
- 示例：`packages/shared-contracts/examples/**`
- 清单：`packages/shared-contracts/manifest.v1.json`
- 校验脚本：`scripts/validate-contracts.ps1`

## Phase 003 认证契约落地状态

Phase 003 已在 `services/identity-service` 中实现认证契约的最小运行时闭环。该实现只覆盖身份域，不表示网关、前端、工单、Agent、RAG 或审批能力已经存在。

已落地契约：

- `POST /api/auth/login`
- `GET /api/auth/me`
- `LoginRequest`
- `LoginResponse`
- `CurrentUserResponse`
- `ErrorResponse`
- `Authorization: Bearer <token>`
- `X-Tenant-Id`
- `X-Trace-Id`

Phase 003 对 `packages/shared-contracts/openapi/agentops-api.v1.yaml` 仅做认证相关补齐，包括 bearer token 安全声明、认证失败响应和当前用户接口响应约束。未新增跨服务事件、Agent 工具契约或业务 API。

当前身份服务行为约束：

- 登录失败统一返回 `AUTH_INVALID_CREDENTIALS`，不泄露租户、用户、密码或用户状态差异。
- token 过期返回 `AUTH_TOKEN_EXPIRED`。
- token 无效、租户不匹配或用户禁用返回 `AUTH_FORBIDDEN`。
- 租户缺失返回 `TENANT_REQUIRED`。
- 失败响应保持统一错误形状：`error_code`、`message`、`trace_id`、`retryable`，允许有 `details`。
- token 载荷不得包含明文密码、密码哈希、邮箱、手机号或客户数据。

## Phase 004 工单契约落地状态

Phase 004 已在 `services/ticket-service` 中实现工单契约的最小运行时闭环。该实现只覆盖工单域，不表示 Agent、RAG、审批、前端、网关或通知能力已经存在。

已落地契约：

- `GET /api/tickets`
- `POST /api/tickets`
- `GET /api/tickets/{id}`
- `TicketStatus`
- `TicketPriority`
- `CustomerSummary`
- `CreateCustomerInput`
- `CreateTicketRequest`
- `TicketSummary`
- `TicketDetail`
- `TicketListResponse`
- `ticket.search.v1`

Phase 004 对 `packages/shared-contracts/openapi/agentops-api.v1.yaml` 做工单相关补齐，包括 bearer token 安全声明、工单过滤参数、客户摘要、分类、SLA 截止时间、工单审计字段和统一错误响应。Phase 004 对 `packages/shared-contracts/tools/ticket.search.v1.schema.json` 做只读检索字段补齐，未新增工单事件、Agent 工具执行桥接或高风险动作契约。

当前工单服务行为约束：

- 所有工单 API 必须要求 `Authorization: Bearer <token>`、`X-Tenant-Id` 和 `X-Trace-Id`。
- token 中的 `tenant_id` 必须与 `X-Tenant-Id` 一致。
- 创建工单时服务端从身份上下文写入 `tenant_id` 和 `created_by`，不得信任请求体中的租户或创建人。
- 新建工单默认状态为 `open`，本阶段不提供状态变更 API。
- 列表查询只能返回当前租户数据。
- 详情查询遇到跨租户工单时必须返回 `RESOURCE_NOT_FOUND`，不得泄露该工单是否存在于其他租户。
- `ticket.search.v1` 保持只读语义，`requires_approval` 仍为 `false`。

## Phase 005 Agent 契约落地状态

Phase 005 已在 `ai-services/agent-service` 中实现 Agent 契约的最小本地闭环。该实现只覆盖本地 Agent 编排、契约化 Mock 工具、Mock 知识来源、Mock 报告保存、事件记录和最小 eval，不表示真实 LangGraph、FastAPI、消息队列、SSE、真实 RAG、真实报告服务或审批动作执行已经存在。

已落地或验证的契约：

- `ai.task.created.v1` 作为本地任务输入语义。
- `agent.run.started.v1`
- `agent.step.completed.v1`
- `agent.run.completed.v1`
- `agent.run.failed.v1`
- `ticket.search.v1` 的只读 Agent 消费方式。
- `knowledge.search.v1` 的显式 Mock 来源标记。
- `report.save.v1` 的本地 Mock 保存语义。

Phase 005 对 `packages/shared-contracts/tools/knowledge.search.v1.schema.json` 做非破坏性补齐，允许知识结果、citation 和 response output 显式携带 Mock 来源标记；对 `knowledge.search.success.v1.json` 补充 Mock 示例；对契约校验脚本补充 Agent 服务验证入口和 Mock 标记检查。Fix Pass 1 修复了本地 Agent 事件记录，确保 run started、run completed、run failed 和 step completed 事件均带 v1 envelope，并为 `agent.run.started.v1` payload 补齐 `status: "started"`。

当前 Agent 服务行为约束：

- `ticket.search.v1` 只能作为只读检索工具消费，不得触发工单写入、状态变更、自动派单或跟进任务创建。
- `knowledge.search.v1` 在本阶段只能返回显式 Mock 来源，不得宣称真实 RAG、Embedding、向量库或文档索引已完成。
- `report.save.v1` 在本阶段只能代表本地 Mock 保存，不得宣称 Java 报告服务、对象存储或生产报告 API 已完成。
- Agent 报告必须区分事实、推断、风险、建议和引用。
- 高风险或低置信度输出必须进入 `waiting_human` 或 `waiting_approval` 占位，不得生成可执行动作命令。
- Agent 不得直接访问身份、工单、知识库或审批数据库。
- 本地事件可以写入内存或本地 run store，但事件形状必须保持 v1 envelope。

## Phase 006 前端契约消费落地状态

Phase 006 已在 `apps/web-console` 中落地最小工作台，对认证、工单、AI 任务、Agent step、报告和统一错误响应契约进行了 Mock-first 消费。该实现只覆盖前端工作台与显式 Mock adapter，不表示真实网关、真实 AI 任务 HTTP 入口、真实 SSE、真实 RAG 或真实审批动作执行已经存在。

已消费或镜像的契约：

- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/tickets`
- `GET /api/tickets/{id}`
- `POST /api/ai/tasks`
- `GET /api/ai/tasks/{id}`
- `GET /api/ai/tasks/{id}/steps`
- `GET /api/ai/tasks/{id}/report`
- `TicketListResponse`
- `TicketDetail`
- `AiTask`
- `AgentStep`
- `ReportSummary`
- `Citation`
- `ErrorResponse`

Phase 006 未修改 `packages/shared-contracts/**`；前端只在 `apps/web-console/src/api/contracts.ts` 中维护与当前 OpenAPI 对齐的类型镜像，并在 UI 层通过独立视图模型派生 Mock 标记、风险分区和报告面板状态。

当前前端工作台行为约束：

- 前端只能消费公开契约字段或前端内部视图模型，不得私自创造跨服务事实字段并回写为共享契约。
- 报告预览必须展示事实、推断、风险、建议和引用来源；风险可由 `AiTask.blocking_reason`、`AgentStep.status` 和步骤错误派生，但这些派生字段只属于前端视图层。
- `RESOURCE_NOT_FOUND` 在报告区必须区分为“等待人工确认”“报告尚未生成”或“报告不存在 / 不可访问”，不得全部折叠为通用成功态或通用空态。
- 高风险或低置信度结果只能展示为 `waiting_human` / `waiting_approval`，不得提供审批通过、动作执行、工单状态变更、自动派单或通知入口。
- Mock 用户、Mock 工单、Mock Agent step、Mock 知识引用和 Mock 报告必须在代码或界面中可识别，不得伪装成生产能力。

## Phase 007 RAG 契约落地状态

Phase 007 已在 `ai-services/rag-service` 中实现 RAG 最小本地闭环，并让 `agent-service` 通过 `knowledge.search.v1` 消费非 Mock 本地文档引用。该实现只覆盖文档登记、解析、切片、本地确定性轻量索引、租户隔离检索、引用溯源、解析失败重试和最小 RAG eval，不表示生产级 FastAPI、对象存储、pgvector、Milvus、异步解析任务或真实上传入口已经存在。

已落地或验证的契约：

- `GET /api/knowledge/documents`
- `POST /api/knowledge/documents`
- `POST /api/knowledge/search`
- `DocumentSummary`
- `RegisterDocumentRequest`
- `KnowledgeSearchRequest`
- `KnowledgeSearchResponse`
- `Citation`
- `document.uploaded.v1`
- `document.indexed.v1`
- `knowledge.search.v1`

Phase 007 对 `packages/shared-contracts/openapi/agentops-api.v1.yaml` 做知识库和引用相关非破坏性补齐；对 `knowledge.search.v1` 补齐 `citation.source_uri` 并保留 `mock_source` 显式边界；对 `document.uploaded.v1` 和 `document.indexed.v1` 补齐文档文件名、知识库和租户相关 payload 字段；对示例和 `manifest.v1.json` 登记知识库 OpenAPI 示例。Phase 007 同时将 `scripts/validate-contracts.ps1` 调整为阶段感知规则，不再把已批准的 `apps/web-console`、`agent-service`、`rag-service` 判定为 forbidden implementation artifact。

当前 RAG 契约行为约束：

- `knowledge.search.v1` 保持只读语义，`requires_approval` 仍为 `false`。
- `knowledge.search.v1` 保持存储无关，不暴露 pgvector、Milvus、Elasticsearch 或 OpenSearch 绑定字段。
- 本地 RAG 检索结果必须按 `tenant_id` 隔离，只返回当前租户已 `indexed` 的文档片段。
- 检索结果中的 citation 必须能追溯到 `document_id` 和 `chunk_id`，可通过 `source_uri` 定位本地片段来源。
- 无命中返回成功和空 `items`，不得伪造引用。
- 解析、重试和检索失败必须使用统一错误形状；跨租户解析或重试返回不可泄露的 `RESOURCE_NOT_FOUND`。
- RAG 失败不得静默降级为 Mock 结果；Mock 来源、本地 RAG 来源和未来生产 RAG 来源必须可区分。
- 高风险或低置信度 Agent 输出仍只能进入 `waiting_human` / `waiting_approval`，不得生成可执行业务动作。

## MVP 契约面

| 契约 | 生产者 | 消费者 | MVP 范围 |
|---|---|---|---|
| 身份认证 API | `identity-service` | `web-console`, `api-gateway` | 登录、当前用户、租户上下文；refresh token 留待后续阶段 |
| 工单 API | `ticket-service` | `web-console`, `agent-service` 工具 | Phase 004 已落地创建、列表、详情、状态、优先级、分类、SLA 字段和租户隔离 |
| AI 任务 API | Java 任务入口或 `workflow-service` | `web-console` | 创建任务、查询任务、查询步骤、查询报告 |
| Agent 任务事件 | Java 任务入口或本地 fixture | `agent-service` | `ai.task.created.v1`；Phase 005 已用于本地输入语义 |
| Agent 状态事件 | `agent-service` | Java 任务入口、前端推送通道或本地 run store | Phase 005 已在本地记录 `agent.run.started.v1`、`agent.step.completed.v1`、`agent.run.failed.v1`、`agent.run.completed.v1` envelope |
| 文档 API | `rag-service` 或 Java facade | `web-console` | Phase 007 已落地本地文档登记、解析状态、失败重试和索引结果；真实上传和生产对象存储留待后续 |
| RAG 查询 API | `rag-service` | `agent-service` | Phase 007 已落地 query、filters、top_k、citations 和本地非 Mock 文档片段引用；生产向量后端留待后续 |
| 动作命令 API | `workflow-service` | `agent-service` | 创建跟进任务请求、审批前置检查 |
| 实时状态通道 | Gateway/任务服务 | `web-console` | SSE 或 WebSocket 二选一 |

## 关键状态枚举

### `ai_task.status`

```text
pending
running
waiting_approval
success
failed
cancelled
```

### `agent_step.status`

```text
pending
running
success
failed
skipped
waiting_human
```

### `approval_instance.status`

```text
pending
approved
rejected
cancelled
expired
```

### `document.parse_status`

```text
uploaded
parsing
indexed
failed
```

## MVP OpenAPI v1 草案清单

| API | 方法 | 说明 |
|---|---|---|
| `/api/auth/login` | POST | 用户登录并返回 token |
| `/api/auth/me` | GET | 获取当前用户、租户和权限摘要 |
| `/api/tickets` | GET/POST | 工单列表和创建 |
| `/api/tickets/{id}` | GET | 工单详情 |
| `/api/ai/tasks` | GET/POST | AI 任务列表和发起 |
| `/api/ai/tasks/{id}` | GET | AI 任务状态 |
| `/api/ai/tasks/{id}/steps` | GET | Agent 步骤链路 |
| `/api/ai/tasks/{id}/report` | GET | 任务报告 |
| `/api/knowledge/documents` | GET/POST | 文档列表和上传登记 |
| `/api/knowledge/search` | POST | 知识库检索 |
| `/api/stream/tasks/{id}` | GET | SSE 状态流候选 |

## MVP 事件 v1 草案清单

| 事件 | 生产者 | 关键字段 |
|---|---|---|
| `ai.task.created.v1` | Java 任务入口 | `task_id`, `tenant_id`, `created_by`, `task_type`, `prompt`, `priority` |
| `agent.run.started.v1` | `agent-service` | `task_id`, `run_id`, `graph_name`, `graph_version`, `started_at` |
| `agent.step.completed.v1` | `agent-service` | `task_id`, `run_id`, `step_id`, `agent_name`, `status`, `summary`, `citations` |
| `agent.run.completed.v1` | `agent-service` | `task_id`, `run_id`, `report_id`, `status` |
| `agent.run.failed.v1` | `agent-service` | `task_id`, `run_id`, `error_code`, `message`, `retryable` |
| `document.uploaded.v1` | 文档入口 | `document_id`, `knowledge_base_id`, `tenant_id`, `filename`, `object_key`, `parse_status` |
| `document.indexed.v1` | `rag-service` | `document_id`, `knowledge_base_id`, `tenant_id`, `chunk_count`, `index_ref`, `parse_status` |

## 工具调用契约

Agent 工具必须声明：

- 工具名和版本。
- 输入 schema。
- 输出 schema。
- 权限要求。
- 是否需要人工审批。
- 幂等键规则。
- 审计字段。
- 超时、重试和降级策略。

MVP 工具 v1 草案：

| 工具 | 宿主 | 说明 |
|---|---|---|
| `ticket.search.v1` | `ticket-service` | Phase 004 已冻结只读检索语义，按时间、分类、SLA、客户等条件检索工单 |
| `ticket.create_followup.v1` | `ticket-service` 或 `workflow-service` | 创建跟进任务，必须带幂等键 |
| `knowledge.search.v1` | `rag-service` | 返回文档片段与引用来源；Phase 007 已验证本地非 Mock RAG 来源，仍保持只读和存储无关 |
| `report.save.v1` | Java 任务/报告服务 | 保存结构化报告；Phase 005 仅允许本地 Mock 保存 |

## 契约冻结标准

- 有 schema。
- 有至少一个成功示例和一个失败示例。
- 有错误码映射。
- 有版本号。
- 有兼容性说明。
- 有契约测试或 schema 校验入口。

Phase 002 已满足以上冻结标准的草案级要求；后续实现阶段如需修改字段、枚举、错误码或状态语义，必须按契约变更规则评审。
