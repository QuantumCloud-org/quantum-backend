# Issue Analysis

## 根因

- 配置层缺少 prod 覆盖和 cookie secure profile。
- 用户服务中读侧与导入路径未复用写侧 guard。
- DTO/Controller 有校验注解但未覆盖全部入口。
- Local 与 RustFS 存储 key 策略实现不一致。
- 现有测试集中在缓存和少量工具类, 无法捕获安全链路回归。

## 修复策略

1. 为 CORS 与 refresh cookie 增加配置驱动策略和单元测试。
2. 在用户服务抽取 read/write guard, 导入路径复用校验与事件发布。
3. 角色 dataScope 做 DTO + service 双层校验。
4. 所有分页查询入参加 `@Validated` / `@Valid`。
5. 抽通用文件路径/key 校验方法, Local 与 RustFS 共用。
6. 分模块补单元测试, 最后跑全量 `mvn test`。
