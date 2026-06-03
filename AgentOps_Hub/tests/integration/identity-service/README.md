# identity-service 集成测试

Phase 003 的集成测试当前由 `services/identity-service/app/test/java/com/agentops/identity/IdentityHttpServerTest.java` 承担。测试会启动本地 HTTP 服务，覆盖 `POST /api/auth/login` 到 `GET /api/auth/me` 的最小闭环，并校验统一错误响应形状。

运行命令：

```powershell
Set-Location services/identity-service
mvn test
```
