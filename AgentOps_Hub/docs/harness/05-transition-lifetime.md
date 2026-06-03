# Transition Lifetime

## 状态迁移原则

AgentOps Hub 的核心复杂度来自长任务、跨服务动作、人工审批和 AI 不确定性。所有跨阶段实现必须显式描述状态生命周期，避免“请求成功但业务状态不可解释”。

## Phase 002 契约映射

Phase 002 已将本文件中的生命周期要求映射到 `packages/shared-contracts` v1 契约草案：

- 状态枚举位于 `packages/shared-contracts/schemas/status.v1.schema.json`。
- AI 任务、Agent run/step、文档处理、审批和动作命令相关字段位于 OpenAPI、事件 schema 和工具 schema 中。
- Agent step 失败状态必须携带错误码、错误信息和可重试标记。
- 工具失败状态必须携带统一错误形状，包含 `error_code`、`message`、`trace_id` 和 `retryable`。
- 知识检索中的 `index_ref` 保持存储无关，不绑定 pgvector 或 Milvus。

这些契约不代表业务服务已经实现，只作为后续阶段的状态与失败语义约束。

## Phase 003 身份生命周期落地状态

Phase 003 已在 `services/identity-service` 中落地最小身份生命周期。该落地只覆盖认证、当前用户、JWT token、租户上下文、权限摘要和身份审计，不覆盖 refresh token、集中式 token 撤销、完整 RBAC 管理 API 或生产级数据迁移。

已实现生命周期：

- 登录成功：请求校验、租户解析、凭据校验、token 签发、登录成功审计。
- 登录失败：租户缺失、凭据错误、用户禁用、token 签发失败均返回统一错误并写入登录失败审计。
- 当前用户：解析 token、校验 token、校验租户一致性、加载用户、返回角色和权限摘要。
- 当前用户失败：租户缺失、token 过期、token 无效、租户不匹配、用户禁用均返回统一错误并写入身份审计。
- token：签发、有效、过期；本阶段不实现 refresh token 和集中式撤销表。

当前约束：

- 用户禁用后，即使 token 尚未过期，`GET /api/auth/me` 也必须失败。
- `X-Tenant-Id` 与 token 中的 `tenant_id` 不一致时必须返回 `AUTH_FORBIDDEN`。
- 所有认证失败响应必须携带 `trace_id` 和 `retryable`。
- 默认运行入口不写入演示身份事实；测试数据只由测试 fixture 创建。

## Phase 004 工单生命周期落地状态

Phase 004 已在 `services/ticket-service` 中落地最小工单生命周期。该落地只覆盖工单创建、列表查询、详情读取、租户隔离、权限校验和工单审计，不覆盖工单状态变更 API、自动派单、完整 SLA 引擎、审批实例、动作命令或事件发布。

已实现生命周期：

- 工单创建：请求接收、token 校验、租户校验、权限校验、请求体验证、客户解析或创建、工单持久化为 `open`、工单审计、响应创建。
- 工单查询：请求接收、token 校验、租户校验、权限校验、过滤参数校验、当前租户范围查询、响应创建。
- 工单详情：请求接收、token 校验、租户校验、权限校验、当前租户范围详情读取、响应创建。
- 工单检索契约：`ticket.search.v1` 冻结为只读语义，供后续 Agent 阶段按契约消费。

当前约束：

- 新建工单默认状态必须是 `open`。
- 本阶段不允许通过 API 迁移工单状态。
- 详情查询必须按租户查找；跨租户资源和不存在资源都返回 `RESOURCE_NOT_FOUND`。
- 创建工单时必须由服务端写入 `tenant_id`、`created_by` 和审计字段。
- 所有失败响应必须携带 `trace_id` 和 `retryable`。
- 默认运行入口不写入演示客户或演示工单；测试数据只由测试 fixture 创建。

## Phase 005 Agent 生命周期落地状态

Phase 005 已在 `ai-services/agent-service` 中落地最小 Agent 生命周期。该落地只覆盖本地任务输入、本地图执行、契约化 Mock 工具、Mock 知识来源、Mock 报告保存、Agent run / step 记录、事件 envelope、失败路径和最小 eval，不覆盖真实 LangGraph / FastAPI、真实消息队列、真实 SSE、真实 RAG、真实外部模型、真实报告服务、审批实例、动作命令或高风险动作执行。

已实现生命周期：

- 本地任务输入：创建包含 `task_id`、`tenant_id`、`created_by`、`task_type`、`prompt`、`priority` 和 `trace_id` 的投诉分析样例任务。
- Agent run：从本地 runner 创建 run，记录 `created`、`started`、`step_running`、`completed` 或 `failed`，并写入 v1 Agent run 事件 envelope。
- Agent step：Planner、Retriever、Data Analyst、Risk、Supervisor、Report 逐步从 `pending` 进入 `running`，再进入 `success`、`failed` 或 `waiting_human`。
- 工具调用：`ticket.search.v1`、`knowledge.search.v1` 和 `report.save.v1` 以契约化 Mock 或本地 Mock 方式记录调用、成功、失败和重试语义。
- 报告生成：生成包含事实、推断、风险、建议和引用的结构化报告草稿，并以本地 Mock 保存。
- 人工确认占位：高风险或低置信度输出由 Supervisor step 进入 `waiting_human`，任务映射为 `waiting_approval` 占位。
- eval：最小 eval 样例覆盖完成路径、工具调用成功、失败路径和人工确认占位。

当前约束：

- `ticket.search.v1` 保持只读，工具结果只可用于分析和报告，不得触发写动作。
- Mock 知识、Mock 报告保存和本地模型逻辑必须在输出、工具记录、报告引用或 eval 中可识别。
- `agent.run.started.v1`、`agent.run.completed.v1`、`agent.run.failed.v1` 和 `agent.step.completed.v1` 本地记录必须携带 v1 envelope。
- 高风险建议不得转换成动作命令；本阶段不创建真实 `approval_instance_id`。
- 节点输入和输出只记录摘要或引用，避免复制身份、工单、知识库或审批事实表。

## Phase 006 前端工作台生命周期落地状态

Phase 006 已在 `apps/web-console` 中落地最小工作台生命周期。该落地只覆盖浏览器侧登录态、工单列表 / 详情加载态、AI 任务详情展示态、Agent step 时间线和报告预览状态，不覆盖真实网关登录联调、真实实时推送、真实审批动作、真实报告服务或真实 RAG 文档流转。

已实现生命周期：

- 登录：`anonymous -> authenticating -> authenticated`，以及 `auth_failed`、`token_expired`、`forbidden` 展示路径。
- 工单列表与详情：`idle -> loading -> loaded / empty / error`，并支持刷新后的再次进入 `loading` 或 `error`。
- AI 任务展示：`pending -> running -> success / waiting_approval / failed / cancelled` 的前端状态展示。
- Agent step 展示：`pending -> running -> success / failed / skipped / waiting_human` 的时间线展示，并可读到步骤摘要、错误和引用。
- 报告预览：`loading -> available / waiting_approval / not_ready / not_found / error` 的独立面板状态。

当前约束：

- 前端只能展示等待人工确认或等待审批，不得在浏览器侧创建审批实例、动作命令、自动派单或通知动作。
- 报告区的“风险”属于前端展示层派生结果，不改变共享契约中的 `ReportSummary` 事实边界。
- `RESOURCE_NOT_FOUND` 在报告区必须与等待审批和报告未生成分支区分开，避免把生命周期错误折叠成通用失败。
- 前端会话只保存最小 token / tenant / user 摘要，不复制身份、工单、Agent 或审批事实表。
- Mock 数据推进的生命周期必须显式标记 Mock 边界，不得伪装成真实实时事件流。

## Phase 007 RAG 生命周期落地状态

Phase 007 已在 `ai-services/rag-service` 中落地最小 RAG 文档生命周期。该落地只覆盖本地样例文档登记、解析、切片、本地确定性轻量索引、租户隔离检索、引用溯源、解析失败重试和最小 RAG eval，不覆盖生产级对象存储、pgvector / Milvus、异步解析任务、真实上传入口或生产持久化数据库。

已实现生命周期：

- 文档登记：记录 `document_id`、`knowledge_base_id`、`tenant_id`、`filename`、`object_key`、`parse_status: uploaded`、`created_by` 和 `trace_id`。
- 文档解析：从 `uploaded` 进入 `parsing`，解析成功后切片并进入 `indexed`，记录 `chunk_count` 与存储无关 `index_ref`。
- 文档失败：空内容或解析错误进入 `failed`，保留 `DOCUMENT_PARSE_FAILED`、错误信息、`trace_id` 和 `retryable`。
- 文档重试：通过带 `tenant_id`、`requested_by`、`trace_id` 的本地入口重试 failed 文档；跨租户解析或重试统一返回 `RESOURCE_NOT_FOUND`。
- 知识检索：请求带 `tenant_id`、`requested_by`、`trace_id`、`query` 和 `top_k`，只访问当前租户已 `indexed` 的文档片段。
- 引用溯源：每条检索结果包含 `document_id`、`chunk_id`、`snippet`、`score`、`citation` 和本地 `source_uri`。
- Agent 消费：`agent-service` 通过 `knowledge.search.v1` 形状消费 RAG 结果，报告引用保留非 Mock 本地文档片段来源。
- RAG eval：最小样例计算 `hit_rate` 与 `citation_accuracy`，Phase 007 验证结果均为 `1.0`。

当前约束：

- 本地确定性轻量索引只是 MVP 替代检索方案，不代表生产级向量检索。
- `index_ref` 必须保持存储无关，后续可在不破坏契约的前提下替换为 pgvector、Milvus 或其他后端。
- RAG 失败不得静默降级为 Mock 来源。
- RAG 不拥有身份、工单、审批、动作命令或报告事实表。
- 高风险建议仍只能进入 `waiting_human` / `waiting_approval`，不得由 RAG 或 Agent 直接执行。

## AI 任务生命周期

```text
pending
  -> running
  -> waiting_approval
  -> running
  -> success

pending
  -> running
  -> failed

pending
  -> cancelled

waiting_approval
  -> cancelled
waiting_approval
  -> failed
```

### 生命周期要求

- `pending` 必须有创建人、租户、原始指令和任务类型。
- `running` 必须有 `agent_run` 或等价执行实例。
- `waiting_approval` 必须有关联审批实例和阻塞原因。
- `success` 必须有关联报告或明确无报告原因。
- `failed` 必须有错误码、错误信息和是否可重试标记。
- `cancelled` 必须有取消人或系统取消原因。

## Agent Run 生命周期

```text
created
  -> started
  -> step_running
  -> completed

created
  -> started
  -> failed
```

### Agent Step 生命周期

```text
pending -> running -> success
pending -> running -> failed
pending -> skipped
running -> waiting_human -> success
running -> waiting_human -> failed
```

每个关键 Agent 步骤至少记录：

- `task_id`
- `run_id`
- `agent_name`
- `step_name`
- `input_payload` 摘要或引用
- `output_payload` 摘要或引用
- `status`
- `started_at`
- `finished_at`
- `model_call_log` 或工具调用引用

## 文档处理生命周期

```text
uploaded
  -> parsing
  -> indexed

uploaded
  -> parsing
  -> failed
```

### 文档要求

- 目标生产态中，原始文件进入对象存储后才能创建解析任务；Phase 007 MVP 仅用本地 fixture 和 `object_key` 表达对象引用，不代表生产对象存储已落地。
- 解析失败必须保留失败原因并允许重试。
- `indexed` 必须记录切片数量和向量索引引用。
- RAG 检索只能返回可追踪到 `document` 和 `document_chunk` 的引用。

## 审批生命周期

```text
pending -> approved
pending -> rejected
pending -> cancelled
pending -> expired
```

### 审批要求

- 高风险动作必须先创建 `approval_instance`。
- 审批通过后才能生成可执行 `action_command`。
- 审批记录必须包含审批人、理由、时间和动作摘要。
- 审批拒绝不得继续执行对应动作。

## 动作命令生命周期

```text
pending -> success
pending -> failed
pending -> cancelled
```

### 动作要求

- 所有动作命令必须有 `idempotency_key`。
- 重试必须复用幂等键。
- 动作结果必须写入 `result_payload` 或错误字段。
- Agent 不直接修改业务事实，只能请求宿主服务执行动作命令。

## 生命周期验收

任意阶段新增状态时必须回答：

1. 谁创建该状态？
2. 谁允许迁移到下一个状态？
3. 失败时状态如何落盘？
4. 用户在哪里看到状态？
5. 审计和追踪如何关联？
6. 该状态是否会泄露租户数据？
