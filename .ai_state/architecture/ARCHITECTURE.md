# Quantum Backend Architecture

## 当前形态

Quantum Backend 是 Spring Boot + Maven 多模块的模块化单体。

| 模块 | 职责 |
|---|---|
| `quantum-common-framework` | 通用实体、上下文、异常、工具 |
| `quantum-common-security` | Spring Security、JWT、refresh cookie、CORS、限流、防重复提交 |
| `quantum-common-orm` | 分页、数据权限上下文、MyBatis-Flex 支撑 |
| `quantum-common-cache` | Redis / local cache 抽象 |
| `quantum-common-logging` | 操作日志、登录日志、日志分页 |
| `quantum-common-file` | 文件校验、本地存储、RustFS/S3 兼容存储、Excel |
| `quantum-biz-system` | 用户、角色、菜单、部门、字典、文件记录等系统域 |
| `quantum-server` | 启动入口与 profile 配置 |

## 子系统索引

- [auth-security.md](auth-security.md)
