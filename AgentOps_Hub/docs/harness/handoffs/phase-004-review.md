# Phase 004 评审交接

## 结论

approve

允许进入 Window 4：是。

## 当前 review 模式

初次 Review。

恢复依据：

- `docs/harness/handoffs/phase-004-implementation.md` 已存在。
- `docs/harness/handoffs/phase-004-review.md` 在本次写入前不存在。
- 不存在 `phase-004-final.md`。
- 不存在 Phase 004 Fix Pass。

## 已读取的文件

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
- `docs/harness/handoffs/steering-decision-phase-004.md`
- `docs/harness/handoffs/phase-004-architect.md`
- `docs/harness/handoffs/phase-004-implementation.md`
- 相关 Phase 002 / Phase 003 review、fix、final handoff，用于区分既有 dirty 变更和本阶段变更来源。

## findings

未发现阻断或需要 Fix Pass 的问题。

证据摘要：

- belongs：`services/ticket-service` 新增客户、工单和工单审计表，符合工单事实源边界；证据见 `services/ticket-service/app/main/resources/schema.sql:1`、`services/ticket-service/app/main/resources/schema.sql:12`、`services/ticket-service/app/main/resources/schema.sql:35`。
- authority：认证只消费 token、租户 header 和权限声明，不复制身份事实；证据见 `services/ticket-service/app/main/java/com/agentops/ticket/TicketAuthService.java:18`、`services/ticket-service/app/main/java/com/agentops/ticket/TicketAuthService.java:19`、`services/ticket-service/app/main/java/com/agentops/ticket/TicketAuthService.java:24`。
- contract：三个工单 API 均补齐 `bearerAuth`、租户 header、trace header、过滤参数和统一错误响应；证据见 `packages/shared-contracts/openapi/agentops-api.v1.yaml:61` 至 `packages/shared-contracts/openapi/agentops-api.v1.yaml:121`、`packages/shared-contracts/openapi/agentops-api.v1.yaml:122` 至 `packages/shared-contracts/openapi/agentops-api.v1.yaml:145`。
- contract：工单状态、优先级、客户摘要、SLA 和创建请求二选一客户来源已进入契约；证据见 `packages/shared-contracts/openapi/agentops-api.v1.yaml:523` 至 `packages/shared-contracts/openapi/agentops-api.v1.yaml:603`。
- transition：创建固定写入 `open`，列表按租户检索，详情按租户查找并隐藏跨租户资源；证据见 `services/ticket-service/app/main/java/com/agentops/ticket/H2TicketRepository.java:114`、`services/ticket-service/app/main/java/com/agentops/ticket/H2TicketRepository.java:140`、`services/ticket-service/app/main/java/com/agentops/ticket/H2TicketRepository.java:158`。
- behavior：HTTP 流程覆盖创建、列表、详情、跨租户 404、缺少 trace 和缺权限统一错误；证据见 `services/ticket-service/app/test/java/com/agentops/ticket/TicketHttpServerTest.java:38`、`services/ticket-service/app/test/java/com/agentops/ticket/TicketHttpServerTest.java:96`、`services/ticket-service/app/test/java/com/agentops/ticket/TicketHttpServerTest.java:132`。

## Window 1 acceptance 是否满足

满足。

- 已创建 `services/ticket-service` 最小 Java + H2 运行时。
- 已实现 `GET /api/tickets`、`POST /api/tickets`、`GET /api/tickets/{id}`。
- 工单响应包含 `tenant_id`、客户摘要、状态、优先级、分类、SLA 截止时间和审计字段。
- 新建工单默认状态为 `open`，未新增状态变更 API。
- 列表和详情按租户隔离，跨租户详情访问返回 `RESOURCE_NOT_FOUND`。
- `ticket.search.v1` 保持只读、`requires_approval: false`，并补齐客户、分类、SLA 和租户字段。
- 未新增 Agent、RAG、workflow、前端、网关、通知、事件契约或高风险动作执行能力。

## AI / RAG eval 检查

本阶段不是 Agent 或 RAG 阶段，未引入 LangGraph、LLM 调用、RAG 检索、报告生成或真实工具调用执行，因此不需要运行 AI eval 样例集。

已检查边界：

- `rg` 扫描未发现 `agent-service` 桥接、workflow adapter、RAG、LLM、LangGraph 或高风险动作执行代码进入 `services/ticket-service`。
- `ticket.create_followup.v1`、`knowledge.search.v1`、`report.save.v1` 和 `agent.step.completed.v1` 的现有 dirty 变更来源于 Phase 002 Fix Pass，已由 `phase-002-review-fix-1.md` 评审通过；本阶段未把这些契约变成运行时能力。

## 验证命令和结果

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1
```

结果：通过，输出 `Phase 002 contract validation passed.`。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-ticket-service.ps1
```

结果：通过；脚本内部运行 `mvn test`，`ticket-service` 共 7 个测试，0 失败，0 错误，并输出 `Phase 004 ticket-service validation passed.`。

```powershell
mvn test
```

运行目录：`services/ticket-service`。

结果：通过，`ticket-service` 共 7 个测试，0 失败，0 错误。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-identity-service.ps1
```

结果：通过，`identity-service` 共 8 个测试，0 失败，0 错误，并输出 `Phase 003 identity-service validation passed.`。

## 非阻断观察

- `docs/harness/state/current-state.md` 仍描述 Phase 003 已完成状态，Phase 004 尚未 final；这符合 Window 4 才更新当前状态的流程。Window 4 需要在 handoff 时更新它。
- 工作区仍保持多阶段 dirty / untracked 状态；本次评审未清理、回滚或 stage 任何既有变更。
- Phase 004 建议新增的 D2 债务合理：`ticket-service` 当前仍是最小 H2 本地闭环，生产化前需要补齐 PostgreSQL 迁移、服务间鉴权集成、受控初始化和状态变更审计。

## 复审说明

不适用。本次为 Phase 004 初次 Review，不存在上一轮 require fixes finding。
