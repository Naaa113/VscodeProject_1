# Phase 002 Fix 1 复审交接

## 评审结论

approve

允许进入 Window 4。

## 当前评审模式

Re-review fix 1。

恢复依据：

- `docs/harness/handoffs/phase-002-implementation.md` 已存在。
- `docs/harness/handoffs/phase-002-review.md` 已存在，结论为 `require fixes`。
- `docs/harness/handoffs/phase-002-fix-1-implementation.md` 已存在。
- 本次复审前不存在 `docs/harness/handoffs/phase-002-review-fix-1.md`。
- `docs/harness/handoffs/phase-002-final.md` 不存在。

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
- `docs/harness/handoffs/phase-002-review.md`
- `docs/harness/handoffs/phase-002-fix-1-implementation.md`
- fix-1 修改范围内的契约文件、示例和 `scripts/validate-contracts.ps1`

## 上次 require fixes 的关闭情况

### Finding 1：`current-state.md` 未推进到实现完成

已关闭。

证据：

- `docs/harness/state/current-state.md:8` 已更新为 `implementation_done`。
- `docs/harness/state/current-state.md:48` 已记录 `packages/shared-contracts` 包含 Phase 002 契约草案、事件 schema、工具 schema、错误码、状态枚举、示例、清单和契约校验脚本。
- `docs/harness/state/current-state.md:54` 已记录 Phase 002 需要通过 Window 3 fix-1 复审。
- `docs/harness/state/current-state.md:65` 已将下一步更新为基于 `phase-002-fix-1-implementation.md` 进行复审。

评估：

- transition 已恢复到可被窗口协议正确读取的状态。
- 没有选择下一阶段，也没有修改项目章程、权威矩阵、host ownership、contract map 或 transition lifetime。

### Finding 2：工具和 Agent 步骤失败契约未强制携带可追踪错误信息

已关闭。

证据：

- `packages/shared-contracts/errors/error-codes.v1.json:21` 新增 `DOWNSTREAM_UNAVAILABLE`，覆盖下游不可用失败分类。
- `packages/shared-contracts/tools/ticket.search.v1.schema.json:82` 至 `packages/shared-contracts/tools/ticket.search.v1.schema.json:87` 定义统一错误字段，`packages/shared-contracts/tools/ticket.search.v1.schema.json:94` 至 `packages/shared-contracts/tools/ticket.search.v1.schema.json:107` 约束 `failed` 状态必须携带错误对象。
- `packages/shared-contracts/tools/ticket.create_followup.v1.schema.json:68` 至 `packages/shared-contracts/tools/ticket.create_followup.v1.schema.json:73` 定义统一错误字段，`packages/shared-contracts/tools/ticket.create_followup.v1.schema.json:80` 至 `packages/shared-contracts/tools/ticket.create_followup.v1.schema.json:93` 约束 `failed` 状态必须携带错误对象。
- `packages/shared-contracts/tools/knowledge.search.v1.schema.json:85` 至 `packages/shared-contracts/tools/knowledge.search.v1.schema.json:90` 定义统一错误字段，`packages/shared-contracts/tools/knowledge.search.v1.schema.json:97` 至 `packages/shared-contracts/tools/knowledge.search.v1.schema.json:110` 约束 `failed` 状态必须携带错误对象。
- `packages/shared-contracts/tools/report.save.v1.schema.json:79` 至 `packages/shared-contracts/tools/report.save.v1.schema.json:84` 定义统一错误字段，`packages/shared-contracts/tools/report.save.v1.schema.json:91` 至 `packages/shared-contracts/tools/report.save.v1.schema.json:104` 约束 `failed` 状态必须携带错误对象。
- `packages/shared-contracts/events/agent.step.completed.v1.schema.json:45` 新增 `message` 字段。
- `packages/shared-contracts/events/agent.step.completed.v1.schema.json:50` 至 `packages/shared-contracts/events/agent.step.completed.v1.schema.json:63` 约束 `status = failed` 时必须携带 `error_code`、`message` 和 `retryable`。
- `packages/shared-contracts/examples/tools/knowledge.search.failure.v1.json:9` 至 `packages/shared-contracts/examples/tools/knowledge.search.failure.v1.json:12` 提供 `DOWNSTREAM_UNAVAILABLE` 失败示例。
- `scripts/validate-contracts.ps1:104` 至 `scripts/validate-contracts.ps1:110` 增加工具失败统一错误字段校验。
- `scripts/validate-contracts.ps1:125` 至 `scripts/validate-contracts.ps1:129` 增加工具失败分类错误码校验。
- `scripts/validate-contracts.ps1:132` 至 `scripts/validate-contracts.ps1:136` 增加 Agent step 失败状态约束校验。

评估：

- contract 已满足上次 review 对错误码、错误信息、追踪 ID、可重试标记和下游不可用分类的要求。
- 高风险工具仍保持审批边界，`ticket.create_followup.v1` 没有被改成可直接执行动作。

## 新问题检查

未发现新的阻断问题或必须修复问题。

### belongs

通过。fix-1 修改集中在 `docs/harness/state/current-state.md`、`packages/shared-contracts/**` 和 `scripts/validate-contracts.ps1`，均与上次 review finding 或 Phase 002 契约范围有关。未修改 `apps/**`、`services/**`、`ai-services/**` 的运行时代码。

### authority

通过。没有引入新的业务事实源；错误码、工具失败和 Agent step 失败语义仍由 `packages/shared-contracts` 统一表达。

### contract

通过。工具失败分支和 Agent step 失败分支已强制可追踪错误字段；新增 `DOWNSTREAM_UNAVAILABLE` 属于错误码目录的兼容性扩展，没有改变既有 URL、事件名、工具名或状态枚举值。

### transition

通过。`current-state.md` 已推进到实现完成并指向 fix-1 复审；失败状态现在具备错误码、错误信息和可重试标记，能支撑失败落盘与审计。

### behavior

通过。fix-1 没有新增业务运行时行为；本地契约校验更严格，且验证命令通过。

## Window 1 验收结论

满足 Phase 002 Window 1 验收条件。

重点依据：

- `packages/shared-contracts` 中存在 OpenAPI、事件、工具、错误码、状态枚举、示例、清单和校验脚本。
- 每个工具 schema 仍包含输入、输出、权限、审批、幂等、审计、超时和重试说明，并补强失败错误约束。
- `ticket.create_followup.v1` 仍需要审批或策略检查，没有允许 Agent 直接绕过审批执行高风险动作。
- 知识检索契约仍保持向量存储无关，没有选择 pgvector 或 Milvus。
- 未新增 Spring、React、FastAPI、数据库迁移、Agent workflow、RAG 实现、mock server 或运行时 adapter。

## Agent / RAG / AI 质量检查

本阶段仍没有运行时 Agent、RAG、模型调用或 AI eval，因此不检查运行指标。复审关注契约边界：

- Agent 工具失败现在可通过错误码、追踪 ID 和可重试标记审计与统计。
- `ticket.create_followup.v1` 保持审批约束。
- `knowledge.search.v1` 仍返回 `document_id`、`chunk_id` 和 citation，支持后续引用追踪。
- `report.save.v1` 仍要求 facts、inferences、recommendations 和 citations，保留事实、推断、建议与引用分离的契约基础。

## 验证命令与结果

```powershell
git status --short
```

结果：显示 fix-1 修改的契约文件、脚本、既有 handoff 修改，以及本阶段开始前已存在的未跟踪文件和同级目录脏状态。

```powershell
git diff -- docs/harness/state/current-state.md packages/shared-contracts/errors/error-codes.v1.json packages/shared-contracts/events/agent.step.completed.v1.schema.json packages/shared-contracts/tools/ticket.search.v1.schema.json packages/shared-contracts/tools/ticket.create_followup.v1.schema.json packages/shared-contracts/tools/knowledge.search.v1.schema.json packages/shared-contracts/tools/report.save.v1.schema.json scripts/validate-contracts.ps1 docs/harness/handoffs/phase-002-fix-1-implementation.md
```

结果：确认 fix-1 修改集中在状态恢复、错误码、工具失败 schema、Agent step 失败约束和校验脚本；未看到服务运行时实现。

```powershell
Get-ChildItem -Recurse packages/shared-contracts -Force
```

结果：列出 OpenAPI、schemas、errors、events、tools、examples、manifest 和既有 `.gitkeep` 文件，包含新增 `knowledge.search.failure.v1.json`。

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
rg -n "DOWNSTREAM_UNAVAILABLE|error_code|message|trace_id|retryable|allOf|status.*failed|implementation_done|Window 3|phase-002-fix-1" docs/harness/state/current-state.md packages/shared-contracts/errors/error-codes.v1.json packages/shared-contracts/events/agent.step.completed.v1.schema.json packages/shared-contracts/tools scripts/validate-contracts.ps1 packages/shared-contracts/examples/tools/knowledge.search.failure.v1.json
```

结果：确认 fix-1 的状态恢复、错误码、工具失败字段、Agent step 失败约束和校验脚本均已落地。

## 是否允许进入 Window 4

允许。
