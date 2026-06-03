# web-console

Phase 006 最小工作台前端。

## 范围

- 使用 React + TypeScript + Vite 提供本地工作台运行时。
- 通过显式 `Mock*Client` 和 `mock*` fixture 消费 Phase 003 到 Phase 005 的公开契约字段。
- 支持登录、工单列表、工单详情、投诉分析任务发起、Agent 步骤链路和报告预览。
- 高风险输出只展示为等待人工确认或等待审批，不提供审批通过、动作执行、自动派单、通知发送或工单状态变更入口。

## 本地命令

```powershell
npm --prefix apps/web-console run typecheck
npm --prefix apps/web-console run test
npm --prefix apps/web-console run build
```

如 PowerShell 阻止 `npm.ps1`，可在本机用等价的 `npm.cmd` 执行上述命令；Phase 006 验证记录会说明该环境差异。

