# Route Note — quantum-mcp S3 implementation

- 日期: 2026-07-07
- 感知: S3 preflight design 已 ship; 用户明确进入代码实现并要求最终合并 main、推送、清理 worktree/branch。
- 候选: Feature(单模块新增) vs System(新 Maven module + server 依赖 + security chain + ai_state/architecture)。跨模块且 ≥5 文件, 护栏取 System。
- 四维权衡: 爆炸半径=认证/配置/系统服务/文档; 可逆性=单分支可回滚; 紧急度=建设型; 不确定性=验收标准已由 preflight design 冻结。
- 决策: path=System, stage=impl→runtime-verify→review→polish→ship, worktree=`quantum-mcp-s3-impl`, branch=`codex/quantum-mcp-s3-impl`。
- 置信度: 0.9
- 廉价退出点: 若必须改造普通 `TokenAuthenticationFilter` 才能隔离 token, 立即 re-route; 当前实现用独立 `OAuthBearerAuthenticationFilter`, 未触发退出点。
