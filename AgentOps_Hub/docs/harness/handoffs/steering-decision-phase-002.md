# Phase 002 Steering 决策

## 决策状态

已由用户在 2026-05-31 批准。Window 0 已选择 Phase 002，并在进入 Window 1 前停止。

## 当前状态摘要

- 状态来源：`docs/harness/state/current-state.md`
- 当前阶段：Phase 001 Monorepo 骨架与本地开发基线 completed
- Steering 状态：`handoff_done`
- 最新 final handoff：`docs/harness/handoffs/phase-001-final.md`
- 架构状态：Phase 001 骨架已实现并通过评审，未落地业务代码
- 业务代码状态：未创建
- 当前仓库事实：已有 Phase 001 占位 monorepo 骨架、本地开发文档、ADR、`packages/shared-contracts` 占位目录和骨架验证脚本
- 当前契约事实：尚未冻结业务 OpenAPI、事件、工具契约、错误码、状态枚举示例或校验命令

## 最新阶段与 handoff 恢复

读取到的 handoff 文件：

- `phase-000-harness-baseline.md`
- `steering-decision-phase-001.md`
- `phase-001-architect.md`
- `phase-001-implementation.md`
- `phase-001-review.md`
- `phase-001-final.md`

最新 completed phase：

- Phase 001：Monorepo 骨架与本地开发基线

当前 active phase：

- 无。Phase 001 已完成，状态为 `handoff_done`。

当前 blocked phase：

- 无。

Phase 002 对应文件检查：

- `steering-decision-phase-002.md`：本文件创建前不存在
- `phase-002-architect.md`：不存在，符合尚未进入 Window 1 的状态
- `phase-002-implementation.md`：不存在
- `phase-002-review.md`：不存在
- `phase-002-final.md`：不存在

阻断判断：

- 不阻断。`current-state.md` 和 `phase-001-final.md` 都明确要求 Window 0 从 Phase 002 候选开始决策。
- 不应回退使用 bootstrap 推荐 Phase 001，因为 `phase-001-final.md` 已存在并且 Phase 001 已完成。

## 候选阶段评分表

评分范围：1 分为弱匹配，5 分为强匹配。

| 候选阶段 | 是否是 backlog 下一步 | 解除 D0/D1 债务 | 缩小不确定性 | 可验证闭环 | 避免过早业务实现 | 总分 |
|---|---:|---:|---:|---:|---:|---:|
| Phase 002：`shared-contracts` / OpenAPI / 事件契约 | 5 | 5 | 5 | 5 | 5 | 25 |
| Phase 003：`identity-service` 最小认证与租户模型 | 3 | 4 | 3 | 4 | 2 | 16 |
| Phase 007：RAG 最小知识库闭环 | 1 | 4 | 3 | 4 | 1 | 13 |
| Phase 008：人工审批与动作命令 | 1 | 5 | 3 | 4 | 1 | 14 |

## Primary candidate

Primary candidate：Phase 002：`shared-contracts` / OpenAPI / 事件契约。

选择原因：

- 它是 `07-phase-backlog.md` 中 Phase 001 之后的直接下一步。
- 它直接处理 DEBT-005，这是当前仍然打开的 D0 债务：跨服务契约未落地。
- Phase 001 已经创建 `packages/shared-contracts` 的目录和规则，Phase 002 可以在该基线上定义真实契约。
- 它能在业务服务、前端、Agent、RAG 之前冻结共同语言，避免 Java、Python、前端并行猜字段。
- 它的交付物可验证：OpenAPI 文件、事件 schema、工具契约、错误码、状态枚举、成功/失败示例、schema 校验命令。
- 它仍然不需要创建 Spring、React、FastAPI、数据库迁移、前端页面或 Agent workflow，符合避免过早业务实现的要求。

建议冻结范围：

- OpenAPI 初稿：认证、工单、AI 任务、知识库检索。
- 事件契约初稿：`ai.task.created.v1`、Agent run/step、document indexed 等。
- 状态枚举：`ai_task.status`、`agent_step.status`、`approval_instance.status`、`document.parse_status` 等。
- 错误码、分页、租户上下文、审计字段和统一错误响应。
- 工具调用契约：`ticket.search.v1`、`ticket.create_followup.v1`、`knowledge.search.v1`、`report.save.v1`。
- 成功示例、失败示例、版本策略、兼容性说明和契约校验入口。

明确非目标：

- 不实现 `identity-service`、`ticket-service`、`agent-service`、`rag-service` 或 `web-console`。
- 不创建业务数据库迁移、服务运行时、前端页面、Agent 节点、RAG 检索实现或真实模型接入。
- 不把候选契约伪装成已经由服务实现的接口。
- 不绕过人工审批边界定义高风险动作执行能力。

## Fallback candidate

Fallback candidate：Phase 003：`identity-service` 最小认证与租户模型。

仅在以下条件下才考虑 fallback：

- 用户明确拒绝先做 Phase 002，且要求优先推进身份域。
- Window 1 能把 Phase 003 限制为“先确认身份契约缺口和最小认证边界”，不得在缺少契约冻结的情况下直接实现服务。

为什么它不是 primary：

- Phase 003 需要依赖 Phase 002 的认证 API、统一错误码、租户上下文和审计字段契约。
- 直接进入身份实现会让服务字段先于 `shared-contracts` 固化，违反契约先行原则。
- 它虽然有助于后续租户隔离和 DEBT-006 的审批链路，但不能替代跨服务契约落地。

## 不选其他阶段的原因

- 不选 Phase 004：工单服务依赖身份、租户上下文、分页、错误码和工单 API 契约，当前还未冻结。
- 不选 Phase 005：Agent 最小闭环依赖事件、工具契约、状态枚举、报告契约和高风险动作边界，当前还未冻结。
- 不选 Phase 006：前端工作台依赖网关 API、状态通道、错误响应和任务步骤契约，当前还未冻结。
- 不选 Phase 007：RAG 仍有 DEBT-004 向量检索方案未决，且知识库检索契约需要先在 Phase 002 明确。
- 不选 Phase 008：它直接处理 DEBT-006，但审批与动作命令必须先在契约层表达，不能跳过 Phase 002。
- 不选 Phase 009 / Phase 010：它们属于后续诊断、评测、运维和平台化扩展，距离当前最小闭环过远。

## Window 1 的任务边界

Window 1 必须拆清楚以下内容，完成后才能允许 Window 2 实现契约文件：

- Contract：哪些 OpenAPI、事件、工具、错误码、状态枚举和示例属于 Phase 002；每类契约的文件位置、命名规则、版本策略和兼容策略。
- 边界：`packages/shared-contracts` 只拥有契约，不拥有业务实现；Java、Python、前端只能消费契约，不能在本阶段新增运行时实现。
- 状态生命周期：AI 任务、Agent run/step、文档处理、审批、动作命令的状态枚举如何与 `05-transition-lifetime.md` 对齐；失败、取消、等待人工的状态如何表达。
- 数据所有权：租户、用户、工单、AI 任务、Agent run、文档、审批、报告等字段的事实归属，避免契约把所有权写混。
- 验收条件：每个契约必须具备版本、schema、成功示例、失败示例、错误响应、租户上下文、审计字段和校验入口。
- 测试策略：至少定义 schema 校验、示例 payload 校验、契约目录完整性校验和禁止业务实现扫描。
- 回滚或降级：若某个契约无法冻结，Window 1 必须说明是 `Deferred`、`Proposed` 还是阻断项，不能让实现窗口猜字段。

## 需要用户批准的问题

是否批准 Window 0 推荐进入：

Phase 002：`shared-contracts` / OpenAPI / 事件契约

批准后，Window 1 应基于本决策输出 Phase 002 的阶段架构、契约影响、host 边界、状态生命周期、验收清单和实现任务拆分。Window 0 到此停止。
