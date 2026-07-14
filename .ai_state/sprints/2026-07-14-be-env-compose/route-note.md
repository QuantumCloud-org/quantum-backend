# Route Note — 2026-07-14-be-env-compose

> v9.9.3 路由审议落盘。quantum 完善阶段① 第 1 步 (见 architecture/blockers-and-roadmap.md)。

- **输入**: "解环境总闸": 补 `deploy/docker-compose.yml` (postgres16 + redis, 挂 init.sql 自动初始化) → 实跑起环境 → 补两笔欠据 (boot `/actuator/health` 200 回填 runtime-env.md + MCP OAuth 冒烟)
- **候选**: Quick (证据: 单个 compose 文件 + 文档回填, 无业务代码; 反对: compose 需实跑验证 + init.sql 挂载细节有不确定性) vs Feature (证据: 交付物含验证环; 反对: 无源码改动, generator 无用武之地)
- **权衡**: 爆炸半径=deploy/ 目录 + 本机容器 · 可逆=高 (compose down -v 即清) · 紧急=中 (阻塞 F7 与全部动态验证) · 不确定性=低 (init.sql 已存在, 端口/口令见 application-dev.yml)
- **决策**: **Quick** · 置信度 0.8 (主 agent 直做, 实跑验证为准)
- **假设**: 本机 Docker 可用; init.sql 可被 postgres 官方镜像 docker-entrypoint-initdb.d 直接消费
- **廉价退出**: 若 Docker 不可用或 init.sql 与 PG16 不兼容 → 停, 输出手动安装步骤清单交用户
- **家**: sprint 档案落本仓 .ai_state
- **状态注记**: 上一 sprint (be-runtime-contract-hardening) 已 ship (28fdf8b) 收口; 本立项同时消解 9.9.3 gate 对旧 sprint 的追溯检查 (schema 误拦缺陷已回流 proposals P8)
