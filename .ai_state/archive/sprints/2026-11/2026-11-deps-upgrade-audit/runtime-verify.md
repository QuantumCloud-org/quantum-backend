# runtime-verify — 2026-11-deps-upgrade-audit

结论：通过。详见 `e2e-report.md`。

- **环境**：后端 jar 以 dev profile、本地缓存模式运行，连接在线 PostgreSQL 18.6；前端是 production 构建；中间用本地网关模拟生产的 `/talent_manger` 前缀；浏览器是 Playwright Chromium。
- **Cookie 场景**：6 个全部通过（登录时 Path 正确、刷新页面后保持登录、清除旧 Path=/ 的 cookie、业务请求不携带 refresh cookie、登出清除、无会话时启动）。
- **发现的问题**：
  - E2E-1（前端验证码竞态）：已修复，并用真实浏览器复测通过。
  - E2E-2（在线库缺两列）：已新增迁移脚本并在线执行，复测时后端不再报 SQL 错误。
- **未覆盖**：
  - Redis 模式：192.168.31.10:16397 从本机不可达，已记为 E-001。
  - RustFS 和 MCP 的真实客户端。
- **清理**：临时账号已删除；测试用的进程和端口已释放；/tmp 下的产物已删除；本机 PostgreSQL 已卸载。
