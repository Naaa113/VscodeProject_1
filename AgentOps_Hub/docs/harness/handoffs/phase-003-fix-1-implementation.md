# Phase 003 Fix 1 实现交接

## 模式与依据

- 模式：Fix Pass 1。
- 当前 phase：Phase 003 `identity-service` 最小认证与租户模型。
- 最新评审 handoff：`docs/harness/handoffs/phase-003-review.md`。
- 最新评审结论：`require fixes`。
- 本次只修复最新评审 handoff 中列出的 P1 / P2 findings，未修复新问题，未扩大到其他 host。
- 启动前已运行 `git status --short --untracked-files=all`。启动前工作区已有大量未跟踪 harness / skeleton / Phase 003 文件、Phase 002 契约相关 dirty 文件，以及仓库外删除项；本窗口未清理、未 stage、未提交这些既有改动。

## 对应 review finding 与修复方式

### P1：部分登录失败生命周期没有写入身份审计

- 对应 finding：`phase-003-review.md` 中的 `P1：部分登录失败生命周期没有写入身份审计`。
- 修复文件：
  - `services/identity-service/app/main/java/com/agentops/identity/AuthService.java`
  - `services/identity-service/app/test/java/com/agentops/identity/AuthServiceTest.java`
- 修复方式：
  - `LoginRequest` 为空或 `tenant_id` 为空时，先写入 `LOGIN` 失败审计，再返回 `TENANT_REQUIRED`。
  - token 签发抛出 `IdentityException` 时，按原错误码写入 `LOGIN` 失败审计。
  - token 签发出现其他运行时异常时，写入 `INTERNAL_ERROR` 审计并返回统一 `IdentityException`。
  - 新增测试覆盖租户缺失登录失败审计和 token 签发失败审计。
- 未扩大 scope 的原因：
  - 只调整 `identity-service` 内部登录生命周期与测试 fixture，没有新增跨服务事件、工具、网关、前端或业务域能力。

### P2：HTTP 方法错误没有使用统一错误响应形状

- 对应 finding：`phase-003-review.md` 中的 `P2：HTTP 方法错误没有使用统一错误响应形状`。
- 修复文件：
  - `services/identity-service/app/main/java/com/agentops/identity/IdentityHttpServer.java`
  - `services/identity-service/app/test/java/com/agentops/identity/IdentityHttpServerTest.java`
- 修复方式：
  - `/api/auth/login` 与 `/api/auth/me` 的 HTTP 方法不匹配分支改为返回统一 `ErrorResponse`，响应体包含 `error_code`、`message`、`trace_id`、`retryable`。
  - 新增 HTTP 测试覆盖方法错误分支的统一错误形状。
- 未扩大 scope 的原因：
  - 只修复既有认证端点失败响应形状，没有新增错误码、OpenAPI 路径或业务 API。

### P2：演示数据种子逻辑位于默认运行入口，未与生产路径隔离

- 对应 finding：`phase-003-review.md` 中的 `P2：演示数据种子逻辑位于默认运行入口，未与生产路径隔离`。
- 修复文件：
  - `services/identity-service/app/main/java/com/agentops/identity/IdentityApplication.java`
  - `services/identity-service/app/main/java/com/agentops/identity/IdentityConfig.java`
  - `services/identity-service/app/main/java/com/agentops/identity/H2IdentityRepository.java`
  - `services/identity-service/app/main/resources/application.example.properties`
  - `services/identity-service/app/test/java/com/agentops/identity/TestIdentityData.java`
  - `services/identity-service/app/test/java/com/agentops/identity/AuthServiceTest.java`
  - `services/identity-service/app/test/java/com/agentops/identity/IdentityHttpServerTest.java`
  - `services/identity-service/README.md`
- 修复方式：
  - 默认应用入口只初始化 schema，不再调用演示数据种子逻辑。
  - 删除运行时配置中的 `IDENTITY_DEMO_LOGIN_SECRET`。
  - 从主代码仓库实现中移除硬编码演示租户、用户、角色和权限种子逻辑。
  - 新增测试侧 `TestIdentityData` fixture，由单元测试和 HTTP 测试显式创建测试数据。
  - README 明确默认运行入口不写入演示身份事实，测试数据只由测试 fixture 创建。
- 未扩大 scope 的原因：
  - 演示数据隔离只在 `identity-service` 范围内调整，未引入用户管理 API、数据导入 API、生产 seed 命令或其他 host 改动。

## 修改文件

- `services/identity-service/app/main/java/com/agentops/identity/AuthService.java`
- `services/identity-service/app/main/java/com/agentops/identity/IdentityHttpServer.java`
- `services/identity-service/app/main/java/com/agentops/identity/IdentityApplication.java`
- `services/identity-service/app/main/java/com/agentops/identity/IdentityConfig.java`
- `services/identity-service/app/main/java/com/agentops/identity/H2IdentityRepository.java`
- `services/identity-service/app/main/resources/application.example.properties`
- `services/identity-service/app/test/java/com/agentops/identity/AuthServiceTest.java`
- `services/identity-service/app/test/java/com/agentops/identity/IdentityHttpServerTest.java`
- `services/identity-service/app/test/java/com/agentops/identity/TestIdentityData.java`
- `services/identity-service/README.md`
- `docs/harness/handoffs/phase-003-fix-1-implementation.md`

## 完成的 architect acceptance

- 登录成功和失败都形成身份审计记录；本次补齐租户缺失和 token 签发失败分支。
- token 签发失败返回 `INTERNAL_ERROR`，并记录内部身份审计。
- 所有认证端点失败响应保持统一错误响应形状。
- 测试 fixture 与默认运行路径隔离，默认入口不再写入硬编码演示身份事实。
- `identity-service` 仍只拥有身份域数据和行为。
- 没有新增工单、审批、Agent、RAG、前端、网关、通知或高风险业务动作能力。

## 保持不变的 contract

- 稳定 URL 仍为 `POST /api/auth/login` 与 `GET /api/auth/me`。
- `LoginRequest`、`LoginResponse`、`CurrentUserResponse`、`ErrorResponse` 字段未变。
- 未新增错误码。
- 未修改 `packages/shared-contracts`。
- 未新增跨服务事件。
- 未新增 Agent 工具契约。
- 未修改工单、AI 任务、知识库、SSE、事件或工具相关契约。

## 行为变化

- `POST /api/auth/login` 在请求为空、租户缺失、token 签发失败时都会写入登录失败审计。
- `/api/auth/login` 与 `/api/auth/me` 的方法错误分支返回统一错误响应体。
- 默认 `IdentityApplication` 启动后只初始化身份域 schema，不写入演示租户或演示用户。
- 测试仍使用 `tenant_demo`、`tenant_other`、`demo.user` 等 fixture 数据，但这些数据只存在于 `app/test/java`。

## 测试与验证结果

- `mvn test`
  - 运行目录：`services/identity-service`。
  - 结果：通过。
  - 覆盖：8 个测试，0 失败，0 错误。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1`
  - 结果：通过。
  - 输出：`Phase 002 contract validation passed.`
- `powershell -ExecutionPolicy Bypass -File scripts/validate-identity-service.ps1`
  - 结果：通过。
  - 内部运行 `mvn test`，8 个测试全部通过。
  - 输出：`Phase 003 identity-service validation passed.`
- 越界扫描：
  - 结果：无输出。
  - 未在 `apps`、`ai-services`、`services/ticket-service`、`services/workflow-service`、`services/notification-service` 下发现 Phase 003 新增业务实现文件。
- `git status --short --untracked-files=all`
  - 已运行。
  - 工作区仍包含启动前已有的大量未跟踪文件、Phase 002 相关 dirty 文件和仓库外删除项；本窗口未 stage、未提交。

## Blocker 与遗留风险

- Blocker：无。
- 遗留风险：
  - 默认运行入口不再写入演示用户，因此本地手动登录需要后续阶段提供受控的数据初始化方案或手工准备本地身份数据；本次未新增生产 seed 命令，避免扩大 scope。
  - HTTP 方法错误使用现有错误码承载统一响应形状，未新增 `METHOD_NOT_ALLOWED` 错误码；如后续要精细化，需要契约阶段先评审。

## 是否需要重新评审

需要重新进入 Window 3 复审。本次 Fix Pass 目标是关闭 `phase-003-review.md` 的三个 findings，尚未进入 Window 4。
