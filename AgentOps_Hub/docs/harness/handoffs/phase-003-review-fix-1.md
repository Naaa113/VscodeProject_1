# Phase 003 Fix 1 复审

## 评审结论

approve

允许进入 Window 4。本次复审确认 `phase-003-review.md` 中三个 require fixes finding 均已关闭；未发现 Fix Pass 1 引入新的 belongs / authority / contract / transition / behavior 阻断问题。

## 当前评审模式

- 模式：Re-review fix 1。
- 输出文件：`docs/harness/handoffs/phase-003-review-fix-1.md`。
- 自动恢复依据：`phase-003-implementation.md`、`phase-003-review.md`、`phase-003-fix-1-implementation.md` 存在，`phase-003-review-fix-1.md` 与 `phase-003-final.md` 不存在。

## 已读取 handoff 文件

- `docs/harness/handoffs/steering-decision-phase-003.md`
- `docs/harness/handoffs/phase-003-architect.md`
- `docs/harness/handoffs/phase-003-implementation.md`
- `docs/harness/handoffs/phase-003-review.md`
- `docs/harness/handoffs/phase-003-fix-1-implementation.md`

同时按协议读取了：

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

## 上次 finding 关闭情况

### P1：部分登录失败生命周期没有写入身份审计

状态：已关闭。

证据：

- `phase-003-review.md:37` 到 `phase-003-review.md:54` 记录原问题为租户缺失和 token 签发失败未完整写入登录失败审计。
- `services/identity-service/app/main/java/com/agentops/identity/AuthService.java:17` 到 `services/identity-service/app/main/java/com/agentops/identity/AuthService.java:23` 在请求为空、租户缺失、用户名或密码缺失时写入登录失败审计。
- `services/identity-service/app/main/java/com/agentops/identity/AuthService.java:44` 到 `services/identity-service/app/main/java/com/agentops/identity/AuthService.java:52` 捕获 token 签发异常，写入登录失败审计，并将非预期异常统一为 `INTERNAL_ERROR`。
- `services/identity-service/app/test/java/com/agentops/identity/AuthServiceTest.java:69` 到 `services/identity-service/app/test/java/com/agentops/identity/AuthServiceTest.java:90` 覆盖租户缺失、校验失败和 token 签发失败审计。

结论：transition 与 authority 要求已补齐，身份审计仍由 `identity-service` 内部落地，未新增跨服务审计事件。

### P2：HTTP 方法错误没有使用统一错误响应形状

状态：已关闭。

证据：

- `phase-003-review.md:56` 到 `phase-003-review.md:70` 记录原问题为方法错误分支没有统一 `ErrorResponse`。
- `services/identity-service/app/main/java/com/agentops/identity/IdentityHttpServer.java:47` 到 `services/identity-service/app/main/java/com/agentops/identity/IdentityHttpServer.java:67` 对 `/api/auth/login` 和 `/api/auth/me` 的方法不匹配分支返回统一错误响应体。
- `services/identity-service/app/main/java/com/agentops/identity/ApiModels.java:15` 到 `services/identity-service/app/main/java/com/agentops/identity/ApiModels.java:24` 定义统一错误字段 `error_code`、`message`、`trace_id`、`retryable` 和可选 `details`。
- `services/identity-service/app/test/java/com/agentops/identity/IdentityHttpServerTest.java:113` 到 `services/identity-service/app/test/java/com/agentops/identity/IdentityHttpServerTest.java:140` 覆盖方法错误分支的统一错误形状。

结论：contract 与 behavior 要求已满足。方法错误当前复用 `AUTH_FORBIDDEN` 作为错误码，但响应形状统一，且 Fix handoff 已记录后续如需 `METHOD_NOT_ALLOWED` 需先走契约评审。

### P2：演示数据种子逻辑位于默认运行入口，未与生产路径隔离

状态：已关闭。

证据：

- `phase-003-review.md:72` 到 `phase-003-review.md:86` 记录原问题为默认运行路径会写入硬编码演示身份事实。
- `services/identity-service/app/main/java/com/agentops/identity/IdentityApplication.java:9` 到 `services/identity-service/app/main/java/com/agentops/identity/IdentityApplication.java:15` 默认入口只读取配置、初始化 schema 并启动认证服务，不再写入演示身份数据。
- `services/identity-service/app/main/java/com/agentops/identity/IdentityConfig.java:7` 到 `services/identity-service/app/main/java/com/agentops/identity/IdentityConfig.java:17` 运行时配置只读取 `IDENTITY_JWT_SECRET`、端口、数据库地址和 token TTL，不再读取演示登录密钥。
- `services/identity-service/app/test/java/com/agentops/identity/TestIdentityData.java:10` 到 `services/identity-service/app/test/java/com/agentops/identity/TestIdentityData.java:35` 将测试数据 seed 限制在测试代码中。
- `services/identity-service/README.md:24` 到 `services/identity-service/README.md:30` 明确默认运行入口不写入演示租户、用户、角色或权限数据。

结论：测试 fixture 已与默认运行路径隔离，authority 风险已关闭，未新增生产 seed、导入 API 或其他 host 桥接能力。

## 新问题检查

未发现新的阻断 finding。

- belongs：Fix Pass 1 修改集中在 `services/identity-service` 及其测试、README 和本次 handoff；未发现 `ticket-service`、`workflow-service`、`agent-service`、`rag-service`、前端或网关新增 Phase 003 运行时代码。
- authority：身份事实仍由 `identity-service` 拥有；测试数据只在 `app/test/java` 中创建，未成为默认运行事实源。
- contract：未新增错误码、OpenAPI 路径、事件或 Agent 工具契约；认证端点和统一错误响应字段保持稳定。
- transition：登录失败、token 签发失败、当前用户失败、租户不匹配、用户禁用和 token 过期路径均有代码和测试覆盖。
- behavior：Window 1 acceptance 中与本次 findings 相关的审计、统一错误响应、测试隔离和越界限制均满足。

## Window 1 acceptance 检查

- 最小 Java 身份服务：满足。
- `POST /api/auth/login` 与 `GET /api/auth/me`：满足。
- 统一错误响应字段：满足。
- 登录失败、token 过期、权限不足、租户缺失、租户不匹配、用户禁用测试：满足。
- 密码不可逆哈希、JWT 密钥不入仓、token 载荷不含密码或密码哈希：代码检查未发现违反。
- 租户隔离测试：满足。
- 身份审计记录覆盖成功登录、失败登录和当前用户失败场景：满足。
- 契约变更只限认证相关补齐：满足；Fix Pass 1 未修改 `packages/shared-contracts`。
- 未新增工单、审批、Agent、RAG、前端、网关或通知业务实现：满足。
- 未新增高风险动作执行能力：满足。

## AI / RAG 评测检查

本阶段不是 Agent 或 RAG 阶段。Fix Pass 1 没有新增 Agent 工具、RAG 检索、模型调用、引用生成或 AI eval 逻辑；未发现 Agent 直接执行高风险动作或 RAG 无引用结论的问题。

## 验证命令与结果

- `git status --short --untracked-files=all`
  - 结果：已执行。工作区仍有大量未跟踪 harness / skeleton / Phase 003 文件、Phase 002 相关 dirty 文件，以及仓库外删除项；这些状态已在实现与 Fix handoff 中声明，本复审未清理、未 stage、未提交。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1`
  - 结果：通过，输出 `Phase 002 contract validation passed.`。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-identity-service.ps1`
  - 结果：通过，内部运行 Maven 测试，输出 `Phase 003 identity-service validation passed.`。
- `mvn test`
  - 运行目录：`services/identity-service`。
  - 结果：串行重跑通过，8 个测试，0 失败，0 错误。
  - 备注：复审过程中曾将独立 `mvn test` 与 `validate-identity-service.ps1` 并行执行，二者同时写入同一 `target` 目录导致一次编译竞争失败；随后串行重跑通过，因此不判定为实现缺陷。
- 越界扫描
  - 命令：扫描 `apps`、`ai-services`、`services\ticket-service`、`services\workflow-service`、`services\notification-service` 下的框架项目文件、源码目录、迁移目录、路由、控制器和组件目录。
  - 结果：无输出，未发现 Phase 003 新增越界业务实现文件。

## 是否允许进入 Window 4

允许。Phase 003 Fix Pass 1 已关闭初次评审的 require fixes findings，当前复审结论为 `approve`，可进入 Window 4 交接。
