# Phase 007 Fix Pass 1 复审交接

## 评审模式

- 模式：Re-review fix 1。
- 输出文件：`docs/harness/handoffs/phase-007-review-fix-1.md`。
- 判定依据：已存在 `phase-007-review.md`，最新结论为 `require fixes`；已存在 `phase-007-fix-1-implementation.md`；尚不存在 `phase-007-review-fix-1.md`。

## 读取的文件

固定治理文件：

- `docs/harness/00-project-charter.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/state/current-state.md`

Phase 007 handoff 文件：

- `docs/harness/handoffs/steering-decision-phase-007.md`
- `docs/harness/handoffs/phase-007-architect.md`
- `docs/harness/handoffs/phase-007-implementation.md`
- `docs/harness/handoffs/phase-007-review.md`
- `docs/harness/handoffs/phase-007-fix-1-implementation.md`

## 结论

`approve`

允许进入 Window 4。上一次 `require fixes` 中的 P1 finding 已关闭；本次复审未发现新的 belongs / authority / contract / transition / behavior 阻断项。

## 上一次 finding 关闭情况

### 已关闭：P1 文档重试 / 解析生命周期绕过租户上下文

上次 review 指出 `retry_parse`、`parse_document` 和 `_document` 缺少租户上下文校验，跨租户调用可能改写 failed 文档。Fix Pass 1 已把解析和重试入口改为显式接收并校验 `tenant_id`、`requested_by`、`trace_id`，并把 `_document` 改为租户范围查询。跨租户文档 ID 现在返回 `RESOURCE_NOT_FOUND`，不会泄露或改写原租户文档。

证据：

- `docs/harness/handoffs/phase-007-review.md:39` 记录原 P1 finding。
- `docs/harness/handoffs/phase-007-review.md:68` 到 `docs/harness/handoffs/phase-007-review.md:70` 记录修复要求。
- `docs/harness/handoffs/phase-007-fix-1-implementation.md:23` 记录修复方式。
- `ai-services/rag-service/src/rag_service/service.py:306` 到 `ai-services/rag-service/src/rag_service/service.py:323`：`parse_document` 要求租户、操作者和 trace 上下文，并按租户读取文档。
- `ai-services/rag-service/src/rag_service/service.py:353` 到 `ai-services/rag-service/src/rag_service/service.py:379`：`retry_parse` 要求租户、操作者和 trace 上下文，并复用租户范围解析入口。
- `ai-services/rag-service/src/rag_service/service.py:466` 到 `ai-services/rag-service/src/rag_service/service.py:476`：`_document` 对不存在或跨租户文档统一返回 `RESOURCE_NOT_FOUND`。
- `ai-services/rag-service/tests/test_rag_service.py:72` 到 `ai-services/rag-service/tests/test_rag_service.py:114`：新增跨租户解析 / 重试拒绝回归测试。

补充复现实验结果：

```text
RESOURCE_NOT_FOUND trace_b_retry tenant_a failed ''
```

该结果说明 `tenant_b` 不能重试或改写 `tenant_a` 的 failed 文档，原文档内容保持未变。

## Findings

无新的阻断 finding。

复审检查结论：

- belongs：Fix Pass 1 只修改 `rag-service` 生命周期入口和对应测试，符合 Phase 007 主实现 host 边界。
- authority：未引入第二事实源；RAG 仍只拥有文档、切片、索引引用和 citation，未接管身份、工单、审批或动作事实。
- contract：`knowledge.search.v1` 仍保持只读、存储无关、`requires_approval: false` 和统一错误形状；未发现破坏既有认证、工单或 AI 任务语义。
- transition：文档解析、失败、重试和索引生命周期现在具备租户上下文；跨租户修改入口已被拒绝。
- behavior：Agent 仍通过 `knowledge.search.v1` 消费本地 RAG；报告引用包含非 Mock 文档片段；高风险建议仍停留在 `waiting_human` / `waiting_approval`，未生成审批实例或动作命令。

## Window 1 acceptance 覆盖情况

- 已满足：`ai-services/rag-service` 有可运行的最小本地 RAG 闭环。
- 已满足：样例 SOP / FAQ 可登记、解析、切片、索引和检索。
- 已满足：文档解析失败落入 `failed`，错误原因可见，并可通过本地入口重试。
- 已满足：文档生命周期修改入口按 `tenant_id` 隔离，跨租户解析 / 重试返回不可泄露的 `RESOURCE_NOT_FOUND`。
- 已满足：检索结果引用可追溯到 `document_id` 和 `chunk_id`。
- 已满足：Agent 报告引用包含非 Mock 本地文档片段来源。
- 已满足：Mock 来源与本地 RAG 来源在工具记录、报告引用和 eval 中可区分。
- 已满足：RAG eval 计算 `hit_rate` 和 `citation_accuracy`。
- 已满足：`scripts/validate-contracts.ps1` 不再因 Phase 006 前端运行时产生假阳性失败。
- 已满足：未新增审批实例、动作命令、自动派单、通知发送、工单状态变更、真实网关、真实 SSE、RabbitMQ、pgvector、Milvus 或生产对象存储接入。

## AI / RAG eval 检查

- RAG 命中率：2 个样例均通过，`hit_rate = 1.0`。
- 引用准确率：2 个样例均通过，`citation_accuracy = 1.0`。
- 引用与幻觉控制：检索结果和 Agent 报告引用均带非 Mock `source_uri`，未发现无来源结论扩散为业务动作。
- 工具调用边界：`RagKnowledgeSearchTool` 通过 `engine.search(...)` 消费 `knowledge.search.v1` 形状响应，工具记录保留 `tenant_id`、`requested_by`、`trace_id`、`run_id` 和 `source_type: local_rag`。
- 人工介入规则：高风险路径仍生成 `waiting_human` step 和 `waiting_approval` task，不生成审批实例、动作命令、自动派单、通知发送或工单状态变更。

证据：

- `ai-services/agent-service/src/agent_service/tools.py:159` 到 `ai-services/agent-service/src/agent_service/tools.py:222`：本地 RAG 工具记录 `knowledge.search.v1` 调用，失败时转为工具错误记录，不静默降级为 Mock。
- `ai-services/agent-service/src/agent_service/graph.py:106` 到 `ai-services/agent-service/src/agent_service/graph.py:116`：Agent Retriever 通过工具消费知识结果并转换 citation。
- `ai-services/agent-service/src/agent_service/graph.py:152` 到 `ai-services/agent-service/src/agent_service/graph.py:157`：高风险建议进入人工确认占位，不生成动作命令。
- `packages/shared-contracts/openapi/agentops-api.v1.yaml:802` 到 `packages/shared-contracts/openapi/agentops-api.v1.yaml:838`：知识检索响应和 citation 字段包含可追踪引用。
- `packages/shared-contracts/tools/knowledge.search.v1.schema.json:65` 到 `packages/shared-contracts/tools/knowledge.search.v1.schema.json:89`：工具契约保留 Mock 标记和 `source_uri`。

## 验证命令

已运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-contracts.ps1
```

结果：通过，输出 `Phase 002 contract validation passed.`。

已运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-rag-service.ps1
```

结果：通过，5 个单元测试通过；2 个 RAG eval 用例通过；`hit_rate` 与 `citation_accuracy` 均为 `1.0`。

已运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-agent-service.ps1
```

结果：通过，3 个单元测试通过；2 个 Agent eval 用例通过；成功路径包含 `local_rag` 与非 Mock 引用，失败路径保留可重试错误。

已运行：

```powershell
git diff --stat
git status --short --untracked-files=all
```

结果：工作区仍包含大量既有 dirty / untracked 文件，也包含仓库外删除项；`docs/harness/state/current-state.md` 已记录多窗口推进期间工作区长期 dirty。本次复审未清理、stage 或提交这些变更。

## 是否允许进入 Window 4

允许。Phase 007 Fix Pass 1 已关闭初审 P1 finding，并满足 Window 1 acceptance。Window 4 应继续诚实记录本地词项索引只是 MVP 替代检索方案，不代表生产级向量检索能力。
