# Route Note — quantum-mcp S3 preflight design (2026-07-07)

- 感知: 上一 FE/BE Convention Pack sprint 已 ship; git 现场无多余 worktree/分支, 但 `.ai_state` 存 active_worktrees 过时记录。S3 已由 2026-07-06 用户拍板 OAuth 2.1, 仍缺 token 存储 / consent / manifest 接口冻结。
- 候选: Quick(只清状态) vs System/design-only(S3 开工前设计)。用户要求“先做 1, 再做 2, 合并推 main”, 且 S3 涉及认证、授权、数据域、MCP 协议, 取 System/design-only。
- 四维: 爆炸半径=系统级但本轮只写文档; 可逆=单 commit 可回滚; 紧急度=建设性收口; 不确定性=验收标准明确。
- 决策: path=System, stage=design-only→ship, confidence=0.86。
- 验收: 1) 旧 worktree 状态收口; 2) S3 三个前置设计项落地且引用官方来源/本仓源码; 3) main 推送后无多余 worktree/分支。
