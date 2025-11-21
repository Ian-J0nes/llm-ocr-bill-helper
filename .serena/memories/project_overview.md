# 项目概述

## 项目名称
AI账单系统 (AI Bill System)

## 项目目的
这是一个智能账单管理系统,集成了 AI 能力用于:
- 账单识别和分类
- 智能财务分析和洞察
- 预算管理和预警
- 通过自然语言与 AI 聊天进行账单管理

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.5
- **Java 版本**: Java 17
- **构建工具**: Maven (使用 Maven Wrapper `./mvnw`)
- **AI 集成**: Spring AI 1.0.0 (集成 OpenAI)
- **数据库**: MySQL + Redis
- **数据访问**: 
  - JPA (Spring Data JPA) - 用于对象关系映射
  - MyBatis Plus 3.5.6 - 用于更直接的 SQL 控制
- **安全**: Spring Security + JWT 认证
- **API 文档**: SpringDoc OpenAPI (Swagger)
- **向量存储**: Redis Vector Store (用于 AI 功能)
- **其他依赖**:
  - Lombok - 减少样板代码
  - Jackson - JSON 处理
  - JWT (jjwt) - Token 处理
  - Qiniu SDK - 七牛云存储

### 前端
- **平台**: 微信小程序
- **开发工具**: 微信开发者工具

## 架构特点
1. **分层架构**: Controller -> Service -> Mapper/Entity
2. **双重数据访问策略**: JPA 和 MyBatis 并用
3. **AI 驱动**: 核心 AI 逻辑集中在 `AIOrchestrationService`
4. **统一响应**: 使用 `Result<T>` 包装所有 API 响应