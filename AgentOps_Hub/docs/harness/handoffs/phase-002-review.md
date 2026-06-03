# Phase 002 评审交接

## 评审结论

require fixes

当前不允许进入 Window 4。需要回到 Window 2 做 Fix Pass。

## 当前评审模式

初次 Review。

恢复依据：

- `docs/harness/handoffs/phase-002-implementation.md` 已存在。
- `docs/harness/handoffs/phase-002-final.md` 不存在。
- 本次评审开始前 `docs/harness/handoffs/phase-002-review.md` 不存在。
- 未发现 Phase 002 的 fix implementation 或 review-fix handoff。

## 已读取文件

- `docs/harness/00-project-charter.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-002.md`
- `docs/harness/handoffs/phase-002-architect.md`
- `docs/harness/handoffs/phase-002-implementation.md`
- Phase 002 修改范围内的 `packages/shared-contracts/**`、`scripts/validate-contracts.ps1`、`tests/contract/README.md`、`docs/development/local-dev.md`、`docs/adr/0004-vector-search.md`

## 问题清单

### P1 - `current-state.md` 未推进到实现完成，窗口恢复状态与实际 handoff 冲突

`docs/harness/state/current-state.md` 仍记录 Phase 002 的 Steering 状态为 `phase_selected`，架构状态仍是“待 Window 1 架构拆解”，并且下一步仍提示进入 Window 1。但仓库已经存在 `phase-002-architect.md` 和 `phase-002-implementation.md`，实现交接也记录了已完成契约实现与验证。这个状态会误导后续窗口恢复和治理判断，违反通用验收中“是否更新了 `state/current-state.md`”的要求，也不符合状态机中 `implementation_done` 才交给 Review/Eval 的语义。

证据：

- `docs/harness/state/current-state.md:8` 仍为 `Steering state: phase_selected`。
- `docs/harness/state/current-state.md:9` 仍写着 Phase 002 待 Window 1 架构拆解。
- `docs/harness/state/current-state.md:48` 仍写 `packages/shared-contracts` 尚未包含业务 schema、payload 或错误码映射。
- `docs/harness/state/current-state.md:64` 仍建议进入 Window 1。
- `docs/harness/08-eval-checklist.md:9` 要求检查是否更新 `state/current-state.md`。
- `docs/harness/10-steering-state-machine.md:30` 定义 `implementation_done` 为实现完成并自测后交给 Review/Eval。
- `docs/harness/handoffs/phase-002-implementation.md:73` 至 `docs/harness/handoffs/phase-002-implementation.md:84` 记录 Phase 002 契约实现和验证已经完成。

修复要求：

- 在 Fix Pass 中将 `docs/harness/state/current-state.md` 推进到 Phase 002 实现完成后的事实状态。
- 明确当前仓库事实已经包含 Phase 002 OpenAPI、事件、工具、错误码、状态枚举、示例、清单和契约校验脚本。
- 将下一步改为 Window 3 复审，而不是 Window 1。

### P1 - 工具和 Agent 步骤失败契约未强制携带可追踪错误信息，不能满足失败分类要求

Phase 002 架构要求所有失败响应使用统一错误响应，所有失败事件有错误码、可重试标记和追踪 ID，所有工具失败区分权限失败、审批缺失、校验失败、下游不可用和未知错误。但当前工具响应的 `error` 字段不是必填，且在各工具 schema 中只是宽泛 object 或 null；`agent.step.completed.v1` 允许 `status = failed`，但 `error_code` 和 `retryable` 不是必填，也没有 `message` 字段。这样消费者可以产生或接收 `failed` 状态但没有错误码、错误信息、可重试标记或失败分类，后续 Agent 工具调用、人工介入、审计和 AI 质量统计都会缺少契约依据。

证据：

- `docs/harness/handoffs/phase-002-architect.md:310` 要求所有失败响应使用统一错误响应。
- `docs/harness/handoffs/phase-002-architect.md:311` 要求所有失败事件有错误码、可重试标记和追踪 ID。
- `docs/harness/handoffs/phase-002-architect.md:312` 要求所有工具失败区分权限失败、审批缺失、校验失败、下游不可用和未知错误。
- `packages/shared-contracts/tools/ticket.create_followup.v1.schema.json:48` 仅要求 `tool_name`、`tool_version`、`tenant_id`、`trace_id`、`status`，未要求 `error`。
- `packages/shared-contracts/tools/ticket.create_followup.v1.schema.json:62` 将 `error` 定义为 object 或 null，没有引用统一 `ErrorResponse`。
- `packages/shared-contracts/tools/report.save.v1.schema.json:60` 仅要求 `tool_name`、`tool_version`、`tenant_id`、`trace_id`、`status`，未要求 `error`。
- `packages/shared-contracts/tools/report.save.v1.schema.json:73` 将 `error` 定义为 object 或 null，没有引用统一 `ErrorResponse`。
- `packages/shared-contracts/events/agent.step.completed.v1.schema.json:23` 的必填字段不包含 `error_code`、`message` 或 `retryable`。
- `packages/shared-contracts/events/agent.step.completed.v1.schema.json:30` 允许 `status` 为 `failed`。
- `packages/shared-contracts/events/agent.step.completed.v1.schema.json:44` 至 `packages/shared-contracts/events/agent.step.completed.v1.schema.json:45` 将 `error_code` 和 `retryable` 定义为可空且非必填。
- `packages/shared-contracts/errors/error-codes.v1.json:8` 至 `packages/shared-contracts/errors/error-codes.v1.json:21` 没有覆盖“下游不可用”这一工具失败分类。

修复要求：

- 工具响应失败分支必须强制携带统一错误形状，至少包含 `error_code`、`message`、`trace_id`、`retryable`。
- 工具失败错误码或分类必须能覆盖权限失败、审批缺失、校验失败、下游不可用和未知错误。
- `agent.step.completed.v1` 若继续允许 `failed` 状态，需要用 schema 约束失败时必须携带错误码、错误信息和可重试标记；或者拆分为单独失败事件并移除 completed 事件中的失败语义。
- `scripts/validate-contracts.ps1` 需要增加对应校验，避免空泛 `error` object 继续通过。

## Window 1 验收检查

- belongs：部分通过。契约资产位于 `packages/shared-contracts`，没有放入业务 host；但 `current-state.md` 未更新导致治理状态归属不完整。
- authority：部分通过。没有新增第二业务事实源；但失败语义没有强制统一错误响应，工具调用失败的权威字段不足。
- contract：不通过。工具失败和 Agent 步骤失败契约未满足失败分类、错误码、可重试和追踪要求。
- transition：不通过。`current-state.md` 没有从 Phase 002 选择状态推进到实现完成状态；失败状态契约也不能保证错误落盘语义。
- behavior：部分通过。没有新增业务运行时行为，验证脚本可运行；但验收不能只以脚本通过为准。

Window 1 acceptance 当前不满足。

## Agent / RAG / AI 质量检查

本阶段没有运行时 Agent、RAG、模型调用或 AI eval，因此不检查幻觉率、命中率或工具调用成功率的运行指标。但 Phase 002 已定义 Agent 工具和 RAG 引用契约，必须确保：

- 高风险工具不能绕过审批：`ticket.create_followup.v1` 已表达 `requires_approval: true`，方向正确。
- RAG 检索引用可追踪：`knowledge.search.v1` 和知识检索响应包含 `document_id`、`chunk_id` 与 citation，方向正确。
- 工具失败必须可审计和可统计：当前未通过，见 P1 finding。

## 验证命令与结果

```powershell
git status --short --untracked-files=all
```

结果：显示 Phase 002 实现文件及既有同级目录脏文件。注意 Git 根目录为 `C:/Users/20978/VscodeProjects`，存在 Phase 002 之外的既有同级目录脏状态，本次评审未归因给 Phase 002。

```powershell
git diff -- docs/harness/handoffs/phase-002-implementation.md
```

结果：仅看到 Phase 002 implementation handoff 从英文正文改为中文正文的文档差异。

```powershell
Get-ChildItem -Recurse packages/shared-contracts -Force
```

结果：列出 OpenAPI、schemas、errors、events、tools、examples、manifest 和既有 `.gitkeep` 文件。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1
```

结果：`Phase 002 contract validation passed.`

```powershell
Get-ChildItem -Recurse apps,services,ai-services,packages,tests -Force |
  Where-Object {
    $_.Name -match '^(pom.xml|build.gradle|settings.gradle|package.json|vite.config.*|next.config.*|vue.config.*|pyproject.toml|requirements.txt|Dockerfile)$' -or
    $_.FullName -match '\\src\\|\\node_modules\\|\\migrations\\|\\alembic\\|\\routes\\|\\controllers\\|\\components\\'
  }
```

结果：未返回任何文件。

```powershell
rg -n "implementation|implemented|server|mock|adapter|fallback|bridge|Spring|FastAPI|React|LangGraph|pgvector|Milvus|approval|required|human|idempotency|tenant_id|audit|trace_id|retryable|error_code|chunk_id|document_id|index_ref" packages/shared-contracts scripts/validate-contracts.ps1 docs/development/local-dev.md docs/adr/0004-vector-search.md tests/contract/README.md
```

结果：命中内容均为契约文本、治理说明、禁止项或 Phase 002 允许的 ADR 说明；未发现运行时代码、mock server、adapter、fallback、Spring、React、FastAPI、LangGraph、数据库迁移或向量库绑定实现。

## 是否允许进入 Window 4

不允许。

需要 Window 2 Fix Pass 修复上述 findings 后，再由 Window 3 写入 `phase-002-review-fix-1.md` 复审。
