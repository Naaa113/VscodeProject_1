# Phase 001 Architect Handoff

## Recovery Check

- Handoffs listed before this document: `phase-000-harness-baseline.md`, `steering-decision-phase-001.md`.
- Latest steering decision: `docs/harness/handoffs/steering-decision-phase-001.md`.
- Approval status: approved by user on 2026-05-31.
- Current state: Phase 001 Monorepo 骨架与本地开发基线, steering state `phase_selected`.
- Existing `phase-001-architect.md`: none before this write, so this is the first Phase 001 architecture handoff.
- Conflict assessment: no blocking conflict. The steering decision records Phase 000 recovery history, while `current-state.md` has already advanced to Phase 001.

## 1. 本阶段目标

Phase 001 只建立 AgentOps Hub 的最小 monorepo 骨架与本地开发基线，让后续 Phase 002+ 可以在稳定目录、命令、文档和技术选择记录上推进。

本阶段完成后应具备：

- 与 host ownership 一致的顶层目录和 host 占位目录。
- 本地开发说明、环境变量模板、验证命令约定和占位测试策略。
- 技术选择记录位置与 Phase 001 必须收敛的 MVP 决策。
- 不含业务 API、前端页面、数据库迁移、Agent workflow 或真实模型接入。

## 2. 允许修改的文件范围

Window 2 仅允许新增或修改以下范围：

- 根目录基础文件：`README.md`, `CONTRIBUTING.md`, `.gitignore`, `.editorconfig`, `.env.example`。
- 本地验证脚本：`scripts/validate-skeleton.ps1` 或同等只读检查脚本。
- 开发文档：`docs/development/README.md`, `docs/development/local-dev.md`。
- ADR 文档：`docs/adr/0001-frontend-stack.md`, `docs/adr/0002-message-queue.md`, `docs/adr/0003-relational-database.md`, `docs/adr/0004-vector-search.md`, `docs/adr/0005-realtime-channel.md`。
- host 占位目录及其 `README.md` / `.gitkeep`：
  - `apps/web-console`
  - `apps/api-gateway`
  - `services/identity-service`
  - `services/ticket-service`
  - `services/workflow-service`
  - `services/notification-service`
  - `ai-services/agent-service`
  - `ai-services/rag-service`
  - `ai-services/analytics-worker`
  - `packages/shared-contracts`
  - `packages/prompt-templates`
  - `packages/test-fixtures`
  - `deploy`
  - `tests`
- 可选测试骨架目录：`tests/contract`, `tests/integration`, `tests/e2e`, `tests/smoke`，只允许放 README 或占位文件。
- 必要时更新 `docs/harness/state/current-state.md` 的 Phase 001 交接状态，但不得改变项目章程、权威矩阵或 backlog。

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

例外：如果发现这些治理文档与 Phase 001 无法兼容，Window 2 必须停止并交回 Window 0 或用户确认，不得自行修订。

Window 2 也不得修改或删除用户既有的项目材料、仓库外文件、上游 handoff 文件，以及与 Phase 001 无关的任意文件。

## 4. 受影响 Host 和数据所有权

Phase 001 创建目录身份，不创建运行时 host。因此所有数据所有权仍沿用 `03-host-ownership.md`，不得在本阶段落地业务数据结构。

| Host | Phase 001 允许内容 | 数据所有权状态 |
|---|---|---|
| `apps/web-console` | 前端工作台占位 README | 不拥有业务事实，不直接访问数据库 |
| `apps/api-gateway` | Java gateway 占位 README | 不拥有业务事实 |
| `services/identity-service` | 身份域占位 README | 未来拥有租户、用户、角色、权限 |
| `services/ticket-service` | 工单域占位 README | 未来拥有客户、工单、SLA、处理记录 |
| `services/workflow-service` | 审批动作域占位 README | 未来拥有审批实例、动作命令 |
| `services/notification-service` | 通知域占位 README | 未来拥有通知投递状态 |
| `ai-services/agent-service` | Agent 编排占位 README | 未来拥有 Agent run/step 推理过程 |
| `ai-services/rag-service` | RAG 占位 README | 未来拥有文档解析、切片、索引引用 |
| `ai-services/analytics-worker` | 异步分析占位 README | 未来拥有批处理与 eval 作业过程 |
| `packages/shared-contracts` | 契约包占位 README 和目录约定 | 拥有契约定义，不拥有业务实现 |
| `packages/prompt-templates` | Prompt 包占位 README | 拥有模板与评测样例，不绑定数据库 |
| `packages/test-fixtures` | 测试资产占位 README | 只允许合成样例，不存真实敏感数据 |

## 5. 必须保持稳定的 URL / API / 事件 / 状态 / 行为

Phase 001 不新增业务 URL、REST API、事件、实时通道或数据库状态。

必须保持稳定：

- `04-contract-map.md` 中的 MVP API、事件、工具候选清单只作为候选，不得被实现窗口改写为已落地契约。
- `05-transition-lifetime.md` 中的 AI task、Agent run/step、document、approval、action command 生命周期不得被 Phase 001 重新定义。
- 前端、Java、Python、RAG、workflow 的所有权边界不得被 README 或 ADR 改写。
- 本地命令行为必须清晰：验证命令只检查骨架、文档和禁止项，不启动业务服务。

## 6. 允许新增的目录、文件、class、method 或 schema 类型

允许新增：

- 目录：第 2 节列出的 host 目录、文档目录、测试占位目录和 `scripts`。
- 文件类型：`README.md`, `.gitkeep`, `.env.example`, ADR markdown, 本地骨架验证脚本。
- 契约占位目录：
  - `packages/shared-contracts/openapi`
  - `packages/shared-contracts/events`
  - `packages/shared-contracts/tools`
  - `packages/shared-contracts/examples`
  - `packages/shared-contracts/errors`

不允许新增：

- Java class、Spring configuration、controller、service、repository。
- Python module、FastAPI app、LangGraph node、Pydantic schema。
- React/Vue component、route、store、API client。
- OpenAPI business schema、event payload schema、tool schema、database migration。

## 7. 不允许新增的 helper / adapter / fallback / bridge

Phase 001 禁止新增任何会伪装成业务能力的 helper、adapter、fallback 或 bridge，包括：

- Mock auth helper、fake tenant adapter、temporary permission bridge。
- Mock ticket API、fake workflow adapter、direct database bridge。
- Agent tool adapter、LLM fallback、RAG search fallback。
- SSE/WebSocket bridge 或前端 API client。
- 数据库访问 helper、migration helper、ORM bootstrap。
- 任何跨服务临时调用封装。

如果需要描述未来 helper，只能写入 README 的“future phase”说明，不得创建可执行实现。

## 8. 需要新增或冻结的 OpenAPI / 事件 / 工具契约

Phase 001 不新增业务 OpenAPI、事件或工具契约。

Phase 001 需要冻结的是契约资产的目录和规则：

- `packages/shared-contracts/openapi`: 未来 OpenAPI 文件位置。
- `packages/shared-contracts/events`: 未来事件 schema 文件位置。
- `packages/shared-contracts/tools`: 未来 Agent 工具契约位置。
- `packages/shared-contracts/examples`: 未来成功和失败 payload 示例位置。
- `packages/shared-contracts/errors`: 未来错误码和错误响应映射位置。
- `packages/shared-contracts/README.md`: 必须声明“Phase 001 只有目录与规则，无业务 schema”。

若 Window 2 认为必须放入示例文件，只允许放非业务模板，例如 `README.md` 中的命名规则，不允许出现 `/api/auth/login`、`ai.task.created.v1` 等具体契约 payload。

## 9. 状态生命周期和失败处理要求

Phase 001 的状态对象是“本地开发基线”，不是业务运行时。

生命周期：

```text
not_created
  -> skeleton_created
  -> decisions_recorded
  -> validation_passed
  -> ready_for_review

skeleton_created
  -> validation_failed
decisions_recorded
  -> validation_failed
```

状态要求：

- `skeleton_created`: 所有允许的顶层目录和 host README 已创建。
- `decisions_recorded`: 5 个 ADR 已存在，并明确状态为 `Accepted`、`Proposed` 或 `Deferred`。
- `validation_passed`: 必须运行第 11 节命令并记录结果。
- `validation_failed`: 不得掩盖失败；必须记录失败命令、失败原因和是否范围内可修。
- `ready_for_review`: 不得包含业务代码、框架项目或真实凭据。

失败处理：

- 如果验证脚本发现禁止文件，Window 2 必须停止并删除自身新增的违规文件，或在无法安全处理时交回用户。
- 如果技术选择无法在 Phase 001 收敛，ADR 必须标为 `Deferred` 并说明阻塞原因，不得把未确认方案写成既成事实。
- 如果目录结构与 ownership 文档冲突，停止并请求 Window 0 或用户确认。

## 10. 验收条件

Phase 001 必须满足：

- 仓库包含最小 monorepo 目录：`apps`, `services`, `ai-services`, `packages`, `deploy`, `tests`, `docs`。
- 每个目标 host 至少有 README 或占位文件，且 README 不声明超出 ownership 的职责。
- `packages/shared-contracts` 只有契约位置和规则，不包含业务 schema。
- 5 个 ADR 文件存在：
  - 前端栈：建议收敛为 React + TypeScript，用户批准本 handoff 后才视为 Phase 001 冻结选择。
  - 消息队列：建议收敛为 RabbitMQ 用于 MVP 异步任务与开发基线，用户批准本 handoff 后才视为 Phase 001 冻结选择。
  - 关系数据库：建议收敛为 PostgreSQL，用户批准本 handoff 后才视为 Phase 001 冻结选择。
  - 向量检索：建议 Phase 001 记录为 `Deferred`，在 Phase 002/007 再决定 pgvector、Milvus 或临时替代方案。
  - 实时通道：建议收敛为 SSE 优先，用户批准本 handoff 后才视为 Phase 001 冻结选择。
- 本地开发文档说明如何检查骨架、如何进入后续阶段、如何避免真实密钥入仓。
- 验证脚本或命令能证明未创建 Spring、Vue、React、FastAPI、数据库迁移、业务 API、业务页面、Agent workflow。
- `git status --short` 可清晰展示 Phase 001 新增文件，且没有修改禁止范围。

## 11. 必须运行的验证命令

Window 2 完成后至少运行：

```powershell
git status --short
```

```powershell
Get-ChildItem -Path apps,services,ai-services,packages,deploy,tests,docs/development,docs/adr -Force
```

```powershell
Get-ChildItem -Recurse -Force apps,services,ai-services,packages,deploy,tests |
  Where-Object {
    $_.Name -match '^(pom.xml|build.gradle|settings.gradle|package.json|vite.config.*|next.config.*|vue.config.*|pyproject.toml|requirements.txt|Dockerfile)$' -or
    $_.FullName -match '\\src\\|\\node_modules\\|\\migrations\\|\\alembic\\|\\routes\\|\\controllers\\|\\components\\'
  }
```

The third command must return no files.

If `scripts/validate-skeleton.ps1` is created, also run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-skeleton.ps1
```

## 12. Blocker 停止规则

Window 2 发现以下任一情况必须停止，不得自行扩大范围：

- 需要创建 Spring、Vue、React、FastAPI 项目才能继续。
- 需要写入业务 API、事件 payload、工具 schema 或数据库迁移才能验证。
- 技术选择存在用户或文档冲突，无法在 ADR 中诚实记录。
- 目录结构与 `03-host-ownership.md` 或 `02-authority-matrix.md` 冲突。
- 发现真实密钥、真实客户数据或敏感数据将进入仓库。
- 发现 current-state 与 Phase 001 不一致。
- 需要修改禁止范围内的治理文档。

停止时必须输出：

- blocker 描述。
- 已完成文件列表。
- 未完成验收条件。
- 建议交回 Window 0 还是用户确认。

## Window 2 Entry Contract

Window 2 可以在用户批准本文件后开始实现。实现窗口只能交付 Phase 001 骨架和开发基线，不得进入 Phase 002 契约内容或任何业务功能。
