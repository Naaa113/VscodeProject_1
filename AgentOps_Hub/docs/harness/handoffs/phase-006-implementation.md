# Phase 006 Implementation Handoff

## 模式

初次实现。

## Git baseline

本窗口开始前已运行：

```powershell
git status --short --untracked-files=all
```

开始前工作区已存在大量 dirty / untracked 文件，包括但不限于 Phase 001 到 Phase 005 的 harness、契约、服务、脚本和仓库外删除项。本窗口未清理、未 stage、未提交这些既有变更。

本窗口实际修改或新增的文件范围：

- `apps/web-console/README.md`
- `apps/web-console/package.json`
- `apps/web-console/package-lock.json`
- `apps/web-console/index.html`
- `apps/web-console/tsconfig.json`
- `apps/web-console/vite.config.ts`
- `apps/web-console/src/main.tsx`
- `apps/web-console/src/app/App.tsx`
- `apps/web-console/src/api/clients.ts`
- `apps/web-console/src/api/contracts.ts`
- `apps/web-console/src/mocks/mockClients.ts`
- `apps/web-console/src/mocks/mockClients.test.ts`
- `apps/web-console/src/mocks/mockFixtures.ts`
- `apps/web-console/src/styles/global.css`
- `scripts/validate-web-console.ps1`
- `docs/harness/handoffs/phase-006-implementation.md`

## 修改内容

- 在 `apps/web-console` 新增 React + TypeScript + Vite 最小运行时。
- 新增 Phase 003 到 Phase 005 公开契约的前端 TypeScript 类型镜像，覆盖认证、当前用户、工单、AI 任务、Agent step、报告、引用和统一错误响应。
- 新增显式 `MockAuthClient`、`MockTicketClient`、`MockAiTaskClient`，所有 Mock 数据均位于 `mockFixtures.ts`，命名带 `mock` 语义。
- 实现登录页、工单列表、工单详情、投诉分析任务发起、Agent 步骤链路和报告预览。
- 实现加载态、空状态、错误态、资源不存在、认证失败、租户错误和高风险等待人工确认展示。
- 新增 `scripts/validate-web-console.ps1`，统一运行 `typecheck`、`test`、`build`。
- 更新 `apps/web-console/README.md`，说明 Phase 006 范围和本地命令。

## 完成的 architect acceptance

- 用户可以通过前端完成“Mock 登录联调 -> 查看工单列表 -> 查看工单详情 -> 发起投诉分析任务 -> 查看 Agent 步骤 -> 查看报告预览”。
- 工单、任务、步骤和报告数据均来自公开契约字段镜像和显式 Mock fixture。
- 页面不读取数据库、H2 fixture、后端私有实体或服务内部文件。
- 登录失败、租户不匹配、资源不存在、Agent 失败、下游不可用形状均通过统一错误面板表达；Mock client 测试覆盖主要错误路径。
- 加载态、空状态、失败态和成功态已实现。
- Agent 步骤展示节点名、状态、摘要、引用和等待人工确认状态。
- 报告预览区分事实、推断、建议和引用来源。
- 高风险输出只展示 `waiting_human` / `waiting_approval`，未提供审批通过、动作执行、自动派单、通知发送、工单关闭或升级入口。
- Mock 边界在代码命名、fixture 和界面 `Mock adapter: Phase 006` 标识中可识别。

## 保持不变的 contract

- 未修改 `packages/shared-contracts/**`。
- 未修改 `POST /api/auth/login`、`GET /api/auth/me`、`GET /api/tickets`、`GET /api/tickets/{id}`、`POST /api/ai/tasks`、`GET /api/ai/tasks/{id}`、`GET /api/ai/tasks/{id}/steps`、`GET /api/ai/tasks/{id}/report` 的契约语义。
- 保持 `Authorization: Bearer <token>`、`X-Tenant-Id`、`X-Trace-Id` 和 `ErrorResponse` 形状不变。
- 保持 `AiTaskStatus`、`AgentStep.status`、`TicketStatus`、`TicketPriority` 枚举不变。
- `ticket.search.v1` 仍为只读语义；本阶段没有新增写动作工具或 bridge。

## 行为变化

- 新增 `apps/web-console` 前端运行时和本地 Mock-first 工作台体验。
- 新增前端构建、类型检查和测试入口。
- 未改变后端服务、共享契约、事件生产者、工具执行语义或业务事实所有权。

## 测试 / 验证结果

已通过：

```powershell
npm.cmd --prefix apps\web-console run typecheck
npm.cmd --prefix apps\web-console run test
npm.cmd --prefix apps\web-console run build
powershell -ExecutionPolicy Bypass -File scripts\validate-web-console.ps1
```

测试结果：

- `mockClients.test.ts`：1 个测试文件通过，4 个测试通过。
- `vite build` 成功生成生产构建。

已尝试但未通过：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-contracts.ps1
```

失败原因：当前 `scripts/validate-contracts.ps1` 仍包含早期契约阶段的“禁止实现产物”扫描规则，会把 Phase 006 architect 明确允许的 `apps/web-console/package.json`、`vite.config.ts` 和前端依赖目录识别为 forbidden implementation artifact。本窗口无权修改 `scripts/validate-contracts.ps1`，因此记录为验证脚本与当前阶段范围冲突。

已尝试但受环境限制：

```powershell
npm --prefix apps\web-console run typecheck
npm --prefix apps\web-console run test
npm --prefix apps\web-console run build
```

失败原因：本机 PowerShell 执行策略禁止加载 `C:\Program Files\nodejs\npm.ps1`。已使用等价的 `npm.cmd` 命令完成验证。

浏览器冒烟：

- 已启动本地 dev server：`http://127.0.0.1:5173`，`Invoke-WebRequest` 返回 `200`。
- in-app Browser 访问 `http://127.0.0.1:5173` 和 `http://localhost:5173` 均被浏览器侧返回 `net::ERR_BLOCKED_BY_CLIENT`，因此未能完成浏览器内点击验证。

## Blocker 或遗留风险

- `scripts/validate-contracts.ps1` 与 Phase 006 已批准前端实现范围冲突，需要后续由架构或评审窗口决定是否更新契约校验脚本的阶段感知规则。本窗口未越权修改。
- `npm install` 报告 5 个中等级别依赖审计项。未运行 `npm audit fix --force`，避免引入未评审的依赖升级。
- in-app Browser 被本地访问拦截，未完成可视化点击冒烟；前端类型检查、单元测试、构建和本地 HTTP 200 已验证。

## 是否需要重新评审

需要进入 Window 3 Review/Eval。重点请复核：

- Phase 006 前端范围是否满足 architect acceptance。
- `validate-contracts.ps1` 的失败是否应作为脚本债务处理，而不是本阶段实现失败。
- 高风险等待人工确认展示是否严格保持无动作执行入口。

