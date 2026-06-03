# 本地开发基线

## 目的

Phase 001 建立稳定的 monorepo 目录和骨架校验命令，用于证明仓库结构存在，并避免业务实现过早进入未批准的 host。

## 骨架检查

从仓库根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-skeleton.ps1
```

该脚本只读。它检查必需占位目录，并拒绝 Phase 001 骨架根目录下未批准的框架项目文件、源目录、迁移目录、路由目录、控制器目录、组件目录、依赖目录和 Dockerfile。

## 契约检查

Phase 002 新增只读契约校验命令：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-contracts.ps1
```

该脚本检查必需的 `shared-contracts` 文件，解析 JSON schema 与示例，按文本校验必需 OpenAPI 路径，并拒绝未批准 host 中出现框架项目或运行时实现产物。

## identity-service 检查

Phase 003 新增最小身份服务校验命令：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-identity-service.ps1
```

该脚本检查身份服务文件、身份域 schema、认证契约补齐和 JWT 载荷字段，然后进入 `services/identity-service` 运行 `mvn test`。

也可以直接运行：

```powershell
Set-Location services/identity-service
mvn test
```

`identity-service` 使用 `.mvn/maven.config` 指向 `identity-service.maven.xml`，以便保留 Phase 002 契约校验脚本的只读性质，同时让 Phase 003 可以独立构建身份服务。

## ticket-service 检查

Phase 004 新增最小工单服务校验命令：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-ticket-service.ps1
```

该脚本检查工单服务文件、客户和工单 schema、工单 OpenAPI 补齐、`ticket.search.v1` 工具契约和租户隔离相关测试，然后进入 `services/ticket-service` 运行 `mvn test`。

也可以直接运行：

```powershell
Set-Location services/ticket-service
mvn test
```

`ticket-service` 使用 `.mvn/maven.config` 指向 `ticket-service.maven.xml`。本阶段只支持最小 Java + H2 本地闭环，不引入 Spring Boot、网关、Agent、RAG、审批或前端能力。

## 后续阶段

后续阶段应复用这里创建的 host 目录，但必须等待各自的 architect handoff 后，才能新增契约、服务、页面、测试或运行时配置。

## 密钥处理

- `.env.example` 只记录非密钥变量名。
- 真实密钥、token、模型凭据、生产数据、客户数据和敏感测试材料必须留在仓库外。
- 不要把生成的凭据放入占位目录。
- `identity-service` 本地运行必须通过 `IDENTITY_JWT_SECRET` 提供 JWT 签名密钥；仓库内只保留占位说明和测试期临时值。
- `ticket-service` 本地运行必须通过 `TICKET_JWT_SECRET` 提供 JWT 签名密钥；测试只使用本地临时值。
