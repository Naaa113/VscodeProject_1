# Phase 003 初次评审

## 评审结论

require fixes

不允许进入 Window 4。当前实现已经完成 `identity-service` 的最小可运行闭环，验证命令均通过，但仍存在审计生命周期和错误响应契约不满足 Phase 003 架构验收的问题。修复后应回到 Window 2 Fix Pass，再由 Window 3 复审。

## 当前评审模式

- 模式：初次评审。
- 输出文件：`docs/harness/handoffs/phase-003-review.md`。
- 自动恢复依据：`docs/harness/handoffs/phase-003-implementation.md` 存在，`docs/harness/handoffs/phase-003-final.md` 不存在，且启动时不存在 `docs/harness/handoffs/phase-003-review.md`。

## 已读取 handoff 文件

- `docs/harness/handoffs/steering-decision-phase-003.md`
- `docs/harness/handoffs/phase-003-architect.md`
- `docs/harness/handoffs/phase-003-implementation.md`

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

## 问题清单

### P1：部分登录失败生命周期没有写入身份审计

Phase 003 architect 要求 `validation_failed` 返回 `VALIDATION_FAILED` 或 `TENANT_REQUIRED`，`token_issue_failed` 返回 `INTERNAL_ERROR`，并明确“登录成功和失败都必须形成身份审计记录”。当前 `AuthService.login` 在请求为空或 `tenant_id` 为空时直接抛出 `TENANT_REQUIRED`，没有调用 `repository.audit`；token 签发失败也没有被 `AuthService` 捕获并记录为登录失败审计。

证据：

- `docs/harness/handoffs/phase-003-architect.md:289` 要求 `validation_failed` 返回 `VALIDATION_FAILED` 或 `TENANT_REQUIRED`。
- `docs/harness/handoffs/phase-003-architect.md:292` 要求 `token_issue_failed` 必须记录内部审计。
- `docs/harness/handoffs/phase-003-architect.md:293` 要求登录成功和失败都形成身份审计记录。
- `services/identity-service/app/main/java/com/agentops/identity/AuthService.java:16` 到 `services/identity-service/app/main/java/com/agentops/identity/AuthService.java:18` 在租户缺失时直接抛错，没有审计。
- `services/identity-service/app/main/java/com/agentops/identity/AuthService.java:42` 到 `services/identity-service/app/main/java/com/agentops/identity/AuthService.java:43` 只在 token 签发成功后写成功审计，没有 token 签发失败审计分支。
- `services/identity-service/app/test/java/com/agentops/identity/AuthServiceTest.java:69` 到 `services/identity-service/app/test/java/com/agentops/identity/AuthServiceTest.java:78` 只断言错误码，没有断言租户缺失登录失败会产生审计记录。

影响：

- transition 不满足登录失败生命周期。
- authority 不满足身份审计事实必须由 `identity-service` 完整落地的要求。
- behavior 不满足 Window 1 acceptance 中“身份审计记录覆盖成功登录、失败登录和当前用户失败场景”的验收。

### P2：HTTP 方法错误没有使用统一错误响应形状

Phase 003 architect 要求所有失败响应携带 `trace_id` 和 `retryable`，统一错误响应字段为 `error_code`、`message`、`trace_id`、`retryable`。当前 `/api/auth/login` 和 `/api/auth/me` 的非预期 HTTP 方法返回 `{"message":"method_not_allowed"}`，缺少 `error_code`、`trace_id` 和 `retryable`。

证据：

- `docs/harness/handoffs/phase-003-architect.md:159` 要求所有失败响应携带 `trace_id` 和 `retryable`。
- `docs/harness/handoffs/phase-003-architect.md:368` 冻结统一错误响应字段。
- `services/identity-service/app/main/java/com/agentops/identity/IdentityHttpServer.java:47` 到 `services/identity-service/app/main/java/com/agentops/identity/IdentityHttpServer.java:50` 对登录接口方法错误返回非统一形状。
- `services/identity-service/app/main/java/com/agentops/identity/IdentityHttpServer.java:63` 到 `services/identity-service/app/main/java/com/agentops/identity/IdentityHttpServer.java:66` 对当前用户接口方法错误返回非统一形状。

影响：

- contract 和 behavior 对统一错误响应的承诺不完整。
- 当前测试只覆盖了 `GET /api/auth/me` 缺租户的统一错误形状，没有覆盖方法错误分支。

### P2：演示数据种子逻辑位于默认运行入口，未与生产路径隔离

Phase 003 architect 禁止 hardcoded tenant helper、自动创建用户或租户 fallback，并要求测试 fixture 或 test profile 必须与生产路径隔离，不能被默认运行配置当成真实能力。当前默认应用入口总是调用 `seedDemoData(config.demoLoginSecret())`；只要设置 `IDENTITY_DEMO_LOGIN_SECRET`，主运行路径就会写入硬编码的 `tenant_demo`、`tenant_other`、`demo.user` 和角色权限数据。

证据：

- `docs/harness/handoffs/phase-003-architect.md:200` 到 `docs/harness/handoffs/phase-003-architect.md:208` 禁止硬编码租户 helper、自动创建用户/租户 fallback，并要求测试能力与生产路径隔离。
- `services/identity-service/app/main/java/com/agentops/identity/IdentityApplication.java:11` 到 `services/identity-service/app/main/java/com/agentops/identity/IdentityApplication.java:13` 默认运行入口初始化仓库后调用演示数据种子逻辑。
- `services/identity-service/app/main/java/com/agentops/identity/H2IdentityRepository.java:44` 到 `services/identity-service/app/main/java/com/agentops/identity/H2IdentityRepository.java:57` 在服务代码中写入硬编码租户、用户和角色。
- `services/identity-service/README.md:29` 将 `IDENTITY_DEMO_LOGIN_SECRET` 作为本地运行环境变量说明，但实现没有独立的 test profile 或 smoke-only 入口隔离。

影响：

- belongs 基本仍在 `identity-service` 内，但 transition 和 authority 存在风险：默认运行路径可能把演示身份数据当作真实身份事实。
- 这不等同于已经泄露真实密钥或真实客户数据，但不满足 architect 对测试/演示能力隔离的边界要求。

## 验收检查

- 最小 Java 身份服务：满足。
- `POST /api/auth/login` 成功路径：满足。
- `GET /api/auth/me` 成功路径：满足。
- 统一错误响应：部分满足；方法错误分支未满足。
- 登录失败、token 过期、租户缺失、租户不匹配、用户禁用测试：部分满足；错误码测试存在，但登录租户缺失审计未覆盖。
- 密码不可逆哈希：满足。
- token 载荷不含密码、密码哈希、邮箱、手机号或客户数据：代码检查未发现违反。
- 租户隔离测试：满足。
- 身份审计记录覆盖成功登录、失败登录和当前用户失败场景：部分满足；部分登录失败分支缺审计。
- 契约变更只限认证相关补齐：Phase 003 范围内满足；但当前工作区仍包含 Phase 002 相关 dirty 变更，来源需由后续 handoff 继续诚实记录。
- 未新增工单、审批、Agent、RAG、前端、网关或通知业务实现：满足。
- 未新增高风险动作执行能力：满足。

## 五类边界结论

- belongs：主体变更位于 `services/identity-service`、认证 OpenAPI、认证示例、本地验证脚本和身份测试范围内；未发现其他业务 host 新增 Phase 003 运行时代码。
- authority：身份事实归属基本正确，但演示数据种子逻辑在默认运行路径写入硬编码身份事实，需要隔离。
- contract：认证 OpenAPI 补齐方向正确；统一错误响应仍有方法错误分支缺口。
- transition：登录失败审计生命周期不完整，不能进入 Window 4。
- behavior：主要认证行为可运行，但审计和失败响应验收未完全满足。

## AI / RAG 评测检查

本阶段不是 Agent 或 RAG 阶段，没有新增 Agent 工具、RAG 检索、模型调用、引用生成或 AI eval 逻辑。未发现 Agent 可直接执行高风险动作或 RAG 无引用结论的问题。

## 验证命令与结果

- `git status --short --untracked-files=all`
  - 结果：通过执行；工作区仍有大量未跟踪文件、Phase 002 相关修改，以及仓库外 `../demo_bailian_memory.py`、`../server.py` 删除项。实现 handoff 已声明这些是启动前既有状态，本 review 未尝试清理。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1`
  - 结果：通过，输出 `Phase 002 contract validation passed.`。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-identity-service.ps1`
  - 结果：通过，内部运行 Maven 测试，输出 `Phase 003 identity-service validation passed.`。
- `mvn test`
  - 运行目录：`services/identity-service`
  - 结果：通过，6 个测试，0 失败，0 错误。
- 越界扫描
  - 首次按 prompt 原命令手动改写时因 PowerShell 正则转义失败，随后使用等价路径分隔符正则重跑。
  - 结果：无输出，未发现 `apps`、`ai-services`、`services/ticket-service`、`services/workflow-service`、`services/notification-service` 下新增 Phase 003 业务实现文件。

## 是否允许进入 Window 4

不允许。需要 Window 2 Fix Pass 至少关闭以上 P1 和 P2 问题，并在 fix implementation handoff 中说明修复、测试与是否引入新的边界变化。
