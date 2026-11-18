# 2026-11 依赖升级报告

`mvn clean test` 退出码 **0**。11 个 Reactor 模块全部成功，113 个测试，失败、错误、跳过均为 0。10 个指定组件及两个构建插件完成升级，回退 0 项。

## 版本

旧版以接手时 worktree 的 POM 和 Spring Boot 管理版本为准。2026-11-18 通过 Maven Central 的 `maven-metadata.xml` 查询 `release`，并核对 Maven effective POM 中的实际管理版本。全部 11 个 POM 已检查，子模块的内部依赖通过 `${revision}` 引用，构建插件版本由根 POM 和 Spring Boot 父 POM 管理。

| 组件 | 旧版 → 最终版 | Central 元数据及选择 |
| --- | --- | --- |
| Spring Boot 父 POM 及 Maven 插件 | 4.1.2 → 4.1.2 | `release` 为 4.2.0-RC2，稳定版沿用 4.1.2。[父 POM](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml) · [插件](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-maven-plugin/maven-metadata.xml) |
| Tomcat | 11.0.27 → 11.0.29 | [Central](https://repo1.maven.org/maven2/org/apache/tomcat/embed/tomcat-embed-core/maven-metadata.xml) |
| Netty | 4.2.19.Final → 4.2.21.Final | [Central](https://repo1.maven.org/maven2/io/netty/netty-bom/maven-metadata.xml) |
| Jackson 3 BOM | 3.2.4 → 3.3.0 | [Central](https://repo1.maven.org/maven2/tools/jackson/jackson-bom/maven-metadata.xml) |
| Jackson 2 BOM | 2.22.4 → 2.23.0 | [Central](https://repo1.maven.org/maven2/com/fasterxml/jackson/jackson-bom/maven-metadata.xml) |
| Nimbus JOSE + JWT | 10.9.1 → 10.10 | [Central](https://repo1.maven.org/maven2/com/nimbusds/nimbus-jose-jwt/maven-metadata.xml) |
| Redisson | 4.6.1 → 4.8.0 | [Central](https://repo1.maven.org/maven2/org/redisson/redisson-spring-boot-starter/maven-metadata.xml) |
| Caffeine | 3.2.4 → 3.3.1 | [Central](https://repo1.maven.org/maven2/com/github/ben-manes/caffeine/caffeine/maven-metadata.xml) |
| springdoc-openapi | 3.0.3 → 3.2.0 | [Central](https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/maven-metadata.xml) |
| ip2region | 3.3.7 → 3.3.8 | [Central](https://repo1.maven.org/maven2/org/lionsoul/ip2region/maven-metadata.xml) |
| AWS SDK S3 | 2.49.6 → 2.59.3 | [Central](https://repo1.maven.org/maven2/software/amazon/awssdk/s3/maven-metadata.xml) |
| Maven Compiler Plugin | 3.15.0 → 3.16.0 | `release` 为 4.0.0-beta-5，采用版本列表中最新稳定版 3.16.0。[Central](https://repo1.maven.org/maven2/org/apache/maven/plugins/maven-compiler-plugin/maven-metadata.xml) |
| GraalVM Native Maven Plugin | 1.1.14 → 1.1.19 | [Central](https://repo1.maven.org/maven2/org/graalvm/buildtools/native-maven-plugin/maven-metadata.xml) |
| PostgreSQL JDBC | 42.7.14 → 42.7.14 | 已是稳定版。[Central](https://repo1.maven.org/maven2/org/postgresql/postgresql/maven-metadata.xml) |
| MyBatis-Flex | 1.11.8 → 1.11.8 | 已是稳定版。[Central](https://repo1.maven.org/maven2/com/mybatis-flex/mybatis-flex-dependencies/maven-metadata.xml) |
| Disruptor | 4.0.0 → 4.0.0 | 已是稳定版。[Central](https://repo1.maven.org/maven2/com/lmax/disruptor/maven-metadata.xml) |
| HikariCP | 7.1.0 → 7.1.0 | 已是稳定版。[Central](https://repo1.maven.org/maven2/com/zaxxer/HikariCP/maven-metadata.xml) |
| Hutool | 5.8.48 → 5.8.48 | 已是稳定版。[Central](https://repo1.maven.org/maven2/cn/hutool/hutool-all/maven-metadata.xml) |
| FastExcel | 1.3.0 → 1.3.0 | 已是稳定版。[Central](https://repo1.maven.org/maven2/cn/idev/excel/fastexcel/maven-metadata.xml) |
| Lombok | 1.18.48 → 1.18.48 | Boot 管理，已是稳定版。[Central](https://repo1.maven.org/maven2/org/projectlombok/lombok/maven-metadata.xml) |

接手前已有 Spring Boot 4.1.0 → 4.1.2、PostgreSQL 42.7.13 → 42.7.14、Hutool 5.8.47 → 5.8.48 和 Spring 7 日志桥接调整。Tomcat、Netty 和两个 Jackson BOM 已有的版本增量是本表的起点。Spring Boot 4.1.2 管理 Spring Framework 7.0.10。

## 分组编译

每组按顺序运行 `mvn -q clean compile -DskipTests`，上一组通过后再修改下一组。使用 Java 25、Maven 3.9.14 和 Maven 默认设置。

| 组 | 组件 | 退出码 |
| --- | --- | --- |
| 基线 | 接手时的 POM | 0 |
| 传输 | Tomcat、Netty | 0 |
| JSON | Jackson 3 BOM、Jackson 2 BOM | 0 |
| 安全与缓存 | Nimbus、Redisson、Caffeine | 0 |
| 集成 | springdoc、ip2region、S3 | 0 |
| 构建 | Compiler Plugin、Native Maven Plugin | 0 |

## API 适配与兼容性

| 文件:行 | 适配与影响 |
| --- | --- |
| [JsonUtil.java:239](../../../quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/util/JsonUtil.java#L239) | Jackson 3 的 `asText()` 改为 `asString()`。3.3.0 字节码显示前者直接委托后者，保留值转换语义。 |
| [RedisConfig.java:48](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/RedisConfig.java#L48) | `setRetryInterval(int)` 改为 `setRetryDelay(ConstantDelay)`。4.8.0 旧方法的字节码也用毫秒构造 `ConstantDelay`。保留 Redis 的 Jackson 2 codec 和缓存编码。 |
| [StartupDependencyVerifier.java:4](../../../quantum-server/src/main/java/com/alpha/server/startup/StartupDependencyVerifier.java#L4) | Spring 7 弃用的 `org.springframework.lang.Nullable` 改用 JSpecify `Nullable`，保留构造器签名。 |
| [TokenService.java:119](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L119) · [TokenService.java:153](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L153) | 认证拒绝缺少有效 `exp` 或签名算法偏离服务端 HS512 的 JWT。原签发端已生成 HS512 和 `exp`，访问令牌只解析一次。 |
| [OAuthBearerAuthenticationFilter.java:73](../../../quantum-mcp/src/main/java/com/alpha/mcp/security/OAuthBearerAuthenticationFilter.java#L73) | MCP Bearer 过滤器按控制器实际路由和 servlet context 匹配。启用 MCP 时，`/mcp` 始终要求 MCP OAuth Bearer。 |
| [LocalFileStorageServiceImpl.java:92](../../../quantum-common/quantum-common-file/src/main/java/com/alpha/file/service/impl/LocalFileStorageServiceImpl.java#L92) · [RustfsFileStorageServiceImpl.java:125](../../../quantum-common/quantum-common-file/src/main/java/com/alpha/file/service/impl/RustfsFileStorageServiceImpl.java#L125) | 文件上传失败保留异常类型，客户端改收固定业务文案。服务端保留故障诊断。 |
| [CacheInfo.java:61](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/monitor/CacheInfo.java#L61) | 缓存统计查询未知名称时沿用未找到响应，查询本身不再创建缓存。 |
| [LogAspect.java:195](../../../quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/aspect/LogAspect.java#L195) · [AuditManagerMessage.java:32](../../../quantum-common/quantum-common-orm/src/main/java/com/alpha/orm/config/AuditManagerMessage.java#L32) | 操作日志的敏感字段与序列化失败走脱敏路径。SQL 审计和项目 SQL 调试日志输出 SQL 模板。 |

依赖升级未触发编译破坏，springdoc 和 S3 源码在目标版本通过编译。以 `mvn clean compile -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.showWarnings=true` 复核，三处项目源码弃用警告已消除。Lombok 1.18.48 仍触发 `Unsafe.objectFieldOffset` 警告，测试中的 Mockito/Byte Buddy 仍有动态 agent 加载提示。

## 最终测试

`mvn clean test`，退出码 **0**，113 个测试全部通过。以下为最终 Maven 输出。

```text
[INFO] Reactor Summary for quantum-backend 1.0.0:
[INFO]
[INFO] quantum-backend .................................... SUCCESS [  0.026 s]
[INFO] quantum-common ..................................... SUCCESS [  0.001 s]
[INFO] quantum-common-framework ........................... SUCCESS [  0.935 s]
[INFO] quantum-common-cache ............................... SUCCESS [  1.494 s]
[INFO] quantum-common-security ............................ SUCCESS [  1.704 s]
[INFO] quantum-common-orm ................................. SUCCESS [  1.519 s]
[INFO] quantum-common-logging ............................. SUCCESS [  1.908 s]
[INFO] quantum-common-file ................................ SUCCESS [  1.927 s]
[INFO] quantum-biz-system ................................. SUCCESS [  3.376 s]
[INFO] quantum-mcp ........................................ SUCCESS [  1.968 s]
[INFO] quantum-server ..................................... SUCCESS [  0.200 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  15.304 s
[INFO] Finished at: 2026-11-18T10:40:27+08:00
[INFO] ------------------------------------------------------------------------
```

验证范围为 Maven 编译、effective POM 和测试。应用连接 PostgreSQL、Redis、S3 的运行验证及 GraalVM native-image 构建均待验证。Athena 在改动后计算源码树哈希时尝试写入主仓共享 Git 对象库，沙箱拒绝 `git add`（`unable to create temporary file: Operation not permitted`）。报告采用直接执行 Maven 的退出码，Athena 的源码树绑定证据待补。
