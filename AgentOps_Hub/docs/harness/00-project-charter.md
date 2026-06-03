# AgentOps Hub 项目章程

## 项目定位

AgentOps Hub 是面向中大型企业的智能运营协同平台。项目目标不是构建单纯聊天机器人，而是通过多 Agent 工作流把企业工单、知识库、客户资料、审批流程、报表和人工确认连接起来，让 AI 从“回答问题”升级为“协同完成任务”。

当前仓库处于 Bootstrap Harness 阶段。本目录只定义项目治理、阶段规划、边界、契约和验收规则，不包含业务代码。

## 核心业务目标

- 让运营人员通过自然语言发起复杂业务任务，例如“分析最近 7 天客户投诉上涨原因，找出高风险客户，生成处理建议，并创建跟进任务”。
- 以 Java + Python 双后端分工承载真实企业系统：Java 侧负责稳定业务能力，Python 侧负责 Agent 编排、RAG、数据分析和 AI 质量控制。
- 以事件驱动方式解耦长耗时 AI 任务、文档处理、通知发送和跨服务动作。
- 为 Kubernetes、可观测性、自动化测试、AI 评测和后续平台化扩展预留清晰边界。

## 成功定义

MVP 成功不是“功能很多”，而是跑通一条可审计、可回放、可评测的智能运营闭环：

1. 用户完成认证并进入工作台。
2. 用户创建或导入工单与知识库材料。
3. 用户发起投诉分析类 AI 任务。
4. Agent 工作流完成规划、检索、分析、风险判断和报告生成。
5. 高风险动作进入人工确认或被明确阻断。
6. 前端可以看到任务状态、Agent 步骤、引用来源和最终报告。

## 目标用户

| 角色 | 主要诉求 | 平台承诺 |
|---|---|---|
| 运营人员 | 快速分析工单、投诉、客户反馈和异常 | 提供自然语言任务入口、报告和处置建议 |
| 客服主管 | 提升工单分派效率和 SLA 达成率 | 提供风险识别、审批和跟进任务链路 |
| 知识库管理员 | 维护 FAQ、SOP、政策和案例 | 提供文档上传、引用追踪和 RAG 质量检查 |
| 系统管理员 | 管理用户、权限、模型和监控 | 提供租户、RBAC、审计和配置治理 |
| AI 工程师 | 优化 Agent、Prompt、工具和评测 | 提供工作流状态、工具日志和 AI 评测入口 |

## 非目标

- 不在 Harness 阶段实现业务代码。
- 不在 Harness 阶段创建 Spring、Vue、React 或 FastAPI 项目。
- 不把 Agent 自动动作设计为绕过人工确认的黑盒执行。
- 不把模型输出作为不可审计的最终事实。
- 不在 MVP 中追求 Agent Marketplace、模型网关、复杂插件系统或完整生产运维能力。

## 技术方向

- 前端：React 或 Vue + TypeScript，用于统一工作台、任务链路和报告展示。
- Java 后端：Spring Boot、Spring Cloud Gateway、Spring Security、Spring AI，负责用户、权限、工单、审批、任务和审计。
- Python 后端：FastAPI、LangChain、LangGraph、Pydantic、Pandas，负责 Agent 编排、RAG、数据分析和 AI 质量评估。
- 中间件：Redis、Kafka 或 RabbitMQ、PostgreSQL 或 MySQL、pgvector 或 Milvus、MinIO、Elasticsearch 或 OpenSearch。
- 部署与观测：Docker、Kubernetes、Helm、Prometheus、Grafana、Jaeger、OpenTelemetry。

## 治理原则

- 契约先行：跨服务 REST、事件、状态机、错误码和数据模型必须先进入 `shared-contracts` 再进入实现。
- 边界清晰：Java 业务服务拥有业务事实和审批结果；Python Agent 服务拥有 AI 推理过程和工作流状态。
- 人机协同：高风险动作必须显式进入人工确认或策略拦截。
- 可观测优先：任务、Agent 步骤、工具调用、模型调用和引用来源必须可追踪。
- 小步闭环：每个阶段必须交付可运行、可验证、可回滚的增量。

