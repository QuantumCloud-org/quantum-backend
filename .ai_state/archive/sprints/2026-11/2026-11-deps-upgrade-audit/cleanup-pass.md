# polish / cleanup-pass — 2026-11-deps-upgrade-audit

本轮的主体就是有界清理：死代码删除（G01-G13）、重复实现合并（D01-D22）、魔法值和注释整理，由第二轮写者完成，详见 `merged-findings.md`。之后的 astra 复核（R01-R20）和两轮独立 review（B1-B4）的修复也都在同一个 worktree 内完成。所以没有另外再跑一轮 polish。

架构档已更新：`.ai_state/architecture/auth-security.md`、`auth-mcp-oauth.md`、`ARCHITECTURE.md`。

留下的 P3（已记入 issues）：
- D-001：迁移脚本中 login_location 缺 DEFAULT ''
- D-002：SESSION_TRANSITION 时不写登录日志
- D-003：sys_config 表在在线库缺失（代码无引用）
