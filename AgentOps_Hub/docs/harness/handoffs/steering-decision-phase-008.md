# Phase 008 Steering 决策

## 决策状态

已由用户在 2026-06-03 批准。Window 0 已选择 Phase 008，并在进入 Window 1 前停止。

## 当前状态摘要

- 状态来源：`docs/harness/state/current-state.md`
- 当前日期：2026-06-03
- 当前阶段：Phase 007 RAG 最小知识库闭环，状态为 `completed`
- Steering 状态：`handoff_done`
- 最新 final handoff：`docs/harness/handoffs/phase-007-final.md`
- 最新 Review 结论：`docs/harness/handoffs/phase-007-review-fix-1.md`，结论为 `approve`
- 当前架构事实：身份域、工单域、Agent 编排域、前端工作台和 RAG 知识库最小本地运行时已落地；`api-gateway`、`workflow-service`、`notification-service` 仍无业务运行时
- 当前契约事实：`packages/shared-contracts` 已包含认证、工单、AI 任务、Agent run / step、文档、知识检索、报告、审批状态和动作命令相关草案；Phase 003 到 Phase 007 已分别验证身份、工单、Agent、本地前端工作台和本地 RAG 闭环
- 当前关键债务：DEBT-006 仍为 D0，高风险动作审批边界尚未实现；DEBT-010、DEBT-011、DEBT-012、DEBT-013、DEBT-015、DEBT-016 仍为 D2 或后续生产化债务

## 最新 completed / blocked / active phase 和读取到的 handoff 文件

最新 completed phase：

- Phase 007：RAG 最小知识库闭环。

当前 active phase：

- 无。Phase 007 已完成 Window 4 交接，当前状态为 `handoff_done`。

当前 blocked phase：

- 无。Phase 007 final handoff 记录本阶段无 blocker；最新复审结论为 `approve`。

本次启动已列出 `docs/harness/handoffs`，发现 Phase 000 到 Phase 007 的 final handoff 均存在，且尚无 Phase 008 handoff 文件。

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

- `docs/harness/handoffs/steering-decision-phase-007.md`
- `docs/harness/handoffs/phase-007-architect.md`
- `docs/harness/handoffs/phase-007-implementation.md`
- `docs/harness/handoffs/phase-007-review.md`
- `docs/harness/handoffs/phase-007-fix-1-implementation.md`
- `docs/harness/handoffs/phase-007-review-fix-1.md`
- `docs/harness/handoffs/phase-007-final.md`

Phase 008 对应文件检查：

- `steering-decision-phase-008.md`：本文件创建前不存在，本次创建。
- `phase-008-architect.md`：不存在，符合尚未进入 Window 1 的状态。
- `phase-008-implementation.md`：不存在。
- `phase-008-review.md`：不存在。
- `phase-008-final.md`：不存在。

阻断判断：

- 不阻断 Window 0 产出候选决策。当前状态、最新 final handoff、债务登记和阶段队列均指向 Phase 008 人工审批与动作命令作为下一步主候选。
- 不得回退使用 bootstrap 推荐 Phase 001，因为 Phase 001 到 Phase 007 均已有 final handoff。
- 不得绕过 Window 1。Phase 008 必须先由 Window 1 拆清楚契约、host 边界、状态生命周期、验收条件和非目标。

## 候选阶段评分表

评分范围为 1 到 5 分，5 分表示强匹配。

| 候选阶段 | 是否是 backlog 下一步 | 是否解除当前 D0/D1 债务 | 是否缩小不确定性 | 是否能形成可验证闭环 | 是否避免过早实现业务功能 | 状态机可推进性 | 总分 |
|---|---:|---:|---:|---:|---:|---:|---:|
| Phase 008：人工审批与动作命令 | 5 | 5 | 5 | 5 | 4 | 5 | 29 |
| Phase 009：可观测性与系统测试 | 3 | 1 | 3 | 5 | 4 | 2 | 18 |
| 报告存储格式和导出格式收敛 | 2 | 2 | 3 | 3 | 4 | 3 | 17 |
| `api-gateway` 真实联调 | 2 | 1 | 3 | 4 | 2 | 2 | 14 |
| 身份 / 工单 / Agent / RAG 生产化增强 | 1 | 1 | 3 | 3 | 2 | 1 | 11 |

评分说明：

- Phase 008 是 `07-phase-backlog.md` 中 Phase 007 后的直接下一步。
- Phase 008 直接处理 DEBT-006。该债务为 D0，当前只通过 `waiting_human` / `waiting_approval` 占位阻断高风险动作，尚无真实审批实例、动作命令、幂等键或审批审计。
- Phase 008 可以把 Phase 003 身份上下文、Phase 004 工单事实、Phase 005 Agent 风险判断、Phase 006 前端等待态和 Phase 007 可追踪引用串成更完整的人机协同闭环。
- Phase 009 虽然能提升诊断和系统验证能力，但在 DEBT-006 未处理前直接进入系统测试，会继续保留高风险动作无审批边界的核心缺口。
- 报告存储、网关联调和生产化增强都重要，但当前优先级低于 D0 审批边界。

## Primary candidate

Primary candidate：Phase 008：人工审批与动作命令。

选择原因：

- 它是 backlog 中 Phase 007 后的直接下一步。
- 它直面当前唯一 D0 债务 DEBT-006，补齐高风险动作的人机协同边界。
- Phase 003 已具备身份上下文、租户校验、权限摘要和审计基础，可为审批人、申请人和操作者提供来源。
- Phase 004 已具备客户与工单事实源、只读工单检索、租户隔离和工单审计字段，可作为动作命令的业务对象边界。
- Phase 005 已具备 Agent run / step、结构化报告和 `waiting_human` / `waiting_approval` 占位，可提供审批触发来源和风险理由。
- Phase 006 已具备工单、任务、步骤、引用和报告预览入口，可在后续承载审批等待态和动作结果展示。
- Phase 007 已具备非 Mock 本地文档引用、RAG 工具记录和最小 RAG eval，使高风险建议有可追踪知识来源。
- 它不是更远的大目标。Phase 008 应只收敛审批实例、动作命令、幂等键、审批记录审计和高风险策略阻断，不应顺手实现通知、生产网关、RabbitMQ、真实 SSE、复杂工单状态机或生产级 RAG 平台。

建议冻结范围：

- 定义并落地最小审批实例：审批对象、申请人、审批人、租户、来源任务、风险原因、引用摘要、状态和审计字段。
- 定义并落地最小动作命令：命令类型、目标业务对象、幂等键、状态、结果、错误、创建来源和审计字段。
- 建立高风险策略阻断：Agent 高风险或低置信度建议不得直接执行，必须先进入审批或被明确阻断。
- 建立审批通过后的最小动作闭环：动作命令只能在审批通过后创建或执行，且同一幂等键只能生效一次。
- 建立审批拒绝后的停止语义：审批拒绝后不得继续执行对应动作。
- 明确 `ticket.create_followup.v1` 或等价创建跟进任务能力是否纳入本阶段；如纳入，必须限制为一个最小、可审计、可回放的动作，不得扩展为自动派单、通知发送或复杂状态机。
- 让前端只展示审批等待、审批结果和动作结果的最小状态，不得绕过后端直接执行动作。

明确非目标：

- 不实现通知发送、邮件、站内信、Webhook 或第三方触达。
- 不实现真实网关生产化、真实 SSE、RabbitMQ、事件总线或异步任务平台。
- 不实现完整工单状态机、自动派单策略、完整 SLA 引擎或复杂流程编排。
- 不让 Agent 直接写入业务事实表、审批表或动作结果表。
- 不让 RAG、前端或 Agent 绕过 `workflow-service` 或等价审批动作边界执行高风险业务动作。
- 不把本阶段的最小动作命令解释为生产级工作流引擎。

## Fallback candidate

Fallback candidate：Phase 009：可观测性与系统测试。

fallback 条件：

- 只有在用户明确拒绝本轮处理 Phase 008，且明确接受 DEBT-006 继续以 `waiting_human` / `waiting_approval` 占位阻断的残余风险时，才考虑该 fallback。
- 即使选择 fallback，Window 1 也必须先记录 DEBT-006 仍为 D0，并说明为什么可观测性与系统测试不会新增任何高风险动作执行入口。

为什么它不是 primary：

- 它不是当前 backlog 的直接下一步。
- 它不能解除 DEBT-006。
- 在审批边界未实现前做端到端系统测试，只能验证“阻断态可见”，不能验证高风险动作的人机协同闭环。
- 它更适合作为 Phase 008 完成后的下一阶段，用来把认证、工单、Agent、RAG、审批动作和前端展示串成可追踪系统链路。

## 不选其他阶段的原因

- 不选报告存储格式和导出格式收敛：DEBT-010 是 D2，报告预览和引用已能支撑当前 MVP 主链路；它不应优先于 D0 审批边界。
- 不选 `api-gateway` 真实联调：真实网关会扩大鉴权、路由、CORS、实时通道和服务联调范围，当前更紧迫的是高风险动作不得绕过审批。
- 不选身份服务生产化：DEBT-011 是 D2，当前身份最小闭环足以支撑 Phase 008 的审批人、申请人和租户上下文。
- 不选工单服务生产化：DEBT-012 是 D2，当前工单事实源和只读检索足以支撑最小动作目标；完整状态机和 SLA 引擎不应提前塞入 Phase 008。
- 不选 Agent 生产化：DEBT-013 是 D2，当前 Agent 本地闭环已经能产生风险、报告、步骤和人工确认占位；本阶段应优先补审批边界，而不是替换真实模型或消息队列。
- 不选 RAG 生产化：DEBT-015 是 D2，Phase 007 已提供可追踪本地引用；生产级向量库、对象存储和异步解析应留到后续生产化阶段。
- 不选 Phase 010：企业级能力扩展属于 MVP 之后的平台化阶段，距离当前状态过远。

## Window 1 的任务边界

Window 1 必须拆清楚以下内容，完成后才能允许 Window 2 实现 Phase 008：

- Contract：审批 API、动作命令 API、`ticket.create_followup.v1` 或等价工具、审批状态枚举、动作命令状态枚举、错误码、示例 payload 和兼容策略。
- Host 边界：明确 `workflow-service` 或等价审批动作 host 拥有审批实例、审批记录、动作命令和幂等；`ticket-service` 仍拥有工单事实；`agent-service` 只提出请求和风险理由；`web-console` 只展示和提交审批意图。
- 数据所有权：明确 `approval_instance`、`approval_record`、`action_command`、`idempotency_key`、动作结果和审计记录归属；不得复制身份、工单、RAG 或 Agent 内部事实。
- 权限边界：明确谁可以创建审批、谁可以审批、谁可以拒绝、谁可以取消，以及权限不足、租户不一致和跨租户访问的错误语义。
- 状态生命周期：定义 `approval_instance.status` 的 `pending -> approved / rejected / cancelled / expired`，以及 `action_command.status` 的 `pending -> success / failed / cancelled`；回答每个状态由谁创建、谁迁移、失败如何落盘、用户在哪里看到、审计如何关联、是否会泄露租户数据。
- Agent 边界：明确 Agent 高风险建议如何绑定审批实例，如何保存风险原因、引用、报告片段和任务来源；不得生成未经审批的可执行命令。
- 幂等边界：明确幂等键来源、重复请求返回规则、审批通过重复提交规则、动作执行重试规则和失败后的可恢复语义。
- 前端边界：明确 Phase 006 工作台如何展示等待审批、审批通过、审批拒绝、动作成功和动作失败；不得在浏览器侧构造内部数据库字段或绕过后端执行动作。
- 审计边界：明确审批记录必须包含审批人、理由、时间、动作摘要、来源任务、trace、tenant 和引用摘要。
- 验收条件：高风险建议不会自动执行；审批通过后动作命令最多执行一次；审批拒绝后动作不执行；跨租户审批和动作访问不可泄露；失败响应保持统一错误形状。
- 验证命令：明确需要运行的契约校验、Java / Python / 前端测试和最小端到端验证；如果新增 `workflow-service` 运行时，必须定义本地验证脚本。
- 停止条件：如果必须引入通知发送、真实网关、RabbitMQ、真实 SSE、复杂流程引擎、生产数据库迁移或完整工单状态机才能完成闭环，Window 1 必须停止并交回 Window 0 / 用户确认。

## 需要用户批准的问题

用户已批准下一阶段冻结为 Phase 008：人工审批与动作命令，并允许 Window 1 开始拆解契约、边界、状态生命周期和验收条件。

Window 0 已停止；不会进入 Window 1，不创建架构 handoff，不实现业务代码。
