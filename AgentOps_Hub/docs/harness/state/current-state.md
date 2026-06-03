# 当前状态

## 快照

- 项目：AgentOps Hub 企业智能运营协同平台
- 日期：2026-06-03
- 当前阶段：Phase 007 RAG 最小知识库闭环，状态为 `completed`
- Steering 状态：`handoff_done`
- 架构状态：Phase 007 已完成 Window 4 交接；身份域、工单域、Agent 编排域、前端工作台和 RAG 知识库最小运行时已落地，网关、审批、通知等业务域仍未创建运行时
- 业务代码状态：已创建 `services/identity-service` 最小 Java 身份服务、`services/ticket-service` 最小 Java 工单服务、`ai-services/agent-service` 最小 Python Agent 本地运行时、`apps/web-console` 最小 React 工作台和 `ai-services/rag-service` 最小 Python RAG 本地运行时；`api-gateway`、`workflow-service`、`notification-service` 仍无运行时
- 最新最终交接：`docs/harness/handoffs/phase-007-final.md`
- 最新 Steering 决策：`docs/harness/handoffs/steering-decision-phase-007.md`
- 最新 Review 结论：`docs/harness/handoffs/phase-007-review-fix-1.md`，结论为 `approve`

## 已读取源材料

- `项目框架.md`
- `AgentOps_Hub_企业智能运营协同平台_项目主题.docx`
- `项目框架.docx`

## 当前决策

- Harness 已基于 AgentOps Hub 项目材料新建，不复用其他项目内容。
- Phase 001 已完成 Window 4 交接。
- Phase 002 已完成 Window 4 交接。
- Phase 003 已完成 Window 4 交接。
- Phase 004 已完成 Window 4 交接。
- Phase 005 已完成 Window 4 交接。
- Phase 006 已完成 Window 4 交接。
- Phase 007 已完成 Window 4 交接。
- MVP 优先顺序：
  1. monorepo 骨架与本地开发基线
  2. `shared-contracts` / OpenAPI / 事件契约
  3. `identity-service` 最小认证与租户模型
  4. `ticket-service` 最小工单模型
  5. `agent-service` 最小 LangGraph 闭环
  6. `web-console` 最小工作台
  7. RAG 最小知识库闭环
- Java 服务拥有业务事实、审批、动作和审计。
- Python 服务拥有 Agent 编排、RAG、AI 推理过程和 AI eval。
- 高风险业务动作必须经过审批或策略阻断。
- Phase 001 技术方向已记录为 ADR：
  - 前端：React + TypeScript，状态为 `Accepted`。
  - 消息队列：RabbitMQ，状态为 `Accepted`。
  - 关系数据库：PostgreSQL，状态为 `Accepted`。
  - 向量检索：Phase 007 已为 MVP 选择本地确定性轻量索引作为替代检索方案；生产级 pgvector / Milvus 仍留待后续生产化阶段评估。
  - 实时通道：SSE 优先，状态为 `Accepted`。
- Phase 002 已冻结 `packages/shared-contracts` v1 契约草案；这些契约是后续实现的共同语言，不代表所有服务已实现。
- Phase 003 已基于认证契约落地 `identity-service` 最小认证与租户模型。
- Phase 004 已基于工单契约落地 `ticket-service` 最小工单模型。
- Phase 005 已基于 Agent 事件、工具和报告契约落地 `agent-service` 最小本地 Agent 编排闭环。
- Phase 006 已基于 Phase 003 到 Phase 005 的公开契约与显式 Mock adapter 落地 `apps/web-console` 最小工作台。
- Phase 007 已基于文档、知识检索、引用和 RAG eval 契约落地 `ai-services/rag-service` 最小本地知识库闭环，并让 `agent-service` 通过 `knowledge.search.v1` 消费非 Mock 本地文档引用。

## 当前仓库事实

- 已创建 `apps/`、`services/`、`ai-services/`、`packages/`、`deploy/`、`tests/` 的 Phase 001 占位骨架。
- 已创建 `docs/development/`、`docs/adr/`、`scripts/validate-skeleton.ps1` 作为本地开发基线。
- `docs/harness` 仍是治理资产。
- `packages/shared-contracts` 已包含 Phase 002 契约草案：OpenAPI、事件 schema、工具 schema、错误码、状态枚举、示例、清单和契约校验脚本。
- `services/identity-service` 已提供登录、当前用户、JWT token、租户上下文、权限摘要、统一错误响应和身份审计；本地验证入口为 `scripts/validate-identity-service.ps1`。
- `services/ticket-service` 已提供工单创建、列表、详情、客户信息、优先级、分类、SLA、租户隔离、统一错误响应和工单审计；本地验证入口为 `scripts/validate-ticket-service.ps1`。
- `ai-services/agent-service` 已提供投诉分析样例任务、本地图执行、`ticket.search.v1` 契约化 Mock、通过 `knowledge.search.v1` 消费本地 RAG 检索结果、本地 Mock `report.save.v1`、Agent run / step 事件记录和最小 eval；本地验证入口为 `scripts/validate-agent-service.ps1`。
- `ai-services/rag-service` 已提供文档登记、解析、切片、本地确定性轻量索引、租户隔离检索、引用溯源、解析失败重试和最小 RAG eval；本地验证入口为 `scripts/validate-rag-service.ps1`。
- `apps/web-console` 已提供最小 React + TypeScript + Vite 工作台运行时，包含：
  - Mock 登录联调。
  - 工单列表与工单详情。
  - 投诉分析任务入口与任务详情页。
  - Agent 步骤时间线、错误展示和引用展示。
  - 报告预览的事实、推断、风险、建议和引用分区。
  - 报告区的 `waiting_approval`、`not_ready`、`not_found`、`error` 独立状态分支。
  - 显式 Mock adapter 与 Mock fixture 边界。
- `scripts/validate-web-console.ps1` 已作为 Phase 006 前端验证入口。
- Phase 006 评审链路为：初次 review `require fixes`，Fix Pass 1 后复审仍为 `require fixes`，Fix Pass 2 后复审结论为 `approve`。
- `scripts/validate-contracts.ps1` 已在 Phase 007 调整为阶段感知规则，不再把已批准的 `apps/web-console`、`agent-service`、`rag-service` 判定为 forbidden implementation artifact。
- Phase 007 评审链路为：初次 review `require fixes`，Fix Pass 1 后复审结论为 `approve`。
- 工作区在多个窗口推进期间保持 dirty 状态，包含大量未跟踪 harness、骨架、契约和服务文件，以及仓库外删除项；Window 4 未 stage、未提交、未清理这些既有变更。

## Phase 003 完成范围

- `identity-service` 拥有租户、用户、角色、权限、用户状态、密码哈希和身份审计事实。
- 已实现登录成功、登录失败、token 过期、token 无效、租户缺失、租户不匹配、用户禁用和当前用户查询失败路径。
- 登录失败不泄露租户是否存在、用户是否存在、密码是否错误或用户是否禁用。
- token 载荷包含 `sub`、`tenant_id`、`roles`、`permissions`、`iat`、`exp`、`jti`，不包含明文密码、密码哈希、邮箱、手机号或客户数据。
- 默认运行入口不写入演示租户或演示用户；测试数据只由测试 fixture 创建。

## Phase 004 完成范围

- `ticket-service` 拥有客户、工单、工单状态、优先级、分类、SLA 截止时间和工单审计事实。
- 已实现工单创建、工单列表查询和工单详情读取。
- 新建工单默认状态为 `open`，本阶段未新增状态变更 API。
- 工单列表支持按时间范围、分类、优先级、状态、客户和普通查询词检索。
- 创建工单时从身份上下文写入 `tenant_id`、`created_by` 和审计字段，不信任请求体中的租户或创建人。
- 列表和详情均按租户隔离；跨租户详情访问返回 `RESOURCE_NOT_FOUND`，不泄露资源是否存在。
- `ticket.search.v1` 保持只读语义，`requires_approval` 仍为 `false`。

## Phase 005 完成范围

- `agent-service` 拥有 Agent 编排状态、图执行过程、节点输入输出摘要、工具调用记录、Mock 模型逻辑、报告草稿和 eval 样例。
- 已实现投诉分析样例任务的本地闭环，可跑完 Planner、Retriever、Data Analyst、Risk、Supervisor、Report 简化节点。
- 已记录 Agent task、run、step、工具调用、报告草稿和 eval 结果；Agent run / step 事件记录带 v1 envelope。
- `ticket.search.v1` 仅以契约化 Mock client 消费，保持只读。
- Phase 005 时 `knowledge.search.v1` 仅使用显式 Mock 知识来源，输出、工具记录、报告引用和 eval 中均可识别 Mock 边界。
- 高风险或低置信度输出进入 `waiting_human` / `waiting_approval` 占位，不创建审批实例、动作命令或跟进任务。

## Phase 006 完成范围

- `apps/web-console` 已落地最小工作台运行时，支持“Mock 登录联调 -> 查看工单列表 -> 查看工单详情 -> 查看投诉分析任务 -> 查看 Agent 步骤 -> 查看报告预览”主链路。
- 前端类型镜像与当前共享契约对齐；前端私有 Mock 标记、风险派生和报告面板状态只停留在视图模型层。
- 报告预览展示事实、推断、风险、建议和引用来源；Agent 步骤展示节点名、状态、摘要、错误和引用。
- 报告区可区分等待人工确认、报告尚未生成、报告不存在 / 不可访问和通用错误。
- 高风险或低置信度结果仍只停留在 `waiting_human` / `waiting_approval` 展示态，不提供审批通过、动作执行、自动派单、通知发送或工单状态变更入口。
- 本阶段未修改 `packages/shared-contracts/**`，也未新增真实网关、真实 AI 任务 HTTP 入口、真实 SSE、真实 RAG、审批实例或动作命令。

## Phase 007 完成范围

- `ai-services/rag-service` 已落地最小本地 RAG 闭环，支持样例 SOP / FAQ 文档登记、解析、切片、索引、租户隔离检索和引用溯源。
- Phase 007 选择本地确定性轻量索引作为 MVP 替代检索方案，`index_ref` 保持存储无关；该方案不代表生产级 pgvector / Milvus 能力。
- 文档生命周期覆盖 `uploaded -> parsing -> indexed` 与 `uploaded -> parsing -> failed`，解析失败保留 `DOCUMENT_PARSE_FAILED`，并可通过带 `tenant_id`、`requested_by`、`trace_id` 的入口重试。
- `knowledge.search.v1` 保持只读、`requires_approval: false`、统一错误形状和存储无关字段；成功结果可追溯到 `document_id` 与 `chunk_id`。
- `agent-service` 默认通过本地 RAG 工具消费 `knowledge.search.v1` 形状结果，报告引用至少包含一个非 Mock 本地文档片段来源；RAG 失败不会静默降级为 Mock。
- RAG eval 已覆盖 2 个样例，计算 `hit_rate` 与 `citation_accuracy`，本阶段验证结果均为 `1.0`。
- 本阶段修正 `scripts/validate-contracts.ps1` 的阶段感知冲突，DEBT-014 已关闭。
- 本阶段未新增审批实例、动作命令、自动派单、通知发送、工单状态变更、真实网关、真实 SSE、RabbitMQ、pgvector、Milvus 或生产对象存储接入。

## 待决事项

- Phase 008 尚未由 Window 0 冻结范围；不得绕过 Window 0 直接进入实现。
- 下一阶段主候选（primary candidate）应回到 Phase 008 人工审批与动作命令，收敛高风险动作审批边界。
- `web-console` 当前仍是 Mock-first 运行时，尚未接入真实 `api-gateway`、真实 AI 任务 HTTP 入口、真实 SSE 或真实审批动作。
- `agent-service` 仍缺少生产级 LangGraph / FastAPI、真实模型适配、真实 `ticket-service` HTTP 工具调用、真实消息队列、JSON Schema 事件校验和持久化存储。
- `rag-service` 仍缺少生产级 FastAPI、对象存储、pgvector / Milvus 或其他生产检索后端、持久化数据库、异步解析任务和真实文档上传。
- `identity-service` 仍缺少生产级 Spring Boot、PostgreSQL 迁移、受控本地数据初始化、refresh token、集中式 token 撤销和完整 RBAC 管理 API。
- `ticket-service` 仍缺少生产级 Spring Boot、PostgreSQL 迁移、服务间鉴权集成、受控样例数据初始化、数据库分页索引和状态变更审计。
- RAG / Agent 质量评测已有最小样例，但仍缺少生产代表性语料、人工标注流程和更完整的幻觉率 / 工具调用质量评估。

## 当前债务

- DEBT-006 仍是 D0 债务；当前只通过 `waiting_human` / `waiting_approval` 占位控制风险，未实现审批边界。
- DEBT-010 仍是 D2 债务；前端已有报告预览，但报告存储格式和导出格式仍未冻结。
- DEBT-011 记录 `identity-service` 最小本地闭环之后的生产化与身份增强缺口。
- DEBT-012 记录 `ticket-service` 最小本地闭环之后的生产化与工单增强缺口。
- DEBT-013 记录 `agent-service` 最小本地闭环之后的生产化、真实工具集成和事件校验缺口。
- DEBT-015 记录 `rag-service` 最小本地闭环之后的生产级检索后端、对象存储、持久化和异步解析缺口。
- DEBT-016 记录 RAG / Agent 质量基准集仍需扩展为生产代表性样本、人工标注和更多质量指标。
- 其余债务见 `../06-debt-register.md`。

## 下一步建议

Window 0 下一次启动时应自动发现：

- 最新 final handoff：`docs/harness/handoffs/phase-007-final.md`
- 当前 Steering 状态：`handoff_done`
- 最新 Review 结论：`docs/harness/handoffs/phase-007-review-fix-1.md`，结论为 `approve`
- `current-state.md` 已更新为 Phase 007 完成态
- 当前仓库已具备身份域、工单域、Agent 编排域、前端工作台和 RAG 知识库的最小运行时

推荐主候选（primary candidate）：Phase 008 人工审批与动作命令。

推荐 candidate 输入：

- Phase 003 已提供身份上下文、租户校验、权限摘要和身份审计基础。
- Phase 004 已提供客户与工单事实源、工单查询 API、详情 API、`ticket.search.v1` 只读契约、租户隔离和工单审计字段。
- Phase 005 已提供 Agent 本地闭环、run / step 状态、结构化报告、人工确认占位和最小 eval 样例。
- Phase 006 已提供可见的工单、任务、步骤、引用和报告预览入口，可承载后续审批等待态与动作结果展示。
- Phase 007 已提供非 Mock 本地文档引用、RAG 工具记录、租户隔离检索和最小 RAG eval，为高风险建议提供可追踪知识来源。
- Phase 008 应限定为审批实例、动作命令、幂等键、审批记录审计和高风险策略阻断，不应顺手实现通知发送、网关生产化、RabbitMQ、真实 SSE、工单复杂状态机或生产级 RAG 平台。
- 本地词项索引只是 MVP 替代检索方案，Window 0 后续不应把它解释为生产级向量检索已完成。
