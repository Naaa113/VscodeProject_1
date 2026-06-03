# Phase 006 Review Fix 1

## 评审模式

- 模式：Re-review fix 1
- 当前 phase：Phase 006 `web-console` 最小工作台
- 读取的治理文档：
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
- 读取的当前 phase handoff：
  - `docs/harness/handoffs/steering-decision-phase-006.md`
  - `docs/harness/handoffs/phase-006-architect.md`
  - `docs/harness/handoffs/phase-006-implementation.md`
  - `docs/harness/handoffs/phase-006-review.md`
  - `docs/harness/handoffs/phase-006-fix-1-implementation.md`

## 评审结论

- 结论：`require fixes`
- Window 1 acceptance：未满足
- 是否允许进入 Window 4：不允许

## 上一次 finding 关闭情况

- `phase-006-review.md` Finding 1：部分关闭。`Citation` 已去掉私有字段并补回 `chunk_id`，但契约镜像仍把非 nullable 字段放宽为可空，尚未完全回到 `shared-contracts`。
- `phase-006-review.md` Finding 2：已关闭。报告预览已展示“风险”分区。
- `phase-006-review.md` Finding 3：已关闭。步骤时间线已展示 `error_code`、`message` 和可重试性。

## Findings

### 1. 中等：`contracts.ts` 仍在放宽共享契约字段，可空镜像没有完全回到 OpenAPI

- 证据：
  - `docs/harness/handoffs/phase-006-architect.md:96` 要求前端类型镜像字段必须来自 `packages/shared-contracts` v1 契约，不得私自创造跨服务事实字段。
  - `packages/shared-contracts/openapi/agentops-api.v1.yaml:643` 到 `packages/shared-contracts/openapi/agentops-api.v1.yaml:644` 中，`AiTask.error` 仅引用 `ErrorResponse`，没有 `nullable: true`。
  - `packages/shared-contracts/openapi/agentops-api.v1.yaml:708` 到 `packages/shared-contracts/openapi/agentops-api.v1.yaml:709` 中，`AgentStep.error` 仅引用 `ErrorResponse`，没有 `nullable: true`。
  - `packages/shared-contracts/openapi/agentops-api.v1.yaml:822` 到 `packages/shared-contracts/openapi/agentops-api.v1.yaml:833` 中，`Citation.source_uri` 是可选字符串，但没有 `nullable: true`。
  - `apps/web-console/src/api/contracts.ts:85` 仍把 `AiTask.error` 声明为 `error?: ErrorResponse | null`。
  - `apps/web-console/src/api/contracts.ts:102` 仍把 `Citation.source_uri` 声明为 `source_uri?: string | null`。
  - `apps/web-console/src/api/contracts.ts:118` 仍把 `AgentStep.error` 声明为 `error?: ErrorResponse | null`。
  - `docs/harness/handoffs/phase-006-fix-1-implementation.md:35` 声称 `Citation` 已恢复为与 OpenAPI 一致的形状，但当前类型镜像仍比 OpenAPI 更宽。
- 影响：
  - 上一次关于 `Citation` 第二事实源的严重问题已经明显收敛，但前端契约镜像仍不是“按共享契约镜像”，而是“按前端方便程度放宽”。
  - 这会让编译器继续接受本不符合 OpenAPI 的 `null` 值，后续接真实 API 时仍可能掩盖契约漂移。
- 修复要求：
  - 把 `AiTask.error`、`AgentStep.error` 和 `Citation.source_uri` 收紧到与 OpenAPI 一致的非 nullable 形状。
  - 如果 UI 需要“未提供”语义，使用字段缺失或独立视图模型表达，不要继续放宽共享契约镜像。

### 2. 中等：报告区仍未实现独立的 `waiting_approval` / `not_found` / “尚未生成”状态

- 证据：
  - `docs/harness/handoffs/phase-006-architect.md:169` 到 `docs/harness/handoffs/phase-006-architect.md:176` 明确要求报告预览生命周期覆盖 `available`、`waiting_approval`、`not_found` 和 `error`。
  - `docs/harness/handoffs/phase-006-architect.md:181` 规定 `RESOURCE_NOT_FOUND` 在报告页面必须展示“不存在或不可访问”。
  - `docs/harness/handoffs/phase-006-architect.md:183` 规定“报告尚未生成都必须有独立 UI 状态”。
  - `apps/web-console/src/app/App.tsx:475` 到 `apps/web-console/src/app/App.tsx:477` 对报告区只区分 `loading`、通用 `error` 和 `loaded` 三种状态。
  - `apps/web-console/src/app/App.tsx:476` 直接把所有报告失败都落到同一个 `ErrorPanel`，没有针对 `waiting_approval`、`RESOURCE_NOT_FOUND` 或“尚未生成”做分支。
  - `apps/web-console/src/mocks/mockClients.ts:120` 到 `apps/web-console/src/mocks/mockClients.ts:123` 的 `getReport` 永远返回 `mockReport`，当前测试路径没有覆盖“报告尚未生成”或“等待审批未出报告”。
  - `apps/web-console/src/app/App.test.tsx:7` 到 `apps/web-console/src/app/App.test.tsx:43` 只覆盖了“风险分区可见”和“步骤错误可见”，没有覆盖报告缺失或等待审批态。
- 影响：
  - 旧 finding 里“风险分区缺失”的问题已修好，但报告页面的生命周期仍未按 architect 约束落地。
  - 一旦任务处于 `waiting_approval` 且报告尚未生成，或者报告接口返回 `RESOURCE_NOT_FOUND`，当前 UI 只能显示通用错误面板，无法区分“等待人工确认”“资源不存在/不可访问”和“系统错误”。
- 修复要求：
  - 在报告区增加独立状态分支，至少区分：
    - `task.status === "waiting_approval"` 且报告未就绪
    - `report.error.error_code === "RESOURCE_NOT_FOUND"`
    - 其他可重试 / 不可重试错误
  - 为这些分支补充测试，避免回归到通用错误面板。

## belongs / authority / contract / transition / behavior 复核

- belongs：修复仍停留在 `apps/web-console`，没有越权修改后端运行时或治理文档。
- authority：前端没有新增业务事实写入，但契约镜像仍在本地放宽共享字段，说明 authority 边界还没有完全收紧。
- contract：上一次 `Citation` 私有字段问题已收敛，但 nullable 放宽仍是契约漂移；因此 contract 维度仍未完全通过。
- transition：高风险结果仍停留在 `waiting_human` / `waiting_approval` 展示态，没有出现审批通过或动作执行入口；但报告生命周期展示仍不完整。
- behavior：旧的“风险分区可见”和“步骤错误可见”已经修复；新的阻断点是报告状态没有按 acceptance 做独立展示，因此仍不能判定通过。

## 验证命令与结果

已运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1
npm.cmd --prefix apps/web-console run typecheck
npm.cmd --prefix apps/web-console run test
npm.cmd --prefix apps/web-console run build
powershell -ExecutionPolicy Bypass -File scripts/validate-web-console.ps1
```

结果：

- `powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1`：失败。失败原因与上次一致，脚本仍把 `apps/web-console/package.json`、`vite.config.ts` 和 `node_modules` 识别为早期 Phase 002 语义下的 forbidden implementation artifact。该问题仍像阶段感知缺失的脚本债务，不是本次 `require fixes` 的主因。
- `npm.cmd --prefix apps/web-console run typecheck`：通过。
- `npm.cmd --prefix apps/web-console run test`：通过，`src/app/App.test.tsx` 2 个测试通过，`src/mocks/mockClients.test.ts` 4 个测试通过，共 6 个测试通过。
- `npm.cmd --prefix apps/web-console run build`：通过，Vite 生产构建成功。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-web-console.ps1`：通过。

## 建议回到 Window 2 的修复方向

- 先把 `contracts.ts` 中剩余的 nullable 放宽收紧，确保“类型镜像”真正等于共享契约，而不是“接近共享契约”。
- 再补报告区的生命周期分支，把 `waiting_approval`、`RESOURCE_NOT_FOUND` 和“报告尚未生成”从通用错误面板里拆出来。
- 为上述分支补测试，尤其是报告未就绪和资源不存在路径。
