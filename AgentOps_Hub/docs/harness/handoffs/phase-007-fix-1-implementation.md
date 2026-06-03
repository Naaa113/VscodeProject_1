# Phase 007 Fix Pass 1 实现交接

## 模式与范围

- 模式：Fix Pass 1。
- 阶段：Phase 007，RAG 最小知识库闭环。
- 最新 review handoff：`docs/harness/handoffs/phase-007-review.md`。
- Review 结论：`require fixes`。
- 本次只修复最新 review handoff 中列出的 P1 finding，未修改治理规则文件，未修改 `current-state.md`，未选择下一阶段，未提交 git commit。

## 对应的 review finding

P1：文档重试 / 解析生命周期绕过租户上下文。

`phase-007-review.md` 指出 `KnowledgeSearchEngine.retry_parse` 与 `_document` 只按全局 `document_id` 查找和修改文档，缺少 `tenant_id`、`requested_by` 和 `trace_id` 上下文。只要知道另一个租户的文档 ID，就可能把 failed 文档重新写入内容并推进到 `indexed`，不满足 Phase 007 对文档、检索结果和报告引用租户隔离的要求。

## 修改文件

- `ai-services/rag-service/src/rag_service/service.py`
- `ai-services/rag-service/tests/test_rag_service.py`
- `docs/harness/handoffs/phase-007-fix-1-implementation.md`

## 修复方式

- 将 `KnowledgeSearchEngine.parse_document` 改为显式接收 `document_id`、`tenant_id`、`requested_by` 和 `trace_id`。
- 将 `KnowledgeSearchEngine.retry_parse` 改为显式接收 `document_id`、`tenant_id`、`requested_by`、`trace_id` 和可选 `source_text`。
- 在解析和重试入口校验租户、操作者和 trace 上下文，缺失时返回 `VALIDATION_FAILED`。
- 将 `_document` 改为租户范围查询：文档不存在或文档租户与调用方 `tenant_id` 不一致时，统一抛出 `RESOURCE_NOT_FOUND`，并使用调用方 `trace_id`，不泄露跨租户资源是否存在。
- `ingest_document` 继续沿用登记时的 `tenant_id`、`created_by` 和 `trace_id` 调用解析入口，保持初次摄取闭环。
- 新增回归测试：同一引擎中，`tenant_b` 不能解析或重试 `tenant_a` 的 failed 文档，且不能改写 `tenant_a` 文档的 `source_text` 或 `parse_status`。

## 完成的 architect acceptance

- 文档生命周期修改入口现在携带并校验 `tenant_id`、`requested_by` 和 `trace_id`。
- 跨租户文档解析 / 重试返回不可泄露的 `RESOURCE_NOT_FOUND`。
- 文档、检索结果和报告引用的租户隔离要求得到回归测试覆盖。
- RAG 本地闭环、RAG eval 和 Agent 通过 `knowledge.search.v1` 消费本地 RAG 的主链路仍通过验证。

## 保持不变的 contract

- 未修改 `knowledge.search.v1` 名称、版本、只读语义、`requires_approval: false` 或存储无关行为。
- 未修改 `document.uploaded.v1`、`document.indexed.v1` 事件名和版本。
- 未修改 `document.parse_status` 枚举：`uploaded`、`parsing`、`indexed`、`failed`。
- 未修改认证、工单、AI 任务、报告或前端 URL / API。
- 未新增审批实例、动作命令、自动派单、通知发送、工单状态变更、真实网关、真实 SSE、RabbitMQ、pgvector、Milvus 或生产对象存储接入。

## 行为变化

- 解析和重试文档时，调用方必须提供租户、操作者和 trace 上下文。
- 跨租户文档 ID 即使存在，也会按不可访问资源处理为 `RESOURCE_NOT_FOUND`。
- RAG 失败仍不会静默降级为 Mock 结果。
- Agent 报告引用、RAG 检索和 eval 行为保持不变。

## 为什么没有扩大 scope

- 本次只改 `rag-service` 的生命周期入口和对应测试。
- 未修改契约文件、前端、Agent 实现、Java 服务、网关、部署文件或治理规则文件。
- 未修复 review handoff 之外的新问题，也未新增 helper / adapter / fallback / bridge。

## 测试 / 验证结果

已运行并通过：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-contracts.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-rag-service.ps1
powershell -ExecutionPolicy Bypass -File scripts\validate-agent-service.ps1
```

验证结果：

- `validate-contracts.ps1` 通过，输出 `Phase 002 contract validation passed.`。
- `validate-rag-service.ps1` 通过，5 个单元测试通过，2 个 RAG eval 用例通过，`hit_rate` 与 `citation_accuracy` 均为 `1.0`。
- `validate-agent-service.ps1` 通过，3 个单元测试通过，2 个 Agent eval 用例通过；成功路径仍包含 `local_rag` 和非 Mock 引用，失败路径保留可重试错误。

## Blocker 与遗留风险

- 无 blocker。
- 本地词项索引仍只是 Phase 007 MVP 替代检索方案，不代表生产级向量检索能力。
- 工作区在本窗口开始前已存在大量 dirty / untracked 文件；本窗口未 stage，也未 commit。

## 是否需要重新评审

需要。P1 finding 已修复并通过验证，应进入 Window 3 复审。
