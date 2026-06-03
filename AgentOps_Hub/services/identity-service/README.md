# identity-service

Phase 003 在本目录落地最小身份域服务。服务只拥有租户、用户、角色、权限、JWT 签发校验和身份审计事实，不管理工单、知识库、Agent、审批、通知或前端行为。

## 本地命令

```powershell
Set-Location services/identity-service
mvn test
```

本目录使用 `.mvn/maven.config` 指向 `identity-service.maven.xml`，源代码目录为 `app/main/java`，测试目录为 `app/test/java`。这样可以在不修改 Phase 002 契约校验脚本的前提下运行 Phase 003 身份服务测试。

## 运行服务

运行前必须提供本地 JWT 签名密钥：

```powershell
$env:IDENTITY_JWT_SECRET="replace-with-local-development-secret-at-least-32-chars"
Set-Location services/identity-service
mvn exec:java
```

可选环境变量：

- `IDENTITY_PORT`：默认 `8081`。
- `IDENTITY_DB_URL`：默认 `jdbc:h2:file:./data/identity-service`。
- `IDENTITY_TOKEN_TTL_SECONDS`：默认 `1800`。

默认运行入口只初始化身份域 schema，不写入演示租户、用户、角色或权限数据。测试数据只在测试 fixture 中显式创建，避免被本地或生产路径当作真实身份事实。

## API

- `POST /api/auth/login`
- `GET /api/auth/me`
- `/actuator/health`，仅本地健康检查，不属于跨服务业务契约。

所有失败响应保持统一形状：`error_code`、`message`、`trace_id`、`retryable`，必要时带 `details`。
