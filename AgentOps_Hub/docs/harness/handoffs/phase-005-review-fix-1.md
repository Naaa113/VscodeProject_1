# Phase 005 Fix Pass 1 复审

## Review 模式

- 当前 review 模式：Re-review fix 1。
- 当前 phase：Phase 005 `agent-service` 最小 LangGraph 闭环。
- 输出文件：`docs/harness/handoffs/phase-005-review-fix-1.md`。

## 读取的 handoff 文件

- `docs/harness/handoffs/steering-decision-phase-005.md`
- `docs/harness/handoffs/phase-005-architect.md`
- `docs/harness/handoffs/phase-005-implementation.md`
- `docs/harness/handoffs/phase-005-review.md`
- `docs/harness/handoffs/phase-005-fix-1-implementation.md`

同时按 Window 3 协议读取了 `docs/harness/00-project-charter.md`、`01-current-architecture.md`、`02-authority-matrix.md`、`03-host-ownership.md`、`04-contract-map.md`、`05-transition-lifetime.md`、`06-debt-register.md`、`08-eval-checklist.md`、`09-window-protocol.md`、`10-steering-state-machine.md` 和 `docs/harness/state/current-state.md`。

## 结论

`approve`

允许进入 Window 4。Fix Pass 1 已关闭初次 review 的 P1，未发现新的 belongs、authority、contract、transition 或 behavior 阻断问题。

## 上次 finding 关闭情况

### P1：本地 Agent 事件记录不符合已冻结事件契约

状态：已关闭。

证据：

- `ai-services/agent-service/src/agent_service/store.py:30` 统一通过 `_record_event` 写入事件 envelope，包含 `event_id`、`event_name`、`event_version`、`occurred_at`、`tenant_id`、`trace_id`、`producer`、`consumers` 和 `payload`。
- `ai-services/agent-service/src/agent_service/store.py:62` 写入 `agent.run.started.v1`，payload 已包含 `status: "started"`。
- `ai-services/agent-service/src/agent_service/store.py:79`、`store.py:96` 和 `store.py:137` 分别为 run completed、run failed 和 step completed 复用同一 envelope 构造。
- `ai-services/agent-service/tests/test_agent_graph.py:15` 增加事件 envelope 断言，`test_agent_graph.py:54`、`test_agent_graph.py:59`、`test_agent_graph.py:64` 和 `test_agent_graph.py:93` 覆盖 run started、run completed、step completed 和 run failed。
- `packages/shared-contracts/events/agent.run.started.v1.schema.json:10`、`agent.run.completed.v1.schema.json:10`、`agent.run.failed.v1.schema.json:10` 和 `agent.step.completed.v1.schema.json:10` 要求的顶层 envelope 已由本地记录满足。

## Findings

无阻断 finding。

## Acceptance 检查

- Window 1 要求的 `agent-service` 最小本地运行时：满足。
- Planner、Retriever、DataAnalyst、Risk、Supervisor、Report 简化节点：满足。
- `ticket.search.v1` 只读 Mock、租户上下文、`requested_by`、`trace_id` 贯穿：满足。
- Mock 知识来源在工具记录、报告引用和 eval 中可见：满足。
- 报告区分事实、推断、风险、建议和引用：满足。
- 高风险或低置信度进入 `waiting_human` / `waiting_approval` 占位：满足。
- 工具失败、节点失败和运行失败包含错误码、`trace_id` 和 `retryable`：满足。
- Agent run / step 事件契约一致性：满足，Fix Pass 1 已补齐 envelope 与 started 状态。
- 未实现审批动作执行、真实 RAG、前端、网关、工单状态变更、自动派单、通知或高风险动作执行：未发现偏离。

## AI Eval 检查

- 最小 eval 样例覆盖工作流完成、工具调用成功、失败路径和人工确认占位。
- eval 输出中 2 个 case 均通过。
- 引用来源仍为显式 Mock，报告引用可追踪到 Mock knowledge citation。
- 当前阶段没有真实 RAG，不要求真实命中率或引用准确率阈值；Mock 边界保持清晰。

## 验证命令

- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-contracts.ps1`：通过，输出 `Phase 002 contract validation passed.`。
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-agent-service.ps1`：通过，3 个 unittest 通过，2 个 eval case 通过。
- `python -m pytest`，工作目录 `ai-services\agent-service`：通过，3 个测试通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-ticket-service.ps1`：通过，7 个 Maven 测试通过。
- 额外检查 `jsonschema` Python 包：本地未安装，因此未运行完整 JSON Schema 校验；本轮通过代码审查和新增测试断言复核 envelope 核心字段。

## 是否允许进入 Window 4

允许。当前结论为 `approve`，可进入 Window 4 Handoff。

## 备注

工作区仍包含大量既有 dirty / untracked 文件；本复审没有清理、回滚、stage 或提交这些变更。后续如需提交，仍应只 stage 当前阶段相关文件。
