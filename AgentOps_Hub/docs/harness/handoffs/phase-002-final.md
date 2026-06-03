# Phase 002 最终交接

## 阶段状态

`completed`（已完成）

Phase 002 `shared-contracts` / OpenAPI / 事件契约已完成 fix-1 后复审并进入 Window 4 收尾。Window 0 后续启动时应自动发现本文件，并基于当前状态、阶段队列、债务和用户最新指令重新选择下一阶段。

## 已完成范围

- 在 `packages/shared-contracts` 中创建 v1 契约草案。
- 创建 OpenAPI 草案：认证、工单、AI 任务、知识库文档、知识库检索和 SSE 任务状态流。
- 创建通用 schema：租户上下文、审计字段、分页、追踪字段、统一错误响应、事件信封和工具信封。
- 创建状态枚举：AI task、Agent run、Agent step、approval instance、action command、document parse status。
- 创建 7 个事件 schema：AI 任务创建、Agent run/step、Agent run 完成/失败、文档上传、文档索引。
- 创建 4 个工具 schema：`ticket.search.v1`、`ticket.create_followup.v1`、`knowledge.search.v1`、`report.save.v1`。
- 创建成功与失败示例、`manifest.v1.json` 和 `scripts/validate-contracts.ps1`。
- fix-1 补强工具失败和 Agent step 失败契约，失败时必须携带可追踪错误信息。
- 保持知识检索契约存储无关，没有选择 pgvector 或 Milvus。
- 未创建服务运行时代码、前端页面、数据库迁移、Agent workflow、RAG 实现、模拟服务、适配器或模型接入。

## 未变化契约

- 未改变 Phase 002 架构交接中冻结的 URL、事件名称、工具名称和状态枚举值。
- 未改变 `ticket.create_followup.v1` 需要审批或策略检查的边界。
- 未改变知识检索契约的存储无关设计。
- 未把任何契约声明为已经由运行时服务实现。

## 契约 / 权威 / 状态迁移变化

- 契约：`packages/shared-contracts` 从 Phase 001 的目录占位推进为 v1 草案，包含 OpenAPI、事件、工具、错误码、状态枚举、示例和清单。
- 权威：`packages/shared-contracts` 成为跨服务契约草案的权威位置；业务事实所有权仍归各自 host，未新增第二事实源。
- 状态迁移：`05-transition-lifetime.md` 的生命周期要求已映射到 `schemas/status.v1.schema.json`、OpenAPI、事件 schema 和工具 schema。
- 失败语义：工具失败分支必须携带 `error_code`、`message`、`trace_id` 和 `retryable`；Agent step 失败状态必须携带错误码、错误信息和可重试标记。
- 行为：仅新增本地契约校验命令，没有新增业务运行时行为。

## 验证摘要

Window 3 fix-1 复审结论：`approve`。

验证记录来自 `docs/harness/handoffs/phase-002-review-fix-1.md`：

- 上次 review 的两个 P1 finding 已关闭。
- `current-state.md` 已推进到可恢复的实现完成状态，随后由本交接推进到 `handoff_done`。
- 工具失败分支和 Agent step 失败分支已强制可追踪错误字段。
- `DOWNSTREAM_UNAVAILABLE` 已加入错误码目录，覆盖下游不可用失败分类。
- `powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1` 通过，输出 `Phase 002 contract validation passed.`
- 禁止实现产物扫描未返回任何文件。
- 未发现新的阻断项或必须修复项。

本 Window 4 也重新运行了契约校验命令和禁止实现产物扫描，结果通过。

## 剩余债务

- DEBT-004：pgvector/Milvus 或临时向量检索替代方案尚未选择，`index_ref` 继续保持存储无关。
- DEBT-006：高风险动作审批边界尚未实现；Phase 002 只定义了审批和策略检查契约。
- DEBT-007：AI 质量指标仍缺少基准集。
- DEBT-008：观测栈尚未落地。
- DEBT-009：多租户深度隔离策略尚未通过实现验证。
- DEBT-010：报告存储格式和导出格式尚未冻结。

已关闭债务：

- DEBT-005：跨服务契约 v1 草案、示例、清单和本地校验入口已落地。

遗留风险：

- Git 根目录是 `C:/Users/20978/VscodeProjects`，因此 `git status` 会显示 `AgentOps_Hub` 之外的既有同级目录脏状态。本交接不把这些同级目录变化归因于 Phase 002。
- Phase 002 契约仍是草案级权威，后续服务实现可能暴露字段细节或兼容性问题；任何变更都必须回到契约变更规则。

## Window 0 最新状态

- 当前阶段：Phase 002，状态为 `completed`。
- Steering 状态：`handoff_done`。
- 业务代码状态：未创建。
- 最新最终交接：`docs/harness/handoffs/phase-002-final.md`。
- 当前仓库事实：monorepo 占位骨架、开发文档、ADR、`packages/shared-contracts` v1 契约草案、示例、清单和契约校验脚本已存在。
- 当前契约事实：OpenAPI、事件、工具、错误码、状态枚举和示例已有 v1 草案；这些契约不代表服务已实现。
- Window 0 下一步：读取本文件、读取 `state/current-state.md`、检查 backlog 和 debt register，再选择并冻结下一阶段。

## 推荐给 Window 0 的候选输入

首要候选输入：

- Phase 003：`identity-service` 最小认证与租户模型。

推荐理由：

- 它是 Phase 002 之后 backlog 中的直接下一阶段。
- Phase 002 已冻结认证、当前用户、租户上下文、统一错误响应和审计字段草案，为身份域实现提供输入。
- 身份域是后续 ticket、workflow、Agent 工具调用和租户隔离的基础。

其他候选输入：

- Phase 004：如果用户明确要求先落地工单事实源，Window 0 需要说明为何跳过 identity 风险。
- Phase 008：如果用户要求优先处理高风险动作审批，Window 0 仍需基于 Phase 002 契约冻结实现范围。

范围提醒：

- Window 0 只做决策和范围冻结，不实现服务。
- Phase 003 若被选择，必须继续遵守 `shared-contracts` v1 草案，不得私自漂移字段、错误码或租户上下文。

## 本次交接修改文件

- `docs/harness/state/current-state.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-001-final.md`
- `docs/harness/handoffs/phase-002-final.md`
