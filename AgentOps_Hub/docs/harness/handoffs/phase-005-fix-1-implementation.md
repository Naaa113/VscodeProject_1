# Phase 005 Fix Pass 1 实现交接

## 模式

- 本窗口模式：Fix Pass 1。
- 当前 phase：Phase 005 `agent-service` 最小 LangGraph 闭环。
- 最新架构交接：`docs/harness/handoffs/phase-005-architect.md`。
- 最新 review handoff：`docs/harness/handoffs/phase-005-review.md`。
- Review 结论：`require fixes`。
- 启动前 git baseline：工作区已存在大量 dirty / untracked 文件，本窗口未清理、回滚或 stage 这些既有变更。

## 对应 review finding

### P1：本地 Agent 事件记录不符合已冻结事件契约

Review 指出 `LocalRunStore` 记录 `agent.run.started.v1`、`agent.run.completed.v1`、`agent.run.failed.v1` 和 `agent.step.completed.v1` 时缺少 v1 事件 envelope 字段；同时 `agent.run.started.v1` 的 payload 缺少 `status: "started"`。现有测试只检查事件名，不能发现该契约偏离。

## 本窗口修改文件

- `ai-services/agent-service/src/agent_service/store.py`
- `ai-services/agent-service/tests/test_agent_graph.py`
- `docs/harness/handoffs/phase-005-fix-1-implementation.md`

## 修复方式

- 在 `LocalRunStore` 中新增统一事件记录方法，为本地事件补齐：
  - `event_id`
  - `event_name`
  - `event_version`
  - `occurred_at`
  - `tenant_id`
  - `trace_id`
  - `producer`
  - `consumers`
  - `payload`
- 为 `agent.run.started.v1` payload 补齐 `status: "started"`。
- 将 run started、run completed、run failed 和 step completed 的事件写入统一走同一个 envelope 构造。
- 在 `test_agent_graph.py` 中补充最小契约形状断言，覆盖：
  - `agent.run.started.v1`
  - `agent.run.completed.v1`
  - `agent.run.failed.v1`
  - `agent.step.completed.v1`

## 为什么没有扩大 scope

- 本窗口只修复最新 review handoff 中列出的 P1。
- 未修改 OpenAPI、事件 schema、工具 schema、错误码或状态枚举。
- 未新增 API、工具、adapter、bridge、审批动作、真实 RAG、真实消息队列或外部 LLM 调用。
- 未触碰 `identity-service`、`ticket-service`、前端、网关、workflow、notification 或部署资产。
- 事件仍只写入本地 `LocalRunStore.events`，不宣称真实 RabbitMQ、SSE、Java 任务入口或生产事件总线已完成。

## 完成的 architect acceptance

- Agent run / step 事件现在保留 v1 契约 envelope。
- `agent.run.started.v1` payload 现在包含 `status: "started"`。
- 本地测试覆盖事件 envelope 的核心字段，避免只断言事件名。
- 高风险人工确认、Mock 工具、Mock 知识和报告结构行为未改变。

## 保持不变的 contract

- `ticket.search.v1` 仍为只读语义，未新增写动作。
- `ticket.create_followup.v1` 未实现、未调用。
- `knowledge.search.v1` 的 Mock 标记语义未改变。
- `report.save.v1` 仍只用于本地 Mock 保存。
- AI task、Agent run 和 Agent step 状态枚举未改变。
- 事件 schema 文件未改变，只让本地记录对齐既有 schema。

## 行为变化

- 本地 `store.events` 中的事件由简化字典变为符合 v1 envelope 的事件字典。
- `agent.run.started.v1` payload 新增 `status: "started"`。
- 除本地事件记录形状外，没有新增用户可见功能或业务动作能力。

## 测试和验证结果

- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-contracts.ps1`：通过，输出 `Phase 002 contract validation passed.`。
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-agent-service.ps1`：通过，3 个 unittest 通过，2 个 eval case 通过。
- `python -m pytest`，工作目录 `ai-services\agent-service`：通过，3 个测试通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-ticket-service.ps1`：通过，7 个 Maven 测试通过。

## Blocker 和遗留风险

- 无 blocker。
- 遗留风险：
  - 当前 `agent-service` 仍是本地最小运行时，不是生产级 LangGraph、FastAPI、SSE 或消息队列集成。
  - 事件契约通过本地断言覆盖核心 shape，尚未引入 JSON Schema 校验依赖。
  - 工作区仍有大量既有 dirty / untracked 文件；后续如需提交必须只 stage 本窗口触碰文件，不能使用 `git add .`。

## 是否需要重新评审

需要重新进入 Window 3 Review/Eval，重点复核 P1 是否关闭，以及本地事件记录是否已经与 v1 事件 envelope 对齐。
