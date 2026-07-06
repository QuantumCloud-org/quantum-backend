#!/usr/bin/env bash
# 把两个 skill 装进 Rlues 提示词架构（CC + CX 双端）。
# 用法: ./install-to-rlues.sh <你的 Rlues 仓库本地路径> [版本, 默认 9.9.0]
set -euo pipefail
RLUES="${1:?用法: ./install-to-rlues.sh <Rlues本地路径> [版本]}"
VER="${2:-9.9.0}"
SRC="$(cd "$(dirname "$0")" && pwd)"
CC="$RLUES/vibeCoding/claude/$VER/.claude/skills"
CX="$RLUES/vibeCoding/codex/$VER/.codex/skills"
[ -d "$CC" ] || { echo "找不到 $CC"; exit 1; }
[ -d "$CX" ] || { echo "找不到 $CX"; exit 1; }
for skill in scaffold-module-gen project-data-reader; do
  for dst in "$CC" "$CX"; do
    mkdir -p "$dst/$skill"
    cp "$SRC/$skill/SKILL.md" "$dst/$skill/SKILL.md"
    echo "✓ $dst/$skill/SKILL.md"
  done
done
echo "完成。到 $RLUES 里 git diff 确认后自行 commit。"
