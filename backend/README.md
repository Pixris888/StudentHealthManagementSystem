# 后端服务

Spring Boot 2.7 + MyBatis-Plus 多模块后端，默认端口 `8888`。

## 模块

| 模块 | 说明 |
| --- | --- |
| `health-common-mng` | 统一响应、异常处理、校验工具、Redis 工具、常量 |
| `health-auth-mng` | JWT 认证、Spring Security、用户/角色/菜单、字典、参数、日志、备份 |
| `health-core-mng` | 健康档案、打卡、体测、知识库、AI 助手、统计报表 |
| `health-message-mng` | 站内消息、已读、健康预警 |
| `health-web-mng` | 启动模块、全局配置、打包入口 |

## 环境要求

- JDK 17
- Maven 3.8+
- MySQL 5.7 / 8.0
- Redis 6 / 7

## 初始化数据库

在仓库根目录执行：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS health_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -uroot -p --default-character-set=utf8mb4 health_management < sql/schema.sql
mysql -uroot -p --default-character-set=utf8mb4 health_management < sql/demo-data.sql
```

> `schema.sql` 只用于全新数据库。

## 启动

```bash
mvn clean package -DskipTests
java -jar health-web-mng/target/health-web-mng-1.0.0.jar
```

或开发模式：

```bash
mvn -pl health-web-mng -am spring-boot:run
```

## 配置

默认读取 `health-web-mng/src/main/resources/application.yml`，支持环境变量覆盖：

```text
DB_HOST / DB_PORT / DB_NAME / DB_USERNAME / DB_PASSWORD
REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
JWT_SECRET
AI_PROVIDER / AI_BASE_URL / AI_MODEL / DASHSCOPE_API_KEY
STORAGE_TYPE / UPLOAD_DIR / BACKUP_DIR
```

完整示例见仓库根目录 `.env.example`。

生产配置模板见 `deploy/docker/application-prod.yml`。

## 测试

```bash
mvn test
```

## 接口

- 统一前缀：`/api`
- 登录：`POST /api/auth/login`
- 学生注册：`POST /api/auth/register`
- 当前用户：`GET /api/auth/info`
- Swagger/Knife4j：按实际引入的文档地址访问

## 说明

- JWT + Redis 双 Token，改密/禁用/改角色后旧 Token 自动失效
- 健康数据属于敏感信息，不要提交真实数据或生产 dump
