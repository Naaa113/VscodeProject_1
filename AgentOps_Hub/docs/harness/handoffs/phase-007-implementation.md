# Phase 007 实现交接

## 模式与范围

- 模式：初次实现。
- 阶段：Phase 007，RAG 最小知识库闭环。
- 范围来源：`docs/harness/handoffs/steering-decision-phase-007.md` 与 `docs/harness/handoffs/phase-007-architect.md`。
- 本窗口未修改治理规则文件，未修改 `current-state.md`，未选择下一阶段，未提交 git commit。

## 修改文件

本窗口新增或修改了以下范围：

- `ai-services/rag-service/**`：新增最小本地 RAG 运行时、模型、命令行入口、测试、fixture、eval 和 README。
- `ai-services/agent-service/**`：让 Agent 默认通过 `knowledge.search.v1` 消费本地 RAG 检索结果，并补充 Phase 007 eval。
- `packages/shared-contracts/openapi/agentops-api.v1.yaml`：补齐文档摘要、知识检索响应和引用字段。
- `packages/shared-contracts/tools/knowledge.search.v1.schema.json`：补充 `citation.source_uri`，保留 `mock_source` 的显式 Mock 边界。
- `packages/shared-contracts/events/document.uploaded.v1.schema.json` 与 `packages/shared-contracts/events/document.indexed.v1.schema.json`：补齐文档文件名、知识库和租户相关 payload 字段。
- `packages/shared-contracts/examples/**`：新增知识库 OpenAPI 示例，并把 `knowledge.search.v1` 成功示例改为非 Mock 本地 RAG 引用。
- `packages/shared-contracts/manifest.v1.json`：登记新增知识库 OpenAPI 示例。
- `scripts/validate-contracts.ps1`：改为只禁止未批准 host 的实现产物，避免继续把已批准的 `apps/web-console`、`agent-service`、`rag-service` 判为 forbidden artifact。
- `scripts/validate-rag-service.ps1`：新增 RAG 本地验证入口。
- `scripts/validate-agent-service.ps1`：加入 `rag-service` 的 `PYTHONPATH`，并显式检查 Python 命令退出码。

## 完成的验收项

- `ai-services/rag-service` 已具备可运行的本地 RAG 闭环。
- 样例 SOP / FAQ 可登记、解析、切片、索引和检索。
- 解析失败会落入 `failed`，保留 `DOCUMENT_PARSE_FAILED` 错误，并可通过本地重试入口重新解析。
- 检索结果按 `tenant_id` 隔离，只返回 `indexed` 文档片段。
- 每条检索结果都包含可回溯到 `document_id` 与 `chunk_id` 的 citation。
- Agent 报告引用至少包含一个非 Mock 本地文档片段来源。
- Mock 来源与本地 RAG 来源在工具记录、报告引用和 eval 中可区分。
- RAG eval 已计算 `hit_rate` 与 `citation_accuracy`。
- `knowledge.search.v1` 保持只读、存储无关、租户隔离和统一错误形状。
- `scripts/validate-contracts.ps1` 不再因 Phase 006 已批准前端运行时产生假阳性失败。
- 未新增审批实例、动作命令、自动派单、通知发送、工单状态变更、真实网关、SSE、RabbitMQ、pgvector、Milvus 或生产对象存储接入。

## 保持稳定的契约

- 既有认证、工单和 AI 任务 URL 未改变。
- `GET /api/knowledge/documents`、`POST /api/knowledge/documents`、`POST /api/knowledge/search` 路径保持稳定。
- `document.uploaded.v1` 与 `document.indexed.v1` 事件名、版本和 envelope 保持稳定。
- `knowledge.search.v1` 名称、版本、只读语义、`requires_approval: false` 和存储无关要求保持稳定。
- `document.parse_status` 仍为 `uploaded`、`parsing`、`indexed`、`failed`。
- 高风险或低置信度 Agent 输出仍只进入 `waiting_human` / `waiting_approval`，不生成可执行业务动作。

## 行为变化

- `agent-service` 的默认知识检索从 Phase 005 的显式 Mock 知识来源，切换为本地 `rag-service` 的 `RagKnowledgeSearchTool`。
- `ticket.search.v1` 仍是只读 Mock 契约工具，`report.save.v1` 仍是本地 Mock 保存。
- RAG 检索失败不会静默降级为 Mock 结果，而是按统一错误形状进入工具失败路径。
- 契约校验脚本从 Phase 002 固定禁止所有运行时产物，调整为阶段感知地禁止未批准 host 的实现产物。

## 测试与验证

已运行并通过：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-contracts.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-rag-service.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-agent-service.ps1
```

验证结果：

- `validate-contracts.ps1` 通过。
- `validate-rag-service.ps1` 通过，4 个单元测试通过，2 个 RAG eval 用例通过，`hit_rate` 与 `citation_accuracy` 均为 `1.0`。
- `validate-agent-service.ps1` 通过，3 个单元测试通过，2 个 Agent eval 用例通过；成功路径包含 `local_rag` 工具记录和非 Mock 引用，失败路径保留可重试错误。

## Blocker 与遗留风险

- 无 blocker。
- 本地词项索引只是 Phase 007 MVP 替代检索方案，不代表生产级向量检索能力。
- `index_ref` 已保持存储无关，后续生产化仍需在不破坏 `knowledge.search.v1` 的前提下选择 pgvector、Milvus 或其他方案。
- 工作区在本窗口开始前已存在大量 dirty / untracked 文件；本窗口未 stage，也未 commit。
