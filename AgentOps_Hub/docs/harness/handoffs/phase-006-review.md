# Phase 006 Review

## 评审模式

- 模式：初次 Review
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

## 评审结论

- 结论：`require fixes`
- Window 1 acceptance：未满足
- 是否允许进入 Window 4：不允许

## Findings

### 1. 严重：前端把 `Citation` 契约改成了自己的私有形状，已经偏离 `shared-contracts`

- 证据：
  - `docs/harness/handoffs/phase-006-architect.md:96` 要求前端类型镜像字段必须来自 `packages/shared-contracts`，不得私自创造跨服务事实字段。
  - `docs/harness/handoffs/phase-006-architect.md:123` 明确冻结给前端消费的报告契约包含 `ReportSummary` 和 `Citation`。
  - `packages/shared-contracts/openapi/agentops-api.v1.yaml:822` 到 `packages/shared-contracts/openapi/agentops-api.v1.yaml:833` 中，`Citation` 只有 `document_id`、`chunk_id`、`source_title`、`source_uri`，其中 `chunk_id` 为必填。
  - `apps/web-console/src/api/contracts.ts:98` 到 `apps/web-console/src/api/contracts.ts:103` 把 `chunk_id` 放宽成可选/可空，删除了 `source_uri`，新增了不在 OpenAPI 中的 `url` 和 `mock_source`。
  - `apps/web-console/src/mocks/mockFixtures.ts:203` 直接构造了 `chunk_id: null` 的报告引用，进一步固化了错误形状。
- 影响：
  - 这不是单纯的前端视图字段映射，而是把“契约镜像”本身改写成了另一份事实源。
  - 一旦后续接真实 `GET /api/ai/tasks/{id}/report` 或 `GET /api/ai/tasks/{id}/steps`，前端会在 `source_uri` 和 `chunk_id` 处理上与 OpenAPI 漂移，破坏 contract-first。
- 修复要求：
  - `contracts.ts` 必须恢复与 `shared-contracts` 一致的 `Citation` 形状。
  - `mock_source` 之类的 UI 标记如果确有需要，只能放到独立的前端视图模型中，不能伪装成共享契约字段。

### 2. 严重：报告预览没有展示“风险”，未达到 Phase 006 明确验收

- 证据：
  - `docs/harness/handoffs/phase-006-architect.md:85` 要求报告必须区分事实、推断、风险、建议和引用来源。
  - `docs/harness/handoffs/phase-006-architect.md:195` 再次把“报告预览展示事实、推断、风险、建议和引用来源”写成 acceptance。
  - `apps/web-console/src/app/App.tsx:457` 到 `apps/web-console/src/app/App.tsx:463` 只渲染了“事实 / 推断 / 建议”三个 section。
  - `apps/web-console/src/api/contracts.ts:126` 到 `apps/web-console/src/api/contracts.ts:135` 的 `ReportSummary` 也没有任何风险字段。
  - `apps/web-console/src/mocks/mockFixtures.ts:177` 到 `apps/web-console/src/mocks/mockFixtures.ts:208` 的 `mockReport` 同样没有风险段落。
- 影响：
  - 当前界面无法把 Agent 风险结论作为报告结构的一部分展示，用户只能看到任务级 `blocking_reason` 横幅，无法满足“报告预览”层面的 acceptance。
  - 按 architect 规则，如果契约缺少支撑字段，应先记录 blocker，而不是继续交付一个缺少风险分区的工作台。
- 修复要求：
  - 至少要让前端报告预览中存在清晰的“风险”分区。
  - 如果现有冻结契约确实无法承载该信息，Window 2 应回到 architect 规则，记录 blocker，而不是继续把缺口掩盖为已完成。

### 3. 中等：Agent 步骤未展示错误信息，失败路径在 UI 上不可读

- 证据：
  - `docs/harness/handoffs/phase-006-architect.md:194` 要求 Agent 步骤至少能展示节点名、状态、摘要、错误和引用。
  - `packages/shared-contracts/openapi/agentops-api.v1.yaml:708` 已为 `AgentStep` 定义 `error` 字段。
  - `apps/web-console/src/api/contracts.ts:108` 到 `apps/web-console/src/api/contracts.ts:119` 也保留了 `AgentStep.error`。
  - `apps/web-console/src/app/App.tsx:436` 到 `apps/web-console/src/app/App.tsx:450` 的 `StepTimeline` 实际只渲染节点名、步骤名、摘要、引用和状态，没有渲染任何 `step.error` 内容。
- 影响：
  - 一旦任务进入 `failed`，界面无法满足 architect 要求的“步骤级错误可读”，用户只能看到状态，无法知道失败原因或是否可重试。
- 修复要求：
  - 在时间线项中补齐 `step.error.error_code`、`step.error.message` 和可重试性展示，至少让失败路径和等待人工路径一样可见。

## belongs / authority / contract / transition / behavior 复核

- belongs：实现基本停留在 `apps/web-console`，没有越权修改后端运行时。
- authority：前端没有直接写业务事实，但 `Citation` 类型镜像已经形成第二事实源，违反 authority 边界。
- contract：存在 `Citation` 契约漂移；报告结构也没有满足 architect 冻结的行为要求。
- transition：高风险结果仍停留在 `waiting_human` / `waiting_approval` 展示态，没有出现审批执行入口，这一点符合要求。
- behavior：主链路大体可走通，但“报告风险可见”和“步骤失败可见”两项 acceptance 未完成，因此不能判定通过。

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

- `powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1`：失败。失败原因为旧的 Phase 002 脚本仍把 `apps/web-console/package.json`、`vite.config.ts` 以及前端依赖产物识别为 forbidden implementation artifact。该结果说明脚本未感知 Phase 006 已批准的前端范围，但本次 `require fixes` 的主因不是这个脚本，而是上面的 contract / behavior finding。
- `npm.cmd --prefix apps/web-console run typecheck`：通过。
- `npm.cmd --prefix apps/web-console run test`：通过，`src/mocks/mockClients.test.ts` 共 4 个测试通过。
- `npm.cmd --prefix apps/web-console run build`：通过，Vite 生产构建成功。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-web-console.ps1`：通过。

额外检查：

- 已尝试启动本地前端并访问 `http://127.0.0.1:4173/login`。
- 本地 HTTP 健康检查返回 `200`，但 in-app Browser 打开该地址返回 `net::ERR_BLOCKED_BY_CLIENT`，因此未完成浏览器内可视化点击冒烟。

## 建议回到 Window 2 的修复方向

- 先收敛 `Citation`：共享契约类型与前端视图模型分离，避免继续把 UI 字段混入 contract mirror。
- 补齐报告预览中的“风险”分区，或者按 architect 规则诚实记录契约 blocker。
- 补齐步骤失败错误展示，并为该路径增加测试。
