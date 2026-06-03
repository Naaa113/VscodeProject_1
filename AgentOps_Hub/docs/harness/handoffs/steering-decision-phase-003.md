# Phase 003 Steering 决策

## 决策状态

已由用户在 2026-05-31 批准。Window 0 已选择 Phase 003，并在进入 Window 1 前停止。

## 当前状态摘要

- 状态来源：`docs/harness/state/current-state.md`
- 当前阶段：Phase 002 `shared-contracts` / OpenAPI / 事件契约，状态为 `completed`
- Steering 状态：`handoff_done`
- 最新 final handoff：`docs/harness/handoffs/phase-002-final.md`
- 架构状态：Phase 002 契约资产已通过 fix-1 复审并完成交接，仍未落地业务代码
- 业务代码状态：未创建
- 当前仓库事实：已有 Phase 001 monorepo 占位骨架、本地开发文档、ADR、`packages/shared-contracts` v1 契约草案、示例、清单和契约校验脚本
- 当前契约事实：OpenAPI、事件、工具、错误码、状态枚举和示例已有 v1 草案；这些契约不代表服务已实现

## 最新 completed / blocked / active phase 和读取到的 handoff 文件

本次启动已列出 `docs/harness/handoffs`，读取到的 handoff 文件包括：

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

最新 completed phase：

- Phase 002：`shared-contracts` / OpenAPI / 事件契约

当前 active phase：

- 无。Phase 002 已完成，状态为 `handoff_done`。

当前 blocked phase：

- 无。

Phase 003 对应文件检查：

- `steering-decision-phase-003.md`：本文件创建前不存在
- `phase-003-architect.md`：不存在，符合尚未进入 Window 1 的状态
- `phase-003-implementation.md`：不存在
- `phase-003-review.md`：不存在
- `phase-003-final.md`：不存在

阻断判断：

- 不阻断。`current-state.md` 和 `phase-002-final.md` 都要求 Window 0 从 Phase 003 或其他候选重新决策。
- 不应回退使用 bootstrap 推荐 Phase 001，因为 Phase 001 和 Phase 002 均已有 final handoff。

## 候选阶段评分表

评分范围：1 分为弱匹配，5 分为强匹配。

| 候选阶段 | 是否是 backlog 下一步 | 解除当前 D0/D1 债务 | 缩小不确定性 | 可验证闭环 | 避免过早业务实现 | 总分 |
|---|---:|---:|---:|---:|---:|---:|
| Phase 003：`identity-service` 最小认证与租户模型 | 5 | 4 | 5 | 5 | 4 | 23 |
| Phase 008：人工审批与动作命令 | 2 | 5 | 3 | 3 | 2 | 15 |
| Phase 004：`ticket-service` 最小工单模型 | 3 | 3 | 4 | 4 | 3 | 17 |
| Phase 005：`agent-service` 最小 LangGraph 闭环 | 2 | 4 | 4 | 4 | 2 | 16 |
| Phase 007：RAG 最小知识库闭环 | 1 | 4 | 3 | 4 | 2 | 14 |

## Primary candidate

Primary candidate：Phase 003：`identity-service` 最小认证与租户模型。

选择原因：

- 它是 `07-phase-backlog.md` 中 Phase 002 之后的直接下一步。
- Phase 002 已经冻结认证 API、当前用户、租户上下文、统一错误响应和审计字段草案，Phase 003 可以在契约基础上落地第一个最小业务服务。
- 身份域是后续 `ticket-service`、`workflow-service`、Agent 工具调用、租户隔离、审计和审批边界的共同前提。
- 它不直接实现高风险动作，但会为 DEBT-006 的后续关闭提供权限、租户、审计和身份上下文基础。
- 它能形成可验证闭环：登录、当前用户、租户上下文、权限不足、租户缺失、统一错误响应、单元测试和集成测试。
- 它比直接进入 Agent、RAG 或前端更小，且不会把 AI 工作流、工单事实源或审批动作提前混入身份域。

建议冻结范围：

- 创建 `identity-service` 的最小运行时骨架，但只限身份域。
- 落地租户、用户、角色基础模型和最小权限摘要。
- 实现 `POST /api/auth/login` 与 `GET /api/auth/me`，遵守 `packages/shared-contracts` v1 草案。
- 实现 JWT 或等价 token 机制，并明确 token 载荷、过期、刷新是否进入本阶段。
- 明确租户上下文如何在请求、响应、审计字段和错误响应中表达。
- 提供最小单元测试、集成测试和契约一致性检查。
- 更新必要的本地开发说明和阶段 handoff。

明确非目标：

- 不实现 `ticket-service`、`workflow-service`、`agent-service`、`rag-service` 或 `web-console`。
- 不实现复杂 OAuth2/OIDC、组织架构、细粒度 RBAC 管理界面或多身份提供方。
- 不实现审批实例、动作命令、工单业务、Agent 工具调用、RAG 检索或前端页面。
- 不修改已冻结的 `shared-contracts` 字段、错误码或状态语义；如发现契约缺口，必须先由 Window 1 标注为契约变更提案。

## Fallback candidate

Fallback candidate：Phase 008：人工审批与动作命令。

仅在以下条件下才考虑 fallback：

- 用户明确要求优先处理 DEBT-006：高风险动作审批边界未实现。
- Window 1 能把范围限制为审批与动作命令边界的架构和契约验证，不得直接绕过身份、租户、权限和审计上下文实现高风险动作。

为什么它不是 primary：

- 它距离当前 backlog 顺序较远，正常依赖 identity、ticket、workflow 和 Agent 工具链的上下文。
- 如果跳过 Phase 003，审批记录、操作者身份、租户隔离和权限判断会缺少稳定事实来源。
- 直接做 Phase 008 容易把动作执行能力提前落地，增加越权风险。

## 不选其他阶段的原因

- 不选 Phase 004：工单服务需要先依赖身份认证、当前用户、租户上下文和审计字段，否则租户隔离与操作归因无法验证。
- 不选 Phase 005：Agent 最小闭环依赖身份、工单、工具调用、状态事件和高风险动作边界；当前直接进入会过早实现 AI 业务流。
- 不选 Phase 006：前端工作台需要真实或 Mock API 的身份入口、工单事实和 AI 任务状态；当前先做前端会放大契约未实现带来的假闭环。
- 不选 Phase 007：RAG 仍有 DEBT-004 向量检索方案未决，且知识库闭环不是认证、租户和权限的前置替代。
- 不选 Phase 009 / Phase 010：它们属于可观测性、系统测试、生产化和平台化扩展，距离当前 MVP 最小闭环过远。

## Window 1 的任务边界

Window 1 必须拆清楚以下内容，完成后才能允许 Window 2 实现 Phase 003：

- Contract：`POST /api/auth/login`、`GET /api/auth/me`、统一错误响应、租户上下文、审计字段、token 载荷和权限摘要如何与 `packages/shared-contracts` v1 草案对齐。
- 边界：`identity-service` 只拥有租户、用户、角色、权限和身份审计；不得管理工单、知识库、Agent 状态、审批实例或动作命令。
- 状态生命周期：登录成功、登录失败、token 过期、权限不足、租户缺失、用户禁用等状态如何落地、如何返回错误码、如何审计。
- 数据所有权：用户、租户、角色、权限事实归 `identity-service`；跨服务只传递租户与身份上下文，不让其他 host 复制身份事实。
- 验收条件：最小认证 API 可运行；失败场景使用统一错误码；租户上下文可被后续服务消费；敏感字段不泄露；测试覆盖成功、失败、权限不足和租户缺失。
- 测试策略：单元测试、集成测试、契约一致性检查、禁止越界服务实现扫描、密钥和敏感信息扫描。
- 回滚或降级：如果 token 机制、密码存储、数据库迁移或契约字段存在缺口，Window 1 必须明确最小可接受方案、延期项和阻断条件。

## 需要用户批准的问题

是否批准 Window 0 推荐进入：

Phase 003：`identity-service` 最小认证与租户模型

批准后，Window 1 应基于本决策输出 Phase 003 的阶段架构、契约影响、host 边界、状态生命周期、验收清单和实现任务拆分。Window 0 到此停止。
