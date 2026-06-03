# Phase 005 架构交接

## 启动确认

- 最新 steering decision：`docs/harness/handoffs/steering-decision-phase-005.md`。
- 用户批准状态：该文件记录“已由用户在 2026-06-01 批准”。
- 当前状态来源：`docs/harness/state/current-state.md` 记录 Phase 004 已完成，Steering 状态为 `handoff_done`。
- 重复启动检查：`docs/harness/handoffs/phase-005-architect.md` 在本窗口写入前不存在。
- 冲突判断：`current-state.md` 尚未把“最新 Steering 决策”更新到 Phase 005，但其“下一步建议”和 Phase 005 steering decision 一致，均指向 `agent-service` 最小 LangGraph 闭环；本窗口不视为冲突。

## 本阶段目标

Phase 005 的目标是落地 `agent-service` 最小 LangGraph 闭环，让平台具备可验证、可追踪、可评测的投诉分析类 Agent 编排能力。

本阶段只覆盖：

- `ai-services/agent-service` 最小 Python Agent 运行时。
- Planner、Retriever、Data Analyst、Risk、Supervisor、Report 的简化节点。
- 基于 `ticket.search.v1` 的只读工单检索调用或显式 Mock。
- 显式 Mock 的知识检索结果，不实现真实 RAG。
- Agent run、Agent step、工具调用和报告生成的本地状态记录。
- 失败、重试、低置信度和高风险人工确认占位。
- 最小 AI eval 样例和本地验证脚本。

## 允许修改的文件范围

Window 2 只允许修改以下范围：

- `ai-services/agent-service/**`
- `packages/shared-contracts/openapi/agentops-api.v1.yaml`
- `packages/shared-contracts/schemas/status.v1.schema.json`
- `packages/shared-contracts/events/ai.task.created.v1.schema.json`
- `packages/shared-contracts/events/agent.run.started.v1.schema.json`
- `packages/shared-contracts/events/agent.step.completed.v1.schema.json`
- `packages/shared-contracts/events/agent.run.completed.v1.schema.json`
- `packages/shared-contracts/events/agent.run.failed.v1.schema.json`
- `packages/shared-contracts/tools/ticket.search.v1.schema.json`
- `packages/shared-contracts/tools/knowledge.search.v1.schema.json`
- `packages/shared-contracts/tools/report.save.v1.schema.json`
- `packages/shared-contracts/examples/events/**`
- `packages/shared-contracts/examples/tools/ticket.search.*.json`
- `packages/shared-contracts/examples/tools/knowledge.search.*.json`
- `packages/shared-contracts/examples/tools/report.save.*.json`
- `packages/shared-contracts/examples/openapi/ai-task*.json`
- `packages/shared-contracts/manifest.v1.json`
- `scripts/validate-contracts.ps1`
- `scripts/validate-agent-service.ps1`
- `docs/development/**`
- `docs/harness/handoffs/phase-005-implementation.md`

仅当现有错误码无法表达 Agent、工具或 eval 失败时，才允许修改：

- `packages/shared-contracts/errors/error-codes.v1.json`

契约修改必须保持 v1 兼容：只允许补齐说明、示例、非破坏性字段或更严格的 Mock 标识，不允许删除必填字段、改变枚举语义或改变错误码含义。

## 禁止修改的文件范围

Window 2 不得修改以下范围，除非先停止并请求 Window 1 或 Window 0 重新决策：

- `services/identity-service/**`
- `services/ticket-service/**`
- `services/workflow-service/**`
- `services/notification-service/**`
- `apps/**`
- `deploy/**`
- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/10-steering-state-machine.md`
- `packages/shared-contracts/tools/ticket.create_followup.v1.schema.json`
- `packages/shared-contracts/events/document.uploaded.v1.schema.json`
- `packages/shared-contracts/events/document.indexed.v1.schema.json`

不得清理、回滚或重写本阶段无关的既有 dirty 变更。

## 受影响 host 和数据所有权

| Host | 本阶段状态 | 数据所有权 |
|---|---|---|
| `ai-services/agent-service` | 新增最小 Python Agent 运行时 | 拥有 Agent 编排状态、图执行过程、节点输入输出摘要、模型或 Mock 模型调用记录、工具调用记录、报告草稿和 eval 样例 |
| `services/ticket-service` | 不修改代码，只作为 `ticket.search.v1` 的目标宿主或 Mock 契约来源 | 继续拥有客户、工单、工单状态、优先级、分类、SLA 和工单审计事实 |
| `services/identity-service` | 不修改代码，只作为身份上下文和权限语义来源 | 继续拥有租户、用户、角色、权限、JWT 和身份审计事实 |
| `ai-services/rag-service` | 不实现 | 未来拥有知识库、文档、切片、Embedding、索引引用和真实检索事实 |
| `services/workflow-service` | 不实现 | 未来拥有审批实例、动作命令、审批记录和动作幂等事实 |
| `packages/shared-contracts` | 允许非破坏性补齐 Agent、工具、事件和示例契约 | 拥有跨 host 字段、状态枚举、错误形状、事件、工具协议和示例 |

`agent-service` 可以记录来自身份上下文的 `tenant_id`、`requested_by`、`roles`、`permissions` 摘要和 `trace_id`，但不得复制身份事实表、工单事实表、审批事实表或知识库事实表。

## 必须保持稳定的契约和行为

### URL 与 API

以下 OpenAPI 仍是 Java 任务入口或未来网关对外契约，Phase 005 不得把它们误实现为生产级入口：

- `GET /api/ai/tasks`
- `POST /api/ai/tasks`
- `GET /api/ai/tasks/{id}`
- `GET /api/ai/tasks/{id}/steps`
- `GET /api/ai/tasks/{id}/report`
- `GET /api/stream/tasks/{id}`

如果 Window 2 为本地闭环提供 CLI、测试入口或开发用 HTTP 入口，必须在 README 和 handoff 中标明“本地最小运行时”，不得宣称网关、Java 任务入口或生产 SSE 已完成。

### 事件

以下事件契约必须保持 v1 兼容：

- `ai.task.created.v1`
- `agent.run.started.v1`
- `agent.step.completed.v1`
- `agent.run.completed.v1`
- `agent.run.failed.v1`

本阶段可以在本地状态或测试 fixture 中模拟事件收发，但不得要求真实 RabbitMQ、网关推送或 Java 任务入口已经存在。

### 状态

必须保持以下状态枚举稳定：

- `ai_task.status`：`pending`、`running`、`waiting_approval`、`success`、`failed`、`cancelled`
- `agent_run.status`：`created`、`started`、`step_running`、`completed`、`failed`
- `agent_step.status`：`pending`、`running`、`success`、`failed`、`skipped`、`waiting_human`

高风险或低置信度输出必须进入 `waiting_human` 或映射为 `waiting_approval` 占位，不得生成可执行动作命令。

### 工具

- `ticket.search.v1` 必须保持只读语义，`requires_approval` 仍为 `false`。
- `knowledge.search.v1` 本阶段只能使用 Mock 或契约化假数据，必须显式标记 `mock_source` 或等价字段。
- `report.save.v1` 本阶段可以使用本地内存、文件或 Mock 存储模拟报告保存，但不得宣称 Java 报告服务已完成。
- `ticket.create_followup.v1` 不得实现、不得调用、不得通过 helper 间接执行。

### 行为

- Agent 不得直接访问 `ticket-service`、`identity-service` 或未来 `rag-service` 的数据库。
- Agent 不得绕过工具契约构造私有跨服务字段。
- Agent 报告必须区分事实、推断、风险和建议。
- Mock 工具、Mock 知识来源、Mock 模型输出必须在输出、日志或 eval 记录中可识别。

## 允许新增的目录、文件、class、method 或 schema 类型

### `ai-services/agent-service`

允许新增最小 Python 项目结构，具体命名可由 Window 2 按仓库风格选择：

- `ai-services/agent-service/pyproject.toml`
- `ai-services/agent-service/README.md`
- `ai-services/agent-service/src/agent_service/**`
- `ai-services/agent-service/tests/**`
- `ai-services/agent-service/evals/**`
- `ai-services/agent-service/fixtures/**`

允许新增的核心类型：

- `AgentTask`
- `AgentRun`
- `AgentStep`
- `AgentState`
- `ToolCallRecord`
- `ReportDraft`
- `RiskAssessment`
- `EvalCase`
- `EvalResult`
- `AgentGraphRunner`
- `PlannerNode`
- `RetrieverNode`
- `DataAnalystNode`
- `RiskNode`
- `SupervisorNode`
- `ReportNode`
- `TicketSearchToolClient`
- `MockKnowledgeSearchTool`
- `MockModelClient`
- `LocalRunStore` 或等价本地状态存储

允许新增的方法类型：

- 创建本地 task / run / step。
- 执行图节点。
- 记录 step 输入摘要、输出摘要、状态、耗时和错误。
- 调用或模拟 `ticket.search.v1`。
- 调用或模拟 `knowledge.search.v1`。
- 生成结构化报告。
- 对高风险和低置信度输出设置人工确认占位。
- 运行最小 eval 样例并输出通过 / 失败结果。

允许新增的本地状态 schema：

- `agent_task`
- `agent_run`
- `agent_step`
- `tool_call_log`
- `model_call_log`
- `report_draft`
- `eval_case`
- `eval_result`

这些 schema 可以是 Pydantic 模型、dataclass、JSON fixture 或本地测试用表；不得作为生产数据库迁移。

### `packages/shared-contracts`

允许非破坏性补齐以下内容：

- AI task、Agent step、报告结构的示例 payload。
- Agent run / step 事件的失败和人工等待示例。
- `ticket.search.v1` 被 Agent 消费时的 trace、tenant、requested_by 示例。
- `knowledge.search.v1` Mock 来源标记说明或示例。
- `report.save.v1` 本地 Mock 保存示例。
- manifest 中新增示例引用。

## 不允许新增的 helper、adapter、fallback、bridge

本阶段不得新增：

- 写入工单、关闭工单、升级工单、自动派单或创建跟进任务的 helper。
- `ticket.create_followup.v1` adapter、client、bridge 或 fallback。
- 审批实例、动作命令、幂等动作执行器或 workflow adapter。
- 真实 RAG 索引、Embedding、向量库、文档解析或对象存储 adapter。
- 直接访问 `ticket-service` H2 数据库的 repository。
- 直接访问 `identity-service` H2 数据库的 repository。
- 生产级网关、SSE、RabbitMQ 消费者或消息发布器。
- 默认调用真实外部 LLM 的客户端。
- 需要真实 API key、真实客户数据或外部网络才能通过测试的 fallback。
- 把 Mock 响应伪装成真实工具、真实知识库或真实模型输出的包装层。

## 需要新增或冻结的契约

### OpenAPI

本阶段优先确认既有 AI task API 是否足够表达最小闭环。若补齐，必须保持非破坏性：

- `CreateAiTaskRequest` 必须能表达投诉分析类任务。
- `AiTask` 必须能表达 `pending`、`running`、`waiting_approval`、`success`、`failed` 和 `cancelled`。
- `AgentStep` 必须能表达节点名、状态、摘要、引用、错误、开始时间和完成时间。
- `ReportSummary` 必须继续区分 `facts`、`inferences`、`recommendations` 和 `citations`。
- 所有失败响应必须使用统一错误形状：`error_code`、`message`、`trace_id`、`retryable`，可带 `details`。

如果需要新增字段，必须添加示例并说明兼容性；不得改变已有字段含义。

### 事件

必须确认并冻结以下事件在 Agent 本地闭环中的语义：

- `ai.task.created.v1`：作为 Agent 输入事件或测试 fixture 输入。
- `agent.run.started.v1`：Agent run 开始时产生。
- `agent.step.completed.v1`：节点完成、失败、跳过或等待人工时产生。
- `agent.run.completed.v1`：全部节点完成且报告生成或明确无报告原因时产生。
- `agent.run.failed.v1`：运行级失败时产生，必须携带 `error_code`、`message`、`retryable`。

本阶段不新增真实消息队列要求。事件可以落入本地 JSON、内存列表、测试断言或本地 run store。

### 工具契约

必须冻结 `ticket.search.v1` 的 Agent 消费方式：

- 输入必须包含 `tool_name`、`tool_version`、`tenant_id`、`requested_by`、`trace_id` 和 `input`。
- 输出必须按 `tenant_id` 和 `trace_id` 关联到当前 run。
- 工具超时按契约默认 `3000ms` 处理。
- 工具失败必须记录统一错误形状。
- 失败可重试时最多按契约重试 `2` 次。
- 工具结果只可用于分析和报告，不得触发写动作。

必须冻结 `knowledge.search.v1` 的本阶段 Mock 语义：

- 允许返回 `document_id`、`chunk_id`、`snippet`、`score`、`citation`。
- Mock 结果必须能被测试或输出识别为 Mock。
- 不得写入真实文档、切片或索引事实。

`report.save.v1` 本阶段只允许本地 Mock 保存：

- 必须使用 `idempotency_key`。
- 可以生成本地 `report_id`。
- 不得宣称报告服务或对象存储已完成。

## 状态生命周期和失败处理要求

### AI task 生命周期

```text
pending -> running -> success
pending -> running -> waiting_approval
pending -> running -> failed
pending -> cancelled
```

要求：

- `pending` 必须有 `task_id`、`tenant_id`、`created_by`、`task_type`、`prompt`、`priority` 和 `trace_id`。
- `running` 必须关联一个 `run_id`。
- `waiting_approval` 必须有 `blocking_reason`，本阶段可以不创建真实 `approval_instance_id`。
- `success` 必须有关联 `report_id` 或明确报告保存为本地 Mock。
- `failed` 必须有错误码、错误信息、`trace_id` 和 `retryable`。

### Agent run 生命周期

```text
created -> started -> step_running -> completed
created -> started -> failed
```

要求：

- `created` 由本地 runner 或测试 fixture 创建。
- `started` 必须记录 `graph_name`、`graph_version` 和 `started_at`。
- `step_running` 必须能定位当前节点。
- `completed` 必须记录 `finished_at`、最终状态和报告引用。
- `failed` 必须记录运行级错误和是否可重试。

### Agent step 生命周期

```text
pending -> running -> success
pending -> running -> failed
pending -> skipped
running -> waiting_human
```

要求：

- 每个关键节点至少记录 `task_id`、`run_id`、`step_id`、`agent_name`、`step_name`、`status`、`started_at`、`finished_at`。
- 节点输入和输出只能记录摘要或引用，避免泄露敏感数据。
- Retriever 节点必须记录工具调用引用。
- Risk 或 Supervisor 节点发现高风险、低置信度或需要人工判断时，必须进入 `waiting_human`。
- 节点失败必须记录 `error_code`、`message` 和 `retryable`。

### 工具调用生命周期

```text
prepared -> called -> success
prepared -> called -> retrying -> success
prepared -> called -> failed
prepared -> skipped
```

要求：

- `prepared` 必须绑定 `tenant_id`、`requested_by`、`trace_id` 和 `run_id`。
- `called` 可以是真实 HTTP 调用、契约化 local client 或 Mock，但必须在记录中标明。
- `retrying` 必须遵守工具契约的最大重试次数。
- `failed` 必须记录统一错误形状。
- `skipped` 必须有跳过原因，例如缺少知识库运行时。

### 报告生成生命周期

```text
drafting -> generated -> saved_mock
drafting -> failed
drafting -> waiting_human
```

要求：

- 报告必须包含标题、事实、推断、风险、建议和引用。
- 事实必须来自工单工具结果或 Mock 知识引用。
- 推断和建议必须与事实分开。
- 高风险建议不得转换成动作命令。
- 本阶段保存结果只能是本地或 Mock，不能宣称生产报告服务完成。

## 验收条件

Window 2 完成后必须满足：

- `ai-services/agent-service` 存在最小 Python Agent 运行时和测试闭环。
- 给定投诉分析样例任务，可以跑完 Planner、Retriever、Data Analyst、Risk、Supervisor、Report 简化节点。
- 每个 Agent step 可追踪，包含状态、耗时、输入摘要、输出摘要、错误或引用。
- `ticket.search.v1` 调用或 Mock 遵守契约字段、租户上下文、trace 和失败响应。
- 知识检索结果如为 Mock，必须在输出、日志或 eval 记录中显式标记。
- 结构化报告区分事实、推断、风险和建议。
- 低置信度或高风险输出进入 `waiting_human` 或 `waiting_approval` 占位。
- 工具失败、模型失败、节点失败和运行级失败都有错误码、`trace_id` 和 `retryable`。
- 最小 AI eval 样例可运行，至少覆盖工作流完成、工具调用成功、失败路径和人工确认占位。
- 不新增审批动作执行、真实 RAG、前端、网关、工单状态变更、自动派单、通知或高风险动作执行能力。
- 默认测试不依赖真实外部模型、真实 API key、真实客户数据或外部网络。

## 必须运行的验证命令

Window 2 至少运行并记录结果：

```powershell
.\scripts\validate-contracts.ps1
```

```powershell
.\scripts\validate-agent-service.ps1
```

```powershell
Push-Location ai-services\agent-service
python -m pytest
Pop-Location
```

如果项目选择 `uv`、`poetry` 或其他 Python 工具，Window 2 可以用等价命令替代，但必须在 handoff 中写清楚原因和实际命令。

建议同时运行：

```powershell
.\scripts\validate-ticket-service.ps1
```

该命令用于确认 Phase 005 没有破坏工单查询契约；如果因本地环境无法运行，必须记录原因。

## 实现窗口发现 blocker 时如何停止

Window 2 遇到以下情况必须停止，不得自行扩大范围：

- 需要修改 `identity-service` 或 `ticket-service` 才能完成 Agent 闭环。
- `ticket.search.v1` 无法满足只读检索需求，且需要破坏性契约变更。
- 需要真实 RAG、Embedding、向量库、文档解析或对象存储才能继续。
- 需要真实审批实例、动作命令或高风险动作执行才能继续。
- 需要真实外部 LLM、真实 API key 或外部网络才能通过基础测试。
- 无法让 Mock 来源在输出、日志或 eval 中明确可见。
- 无法保证租户上下文、`trace_id` 和 `requested_by` 在工具调用与 step 记录中贯穿。
- 需要新增 `ticket.create_followup.v1` 执行能力、工单状态变更 API、自动派单、自动升级、自动关闭或通知发送能力。
- 契约修改会删除字段、改变枚举语义或改变错误码含义。

停止时应写入 `docs/harness/handoffs/phase-005-implementation.md` 的阻断摘要，说明：

- 已完成的契约或代码变更。
- 阻断点。
- 已验证命令及结果。
- 需要 Window 1、Window 0 或用户裁决的问题。

## Window 2 交接要求

实现窗口完成后必须写入：

- `docs/harness/handoffs/phase-005-implementation.md`

交接内容必须包含：

- 变更摘要。
- Agent 图节点清单。
- 契约补齐清单。
- Mock 边界说明。
- 工具调用与状态生命周期说明。
- 测试和验证命令结果。
- 最小 AI eval 结果。
- 未完成项。
- 新增债务建议。
- 是否建议进入 Window 3。

Window 1 到此停止。请用户批准后在新的 Window 2 中启动实现。
