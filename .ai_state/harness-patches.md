# Harness 补丁台账

> delivery-gate.cjs 等 hook 源码内 "台账见 .ai_state/harness-patches.md" 所指文件。
> 2026-07 的历史补丁记录在当时的对话与 proposals.md 中, 本文件自 2026-09-07 起实体化。

## 2026-09-07 · findAiState git-boundary fix

- **改动**: 每份复制的 `findAiState()` 在未命中 `.ai_state` 时先查 `path.join(current, ".git")`,
  命中即 return null (uninitialized), 不再爬出本仓继承父项目状态。4 种变体统一打补丁 (+2 行/文件)。
- **范围**: 部署 `~/.claude/hooks/` 16 文件 (逐文件备份 `*.bak-20260907`) + 源码库
  `~/workspace/Rlues/vibeCoding/claude/9.9.9/.claude/hooks/` 15 文件 (git 工作区改动, 未 commit,
  留用户审; token-usage-collector.cjs 仅存在于部署侧)。old-version/codex/pi-agent 历史快照未动。
- **验证**: 31/31 `node --check` 通过; 每文件行为测试 4/4 (git 根无 .ai_state→null / 自有→命中 /
  子仓自有→命中 / 无 .git 目录链→行为不变); 实机冒烟 session-start @ quantum-agent exit 0。
- **动机**: proposals.md 2026-09-07 条 (quantum-agent 删 .ai_state 后被父项目 ESCALATED ship 门禁误拦)。
- **执行**: 主 agent 直改 (用户点名豁免 fable5-no-direct-writes 铁令); 补丁脚本 /tmp/fas-patch/patch-findaistate.cjs。
