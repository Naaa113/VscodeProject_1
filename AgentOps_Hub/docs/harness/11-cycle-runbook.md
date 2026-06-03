# Cycle Runbook

## 每阶段运行流程

### 1. Steering 启动

读取：

- `state/current-state.md`
- 最近 handoff
- `07-phase-backlog.md`
- `06-debt-register.md`

产出：

- 当前 phase。
- 冻结范围。
- 非目标。
- 成功标准。

### 2. Phase Architecture

检查：

- 哪些 host 受影响。
- 哪些契约需要新增或修改。
- 哪些状态生命周期需要定义。
- 哪些债务必须先解决。

产出：

- 阶段架构说明。
- 契约清单。
- 实现任务拆分。
- 验收清单。

### 3. Implementation

执行：

- 只做冻结范围内的变更。
- 优先实现契约、测试和最小闭环。
- 保持 host ownership。
- 记录无法完成的事项。

### 4. Review/Eval

验证：

- 单元、集成、系统或 AI eval 是否符合阶段要求。
- 契约是否被遵守。
- 安全、租户、审批和审计边界是否清晰。
- 是否引入新债务。

### 5. Handoff

更新：

- `state/current-state.md`
- `handoffs/phase-XXX-*.md`
- 必要时更新 debt、architecture、contract map。

交接内容：

- 完成了什么。
- 如何验证。
- 未完成什么。
- 新增债务。
- 下一步建议。

## Bootstrap Harness 特殊规则

Phase 000 只允许创建 `docs/harness` 文档，不允许创建业务代码、框架项目或服务目录。

## 检查命令建议

在文档阶段可使用：

```text
git status --short
Get-ChildItem -Recurse docs/harness
```

后续代码阶段根据技术栈补充：

- 前端 lint/test。
- Java unit/integration test。
- Python unit/graph/eval test。
- 契约 schema 校验。
- 端到端冒烟。

## Handoff 模板

```text
# Phase XXX Handoff

## Phase

## Scope

## Completed

## Validation

## Not Done

## Debt Added/Closed

## Risks

## Next Recommended Phase
```

