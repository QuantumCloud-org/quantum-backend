# Design Review Round 1

Verdict: **NEEDS_CHANGES**

P1：root 已通过 `spring-boot-starter-parent` 继承 Boot dependency management；继续显式导入 Boot、Netty、Jackson 2/3 BOM 会重复管理并依赖导入顺序。应删除冗余 BOM，使用 Spring Boot 4.1.0 官方版本属性覆盖：`netty.version`、`jackson-bom.version`、`jackson-2-bom.version`、`hikaricp.version`、`tomcat.version`。

本地缓存的官方 Spring Boot 4.1.0 BOM 已核实上述属性名；后续仍以 effective POM 与 SBOM 验证实际解析版本。
