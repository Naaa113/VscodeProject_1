# Steering State Machine

## 目的

Steering State Machine 用于控制阶段推进，防止项目在未冻结范围、未定义契约或未完成评审时进入实现。

## 状态

```text
idle
  -> phase_selected
  -> architecture_ready
  -> contracts_ready
  -> implementation_ready
  -> implementation_done
  -> review_done
  -> handoff_done
  -> idle
```

## 状态定义

| 状态 | 含义 | 退出条件 |
|---|---|---|
| `idle` | 没有正在执行的阶段 | Steering 选择 phase |
| `phase_selected` | 阶段目标已选定 | 范围和非目标写清 |
| `architecture_ready` | 阶段架构和 host 边界清晰 | 契约影响已识别 |
| `contracts_ready` | 契约已定义或确认无需变更 | 验收标准已确认 |
| `implementation_ready` | 可以进入实现 | Implementer 接手 |
| `implementation_done` | 实现完成并自测 | Review/Eval 接手 |
| `review_done` | 评审和验证完成 | 阻断项清零或 Steering 豁免 |
| `handoff_done` | 交接完成 | 当前状态更新 |

## 转换规则

### `idle -> phase_selected`

必须有：

- phase ID。
- 目标。
- 范围。
- 非目标。

### `phase_selected -> architecture_ready`

必须有：

- 受影响 host。
- 数据所有权。
- 状态生命周期。
- 已知债务。

### `architecture_ready -> contracts_ready`

必须有：

- API、事件、工具或数据库状态契约。
- 版本策略。
- 示例 payload。
- 错误处理。

### `contracts_ready -> implementation_ready`

必须有：

- 验收清单。
- 测试策略。
- 回滚或降级策略。

### `implementation_done -> review_done`

必须有：

- 测试结果。
- 手动验证记录。
- 变更摘要。
- 未完成项。

### `review_done -> handoff_done`

必须有：

- 阻断项处理结论。
- 债务登记。
- 当前状态更新。
- 下一阶段建议。

## 阻断规则

不得推进状态机的情况：

- 当前 phase 范围不清。
- 契约缺失但实现已依赖跨服务字段。
- 测试无法运行且无解释。
- 高风险动作无审批边界。
- 当前状态文档未更新。

