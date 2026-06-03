# Phase 003 最终交接

## phase status

completed

Phase 003 `identity-service` 最小认证与租户模型已完成。最新复审文件为 `docs/harness/handoffs/phase-003-review-fix-1.md`，结论为 `approve`，允许进入 Window 4。本窗口已完成阶段收尾和状态冻结。

## completed scope

- 落地 `services/identity-service` 最小 Java 身份服务运行时。
- 实现 `POST /api/auth/login` 与 `GET /api/auth/me`。
- 实现 JWT token 签发与校验，token 载荷包含 `sub`、`tenant_id`、`roles`、`permissions`、`iat`、`exp`、`jti`。
- 实现租户上下文校验：`X-Tenant-Id` 与 token 中的 `tenant_id` 不一致时返回 `AUTH_FORBIDDEN`。
- 实现角色和权限摘要返回，数组为空时仍返回空数组。
- 实现统一错误响应：`error_code`、`message`、`trace_id`、`retryable`，允许有 `details`。
- 实现身份审计：登录成功、登录失败、当前用户失败路径均写入身份审计。
- 使用不可逆密码哈希校验，未将明文密码、密码哈希、真实 JWT 密钥、真实客户数据或生产 token 放入仓库。
- 将测试 fixture 与默认运行路径隔离，默认服务启动不写入演示租户或演示用户。
- 新增 `scripts/validate-identity-service.ps1` 作为身份服务本地校验入口。
- 更新本地开发说明，记录身份服务运行和验证命令。

## unchanged contracts

- `LoginRequest`、`LoginResponse`、`CurrentUserResponse` 和 `ErrorResponse` 的字段含义保持稳定。
- 已有错误码含义保持稳定。
- 未新增跨服务事件。
- 未新增 Agent 工具契约。
- 未修改工单、AI 任务、知识库、SSE、事件或工具相关契约。
- 未新增 `ticket-service`、`workflow-service`、`agent-service`、`rag-service`、`web-console`、`api-gateway` 或通知服务运行时能力。

## changed contracts / authority / transition

### contracts

- `packages/shared-contracts/openapi/agentops-api.v1.yaml` 已补齐认证相关契约，包括 bearer token 安全声明、`/api/auth/login` 的失败响应、`/api/auth/me` 的 bearer security 和认证失败响应。
- 新增认证示例：
  - `packages/shared-contracts/examples/openapi/auth-me.success.v1.json`
  - `packages/shared-contracts/examples/openapi/auth-me.token-expired.v1.json`
  - `packages/shared-contracts/examples/openapi/auth-me.forbidden.v1.json`
- `packages/shared-contracts/manifest.v1.json` 已登记新增认证示例。

### authority

- `identity-service` 已成为租户、用户、角色、权限、用户状态、密码哈希和身份审计事实的最小运行时 owner。
- 其他 host 只能消费身份上下文、租户 ID、用户 ID、角色和权限摘要，不得复制或解释身份事实表。
- 密码、token 签名密钥和身份审计仍限制在身份域内。

### transition

- 登录生命周期已覆盖请求校验、租户解析、凭据校验、token 签发、成功审计和失败审计。
- 当前用户生命周期已覆盖 token 解析、token 校验、租户匹配、用户加载、权限摘要返回和失败审计。
- token 生命周期覆盖签发、有效、过期；refresh token 和集中式 token 撤销未进入本阶段。
- 用户禁用后，即使 token 尚未过期，`GET /api/auth/me` 也必须失败。

## validation summary

来自 `phase-003-implementation.md`、`phase-003-fix-1-implementation.md` 和 `phase-003-review-fix-1.md` 的验证记录：

- `mvn test`
  - 运行目录：`services/identity-service`
  - 结果：通过，Fix Pass 1 后为 8 个测试，0 失败，0 错误。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1`
  - 结果：通过，输出 `Phase 002 contract validation passed.`
- `powershell -ExecutionPolicy Bypass -File scripts/validate-identity-service.ps1`
  - 结果：通过，内部运行 Maven 测试并输出 `Phase 003 identity-service validation passed.`
- 越界扫描
  - 结果：无输出，未在 `apps`、`ai-services`、`services/ticket-service`、`services/workflow-service`、`services/notification-service` 下发现 Phase 003 新增越界业务实现文件。
- Window 3 Fix 1 复审
  - 结论：`approve`。
  - 初次评审中的三个 finding 均已关闭。

Window 4 未重新运行测试；本窗口只做阶段收尾文档更新。

## remaining debt

- DEBT-004：pgvector/Milvus 或 MVP 替代检索方案仍未选择。
- DEBT-006：高风险动作审批边界仍未实现，仍是后续 MVP 的 D0 债务。
- DEBT-007：AI 质量指标仍只有概念，没有基准集。
- DEBT-008：观测栈尚未落地。
- DEBT-009：多租户深度隔离策略仍需从 Phase 003 起持续验证。
- DEBT-010：报告存储格式和导出格式未冻结。
- DEBT-011：`identity-service` 仍缺少生产级数据初始化、PostgreSQL 迁移、refresh token、集中式 token 撤销和完整 RBAC 管理 API。

## latest state for Window 0

Window 0 下一次启动时应自动发现：

- 最新 final handoff：`docs/harness/handoffs/phase-003-final.md`
- 当前阶段：Phase 003 `identity-service` 最小认证与租户模型
- 当前阶段状态：`completed`
- Steering 状态：`handoff_done`
- 最新复审：`docs/harness/handoffs/phase-003-review-fix-1.md`
- 最新复审结论：`approve`
- 当前业务代码状态：`services/identity-service` 最小 Java 身份服务已落地；其他业务域仍未落地运行时
- 当前契约状态：Phase 002 v1 契约草案仍是共同语言；Phase 003 已补齐认证相关 OpenAPI 和示例；未新增事件或 Agent 工具

Window 0 不需要用户手动总结 Phase 003。应直接读取 `current-state.md` 和本 final handoff，基于 backlog、债务和用户最新指令选择下一阶段。

## recommended candidate inputs for Window 0

推荐 primary candidate：Phase 004 `ticket-service` 最小工单模型。

候选输入：

- Phase 003 已提供当前用户、租户上下文、权限摘要和身份审计基础。
- Phase 004 可以基于这些身份上下文落地工单事实源、客户和工单基础模型、工单创建/查询/详情、状态、优先级、SLA 字段、租户隔离和审计字段。
- Phase 004 应继续遵守 `packages/shared-contracts` v1，必要契约缺口必须先由 Window 1 提案。
- Phase 004 不应实现 Agent 工作流、RAG、前端页面、审批动作、网关生产鉴权或高风险动作执行。
- 如果用户明确要求优先关闭 DEBT-006，可由 Window 0 评估 Phase 008 作为 fallback，但不得绕过身份、租户、权限和审计上下文。

## files changed in this handoff

- `docs/harness/state/current-state.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-003-final.md`

## commit status

用户未明确要求提交。本窗口未 stage、未 commit。
