# Runtime Verify — be-env-compose (2026-07-14)

> Quick 路径, 主 agent 直做。scope 已重定 (见 route-note): 用户自备远程中间件 (本机 VM), compose 取消,
> 交付 = 两笔欠据实证 + docs/ai/skills 残留清理。

## 测试场景 (实跑)

| 场景 | 命令 | 实际输出 | 判定 |
|---|---|---|---|
| 中间件连通 | `nc -z <dev.yml 所指 host> 15432 / 16379` | 均 succeeded (UP) | ✅ |
| 打包 | `mvn -pl quantum-server -am -DskipTests package` | exit 0 | ✅ |
| **boot (欠据①)** | `java -jar quantum-server/target/*.jar --spring.profiles.active=dev --ai.mcp.enabled=true` | `Application 'quantum-backend' is running!` | ✅ |
| **health 200 (欠据①)** | `curl /actuator/health` | `{"groups":["liveness","readiness"],"status":"UP"}` · 200 | ✅ |
| **OAuth 元数据 (欠据②)** | `curl /.well-known/oauth-protected-resource` + `/oauth-authorization-server` | 均 200, scopes/endpoints 齐 | ✅ |
| **/mcp fail-closed (欠据②)** | `POST /mcp` 无 token | `401 invalid MCP OAuth token` | ✅ |
| teardown | kill 8080 进程 | clean | ✅ |

## 交付

1. runtime-env.md: 校准状态 BLOCKED → 2026-07-14 实跑实证 (证据内嵌); 依赖段改为"连接参数以开发者本地
   dev.yml 为准" (repo-safe, 不硬编码环境细节)。
2. docs/ai/skills/ 孵化残留删除 + README/ai-sprint-design §5.5 注记 (skill 真相源 = Rlues)。
3. 密码处置: 用户拍板方案 b (dev.yml diff 保持本地不提交, 本机 VM 风险自担); 已验证该文件不在任何 commit。

## Reflect

- [x] 环境总闸解除 — F7 (真·动态 E2E) 的唯一硬前置已消失, 随时可立项。
- [ ] OAuth 深链 (authorize→consent→token→tools/call 带测试账号) 留 F7。
- [ ] MinIO (file.endpoint :9000) 未探测 — 本次 boot 未触发文件模块报错, 文件上传功能验证留业务 sprint。

## VERDICT: PASS
