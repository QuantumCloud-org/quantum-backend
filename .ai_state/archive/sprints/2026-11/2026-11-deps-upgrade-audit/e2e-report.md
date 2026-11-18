# E2E 实测（2026-11-18）

环境：后端 worktree jar（dev profile、cache.mode=local、refresh-cookie-path=/talent_manger/auth、refresh-cookie-secure=true、captcha-required=false）连在线 PostgreSQL 18.6（192.168.31.10:15432，经 Tailscale）；前端以 production 模式构建（产物前缀 /talent_manger）；本地网关脚本把 /talent_manger/* 去前缀后转发到后端；浏览器用 Playwright Chromium。测试账号 e2e_cookie_tmp 为临时账号，测完删除。

## Cookie Path 场景
| # | 场景 | 结果 | 证据（网关日志） |
|---|---|---|---|
| 1 | 登录后 refresh cookie 的 Path | ✅ `Path=/talent_manger/auth`，HttpOnly，Secure，SameSite=Strict，Max-Age=604800 | `POST /auth/login -> 200 set-cookie=…Path=/talent_manger/auth; Max-Age=604800` |
| 2 | 刷新页面后保持登录 | ✅ 启动时调 refresh 返回 200 并轮换 cookie，页面停留在 `/` | `POST /auth/refresh -> 200 cookie=yes` |
| 3 | 清理旧的 Path=/ cookie | ✅ 预置的 legacy cookie 在登录后消失 | `set-cookie=… \| refresh_token=; Path=/; Max-Age=0` |
| 4 | 业务请求不带 refresh cookie | ✅ `/system/menu/getRouters` 请求里 cookie=no | 网关日志 |
| 5 | 登出 | ✅ 两个 Path 的 cookie 都被清除 | `POST /auth/logout -> 200 set-cookie=Max-Age=0 ×2` |
| 6 | 无会话时启动 | ✅ refresh 返回 401，并下发两条清除指令 | `POST /auth/refresh -> 401` |

## E2E 发现
| ID | 级别 | 问题 | 处置 |
|---|---|---|---|
| E2E-1 | P1 | 冷启动登录页时，启动 refresh 返回 401 会触发会话作废，把同时在途的 captcha 200 响应一并丢弃，导致「验证码加载失败」、登录按钮一直禁用 | 前端修复，修后用浏览器复测 |
| E2E-2 | P1 | 在线库缺 init.sql 已声明的 `sys_user.login_location` 和 `sys_dict_type.business` 两列，登录信息回写报 PSQLException；S08 的字典过滤依赖 business 列 | 新增 `deploy/migrations/2026-11-18-sync-init-columns.sql`（幂等），已在线执行，复测后不再报错 |
| E2E-3 | — | 在线库缺 `sys_config` 表，但代码中无引用 | 不创建，记为遗留 |
| E2E-4 | 环境 | Redis 192.168.31.10 的 16379 和 16397 端口都能建立 TCP 连接，但都不响应 RESP PING，与必定关闭的端口 1 表现相同（Surge TUN 代答），实际不可达 | 待用户确认 Redis 端口和可达性 |
