# agent-service

本目录是 Agent 本地编排运行时。Phase 007 之后，它仍然只承担本地闭环验证，不代表生产级 Agent 服务已经完成。

当前能力：

- 使用标准库实现简化图执行器，不依赖真实外部模型、真实 API key 或外部网络。
- 固定节点顺序为 Planner、Retriever、Data Analyst、Risk、Supervisor、Report。
- `ticket.search.v1` 继续通过契约化 Mock 工具消费，保持只读语义。
- `knowledge.search.v1` 默认通过本地 `rag-service` 检索工具消费，报告引用可回溯到真实本地文档片段。
- `report.save.v1` 仍使用本地 Mock 保存，不代表生产报告服务。
- 每个 Agent step 记录状态、耗时、输入摘要、输出摘要、错误和引用。
- 高风险或低置信度输出进入 `waiting_human` step，并把 task 标记为 `waiting_approval` 占位。

明确非目标：

- 不实现 FastAPI、网关、SSE、RabbitMQ 或生产级任务入口。
- 不直接读取 RAG 内部索引、fixture、缓存或存储文件；Agent 只通过 `knowledge.search.v1` 消费结果。
- 不实现审批实例、动作命令、自动派单、工单状态变更或通知发送。
- 不直接访问身份服务、工单服务或知识库数据库。

本地验证：

```powershell
.\scripts\validate-agent-service.ps1
```

也可以在本目录运行：

```powershell
python -m unittest discover -s tests
python -m agent_service.cli --scenario complaint
python -m agent_service.cli --scenario eval
```
