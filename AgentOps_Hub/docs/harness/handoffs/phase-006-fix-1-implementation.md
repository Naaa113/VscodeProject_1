# Phase 006 Fix Pass 1 Implementation Handoff

## 模式

- 模式：Fix Pass
- Fix 编号：`fix-1`
- 对应最新 review handoff：`docs/harness/handoffs/phase-006-review.md`

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
- `apps/web-console/src/mocks/mockClients.test.ts`
- `apps/web-console/src/mocks/mockFixtures.ts`
- `apps/web-console/src/styles/global.css`
- `docs/harness/handoffs/phase-006-fix-1-implementation.md`

## 对应 review finding 与修复方式

### Finding 1：`Citation` 契约镜像漂移

- 对应 finding：`docs/harness/handoffs/phase-006-review.md` Finding 1
- 修复方式：
  - 将 `apps/web-console/src/api/contracts.ts` 中的 `Citation` 恢复为与 OpenAPI 一致的形状，只保留 `document_id`、`chunk_id`、`source_title`、`source_uri`。
  - 删除契约镜像中的 `mock_source`、`url` 和可空 `chunk_id`。
  - 将 `apps/web-console/src/mocks/mockFixtures.ts` 中的 Mock 引用改为使用契约内的 `source_uri: "mock://..."` 明确表达 Mock 边界，并补齐必填 `chunk_id`。
  - 在 `apps/web-console/src/app/App.tsx` 内新增前端内部 `DisplayCitation` 视图模型，只在 UI 层根据 `source_uri` 判断是否显示 `Mock` 标记，不再把 UI 字段混入共享契约镜像。

### Finding 2：报告预览缺少“风险”分区

- 对应 finding：`docs/harness/handoffs/phase-006-review.md` Finding 2
- 修复方式：
  - 保持 `ReportSummary` 契约不变，不私自新增跨服务字段。
  - 在 `apps/web-console/src/app/App.tsx` 中新增前端内部 `buildRiskItems` / `buildReportPreviewModel`，从已存在的 `AiTask.blocking_reason` 与 `AgentStep` 的 `waiting_human` / `error` 信息派生“风险”分区。
  - `ReportPreview` 现已展示“事实 / 推断 / 风险 / 建议 / 引用”五个分区，满足 architect acceptance，同时没有修改共享契约。

### Finding 3：步骤时间线未展示错误信息

- 对应 finding：`docs/harness/handoffs/phase-006-review.md` Finding 3
- 修复方式：
  - 在 `apps/web-console/src/app/App.tsx` 的 `StepTimeline` 中补充步骤级错误展示，显示 `error_code`、`message` 和可重试性。
  - 为失败步骤使用单独的视觉提示，并在 `apps/web-console/src/styles/global.css` 中补齐最小样式。
  - 新增 `apps/web-console/src/app/App.test.tsx`，用静态渲染测试锁定“风险分区可见”和“步骤错误可见”。

## 为什么修复没有扩大 scope

- 仅修改 `apps/web-console` 内部文件和本次 fix handoff，没有触碰 `services/**`、`ai-services/**`、`packages/shared-contracts/**` 或治理规则文件。
- 没有新增审批通过、动作执行、自动派单、通知发送、工单状态变更或后端 bridge。
- 没有修改 OpenAPI、事件、工具、错误码或状态枚举。
- “风险”展示完全基于已有前端可消费契约字段派生，没有把前端私有字段伪装成共享契约事实。

## 完成的 architect acceptance

- 前端报告预览现在展示事实、推断、风险、建议和引用来源。
- Agent 步骤现在可展示节点名、状态、摘要、错误和引用。
- Mock 边界仍然清晰可识别，并且不再污染共享契约镜像。
- 高风险结果仍只停留在 `waiting_human` / `waiting_approval` 展示态，没有新增执行入口。

## 保持不变的 contract

- 未修改 `packages/shared-contracts/**`。
- 未修改 `POST /api/auth/login`、`GET /api/auth/me`、`GET /api/tickets`、`GET /api/tickets/{id}`、`POST /api/ai/tasks`、`GET /api/ai/tasks/{id}`、`GET /api/ai/tasks/{id}/steps`、`GET /api/ai/tasks/{id}/report` 的契约语义。
- 保持 `Authorization: Bearer <token>`、`X-Tenant-Id`、`X-Trace-Id` 和 `ErrorResponse` 形状不变。
- 保持 `AiTaskStatus`、`AgentStep.status`、`TicketStatus`、`TicketPriority` 枚举不变。

## 行为变化

- 报告预览新增“风险”分区。
- 步骤时间线新增失败错误信息展示。
- 引用列表现在展示契约内 `chunk_id`，并根据 `source_uri` 标记 Mock 来源。
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

- `src/app/App.test.tsx`：2 个测试通过，覆盖报告风险分区和步骤错误展示。
- `src/mocks/mockClients.test.ts`：4 个测试通过，补充覆盖 Mock 引用仍保持契约形状。
- `vite build` 成功生成生产构建。

仍未通过：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-contracts.ps1
```

失败原因：

- 该脚本仍按早期“禁止实现产物”规则扫描 `apps/web-console/package.json`、`vite.config.ts` 和前端依赖目录，与 Phase 006 architect 已批准的前端实现范围冲突。
- 本次 Fix Pass 未修改该脚本，因为最新 review finding 未要求修复它，且 architect 仍限制 Window 2 不得越权修改治理规则或扩大范围。

## blocker 或遗留风险

- `scripts/validate-contracts.ps1` 与 Phase 006 前端范围仍存在阶段冲突，需要后续 Window 3 / Window 1 / Steering 决定是否调整脚本规则或将其认定为既有脚本债务。
- 本窗口未新增浏览器内可视化冒烟；本次 fix 主要通过类型检查、单测和构建验证。

## 是否需要重新评审

- 需要重新进入 Window 3 Review/Eval。
- 本次 Fix Pass 仅针对 `docs/harness/handoffs/phase-006-review.md` 中列出的 finding 修复，适合由 Review 窗口复核关闭。
