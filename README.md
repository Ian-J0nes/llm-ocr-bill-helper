# AI 智能账单管理系统

一个基于 **Spring Boot** 和 **微信小程序** 的智能账单管理系统，集成 AI 能力实现发票识别、智能分类、财务洞察等功能。

## 目录

- [功能特性](#功能特性)
- [技术架构](#技术架构)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [API 文档](#api-文档)
- [数据库设计](#数据库设计)
- [开发指南](#开发指南)
- [部署指南](#部署指南)

---

## 功能特性

### 核心功能

| 功能模块 | 描述 |
|---------|------|
| **AI 发票识别** | 上传发票图片，AI 自动识别并提取关键信息 |
| **智能分类** | 基于 AI 的账单自动分类，支持用户自定义分类 |
| **AI 聊天助手** | 通过自然语言与 AI 交互，查询账单、获取建议 |
| **财务洞察** | AI 生成月度/季度/年度财务分析报告 |
| **预算管理** | 设置预算目标，实时追踪使用情况，超支预警 |
| **多币种支持** | 支持多种货币及实时汇率转换 |
| **微信登录** | 一键微信授权登录，无需注册 |

### 技术亮点

- **Spring AI 集成**: 深度集成 OpenAI/GLM 等大语言模型
- **Redis 向量存储**: 基于语义搜索的智能检索
- **流式响应**: AI 对话支持 SSE 流式输出
- **多环境配置**: 支持 dev/uat/prod 环境隔离
- **统一异常处理**: 全局异常捕获，标准化错误响应

---

## 技术架构

### 后端技术栈

| 技术 | 版本 | 说明 |
|-----|------|-----|
| Java | 17 | LTS 版本 |
| Spring Boot | 3.2.5 | 核心框架 |
| Spring AI | 1.0.0 | AI 能力集成 |
| Spring Security | 6.x | 安全框架 |
| MyBatis Plus | 3.5.6 | ORM 框架 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.0+ | 缓存 & 向量存储 |
| JWT | 0.12.5 | 身份认证 |
| Lombok | - | 代码简化 |
| SpringDoc | 2.5.0 | API 文档 |

### 前端技术栈

| 技术 | 说明 |
|-----|------|
| 微信小程序 | 原生开发 |
| WXML/WXSS | 页面布局和样式 |
| JavaScript | 业务逻辑 |

### 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        微信小程序                                │
│  ┌─────────┬─────────┬─────────┬─────────┬─────────┐           │
│  │  账单   │  聊天   │  洞察   │  预算   │  我的   │           │
│  └────┬────┴────┬────┴────┬────┴────┬────┴────┬────┘           │
└───────┼─────────┼─────────┼─────────┼─────────┼─────────────────┘
        │         │         │         │         │
        └─────────┴─────────┴────┬────┴─────────┘
                                 │ HTTPS/JWT
┌────────────────────────────────┼────────────────────────────────┐
│                     Spring Boot 后端                             │
│  ┌─────────────────────────────┴──────────────────────────────┐ │
│  │                    Controller 层                            │ │
│  │  UserController │ BillController │ AIOChatController │ ...  │ │
│  └─────────────────────────────┬──────────────────────────────┘ │
│  ┌─────────────────────────────┴──────────────────────────────┐ │
│  │                     Service 层                              │ │
│  │  AIOrchestrationService │ BillService │ UserService │ ...   │ │
│  └─────────────────────────────┬──────────────────────────────┘ │
│  ┌─────────────────────────────┴──────────────────────────────┐ │
│  │                     Mapper 层 (MyBatis Plus)                │ │
│  └─────────────────────────────┬──────────────────────────────┘ │
└────────────────────────────────┼────────────────────────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
   ┌────┴────┐             ┌─────┴─────┐           ┌──────┴──────┐
   │  MySQL  │             │   Redis   │           │  OpenAI/GLM │
   │ 业务数据 │             │ 缓存/向量 │           │   AI 服务   │
   └─────────┘             └───────────┘           └─────────────┘
```

---

## 项目结构

```
ai-bill-main/
├── src/main/java/org/maram/bill/
│   ├── BillApplication.java          # 应用入口
│   ├── config/                        # 配置类
│   │   ├── ai/                        # AI 相关配置
│   │   │   ├── OpenAiConfig.java      # OpenAI 客户端配置
│   │   │   └── ChatConfig.java        # 聊天模型配置
│   │   ├── cache/                     # 缓存配置
│   │   │   ├── CacheConfig.java       # Spring Cache 配置
│   │   │   └── RedisConfig.java       # Redis 模板配置
│   │   ├── core/                      # 核心配置
│   │   │   └── JacksonConfig.java     # JSON 序列化配置
│   │   ├── persistence/               # 持久化配置
│   │   │   └── MyMetaObjectHandler.java  # MyBatis Plus 自动填充
│   │   ├── security/                  # 安全配置
│   │   │   ├── SecurityConfig.java    # Spring Security 配置
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── JwtTokenProvider.java
│   │   │   └── DatabaseUserDetailsService.java
│   │   └── properties/                # 配置属性类
│   │       ├── AppConfigProperties.java
│   │       ├── JwtProperties.java
│   │       ├── QiniuProperties.java
│   │       └── ...
│   ├── controller/                    # 控制器层
│   │   ├── UserController.java        # 用户管理
│   │   ├── BillController.java        # 账单管理
│   │   ├── AIOChatController.java     # AI 聊天
│   │   ├── AiInsightController.java   # AI 洞察
│   │   └── ...
│   ├── service/                       # 服务层接口
│   │   ├── impl/                      # 服务实现
│   │   │   ├── AIOrchestrationServiceImpl.java  # 核心 AI 编排
│   │   │   └── ...
│   │   └── ...
│   ├── mapper/                        # MyBatis Mapper
│   ├── entity/                        # 实体类
│   ├── common/                        # 公共模块
│   │   ├── base/                      # 基础类
│   │   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   │   ├── exception/                 # 自定义异常
│   │   ├── utils/                     # 工具类
│   │   │   ├── Result.java            # 统一响应
│   │   │   └── ResultCode.java        # 响应码枚举
│   │   └── enums/                     # 枚举类
│   ├── integration/                   # 外部服务集成
│   └── scheduler/                     # 定时任务
├── src/main/resources/
│   ├── application.yaml               # 主配置文件
│   ├── application-dev.yaml           # 开发环境配置
│   ├── application-uat.yaml           # 测试环境配置
│   └── application-prod.yaml          # 生产环境配置
├── ai-bill-front/                     # 微信小程序前端
│   ├── pages/                         # 页面
│   │   ├── index/                     # 首页
│   │   ├── chat/                      # AI 聊天
│   │   ├── ai-insight/                # AI 洞察
│   │   ├── bill-detail/               # 账单详情
│   │   ├── bill-summary/              # 账单汇总
│   │   ├── user-budget/               # 预算管理
│   │   ├── currency-exchange/         # 汇率转换
│   │   └── ...
│   ├── utils/                         # 工具函数
│   ├── static/                        # 静态资源
│   ├── app.js                         # 应用入口
│   ├── app.json                       # 应用配置
│   └── app.wxss                       # 全局样式
├── pom.xml                            # Maven 配置
├── db.ddl                             # 数据库 DDL
├── api.md                             # API 文档
├── .env.example                       # 环境变量示例
└── .gitignore                         # Git 忽略文件
```

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- 微信开发者工具 (前端开发)

### 1. 克隆项目

```bash
git clone <repository-url>
cd ai-bill-main
```

### 2. 配置环境变量

```bash
# 复制环境变量示例文件
cp .env.example .env

# 编辑 .env 文件，填入实际配置
vim .env
```

必填环境变量：

```properties
# 数据库
DB_URL=jdbc:mysql://localhost:3306/ai_bill?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8
DB_USERNAME=your_username
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# JWT (至少64字符)
JWT_SECRET=your-super-secret-jwt-key-at-least-64-characters-long

# OpenAI/AI 服务
OPENAI_BASE_URL=https://api.openai.com
OPENAI_API_KEY=sk-your-api-key

# 微信小程序
WECHAT_APPID=your_appid
WECHAT_SECRET=your_secret

# 七牛云存储
QINIU_ACCESS_KEY=your_access_key
QINIU_SECRET_KEY=your_secret_key
QINIU_BUCKET=your_bucket
QINIU_DOMAIN=your_domain
```

### 3. 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE ai_bill CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 执行 DDL
mysql -u root -p ai_bill < db.ddl
```

### 4. 启动后端服务

```bash
# 加载环境变量并启动
export $(cat .env | grep -v '^#' | xargs) && mvn spring-boot:run

# 或指定环境
mvn spring-boot:run -Dspring.profiles.active=dev
```

服务将在 `http://localhost:8080` 启动。

### 5. 启动前端

1. 打开 **微信开发者工具**
2. 导入 `ai-bill-front` 目录
3. 配置 AppID
4. 点击编译运行

---

## 配置说明

### 多环境配置

| 环境 | 配置文件 | 说明 |
|-----|---------|------|
| 开发 | `application-dev.yaml` | 详细日志，宽松超时 |
| 测试 | `application-uat.yaml` | 中等日志级别 |
| 生产 | `application-prod.yaml` | 精简日志，强制环境变量 |

### 主要配置项

```yaml
# application.yaml

spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}  # 激活的环境

  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  ai:
    openai:
      base-url: ${OPENAI_BASE_URL}
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: ${OPENAI_MODEL:gpt-4}
          temperature: 0.5

jwt:
  secret: ${JWT_SECRET}
  expiration:
    ms: ${JWT_EXPIRATION_MS:604800000}  # 7天

server:
  port: ${SERVER_PORT:8080}
```

### JWT 配置

| 参数 | 默认值 | 说明 |
|-----|-------|------|
| jwt.secret | - | 签名密钥 (至少64字符) |
| jwt.expiration.ms | 604800000 | Token 有效期 (毫秒) |
| jwt.header | Authorization | 请求头名称 |
| jwt.prefix | Bearer | Token 前缀 |

---

## API 文档

### 访问 Swagger UI

启动服务后访问：`http://localhost:8080/swagger-ui.html`

### 主要接口

#### 用户模块 `/user`

| 方法 | 路径 | 说明 | 认证 |
|-----|------|-----|------|
| POST | `/user/wxlogin` | 微信登录 | 否 |
| GET | `/user/me` | 获取当前用户 | 是 |
| PUT | `/user/{id}` | 更新用户信息 | 是 |

#### 账单模块 `/bill`

| 方法 | 路径 | 说明 | 认证 |
|-----|------|-----|------|
| GET | `/bill` | 账单列表 | 是 |
| POST | `/bill` | 创建账单 | 是 |
| PUT | `/bill/{id}` | 更新账单 | 是 |
| DELETE | `/bill/{id}` | 删除账单 | 是 |

#### AI 模块

| 方法 | 路径 | 说明 | 认证 |
|-----|------|-----|------|
| POST | `/aio/messages` | AI 聊天 (流式) | 是 |
| GET | `/ai-insight/monthly` | 月度洞察 (流式) | 是 |
| GET | `/ai-insight/quarterly` | 季度洞察 (流式) | 是 |
| GET | `/ai-insight/yearly` | 年度洞察 (流式) | 是 |

#### 文件上传 `/files`

| 方法 | 路径 | 说明 | 认证 |
|-----|------|-----|------|
| POST | `/files` | 上传发票文件 | 是 |
| GET | `/files/{fileId}` | 获取文件信息 | 是 |

### 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "success": true
}
```

### 错误码

| 范围 | 模块 | 示例 |
|-----|------|-----|
| 200 | 成功 | SUCCESS |
| 400-499 | 客户端错误 | BAD_REQUEST, UNAUTHORIZED |
| 1xxx | 用户模块 | USER_NOT_FOUND |
| 2xxx | 账单模块 | BILL_NOT_FOUND |
| 3xxx | 文件模块 | FILE_UPLOAD_FAILED |
| 4xxx | AI 模块 | AI_SERVICE_ERROR |
| 5xxx | 汇率模块 | EXCHANGE_RATE_NOT_FOUND |

详细 API 文档请参考 [api.md](./api.md)

---

## 数据库设计

### ER 图

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│     user     │────<│     bill     │>────│ bill_category│
└──────────────┘     └──────────────┘     └──────────────┘
       │                    │
       │                    │
       ▼                    ▼
┌──────────────┐     ┌──────────────┐
│ user_budget  │     │ invoice_file │
└──────────────┘     └──────────────┘

┌──────────────┐     ┌──────────────┐
│  currencies  │────<│exchange_rates│
└──────────────┘     └──────────────┘

┌──────────────┐
│ai_model_config│
└──────────────┘
```

### 主要表说明

| 表名 | 说明 |
|-----|------|
| `user` | 用户账户信息 |
| `bill` | 账单记录 |
| `bill_category` | 账单分类 |
| `user_budget` | 用户预算设置 |
| `invoice_file` | 上传的发票文件 |
| `currencies` | 支持的货币 |
| `exchange_rates` | 汇率信息 |
| `ai_model_config` | AI 模型配置 |

完整 DDL 请参考 [db.ddl](./db.ddl)

---

## 开发指南

### 常用命令

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 运行单个测试
mvn test -Dtest=TestClassName#testMethodName

# 打包
mvn clean package -DskipTests

# 启动应用
mvn spring-boot:run
```

### 代码规范

1. **命名规范**
   - 类名: PascalCase (`UserController`)
   - 方法名/变量名: camelCase (`getUserById`)
   - 常量: UPPER_SNAKE_CASE (`MAX_FILE_SIZE`)

2. **分层架构**
   - Controller: 处理 HTTP 请求，不包含业务逻辑
   - Service: 业务逻辑层，接口与实现分离
   - Mapper: 数据访问层，使用 MyBatis Plus

3. **API 响应**
   - 统一使用 `Result<T>` 包装
   - 错误码使用 `ResultCode` 枚举

4. **异常处理**
   - 业务异常使用 `BusinessException`
   - 全局异常由 `GlobalExceptionHandler` 处理

### 添加新功能

1. 创建 Entity 类 (`entity/`)
2. 创建 Mapper 接口 (`mapper/`)
3. 创建 Service 接口和实现 (`service/`, `service/impl/`)
4. 创建 Controller (`controller/`)
5. 添加必要的配置

---

## 部署指南

### Docker 部署

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# 构建镜像
mvn clean package -DskipTests
docker build -t ai-bill:latest .

# 运行容器
docker run -d \
  --name ai-bill \
  -p 8080:8080 \
  --env-file .env \
  ai-bill:latest
```

### 生产环境检查清单

- [ ] 所有敏感信息通过环境变量配置
- [ ] 启用 HTTPS
- [ ] 配置适当的 JWT 过期时间
- [ ] Redis 设置密码
- [ ] MySQL 使用专用用户（非 root）
- [ ] 配置日志轮转
- [ ] 设置监控告警

---

## 许可证

[MIT License](LICENSE)

---

## 联系方式

如有问题，请提交 Issue 或联系开发团队。
