# Phase 005 实现交接

## 模式

- 本窗口模式：初次实现。
- 当前 phase：Phase 005 `agent-service` 最小 LangGraph 闭环。
- 最新架构交接：`docs/harness/handoffs/phase-005-architect.md`。
- 启动前 git baseline：工作区已存在大量 dirty / untracked 文件，本窗口未清理、回滚或 stage 这些既有变更。

## 本窗口修改文件

- `ai-services/agent-service/README.md`
- `ai-services/agent-service/pyproject.toml`
- `ai-services/agent-service/src/agent_service/__init__.py`
- `ai-services/agent-service/src/agent_service/models.py`
- `ai-services/agent-service/src/agent_service/store.py`
- `ai-services/agent-service/src/agent_service/tools.py`
- `ai-services/agent-service/src/agent_service/graph.py`
- `ai-services/agent-service/src/agent_service/eval_runner.py`
- `ai-services/agent-service/src/agent_service/cli.py`
- `ai-services/agent-service/tests/test_agent_graph.py`
- `ai-services/agent-service/evals/complaint-analysis.phase-005.json`
- `packages/shared-contracts/tools/knowledge.search.v1.schema.json`
- `packages/shared-contracts/examples/tools/knowledge.search.success.v1.json`
- `scripts/validate-contracts.ps1`
- `scripts/validate-agent-service.ps1`
- `docs/harness/handoffs/phase-005-implementation.md`

说明：`packages/shared-contracts/tools/knowledge.search.v1.schema.json`、`packages/shared-contracts/examples/tools/knowledge.search.success.v1.json` 和 `scripts/validate-contracts.ps1` 在本窗口开始前已处于 dirty 状态；本窗口只在允许范围内追加 Phase 005 Mock 标记和校验放行。

## 完成的架构验收

- 已新增 `ai-services/agent-service` 最小 Python 本地运行时。
- 已实现简化图节点：Planner、Retriever、DataAnalyst、Risk、Supervisor、Report。
- 已记录本地 `AgentTask`、`AgentRun`、`AgentStep`、`ToolCallRecord`、`ReportDraft` 和 eval 结果。
- `ticket.search.v1` 通过契约化 Mock client 消费，贯穿 `tenant_id`、`requested_by`、`trace_id` 和 `run_id`，保持只读。
- `knowledge.search.v1` 只使用显式 Mock 知识结果，schema、示例、工具记录和报告引用均带 `mock_source` 或 `mock_knowledge` 标记。
- `report.save.v1` 只使用本地 Mock 保存，工具记录标记为 `mock_report_store`。
- 工具失败路径会生成 `DOWNSTREAM_UNAVAILABLE`、`trace_id` 和 `retryable=true`，并记录运行失败事件。
- 高风险或低置信度输出由 Supervisor step 标记为 `waiting_human`，task 最终进入 `waiting_approval` 占位，不生成动作命令。
- 报告区分事实、推断、风险、建议和引用。
- 最小 AI eval 覆盖完成路径、工具调用成功、失败路径和人工确认占位。

## 契约补齐和保持不变

- 非破坏性补齐 `knowledge.search.v1`：
  - 知识条目允许 `mock_source`。
  - citation 允许 `mock_source`。
  - response output 允许整体 `mock_source`。
- 更新 `knowledge.search.success.v1.json`，显式标记当前成功示例为 Mock 来源。
- 更新 `scripts/validate-contracts.ps1`：
  - 要求存在 `scripts/validate-agent-service.ps1`。
  - 检查 `knowledge.search.v1` 的 Phase 005 Mock 标记。
  - 放行 `ai-services/agent-service` 下本阶段允许的 `pyproject.toml` 和 `src` 目录。
- 保持不变：
  - `ticket.search.v1` 仍为只读，`requires_approval=false`。
  - `ticket.create_followup.v1` 未实现、未调用、未新增 adapter。
  - AI task、Agent run、Agent step 状态枚举未改变。
  - Agent run / step 事件名、版本和生产者语义未改变。
  - OpenAPI 路径未被本窗口实现为生产入口。

## 行为变化

- 新增本地命令行闭环：`python -m agent_service.cli --scenario complaint`。
- 新增本地 eval 闭环：`python -m agent_service.cli --scenario eval`。
- 新增 `scripts/validate-agent-service.ps1` 作为 Phase 005 验证入口。
- 这些行为均为本地最小运行时，不代表 FastAPI、网关、SSE、RabbitMQ、真实 RAG、真实报告服务或生产任务入口已完成。

## Mock 边界

- 工单检索：`TicketSearchToolClient` 返回契约化 Mock 数据，标记为 `mock_contract`。
- 知识检索：`MockKnowledgeSearchTool` 返回 Mock snippet 和 Mock citation，标记为 `mock_knowledge` 与 `mock_source=true`。
- 报告保存：`MockReportSaveTool` 返回本地 `report_id`，标记为 `mock_report_store` 与 `mock_local`。
- 模型输出：本阶段没有真实 LLM client，节点逻辑为确定性本地规则。

## 测试和验证结果

- 直接运行 `.\scripts\validate-agent-service.ps1`：失败，原因是当前系统 PowerShell execution policy 禁止直接执行 `.ps1`。
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-agent-service.ps1`：通过，3 个 unittest 通过，2 个 eval case 通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-contracts.ps1`：通过。
- `python -m unittest discover -s tests`，工作目录 `ai-services\agent-service`：通过，3 个测试通过。
- `python -m pytest`，工作目录 `ai-services\agent-service`：通过，3 个测试通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\validate-ticket-service.ps1`：通过，7 个 Maven 测试通过。

说明：本机初始缺少 `pytest`，已在用户批准后执行 `python -m pip install pytest` 安装，以满足架构交接要求的 `python -m pytest` 验证命令。

## Blocker 和遗留风险

- 无实现 blocker。
- 遗留风险：
  - 当前 `agent-service` 是标准库本地运行时，不是生产级 LangGraph/FastAPI 服务。
  - Mock 工具无法证明真实 `ticket-service` HTTP 集成、真实消息队列和真实 RAG 检索质量。
  - PowerShell 默认执行策略会阻止直接运行 `.ps1`，本窗口使用 `-ExecutionPolicy Bypass` 验证。
  - 工作区已有大量既有 dirty / untracked 文件，后续如需提交必须只 stage 本窗口文件，不能使用 `git add .`。

## 是否建议进入 Window 3

建议进入 Window 3 Review/Eval。请重点评审：

- `agent-service` 是否严格停留在本地最小运行时边界。
- Mock 标记是否足够清晰。
- `waiting_human` / `waiting_approval` 是否满足高风险动作阻断要求。
- 本窗口对既有 dirty 契约文件的增量是否仍属于非破坏性补齐。
