# Phase 005 Review

## Review 模式

- 当前 review 模式：初次 Review。
- 当前 phase：Phase 005 `agent-service` 最小 LangGraph 闭环。
- 输出文件：`docs/harness/handoffs/phase-005-review.md`。

## 读取的 handoff 文件

- `docs/harness/handoffs/steering-decision-phase-005.md`
- `docs/harness/handoffs/phase-005-architect.md`
- `docs/harness/handoffs/phase-005-implementation.md`

同时按 Window 3 协议读取了 `docs/harness/00-project-charter.md`、`01-current-architecture.md`、`02-authority-matrix.md`、`03-host-ownership.md`、`04-contract-map.md`、`05-transition-lifetime.md`、`06-debt-register.md`、`08-eval-checklist.md`、`09-window-protocol.md`、`10-steering-state-machine.md` 和 `docs/harness/state/current-state.md`。

## 结论

`require fixes`

不允许进入 Window 4。需要 Window 2 Fix Pass 修复事件记录与事件契约不一致的问题，并补充对应验证。

## Findings

### P1：本地 Agent 事件记录不符合已冻结事件契约

证据：

- `ai-services/agent-service/src/agent_service/store.py:50` 到 `store.py:62` 记录 `agent.run.started.v1` 时，只写入 `event_name`、`tenant_id`、`trace_id` 和 `payload`，缺少事件 schema 要求的 `event_id`、`event_version`、`occurred_at`、`producer`、`consumers`。
- `ai-services/agent-service/src/agent_service/store.py:55` 到 `store.py:61` 的 started payload 缺少 `status`，但 `packages/shared-contracts/events/agent.run.started.v1.schema.json:23` 要求 `status`。
- `ai-services/agent-service/src/agent_service/store.py:69` 到 `store.py:82`、`store.py:88` 到 `store.py:102`、`store.py:133` 到 `store.py:154` 记录 completed、failed 和 step completed 事件时同样缺少顶层事件 envelope 字段。
- `packages/shared-contracts/events/agent.run.started.v1.schema.json:10`、`agent.run.completed.v1.schema.json:10`、`agent.run.failed.v1.schema.json:10`、`agent.step.completed.v1.schema.json:10` 均要求完整事件 envelope。

影响：

- Phase 005 架构要求事件可以落入本地 JSON、内存列表、测试断言或本地 run store，但仍必须确认并冻结 `agent.run.started.v1`、`agent.step.completed.v1`、`agent.run.completed.v1` 和 `agent.run.failed.v1` 的语义。
- 当前实现把本地事件命名为 v1 契约事件，但 payload 无法按 v1 schema 消费，属于 contract 偏离。
- 现有测试只断言事件名存在，未校验事件 envelope，因此验证无法发现该偏离。

修复要求：

- 为本地事件记录补齐契约 envelope：`event_id`、`event_name`、`event_version`、`occurred_at`、`tenant_id`、`trace_id`、`producer`、`consumers`、`payload`。
- `agent.run.started.v1` payload 补齐 `status: "started"`。
- 为 run started、run completed、run failed 和 step completed 至少补充最小契约形状断言；如果可行，直接用 schema 或等价断言覆盖。

## Acceptance 检查

- `agent-service` 最小本地运行时：已满足。
- Planner、Retriever、DataAnalyst、Risk、Supervisor、Report 简化节点：已满足。
- `ticket.search.v1` 只读 Mock 且贯穿 `tenant_id`、`requested_by`、`trace_id`：已满足。
- Mock 知识来源可见：已满足。
- 报告区分事实、推断、风险、建议和引用：已满足。
- 高风险或低置信度进入 `waiting_human` / `waiting_approval`：已满足。
- 工具失败路径包含错误码、`trace_id` 和 `retryable`：已满足。
- Agent run / step 事件契约一致性：未满足，见 P1。
- 未实现审批动作执行、真实 RAG、前端、网关、工单状态变更、自动派单、通知或高风险动作执行：未发现偏离。

## AI Eval 检查

- 最小 eval 样例已覆盖完成路径、工具调用成功、失败路径和人工确认占位。
- eval 输出显示 2 个 case 均通过。
- 引用来源为显式 Mock，报告引用可追踪到 Mock knowledge citation。
- 当前阶段没有真实 RAG，因此不要求真实命中率或引用准确率阈值；Mock 边界说明清晰。

## 验证命令

- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-contracts.ps1`：通过，输出 `Phase 002 contract validation passed.`
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-agent-service.ps1`：通过，3 个 unittest 通过，2 个 eval case 通过。
- `python -m pytest`，工作目录 `ai-services\agent-service`：通过，3 个测试通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-ticket-service.ps1`：通过，7 个 Maven 测试通过。
- 额外抽查本地事件样例：确认 `store.events` 中事件缺少 v1 schema 所需 envelope 字段。

## 是否允许进入 Window 4

不允许。当前结论为 `require fixes`，应回到 Window 2 执行 Fix Pass 1。
