# Phase 007 评审交接

## 评审模式

- 模式：初次 Review。
- 输出文件：`docs/harness/handoffs/phase-007-review.md`。
- 判定依据：存在 `phase-007-implementation.md`，不存在 `phase-007-review.md`，且不存在 Phase 007 fix implementation。

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

## 结论

`require fixes`

不允许进入 Window 4。当前实现已跑通本地 RAG、Agent 和 eval 主路径，但文档生命周期修改入口缺少租户上下文，不能确认满足 Phase 007 对文档、检索结果和报告引用的租户隔离要求。

## Findings

### P1：文档重试/解析生命周期绕过租户上下文

`KnowledgeSearchEngine.retry_parse` 只接收 `document_id` 和可选 `source_text`，随后通过 `_document(document_id)` 直接取出并修改文档；`_document` 同样不校验 `tenant_id`、`requested_by` 或 `trace_id`。这意味着只要知道文档 ID，就可以把另一个租户的 failed 文档重新写入内容并推进到 `indexed`，偏离了 Phase 007 的“租户隔离必须贯穿文档登记、解析、检索、引用和 Agent 报告”要求。

证据：

- `ai-services/rag-service/src/rag_service/service.py:331`：`retry_parse` 签名没有 `tenant_id`、`requested_by` 或 `trace_id`。
- `ai-services/rag-service/src/rag_service/service.py:332`：重试直接调用 `_document(document_id)`。
- `ai-services/rag-service/src/rag_service/service.py:335`：重试继续调用 `parse_document(document_id)`，仍无租户上下文。
- `ai-services/rag-service/src/rag_service/service.py:422`：`_document` 仅按全局 `document_id` 查找。
- `ai-services/rag-service/tests/test_rag_service.py:55`：现有重试测试也只传 `document_id`，未覆盖跨租户重试拒绝。
- `docs/harness/handoffs/phase-007-architect.md:202`：检索请求必须带 `tenant_id`、`requested_by`、`trace_id`。
- `docs/harness/handoffs/phase-007-architect.md:272`：无法保证文档、检索结果或报告引用按 `tenant_id` 隔离时必须停止。

补充复现实验：

```powershell
$env:PYTHONPATH = "C:\Users\20978\VscodeProjects\AgentOps_Hub\ai-services\rag-service\src"
python -c "from rag_service.service import KnowledgeSearchEngine; e=KnowledgeSearchEngine(); d=e.register_document(tenant_id='tenant_a', requested_by='user_a', trace_id='trace_a', knowledge_base_id='kb_shared', filename='empty.txt', object_key='missing.txt', source_text='', document_id='doc_shared'); e.parse_document(d.document_id); r=e.retry_parse('doc_shared', source_text='secret repaired by tenant_b context absent'); print(r.tenant_id, r.parse_status, r.source_text)"
```

结果：

```text
tenant_a indexed secret repaired by tenant_b context absent
```

修复要求：

- `retry_parse` 和等价的生命周期修改入口必须接收并校验 `tenant_id`，建议同时保留 `requested_by` 与 `trace_id`，并在失败响应中使用统一错误形状。
- `_document` 或调用层必须提供租户范围查询；跨租户文档应返回 `RESOURCE_NOT_FOUND` 或等价不可泄露错误。
- 增加测试覆盖：同一引擎内另一个租户不能重试、解析或读取不属于自己的文档。

## Window 1 acceptance 覆盖情况

- 已满足：存在可运行的 `ai-services/rag-service` 本地闭环。
- 已满足：样例 SOP / FAQ 可登记、解析、切片、索引和检索。
- 已满足：RAG eval 计算 `hit_rate` 与 `citation_accuracy`。
- 已满足：Agent 报告包含非 Mock 本地文档片段引用。
- 已满足：`scripts/validate-contracts.ps1` 不再因 Phase 006 前端运行时产生假阳性失败。
- 未满足：文档生命周期的重试 / 解析修改入口未按 `tenant_id` 隔离，不能确认“文档、检索结果或报告引用按租户隔离”。

## AI / RAG eval 检查

- RAG 命中率：已有样例集和计算方式，本次验证输出两个用例 `hit_rate = 1.0`。
- 引用准确率：已有样例集和计算方式，本次验证输出两个用例 `citation_accuracy = 1.0`。
- 幻觉控制：报告区引用来自非 Mock 文档片段，未发现无来源结论扩散为业务动作。
- 工具调用边界：`knowledge.search.v1` 工具记录包含 `tenant_id`、`requested_by`、`trace_id`、`run_id`、请求、响应、状态和来源类型。
- 人工介入规则：高风险结果仍进入 `waiting_human` / `waiting_approval`，未发现审批实例、动作命令、自动派单、通知发送或工单状态变更。

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

结果：通过，4 个单元测试通过，2 个 RAG eval 用例通过，`hit_rate` 与 `citation_accuracy` 均为 `1.0`。

已运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate-agent-service.ps1
```

结果：通过，3 个单元测试通过，2 个 Agent eval 用例通过；成功路径包含 `local_rag` 与非 Mock 引用，失败路径保留可重试错误。

已运行：

```powershell
git diff --stat
git status --short --untracked-files=all
```

结果：工作区仍包含大量既有 dirty / untracked 文件；本次评审没有清理、stage 或提交。`git diff --stat` 中存在部分非 Phase 007 范围的已跟踪修改，但 `current-state.md` 已记录多窗口推进期间工作区长期 dirty，本次不将这些既有差异单独判为 Phase 007 blocker。

## 是否允许进入 Window 4

不允许。需先回 Window 2 Fix Pass，关闭 P1 finding 后再进入复审。

