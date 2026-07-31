# quantum-backend 依赖升级报告

日期：2026-07-31  
检查：`deps-check` + Maven Versions Plugin + effective POM + CycloneDX SBOM + OSV Query Batch

## 结果

- 升级范围严格限定在 root `pom.xml`；未产生兼容性源码改动。
- 8 个生效版本控制点均达到 Maven Central 当前最新稳定版：Hutool 5.8.47、Jackson 3.2.1、Jackson 2.22.1、Netty 4.2.16.Final、PostgreSQL 42.7.13、AWS SDK S3 2.49.6、Tomcat 11.0.24、HikariCP 7.1.0。
- 删除重复的 Spring Boot、Netty、Jackson BOM import，保留 `spring-boot-starter-parent` 的单一依赖治理入口和 MyBatis-Flex BOM；Jackson/Netty/Hikari/Tomcat 使用 Spring Boot 4.1.0 官方覆盖属性。
- 全 reactor Maven Versions 检查：parent 已最新；全部 version properties 已最新；全部显式 plugin 已最新；direct dependencies 无可用更新。
- 统计：`unqueried=0`、`outdated=0`、`prerelease_selected=0`。Maven Enforcer “未声明最低 Maven 版本”提示不是依赖版本落后，且本轮没有第二消费者需求，因此未新增构建机制。

## 实际解析证据

- 升级后 CycloneDX 1.6 SBOM：211 个组件；SHA-256 `40c539a533392eaaa1858e8d05dc53df6a7ecfabc3b64f1b615df3b4a1f4da70`。
- 完整 dependency tree：`dependency-tree.txt`，3815 行；SHA-256 `e109c1b02d4e5700ff6e39fff2e725b61f3abba696593ff50bf9ad737473388f`。
- effective POM / SBOM 均解析出目标版本；不是仅修改未生效的文本属性。
- 升级后对 209 个第三方 Package URL 调用 OSV `POST /v1/querybatch`：0 个组件命中、0 个唯一公告；基线 13 个 Jackson/Netty 公告全部归零。

## 验证

- `mvn clean test`：11/11 reactor modules `BUILD SUCCESS`，101 tests，0 failure / 0 error / 0 skipped，10.359 秒。
- `mvn -pl quantum-server -am package -DskipTests`：11/11 modules `BUILD SUCCESS`，生成可执行 `quantum-server.jar`。
- `mvn -DskipTests dependency:tree -Dverbose -DappendOutput=true -DoutputFile=.../dependency-tree.txt`：11/11 modules `BUILD SUCCESS`。
- `mvn -DskipTests org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom`：`BUILD SUCCESS`，211 components。

## 主 agent 独立复核（2026-07-31，ship 前）

上文由升级 worker 产出；ship 前由主 agent 重跑关键断言，取第一手证据而非转述。

| 断言 | 复核命令 | 实测结果 |
|---|---|---|
| 版本真生效（非只改属性文本） | `mvn -o -pl quantum-server -am dependency:list` | 8/8 命中：Hutool 5.8.47、`tools.jackson` 3.2.1、`com.fasterxml.jackson` 2.22.1、HikariCP 7.1.0、Netty 4.2.16.Final、tomcat-embed-core 11.0.24、PostgreSQL 42.7.13、AWS S3 2.49.6 |
| 全量回归 | `mvn clean test`（全量日志，无截断） | 11/11 modules `BUILD SUCCESS`；101 tests / 0 failure / 0 error / 0 skipped |
| 删 BOM import 安全 | 读 root `pom.xml` | `<parent>` = `spring-boot-starter-parent:4.1.0`，dependencyManagement 由父 POM 继承，删除同级 `spring-boot-dependencies` import 不丢版本治理 |
| worktree 有无独立产出 | `git log/branch/stash` + `diff` 两份 `pom.xml` | 零 commit、零分支、无 stash，两份 `pom.xml` 逐字节相同 → 无可合并内容 |

复核过程中的一次自纠：首轮后台命令写成 `mvn clean test 2>&1 | tail -60`，日志被截断导致只统计到最后一个模块的 17 tests，与本报告的 101 不符。重跑并落全量日志后确认 101 正确，是复核方法出错而非报告有误。

## 本切片未覆盖范围

依赖升级切片单独 ship，以下 design 条目**仍未闭环**，随安全报告切片进入后继 sprint：AC1（canonical 扫描产物入仓）、Done Contract #1（`security/report.md`）、Done Contract #6（review 三件套 PASS + cleanup）。

权威来源：[Maven Central](https://repo1.maven.org/maven2/)；漏洞查询：[OSV API](https://api.osv.dev/v1/querybatch)。
