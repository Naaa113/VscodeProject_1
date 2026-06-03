# Phase 006 Fix Pass 2 Implementation Handoff

## 模式

- 模式：Fix Pass
- Fix 编号：`fix-2`
- 对应最新 review handoff：`docs/harness/handoffs/phase-006-review-fix-1.md`

## Git baseline

本窗口开始前已运行：

```powershell
git status --short --untracked-files=all
```

开始前工作区已存在大量 dirty / untracked 文件，包括早于本窗口的 harness、契约、服务、文档和仓库外删除项。本窗口未清理、未 stage、未提交这些既有变更。

本窗口实际修改或新增的文件：

- `apps/web-console/src/api/contracts.ts`
- `apps/web-console/src/app/App.tsx`
- `apps/web-console/src/app/App.test.tsx`
- `apps/web-console/src/styles/global.css`
- `docs/harness/handoffs/phase-006-fix-2-implementation.md`

## 对应 review finding 与修复方式

### Finding 1：契约镜像仍存在 nullable 放宽

- 对应 finding：`docs/harness/handoffs/phase-006-review-fix-1.md` Finding 1
- 修复方式：
  - 将 `apps/web-console/src/api/contracts.ts` 中的 `AiTask.error` 从 `ErrorResponse | null` 收紧为 `ErrorResponse` 可选字段。
  - 将 `apps/web-console/src/api/contracts.ts` 中的 `AgentStep.error` 从 `ErrorResponse | null` 收紧为 `ErrorResponse` 可选字段。
  - 保留 `Citation.source_uri?: string | null` 不变，并在本次 handoff 中明确说明原因：当前 `packages/shared-contracts/openapi/agentops-api.v1.yaml` 仍把 `Citation.source_uri` 标记为 `nullable: true`。如果前端把它收紧成非 nullable，反而会制造新的契约漂移。
  - 相关 UI 逻辑继续通过字段缺失或前端视图模型表达“未提供”语义，不再通过放宽错误对象的可空镜像隐藏契约差异。

### Finding 2：报告区缺少独立的 `waiting_approval` / `not_found` / “尚未生成”状态

- 对应 finding：`docs/harness/handoffs/phase-006-review-fix-1.md` Finding 2
- 修复方式：
  - 在 `apps/web-console/src/app/App.tsx` 中新增 `buildReportPanelState`，把报告区状态显式拆分为 `loading`、`available`、`waiting_approval`、`not_ready`、`not_found`、`error`。
  - 当报告接口返回 `RESOURCE_NOT_FOUND` 且任务状态为 `waiting_approval` 时，报告区显示“等待人工确认”，不再落入通用错误面板。
  - 当报告接口返回 `RESOURCE_NOT_FOUND` 且任务仍处于 `pending` / `running` 时，报告区显示“报告尚未生成”。
  - 当报告接口返回 `RESOURCE_NOT_FOUND` 且任务已不处于等待审批或处理中时，报告区显示“报告不存在或不可访问”。
  - 新增 `ReportStatePanel` 作为独立状态组件，并在 `apps/web-console/src/styles/global.css` 补齐最小样式。
  - 在 `apps/web-console/src/app/App.test.tsx` 增加等待审批、尚未生成、资源不存在三条静态渲染测试，锁定这些状态分支。

## 为什么修复没有扩大 scope

- 仅修改 `apps/web-console` 内部文件和本次 fix handoff，没有触碰 `services/**`、`ai-services/**`、`packages/shared-contracts/**` 或治理规则文件。
- 没有新增审批通过、动作执行、自动派单、通知发送、工单状态变更、后端 bridge 或新的 API 契约。
- 报告区新增状态只是在前端消费既有任务状态和错误码，不是新增跨服务字段或私有事实。
- 没有为了满足 review 文案而错误收紧 `Citation.source_uri`；本窗口以 `shared-contracts` 当前权威内容为准，避免制造新的 contract drift。

## 完成的 architect acceptance

- 前端报告区现在独立展示：
  - `waiting_approval`
  - 报告尚未生成
  - 报告不存在或不可访问
  - 通用错误
  - 正常报告预览
- 报告预览继续展示事实、推断、风险、建议和引用来源。
- Agent 步骤继续展示节点名、状态、摘要、错误和引用。
- 高风险结果仍只停留在 `waiting_human` / `waiting_approval` 展示态，没有新增审批通过或动作执行入口。

## 保持不变的 contract

- 未修改 `packages/shared-contracts/**`。
- 未修改 `POST /api/auth/login`、`GET /api/auth/me`、`GET /api/tickets`、`GET /api/tickets/{id}`、`POST /api/ai/tasks`、`GET /api/ai/tasks/{id}`、`GET /api/ai/tasks/{id}/steps`、`GET /api/ai/tasks/{id}/report` 的契约语义。
- `AiTask.error` 与 `AgentStep.error` 的前端类型镜像已收紧回共享契约。
- `Citation.source_uri` 保持与当前 OpenAPI 一致的 nullable 语义，没有私自收紧或放宽。
- 保持 `Authorization: Bearer <token>`、`X-Tenant-Id`、`X-Trace-Id` 和 `ErrorResponse` 形状不变。
- 保持 `AiTaskStatus`、`AgentStep.status`、`TicketStatus`、`TicketPriority` 枚举不变。

## 行为变化

- 报告区不再把等待审批、报告缺失和通用失败混成同一个错误面板。
- 运行中任务在报告未生成时会展示独立提示，而不是只显示通用错误。
- 任务已完成但报告缺失时会展示“报告不存在或不可访问”状态。
- 未改变任何后端运行时、共享契约、工具语义或业务事实所有权。

## 测试 / 验证结果

已通过：

```powershell
npm.cmd --prefix apps\web-console run typecheck
npm.cmd --prefix apps\web-console run test
npm.cmd --prefix apps\web-console run build
powershell -ExecutionPolicy Bypass -File scripts\validate-web-console.ps1
```

测试结果：

- `src/app/App.test.tsx`：5 个测试通过，覆盖报告风险分区、步骤错误展示、等待审批、报告尚未生成和报告不存在状态。
- `src/mocks/mockClients.test.ts`：4 个测试通过。
- 前端共 9 个测试通过。
- `vite build` 成功生成生产构建。

仍未通过：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-contracts.ps1
```

失败原因：

- 该脚本仍按早期 Phase 002 的“禁止实现产物”规则扫描 `apps/web-console/package.json`、`vite.config.ts` 和 `node_modules`，与 Phase 006 architect 已批准的前端实现范围冲突。
- 本次 Fix Pass 未修改该脚本，因为最新 review finding 未要求处理该阶段感知问题，而且 architect 仍限制 Window 2 不得越权修改治理规则或扩大范围。

## blocker 或遗留风险

- `scripts/validate-contracts.ps1` 与 Phase 006 前端范围的阶段冲突仍然存在，需要后续 Window 3 / Window 1 / Steering 判断是脚本债务还是需更新规则。
- 本窗口没有引入新的后端联调或浏览器内点击冒烟，验证仍以类型检查、静态测试和构建为主。

## 是否需要重新评审

- 需要重新进入 Window 3 Review/Eval。
- 本次 Fix Pass 仅处理 `docs/harness/handoffs/phase-006-review-fix-1.md` 中列出的 finding，没有扩大到新的实现范围。
