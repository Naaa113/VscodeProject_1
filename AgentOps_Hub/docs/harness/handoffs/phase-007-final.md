# Phase 007 最终交接

## 阶段状态（phase status）

`completed`。

Phase 007 RAG 最小知识库闭环已完成 Window 4 交接。最新复审文件为 `docs/harness/handoffs/phase-007-review-fix-1.md`，结论为 `approve`。本阶段没有 blocker；本地确定性轻量索引仅代表 MVP 替代检索方案，不代表生产级 pgvector / Milvus 能力。

## 完成范围（completed scope）

- `ai-services/rag-service` 已落地最小本地 RAG 运行时，支持样例 SOP / FAQ 文档登记、解析、切片、本地索引、租户隔离检索和引用溯源。
- 文档生命周期覆盖 `uploaded -> parsing -> indexed` 与 `uploaded -> parsing -> failed`，解析失败保留 `DOCUMENT_PARSE_FAILED`，并可通过带 `tenant_id`、`requested_by`、`trace_id` 的入口重试。
- `agent-service` 默认通过 `knowledge.search.v1` 形状消费本地 RAG 检索结果，报告引用包含非 Mock 本地文档片段来源。
- RAG eval 已覆盖 2 个样例，计算 `hit_rate` 与 `citation_accuracy`，验证结果均为 `1.0`。
- `scripts/validate-contracts.ps1` 已改为阶段感知规则，不再把已批准的 `apps/web-console`、`agent-service`、`rag-service` 判定为 forbidden implementation artifact。

## 未变化契约（unchanged contracts）

- 认证、工单和 AI 任务既有 URL 未改变。
- `ticket.search.v1` 保持只读。
- `knowledge.search.v1` 保持只读、`requires_approval: false` 和存储无关。
- `document.parse_status` 仍为 `uploaded`、`parsing`、`indexed`、`failed`。
- 高风险或低置信度 Agent 输出仍只进入 `waiting_human` / `waiting_approval`，不生成审批实例、动作命令、自动派单、通知发送或工单状态变更。

## 契约、权威、生命周期与行为变化（changed contracts / authority / transition / behavior）

- Contract：`packages/shared-contracts/openapi/agentops-api.v1.yaml` 已补齐知识库文档摘要、知识检索响应和 citation 字段。
- Contract：`packages/shared-contracts/tools/knowledge.search.v1.schema.json` 已补齐 `citation.source_uri`，并保留 `mock_source` 的显式 Mock 边界。
- Contract：`document.uploaded.v1` 与 `document.indexed.v1` 已补齐文档文件名、知识库和租户相关 payload 字段。
- Authority：`rag-service` 只拥有文档、切片、本地索引引用和 citation；未接管身份、工单、审批、动作命令或报告事实表。
- Transition：文档解析、失败、重试和索引生命周期已具备租户上下文；跨租户解析或重试统一返回 `RESOURCE_NOT_FOUND`。
- Behavior：RAG 检索失败不会静默降级为 Mock；Mock 来源、本地 RAG 来源和未来生产 RAG 来源必须保持可区分。

## 验证摘要（validation summary）

Window 2 与 Window 3 已运行并通过：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-contracts.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-rag-service.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-agent-service.ps1
```

复审记录：

- 初次 review：`require fixes`，阻断项为文档解析 / 重试入口缺少租户上下文。
- Fix Pass 1：已要求 `parse_document` 与 `retry_parse` 校验 `tenant_id`、`requested_by`、`trace_id`。
- 复审：`approve`，确认 P1 finding 已关闭，无新的 belongs / authority / contract / transition / behavior 阻断项。

## 剩余债务（remaining debt）

- DEBT-006 仍为 D0：高风险动作审批边界尚未实现；当前只通过 `waiting_human` / `waiting_approval` 占位阻断。
- DEBT-010 仍为 D2：报告存储格式和导出格式未冻结。
- DEBT-011、DEBT-012、DEBT-013 仍记录身份、工单和 Agent 服务的生产化缺口。
- DEBT-015 新增：`rag-service` 仍缺少生产级 FastAPI、对象存储、pgvector / Milvus 或其他检索后端、持久化数据库、异步解析任务和真实上传入口。
- DEBT-016 新增：RAG / Agent 质量基准集仍缺少生产代表性语料、人工标注流程、幻觉率和工具调用质量扩展指标。

已关闭债务：

- DEBT-004：MVP 向量检索方案已以本地确定性轻量索引收敛。
- DEBT-007：最小 RAG eval 已落地。
- DEBT-014：契约校验脚本阶段感知冲突已修复。

## Window 0 最新状态（latest state for Window 0）

Window 0 下一次启动时应自动发现：

- 最新 final handoff：`docs/harness/handoffs/phase-007-final.md`
- 当前 Steering 状态：`handoff_done`
- 最新 Steering 决策：`docs/harness/handoffs/steering-decision-phase-007.md`
- 最新 Review 结论：`docs/harness/handoffs/phase-007-review-fix-1.md`，结论为 `approve`
- `docs/harness/state/current-state.md` 已更新为 Phase 007 完成态
- 当前仓库已具备身份域、工单域、Agent 编排域、前端工作台和 RAG 知识库的最小本地运行时

Window 0 不需要用户手动总结 Phase 007。下一次应从 Phase 008 人工审批与动作命令候选开始评估，并重新冻结范围。

## Window 0 推荐候选输入（recommended candidate inputs for Window 0）

- Phase 003 已提供身份上下文、租户校验、权限摘要和身份审计基础。
- Phase 004 已提供客户与工单事实源、工单查询 API、详情 API、`ticket.search.v1` 只读契约、租户隔离和工单审计字段。
- Phase 005 已提供 Agent 本地闭环、run / step 状态、结构化报告、人工确认占位和最小 eval 样例。
- Phase 006 已提供可见的工单、任务、步骤、引用和报告预览入口，可承载后续审批等待态与动作结果展示。
- Phase 007 已提供非 Mock 本地文档引用、RAG 工具记录、租户隔离检索和最小 RAG eval，为高风险建议提供可追踪知识来源。
- Phase 008 应限定为审批实例、动作命令、幂等键、审批记录审计和高风险策略阻断，不应顺手实现通知发送、网关生产化、RabbitMQ、真实 SSE、工单复杂状态机或生产级 RAG 平台。

## 本次交接修改文件（files changed in this handoff）

- `docs/harness/state/current-state.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-007-final.md`

本窗口未提交 commit，未 stage 文件，未修改业务代码。
