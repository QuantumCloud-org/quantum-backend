# quantum-backend 依赖基线

日期：2026-07-30  
检查方式：`deps-check`（Maven Central 官方元数据）+ CycloneDX SBOM + OSV 批量查询

## 结论

- Maven reactor：11 个 POM；Spring Boot parent `4.1.0` 已是当前最新版。
- 基线 SBOM：210 个组件，SHA-256 `0f480f1dc180100cca3acaf5c6d37c404f1b153821d672d89e4b7f457b7eb95a`。
- OSV 基线：6 个实际解析组件命中漏洞，合计 13 个唯一公告；均有已发布修复版本。
- 需要更新 8 个版本控制点，并删除 root 中重复的 Boot/Netty/Jackson BOM import，改用 Spring Boot 4.1.0 识别的版本属性，避免只改非生效属性而实际解析版本不变。

## 升级矩阵

| 控制点 | POM 声明/当前实际 | 官方最新目标 | 说明 |
|---|---:|---:|---|
| Hutool | 5.8.46 | 5.8.47 | 直接版本属性 |
| Jackson 3 | 属性 3.2.0 / 实际 3.1.4 | 3.2.1 | 删除重复 BOM；改用 Boot 属性 `jackson-bom.version` |
| Jackson 2 | 实际 2.21.4 | 2.22.1 | 使用 Boot 属性 `jackson-2-bom.version`，避免 core/databind/jsr310 混版 |
| Netty | 属性 4.2.14.Final / 实际 4.2.15.Final | 4.2.16.Final | 删除重复 BOM；保留 Boot 属性 `netty.version`；4.2.16 修复本次 OSV 命中 |
| PostgreSQL JDBC | 42.7.12 | 42.7.13 | 直接版本属性 |
| AWS SDK S3 | 2.46.21 | 2.49.6 | 直接版本属性 |
| Tomcat | 11.0.22 | 11.0.24 | Spring Boot 支持的版本覆盖属性 |
| HikariCP | 实际 7.0.2 | 7.1.0 | 新增 Boot 属性 `hikaricp.version` |

## 已确认无需更新

Spring Boot 4.1.0、Caffeine 3.2.4、Disruptor 4.0.0、FastExcel 1.3.0、ip2region 3.3.7、MyBatis-Flex 1.11.8、Nimbus JOSE JWT 10.9.1、Redisson 4.6.1、Spring JCL 6.2.19、Springdoc 3.0.3，以及 POM 中显式声明的 Maven 插件。

## 漏洞基线

- `tools.jackson.core:jackson-databind:3.1.4`：命中 1 个公告；3.2.1 已修复。
- `com.fasterxml.jackson.core:jackson-databind:2.21.4`：命中 3 个公告；2.22.1 已修复。
- Netty `4.2.15.Final` 的 compression、DNS、HTTP、HTTP/2 组件：合计命中 9 个唯一公告；4.2.16.Final 已修复。

## 完成判据

升级后重新生成有效 POM 与 CycloneDX SBOM，重新执行 Maven 版本检查和 OSV 查询；实际解析版本必须等于上表目标，本次 13 个公告必须归零，且 reactor 编译、测试与运行时验证全部通过。

官方版本来源：[Maven Central](https://repo1.maven.org/maven2/)；漏洞来源：[OSV API](https://osv.dev/docs/)。
