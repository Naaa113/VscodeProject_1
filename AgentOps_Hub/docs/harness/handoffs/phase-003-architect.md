# Phase 003 架构交接

## 恢复检查

- 本次启动前已列出 `docs/harness/handoffs`，最新 steering decision 为 `docs/harness/handoffs/steering-decision-phase-003.md`。
- `steering-decision-phase-003.md` 明确记录：已由用户在 2026-05-31 批准进入 Phase 003。
- `docs/harness/state/current-state.md` 当前阶段为 Phase 003 `identity-service` 最小认证与租户模型，状态为 `phase_selected`。
- 启动前不存在 `docs/harness/handoffs/phase-003-architect.md`，本文件是 Phase 003 首次架构拆解，不是补充架构或误重复启动。
- 当前 phase 与 `current-state.md` 不冲突；Phase 002 已完成并有 `phase-002-final.md`。

## 拆解顺序

本文件按 `belongs -> authority -> contract -> transition -> behavior` 拆解：

- 归属：先确认 `identity-service` 只属于身份域。
- 权威：再确认用户、租户、角色、权限和身份审计的事实来源。
- 契约：随后冻结认证 API、统一错误、token 载荷、租户上下文和权限摘要。
- 迁移：再定义登录、当前用户、token 过期、租户缺失、权限不足和用户禁用的状态生命周期。
- 行为：最后约束实现窗口的验收、验证命令和阻断处理。

## 1. 本阶段目标

Phase 003 的目标是落地 `services/identity-service` 的最小身份域，让后续 `ticket-service`、`agent-service`、`web-console` 和 `api-gateway` 可以依赖稳定的当前用户、租户上下文、权限摘要和统一错误语义。

本阶段必须交付：

- 最小 Java 身份服务运行时，只限 `identity-service`。
- 租户、用户、角色、权限的最小数据模型和测试数据策略。
- `POST /api/auth/login` 与 `GET /api/auth/me` 的实现，对齐 `packages/shared-contracts` v1 草案。
- JWT token 机制，包含明确的 token 载荷、过期时间、签名配置和敏感信息限制。
- 租户上下文、追踪 ID、统一错误响应和身份审计字段。
- 登录成功、登录失败、token 过期、权限不足、租户缺失、用户禁用、租户不匹配的测试覆盖。
- 本地验证脚本或等价命令说明。

本阶段不实现：

- `ticket-service`、`workflow-service`、`agent-service`、`rag-service`、`web-console` 或 `api-gateway`。
- 复杂 OAuth2/OIDC、多身份提供方、组织架构树、完整权限管理后台、刷新 token、单点登录或验证码。
- 审批实例、动作命令、工单业务、Agent 工具调用、RAG 检索、前端页面或 SSE 推送。
- 高风险业务动作执行能力。

## 2. 允许修改的文件范围

Window 2 仅允许新增或修改以下范围：

- `services/identity-service/**`
- `tests/integration/identity-service/**`
- `tests/smoke/identity-service/**`
- `scripts/validate-identity-service.ps1`
- `packages/shared-contracts/openapi/agentops-api.v1.yaml`，仅限认证相关契约补齐。
- `packages/shared-contracts/examples/openapi/auth-*.v1.json`
- `packages/shared-contracts/errors/error-codes.v1.json`，仅限补充身份域必需且向后兼容的错误码。
- `packages/shared-contracts/manifest.v1.json`，仅限登记新增认证示例。
- `docs/development/local-dev.md`，仅限补充 `identity-service` 本地运行和测试命令。
- `docs/harness/handoffs/phase-003-implementation.md`

允许在 `services/identity-service` 内新增最小 Java 服务项目文件、源代码、测试代码、配置模板、数据库 schema 初始化或迁移资产。该许可只覆盖 Phase 003 已批准的身份服务运行时，不代表允许创建其他 Spring、前端、Python 或网关项目。

如果 Window 2 需要修改其他文件，必须先停止并交回 Window 0 或用户确认。

## 3. 禁止修改的文件范围

Window 2 不得修改：

- `docs/harness/00-project-charter.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/11-cycle-runbook.md`
- `apps/**`
- `services/ticket-service/**`
- `services/workflow-service/**`
- `services/notification-service/**`
- `ai-services/**`
- `packages/prompt-templates/**`
- `packages/test-fixtures/**`，除非只引用已有占位说明且不放真实敏感数据。
- `deploy/**`

例外：Window 4 后续交接可按协议更新治理文档；Window 2 不得抢先做这些更新。

## 4. 受影响 host 和数据所有权

| Host | Phase 003 影响 | 数据所有权要求 |
|---|---|---|
| `services/identity-service` | 新增最小身份服务运行时、认证 API、token 签发、身份数据模型和身份审计 | 拥有租户、用户、角色、权限、用户状态、密码哈希、身份审计事实 |
| `packages/shared-contracts` | 只允许补齐认证契约、认证示例和身份错误码 | 拥有跨 host 契约，不拥有业务事实 |
| `tests/integration/identity-service` | 新增身份服务集成测试 | 不拥有生产数据，只验证身份服务行为 |
| `tests/smoke/identity-service` | 新增最小冒烟验证 | 不拥有业务事实 |
| `docs/development/local-dev.md` | 补充身份服务本地命令 | 只记录开发说明 |

数据所有权必须保持：

- `tenant`、`sys_user`、`sys_role`、`sys_permission`、用户角色关系、角色权限关系归 `identity-service`。
- 密码只允许以不可逆哈希形式存储，明文密码不得落盘、不得写日志、不得进入示例。
- `tenant_id`、`user_id`、`roles`、`permissions` 可以作为跨服务上下文传递，但其他 host 不得复制身份事实表。
- 审计字段中的 `created_by`、`updated_by` 可以引用身份用户 ID，但不改变数据事实归属。
- 后续服务只能消费身份上下文和权限摘要，不得私自解释密码、角色关系或用户状态。

## 5. 必须保持稳定的 URL / API / 事件 / 状态 / 行为

### OpenAPI 路径

本阶段必须实现并保持稳定：

- `POST /api/auth/login`
- `GET /api/auth/me`

本阶段不得实现其他业务 API。若为本地健康检查新增 `/actuator/health` 或等价内部健康端点，该端点不属于跨服务业务契约，不得写入 `agentops-api.v1.yaml` 的 MVP 业务 API 清单。

### 请求与响应

`POST /api/auth/login` 必须遵守：

- 请求体使用 `LoginRequest`：`tenant_id`、`username`、`password`。
- 成功响应使用 `LoginResponse`：`tenant_id`、`user_id`、`access_token`、`expires_at`。
- 失败响应使用统一 `ErrorResponse`。

`GET /api/auth/me` 必须遵守：

- 请求必须携带 `Authorization: Bearer <token>`。
- 请求必须携带或可从 token 校验出 `tenant_id`；若同时存在 `X-Tenant-Id`，必须与 token 中的租户一致。
- 成功响应使用 `CurrentUserResponse`：`tenant_id`、`user_id`、`username`、`roles`、`permissions`。
- 失败响应使用统一 `ErrorResponse`。

### 错误码

本阶段必须使用已有错误码：

- `AUTH_INVALID_CREDENTIALS`
- `AUTH_TOKEN_EXPIRED`
- `AUTH_FORBIDDEN`
- `TENANT_REQUIRED`
- `VALIDATION_FAILED`
- `INTERNAL_ERROR`

如需新增身份域错误码，只允许做向后兼容新增，并必须补充示例和校验；不得改变已有错误码含义。

### 事件和工具

本阶段不新增跨服务事件，不新增 Agent 工具契约，不发布 RabbitMQ 消息。身份审计只在 `identity-service` 内部落地，直到后续阶段明确审计事件契约。

### 必须稳定的行为

- 登录失败不得泄露用户名是否存在、租户是否存在、用户是否禁用或密码是否错误的差异。
- token 不得包含密码哈希、明文密码、邮箱、手机号或真实客户数据等敏感信息。
- token 载荷至少包含 `sub`、`tenant_id`、`roles`、`permissions`、`iat`、`exp`，建议包含 `jti`。
- token 过期必须返回 `AUTH_TOKEN_EXPIRED`，并保持统一错误形状。
- 用户禁用后不得通过 `GET /api/auth/me`，即使 token 尚未过期。
- 租户缺失必须返回 `TENANT_REQUIRED`。
- 租户不匹配必须返回 `AUTH_FORBIDDEN`。
- 权限摘要为空时仍可返回空数组；不得返回 `null` 替代数组。
- 所有失败响应必须携带 `trace_id` 和 `retryable`。

## 6. 允许新增的目录、文件、class、method 或 schema 类型

允许在 `services/identity-service` 下新增：

- Java 应用入口。
- 认证 controller 或等价 HTTP handler。
- 身份领域 model：`Tenant`、`User`、`Role`、`Permission`。
- 关系 model：用户角色、角色权限。
- repository 或等价持久化边界。
- service：认证、token、当前用户、权限摘要、密码校验、审计记录。
- security 组件：JWT 签发、JWT 校验、请求上下文解析。
- DTO 映射：只映射 `LoginRequest`、`LoginResponse`、`CurrentUserResponse` 和统一错误响应。
- 配置模板：本地端口、数据库连接、JWT 密钥占位、token 过期时间。
- 测试：单元测试、集成测试、冒烟测试。
- 数据库 schema 初始化或迁移资产，范围只限身份域表。

允许在 `packages/shared-contracts` 中补充：

- `bearerAuth` 或等价 bearer token 安全声明。
- `GET /api/auth/me` 的 `Authorization` 要求。
- `401`、`403`、`400` 的认证失败响应说明。
- `auth-me` 成功和失败示例。
- 身份域错误示例。

不允许新增：

- 工单、审批、Agent、RAG、通知或前端 class、method、schema、route、component。
- `api-gateway` 鉴权实现。
- 跨服务事件 schema。
- Agent 工具 schema。
- 运行时 mock server。
- 生产部署 chart、Dockerfile 或 Kubernetes manifest。

## 7. 不允许新增的 helper / adapter / fallback / bridge

Phase 003 禁止新增以下临时层或绕行能力：

- fake auth helper、hardcoded tenant helper、static token helper。
- 绕过 JWT 校验的 debug adapter。
- 让 `api-gateway` 代管身份事实的 bridge。
- 让 `ticket-service`、`workflow-service` 或 `agent-service` 复制用户、角色、权限表的 adapter。
- 登录失败时自动创建用户、自动创建租户或自动赋权的 fallback。
- 内存用户库作为生产路径 fallback。
- 明文密码验证 fallback。
- Agent 工具调用身份服务的临时 bridge。
- 未经契约登记的私有 `/internal/auth/*` 跨服务 API。

测试可以使用 fixture 或 test profile，但必须与生产路径隔离，并且不得被默认运行配置当成真实能力。

## 8. 需要新增或冻结的 OpenAPI / 事件 / 工具契约

### OpenAPI

冻结并实现：

- `POST /api/auth/login`
- `GET /api/auth/me`
- `LoginRequest`
- `LoginResponse`
- `CurrentUserResponse`
- `ErrorResponse`
- `X-Tenant-Id`
- `X-Trace-Id`

允许补齐：

- `Authorization: Bearer <token>` 安全声明。
- `GET /api/auth/me` 的 `403` 响应。
- `POST /api/auth/login` 的 `400` 响应。
- `auth-me.success.v1.json`、`auth-me.token-expired.v1.json`、`auth-me.forbidden.v1.json` 或等价示例。

不得修改：

- 工单、AI 任务、知识库、SSE、事件、工具相关契约。
- 已有字段名的含义。
- `LoginRequest`、`LoginResponse`、`CurrentUserResponse` 的必填字段集合，除非 Window 2 停止并交回契约评审。

### 事件

本阶段冻结为“不新增事件”。如果 Window 2 认为必须发布 `identity.*` 事件，必须停止并交回 Window 0 或用户确认。

### 工具

本阶段冻结为“不新增工具”。如果 Window 2 认为 Agent 需要身份工具，必须停止并交回 Window 0 或用户确认。

### 数据库 schema

数据库 schema 不等于跨服务契约，但本阶段允许在 `identity-service` 内部新增最小表：

- `tenant`
- `sys_user`
- `sys_role`
- `sys_permission`
- `sys_user_role`
- `sys_role_permission`
- `identity_audit_log` 或等价身份审计表

字段必须支持租户隔离、用户状态、密码哈希、创建时间、更新时间和审计操作者。表名可按实现语言和框架习惯调整，但事实归属不得改变。

## 9. 状态生命周期和失败处理要求

### 登录生命周期

```text
request_received
  -> tenant_resolved
  -> credentials_verified
  -> token_issued
```

失败分支：

```text
request_received
  -> validation_failed

tenant_resolved
  -> credentials_rejected

credentials_verified
  -> user_disabled

credentials_verified
  -> token_issue_failed
```

要求：

- `validation_failed` 返回 `VALIDATION_FAILED` 或 `TENANT_REQUIRED`。
- `credentials_rejected` 返回 `AUTH_INVALID_CREDENTIALS`，不得区分租户不存在、用户不存在或密码错误。
- `user_disabled` 在登录场景返回 `AUTH_INVALID_CREDENTIALS`，避免泄露用户状态。
- `token_issue_failed` 返回 `INTERNAL_ERROR`，必须记录内部审计，不得向客户端泄露签名配置。
- 登录成功和失败都必须形成身份审计记录，审计记录不得包含明文密码或 token 全文。

### 当前用户生命周期

```text
request_received
  -> token_parsed
  -> token_validated
  -> tenant_matched
  -> user_loaded
  -> permission_summary_returned
```

失败分支：

```text
request_received
  -> tenant_missing

token_parsed
  -> token_expired

token_validated
  -> token_invalid

tenant_matched
  -> tenant_mismatch

user_loaded
  -> user_disabled
```

要求：

- `tenant_missing` 返回 `TENANT_REQUIRED`。
- `token_expired` 返回 `AUTH_TOKEN_EXPIRED`。
- `token_invalid` 返回 `AUTH_FORBIDDEN` 或 `AUTH_INVALID_CREDENTIALS`，实现窗口必须在 handoff 说明采用哪一个，并保持测试一致。
- `tenant_mismatch` 返回 `AUTH_FORBIDDEN`。
- `user_disabled` 返回 `AUTH_FORBIDDEN`。
- 成功响应中的 `roles` 和 `permissions` 必须是数组。

### token 生命周期

```text
issued -> valid -> expired
issued -> revoked_or_user_disabled
```

要求：

- 本阶段不实现 refresh token。
- 本阶段不要求集中式 token 撤销表；用户禁用必须在 `GET /api/auth/me` 时通过用户状态拦截。
- token 过期时间必须可配置，默认值不得写死在业务代码深处。
- JWT 签名密钥必须来自环境变量或本地配置模板占位，不得提交真实密钥。

### 权限摘要生命周期

```text
roles_loaded -> permissions_loaded -> summary_returned
```

要求：

- 权限摘要只表达字符串权限，例如 `ticket:read`。
- 本阶段不实现权限管理 API。
- 本阶段不做复杂 RBAC 继承、组织架构、数据范围策略。
- 权限不足只在本阶段已有接口需要时返回 `AUTH_FORBIDDEN`；不得提前实现工单或工具权限判断。

## 10. 验收条件

Phase 003 必须满足：

- `services/identity-service` 存在最小可运行 Java 身份服务。
- `POST /api/auth/login` 可用，并返回契约规定的 token 响应。
- `GET /api/auth/me` 可用，并返回当前用户、租户、角色和权限摘要。
- 统一错误响应字段为 `error_code`、`message`、`trace_id`、`retryable`，允许有 `details`。
- 登录失败、token 过期、权限不足、租户缺失、租户不匹配、用户禁用均有测试。
- 密码使用不可逆哈希校验；仓库中没有真实密码、真实 JWT 密钥、真实客户数据或生产 token。
- token 载荷不包含明文密码、密码哈希或真实个人敏感信息。
- 租户隔离被测试覆盖：A 租户 token 不得通过 B 租户请求。
- 身份审计记录覆盖成功登录、失败登录和当前用户失败场景。
- 契约变更只限认证相关补齐，且 `scripts/validate-contracts.ps1` 仍通过。
- 没有新增工单、审批、Agent、RAG、前端、网关或通知业务实现。
- 没有新增高风险动作执行能力。
- Window 2 handoff 必须记录使用的构建工具、运行命令、测试结果、未完成项和新增债务。

## 11. 必须运行的验证命令

Window 2 完成后至少运行：

```powershell
git status --short
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-identity-service.ps1
```

并根据 `services/identity-service` 选择的构建工具运行下列命令之一，必须在 handoff 中说明实际使用项：

```powershell
Set-Location services/identity-service
mvn test
```

或：

```powershell
Set-Location services/identity-service
.\gradlew test
```

还必须运行越界扫描：

```powershell
Get-ChildItem -Recurse apps,ai-services,services\ticket-service,services\workflow-service,services\notification-service -Force |
  Where-Object {
    $_.Name -match '^(pom.xml|build.gradle|settings.gradle|package.json|vite.config.*|next.config.*|vue.config.*|pyproject.toml|requirements.txt|Dockerfile)$' -or
    $_.FullName -match '\\src\\|\\node_modules\\|\\migrations\\|\\alembic\\|\\routes\\|\\controllers\\|\\components\\'
  }
```

最后一个命令不得返回由 Phase 003 新增的业务实现文件。

如果构建或测试需要下载依赖，Window 2 必须按当前环境审批规则请求网络或外部执行权限；不得用跳过测试伪装通过。

## 12. 实现窗口发现阻断项时应该如何停止

Window 2 发现以下任一情况必须停止：

- 无法在 `services/identity-service` 内独立实现认证 API，必须修改其他 host 才能继续。
- 必须修改 `03-host-ownership.md`、`04-contract-map.md` 或 `05-transition-lifetime.md` 才能继续。
- 需要新增跨服务事件、工具契约或网关认证实现。
- 现有 `LoginRequest`、`LoginResponse`、`CurrentUserResponse` 必填字段无法满足实现，且需要破坏性契约变更。
- 无法保证密码哈希、JWT 密钥或 token 不泄露。
- 无法测试租户隔离、token 过期或用户禁用。
- 需要实现 refresh token、OAuth2/OIDC、多身份提供方或权限管理 UI 才能继续。
- 需要引入工单、审批、Agent、RAG 或前端能力才能完成测试。
- 发现真实密钥、真实客户数据或生产敏感数据将进入仓库。
- 发现 `current-state.md` 已经不是 Phase 003。

停止时必须输出：

- 阻断项描述。
- 已完成的文件列表。
- 未完成的验收条件。
- 是否建议交回 Window 0、用户确认或后续 phase 处理。

## Window 2 进入条件

用户批准本文件后，Window 2 才能开始实现 Phase 003。实现窗口只能实现 `identity-service` 最小认证与租户模型，以及必要的认证契约补齐；不得进入其他业务域、前端、Agent、RAG、审批、网关或生产部署能力。
