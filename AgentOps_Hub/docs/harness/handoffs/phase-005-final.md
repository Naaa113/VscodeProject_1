# Phase 005 最终交接

## phase status（阶段状态）

`completed`。

Phase 005 `agent-service` 最小 LangGraph 闭环已完成，并已通过 Window 3 Fix Pass 1 复审。最新 review 文件为 `docs/harness/handoffs/phase-005-review-fix-1.md`，结论为 `approve`。本阶段没有用户接受 residual risk 的例外裁决；剩余问题按债务登记保留，不视为本阶段阻断项。

## completed scope（完成范围）

- 新增 `ai-services/agent-service` 最小 Python Agent 本地运行时。
- 实现投诉分析样例任务闭环，包含 Planner、Retriever、Data Analyst、Risk、Supervisor、Report 简化节点。
- 记录 `AgentTask`、`AgentRun`、`AgentStep`、`ToolCallRecord`、`ReportDraft` 和 eval 结果。
- 使用契约化 Mock 消费 `ticket.search.v1`，保持只读语义，并贯穿 `tenant_id`、`requested_by`、`trace_id` 和 `run_id`。
- 使用显式 Mock 的 `knowledge.search.v1` 知识结果，报告引用和 eval 中均可识别 Mock 来源。
- 使用本地 Mock `report.save.v1` 保存报告草稿，不宣称生产报告服务完成。
- 报告区分事实、推断、风险、建议和引用。
- 高风险或低置信度输出进入 `waiting_human` / `waiting_approval` 占位，不生成审批实例、动作命令或跟进任务。
- Fix Pass 1 已补齐本地 Agent run / step 事件 v1 envelope，并为 `agent.run.started.v1` payload 补齐 `status: "started"`。

## unchanged contracts（未改变契约）

- `ticket.search.v1` 仍为只读工具，`requires_approval=false`。
- `ticket.create_followup.v1` 未实现、未调用、未新增 adapter、bridge 或 fallback。
- AI task、Agent run、Agent step 的状态枚举未改变。
- 工单 API、身份 API、工单状态变更、审批动作、通知、网关、前端和真实 RAG 契约未被扩展为已实现能力。
- OpenAPI 中的 AI task 路径仍是未来 Java 任务入口或网关契约，本阶段没有宣称生产 HTTP 入口完成。

## changed contracts / authority / transition（契约、权威与生命周期变化）

- 契约：`knowledge.search.v1` 非破坏性补齐 Mock 来源标记，`knowledge.search.success.v1.json` 明确标记 Mock 示例。
- 契约：`scripts/validate-contracts.ps1` 增加 Phase 005 Mock 标记检查和 `scripts/validate-agent-service.ps1` 存在性检查。
- 契约：本地 Agent 事件记录对齐 v1 envelope，覆盖 `event_id`、`event_name`、`event_version`、`occurred_at`、`tenant_id`、`trace_id`、`producer`、`consumers` 和 `payload`。
- 权威：`agent-service` 当前只拥有 Agent 编排状态、图执行过程、节点摘要、工具调用记录、Mock 模型逻辑、报告草稿和 eval 样例；不拥有身份、工单、知识库、审批或动作事实。
- 生命周期：Phase 005 已落地本地 AI task、Agent run、Agent step、工具调用、报告生成和人工确认占位生命周期。
- 行为：新增本地命令行闭环和 eval 闭环，但不代表 FastAPI、LangGraph 生产运行时、RabbitMQ、SSE、真实外部模型、真实 RAG 或生产报告服务完成。

## validation summary（验证摘要）

Window 2 初次实现验证：

- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-contracts.ps1`：通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-agent-service.ps1`：通过，3 个 unittest 通过，2 个 eval case 通过。
- `python -m pytest`，工作目录 `ai-services\agent-service`：通过，3 个测试通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-ticket-service.ps1`：通过，7 个 Maven 测试通过。

Window 3 初审结论为 `require fixes`，发现本地 Agent 事件记录缺少 v1 envelope。Fix Pass 1 修复后复审验证通过：

- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-contracts.ps1`：通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-agent-service.ps1`：通过，3 个 unittest 通过，2 个 eval case 通过。
- `python -m pytest`，工作目录 `ai-services\agent-service`：通过，3 个测试通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-ticket-service.ps1`：通过，7 个 Maven 测试通过。

复审备注：本地未安装 `jsonschema` Python 包，因此未运行完整 JSON Schema 校验；本轮通过代码审查和新增测试断言复核事件 envelope 核心字段。

## remaining debt（剩余债务）

- DEBT-004：向量检索方案仍 deferred，进入 RAG 阶段前必须收敛。
- DEBT-006：高风险动作审批边界仍为 D0；Phase 005 只做 `waiting_human` / `waiting_approval` 阻断占位，未实现审批实例或动作命令。
- DEBT-007：Phase 005 已提供最小 eval 样例，但仍缺真实 RAG / Agent 基准集。
- DEBT-011：`identity-service` 仍缺生产级数据初始化、PostgreSQL 迁移、refresh token、集中式 token 撤销和完整 RBAC 管理 API。
- DEBT-012：`ticket-service` 仍缺生产级 Spring Boot、PostgreSQL 迁移、服务间鉴权、受控样例数据初始化、数据库分页索引、工单状态变更审计和完整 SLA 引擎。
- DEBT-013：`agent-service` 仍缺生产级 LangGraph / FastAPI、真实模型适配、真实 HTTP 工具调用、消息队列 / SSE 集成、持久化存储和完整 JSON Schema 事件校验。

## latest state for Window 0（Window 0 最新状态）

Window 0 下一次启动时应自动发现：

- 最新 final handoff：`docs/harness/handoffs/phase-005-final.md`。
- 当前 Steering 状态：`handoff_done`。
- 最新 Review 结论：`docs/harness/handoffs/phase-005-review-fix-1.md`，结论为 `approve`。
- Phase 005 已完成，当前仓库具备身份域、工单域和 Agent 编排域的最小本地运行时。
- `current-state.md` 已更新为 Phase 005 完成态。
- 下一阶段尚未冻结；不得绕过 Window 0 直接进入实现。

## recommended candidate inputs for Window 0（推荐候选输入）

推荐 primary candidate：Phase 006 `web-console` 最小工作台。

候选输入：

- Phase 003 已提供身份上下文、租户校验、权限摘要和身份审计基础。
- Phase 004 已提供客户与工单事实源、工单查询 API、详情 API、`ticket.search.v1` 只读契约、租户隔离和工单审计字段。
- Phase 005 已提供 Agent 本地闭环、Agent run / step 状态、显式 Mock 工具与知识来源、结构化报告、人工确认占位和最小 eval 样例。
- Phase 006 应限定为登录或 Mock 登录联调、工单列表 / 详情、AI 任务发起、Agent 执行链路展示、报告预览、错误 / 加载 / 空状态。
- Phase 006 不应实现真实 RAG、审批动作执行、生产网关鉴权、消息队列、工单状态变更、自动派单、自动通知或高风险动作执行。

Fallback candidate：Phase 008 人工审批与动作命令。只有用户明确要求优先收敛 DEBT-006 时，Window 0 才应评估 fallback；即便选择 fallback，也不得绕过身份、租户、权限、工单事实源、Agent 风险标记和审计上下文。

## files changed in this handoff（本窗口修改文件）

- `docs/harness/state/current-state.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-005-final.md`

## 提交状态

用户未明确要求 commit。本窗口未 stage、未提交、未清理既有 dirty / untracked 变更。
