# Phase 000 Handoff: Harness Baseline

## Phase

Phase 000: Harness Baseline

## Scope

基于 AgentOps Hub 三份项目材料建立全新的 harness。范围限定为项目治理、阶段规划、边界、契约和验收规则。

## Completed

- 创建项目章程。
- 标注当前架构为“文档阶段 / 未落地代码”。
- 建立权威矩阵、host ownership、contract map、状态生命周期和债务登记。
- 建立 MVP 起步的 phase backlog。
- 建立 eval checklist、window protocol、steering state machine 和 cycle runbook。
- 建立当前状态文件。
- 建立后续窗口 prompts。

## Validation

- 未创建业务代码。
- 未创建 Spring、Vue、React 或 FastAPI 项目。
- Phase backlog 包含用户指定的 MVP 优先序列。

## Not Done

- 未选择具体前端框架。
- 未选择 MQ、关系库、向量库和实时通道方案。
- 未创建 `shared-contracts`。

## Debt Added

见 `../06-debt-register.md`。

## Risks

- 如果 Phase 001 直接生成完整框架样板，可能过早固化技术选择。
- 如果 Phase 002 前跳到业务实现，Java/Python/前端契约会漂移。
- 高风险动作的审批边界必须在 Agent 最小闭环前明确。

## Next Recommended Phase

Phase 001: Monorepo 骨架与本地开发基线。

