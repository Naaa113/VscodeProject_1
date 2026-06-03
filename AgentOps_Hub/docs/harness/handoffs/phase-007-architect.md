# Phase 007 架构交接

## 启动确认

- 本窗口已列出 `docs/harness/handoffs`，最新 steering decision 为 `docs/harness/handoffs/steering-decision-phase-007.md`。
- `steering-decision-phase-007.md` 明确记录：Phase 007 已由用户在 2026-06-03 批准，Window 0 已停止，允许 Window 1 拆解架构。
- `docs/harness/handoffs/phase-007-architect.md` 在本次启动前不存在，因此本次不是重复启动。
- `docs/harness/state/current-state.md` 仍记录 Phase 006 `web-console` 最小工作台为 `completed`，Steering 状态为 `handoff_done`。这与 Phase 007 steering decision 的“在 Phase 006 完成后选择下一阶段”一致，不构成冲突。
- 本文件只冻结 Phase 007 的 contract、边界、验收条件和禁止事项；不代表实现已完成。

## 本阶段目标

Phase 007 的目标是落地 RAG 最小知识库闭环，让 Agent 报告引用从 Phase 005 / Phase 006 的显式 Mock 来源，推进到可追踪的本地真实文档片段来源。

本阶段必须形成以下最小闭环：

1. 登记样例 SOP / FAQ 文档。
2. 将文档解析为文本并切片。
3. 为切片建立本地、确定性、存储无关的最小检索索引。
4. 通过 `knowledge.search.v1` 返回带 `document_id`、`chunk_id`、`snippet`、`score` 和 `citation` 的检索结果。
5. 让 `agent-service` 只能通过 `knowledge.search.v1` 消费知识结果，并在报告引用中保留可回溯来源。
6. 新增最小 RAG eval 样例，覆盖命中率和引用准确率的计算方式。
7. 收敛 DEBT-004 和 DEBT-014：本阶段选择 MVP 替代检索方案，并修正契约校验脚本对已批准 Phase 006 前端运行时的假阳性。

## Belongs：归属边界

### 受影响 host

| Host | Phase 007 归属 | 本阶段允许承担 | 本阶段不得承担 |
|---|---|---|---|
| `ai-services/rag-service` | 主实现 host | 文档登记、解析、切片、本地索引、检索、引用溯源、RAG eval | 业务事实源、审批、动作命令、工单状态变更、生产级向量平台 |
| `ai-services/agent-service` | 契约消费者 | 通过 `knowledge.search.v1` 消费 RAG 检索结果，生成带真实文档引用的本地报告 | 直接读取 RAG 内部存储、索引文件或数据库；绕过工具契约 |
| `packages/shared-contracts` | 契约权威 | 对知识检索、文档状态、示例和校验脚本做非破坏性补齐 | 私自改变认证、工单、AI 任务既有语义 |
| `apps/web-console` | 可选承载面 | 最小展示文档状态、检索结果或非 Mock 引用；保持 Mock / 本地 RAG 边界可识别 | 真实网关联调、真实 SSE、审批通过、动作执行、通知发送 |
| `scripts` | 验证入口 | 新增 RAG 验证脚本，修正契约校验阶段感知规则 | 生成代码、安装依赖、启动服务或迁移数据库 |

### 数据所有权

| 数据 / 概念 | Owner | Phase 007 要求 |
|---|---|---|
| `knowledge_base` | `rag-service` | 可用本地 fixture 或内存 / JSON 记录表达；必须带 `tenant_id` |
| `document` | `rag-service` | 拥有 `document_id`、`knowledge_base_id`、`tenant_id`、`filename`、`object_key`、`parse_status`、错误信息和审计上下文 |
| `document_chunk` | `rag-service` | 拥有 `chunk_id`、`document_id`、切片顺序、文本片段摘要和来源范围 |
| `embedding_index_ref` | `rag-service` | 只记录存储无关引用，不绑定 pgvector 或 Milvus |
| `citation` | `rag-service` 生成，`agent-service` 消费 | 必须能回溯到 `document_id` 和 `chunk_id`，可带 `source_uri` |
| `report` | Phase 007 仍由 `agent-service` 本地 Mock 保存 | 报告只保存引用，不复制 RAG 内部索引或完整文档事实 |
| 工单、客户、身份、审批、动作 | 既有 Java host | Phase 007 不新增或迁移这些事实 |

## Authority：允许范围与禁止范围

### 允许修改的文件范围

Window 2 只能在以下范围内实现：

- `ai-services/rag-service/**`
- `ai-services/agent-service/**`，仅限 `knowledge.search.v1` 消费、报告引用、测试和 eval 相关改动。
- `packages/shared-contracts/openapi/agentops-api.v1.yaml`，仅限知识库文档、知识检索、引用字段和错误示例的非破坏性补齐。
- `packages/shared-contracts/tools/knowledge.search.v1.schema.json`
- `packages/shared-contracts/events/document.uploaded.v1.schema.json`
- `packages/shared-contracts/events/document.indexed.v1.schema.json`
- `packages/shared-contracts/examples/**`，仅限 Phase 007 相关成功 / 失败示例。
- `packages/shared-contracts/manifest.v1.json`，仅限登记新增示例或校验资产。
- `packages/shared-contracts/errors/error-codes.v1.json`，仅在现有错误码无法表达 RAG 失败时做非破坏性新增；优先复用现有错误码。
- `apps/web-console/src/**`，仅在实现最小知识库入口、文档状态展示、检索结果展示或非 Mock 引用展示时允许修改。
- `scripts/validate-contracts.ps1`
- `scripts/validate-rag-service.ps1`
- `scripts/validate-agent-service.ps1`，仅在 Agent 验证入口需要纳入 RAG 场景时允许修改。
- `scripts/validate-web-console.ps1`，仅在前端确实新增 Phase 007 UI 时允许修改。
- `docs/harness/handoffs/phase-007-implementation.md`，作为 Window 2 实现交接。

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
- `services/workflow-service/**`
- `services/notification-service/**`
- `apps/api-gateway/**`
- `deploy/**`
- 根目录构建、部署、容器或数据库迁移文件，除非本 architect handoff 后续被 Window 0 明确改判。

Window 2 如发现必须修改上述文件才能完成 Phase 007，必须停止并写入 blocker，不得自行扩大范围。

### 允许新增的目录、文件、class、method 或 schema 类型

允许 Window 2 新增以下类型的实现资产：

- `ai-services/rag-service/pyproject.toml`，仅用于最小 Python 本地运行时和测试。
- `ai-services/rag-service/src/rag_service/**`
- `ai-services/rag-service/tests/**`
- `ai-services/rag-service/evals/**`
- `ai-services/rag-service/fixtures/**`
- `ai-services/rag-service/README.md` 的 Phase 007 更新。
- RAG 内部类型：`KnowledgeBaseRecord`、`DocumentRecord`、`DocumentChunkRecord`、`IndexRecord`、`CitationRecord`、`SearchResult`。
- RAG 内部 class：`DocumentRegistry`、`DocumentParser`、`DocumentChunker`、`LocalEmbeddingIndex`、`KnowledgeSearchEngine`、`CitationBuilder`、`RagEvalRunner`。
- RAG 内部 method：`register_document`、`parse_document`、`chunk_document`、`index_document`、`search`、`evaluate`、`retry_parse`。
- Agent 契约消费者 class：`RagKnowledgeSearchTool` 或等价名称，但它只能发送 / 接收 `knowledge.search.v1` 形状的数据。
- 最小契约示例：知识检索成功、知识检索失败、文档登记成功、文档解析失败、文档索引完成。

### 不允许新增的 helper / adapter / fallback / bridge

以下类型不得新增：

- 不得新增把 RAG 失败静默替换为 Mock 结果的 fallback。
- 不得新增隐藏 Mock 来源的 adapter。
- 不得新增让 `agent-service` 直接读取 `rag-service` 内部索引、fixture、缓存或存储文件的 bridge。
- 不得新增绕过 `knowledge.search.v1` 的通用工具调用 helper。
- 不得新增从 RAG 直接调用 `ticket-service`、`identity-service`、`workflow-service` 或 `notification-service` 的 helper。
- 不得新增 `api-gateway` 代理、真实 SSE bridge、RabbitMQ bridge、对象存储 bridge、pgvector bridge 或 Milvus bridge。
- 不得新增审批通过、动作命令、自动派单、通知发送或工单状态变更相关 helper。

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
- `knowledge.search.v1`
- `document.parse_status`: `uploaded`、`parsing`、`indexed`、`failed`
- `ai_task.status`、`agent_step.status`、`ErrorResponse` 统一错误形状。

必须保持的行为：

- `ticket.search.v1` 仍为只读。
- `knowledge.search.v1` 仍为只读，`requires_approval` 仍为 `false`。
- 高风险或低置信度 Agent 输出仍只能进入 `waiting_human` / `waiting_approval` 占位。
- Agent 不得直接写业务事实表，不得执行高风险动作。
- 租户隔离必须贯穿文档登记、解析、检索、引用和 Agent 报告。
- Mock 结果、本地 RAG 结果和未来生产 RAG 结果必须可区分，不得混淆。

### OpenAPI 契约要求

Window 2 必须确认或补齐以下契约细节：

- `DocumentSummary` 必须表达 `document_id`、`knowledge_base_id`、`tenant_id`、`filename`、`parse_status`、`chunk_count`、`index_ref` 和错误信息。
- `RegisterDocumentRequest` 继续使用 `knowledge_base_id`、`filename`、`object_key`；本阶段不引入真实二进制上传 API。
- `KnowledgeSearchRequest` 继续使用 `query`、`top_k` 和 `filters`；如需收窄过滤项，只能非破坏性新增 `knowledge_base_id`、`document_id`、`parse_status` 等可选字段。
- `KnowledgeSearchResponse.items[]` 必须包含 `document_id`、`chunk_id`、`snippet`、`score` 和 `citation`。
- `Citation` 必须至少包含 `document_id`、`chunk_id`、`source_title`；建议保留或补齐 `source_uri`，使前端和报告能定位片段。
- 需要新增知识库 OpenAPI 示例时，必须同步登记到 `manifest.v1.json`。

### 事件契约要求

- `document.uploaded.v1` 继续表示文档已登记，`parse_status` 固定为 `uploaded`。
- `document.indexed.v1` 继续表示文档已完成索引，必须包含 `document_id`、`chunk_count`、`index_ref` 和 `parse_status: indexed`。
- 本阶段默认不新增 `document.parsing.v1` 或 `document.failed.v1`。`parsing` 与 `failed` 先通过文档状态和 API / 本地记录表达。
- 如果 Window 2 发现必须新增失败事件才能形成验收闭环，必须停止并交回 Window 0 / 用户确认，不得自行新增事件面。

### 工具契约要求

- `knowledge.search.v1` 必须继续保持存储无关，不得出现 pgvector、Milvus、Elasticsearch、OpenSearch 等具体存储绑定字段。
- `knowledge.search.v1` 的真实 RAG 输出不得设置 `mock_source: true`；如字段存在，只能省略或显式为 `false`。
- `knowledge.search.v1` 的失败响应必须使用统一 `error` 形状，优先使用 `DOCUMENT_PARSE_FAILED`、`VALIDATION_FAILED`、`RESOURCE_NOT_FOUND`、`DOWNSTREAM_UNAVAILABLE` 或 `INTERNAL_ERROR`。
- Agent 工具调用记录必须保留 `tenant_id`、`requested_by`、`trace_id`、`run_id`、请求、响应、状态和来源类型。

### 向量检索决策

Phase 007 不引入 pgvector 或 Milvus。DEBT-004 在本阶段以 MVP 替代检索方案收敛：

- 使用本地确定性轻量索引作为最小闭环，可采用分词词频、哈希向量或等价的无外部依赖 embedding 表达。
- 检索评分必须可重复，测试 fixture 不依赖真实模型或外部服务。
- `index_ref` 必须保持存储无关，例如 `local-index://tenant/<tenant_id>/knowledge-base/<knowledge_base_id>/document/<document_id>`。
- 本决策不代表生产向量库最终选择；后续生产化阶段可以在保持 `index_ref` 与 `knowledge.search.v1` 不破坏的前提下切换到 pgvector 或 Milvus。
- 不得把本地轻量索引宣传为生产级向量检索。

## Transition：状态生命周期和失败处理

### 文档生命周期

| 状态 | 创建者 / 迁移者 | 必须记录 | 用户可见性 | 失败处理 |
|---|---|---|---|---|
| `uploaded` | 文档登记入口 | `document_id`、`knowledge_base_id`、`tenant_id`、`object_key`、`filename`、`trace_id` | 文档列表可见 | 登记失败返回 `VALIDATION_FAILED` 或 `CONFLICT` |
| `parsing` | `rag-service` | 开始时间、操作者、源文档引用 | 文档详情或列表可见 | 解析失败迁移到 `failed` |
| `indexed` | `rag-service` | `chunk_count`、`index_ref`、完成时间 | 文档列表、检索结果、Agent 引用可见 | 不得返回不可追踪引用 |
| `failed` | `rag-service` | `error_code`、`message`、`trace_id`、`retryable` | 文档状态可见 | `retryable: true` 时必须允许通过本地入口重试 |

### 检索生命周期

- 请求必须带 `tenant_id`、`requested_by`、`trace_id`、`query` 和 `top_k`。
- 检索只能访问当前租户已 `indexed` 的文档片段。
- 无命中时返回 `success` 和空 `items`，不得伪造引用。
- 依赖不可用或索引损坏时返回 `failed` 与统一错误形状。
- 检索结果必须可追溯到 `document` 与 `document_chunk`。

### Agent 生命周期影响

- Retriever 步骤可以从 “Mock knowledge” 改为 “RAG knowledge”，但仍必须记录工具调用。
- 如果 RAG 检索失败，Agent run 必须进入可解释失败或保留人工确认占位，不得静默降级到 Mock 来源。
- 报告仍必须区分事实、推断、风险、建议和引用。
- 高风险建议仍不得生成审批实例、动作命令或跟进任务。

### 契约校验生命周期

DEBT-014 必须在 Phase 007 中处理。`scripts/validate-contracts.ps1` 不得继续把 Phase 006 已批准的 `apps/web-console` 运行时判定为 forbidden artifact。

允许的处理方式：

- 将脚本从 Phase 002 固定规则调整为阶段感知规则。
- 明确允许已完成阶段的运行时目录，同时继续禁止未批准 host 的实现产物。
- 保留对契约文件、事件 schema、工具 schema、错误码和示例 payload 的校验。

## Behavior：行为边界、验收与停止条件

### 验收条件

Window 2 完成后必须满足：

- `ai-services/rag-service` 有可运行的最小本地 RAG 闭环。
- 样例 SOP / FAQ 文档可以被登记、解析、切片、索引和检索。
- 文档解析失败可以落入 `failed`，错误原因可见，并能通过本地入口重试。
- 检索结果中的每个引用都能追溯到 `document_id` 和 `chunk_id`。
- Agent 报告引用至少包含一个非 Mock 文档片段来源。
- Mock 来源和本地 RAG 来源在工具记录、报告引用、eval 或前端展示中可区分。
- RAG eval 至少定义并计算命中率和引用准确率。
- `knowledge.search.v1` 保持只读、存储无关、租户隔离和统一错误响应。
- `scripts/validate-contracts.ps1` 不再因 Phase 006 前端运行时产生假阳性失败。
- 未新增审批实例、动作命令、自动派单、通知发送、工单状态变更、真实网关、真实 SSE、RabbitMQ 消费者、pgvector、Milvus 或生产对象存储接入。

### 必须运行的验证命令

Window 1 不运行以下命令。Window 2 实现完成后必须运行并记录结果：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-contracts.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-rag-service.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-agent-service.ps1
```

如果 Window 2 修改了 `apps/web-console/src/**`，还必须运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-web-console.ps1
```

如果 Window 2 修改了认证、工单或共享 OpenAPI 中认证 / 工单字段，还必须运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-identity-service.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-ticket-service.ps1
```

### 实现窗口发现 blocker 时的停止规则

Window 2 遇到以下任一情况必须停止，不得绕过：

- 当前实现需要修改禁止范围内的文件。
- 需要新增 pgvector、Milvus、RabbitMQ、FastAPI、Spring Boot、网关、SSE、对象存储或数据库迁移才能完成闭环。
- `agent-service` 只能通过读取 RAG 内部存储才能拿到检索结果。
- 无法保证文档、检索结果或报告引用按 `tenant_id` 隔离。
- 需要新增 `document.failed.v1` 等新事件才能表达失败生命周期。
- RAG 失败只能靠 Mock 结果掩盖。
- 需要执行审批、动作命令、自动派单、通知发送或工单状态变更。
- 验证命令失败且无法在当前冻结范围内修复。

停止时必须写入 `docs/harness/handoffs/phase-007-implementation.md`，说明 blocker、已完成内容、未完成内容、失败命令和建议交回 Window 0 或用户确认的决策点。

## Window 2 任务拆分建议

1. 先修正 `scripts/validate-contracts.ps1` 的阶段感知规则，确保契约校验入口与 Phase 006 之后的仓库事实一致。
2. 补齐知识检索和文档相关契约示例，必要时非破坏性补齐 `Citation.source_uri`。
3. 在 `ai-services/rag-service` 中实现本地文档登记、解析、切片、索引、检索和 eval。
4. 让 `agent-service` 通过 `knowledge.search.v1` 消费本地 RAG 检索结果，并保留显式来源记录。
5. 如修改前端，仅做最小文档状态 / 检索结果 / 非 Mock 引用展示。
6. 运行并记录所有必需验证命令。
