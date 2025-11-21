# 代码库结构

## 后端结构 (src/main/java/org/maram/bill/)

### 主要包和职责

#### 1. **config/** - 配置包
- 应用配置类: `SecurityConfig`, `WebMvcConfig`
- **config/properties/** - 类型安全的配置属性类
  - 应用设置、AI 提示词、外部 API 配置等

#### 2. **controller/** - 控制器层
- 处理 HTTP 请求
- 主要 AI 相关控制器:
  - `AIOChatController` - AI 聊天接口
  - `AiInsightController` - AI 洞察分析
  - `AiConfigController` - AI 配置管理
- 其他控制器:
  - `UserController` - 用户管理
  - `BillController` - 账单管理
  - `BillCategoryController` - 账单分类
  - `UserBudgetController` - 预算管理
  - `InvoiceFileController` - 发票文件上传
  - `ExchangeRateController` - 汇率转换

#### 3. **service/** - 服务层
- **service/** - 接口定义
- **service/impl/** - 接口实现
- **核心服务**: `AIOrchestrationService` - 集成 OpenAI 和 Redis 向量存储的核心 AI 逻辑

#### 4. **mapper/** - MyBatis 映射器
- 直接 SQL 控制
- 与数据库交互

#### 5. **entity/** - JPA 实体
- 对象关系映射
- 数据库表模型

#### 6. **common/** - 通用工具包
- 工具类
- 常量定义
- 基类
- **核心组件**:
  - `Result` 类 - 统一 API 响应格式
  - 各种枚举类

#### 7. **scheduler/** - 定时任务
- `ScheduledTasks` - 周期性任务(如获取汇率)

#### 8. **integration/** - 集成包
- 外部服务集成

### 入口类
- `BillApplication.java` - Spring Boot 应用主入口

## 前端结构 (ai-bill-front/)

### 主要目录

#### 1. **pages/** - 页面
- 所有小程序页面

#### 2. **utils/** - 工具函数
- `api.js` - API 请求封装
- `login-helper.js` - 用户认证

#### 3. **static/** - 静态资源
- 图标等静态文件

#### 4. **custom-tab-bar/** - 自定义导航栏

### 配置文件
- `app.js` - 应用入口,全局配置
- `app.json` - 小程序全局配置(页面、窗口样式、tab bar)
- `project.config.json` - 微信开发者工具项目配置
- `app.wxss` - 全局样式

## 资源文件 (src/main/resources/)
- `application.yaml` - 应用配置
  - 数据库配置
  - AI/OpenAI 配置
  - Redis 配置
  - JWT 配置
  - 微信小程序配置

## 其他重要文件
- `pom.xml` - Maven 项目配置和依赖
- `db.ddl` - 数据库 DDL 定义
- `api.md` - API 文档
- `CLAUDE.md` - 项目开发指南
- `AGENTS.md` - Agent 相关文档