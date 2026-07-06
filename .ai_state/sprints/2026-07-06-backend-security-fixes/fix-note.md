# Fix Note

## 改动

- 收紧 credentialed CORS, 生产使用显式 Origin 配置。
- refresh cookie 支持配置驱动 `Secure`。
- 用户详情和用户角色读取补数据域校验。
- 用户导入补唯一性、数据域、冲突和缓存刷新事件。
- 角色 dataScope 补 DTO 与 service 双层范围校验。
- 7 个分页入口补 `@Validated`。
- RustFS object key 补 path/fileName 校验并与 Local 复用工具方法。

## 验证

```bash
mvn -pl quantum-common/quantum-common-security,quantum-common/quantum-common-file,quantum-biz-system -am test
```

结果: Reactor 9/9 SUCCESS。

```bash
mvn test
```

结果: Reactor 10/10 SUCCESS; Failures 0; Errors 0。
