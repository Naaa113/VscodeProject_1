# Phase 006 Steering 决策

## 决策状态

已由用户在 2026-06-01 批准。Window 0 已选择 Phase 006，并在进入 Window 1 前停止。

## 当前状态摘要

- 状态来源：`docs/harness/state/current-state.md`
- 当前阶段：Phase 005 `agent-service` 最小 LangGraph 闭环，状态为 `completed`
- Steering 状态：`handoff_done`
- 最新 final handoff：`docs/harness/handoffs/phase-005-final.md`
- 最新 Review 结论：`docs/harness/handoffs/phase-005-review-fix-1.md`，结论为 `approve`
- 架构状态：身份域、工单域和 Agent 编排域的最小本地运行时已落地；RAG、前端、网关、审批、通知等业务域仍未创建运行时
- 契约状态：`packages/shared-contracts` 已包含认证、工单、AI 任务、Agent run / step 事件、工具调用、知识检索和报告保存的 v1 草案；Phase 003 到 Phase 005 已分别验证身份、工单和 Agent 本地闭环
- 当前关键债务：DEBT-006 高风险动作审批边界仍为 D0；DEBT-007 AI 质量指标仍缺真实 RAG / Agent 基准集；DEBT-004 向量检索方案仍 deferred

## 最新 completed / blocked / active phase 和读取到的 handoff 文件

本次启动已列出 `docs/harness/handoffs`，并按恢复规则读取当前状态与最新阶段相关文件。

最新 completed phase：

- Phase 005：`agent-service` 最小 LangGraph 闭环

当前 active phase：

- 无。Phase 005 已完成，状态为 `handoff_done`。

当前 blocked phase：

- 无。

读取到的关键 handoff 文件包括：

- `phase-000-harness-baseline.md`
- `steering-decision-phase-005.md`
- `phase-005-architect.md`
- `phase-005-implementation.md`
- `phase-005-review.md`
- `phase-005-fix-1-implementation.md`
- `phase-005-review-fix-1.md`
- `phase-005-final.md`

Phase 006 对应文件检查：

- `steering-decision-phase-006.md`：本文件创建前不存在
- `phase-006-architect.md`：不存在，符合尚未进入 Window 1 的状态
- `phase-006-implementation.md`：不存在
- `phase-006-review.md`：不存在
- `phase-006-final.md`：不存在

阻断判断：

- 不阻断。`current-state.md`、`phase-005-final.md` 和 `07-phase-backlog.md` 均指向 Phase 006 `web-console` 最小工作台作为推荐下一步。
- 不应回退使用 bootstrap 推荐 Phase 001，因为 Phase 001 到 Phase 005 均已有 final handoff。

## 候选阶段评分表

评分范围：1 分为弱匹配，5 分为强匹配。

| 候选阶段 | 是否是 backlog 下一步 | 解除当前 D0/D1 债务 | 缩小不确定性 | 可验证闭环 | 避免过早实现业务功能 | 总分 |
|---|---:|---:|---:|---:|---:|---:|
| Phase 006：`web-console` 最小工作台 | 5 | 3 | 5 | 5 | 4 | 22 |
| Phase 008：人工审批与动作命令 | 2 | 5 | 4 | 4 | 3 | 18 |
| Phase 007：RAG 最小知识库闭环 | 4 | 4 | 4 | 4 | 3 | 19 |
| 身份 / 工单 / Agent 生产化增强 | 2 | 2 | 3 | 3 | 3 | 13 |
| Phase 009：可观测性与系统测试 | 1 | 3 | 3 | 3 | 2 | 12 |

## Primary candidate

Primary candidate：Phase 006：`web-console` 最小工作台。

选择原因：

- 它是 `07-phase-backlog.md` 中 Phase 005 之后的直接下一步，符合 MVP 优先顺序。
- Phase 003 已提供身份上下文、租户校验、权限摘要和身份审计基础；Phase 004 已提供客户与工单事实源、工单列表和详情能力；Phase 005 已提供 Agent run / step / report 本地闭环。现在已经有足够的可展示对象，而不只是空 UI。
- 它可以把 MVP 主链路第一次拉到用户可见层：用户从界面完成“登录或 Mock 登录联调 -> 查看工单 -> 发起任务 -> 查看步骤 -> 查看报告”。
- 它能缩小前端与契约之间的不确定性，验证 OpenAPI、状态枚举、错误响应、加载态、空状态和 Agent 执行链路是否足够支撑工作台体验。
- 它不会直接关闭 DEBT-006，但必须把高风险结果展示为 `waiting_human` / `waiting_approval`，不提供审批通过、动作执行、自动派单或通知能力，从而保持风险受控。
- 它比直接进入 RAG 或审批更近：先建立可操作工作台，再让后续 RAG 引用和审批动作有明确展示入口。

建议冻结范围：

- 创建或补齐 `apps/web-console` 最小 React + TypeScript 工作台运行时。
- 实现登录页或 Mock 登录联调入口，消费或模拟 Phase 003 身份契约，不复制身份事实。
- 实现工单列表和工单详情，消费或模拟 Phase 004 工单契约，不依赖 `ticket-service` 内部数据库字段。
- 实现 AI 任务发起入口，限定为投诉分析类样例任务。
- 实现 Agent 执行链路展示，至少展示 run 状态、step 状态、节点摘要、错误和 Mock 来源标记。
- 实现报告预览，展示事实、推断、风险、建议和引用来源。
- 实现加载、错误、空状态和权限 / 租户错误提示。
- 可以使用 API Mock、fixture 或本地最小后端联调，但必须显式标记 Mock 边界。
- 提供前端本地验证入口，例如 lint、单元测试、组件测试或最小端到端冒烟。

明确非目标：

- 不实现真实 RAG、文档上传、Embedding、向量库、文档解析或引用准确率完整评测。
- 不实现审批动作执行、审批实例、动作命令、自动创建跟进任务或任何高风险业务动作。
- 不实现生产级 `api-gateway`、统一鉴权网关、RabbitMQ、SSE、通知服务或系统级可观测性。
- 不新增工单状态变更 API、自动派单、自动升级、自动关闭或对外通知能力。
- 不修改 `identity-service`、`ticket-service` 或 `agent-service` 的业务事实所有权。
- 不把 Mock API、Mock Agent 状态、Mock 知识来源或 Mock 报告保存描述为生产能力。

## Fallback candidate

Fallback candidate：Phase 008：人工审批与动作命令。

仅在以下条件下考虑 fallback：

- 用户明确要求优先收敛 DEBT-006：高风险动作审批边界未实现。
- Window 1 能把范围限制为审批实例、动作命令、幂等键、审批记录审计和策略阻断边界。
- Window 1 明确审批如何绑定 Phase 003 身份上下文、Phase 004 工单事实源和 Phase 005 Agent 风险标记。
- 不允许在没有审批通过记录的情况下执行高风险动作。

为什么它不是 primary：

- 它不是 backlog 中 Phase 005 后的直接下一步。
- 当前还没有前端工作台承载审批可见性，直接做审批会缺少用户确认入口和操作反馈闭环。
- Phase 005 已通过 `waiting_human` / `waiting_approval` 占位阻断风险，DEBT-006 仍是 D0，但不要求为了关闭债务而跳过当前 MVP 主链路的前端闭环。

## 不选其他阶段的原因

- 不选 Phase 007：RAG 最小知识库闭环仍受 DEBT-004 向量检索方案影响；同时前端尚不能展示知识引用和报告链路，先做工作台能让 RAG 的后续验收更清晰。
- 不选 Phase 009：可观测性与系统测试需要前端、Agent、RAG 或审批至少形成更完整端到端路径；当前过早。
- 不选 Phase 010：企业级能力扩展属于 MVP 之后的平台化能力，距离当前状态过远。
- 不选身份 / 工单 / Agent 生产化增强：DEBT-011、DEBT-012、DEBT-013 当前为 D2，不应阻断 MVP 主链路进入用户可见工作台。

## Window 1 的任务边界

Window 1 必须拆清楚以下内容，完成后才能允许 Window 2 实现 Phase 006：

- Contract：确认前端消费的认证、当前用户、工单列表、工单详情、AI 任务、Agent step、报告和错误响应契约；明确哪些使用真实本地服务，哪些使用 Mock fixture。
- API 边界：前端只能通过公开 API 契约或 Mock adapter 消费数据，不得读取服务内部数据库、H2 fixture 或私有模型字段。
- Host 边界：`apps/web-console` 只拥有页面状态、交互状态、视图模型和 Mock adapter；不拥有身份、工单、Agent、知识库、审批或动作事实。
- 状态生命周期：定义登录态、租户上下文、工单加载、任务发起、Agent run / step 展示、报告生成、错误、空状态和高风险等待人工确认的前端生命周期。
- Mock 边界：所有 Mock 用户、Mock 工单、Mock Agent run / step、Mock 知识引用和 Mock 报告必须在代码、fixture 或文档中可识别。
- 高风险边界：前端只能展示高风险或低置信度结果进入人工确认 / 等待审批状态，不得提供审批通过、动作执行、自动派单、通知发送或状态变更入口。
- 验收条件：用户能完成“登录或 Mock 登录联调 -> 查看工单 -> 发起任务 -> 查看步骤 -> 查看报告”；加载、错误、空状态完整；报告引用和风险标记可见；页面不依赖内部数据库字段。
- 测试策略：前端组件测试、契约 fixture 一致性检查、关键流程冒烟、错误 / 空状态测试和最小可访问性检查。
- 回滚或降级：如果真实后端联调不稳定，Window 1 必须定义可接受的 Mock-first 路径，并说明哪些能力延期到网关、RAG 或审批阶段。

## 用户批准结论

用户已批准 Window 0 推荐进入：

Phase 006：`web-console` 最小工作台

Window 1 应基于本决策输出 Phase 006 的阶段架构、契约影响、host 边界、状态生命周期、验收清单和实现任务拆分。Window 0 到此停止。
