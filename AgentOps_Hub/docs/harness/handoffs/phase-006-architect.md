# Phase 006 Architect Handoff

## 阶段目标

Phase 006 的目标是落地 `apps/web-console` 最小工作台，让用户可以通过前端完成一条可见的 MVP 主链路：

```text
登录或 Mock 登录联调 -> 查看工单列表 -> 查看工单详情 -> 发起投诉分析任务 -> 查看 Agent 步骤 -> 查看报告预览
```

本阶段只建立用户可操作的前端体验和契约消费边界，不关闭 RAG、审批、通知、网关、消息队列或生产化债务。`steering-decision-phase-006.md` 已记录用户在 2026-06-01 批准进入 Phase 006；`current-state.md` 仍停留在 Phase 005 `handoff_done`，这是 Window 0 决策前的完成态快照，不代表 Phase 006 被阻断。

## 允许修改的文件范围

Window 2 允许修改或新增以下范围：

- `apps/web-console/**`：React + TypeScript 最小运行时、页面、路由、组件、API client、Mock adapter、fixture、样式、测试和本应用 README。
- 根目录前端工作区必要文件：仅限为了运行 `apps/web-console` 所需的 `package.json`、锁文件或前端工具配置；不得把仓库改造成无关框架模板。
- `scripts/validate-web-console.ps1`：如 Window 2 需要统一前端验证入口，可以新增该脚本。
- `tests/web-console/**` 或 `apps/web-console` 内部测试目录：用于最小工作台冒烟、组件、契约 fixture 一致性检查。
- `packages/shared-contracts/examples/**`：仅允许补充非破坏性的前端 Mock 示例，且必须保持 v1 契约字段含义不变。
- `docs/harness/handoffs/phase-006-implementation.md`：Window 2 的实现交接文档。

## 禁止修改的文件范围

Window 2 不得修改以下范围，除非新 steering decision 明确批准：

- `services/identity-service/**`、`services/ticket-service/**`、`ai-services/agent-service/**`。
- `services/workflow-service/**`、`services/notification-service/**`、`ai-services/rag-service/**`、`apps/api-gateway/**` 的业务运行时。
- `deploy/**` 中的生产部署、Kubernetes、Docker 或观测资产。
- `docs/harness/00-project-charter.md`、`02-authority-matrix.md`、`03-host-ownership.md`、`04-contract-map.md`、`state/current-state.md`。
- 任何数据库迁移、H2 fixture 读取器、后端代理服务、消息队列消费者、审批实例、动作命令或通知发送实现。

## 受影响 Host 和数据所有权

受影响 host 是 `apps/web-console`。

`apps/web-console` 拥有：

- 页面路由、页面状态、交互状态和前端缓存。
- 登录态在浏览器侧的最小会话表示，例如 access token、当前租户、当前用户摘要。
- 面向 UI 的视图模型和格式化逻辑。
- 明确标记的 Mock adapter 与 Mock fixture。

`apps/web-console` 不拥有：

- 身份事实、用户密码、角色权限事实和身份审计。
- 工单、客户、SLA、工单审计和工单状态迁移。
- Agent run、Agent step、工具调用、报告保存事实。
- 知识库、文档、向量索引、审批实例、动作命令和通知事实。

前端只能通过公开 API 契约或显式 Mock adapter 消费数据，不得读取内部数据库、测试 fixture 表结构或服务私有模型。

## 必须保持稳定的契约和行为

必须保持稳定的 API：

- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/tickets`
- `GET /api/tickets/{id}`
- `POST /api/ai/tasks`
- `GET /api/ai/tasks/{id}`
- `GET /api/ai/tasks/{id}/steps`
- `GET /api/ai/tasks/{id}/report`

必须保持稳定的 header 和错误形状：

- `Authorization: Bearer <token>`
- `X-Tenant-Id`
- `X-Trace-Id`
- `ErrorResponse`：`error_code`、`message`、`trace_id`、`retryable`，可选 `details`

必须保持稳定的状态枚举：

- `AiTaskStatus`：`pending`、`running`、`waiting_approval`、`success`、`failed`、`cancelled`
- `AgentStep.status`：`pending`、`running`、`success`、`failed`、`skipped`、`waiting_human`
- `TicketStatus`：`open`、`in_progress`、`waiting_customer`、`resolved`、`closed`
- `TicketPriority`：`low`、`normal`、`high`、`urgent`

必须保持稳定的前端行为：

- 高风险或低置信度结果只能展示为 `waiting_human` 或 `waiting_approval`，不得提供审批通过、动作执行、自动派单、自动通知、工单关闭或工单升级入口。
- Mock 工单、Mock Agent 步骤、Mock 知识引用和 Mock 报告保存必须在 fixture、adapter 或界面状态中可识别。
- 报告必须区分事实、推断、风险、建议和引用来源。
- 权限、租户、token 过期和资源不存在错误必须以用户可理解的错误态展示，不得静默转成成功。

## 允许新增的结构

Window 2 可以在 `apps/web-console` 内新增：

- `src/app`、`src/routes`、`src/pages`、`src/components`、`src/features`、`src/api`、`src/mocks`、`src/fixtures`、`src/styles`、`src/test` 等目录。
- API adapter 接口，例如 `AuthClient`、`TicketClient`、`AiTaskClient`、`ReportClient`。
- Mock adapter，例如 `MockAuthClient`、`MockTicketClient`、`MockAiTaskClient`，但命名和配置必须显式带有 Mock 语义。
- 视图模型 mapper，例如把 OpenAPI 响应映射为页面列表项、步骤时间线和报告分区。
- 前端本地 schema 或 TypeScript 类型镜像，但字段必须来自 `packages/shared-contracts` v1 契约，不得私自创造跨服务事实字段。
- 最小路由：`/login`、`/tickets`、`/tickets/:id`、`/tasks/:id`，以及默认工作台入口。

Window 2 可以新增最小构建、测试和样式配置；React + TypeScript 已由 ADR 接受，具体构建工具应保持轻量，并在实现交接中说明选择理由。

## 不允许新增的 Helper / Adapter / Fallback / Bridge

不得新增：

- 直接访问 H2、数据库、服务内部 fixture 或私有实体的前端 helper。
- 后端代理、Node API 服务、BFF、网关替代物或服务间桥接。
- 将真实 API 失败自动吞掉并伪造成成功的 fallback。
- 审批通过、动作命令、自动派单、工单状态变更、通知发送或跟进任务创建 adapter。
- Agent 工具执行 bridge，尤其是 `ticket.create_followup.v1` 写动作桥接。
- 把 Mock 数据包装成生产 API 能力的命名、文案或配置。
- 绕过 `shared-contracts` 的私有字段约定。

## OpenAPI / 事件 / 工具契约要求

本阶段默认冻结现有 OpenAPI v1 草案，优先消费而不是修改。

需要冻结给前端消费的契约：

- 认证：`LoginRequest`、`LoginResponse`、`CurrentUserResponse`。
- 工单：`TicketListResponse`、`TicketSummary`、`TicketDetail`、`TicketStatus`、`TicketPriority`。
- AI 任务：`CreateAiTaskRequest`、`AiTask`、`AiTaskListResponse`。
- Agent 步骤：`AgentStep`、`AgentStepListResponse`。
- 报告：`ReportSummary`、`Citation`。
- 错误：`ErrorResponse` 和 `error-codes.v1.json`。

本阶段不新增事件生产者，不新增真实 SSE 服务，不新增消息队列消费者。前端可以用轮询或 Mock 状态推进展示 `AiTask` 与 `AgentStep`，但不得宣称实时通道已实现。

如 Window 2 发现契约缺少支撑工作台的必要字段，必须优先停止并记录 blocker；只有非破坏性的示例补充或前端内部视图字段可以继续。不得在前端私自定义跨服务字段并把它当作后端契约。

## 状态生命周期和失败处理

登录生命周期：

```text
anonymous -> authenticating -> authenticated
anonymous -> authenticating -> auth_failed
authenticated -> token_expired -> anonymous
authenticated -> forbidden
```

工单列表和详情生命周期：

```text
idle -> loading -> loaded
idle -> loading -> empty
idle -> loading -> error
loaded -> refreshing -> loaded
loaded -> refreshing -> error
```

AI 任务生命周期展示：

```text
draft -> submitting -> pending -> running -> success
draft -> submitting -> pending -> running -> waiting_approval
draft -> submitting -> failed
running -> failed
```

Agent step 展示生命周期：

```text
pending -> running -> success
pending -> running -> failed
pending -> skipped
running -> waiting_human
```

报告预览生命周期：

```text
not_requested -> loading -> available
not_requested -> loading -> waiting_approval
not_requested -> loading -> not_found
not_requested -> loading -> error
```

失败处理要求：

- `AUTH_INVALID_CREDENTIALS`、`AUTH_TOKEN_EXPIRED`、`AUTH_FORBIDDEN`、`TENANT_REQUIRED` 必须进入认证或权限错误态。
- `RESOURCE_NOT_FOUND` 在工单详情和报告页面必须展示不存在或不可访问，不得泄露跨租户资源。
- `AGENT_TASK_FAILED`、`DOWNSTREAM_UNAVAILABLE`、`INTERNAL_ERROR` 必须展示可重试性，不得自动无限重试。
- 空列表、加载中、权限不足、租户不匹配、Mock 数据为空、报告尚未生成都必须有独立 UI 状态。

## 验收条件

Phase 006 验收必须同时满足：

- 用户可以从前端完成“登录或 Mock 登录联调 -> 查看工单列表 -> 查看工单详情 -> 发起投诉分析任务 -> 查看 Agent 步骤 -> 查看报告预览”。
- 工单列表、详情、AI 任务、Agent 步骤和报告数据均来自公开契约或显式 Mock fixture。
- 页面不依赖内部数据库字段、H2 表结构、服务私有实体或测试 fixture 表结构。
- 登录失败、token 过期、租户缺失、权限不足、资源不存在、Agent 失败、下游不可用均有可见错误态。
- 加载态、空状态、失败态和成功态齐全。
- Agent 步骤可读，至少能展示节点名、状态、摘要、错误和引用。
- 报告预览展示事实、推断、风险、建议和引用来源。
- 高风险或低置信度结果只进入等待人工确认或等待审批展示态，不提供执行入口。
- Mock 边界在代码、fixture 或界面状态中清晰可识别。
- 本地验证命令有记录，失败必须解释原因和影响。

## 必须运行的验证命令

Window 2 完成实现后必须运行并记录结果：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1
npm --prefix apps/web-console run typecheck
npm --prefix apps/web-console run test
npm --prefix apps/web-console run build
```

如果 Window 2 新增 `scripts/validate-web-console.ps1`，还必须运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-web-console.ps1
```

如果实现中新增端到端冒烟脚本，还必须运行对应脚本，并在 handoff 中记录访问 URL、浏览器验证范围和失败截图或失败说明。

## Blocker 停止规则

Window 2 遇到以下情况必须停止并写入实现 handoff，不得自行扩大范围：

- 需要修改 `identity-service`、`ticket-service`、`agent-service` 或新增后端服务才能完成主流程。
- 需要新增或破坏性修改 OpenAPI、事件、工具或错误码契约。
- 无法明确区分 Mock 数据和真实服务能力。
- 需要实现审批通过、动作命令、自动派单、通知发送或工单状态变更。
- 前端必须读取数据库、H2 fixture、服务私有模型或内部文件才能展示数据。
- 安装依赖或运行验证命令受阻，且用户未批准必要的网络或环境权限。
- `current-state.md`、最新 steering decision 或用户最新指令出现新的 phase 冲突。

本架构窗口到此结束。请在新的 Window 2 中启动实现。
