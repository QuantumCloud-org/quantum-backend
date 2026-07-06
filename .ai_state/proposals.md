# Athena 框架改进提案 (proposals)

> 铁律[Hook 是进化器]: Stop 反思沉淀于此, 定期回流 Rlues 框架仓库。

## 2026-07-06 · 跨平台 sprint 的 generator 证据互认

- **现象**: delivery-gate (CC) 要求 System 路径 impl 必须在 CC 端 subagent-log.md 有 generator 记录;
  但 platforms_enabled=["cx"] 的项目里, impl 常由 CX 端 generator 管线完成 (本例: Convention Pack 经 PR #2/#3 合入),
  CC 端只做 review/polish — 门禁必然误报, 只能靠 skip_impl_subagent_check 豁免。
- **提案**: delivery-gate 增加第三种通过条件 — checklist.yaml 的 done 项带 CX 侧证据
  (evidence 字段指向真实产物 + evaluator Evidence Cross-Check PASS) 时, 视同 generator 记录;
  或 CX 端 subagent-tracker.py 在 ship 前把 spawn_agent 记录镜像写入 sprints/{slug}/subagent-log.md 统一格式。
- **触发 sprint**: 2026-07-06-ai-capability-architecture-design (豁免留痕见其 cleanup-pass.md)。

## 2026-07-06 · frontmatter 解析需要单一共享实现

- **现象**: CC 端 4 个 hook 各自复制粘贴 readFrontmatter, 其中"剥首尾引号"逻辑对带行尾注释的值出错,
  实际制造过脏 sprint 目录; 修复要同步 8 个文件 (部署 + Rlues 源) — 见 Rlues 366ee6b。
- **提案**: 抽 `~/.claude/hooks/lib/frontmatter.cjs` 共享模块 (或构建期注入), 消灭 4 份拷贝;
  CX 端 Python 同理 (read_field 已散在 5+ 文件, 目前实现碰巧无病但同样脆弱)。
