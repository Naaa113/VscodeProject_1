# 债务登记

## 债务分级

| 等级 | 含义 | 处理规则 |
|---|---|---|
| D0 | 阻断 MVP 或造成安全/数据风险 | 当前阶段必须处理 |
| D1 | 影响架构一致性或后续阶段成本 | 下一阶段前必须有计划 |
| D2 | 可接受的局部技术债 | 记录 owner 和触发条件 |
| D3 | 优化项 | 不阻塞交付 |

## 当前债务

| ID | 等级 | 债务 | 影响 | 建议处理阶段 |
|---|---|---|---|---|
| DEBT-006 | D0 | 高风险动作审批边界未实现 | Agent 可能越权执行动作；Phase 005 仅以人工确认占位阻断，未实现审批实例或动作命令 | Phase 008，或用户要求时由 Window 0 提前评估 |
| DEBT-008 | D2 | 观测栈尚未落地 | 初期故障定位困难 | Phase 001 后逐步补齐 |
| DEBT-009 | D2 | 多租户深度隔离策略未验证 | 平台化阶段风险上升 | Phase 003 起持续验证 |
| DEBT-010 | D2 | 报告存储格式和导出格式未冻结 | Phase 006 已形成前端报告预览，Phase 007 已提供可追踪引用，但仍影响后续对象存储、导出和跨服务报告消费设计 | Phase 008 后按报告服务阶段收敛 |
| DEBT-011 | D2 | `identity-service` 仍缺少生产级数据初始化、PostgreSQL 迁移、refresh token、集中式 token 撤销和完整 RBAC 管理 API | Phase 003 只完成最小本地身份闭环，后续服务联调和生产化前仍需增强 | Phase 004 后按需要拆分，生产化前必须收敛 |
| DEBT-012 | D2 | `ticket-service` 仍缺少生产级 Spring Boot、PostgreSQL 迁移、服务间鉴权集成、受控样例数据初始化、数据库分页索引、工单状态变更审计和完整 SLA 引擎 | Phase 004 只完成最小本地工单闭环，后续 Agent、前端和生产化前仍需增强 | Phase 005 后按需要拆分，生产化前必须收敛 |
| DEBT-013 | D2 | `agent-service` 仍缺少生产级 LangGraph / FastAPI、真实模型适配、真实 HTTP 工具调用、消息队列 / SSE 集成、持久化存储和完整 JSON Schema 事件校验 | Phase 005 只完成最小本地 Agent 闭环，后续前端联调、RAG、审批和生产化前仍需增强 | Phase 006 后按需要拆分，生产化前必须收敛 |
| DEBT-015 | D2 | `rag-service` 仍缺少生产级 FastAPI、对象存储、pgvector / Milvus 或其他检索后端、持久化数据库、异步解析任务和真实上传入口 | Phase 007 只完成本地确定性轻量索引与 fixture 闭环，后续生产化前仍需替换或增强 | Phase 009 后按生产化阶段收敛 |
| DEBT-016 | D2 | RAG / Agent 质量基准集仍缺少生产代表性语料、人工标注流程、幻觉率和工具调用质量扩展指标 | Phase 007 只有 2 个最小 RAG eval 样例，不能代表生产质量 | Phase 009 |

## 已关闭债务

| ID | 原等级 | 关闭原因 | 验证来源 |
|---|---|---|---|
| DEBT-001 | D1 | 前端技术栈已收敛为 React + TypeScript。 | `docs/adr/0001-frontend-stack.md`, `docs/harness/handoffs/phase-001-review.md` |
| DEBT-002 | D1 | MVP 消息队列方向已收敛为 RabbitMQ。 | `docs/adr/0002-message-queue.md`, `docs/harness/handoffs/phase-001-review.md` |
| DEBT-003 | D1 | 主关系数据库方向已收敛为 PostgreSQL。 | `docs/adr/0003-relational-database.md`, `docs/harness/handoffs/phase-001-review.md` |
| DEBT-005 | D0 | 跨服务契约 v1 草案、示例、清单和本地校验入口已落地。 | `packages/shared-contracts/**`, `scripts/validate-contracts.ps1`, `docs/harness/handoffs/phase-002-review-fix-1.md` |
| DEBT-004 | D1 | Phase 007 已为 MVP RAG 闭环选择本地确定性轻量索引作为替代检索方案，`index_ref` 保持存储无关；生产级向量库选择另登记为 DEBT-015。 | `ai-services/rag-service/**`, `docs/harness/handoffs/phase-007-review-fix-1.md`, `docs/harness/state/current-state.md` |
| DEBT-007 | D1 | Phase 007 已新增本地 RAG eval，覆盖 `hit_rate` 与 `citation_accuracy` 计算方式；生产代表性质量基准扩展另登记为 DEBT-016。 | `ai-services/rag-service/evals/**`, `ai-services/agent-service/evals/complaint-analysis.phase-007.json`, `docs/harness/handoffs/phase-007-review-fix-1.md` |
| DEBT-014 | D1 | Phase 007 已将 `scripts/validate-contracts.ps1` 改为阶段感知规则，不再把已批准运行时目录判定为 forbidden implementation artifact。 | `scripts/validate-contracts.ps1`, `docs/harness/handoffs/phase-007-review-fix-1.md` |

## 债务进入规则

新增债务必须记录：

- ID。
- 等级。
- 触发背景。
- 影响范围。
- Owner。
- 处理阶段。
- 退出标准。

## 债务退出规则

债务只有在满足以下条件时才能关闭：

- 有代码、契约或文档变更证明已处理。
- 有验证记录。
- 若涉及架构选择，`state/current-state.md` 已更新。
- 若涉及跨服务契约，`04-contract-map.md` 或 `shared-contracts` 已更新。
