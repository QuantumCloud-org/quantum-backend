# Runtime Verify — Convention Pack 模板实证 (2026-07-06)

## 验证方式

模板实例化真实模块（非纸面检查）：`docs/ai/convention-pack/templates/` 11 个模板
→ sed 替换占位符生成 `quantum-biz-notice`（SysNotice：title/content/status，无部门维度，
按约定裁掉 @DataScope 分支）→ 注册根 pom → 实跑编译。

## 证据

```
$ mvn -B -pl quantum-biz-notice -am -DskipTests compile
[INFO] Building quantum-biz-notice 1.0.0    [9/9]
[INFO] BUILD SUCCESS
```

10 个 Java 文件（Entity/Mapper/IService/ServiceImpl/Controller/Query/Create/Update/VO/Convert）
一次编译通过，含 MyBatis-Flex processor 生成 SysNoticeTableDef 并被静态导入解析。

## 实跑发现并已修复

- F: common 模块依赖坐标是 `com.alpha.<module>:quantum-common-<module>`（如 com.alpha.logging），
  不是统一 `com.alpha`——首次编译因此失败。已回写 conventions.md「新增模块 pom」一节。

## 现场清理

试算模块验证后删除、根 pom 还原，不进 git 历史；本文件为证据存档。
