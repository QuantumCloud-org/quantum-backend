---
sprint_slug: "2026-07-30-security-dependency-refresh"
path: "System"
stage: "polish"
scope: "依赖升级切片 (安全报告切片不在本次 polish 范围)"
---

# Cleanup Pass — 依赖升级切片

改动面 1 文件 8 行 (root `pom.xml`), 属绿区, 主 agent 直做, 未 spawn polish_worker。

## 扫描范围

本切片只改 root `pom.xml` 的版本治理, 无源码改动, 因此 polish 聚焦 POM 自身卫生:

1. 删除 3 个 BOM import 后是否留下死属性
2. 版本号是否出现多处存放 (DRY)
3. 模块 POM 是否有冗余版本钉死
4. 文档与实际是否一致

## Findings

### F1 (P1, 已修) — `spring-boot.version` 属性在删除 BOM import 后成为死配置

**事实**: 该属性唯一的 Maven 消费者是被删掉的 `spring-boot-dependencies` import。删除后:

- 全仓检索 (不限文件类型, 含 CI/Dockerfile/脚本) 只剩两处命中: `pom.xml:34` 自身声明, 与 `banner.txt:20`。
- `banner.txt` 不受 Maven 过滤: `spring-boot-starter-parent` 的默认资源过滤只覆盖 `application*.yml/yaml/properties`, 且分隔符是 `@` 而非 `${}`; 本仓也没有自定义 `<resources>` / `<filtering>` / maven-resources-plugin 配置。
- **实证**: `quantum-server/target/classes/banner.txt:20` 中 `${spring-boot.version}` 仍是字面量, 证明 Maven 从未替换它 — 它由 Boot 运行期 `ResourceBanner` 解析, 与 POM 属性无关。
- **上游一手证据** (polish-worker 直接读本地 repo 中的 parent POM, 非凭记忆):
  - `spring-boot-starter-parent-4.1.0.pom:40-56` — 过滤型 `<resource>` 的 includes 只有
    `**/application*.yml|yaml|properties`; `banner.txt` 落在第二个 `<filtering>false</filtering>` 块。
  - 同文件 `157-165` — maven-resources-plugin 固定 `<delimiter>@</delimiter>` 且
    `<useDefaultDelimiters>false</useDefaultDelimiters>`, 因此即便在被过滤的文件里 `${...}` 也不是占位符。
  - `grep -c 'spring-boot\.version'` 在 `spring-boot-starter-parent-4.1.0.pom` 与
    `spring-boot-dependencies-4.1.0.pom` 上均返回 `0` — 上游同样没有消费者。
  - 本地 repo 路径 `/Users/mi_manchi/workspace/Tools/apache-maven-3.9.14/repository` (无 `~/.m2`, 见 `conf/settings.xml:6`)。

  检索式覆盖了 coding-standards 要求的「类型系统看不见的依赖」对应面: 不限文件类型 (含 CI/Dockerfile/脚本)、
  资源过滤间接消费、上游 POM 继承链三条路径。

**为什么要修**: `<parent>` 已硬写 `4.1.0`, 属性再写一遍 = 同一版本号两处存放。下次升 Boot 只会改 `<parent>`, 这个无人读取的属性会静默停在 4.1.0 —— 正是本 sprint 刚修掉的「属性文本与实际解析脱节」同一类缺陷, 留着等于把刚填的坑重挖一遍。

**修法**: 删除属性, 原位留注释说明 Boot 版本单点声明于 `<parent>`, 以及 banner 占位符的真实解析来源。

### F2 (P2, 不修) — `postgresql.version` 同时是 Boot 覆盖属性和显式 dependencyManagement 条目

root `dependencyManagement` 显式钉了 `org.postgresql:postgresql:${postgresql.version}`, 而该属性名同时是 Boot BOM 的官方覆盖属性名。两条路径指向同一版本, 显式条目优先, 实测解析 42.7.13 正确。

不修: 两者不冲突且值同源 (同一属性), 不存在漂移面; 删掉显式条目属于无收益改动, 反而降低可读性。记录备查。

### F3 (P2, 无问题) — 模块 POM 无冗余版本钉死

全仓 `pom.xml` 中不带 `${}` 的硬编码 `<version>` 只有 1 处: root `pom.xml:21` 的 `<parent><version>4.1.0</version>`, 这是 parent 坐标必需的字面量, 不可属性化 (Maven 解析 parent 早于属性插值)。子模块无版本钉死。

### F4 (P1, 已修) — 架构档案缺依赖治理约定

删除 3 个 BOM import 是一条会被后人「好心加回来」的决策 (看上去像漏了 BOM)。已在 `architecture/ARCHITECTURE.md` 新增「依赖治理」节, 写明单一治理入口、覆盖属性清单、以及完成证据取实解析版本而非 property 文本。

## post-fix 实测

F1 由 `polish-worker` subagent 落盘 (System 路径 polish 法定写者), 改动 `-1/+2` 行, 仅 root `pom.xml`;
`grep -c '<spring-boot.version>' pom.xml` → `0`。主 agent 复核解析集合不变性。

| 项 | 命令 | 实测 |
|---|---|---|
| 全量回归 | `mvn clean test`(全量日志 `/tmp/polish-clean-test.log`, 无截断) | 11/11 modules SUCCESS; **101 tests / 0 failure / 0 error / 0 skipped**; `BUILD FAILURE` 出现 0 次 |
| 解析集合不变性 (11/11 模块) | `mvn -f <pom> -pl quantum-server -am package -DskipTests dependency:list`, 基线侧在 HEAD 临时 worktree 跑同一命令 | before=**232**, after=**232**, `diff` **为空** |
| Boot 自身版本未受影响 | 上述结果集检索 | `org.springframework.boot:spring-boot:jar:4.1.0`、`spring-boot-autoconfigure:jar:4.1.0` — 删掉的属性从未参与解析 |
| banner 行为不变 | clean 重建后读 `quantum-server/target/classes/banner.txt:20` | 仍是字面量 `${spring-boot.version}`, 与改动前逐字节相同 |

### 验证方法上的两次自纠 (过程如实记录)

1. **worker 首轮归因错误并自行推翻**: 它先把 `dependency:list` EXIT=1 归因于 worktree 缺 `target/`,
   复核后确认在 `target/` 已填充的主 checkout 同样复现。真实原因是 `dependency:list` 不跑构建阶段,
   reactor 内 `com.alpha.mcp:quantum-mcp:jar` 没有附着产物, 解析穿透到远程仓库而 miss。
   它同时主动指出: 靠 `-DappendOutput=true` 得到的 231 条实际只覆盖 **10/11** 模块 (失败点在最后的
   `quantum-server`), 「diff 为空」不等于全覆盖。
2. **主 agent A/B 首轮作废并重做**: 补全覆盖时改用 `package -DskipTests dependency:list` 单次调用。
   首次 A/B 因 Bash 工作目录跨调用保持 (前一条 `cd` 到基线 worktree 后未切回), 两侧可能跑在同一份 POM 上,
   结果不可信, **作废**。重做时改用 `-f <绝对路径>` 显式指定 reactor 根、不依赖 cwd, 并在跑之前先验证
   两侧 POM 确实不同 (基线 `grep -c` = 1, 主 checkout = 0)。上表数字来自重做后的那一轮。

## 验证缺口

无。11/11 模块覆盖, 缺口在 post-fix 阶段已闭合。

## 未纳入本次 polish

安全报告切片 (44 条 findings writeup/PoC、`hardening_final`、finalizer) 不在本 polish 范围, 随后继 sprint 单独走 review→polish。
