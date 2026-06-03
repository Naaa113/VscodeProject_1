# Phase 002 Fix 1 实现交接

## 模式

Fix Pass：fix-1。

## 最新 review handoff

- `docs/harness/handoffs/phase-002-review.md`
- 结论：`require fixes`

## 恢复情况

- Phase 002 已存在初次实现 handoff：`docs/harness/handoffs/phase-002-implementation.md`。
- Phase 002 尚不存在 final handoff。
- 当前 fix 编号为 `fix-1`，因为本阶段此前不存在 `phase-002-fix-*-implementation.md`。
- 本次只修复最新 review handoff 中列出的两个 P1 finding。

## Git 基线

修改前已运行 `git status --short --untracked-files=all`。本窗口开始前已有脏文件和未跟踪文件，包括：

- `docs/harness/handoffs/phase-002-implementation.md` 已有未提交修改；这是此前将 handoff 正文改为中文留下的状态，本 fix pass 未继续修改该文件。
- 已删除的同级目录文件：`../demo_bailian_memory.py`、`../server.py`。
- 大量 Phase 001、harness、同级 `../test` 文件仍处于未跟踪状态。
- `docs/harness/handoffs/phase-002-review.md` 已存在但未跟踪，作为本次 fix pass 的输入 handoff。

## 对应 review finding 与修复方式

### Finding 1：`current-state.md` 未推进到实现完成，窗口恢复状态与实际 handoff 冲突

修复文件：

- `docs/harness/state/current-state.md`

修复方式：

- 将 `Steering state` 更新为 `implementation_done`。
- 将架构状态更新为 Phase 002 契约实现已完成并进入 fix-1。
- 将仓库事实更新为已包含 Phase 002 OpenAPI、事件 schema、工具 schema、错误码、状态枚举、示例、清单和契约校验脚本。
- 将下一步更新为进入 Window 3 对 `phase-002-fix-1-implementation.md` 进行复审。

为什么没有扩大 scope：

- 修改只限于 review finding 明确要求的 `current-state.md` 状态恢复事实。
- 未修改项目章程、权威矩阵、host ownership、contract map、transition lifetime 或 backlog。
- 未选择下一阶段。

### Finding 2：工具和 Agent 步骤失败契约未强制携带可追踪错误信息

修复文件：

- `packages/shared-contracts/errors/error-codes.v1.json`
- `packages/shared-contracts/events/agent.step.completed.v1.schema.json`
- `packages/shared-contracts/tools/ticket.search.v1.schema.json`
- `packages/shared-contracts/tools/ticket.create_followup.v1.schema.json`
- `packages/shared-contracts/tools/knowledge.search.v1.schema.json`
- `packages/shared-contracts/tools/report.save.v1.schema.json`
- `packages/shared-contracts/examples/tools/knowledge.search.failure.v1.json`
- `scripts/validate-contracts.ps1`

修复方式：

- 在错误码目录中新增 `DOWNSTREAM_UNAVAILABLE`，覆盖工具失败中的下游不可用分类。
- 在 4 个工具 schema 的 response 中为 `failed` 状态增加条件约束：失败时必须携带统一错误形状，至少包含 `error_code`、`message`、`trace_id`、`retryable`。
- 保留成功或等待审批场景中 `error = null` 的表达。
- 在 `agent.step.completed.v1` 中为 `failed` 状态增加条件约束：失败时必须携带 `error_code`、`message` 和 `retryable`。
- 新增 `knowledge.search.failure.v1.json` 示例，展示 `DOWNSTREAM_UNAVAILABLE` 失败分类。
- 增强 `scripts/validate-contracts.ps1`，校验工具失败统一错误字段、禁止宽泛 nullable object 错误形状、校验必要失败分类错误码，并校验 Agent step 失败状态约束。

为什么没有扩大 scope：

- 修改只影响 Phase 002 已批准的契约资产和校验脚本。
- 未新增运行时 helper、adapter、fallback、bridge、mock server、服务实现、前端页面、数据库迁移、Agent workflow、RAG 实现或模型接入。
- 未新增 review finding 之外的功能。

## 修改文件

- `docs/harness/state/current-state.md`
- `packages/shared-contracts/errors/error-codes.v1.json`
- `packages/shared-contracts/events/agent.step.completed.v1.schema.json`
- `packages/shared-contracts/tools/ticket.search.v1.schema.json`
- `packages/shared-contracts/tools/ticket.create_followup.v1.schema.json`
- `packages/shared-contracts/tools/knowledge.search.v1.schema.json`
- `packages/shared-contracts/tools/report.save.v1.schema.json`
- `packages/shared-contracts/examples/tools/knowledge.search.failure.v1.json`
- `scripts/validate-contracts.ps1`
- `docs/harness/handoffs/phase-002-fix-1-implementation.md`

## 已完成的 architect acceptance

- 保持 OpenAPI、事件、工具、错误码、状态枚举、示例和校验入口仍位于 `packages/shared-contracts` 与允许脚本范围内。
- 统一错误响应字段在工具失败分支中被强制表达。
- Agent step 的失败状态被约束为必须携带错误码、错误信息和可重试标记。
- 工具失败分类覆盖权限失败、审批缺失、校验失败、下游不可用和未知错误。
- 本地契约校验脚本可检测上述失败契约约束。

## 保持不变的 contract

- 未改变已冻结的 URL、API 路径、事件名称、工具名称和状态枚举值。
- 未改变 `ticket.create_followup.v1` 需要审批或策略检查的约束。
- 未改变知识检索契约的存储无关设计。
- 未修改 `04-contract-map.md` 或 `05-transition-lifetime.md`。

## 行为变化

- 无业务运行时行为变化。
- 契约校验更严格：空泛工具失败 `error` object 不再能通过本地校验。

## 测试与验证结果

预检已通过：

- `powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1`
- 禁止实现产物扫描：未返回任何文件。

本 handoff 写入后已运行 Phase 002 architect handoff 要求的完整验证命令：

- `git status --short`：显示本 fix pass 修改的契约文件、脚本、既有中文 handoff 修改，以及实现前已存在的未跟踪/同级目录脏状态。
- `Get-ChildItem -Recurse packages/shared-contracts -Force`：列出 OpenAPI、schemas、errors、events、tools、examples、manifest 和既有 `.gitkeep` 文件。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1`：通过。
- 禁止实现产物扫描：未返回任何文件。

## 阻断项与遗留风险

- 未遇到阻断项。
- 遗留风险：工作区仍有大量本 fix pass 开始前已存在的未跟踪文件和同级目录脏状态；后续如需提交，必须只 stage 本 fix pass 实际触碰的文件。

## 是否需要重新评审

需要。请 Window 3 基于 `docs/harness/handoffs/phase-002-review.md` 和本文件写入 `phase-002-review-fix-1.md` 进行复审。
