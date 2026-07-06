# Issue Report

## 现象

后端 review 发现 7 个第一轮必须修复的安全与一致性问题:

1. 生产 CORS 继承 `*` origin pattern 且允许 credentials。
2. refresh token cookie 缺少生产 HTTPS `Secure` 开关。
3. 用户详情与用户角色读取按 ID 裸查, 绕过数据域。
4. 用户导入新增/更新绕过用户新增编辑校验链。
5. 角色 `dataScope` 新增/编辑通道缺范围校验。
6. 分页 DTO 的 `@Min/@Max` 未被 GET 查询入参触发。
7. RustFS object key 接受未校验的 path/fileName。

## 期望

- 凭证型 CORS 仅允许显式可信 Origin。
- HTTPS profile 下 refresh cookie 带 `Secure`。
- read/write 用户操作均复用数据域和唯一性校验。
- 分页、角色数据域、文件 key 均在入口处拒绝非法输入。
- 每个修复点有自动化回归测试覆盖。
