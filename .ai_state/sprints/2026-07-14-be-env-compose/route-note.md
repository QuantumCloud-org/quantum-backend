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

## Scope 重定 (2026-07-14, 用户输入后)

- **用户已自备远程中间件**: application-dev.yml (本地未提交 diff) 指向 `192.168.31.10` —
  postgres `:15432` **UP** + redis `:16379` **UP** (nc 实测)。docker-compose 交付**取消** (无需求不建设, 铁律[反过度工程])。
- **Scope 收敛为补两笔欠据**: ① boot `mvn spring-boot:run -Dspring-boot.run.profiles=dev` + `curl /actuator/health` 200 回填 runtime-env.md 校准段 ② MCP OAuth 冒烟 (mcp-test-access.md §4)。
- **安全前提 (P0, 开工前处理)**: application-dev.yml 是 git tracked 文件, 用户的未提交 diff 含**明文内网口令** —
  绝不能 commit (密钥入 history 违 security-checklist P0)。处理方案二选一由用户拍板: (a) 密码改
  `${DB_PASSWORD:...}` 环境变量引用后提交结构、值走 shell; (b) 该 diff 永久保持本地不提交 (需自律, 易失手)。
- **顺手已清**: docs/ai/skills/ 孵化残留删除 (skill 唯一真相源=Rlues, 已演化为 quantum-codegen/quantum-data), README + ai-sprint-design §5.5 同步注记。
