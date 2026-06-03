# rag-service

本目录在 Phase 007 落地最小本地 RAG 知识库闭环。

当前能力只用于本地验证：

- 登记样例 SOP / FAQ 文档，并记录 `document.uploaded.v1` 形状的本地事件。
- 将文本文档解析、切片，并建立确定性的本地词项索引。
- 通过 `knowledge.search.v1` 形状返回带 `document_id`、`chunk_id`、`snippet`、`score` 和 `citation` 的检索结果。
- 检索结果按 `tenant_id` 隔离，只返回已进入 `indexed` 状态的文档片段。
- 解析失败会落入 `failed` 状态，保留统一错误形状，并允许通过本地入口重试。
- 最小 eval 计算命中率和引用准确率。

明确非目标：

- 不实现生产级 FastAPI、消息队列、对象存储、数据库迁移或实时推送。
- 不绑定 pgvector、Milvus 或任何生产向量库；`index_ref` 保持存储无关。
- 不充当身份、工单、审批、动作命令或业务事实源。
- 不把本地词项索引宣传为生产级 RAG 能力。

本地验证：

```powershell
.\scripts\validate-rag-service.ps1
```

也可以在本目录运行：

```powershell
python -m unittest discover -s tests
python -m rag_service.cli --scenario eval
```
