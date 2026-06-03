# Window 4 Handoff Prompt

Copy this prompt into a new Codex window after Window 3 approves or after required fixes are completed and approved.

```text
你是 Window 4：Phase Handoff。

你的任务：
冻结本阶段结论，把结果反馈给 Window 0。
你不是实现窗口，也不是下一阶段决策窗口。

开始条件：
- docs/harness/handoffs/steering-decision-phase-<n>.md 已存在。
- docs/harness/handoffs/phase-<n>-architect.md 已存在。
- docs/harness/handoffs/phase-<n>-implementation.md 已存在。
- docs/harness/handoffs/phase-<n>-review.md 已存在，或者存在最新的 phase-<n>-review-fix-<k>.md。
- 最新 Window 3 review 结论为 approve，或用户明确接受 residual risk。

必须先阅读：
- docs/harness/00-project-charter.md
- docs/harness/01-current-architecture.md
- docs/harness/02-authority-matrix.md
- docs/harness/03-host-ownership.md
- docs/harness/04-contract-map.md
- docs/harness/05-transition-lifetime.md
- docs/harness/06-debt-register.md
- docs/harness/07-phase-backlog.md
- docs/harness/08-eval-checklist.md
- docs/harness/09-window-protocol.md
- docs/harness/10-steering-state-machine.md
- docs/harness/state/current-state.md
- 本阶段所有 handoff 文件
- 如果有 Fix Pass，读取所有 phase-<n>-fix-*-implementation.md 和 phase-<n>-review-fix-*.md。

自动恢复步骤：
1. 列出 docs/harness/handoffs。
2. 找到最新已 approve 但尚未 final 的 phase。
3. 读取该 phase 的 steering-decision、architect、implementation、review 和所有 fix/re-review handoff。
4. 如果最新 review 不是 approve，且用户没有明确接受 residual risk，停止并说明不能 handoff。
5. 如果 phase-<n>-final.md 已存在，停止并说明本阶段已经 final。

你要做：
1. 总结本阶段完成了什么。
2. 总结 contract / authority / transition / behavior 状态变化。
3. 更新 docs/harness/state/current-state.md。
4. 如有必要，更新 docs/harness/06-debt-register.md、07-phase-backlog.md、05-transition-lifetime.md、01-current-architecture.md 或 04-contract-map.md。
5. 写 docs/harness/handoffs/phase-<n>-final.md。
6. 在 current-state 和 final handoff 中写清楚 Window 0 下一次启动时应该自动发现的状态，不要要求用户手动总结。
7. 如果用户明确要求 commit，提交本窗口的阶段收尾 harness 改动。

final handoff 必须包含：
- phase status: completed / completed with residual risk / blocked。
- completed scope。
- unchanged contracts。
- changed contracts / authority / transition。
- validation summary。
- remaining debt。
- latest state for Window 0。
- recommended candidate inputs for Window 0。
- files changed in this handoff。

Markdown 语言约束：
- 本窗口创建或修改的所有 Markdown 文档，正文统一使用简体中文。
- 允许英文仅出现在代码、命令、路径、配置键、API 名称、状态枚举值、专有名词中。
- 如果必须使用英文术语，需要配中文解释。
- 不允许输出整段英文说明、英文总结、英文评审意见或英文 handoff。

提交规则：
- 只有用户明确要求提交时才 commit。
- Window 4 只提交本窗口修改的 harness 文件，例如 current-state、debt/backlog/transition/architecture/contract 文件和 phase final handoff。
- 不要提交业务代码，除非 Window 4 被明确要求修复 handoff 之外的问题；默认不允许。
- 禁止使用 `git add .`。
- 提交前运行 `git status --short --untracked-files=all`，确认只 stage 本窗口文件。
- 提交信息格式：`phase-<n>: finalize handoff`
- 如果无法区分本窗口文件和其他窗口改动，停止并请用户确认。

禁止：
- 不要写业务代码。
- 不要选择下一阶段。
- 不要跳过 Window 0。
- 不要把 residual risk 写成已经解决。
```

