# Phase 002 架构交接

## 恢复检查

- 本次启动前已列出 `docs/harness/handoffs`，最新 steering decision 为 `docs/harness/handoffs/steering-decision-phase-002.md`。
- `steering-decision-phase-002.md` 明确记录：已由用户在 2026-05-31 批准进入 Phase 002。
- `docs/harness/state/current-state.md` 当前阶段为 Phase 002 `shared-contracts` / OpenAPI / 事件契约，状态为 `phase_selected`。
- 启动前不存在 `docs/harness/handoffs/phase-002-architect.md`，本文件是 Phase 002 首次架构拆解，不是补充架构或误重复启动。
- 当前 phase 与 `current-state.md` 不冲突；Phase 001 已完成并有 `phase-001-final.md`。

## 拆解顺序

本文件按 `belongs -> authority -> contract -> transition -> behavior` 拆解：

- 归属：先确认 `shared-contracts` 属于契约包，不属于任何业务服务。
- 权威：再确认哪些文件可以成为跨服务权威来源。
- 契约：随后冻结 OpenAPI、事件、工具、错误码、状态枚举和示例。
- 迁移：再对齐状态生命周期、失败、取消、等待人工和重试。
- 行为：最后约束实现窗口的验收、验证命令和阻断处理。

## 1. 本阶段目标

Phase 002 的目标是把跨服务共同语言落到 `packages/shared-contracts`，让后续 Java 服务、Python 服务和前端不再猜字段。

本阶段必须交付：

- OpenAPI 初稿：认证、工单、AI 任务、知识库文档、知识库检索和任务状态流。
- 事件契约初稿：AI 任务创建、Agent 运行、Agent 步骤、文档上传和文档索引。
- 工具契约初稿：工单检索、创建跟进任务请求、知识检索、报告保存。
- 统一错误码、统一错误响应、分页、租户上下文、审计字段和状态枚举。
- 成功示例、失败示例、版本策略、兼容性说明和本地契约校验入口。

本阶段不实现任何服务、页面、数据库迁移、Agent 工作流、RAG 检索逻辑或真实模型接入。

## 2. 允许修改的文件范围

Window 2 仅允许新增或修改以下范围：

- `packages/shared-contracts/README.md`
- `packages/shared-contracts/openapi/**`
- `packages/shared-contracts/events/**`
- `packages/shared-contracts/tools/**`
- `packages/shared-contracts/examples/**`
- `packages/shared-contracts/errors/**`
- `packages/shared-contracts/schemas/**`
- `packages/shared-contracts/manifest.v1.json`
- `scripts/validate-contracts.ps1`
- `tests/contract/README.md`
- `docs/development/local-dev.md` 中与契约校验命令有关的段落
- `docs/adr/0004-vector-search.md`，仅允许补充“Phase 002 保持存储无关契约，向量技术选择继续 deferred”的说明

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
- 任何 `apps/**`、`services/**`、`ai-services/**` 的业务或运行时代码
- 任何数据库迁移、Dockerfile、Kubernetes、Spring、React、FastAPI、LangGraph 或模型调用文件

例外：Window 4 后续交接可按协议更新治理文档；Window 2 不得抢先做这些更新。

## 4. 受影响运行单元和数据所有权

Phase 002 直接修改的运行单元（host，指一个可拥有职责边界的目录或服务单元）只有 `packages/shared-contracts`。其他运行单元只作为契约生产者或消费者被描述，不被实现。

| 运行单元 | Phase 002 影响 | 数据所有权要求 |
|---|---|---|
| `packages/shared-contracts` | 拥有 OpenAPI、事件、工具、错误码、状态枚举、示例和校验规则 | 只拥有契约定义，不拥有业务事实 |
| `apps/web-console` | 未来消费 OpenAPI、错误响应、SSE 状态流和示例 | 不得依赖内部数据库字段 |
| `apps/api-gateway` | 未来暴露统一入口和 SSE 路由 | 不拥有业务事实，不承载 Agent 推理 |
| `services/identity-service` | 未来生产认证与当前用户契约 | 拥有租户、用户、角色、权限和身份审计 |
| `services/ticket-service` | 未来生产工单 API 和工单工具契约 | 拥有客户、工单、SLA 和处理记录 |
| `services/workflow-service` | 未来生产审批和动作命令相关契约 | 拥有审批实例、审批记录和动作命令 |
| `ai-services/agent-service` | 未来消费任务事件和工具契约，生产 Agent 状态事件 | 拥有 Agent 推理过程和运行步骤，不直接写业务事实 |
| `ai-services/rag-service` | 未来生产知识检索和文档索引事件 | 拥有文档解析、切片和索引引用，不充当业务事实源 |

字段归属必须保持清晰：

- `tenant_id`、`created_by`、`updated_by` 是跨服务上下文字段，不改变事实归属。
- 工单事实字段归 `ticket-service`。
- 用户与权限事实字段归 `identity-service`。
- Agent 运行过程字段归 `agent-service`。
- 审批和动作执行字段归 `workflow-service`。
- 文档解析和引用字段归 `rag-service`。

## 5. 必须保持稳定的 URL / API / 事件 / 状态 / 行为

Phase 002 必须冻结以下候选为契约初稿，但不得声明已有服务实现：

### OpenAPI 路径

- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/tickets`
- `POST /api/tickets`
- `GET /api/tickets/{id}`
- `GET /api/ai/tasks`
- `POST /api/ai/tasks`
- `GET /api/ai/tasks/{id}`
- `GET /api/ai/tasks/{id}/steps`
- `GET /api/ai/tasks/{id}/report`
- `GET /api/knowledge/documents`
- `POST /api/knowledge/documents`
- `POST /api/knowledge/search`
- `GET /api/stream/tasks/{id}`

### 事件名称

- `ai.task.created.v1`
- `agent.run.started.v1`
- `agent.step.completed.v1`
- `agent.run.completed.v1`
- `agent.run.failed.v1`
- `document.uploaded.v1`
- `document.indexed.v1`

### 工具名称

- `ticket.search.v1`
- `ticket.create_followup.v1`
- `knowledge.search.v1`
- `report.save.v1`

### 状态枚举

- `ai_task.status`: `pending`, `running`, `waiting_approval`, `success`, `failed`, `cancelled`
- `agent_run.status`: `created`, `started`, `step_running`, `completed`, `failed`
- `agent_step.status`: `pending`, `running`, `success`, `failed`, `skipped`, `waiting_human`
- `approval_instance.status`: `pending`, `approved`, `rejected`, `cancelled`, `expired`
- `action_command.status`: `pending`, `success`, `failed`, `cancelled`
- `document.parse_status`: `uploaded`, `parsing`, `indexed`, `failed`

### 必须稳定的行为

- 所有业务 API 必须表达租户上下文、统一错误响应和审计字段。
- 所有列表接口必须有分页契约。
- 所有事件必须有事件名、版本、事件 ID、发生时间、租户 ID 和追踪 ID。
- 所有工具契约必须声明权限、幂等、审批要求、超时、重试和审计字段。
- `ticket.create_followup.v1` 必须默认表达需要审批或策略检查，不得定义为 Agent 可直接执行高风险动作。
- `GET /api/stream/tasks/{id}` 使用 SSE 优先，与 Phase 001 ADR 对齐；本阶段只定义契约，不实现推送服务。

## 6. 允许新增的目录、文件、class、method 或 schema 类型

允许新增目录：

- `packages/shared-contracts/schemas`
- `packages/shared-contracts/examples/openapi`
- `packages/shared-contracts/examples/events`
- `packages/shared-contracts/examples/tools`

允许新增文件类型：

- `.yaml` 或 `.yml` 的 OpenAPI 文件。
- `.json` 的 JSON Schema、示例和清单文件。
- `.md` 的契约说明。
- `.ps1` 的本地校验脚本。

允许新增 schema 类型：

- 通用 envelope schema：事件信封、工具请求信封、工具响应信封。
- 通用基础 schema：租户上下文、审计字段、分页请求、分页响应、统一错误响应。
- 状态枚举 schema：AI 任务、Agent 运行、Agent 步骤、审批、动作命令、文档解析。
- 领域 DTO schema：认证、当前用户、工单摘要、工单详情、AI 任务、Agent 步骤、报告摘要、文档摘要、知识检索结果。
- 工具 schema：工单检索、创建跟进任务请求、知识检索、报告保存。

不允许新增：

- Java class、method、controller、service、repository。
- Python module、function、FastAPI app、LangGraph node、Pydantic model。
- React component、route、store、API client。
- SQL migration、ORM model、Dockerfile、Kubernetes manifest。

## 7. 不允许新增的 helper / adapter / fallback / bridge

Phase 002 禁止新增任何运行时辅助层，包括：

- API client helper、服务 adapter、工具调用 adapter。
- mock server、fake service、临时 gateway bridge。
- LLM fallback、RAG fallback、向量库 bridge。
- 数据库访问 helper、迁移 helper、ORM bootstrap。
- SSE 或 WebSocket 运行时 bridge。
- 用于绕过审批的动作 fallback。

如果需要表达未来适配方式，只能写入契约说明，不能创建可执行运行时代码。

## 8. 需要新增或冻结的 OpenAPI / 事件 / 工具契约

### OpenAPI 文件

建议 Window 2 创建：

- `packages/shared-contracts/openapi/agentops-api.v1.yaml`

该文件必须包含：

- 认证：`POST /api/auth/login`, `GET /api/auth/me`
- 工单：`GET /api/tickets`, `POST /api/tickets`, `GET /api/tickets/{id}`
- AI 任务：`GET /api/ai/tasks`, `POST /api/ai/tasks`, `GET /api/ai/tasks/{id}`, `GET /api/ai/tasks/{id}/steps`, `GET /api/ai/tasks/{id}/report`
- 知识库：`GET /api/knowledge/documents`, `POST /api/knowledge/documents`, `POST /api/knowledge/search`
- 实时状态：`GET /api/stream/tasks/{id}`，以 SSE 契约表达

每个路径必须有至少一个成功响应和一个失败响应引用。

### 事件 schema

建议 Window 2 创建：

- `packages/shared-contracts/events/ai.task.created.v1.schema.json`
- `packages/shared-contracts/events/agent.run.started.v1.schema.json`
- `packages/shared-contracts/events/agent.step.completed.v1.schema.json`
- `packages/shared-contracts/events/agent.run.completed.v1.schema.json`
- `packages/shared-contracts/events/agent.run.failed.v1.schema.json`
- `packages/shared-contracts/events/document.uploaded.v1.schema.json`
- `packages/shared-contracts/events/document.indexed.v1.schema.json`

每个事件必须包含事件信封和 payload，并明确生产者、消费者和兼容策略。

### 工具契约

建议 Window 2 创建：

- `packages/shared-contracts/tools/ticket.search.v1.schema.json`
- `packages/shared-contracts/tools/ticket.create_followup.v1.schema.json`
- `packages/shared-contracts/tools/knowledge.search.v1.schema.json`
- `packages/shared-contracts/tools/report.save.v1.schema.json`

每个工具必须包含：

- 工具名和版本。
- 输入 schema。
- 输出 schema。
- 权限要求。
- 是否需要人工审批。
- 幂等键规则。
- 审计字段。
- 超时、重试和降级策略。

### 错误码和状态枚举

建议 Window 2 创建：

- `packages/shared-contracts/errors/error-codes.v1.json`
- `packages/shared-contracts/schemas/common.v1.schema.json`
- `packages/shared-contracts/schemas/status.v1.schema.json`

错误码至少覆盖：

- `AUTH_INVALID_CREDENTIALS`
- `AUTH_TOKEN_EXPIRED`
- `AUTH_FORBIDDEN`
- `TENANT_REQUIRED`
- `VALIDATION_FAILED`
- `RESOURCE_NOT_FOUND`
- `CONFLICT`
- `RATE_LIMITED`
- `AGENT_TASK_FAILED`
- `AGENT_TOOL_FORBIDDEN`
- `APPROVAL_REQUIRED`
- `DOCUMENT_PARSE_FAILED`
- `INTERNAL_ERROR`

## 9. 状态生命周期和失败处理要求

Phase 002 必须把 `05-transition-lifetime.md` 的生命周期转化为可校验契约。

### AI 任务

- `pending` 必须包含创建人、租户、原始指令和任务类型。
- `running` 必须能关联 `run_id`。
- `waiting_approval` 必须能关联审批实例和阻塞原因。
- `success` 必须能关联报告或明确无报告原因。
- `failed` 必须包含错误码、错误信息和 `retryable`。
- `cancelled` 必须包含取消人或系统取消原因。

### Agent 运行和步骤

- Agent 运行必须区分 `created`、`started`、`step_running`、`completed`、`failed`。
- Agent 步骤必须记录 `task_id`、`run_id`、`agent_name`、`step_name`、状态、开始时间、结束时间、输出摘要、引用和错误信息。
- `waiting_human` 只能表达等待人工，不得表达自动继续执行高风险动作。

### 文档处理

- 文档契约必须覆盖 `uploaded`、`parsing`、`indexed`、`failed`。
- `indexed` 必须有切片数量和 `index_ref`。
- 检索结果必须能追踪到 `document_id` 和 `chunk_id`。
- `index_ref` 必须保持存储无关，不得绑定 pgvector 或 Milvus。

### 审批和动作命令

- 审批状态必须覆盖 `pending`、`approved`、`rejected`、`cancelled`、`expired`。
- 动作命令状态必须覆盖 `pending`、`success`、`failed`、`cancelled`。
- 动作请求必须有 `idempotency_key`。
- 审批拒绝后不得继续执行对应动作。

### 失败处理

- 所有失败响应必须使用统一错误响应。
- 所有失败事件必须有错误码、可重试标记和追踪 ID。
- 所有工具失败必须区分权限失败、审批缺失、校验失败、下游不可用和未知错误。
- 示例中必须包含至少一个失败 payload，证明错误码和状态字段能落地。

## 10. 验收条件

Phase 002 必须满足：

- `packages/shared-contracts` 中存在 OpenAPI、事件、工具、错误码、状态枚举、示例和清单。
- 每个契约文件都有版本号，且文件名包含 `.v1` 或等价版本标识。
- 每个 OpenAPI 路径都有成功响应和失败响应。
- 每个事件 schema 有事件信封、payload、生产者、消费者和示例。
- 每个工具 schema 有输入、输出、权限、审批、幂等、审计、超时和重试说明。
- 统一错误响应被 OpenAPI、事件失败和工具失败共同引用或保持字段一致。
- 租户上下文、分页和审计字段有共享 schema。
- 状态枚举与 `05-transition-lifetime.md` 一致。
- 知识检索契约保持向量存储无关，不关闭 DEBT-004，除非用户另行批准。
- `ticket.create_followup.v1` 不允许 Agent 直接绕过审批创建高风险动作。
- 本阶段没有新增 Spring、React、FastAPI、数据库迁移、Agent workflow、RAG 实现、mock server 或运行时 adapter。
- 契约校验脚本能在本地运行，并能校验 JSON 文件格式、必需文件存在、示例文件可解析和禁止实现文件不存在。

## 11. 必须运行的验证命令

Window 2 完成后至少运行：

```powershell
git status --short
```

```powershell
Get-ChildItem -Recurse packages/shared-contracts -Force
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1
```

```powershell
Get-ChildItem -Recurse apps,services,ai-services,packages,tests -Force |
  Where-Object {
    $_.Name -match '^(pom.xml|build.gradle|settings.gradle|package.json|vite.config.*|next.config.*|vue.config.*|pyproject.toml|requirements.txt|Dockerfile)$' -or
    $_.FullName -match '\\src\\|\\node_modules\\|\\migrations\\|\\alembic\\|\\routes\\|\\controllers\\|\\components\\'
  }
```

最后一个命令必须不返回任何文件。

如果 Window 2 没有创建 `scripts/validate-contracts.ps1`，必须停止并说明替代校验命令；但默认要求创建该脚本。

## 12. 实现窗口发现阻断项时应该如何停止

Window 2 发现以下任一情况必须停止：

- 需要创建服务运行时代码才能表达契约。
- 需要修改 `03-host-ownership.md`、`04-contract-map.md` 或 `05-transition-lifetime.md` 才能继续。
- OpenAPI、事件或工具契约与数据所有权冲突。
- 高风险动作无法表达审批或策略阻断。
- 租户上下文、审计字段或统一错误响应无法保持一致。
- 向量检索方案必须在 pgvector 和 Milvus 中二选一才能继续。
- 校验脚本需要引入网络依赖或下载外部包才能运行。
- 发现真实密钥、真实客户数据或生产敏感数据将进入示例。
- 发现 `current-state.md` 已经不是 Phase 002。

停止时必须输出：

- 阻断项描述。
- 已完成的契约文件列表。
- 未完成的验收条件。
- 是否建议交回 Window 0、用户确认或后续 phase 处理。

## Window 2 进入条件

用户批准本文件后，Window 2 才能开始实现 Phase 002。实现窗口只能创建和校验契约资产，不得进入任何业务服务、前端页面、Agent 工作流或 RAG 实现。
