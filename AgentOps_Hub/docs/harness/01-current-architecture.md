# 当前架构

## 当前状态

**治理文档 + Phase 007 RAG 最小知识库闭环阶段 / 已落地身份域、工单域、Agent 编排域、前端工作台与 RAG 知识库最小运行时。**

截至 Phase 007 最终交接，仓库已具备最小 monorepo 占位骨架、本地开发说明、ADR、骨架验证脚本、`packages/shared-contracts` v1 契约草案和契约校验脚本，并已在 `services/identity-service` 落地最小 Java 身份服务运行时，在 `services/ticket-service` 落地最小 Java 工单服务运行时，在 `ai-services/agent-service` 落地最小 Python Agent 本地运行时，在 `apps/web-console` 落地最小 React + TypeScript 工作台运行时，在 `ai-services/rag-service` 落地最小 Python RAG 本地运行时。身份服务提供登录、当前用户、JWT token、租户上下文、权限摘要、身份审计和本地测试闭环；工单服务提供客户与工单基础模型、工单创建、列表查询、详情读取、租户隔离、工单审计和本地测试闭环；Agent 服务提供投诉分析样例任务、本地图执行、只读工单工具、本地 RAG 知识检索、报告草稿、Agent run / step 记录和最小 eval 闭环；前端工作台提供 Mock-first 的登录联调、工单列表 / 详情、AI 任务查看、Agent 步骤链路和报告预览；RAG 服务提供文档登记、解析、切片、本地确定性轻量索引、租户隔离检索、引用溯源、失败重试和最小 RAG eval。

当前仍未创建 `api-gateway`、`workflow-service`、`notification-service` 的业务运行时，也未创建生产级 Spring Boot、FastAPI、Docker、Kubernetes、审批执行、通知发送或实时推送运行时。Phase 003 的身份服务和 Phase 004 的工单服务均使用最小 Java HTTP 运行时、H2 和 Maven 测试闭环；Phase 005 的 Agent 服务使用 Python 标准库本地运行时、契约化 Mock 工具和本地测试 / eval 闭环；Phase 006 的 `web-console` 使用 React + TypeScript + Vite 本地运行时和显式 Mock adapter；Phase 007 的 `rag-service` 使用 Python 标准库本地运行时、本地 fixture 和确定性轻量索引。五者均属于 MVP 本地基线，不代表生产级 Spring Boot、FastAPI、LangGraph、消息队列、网关联调、SSE、对象存储、pgvector / Milvus 或持久化平台已经完成。

## 已有输入材料

- `项目框架.md`
- `AgentOps_Hub_企业智能运营协同平台_项目主题.docx`
- `项目框架.docx`

这些材料共同定义了 AgentOps Hub 的项目主题、目标角色、业务流程、推荐服务划分、数据库模型、测试体系和阶段落地建议。

## 目标逻辑架构

```text
web-console
  -> api-gateway
     -> identity-service
     -> ticket-service
     -> workflow-service
     -> notification-service
     -> agent-service
     -> rag-service

agent-service
  -> shared contracts
  -> ticket/workflow tools
  -> rag-service
  -> message queue
  -> observability

rag-service
  -> document storage
  -> vector index
  -> document metadata store
```

## 推荐服务边界

| 服务 | 目标职责 | 当前落地状态 |
|---|---|---|
| `web-console` | 运营工作台、任务状态、报告、知识库管理 | Phase 006 最小工作台已落地（Mock-first） |
| `api-gateway` | 统一入口、鉴权、限流、路由 | Phase 001 占位目录已创建，未实现 |
| `identity-service` | 用户、租户、角色、权限、JWT、审计 | Phase 003 最小认证与租户模型已落地 |
| `ticket-service` | 客户、工单、SLA、处理记录和任务关联 | Phase 004 最小工单模型已落地 |
| `workflow-service` | 审批、人机确认、业务动作编排 | Phase 001 占位目录已创建，未实现 |
| `notification-service` | 邮件、站内信、Webhook、通知事件 | Phase 001 占位目录已创建，未实现 |
| `agent-service` | LangGraph 工作流、多 Agent 编排、任务执行状态 | Phase 005 最小 Agent 本地闭环已落地 |
| `rag-service` | 文档解析、切片、Embedding、检索、引用溯源 | Phase 007 最小本地 RAG 闭环已落地，使用本地确定性轻量索引 |
| `analytics-worker` | 异步分析、报表和 AI 评测批处理 | Phase 001 占位目录已创建，未实现 |
| `shared-contracts` | OpenAPI、事件契约、DTO、错误码、状态枚举 | Phase 002 v1 契约草案已创建；Phase 003 到 Phase 007 已补齐认证、工单、Agent Mock 标记、事件记录、文档状态、知识检索和引用相关契约 |

## Phase 001 技术方向

- 前端：React + TypeScript，记录于 `docs/adr/0001-frontend-stack.md`。
- 消息队列：RabbitMQ，记录于 `docs/adr/0002-message-queue.md`。
- 关系数据库：PostgreSQL，记录于 `docs/adr/0003-relational-database.md`。
- 向量检索：Phase 007 为 MVP 选择本地确定性轻量索引作为替代检索方案；生产级 pgvector / Milvus 仍留待后续生产化阶段评估，记录于 `docs/adr/0004-vector-search.md`。
- 实时通道：SSE 优先，记录于 `docs/adr/0005-realtime-channel.md`。

## Phase 002 契约资产

- OpenAPI 草案：`packages/shared-contracts/openapi/agentops-api.v1.yaml`。
- 通用 schema：`packages/shared-contracts/schemas/common.v1.schema.json`、`packages/shared-contracts/schemas/status.v1.schema.json`。
- 错误码：`packages/shared-contracts/errors/error-codes.v1.json`，包含工具下游不可用等失败分类。
- 事件 schema：AI 任务、Agent run/step、文档上传和文档索引相关 v1 事件。
- 工具 schema：`ticket.search.v1`、`ticket.create_followup.v1`、`knowledge.search.v1`、`report.save.v1`。
- 示例与清单：`packages/shared-contracts/examples/**` 与 `packages/shared-contracts/manifest.v1.json`。
- 校验入口：`scripts/validate-contracts.ps1`。

## Phase 003 身份服务资产

- 身份服务运行时：`services/identity-service/**`。
- 认证 API：`POST /api/auth/login` 与 `GET /api/auth/me`。
- 身份域内部 schema：`tenant`、`sys_user`、`sys_role`、`sys_permission`、`sys_user_role`、`sys_role_permission`、`identity_audit_log`。
- token 能力：HMAC-SHA256 JWT 签发与校验，载荷包含 `sub`、`tenant_id`、`roles`、`permissions`、`iat`、`exp`、`jti`。
- 租户与权限：`X-Tenant-Id` 与 token 租户一致性校验，当前用户响应返回角色和权限摘要。
- 审计：登录成功、登录失败、当前用户失败路径写入身份审计。
- 本地校验入口：`scripts/validate-identity-service.ps1`。
- 测试入口：`services/identity-service` 下 `mvn test`，当前覆盖登录、当前用户、租户隔离、token 过期、用户禁用、统一错误响应和测试数据隔离。

## Phase 004 工单服务资产

- 工单服务运行时：`services/ticket-service/**`。
- 工单 API：`POST /api/tickets`、`GET /api/tickets`、`GET /api/tickets/{id}`。
- 工单域内部 schema：`customer`、`ticket`、`ticket_audit_log`。
- 工单能力：创建带客户信息的工单、按时间范围 / 分类 / 优先级 / 状态 / 客户 / 查询词检索工单、读取工单详情。
- 租户与权限：校验 `Authorization: Bearer <token>`、`X-Tenant-Id`、`X-Trace-Id`，要求 token 租户与 header 租户一致，并检查 `ticket:read` / `ticket:write` 权限。
- 隔离与审计：列表和详情按租户隔离，跨租户详情访问返回 `RESOURCE_NOT_FOUND`；创建路径写入 `tenant_id`、`created_by` 和工单审计。
- 契约能力：补齐工单 OpenAPI、工单示例和 `ticket.search.v1` 只读工具契约；未新增工单事件契约。
- 本地校验入口：`scripts/validate-ticket-service.ps1`。
- 测试入口：`services/ticket-service` 下 `mvn test`，当前覆盖创建、列表过滤、详情读取、租户隔离、统一错误响应和客户来源校验。

## Phase 005 Agent 服务资产

- Agent 服务本地运行时：`ai-services/agent-service/**`。
- 本地命令行入口：`python -m agent_service.cli --scenario complaint` 与 `python -m agent_service.cli --scenario eval`。
- 图节点：Planner、Retriever、Data Analyst、Risk、Supervisor、Report 的简化节点。
- 本地状态：`AgentTask`、`AgentRun`、`AgentStep`、`ToolCallRecord`、`ReportDraft` 和 eval 结果。
- 工具边界：`ticket.search.v1` 只读契约化 Mock、`knowledge.search.v1` 显式 Mock 知识来源、`report.save.v1` 本地 Mock 保存。
- 事件记录：`agent.run.started.v1`、`agent.step.completed.v1`、`agent.run.completed.v1`、`agent.run.failed.v1` 均带 v1 envelope。
- 风险边界：高风险或低置信度输出进入 `waiting_human` / `waiting_approval` 占位，不生成动作命令。
- 本地校验入口：`scripts/validate-agent-service.ps1`。
- 测试入口：`ai-services/agent-service` 下 `python -m pytest`，当前覆盖完成路径、失败路径、事件 envelope、工具调用和人工确认占位。

## Phase 006 `web-console` 资产

- 前端工作台运行时：`apps/web-console/**`。
- 本地入口：`npm --prefix apps/web-console run dev`。
- 页面能力：Mock 登录联调、工单列表、工单详情、投诉分析任务查看、Agent 步骤链路和报告预览。
- 前端契约镜像：`apps/web-console/src/api/contracts.ts`，字段来源于 Phase 003 到 Phase 005 已冻结的认证、工单、AI 任务、Agent step、报告和错误响应契约。
- Mock 边界：`apps/web-console/src/mocks/**`，显式区分 Mock 用户、Mock 工单、Mock Agent run / step 和 Mock 报告。
- 报告与风险展示：报告区展示事实、推断、风险、建议和引用；高风险或低置信度结果只进入 `waiting_human` / `waiting_approval` 展示态，不提供审批通过或动作执行入口。
- 本地校验入口：`scripts/validate-web-console.ps1`。
- 测试入口：`apps/web-console` 下 `npm run test`，当前覆盖 Mock client、报告风险分区、步骤错误展示和报告状态分支。

## Phase 007 RAG 服务资产

- RAG 服务本地运行时：`ai-services/rag-service/**`。
- 本地命令行入口：`python -m rag_service.cli --scenario ingest-search`、`python -m rag_service.cli --scenario retry-failed` 与 `python -m rag_service.cli --scenario eval`。
- 文档能力：登记样例 SOP / FAQ，记录 `document_id`、`knowledge_base_id`、`tenant_id`、`filename`、`object_key`、`parse_status`、错误信息和审计上下文。
- 解析与检索：文本解析、切片、本地确定性轻量索引、租户隔离检索、引用溯源和存储无关 `index_ref`。
- Agent 集成：`agent-service` 通过 `knowledge.search.v1` 形状消费本地 RAG 结果，报告引用包含非 Mock 文档片段来源。
- 失败边界：解析失败进入 `failed`，保留 `DOCUMENT_PARSE_FAILED`，重试入口必须携带 `tenant_id`、`requested_by` 和 `trace_id`；跨租户解析或重试返回 `RESOURCE_NOT_FOUND`。
- 本地校验入口：`scripts/validate-rag-service.ps1`。
- 测试入口：`ai-services/rag-service` 下 `python -m pytest`，当前覆盖登记、解析、切片、检索、租户隔离、失败重试和 eval。

## 目标数据域

| 数据域 | 代表实体 | 事实归属 |
|---|---|---|
| 身份权限 | `tenant`, `sys_user`, `sys_role`, `sys_permission` | `identity-service` |
| 工单客户 | `customer`, `ticket`, `ticket_comment`, `ticket_sla_event` | `ticket-service` |
| AI 任务 | `ai_task`, `agent_run`, `agent_step`, `tool_call_log` | Java 建档，Python 执行，契约定义状态 |
| 知识库 | `knowledge_base`, `document`, `document_chunk`, `embedding_index_ref` | `rag-service` 与对象/向量存储 |
| 审批动作 | `approval_instance`, `approval_record`, `action_command` | `workflow-service` |
| 报告审计 | `report`, `audit_log`, `model_call_log` | 业务服务与 Agent 服务共同写入，按契约约束 |

## 当前架构风险

- 服务划分已被文档、占位骨架和 Phase 002 契约草案提出；`identity-service` 已通过 Phase 003 最小运行时和测试验证，`ticket-service` 已通过 Phase 004 最小运行时和测试验证，`agent-service` 已通过 Phase 005 最小本地运行时、测试和 eval 验证，`web-console` 已通过 Phase 006 最小工作台验证，`rag-service` 已通过 Phase 007 最小本地 RAG 验证；`api-gateway`、`workflow-service`、`notification-service` 仍未通过业务代码或集成测试验证。
- Phase 007 已为 MVP 选择本地确定性轻量索引作为替代检索方案，但生产级 pgvector / Milvus 或其他检索后端仍未选择。
- Agent 与业务服务之间的动作权限、幂等和审批边界已有契约草案表达；Phase 005 已验证高风险阻断占位，但尚未实现真实审批实例、动作命令或写动作执行。
- RAG 引用、幻觉率、工具调用成功率等 AI 质量指标必须继续进入验收；Phase 007 已有最小 RAG eval 样例，但不代表生产代表性语料或生产模型质量已达标。
- `web-console` 已落地最小工作台，但当前仍以 Mock-first 方式消费 AI 任务、Agent 步骤和报告数据，尚未完成真实网关联调、真实 AI 任务 HTTP 入口、真实 SSE 或真实审批动作。
- `identity-service` 仍是最小本地闭环，生产级 Spring Boot、PostgreSQL 迁移、refresh token、集中式 token 撤销和完整 RBAC 管理 API 尚未落地。
- `ticket-service` 仍是最小本地闭环，生产级 Spring Boot、PostgreSQL 迁移、服务间鉴权集成、受控样例数据初始化、数据库分页索引、工单状态变更审计和完整 SLA 引擎尚未落地。
- `agent-service` 仍是最小本地闭环，生产级 LangGraph / FastAPI、真实模型适配、真实 HTTP 工具调用、真实消息队列、持久化存储和完整 JSON Schema 事件校验尚未落地。
- `rag-service` 仍是最小本地闭环，生产级 FastAPI、对象存储、pgvector / Milvus、持久化数据库、异步解析任务和真实上传入口尚未落地。

## 下一步架构动作

1. 基于 Phase 007 已落地的真实本地文档引用，继续推进 Phase 008 人工审批与动作命令，补齐高风险动作的人机协同闭环。
2. 在后续生产化阶段选择 pgvector、Milvus 或其他检索后端，并保持 `index_ref` 与 `knowledge.search.v1` 不破坏。
3. 扩展 RAG / Agent 质量基准集，补齐生产代表性语料、人工标注、幻觉率和工具调用质量指标。
4. 在后续生产化阶段补齐受控本地数据初始化、PostgreSQL 迁移、refresh token、集中式 token 撤销、服务间鉴权、完整 RBAC 管理 API、工单状态变更审计、SLA 引擎、真实 Agent 编排、真实工具集成、网关联调、真实上传入口和事件校验。
