# Issue Report

## 现象

二次 review 发现第一轮安全修复后仍有 2 个业务后端缺口:

1. 用户导入仍未完全复用新增/编辑契约: Excel 入参没有部门与角色承载, service 只做唯一性和数据域校验, 未覆盖用户名/昵称/手机号/邮箱格式、性别/状态范围、部门/角色必填。
2. `assertDeptInDataScope` 对 `UserContext == null` fail-open, 未来 CLI/MCP/内部任务直接调 service 时可能绕过数据域。

架构后续项:

3. 数据权限来源仍混用用户表和角色表; 登录态以 `user.dataScope` 决定类型, CUSTOM 又从角色部门聚合。
4. 缺少 MVC 端点级契约测试验证异常响应和控制器映射。

## 期望

- 导入用户与新增/编辑用户共享同一组格式/必填/数据域/角色校验。
- service 数据域 guard 无认证上下文默认拒绝。
- 登录态数据权限由角色聚合生成最终权限, 用户表 `dataScope` 不再作为运行时主来源。
- 补充轻量 MVC/controller 契约测试, 让 Claude review 前有自动化证据。
