# Phase 007 Steering 决策

## 决策状态

已由用户在 2026-06-03 批准。Window 0 已选择 Phase 007，并在进入 Window 1 前停止。

## 当前状态摘要

- 状态来源：`docs/harness/state/current-state.md`
- 当前日期：2026-06-03
- 当前阶段：Phase 006 `web-console` 最小工作台，状态为 `completed`
- Steering 状态：`handoff_done`
- 最新 final handoff：`docs/harness/handoffs/phase-006-final.md`
- 最新 Review 结论：`docs/harness/handoffs/phase-006-review-fix-2.md`，结论为 `approve`
- 当前架构事实：身份域、工单域、Agent 编排域和前端工作台最小运行时已落地；`api-gateway`、`workflow-service`、`notification-service`、`rag-service` 仍无业务运行时
- 当前契约事实：`packages/shared-contracts` 已包含认证、工单、AI 任务、Agent run / step、文档、知识检索、报告和工具调用的 v1 草案；Phase 003 到 Phase 006 已分别验证身份、工单、Agent 本地闭环和前端 Mock-first 工作台
- 当前关键债务：DEBT-004、DEBT-006、DEBT-007、DEBT-010、DEBT-014 仍需在后续阶段收敛，其中 DEBT-004 与 DEBT-014 进入 RAG 阶段前必须处理或明确处理方式

## 最新 completed / blocked / active phase 和读取到的 handoff 文件

最新 completed phase：

- Phase 006：`web-console` 最小工作台

当前 active phase：

- 无。Phase 006 已完成 Window 4 交接，当前状态为 `handoff_done`。

当前 blocked phase：

- 无。`current-state.md` 与 `phase-006-final.md` 均未记录当前阶段阻断。

本次启动已列出 `docs/harness/handoffs`，发现 Phase 000 到 Phase 006 的 handoff 文件均存在，且尚无 Phase 007 handoff 文件。

已读取的固定治理工件：

- `docs/harness/00-project-charter.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/11-cycle-runbook.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/phase-000-harness-baseline.md`

已读取的最新阶段 handoff：

- `docs/harness/handoffs/steering-decision-phase-006.md`
- `docs/harness/handoffs/phase-006-architect.md`
- `docs/harness/handoffs/phase-006-implementation.md`
- `docs/harness/handoffs/phase-006-review.md`
- `docs/harness/handoffs/phase-006-fix-1-implementation.md`
- `docs/harness/handoffs/phase-006-review-fix-1.md`
- `docs/harness/handoffs/phase-006-fix-2-implementation.md`
- `docs/harness/handoffs/phase-006-review-fix-2.md`
- `docs/harness/handoffs/phase-006-final.md`

Phase 007 对应文件检查：

- `steering-decision-phase-007.md`：本文件创建前不存在，本次创建
- `phase-007-architect.md`：不存在，符合尚未进入 Window 1 的状态
- `phase-007-implementation.md`：不存在
- `phase-007-review.md`：不存在
- `phase-007-final.md`：不存在

阻断判断：

- 不阻断。当前状态、最新 final handoff 和阶段队列均指向 Phase 007 RAG 最小知识库闭环作为下一步候选。
- 不应回退使用 bootstrap 推荐 Phase 001，因为 Phase 001 到 Phase 006 均已有 final handoff。
- 不得绕过 Window 1。Phase 007 必须先由 Window 1 拆清楚架构、契约、状态生命周期、债务处理和验收边界。

## 候选阶段评分表

评分范围为 1 到 5 分，5 分表示强匹配。

| 候选阶段 | 是否是 backlog 下一步 | 是否解除当前 D0/D1 债务 | 是否缩小不确定性 | 是否能形成可验证闭环 | 是否避免过早实现业务功能 | 总分 |
|---|---:|---:|---:|---:|---:|---:|
| Phase 007：RAG 最小知识库闭环 | 5 | 4 | 5 | 5 | 4 | 24 |
| Phase 008：人工审批与动作命令 | 3 | 5 | 4 | 4 | 3 | 19 |
| 契约校验阶段感知修复单独阶段 | 2 | 4 | 2 | 4 | 5 | 17 |
| `api-gateway` 真实联调阶段 | 2 | 2 | 3 | 3 | 2 | 12 |
| 身份 / 工单 / Agent 生产化增强 | 1 | 2 | 3 | 3 | 3 | 12 |
| Phase 009：可观测性与系统测试 | 1 | 3 | 3 | 3 | 2 | 12 |

评分说明：

- Phase 007 是 backlog 中 Phase 006 后的直接下一步，且与 `phase-006-final.md` 的推荐一致。
- Phase 007 可以收敛 DEBT-004、DEBT-007，并推动 DEBT-010 的报告引用与知识来源边界；同时必须处理或显式安排 DEBT-014，否则后续契约校验入口继续失真。
- Phase 008 对 DEBT-006 的 D0 债务最直接，但 current-state 和 Phase 006 final 都将它列为 fallback，适合用户明确要求优先处理审批动作时使用。
- 单独修复契约校验脚本能处理 DEBT-014，但范围过窄，不足以作为 MVP 主链路下一阶段；它更适合作为 Phase 007 的入口治理任务。

## Primary candidate

Primary candidate：Phase 007：RAG 最小知识库闭环。

选择原因：

- 它是 `07-phase-backlog.md` 中 Phase 006 后的直接下一步。
- Phase 006 已提供工单、任务、Agent 步骤、引用和报告预览入口，RAG 可以把前端已有的 Mock 知识引用替换为真实可追踪文档来源。
- Phase 005 已提供 `knowledge.search.v1` 的显式 Mock 消费方式，Phase 007 可以把该工具从 Mock 边界推进到最小真实检索闭环。
- Phase 007 直接缩小 DEBT-004 的向量检索方案不确定性，也能补齐 DEBT-007 真实 RAG / Agent 质量基准集的第一版样例。
- 它能形成清晰验收闭环：上传或登记样例 SOP / FAQ，完成解析、切片、检索、引用追踪，并让 Agent 报告引用可回溯到文档片段。
- 它不是更远的大目标。Phase 007 只解决真实知识来源和引用闭环，不实现审批动作、生产网关、消息队列、自动通知、工单状态变更或完整生产 RAG 平台。

建议冻结范围：

- 收敛 DEBT-004：在 Window 1 明确 MVP 向量检索或替代检索方案，说明为什么选择该方案，以及如何保持 `index_ref` 存储无关。
- 收敛 DEBT-014：在进入 RAG 实现前，让契约校验脚本的阶段感知规则与 Phase 006 已批准前端运行时一致，或者明确它在 Phase 007 中的处理边界和验收命令。
- 明确 `rag-service` 的最小 host 边界，可创建最小运行时，但不得扩展为完整生产 RAG 平台。
- 建立文档上传登记或本地样例文档导入，记录 `document_id`、`knowledge_base_id`、`tenant_id`、`object_key`、`parse_status` 和审计上下文。
- 建立文档解析、切片和引用追踪的最小模型，确保检索结果可回溯到 `document` 与 `document_chunk`。
- 建立 `knowledge.search.v1` 的最小真实或半真实检索实现，允许使用 MVP 替代检索方案，但必须显式标记能力边界。
- 让 `agent-service` 通过契约化工具消费 RAG 检索结果，不直接读取 RAG 内部存储。
- 让报告引用从 Mock 来源走向可追踪文档片段，保持事实、推断、风险、建议和引用分区。
- 建立最小 RAG 评测样例，至少覆盖命中率和引用准确率的计算方式。

明确非目标：

- 不实现审批实例、动作命令、自动派单、工单状态变更、通知发送或对外通知。
- 不让 Agent 直接写入身份、工单、知识库、审批或报告事实表。
- 不实现生产级 FastAPI、Spring Boot 网关、RabbitMQ 消费者、SSE 实时流、对象存储生产接入或 Kubernetes 部署。
- 不把临时检索、样例文档或本地索引伪装成生产 RAG 能力。
- 不绕过 `shared-contracts` 私自新增跨服务字段。
- 不在没有人工确认边界的情况下执行高风险建议。

## Fallback candidate

Fallback candidate：Phase 008：人工审批与动作命令。

仅在以下条件下考虑 fallback：

- 用户明确要求优先收敛 DEBT-006 高风险动作审批边界。
- Window 1 能把范围严格限制为审批实例、动作命令、幂等键、审批记录审计和策略阻断边界。
- Window 1 明确审批如何绑定 Phase 003 身份上下文、Phase 004 工单事实源、Phase 005 Agent 风险标记和 Phase 006 前端展示入口。
- 不允许在没有审批通过记录的情况下执行高风险动作。

为什么它不是 primary：

- 它不是 backlog 中 Phase 006 后的直接下一步。
- 当前 RAG 仍停留在显式 Mock 知识来源；如果先做动作命令，审批链路会建立在不完整知识引用基础上。
- 当前 DEBT-006 已通过 `waiting_human` / `waiting_approval` 占位阻断高风险执行，仍是严重债务，但没有要求绕过 RAG 引用闭环立刻实现动作执行。

## 不选其他阶段的原因

- 不选单独的契约校验阶段：DEBT-014 必须收敛，但单独作为阶段会打断 MVP 主链路。它应作为 Phase 007 的入口治理任务或验收前置项。
- 不选 `api-gateway` 真实联调：真实网关会扩大鉴权、路由、CORS、SSE 和服务联调范围，当前更紧迫的不确定性是 RAG 知识来源和引用可追踪性。
- 不选身份 / 工单 / Agent 生产化增强：DEBT-011、DEBT-012、DEBT-013 当前为 D2，生产化重要但不是下一步 MVP 闭环的最大缺口。
- 不选 Phase 009：系统测试和可观测性需要更完整的真实 RAG 或审批链路作为端到端对象，当前进入过早。
- 不选 Phase 010：企业级能力扩展属于 MVP 之后的平台化阶段，距离当前状态过远。

## Window 1 的任务边界

Window 1 必须拆清楚以下内容，完成后才能允许 Window 2 实现 Phase 007：

- Contract：确认或提议文档 API、RAG 查询 API、`knowledge.search.v1`、`document.uploaded.v1`、`document.indexed.v1`、引用字段、错误码和示例 payload。
- Host 边界：明确 `ai-services/rag-service` 拥有文档解析、切片、检索和引用溯源；不拥有工单、身份、审批、动作或业务结论。
- 数据所有权：明确 `knowledge_base`、`document`、`document_chunk`、`embedding_index_ref` 的归属，说明哪些是本地 MVP 存储，哪些只是未来生产存储引用。
- 向量检索决策：处理 DEBT-004，选择 pgvector、Milvus、轻量替代检索或其他 MVP 方案，并说明退出条件。
- 契约校验债务：处理 DEBT-014，明确 `scripts/validate-contracts.ps1` 如何识别已批准实现产物，避免继续把 `apps/web-console` 视为 forbidden artifact。
- 状态生命周期：定义文档从 `uploaded` 到 `parsing`、`indexed` 或 `failed` 的创建者、迁移者、失败落盘、用户可见位置、审计和租户隔离。
- Agent 工具边界：明确 `agent-service` 只能通过 `knowledge.search.v1` 消费 RAG，不得读取 RAG 内部文件、索引或数据库。
- 前端承载边界：明确 Phase 006 工作台是否只展示引用结果，还是允许新增最小知识库入口；如果新增入口，必须限制在文档登记、状态查看和检索结果展示。
- 评测边界：定义最小样例集、RAG 命中率、引用准确率、失败样例和人工标注入口，不追求生产级指标阈值。
- 高风险边界：RAG 只提供来源和引用，不提供审批通过、动作执行、自动派单、通知发送或工单状态变更。
- 验收命令：明确需要运行的 Java、Python、前端、契约和 RAG 本地验证命令；如果 `validate-contracts` 因旧规则失败，必须在本阶段解释或修复。
- 回滚或降级：如果向量方案或依赖安装受限，必须定义可接受的本地替代检索方案和 Mock 边界，不得伪装成生产向量检索。

## 需要用户批准的问题

用户已批准将下一阶段冻结为 Phase 007：RAG 最小知识库闭环，并允许 Window 1 开始拆解契约、边界、状态生命周期和验收条件。

Window 0 不进入 Window 1，不创建架构 handoff，不实现业务代码。
