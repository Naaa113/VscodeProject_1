# workflow-service

Phase 008 在此目录落地最小本地审批与动作命令运行时。

当前范围：

- 提供审批实例、审批记录、动作命令和幂等键的本地 Java/H2 闭环。
- 提供 `POST /api/approvals`、`GET /api/approvals/{id}`、审批通过/拒绝/取消，以及 `GET /api/action-commands/{id}` 最小 API。
- 提供 `ticket.create_followup.v1` 对应的最小动作执行结果，但不写入 `ticket-service` 工单事实表。

明确非目标：

- 不实现通知发送、邮件、站内信、Webhook 或第三方触达。
- 不实现 Spring Boot、真实网关、真实 SSE、RabbitMQ、Kafka 或完整流程引擎。
- 不实现工单复杂状态机、自动派单、SLA 引擎或生产数据库迁移。
- 不让 Agent、前端或其他 host 绕过本服务直接写审批或动作结果。

运行方式：

- 使用 `workflow-service.maven.xml` 作为最小 Maven 入口。
- 使用 H2 本地数据库和 `app/main/resources/schema.sql`。
- 使用 `scripts/validate-workflow-service.ps1` 运行本地验证。
