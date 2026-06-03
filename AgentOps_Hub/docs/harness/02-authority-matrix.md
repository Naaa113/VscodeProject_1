# 权威矩阵

## 决策权威

| 决策主题 | 权威来源 | 修改方式 |
|---|---|---|
| 项目目标与非目标 | `00-project-charter.md` | Steering Window 批准 |
| 当前架构事实 | `01-current-architecture.md` | 每阶段结束更新 |
| 服务所有权 | `03-host-ownership.md` | Phase Architect 提案，Steering 批准 |
| 跨服务契约 | `04-contract-map.md` 和未来 `packages/shared-contracts` | 契约变更必须先评审 |
| 阶段计划 | `07-phase-backlog.md` | 每阶段开始前冻结本阶段范围 |
| 验收标准 | `08-eval-checklist.md` | Review/Eval Window 维护 |
| 当前状态 | `state/current-state.md` | 每个窗口交接时更新 |
| 窗口协议 | `09-window-protocol.md` | Steering Window 批准 |

## 实现权威

| 组件 | 主要权威 | 不得越权 |
|---|---|---|
| `web-console` | 前端阶段实现窗口 | 不直接绕过网关访问内部服务 |
| `api-gateway` | 平台/后端阶段实现窗口 | 不承载业务事实和 Agent 推理 |
| `identity-service` | 身份域实现窗口 | 不管理工单或 Agent 业务状态 |
| `ticket-service` | 工单域实现窗口 | 不直接调用 LLM 决策高风险动作 |
| `workflow-service` | 审批与动作域实现窗口 | 不绕过审计执行动作 |
| `agent-service` | AI 编排实现窗口 | 不直接写入业务事实表，不绕过工具契约 |
| `rag-service` | 知识库与检索实现窗口 | 不生成未经引用约束的业务结论 |
| `shared-contracts` | 架构窗口与评审窗口共同维护 | 不跟随单个服务私自漂移 |

## 人工确认权威

| 场景 | 必须确认方 | 最低规则 |
|---|---|---|
| 标记高风险客户 | 客服主管或授权运营人员 | 需要原因、来源和操作者审计 |
| 自动派单或创建跟进任务 | 业务负责人或审批规则 | 需要幂等键与审批记录 |
| 关闭、撤销或升级工单 | 客服主管 | MVP 可只定义契约，后续实现 |
| 对外通知客户或第三方 | 授权审批人 | 必须记录通知内容和触达目标 |
| 低置信度 Agent 结论 | 人工处理队列 | 不允许自动执行高风险动作 |

## 契约变更规则

- 任意跨服务字段、枚举、事件、错误码和状态流转变更都必须先进入契约文档。
- 已发布契约不得无版本号破坏性修改。
- Python Agent 工具调用 Java API 时只能使用公开的内部工具契约。
- 前端不得基于服务内部数据库结构构造页面逻辑。
- 数据库表结构不等于 API 契约，二者需要显式映射。

## 冲突处理

当文档之间出现冲突时，优先级如下：

1. 用户最新明确指令。
2. `state/current-state.md` 中记录的已批准状态。
3. `00-project-charter.md` 与 `02-authority-matrix.md`。
4. `04-contract-map.md` 中已冻结契约。
5. 阶段 backlog 与 handoff。
6. 早期项目材料。

