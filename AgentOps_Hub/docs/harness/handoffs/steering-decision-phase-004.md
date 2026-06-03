# Phase 004 Steering 决策

## 决策状态

已由用户在 2026-06-01 批准。Window 0 已选择 Phase 004，并在进入 Window 1 前停止。

## 当前状态摘要

- 状态来源：`docs/harness/state/current-state.md`
- 当前阶段：Phase 003 `identity-service` 最小认证与租户模型，状态为 `completed`
- Steering 状态：`handoff_done`
- 最新 final handoff：`docs/harness/handoffs/phase-003-final.md`
- 最新复审：`docs/harness/handoffs/phase-003-review-fix-1.md`，结论为 `approve`
- 架构状态：首个身份域最小运行时已落地，其他业务域仍未创建运行时
- 业务代码状态：`services/identity-service` 已具备最小 Java 身份服务；`ticket-service`、`workflow-service`、`agent-service`、`rag-service`、`web-console` 和 `api-gateway` 仍未落地业务实现
- 当前契约事实：`packages/shared-contracts` v1 草案仍是共同语言；Phase 003 只补齐认证相关 OpenAPI 和示例，未新增事件或 Agent 工具
- 当前关键债务：DEBT-006 高风险动作审批边界仍为 D0；DEBT-004、DEBT-007 为后续 RAG / Agent 相关 D1；DEBT-011 为身份服务生产化增强 D2

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
- `steering-decision-phase-003.md`
- `phase-003-architect.md`
- `phase-003-implementation.md`
- `phase-003-review.md`
- `phase-003-fix-1-implementation.md`
- `phase-003-review-fix-1.md`
- `phase-003-final.md`

最新 completed phase：

- Phase 003：`identity-service` 最小认证与租户模型

当前 active phase：

- 无。Phase 003 已完成，状态为 `handoff_done`。

当前 blocked phase：

- 无。

Phase 004 对应文件检查：

- `steering-decision-phase-004.md`：本文件创建前不存在
- `phase-004-architect.md`：不存在，符合尚未进入 Window 1 的状态
- `phase-004-implementation.md`：不存在
- `phase-004-review.md`：不存在
- `phase-004-final.md`：不存在

阻断判断：

- 不阻断。`current-state.md` 和 `phase-003-final.md` 都要求 Window 0 从 Phase 004 或其他候选重新决策。
- 不应回退使用 bootstrap 推荐 Phase 001，因为 Phase 001、Phase 002 和 Phase 003 均已有 final handoff。

## 候选阶段评分表

评分范围：1 分为弱匹配，5 分为强匹配。

| 候选阶段 | 是否是 backlog 下一步 | 解除当前 D0/D1 债务 | 缩小不确定性 | 可验证闭环 | 避免过早业务实现 | 总分 |
|---|---:|---:|---:|---:|---:|---:|
| Phase 004：`ticket-service` 最小工单模型 | 5 | 4 | 5 | 5 | 4 | 23 |
| Phase 008：人工审批与动作命令 | 2 | 5 | 3 | 3 | 2 | 15 |
| Phase 005：`agent-service` 最小 LangGraph 闭环 | 3 | 4 | 4 | 4 | 2 | 17 |
| Phase 006：`web-console` 最小工作台 | 2 | 2 | 3 | 4 | 3 | 14 |
| Phase 007：RAG 最小知识库闭环 | 1 | 4 | 3 | 4 | 2 | 14 |

## Primary candidate

Primary candidate：Phase 004：`ticket-service` 最小工单模型。

选择原因：

- 它是 `07-phase-backlog.md` 中 Phase 003 之后的直接下一步。
- Phase 003 已提供当前用户、租户上下文、权限摘要、统一错误响应和身份审计基础，Phase 004 可以在此基础上验证第一个真实业务事实域的租户隔离和审计归因。
- 工单是后续 Agent 投诉分析、工单检索工具、前端列表详情、报告引用和动作命令的业务事实底座；先落地最小工单模型能缩小后续 Agent / 前端 / 审批阶段的不确定性。
- 它能形成可验证闭环：创建客户与工单、按时间范围 / 分类 / 优先级 / 状态查询、查看详情、校验租户隔离、写入审计字段、执行契约测试。
- 它不直接实现 Agent 工作流或高风险动作执行，但可以为 DEBT-006 后续关闭提供被审批动作要作用的业务对象和审计字段。
- 它比直接进入 Agent、RAG、前端或审批更贴近当前依赖顺序，不会把 AI 推理、人工审批或 UI 假闭环提前混入工单域。

建议冻结范围：

- 创建 `services/ticket-service` 的最小 Java 工单服务运行时，范围只限工单域。
- 落地客户和工单基础模型，至少覆盖客户标识、工单标题、描述、分类、状态、优先级、SLA 字段、租户 ID、创建人、更新时间和审计字段。
- 实现 `GET /api/tickets`、`POST /api/tickets`、`GET /api/tickets/{id}`，遵守 `packages/shared-contracts` v1 草案；如发现契约缺口，必须由 Window 1 先提出兼容契约变更。
- 实现 Agent 工具可用的工单检索语义，但本阶段只落地工单服务 API 与必要契约，不实现 `agent-service` 或真实 Agent 工具调用。
- 复用 Phase 003 的身份上下文语义：`Authorization: Bearer <token>`、`X-Tenant-Id`、`X-Trace-Id`、当前用户 ID、租户隔离和统一错误响应。
- 提供单元测试、集成测试、契约测试和本地验证脚本，例如 `scripts/validate-ticket-service.ps1`。
- 更新必要的本地开发说明和阶段 handoff。

明确非目标：

- 不实现复杂自动派单、BI 分析、客服队列分配、通知发送或完整 SLA 引擎。
- 不实现 `workflow-service`、`agent-service`、`rag-service`、`web-console`、`api-gateway` 或通知服务运行时。
- 不实现审批实例、动作命令、高风险动作执行、真实 Agent 工具调用、RAG 检索、前端页面或 SSE 推送。
- 不把 `ticket-service` 变成身份事实 owner；它只能消费身份上下文和用户 ID。
- 不修改认证契约语义，不复制 `identity-service` 的用户、角色、权限、密码或 token 事实表。

## Fallback candidate

Fallback candidate：Phase 008：人工审批与动作命令。

仅在以下条件下才考虑 fallback：

- 用户明确要求优先处理 DEBT-006：高风险动作审批边界未实现。
- Window 1 能把范围限制为审批实例、动作命令、幂等键和审计边界的架构与契约，不直接落地高风险动作执行能力。
- Window 1 明确说明在缺少 `ticket-service` 最小事实源时，审批对象、操作者身份、租户隔离和动作目标如何不被伪造或悬空。

为什么它不是 primary：

- 它距离当前 backlog 顺序较远，正常依赖身份、工单、Agent 工具链和动作目标的上下文。
- DEBT-006 虽是 D0，但 Phase 004 可以先提供工单事实源和审计字段，为后续审批边界落地提供必要对象。
- 直接进入 Phase 008 容易把动作命令和审批执行提前落地，增加越权和假闭环风险。

## 不选其他阶段的原因

- 不选 Phase 005：Agent 最小闭环依赖工单检索对象、工具调用语义、任务状态和高风险动作边界；当前直接进入会过早实现 AI 业务流。
- 不选 Phase 006：前端工作台需要真实或 Mock API 的身份入口、工单事实和 AI 任务状态；当前先做前端会放大契约未实现带来的假闭环。
- 不选 Phase 007：RAG 仍有 DEBT-004 向量检索方案未决，且知识库闭环不是工单事实源的前置替代。
- 不选 Phase 009：可观测性与系统测试需要更完整的 MVP 链路，当前还缺工单、Agent、前端和 RAG。
- 不选 Phase 010：企业级扩展属于 MVP 之后的平台化能力，距离当前阶段过远。
- 不选身份生产化增强：DEBT-011 当前为 D2，Phase 003 已足够支撑 Phase 004 的最小身份上下文；生产级迁移、refresh token、集中式 token 撤销和完整 RBAC 管理 API 不应阻断工单事实源落地。

## Window 1 的任务边界

Window 1 必须拆清楚以下内容，完成后才能允许 Window 2 实现 Phase 004：

- Contract：`GET /api/tickets`、`POST /api/tickets`、`GET /api/tickets/{id}`、分页、过滤、排序、统一错误响应、租户上下文、创建人 / 更新人审计字段、工单状态、优先级、分类、SLA 字段如何与 `packages/shared-contracts` v1 草案对齐。
- 工具契约：`ticket.search.v1` 是否已有足够输入 / 输出 schema；若缺少字段，只允许提出兼容变更，不允许实现窗口私自漂移。
- 边界：`ticket-service` 只拥有客户、工单、工单状态、SLA 字段和工单审计；不得拥有身份事实、Agent run/step、审批实例、动作命令、知识库文档或通知策略。
- 状态生命周期：工单创建、查询、详情读取、状态初始值、状态变更是否进入本阶段、失败时如何落盘、错误码如何返回、审计如何关联。
- 数据所有权：客户和工单事实归 `ticket-service`；用户、租户、角色和权限事实归 `identity-service`；Agent 只能通过契约化 API 或工具查询工单。
- 租户隔离：所有列表、详情和创建路径必须显式验证 `tenant_id`，跨租户访问不得泄露工单是否存在。
- 验收条件：可创建样例客户与工单；可按时间范围、分类、优先级、状态查询；详情接口稳定；统一错误响应稳定；租户隔离和审计字段有测试；契约校验通过。
- 测试策略：单元测试、集成测试、契约一致性检查、越界服务实现扫描、敏感信息检查、本地验证脚本。
- 回滚或降级：如果身份服务联调、契约字段、数据库初始化、SLA 语义或状态流转存在缺口，Window 1 必须明确最小可接受方案、延期项和阻断条件。

## 需要用户批准的问题

是否批准 Window 0 推荐进入：

Phase 004：`ticket-service` 最小工单模型

批准后，Window 1 应基于本决策输出 Phase 004 的阶段架构、契约影响、host 边界、状态生命周期、验收清单和实现任务拆分。Window 0 到此停止。
