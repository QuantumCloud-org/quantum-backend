# Issues

| id | 类型 | 级别 | 一句话 | 发现于 | 去向 | 状态 |
|---|---|---|---|---|---|---|
| G-001 | gate | P2 | 熔断放行(3次): H2 evidence: cannot compute the source tree sha (not a git work tree?) | 2026-07-30-security-dependency-refresh | harness-iteration 分诊 | triage |
| G-002 | gate | P2 | 熔断放行(3次): H2 evidence: cannot compute the source tree sha (not a git work tree?) | 2026-07-30-security-dependency-refresh | harness-iteration 分诊 | triage |
| G-003 | gate | P2 | 熔断放行(3次): H2 evidence: cannot compute the source tree sha (not a git work tree?) | 2026-07-30-security-dependency-refresh | harness-iteration 分诊 | triage |
| G-004 | gate | P2 | 熔断放行(3次): H2 evidence: cannot compute the source tree sha (not a git work tree?) | 2026-07-30-security-dependency-refresh | harness-iteration 分诊 | triage |
| G-005 | gate | — | athena run 把 `mvn clean test` 判为 kind=other provable=false，Maven 项目无法产生可证明的 test 证据 (2026-11-deps-upgrade-audit) | 2026-11-deps-upgrade-audit | — | open |
| D-001 | debt | — | P3 迁移脚本 login_location 缺 DEFAULT ''，与 init.sql:252 不一致（2026-11-deps-upgrade-audit 复审） | 2026-11-deps-upgrade-audit | — | open |
| D-002 | debt | — | P3 LoginServiceImpl:108 SESSION_TRANSITION 不写登录日志，审计缺这次尝试（复审） | 2026-11-deps-upgrade-audit | — | open |
| E-001 | env | — | Redis 192.168.31.10:16397 从本机不可达（Surge TUN 代答，PG 经 Tailscale 可达），dev 已按用户指定改为 16397，需排查网络 | 2026-11-deps-upgrade-audit | — | open |
| D-003 | debt | — | init.sql 声明的 sys_config 表在在线库缺失，代码无引用，未创建 | 2026-11-deps-upgrade-audit | — | open |
