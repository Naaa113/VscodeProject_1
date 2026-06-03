# Phase 005 Steering 决策

## 决策状态

已由用户在 2026-06-01 批准。Window 0 已选择 Phase 005，并在进入 Window 1 前停止。

## 当前状态摘要

- 状态来源：`docs/harness/state/current-state.md`
- 当前阶段：Phase 004 `ticket-service` 最小工单模型，状态为 `completed`
- Steering 状态：`handoff_done`
- 最新 final handoff：`docs/harness/handoffs/phase-004-final.md`
- 最新 Review 结论：`docs/harness/handoffs/phase-004-review.md`，结论为 `approve`
- 架构状态：`identity-service` 和 `ticket-service` 的最小 Java + H2 本地运行时已落地；二者不是生产级 Spring Boot / PostgreSQL 服务
- 业务代码状态：已存在最小身份服务与最小工单服务；Agent、RAG、前端、网关、审批、通知等运行时仍未实现
- 契约状态：`packages/shared-contracts` 已包含认证、工单、AI 任务、Agent 事件、知识检索、工具调用等 v1 草案；其中认证和工单契约已被最小运行时验证，Agent 运行时尚未验证
- 当前关键债务：DEBT-006 高风险动作审批边界仍为 D0；DEBT-007 AI 质量指标基准集仍为 D1；DEBT-004 向量检索方案仍 deferred

## 最新 completed / blocked / active phase 和读取到的 handoff 文件

本次启动已列出 `docs/harness/handoffs`，读取到的关键 handoff 文件包括：

- `phase-000-harness-baseline.md`
- `steering-decision-phase-001.md`
- `phase-001-architect.md`
- `phase-001-implementation.md`
- `phase-001-review.md`
- `phase-001-final.md`
- `steering-decision-phase-002.md`
- `phase-002-architect.md`
- `phase-002-implementation.md`
- `phase-002-review.md`
- `phase-002-fix-1-implementation.md`
- `phase-002-review-fix-1.md`
- `phase-002-final.md`
- `steering-decision-phase-003.md`
- `phase-003-architect.md`
- `phase-003-implementation.md`
- `phase-003-review.md`
- `phase-003-fix-1-implementation.md`
- `phase-003-review-fix-1.md`
- `phase-003-final.md`
- `steering-decision-phase-004.md`
- `phase-004-architect.md`
- `phase-004-implementation.md`
- `phase-004-review.md`
- `phase-004-final.md`

最新 completed phase：

- Phase 004：`ticket-service` 最小工单模型

当前 active phase：

- 无。Phase 004 已完成，状态为 `handoff_done`。

当前 blocked phase：

- 无。

Phase 005 对应文件检查：

- `steering-decision-phase-005.md`：本文件创建前不存在
- `phase-005-architect.md`：不存在，符合尚未进入 Window 1 的状态
- `phase-005-implementation.md`：不存在
- `phase-005-review.md`：不存在
- `phase-005-final.md`：不存在

阻断判断：

- 不阻断。`current-state.md` 与 `phase-004-final.md` 均指向 Phase 005 `agent-service` 最小 LangGraph 闭环作为推荐下一步。
- 不应回退使用 bootstrap 推荐 Phase 001，因为 Phase 001 到 Phase 004 均已有 final handoff。

## 候选阶段评分表

评分范围：1 分为弱匹配，5 分为强匹配。

| 候选阶段 | 是否是 backlog 下一步 | 解除当前 D0/D1 债务 | 缩小不确定性 | 可验证闭环 | 避免过早实现业务功能 | 总分 |
|---|---:|---:|---:|---:|---:|---:|
| Phase 005：`agent-service` 最小 LangGraph 闭环 | 5 | 4 | 5 | 5 | 4 | 23 |
| Phase 008：人工审批与动作命令 | 2 | 5 | 3 | 4 | 2 | 16 |
| Phase 006：`web-console` 最小工作台 | 4 | 2 | 3 | 4 | 3 | 16 |
| Phase 007：RAG 最小知识库闭环 | 3 | 4 | 4 | 4 | 3 | 18 |
| Phase 009：可观测性与系统测试 | 1 | 3 | 3 | 3 | 2 | 12 |

## Primary candidate

Primary candidate：Phase 005：`agent-service` 最小 LangGraph 闭环。

选择原因：

- 它是 `07-phase-backlog.md` 中 Phase 004 之后的直接下一步，符合 MVP 优先顺序。
- Phase 003 已提供身份上下文、租户校验、权限摘要和身份审计基础；Phase 004 已提供客户与工单事实源、工单查询 API、工单详情 API、`ticket.search.v1` 只读契约、租户隔离和工单审计字段。Phase 005 已具备进入最小 Agent 闭环的前置事实源。
- 它可以开始收敛 DEBT-007：通过最小 AI eval 样例、Agent run / step 状态和工具调用日志，让 Agent 质量从概念进入可验证状态。
- 它不能直接关闭 DEBT-006，但可以在 Risk / Supervisor 节点和 `waiting_human` / `waiting_approval` 占位状态中明确高风险输出必须进入人工确认，防止 Agent 阶段绕过审批执行动作。
- 它能形成可验证闭环：给定样例投诉分析任务，使用 Mock 或契约化工具调用检索工单与知识，生成结构化报告，并记录每个 Agent step 的状态、输入摘要、输出摘要、失败原因和引用来源标记。
- 它比直接做前端、RAG 或审批更贴近当前依赖顺序：先验证 Agent 编排和工具契约，再让前端展示链路，再补真实 RAG 与审批执行。

建议冻结范围：

- 创建或补齐 `ai-services/agent-service` 的最小 Python Agent 运行时，范围只限 Agent 编排闭环。
- 定义最小 LangGraph 或等价状态机，包含 Planner、Retriever、Data Analyst、Risk、Supervisor、Report 的简化节点。
- 使用 Mock 或契约化工具调用消费 `ticket.search.v1`，不得直接访问 `ticket-service` 数据库或绕过公开 API / 工具契约。
- 可使用 Mock 知识检索结果或明确标记为 Mock 的来源；不得实现真实 RAG 索引、Embedding 或向量库。
- 写入或模拟 Agent run / step 状态，至少覆盖 `created`、`started`、`step_running`、`completed`、`failed`，以及 step 的 `pending`、`running`、`success`、`failed`、`waiting_human`。
- 输出结构化报告，并区分事实、推断、风险和建议。
- 提供最小 AI eval 样例，覆盖工作流完成、工具调用成功、低置信度或高风险进入人工确认占位。
- 提供本地验证脚本，例如 `scripts/validate-agent-service.ps1`。

明确非目标：

- 不实现审批动作执行、动作命令落地、自动创建跟进任务或任何高风险业务动作。
- 不实现真实 RAG 检索、文档上传、Embedding、向量库、文档解析或引用准确率完整评测。
- 不实现 `web-console`、`api-gateway`、`workflow-service`、`notification-service` 或新的 Java 业务服务运行时。
- 不修改 `identity-service` 或 `ticket-service` 的业务事实所有权。
- 不新增工单状态变更 API、自动派单、自动升级、自动关闭或通知发送能力。
- 不把 Mock 工具或 Mock 知识来源伪装成生产能力。

## Fallback candidate

Fallback candidate：Phase 008：人工审批与动作命令。

仅在以下条件下才考虑 fallback：

- 用户明确要求优先处理 DEBT-006：高风险动作审批边界未实现。
- Window 1 能把范围限制为审批实例、动作命令、幂等键、审批记录审计和策略阻断边界，不直接让 Agent 执行动作。
- Window 1 明确说明审批对象如何绑定 Phase 003 身份上下文与 Phase 004 工单事实源，避免动作目标悬空或跨租户泄露。

为什么它不是 primary：

- 它不是 backlog 中 Phase 004 后的直接下一步。
- 审批和动作命令需要 Agent 输出、风险判断、动作请求和工单事实源共同驱动；在 Agent 最小闭环之前先实现审批，容易出现没有真实调用方的空转模型。
- DEBT-006 可以通过 Phase 005 的非目标和 Risk / Supervisor / 人工确认占位继续受控，不需要为了关闭债务而跳过 Agent 编排闭环。

## 不选其他阶段的原因

- 不选 Phase 006：前端工作台需要可展示的 Agent run / step 和报告状态；当前直接做前端会依赖大量 Mock，容易形成 UI 假闭环。
- 不选 Phase 007：RAG 最小闭环仍受 DEBT-004 向量检索方案影响；Phase 005 可以先允许 Mock 知识来源，并把真实 RAG 留到下一阶段收敛。
- 不选 Phase 009：可观测性与系统测试需要身份、工单、Agent、前端和 RAG 至少部分闭环；当前过早。
- 不选 Phase 010：企业级扩展属于 MVP 之后的平台化能力，距离当前阶段过远。
- 不选身份或工单生产化增强：DEBT-011 和 DEBT-012 当前为 D2，不应阻断 MVP 主链路继续进入 Agent 最小闭环。

## Window 1 的任务边界

Window 1 必须拆清楚以下内容，完成后才能允许 Window 2 实现 Phase 005：

- Contract：确认或补齐 AI 任务 API、Agent run / step 事件、Agent step 状态、报告结构、统一错误响应、工具调用日志和 `ticket.search.v1` 的消费方式。
- 工具边界：明确 `ticket.search.v1` 只读调用的输入、输出、权限、trace、租户上下文、失败响应、超时和降级策略；不得引入写操作工具执行。
- Host 边界：`agent-service` 只拥有 Agent 编排状态、推理过程、模型调用记录、工具调用记录和 eval 样例；不得拥有工单、客户、身份、审批实例、动作命令或知识库事实。
- 数据所有权：工单与客户事实归 `ticket-service`；身份与权限事实归 `identity-service`；知识库事实暂可使用 Mock 或契约化引用，真实 RAG 归后续 `rag-service`。
- 状态生命周期：定义 AI task、Agent run、Agent step、工具调用、报告生成、失败、重试、低置信度和高风险人工确认占位的生命周期。
- 高风险边界：Risk / Supervisor 节点必须把高风险建议标记为人工确认或等待审批，不得产生可执行动作命令。
- Mock 边界：所有 Mock 工具、Mock 知识来源和 Mock 模型响应必须显式标记，不得在 handoff 中描述为生产能力。
- 验收条件：给定样例任务可生成结构化报告；每个步骤可追踪；工具调用遵守契约；低置信度或高风险输出进入人工确认占位；失败路径有错误码和 trace；最小 AI eval 样例可运行。
- 测试策略：图状态测试、节点单元测试、工具 Mock 测试、契约一致性检查、失败路径测试、租户隔离上下文测试和最小 AI eval。
- 回滚或降级：如果 LangGraph、模型调用、工具契约、AI task API 或报告结构存在缺口，Window 1 必须明确最小可接受方案、延期项和阻断条件。

## 需要用户批准的问题

是否批准 Window 0 推荐进入：

Phase 005：`agent-service` 最小 LangGraph 闭环

批准后，Window 1 应基于本决策输出 Phase 005 的阶段架构、契约影响、host 边界、状态生命周期、验收清单和实现任务拆分。Window 0 到此停止。
