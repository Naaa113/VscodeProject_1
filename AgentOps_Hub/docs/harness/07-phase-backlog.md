# 阶段队列

## 阶段原则

- 从 MVP 最小闭环开始。
- 每个阶段必须有可验证交付物。
- 不把后续平台化能力提前塞入 MVP。
- 契约、状态、验收和 handoff 是每阶段必需品。

## Phase 000: Harness Baseline

目标：建立项目治理、窗口协议、架构边界、契约地图、阶段队列和当前状态。

交付物：

- `docs/harness` 全套文档。
- 当前架构明确为“文档阶段 / 未落地代码”。
- 后续窗口可以按协议推进。

验收：

- 不创建业务代码。
- 不创建 Spring、Vue、React、FastAPI 项目。
- 文件清单完整。

## Phase 001: Monorepo 骨架与本地开发基线

状态：`completed`，见 `docs/harness/handoffs/phase-001-final.md`。

目标：创建最小 monorepo 结构、开发规范和本地运行约定。

范围：

- 根目录基础文件。
- `apps/`, `services/`, `ai-services/`, `packages/`, `deploy/`, `tests/`, `docs/` 目录骨架。
- 技术栈选择记录：前端 React/Vue、MQ、数据库、向量库的 MVP 决策。
- 本地开发命令约定。
- 基础 lint/test 占位策略。

不做：

- 不实现业务 API。
- 不实现前端页面。
- 不接真实模型。

验收：

- 仓库结构与所有权文档一致。
- 本地开发 README 可指导下一阶段。
- 未引入无法解释的框架样板。

## Phase 002: `shared-contracts` / OpenAPI / 事件契约

状态：`completed`，见 `docs/harness/handoffs/phase-002-final.md`。

目标：建立跨服务共同语言。

范围：

- OpenAPI 初稿：auth、tickets、ai tasks、knowledge search。
- 事件契约初稿：`ai.task.created.v1`、Agent run/step、document indexed。
- 状态枚举、错误码、分页、租户上下文、审计字段。
- 工具调用契约：ticket search、create followup、knowledge search、report save。
- schema 示例和契约校验命令。

验收：

- 每个契约有版本、示例、错误响应。
- Java、Python、前端可以基于契约生成或手写客户端而不猜字段。
- 契约变更规则写入 README 或等价文档。

## Phase 003: `identity-service` 最小认证与租户模型

状态：`completed`，见 `docs/harness/handoffs/phase-003-final.md`。

目标：落地最小身份域。

范围：

- 租户、用户、角色基础模型。
- 登录、当前用户、JWT 或等价 token 机制。
- 租户上下文传递。
- 最小审计字段。
- 单元测试和集成测试。

不做：

- 不做复杂 OAuth2/OIDC。
- 不做完整组织架构。

验收：

- 前端和其他服务可以获得当前用户和租户上下文。
- 所有业务 API 契约能表达 `tenant_id` 或租户上下文。
- 登录失败、权限不足、租户缺失有统一错误码。

## Phase 004: `ticket-service` 最小工单模型

状态：`completed`，见 `docs/harness/handoffs/phase-004-final.md`。

目标：落地工单域最小事实源。

范围：

- 客户和工单基础模型。
- 工单创建、查询、详情、状态、优先级、SLA 字段。
- Agent 工具可用的工单检索接口。
- 租户隔离与审计字段。
- 单元测试、集成测试、契约测试。

不做：

- 不做复杂自动派单。
- 不做完整 BI 分析。

验收：

- 可创建样例客户与工单。
- 可按时间范围、分类、优先级、状态查询工单。
- Agent 工具调用有稳定契约。

## Phase 005: `agent-service` 最小 LangGraph 闭环

状态：`completed`，见 `docs/harness/handoffs/phase-005-final.md`。

目标：跑通单条投诉分析类 Agent 工作流。

范围：

- 最小 LangGraph 状态机。
- Planner、Retriever、Data Analyst、Risk、Supervisor、Report 的简化节点。
- 使用 Mock 或契约化工具调用检索工单与知识。
- 写入 Agent run/step 状态。
- 失败、重试、人工确认占位状态。
- 最小 AI eval 样例。

不做：

- 不让 Agent 直接执行高风险动作。
- 不实现复杂多 Agent 自主协商。

验收：

- 给定样例任务可生成结构化报告。
- 每个步骤可追踪。
- 引用来源可显示或明确标记为 Mock。
- 低置信度或高风险输出进入人工确认状态。

## Phase 006: `web-console` 最小工作台

状态：`completed`，见 `docs/harness/handoffs/phase-006-final.md`。

目标：提供可操作的 MVP 前端体验。

范围：

- 登录页或登录流程。
- 工单列表/详情。
- AI 任务发起。
- Agent 执行链路展示。
- 报告预览。
- 知识库入口占位或最小列表。
- API Mock 或真实后端联调。

验收：

- 用户能从界面完成“登录 -> 查看工单 -> 发起任务 -> 查看步骤 -> 查看报告”。
- 页面不依赖内部数据库字段。
- 错误、加载、空状态完整。

## Phase 007: RAG 最小知识库闭环

状态：`completed`，见 `docs/harness/handoffs/phase-007-final.md`。

目标：让 Agent 报告有可追踪知识来源。

范围：

- 文档上传登记。
- 文档解析、切片和本地确定性轻量索引。
- 向量检索的 MVP 替代检索方案；生产级 pgvector / Milvus 留待后续。
- 引用来源追踪。
- RAG 命中率和引用准确率评测样例。

验收：

- 上传样例 SOP 或 FAQ 后可检索。
- Agent 报告引用可追溯到文档片段。
- 解析失败可重试并可见。

## Phase 008: 人工审批与动作命令

目标：补齐高风险动作的人机协同。

范围：

- 审批实例。
- 动作命令。
- 幂等键。
- 创建跟进任务。
- 审批记录审计。

验收：

- 高风险建议不会自动执行。
- 审批通过后动作命令执行一次。
- 审批拒绝后动作不执行。

## Phase 009: 可观测性与系统测试

目标：让平台可诊断、可评测、可压测。

范围：

- OpenTelemetry trace 约定。
- 关键指标。
- 结构化日志。
- 系统测试：投诉分析端到端。
- 压测入口。
- AI eval 扩展。

验收：

- 单次任务可从前端请求追踪到 Agent 步骤和工具调用。
- 系统测试可以稳定复现 MVP 闭环。

## Phase 010: 企业级能力扩展

目标：从 MVP 走向 V1.0。

候选范围：

- 多 Agent 协作增强。
- 异步队列生产化。
- MinIO、搜索引擎、向量库强化。
- 模型成本统计。
- 灰度发布和 Kubernetes 部署基线。
