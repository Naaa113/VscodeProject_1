# Window Protocol

## 窗口模型

AgentOps Hub 使用多窗口协作方式推进，每个窗口只承担一种主要职责，避免规划、实现、评审和交接混杂。

| Window | 名称 | 职责 |
|---|---|---|
| Window 0 | Steering | 决策、范围冻结、状态机推进 |
| Window 1 | Phase Architect | 阶段架构、契约、边界和计划 |
| Window 2 | Implementer | 按冻结范围实现 |
| Window 3 | Review/Eval | 评审、测试、AI eval、安全检查 |
| Window 4 | Handoff | 汇总状态、债务、下一步和交接 |

## 全局规则

- 任何窗口开始前必须读取 `state/current-state.md`。
- 任何窗口不得越过当前 phase 的冻结范围。
- 任何跨服务变更必须先经过契约文档。
- 实现窗口不得单独修改项目章程和权威矩阵。
- 评审窗口不得悄悄扩大实现范围。
- Handoff 必须诚实记录未完成、风险和债务。

## Window 0: Steering

输入：

- 当前状态。
- 上一阶段 handoff。
- backlog。
- 用户最新指令。

输出：

- 当前 phase。
- 冻结范围。
- 明确非目标。
- 是否允许进入实现。

## Window 1: Phase Architect

输入：

- Steering 决策。
- 项目章程。
- 当前架构。
- 契约地图。

输出：

- 阶段架构说明。
- 契约变更提案。
- host ownership 更新提案。
- 验收标准。
- 实现任务拆分。

## Window 2: Implementer

输入：

- 冻结后的阶段计划。
- 契约和验收标准。

输出：

- 代码或文档变更。
- 本地验证记录。
- 未解决问题。

限制：

- 不私自改变跨服务契约。
- 不扩大阶段范围。
- 不把临时 mock 伪装成生产能力。

## Window 3: Review/Eval

输入：

- 实现变更。
- 验收清单。
- 契约和当前状态。

输出：

- 发现的问题。
- 阻断项。
- 测试和评测结果。
- 是否建议进入 handoff。

## Window 4: Handoff

输入：

- 当前阶段所有输出。
- Review/Eval 结论。
- Debt Register。

输出：

- phase handoff 文档。
- `state/current-state.md` 更新。
- 下一 phase 建议。

## 中断恢复规则

如果窗口被中断：

1. 读取 `state/current-state.md`。
2. 查看最近 handoff。
3. 检查工作区变更。
4. 只继续未完成且仍在范围内的任务。
5. 不假设中断前的未写入内容已经完成。

