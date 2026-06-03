# Host Ownership

## 所有权原则

Host Ownership 描述每个运行单元或文档单元的归属边界。一个 host 必须有清晰职责、拥有的数据、暴露的契约、禁止承担的职责和验收入口。

当前项目已落地 Phase 001 占位骨架、Phase 003 `identity-service` 最小运行时、Phase 004 `ticket-service` 最小运行时、Phase 005 `agent-service` 最小本地运行时和 Phase 006 `web-console` 最小工作台运行时。下表同时描述目标所有权和当前已知归属；未实现的 host 仍只代表目标边界。

## 目标 Host 清单

| Host | 类型 | 拥有职责 | 禁止职责 |
|---|---|---|---|
| `apps/web-console` | 前端应用 | 工作台、任务展示、报告预览、知识库管理界面 | 直接访问数据库、绕过网关调用内部服务 |
| `apps/api-gateway` | Java 网关 | 统一入口、路由、鉴权适配、限流、CORS | 存储业务事实、执行 Agent 工作流 |
| `services/identity-service` | Java 服务 | 租户、用户、角色、权限、JWT、审计身份上下文 | 管理工单、知识库和模型推理 |
| `services/ticket-service` | Java 服务 | 客户、工单、SLA、处理记录、工单查询工具 | 自主执行高风险 AI 动作、复制身份事实 |
| `services/workflow-service` | Java 服务 | 审批流、人机确认、动作命令、业务动作幂等 | 生成无审计的动作结果 |
| `services/notification-service` | Java 服务 | 站内信、邮件、Webhook、通知状态 | 决定通知业务策略 |
| `ai-services/agent-service` | Python 服务 | LangGraph 状态机、Planner/Retriever/Analyst/Risk/Supervisor/Report Agent、Agent run/step 状态、工具调用记录和 AI eval；Phase 005 已落地本地最小闭环 | 直接写业务事实、绕过审批、执行高风险动作或把 Mock 工具伪装成生产能力 |
| `ai-services/rag-service` | Python 服务 | 文档解析、切片、Embedding、检索、引用溯源 | 充当业务事实源 |
| `ai-services/analytics-worker` | Python worker | 异步分析、报表、批处理、AI 评测任务 | 承担同步用户请求入口 |
| `packages/shared-contracts` | 契约包 | OpenAPI、事件、DTO、错误码、状态枚举 | 包含业务实现 |
| `packages/prompt-templates` | Prompt 包 | Prompt 模板、版本和评测样例 | 私自绑定业务数据库 |
| `packages/test-fixtures` | 测试资产 | 样例工单、知识库文档、Mock 响应 | 存放生产密钥或真实敏感数据 |

## 关键所有权边界

### Java 业务服务

Java 服务拥有企业业务事实，包括用户、权限、工单、审批、动作命令和审计记录。Java 服务可以调用 Spring AI 承载轻量摘要、分类或建议，但不能让模型输出绕过权限、审批和审计。

### Python AI 服务

Python 服务拥有 Agent 推理过程、工作流状态、RAG 检索、模型调用和 AI 评测。Python 服务必须通过契约化工具调用访问业务能力，不直接修改业务事实表。

### 共享契约

`shared-contracts` 是跨 host 的共同语言。所有跨服务 REST API、事件消息、状态枚举、错误码和工具协议必须在这里先定义。

### 前端工作台

前端只消费网关公开的 API 和实时状态通道。前端可以展示 Agent 执行链路，但不解释服务内部实现细节，不构造内部数据库字段依赖。

## 验收责任

| Host | 最低验收入口 |
|---|---|
| 前端 | 组件测试、端到端冒烟、API Mock 一致性 |
| Java 服务 | 单元测试、契约测试、集成测试、审计检查 |
| Python 服务 | Agent 节点测试、图状态测试、工具 Mock、AI eval |
| 契约包 | schema 校验、兼容性检查、示例 payload |
| 部署资产 | 本地启动脚本、健康检查、配置模板检查 |
