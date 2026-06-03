# Phase 006 Review Fix 2

## 评审模式

- 模式：Re-review fix 2
- 当前 phase：Phase 006 `web-console` 最小工作台
- `git diff` 检查结果：由于本仓库当前相关 Phase 006 文件仍处于未跟踪状态，针对这些文件执行 `git diff -- ...` 返回空；本次同时结合 `git status --short -- apps/web-console ...`、实现 handoff 和实际文件内容完成复审。
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
  - `docs/harness/handoffs/phase-006-review-fix-1.md`
  - `docs/harness/handoffs/phase-006-fix-2-implementation.md`

## 评审结论

- 结论：`approve`
- Window 1 acceptance：满足
- 是否允许进入 Window 4：允许

## 上一次 finding 关闭情况

- `phase-006-review-fix-1.md` Finding 1：已关闭。前端契约镜像已与当前 OpenAPI 对齐：
  - `AiTask.error` 为可选 `ErrorResponse`，未再放宽为 `null`，见 `apps/web-console/src/api/contracts.ts:75-90` 与 `packages/shared-contracts/openapi/agentops-api.v1.yaml:617-645`。
  - `AgentStep.error` 为可选 `ErrorResponse`，未再放宽为 `null`，见 `apps/web-console/src/api/contracts.ts:107-119` 与 `packages/shared-contracts/openapi/agentops-api.v1.yaml:677-709`。
  - `Citation.source_uri` 保持 `?: string | null`，这与当前 OpenAPI 中 `nullable: true` 一致，见 `apps/web-console/src/api/contracts.ts:98-103` 与 `packages/shared-contracts/openapi/agentops-api.v1.yaml:822-834`。
- `phase-006-review-fix-1.md` Finding 2：已关闭。报告区已补齐 `waiting_approval`、`not_ready`、`not_found` 和通用 `error` 分支，见 `apps/web-console/src/app/App.tsx:139-183`、`apps/web-console/src/app/App.tsx:602-658`；对应静态测试已覆盖等待审批、报告未生成和报告不存在，见 `apps/web-console/src/app/App.test.tsx:52-95`。

## Findings

本次复审未发现新的阻断或需要修复的 finding。

## belongs / authority / contract / transition / behavior 复核

- belongs：实现仍限制在 `apps/web-console/**` 与前端验证脚本范围内，符合 `docs/harness/handoffs/phase-006-architect.md:15-22` 的允许修改边界；未越权修改后端运行时或治理文档。
- authority：前端只持有页面状态、会话摘要、视图模型和显式 Mock adapter，未新增身份、工单、Agent、审批或通知事实源，符合 `docs/harness/handoffs/phase-006-architect.md:38-52`。
- contract：前端类型镜像已与当前共享契约一致，且 Mock 标记停留在 UI 视图层而非共享契约层，见 `apps/web-console/src/api/contracts.ts:75-134`、`apps/web-console/src/app/App.tsx:104-109`。
- transition：高风险结果仍只表现为 `waiting_human` / `waiting_approval` 展示态，报告区也已按前端生命周期拆出等待审批、未生成、不可访问和错误分支，符合 `docs/harness/handoffs/phase-006-architect.md:169-198`，对应实现见 `apps/web-console/src/app/App.tsx:139-183`、`apps/web-console/src/app/App.tsx:621-657`。
- behavior：主链路“登录或 Mock 登录联调 -> 查看工单列表 -> 查看工单详情 -> 发起投诉分析任务 -> 查看 Agent 步骤 -> 查看报告预览”已经具备，且报告展示事实、推断、风险、建议和引用来源，步骤展示状态、摘要、错误和引用，见 `apps/web-console/src/app/App.tsx:293-322`、`apps/web-console/src/app/App.tsx:344-385`、`apps/web-console/src/app/App.tsx:419-465`、`apps/web-console/src/app/App.tsx:499-658`。

## Window 1 acceptance 复核

- `docs/harness/handoffs/phase-006-architect.md:189-198` 的验收项已满足：
  - 主链路页面与路由已落地，见 `apps/web-console/src/app/App.tsx:72-102`、`apps/web-console/src/app/App.tsx:251-257`。
  - 工单、任务、步骤和报告均来自公开契约字段镜像与显式 Mock fixture，见 `apps/web-console/src/api/contracts.ts:1-134`、`apps/web-console/src/mocks/mockFixtures.ts:26-208`。
  - 报告预览已展示事实、推断、风险、建议和引用来源，见 `apps/web-console/src/app/App.tsx:131-137`、`apps/web-console/src/app/App.tsx:570-599`。
  - 步骤已展示节点名、状态、摘要、错误和引用，见 `apps/web-console/src/app/App.tsx:536-567`。
  - 高风险结果没有审批通过、动作执行、自动派单或通知入口；仅显示风险边界和等待人工确认信息，见 `apps/web-console/src/app/App.tsx:452-460`、`apps/web-console/src/app/App.tsx:621-642`。

## 验证命令与结果

已运行：

```powershell
git status --short -- apps/web-console scripts/validate-web-console.ps1 docs/harness/handoffs/phase-006-review.md docs/harness/handoffs/phase-006-review-fix-1.md docs/harness/handoffs/phase-006-fix-1-implementation.md docs/harness/handoffs/phase-006-fix-2-implementation.md
git diff -- apps/web-console/src/api/contracts.ts apps/web-console/src/app/App.tsx apps/web-console/src/app/App.test.tsx apps/web-console/src/mocks/mockFixtures.ts scripts/validate-web-console.ps1
powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1
npm.cmd --prefix apps/web-console run typecheck
npm.cmd --prefix apps/web-console run test
npm.cmd --prefix apps/web-console run build
powershell -ExecutionPolicy Bypass -File scripts/validate-web-console.ps1
```

结果：

- `git status --short -- ...`：显示 `apps/web-console/`、`scripts/validate-web-console.ps1` 和 Phase 006 review / fix handoff 文件为未跟踪状态。
- `git diff -- ...`：对上述未跟踪文件返回空输出；因此本次复审以工作区文件实读为准。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1`：失败，但失败原因来自 Phase 002 时期的“禁止实现产物”扫描规则仍把 `apps/web-console/package.json`、`vite.config.ts` 和 `node_modules` 视为 forbidden implementation artifact，见 `scripts/validate-contracts.ps1:164-177`。这与 Phase 006 architect 明确允许前端运行时与前端工具配置进入 `apps/web-console/**` 的范围冲突，见 `docs/harness/handoffs/phase-006-architect.md:17-20`。该失败反映的是阶段感知缺失的治理脚本债务，不是本次 Fix Pass 2 引入的实现缺陷。
- `npm.cmd --prefix apps/web-console run typecheck`：通过。
- `npm.cmd --prefix apps/web-console run test`：通过，`src/app/App.test.tsx` 5 个测试通过，`src/mocks/mockClients.test.ts` 4 个测试通过，共 9 个测试通过。
- `npm.cmd --prefix apps/web-console run build`：通过，Vite 生产构建成功。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-web-console.ps1`：通过。

补充人工验证：

- 已启动本地前端并在 in-app Browser 访问 `http://127.0.0.1:5173/login`、`/tickets`、`/tasks/task_complaint_demo`。
- 实际页面可见 Mock 登录入口、工单列表、等待审批任务状态、Agent 步骤、风险横幅和报告分区，和 `apps/web-console/src/mocks/mockFixtures.ts:108-208` 的样例数据一致。

## 备注

- `scripts/validate-contracts.ps1` 的阶段冲突仍应保留在后续治理视角中处理，但它不构成 Phase 006 Fix Pass 2 的新增 belongs / authority / contract / transition / behavior 缺陷。
