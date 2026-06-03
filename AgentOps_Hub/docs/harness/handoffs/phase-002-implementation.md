# Phase 002 实现交接

## 模式

初次实现。

## 恢复情况

- 最新未 final 的阶段为 Phase 002。
- 当前阶段由 `docs/harness/state/current-state.md` 和 `docs/harness/handoffs/phase-002-architect.md` 恢复确认。
- 实现开始时，`docs/harness/handoffs/phase-002-implementation.md` 尚不存在。
- 实现开始时，不存在 Phase 002 的 review handoff。

## Git 基线

修改文件前已运行 `git status --short --untracked-files=all`，当时工作区已有脏文件和未跟踪文件，包括：

- 已删除的同级目录文件：`../demo_bailian_memory.py`、`../server.py`。
- 本工作区内已存在的未跟踪 Phase 001 文件和 harness 文件。
- 已存在的未跟踪同级目录：`../harness`、`../test`。
- 已存在的未跟踪 Phase 002 steering 与 architect 文件。

本窗口只触碰下方列出的 Phase 002 允许范围文件。

## 修改文件

共享契约资产：

- `packages/shared-contracts/README.md`
- `packages/shared-contracts/manifest.v1.json`
- `packages/shared-contracts/openapi/agentops-api.v1.yaml`
- `packages/shared-contracts/schemas/common.v1.schema.json`
- `packages/shared-contracts/schemas/status.v1.schema.json`
- `packages/shared-contracts/errors/error-codes.v1.json`
- `packages/shared-contracts/events/ai.task.created.v1.schema.json`
- `packages/shared-contracts/events/agent.run.started.v1.schema.json`
- `packages/shared-contracts/events/agent.step.completed.v1.schema.json`
- `packages/shared-contracts/events/agent.run.completed.v1.schema.json`
- `packages/shared-contracts/events/agent.run.failed.v1.schema.json`
- `packages/shared-contracts/events/document.uploaded.v1.schema.json`
- `packages/shared-contracts/events/document.indexed.v1.schema.json`
- `packages/shared-contracts/tools/ticket.search.v1.schema.json`
- `packages/shared-contracts/tools/ticket.create_followup.v1.schema.json`
- `packages/shared-contracts/tools/knowledge.search.v1.schema.json`
- `packages/shared-contracts/tools/report.save.v1.schema.json`
- `packages/shared-contracts/examples/openapi/*.v1.json`
- `packages/shared-contracts/examples/events/*.v1.json`
- `packages/shared-contracts/examples/tools/*.v1.json`

校验与文档：

- `scripts/validate-contracts.ps1`
- `tests/contract/README.md`
- `docs/development/local-dev.md`
- `docs/adr/0004-vector-search.md`

实现交接：

- `docs/harness/handoffs/phase-002-implementation.md`

## 已完成的架构验收项

- 新增 OpenAPI v1 草案，覆盖认证、工单、AI 任务、知识库文档、知识库检索和 SSE 任务状态流路径。
- 新增统一错误响应形状、错误码目录、租户上下文、审计字段、分页、追踪字段、事件信封和工具信封 schema。
- 新增 AI task、Agent run、Agent step、approval instance、action command、document parse status 的生命周期状态枚举。
- 新增 7 个 v1 事件 schema，包含事件信封字段、payload、生产者、消费者和兼容性元数据。
- 新增 4 个 v1 工具 schema，包含输入、输出、权限、审批、幂等、审计、超时和重试元数据。
- 新增可解析的 OpenAPI、事件和工具成功/失败示例。
- 新增 `manifest.v1.json`，作为契约清单和兼容性策略记录。
- 新增本地只读契约校验命令。
- 保持知识检索契约与向量存储无关；`index_ref`、`document_id` 和 `chunk_id` 是契约字段，但不选择 pgvector 或 Milvus。
- 保持 `ticket.create_followup.v1` 位于审批或策略检查语义之后；不允许 Agent 直接执行高风险动作。

## 保持不变的契约

- Phase 002 只冻结契约草案；不声明任何服务已经实现这些 API、事件或工具。
- 既有 host ownership 保持不变：`packages/shared-contracts` 只拥有契约定义，不拥有业务事实。
- 未修改 `04-contract-map.md` 和 `05-transition-lifetime.md`。
- 未新增运行时 helper、adapter、fallback、bridge、mock server、generated client、服务实现、数据库迁移、前端页面、Agent workflow、RAG 实现或模型接入。

## 行为变化

- 新增本地命令：`powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1`。
- 未引入任何业务运行时行为变化。

## 测试与验证结果

实现过程中已验证：

- `powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1` 通过。
- 在 `apps`、`services`、`ai-services`、`packages` 和 `tests` 下执行禁止实现产物扫描，未返回任何文件。

本 handoff 写入后已运行最终必需验证命令：

- `git status --short`：显示实现前已存在的同级目录脏文件和未跟踪工作区根目录。
- `Get-ChildItem -Recurse packages/shared-contracts -Force`：列出 OpenAPI、schema、error、event、tool、example 和 manifest 文件。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1`：通过。
- 在 `apps`、`services`、`ai-services`、`packages` 和 `tests` 下执行禁止实现产物扫描：未返回任何文件。

## 阻断项与遗留风险

- 未遇到阻断项。
- 遗留风险：本 Window 2 启动前已有大量 Phase 001 和 harness 文件处于未跟踪状态。提交时必须继续只 stage 本 Phase 002 实现触碰的文件。
