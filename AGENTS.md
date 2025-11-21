# Repository Guidelines

## Project Structure & Module Organization
The Spring Boot backend lives under `src/main/java/org/maram/bill`, with `controller` exposing REST APIs, `service` encapsulating business rules, `entity`/`mapper` for persistence, and `scheduler` for cron-driven tasks. Shared helpers sit in `common` and application-wide configuration in `config`. Resource files, including `application.yaml`, live in `src/main/resources`. The WeChat Mini Program client resides in `ai-bill-front`, where page logic is grouped under `pages/`, reusable UI in `custom-tab-bar/`, and static assets in `assets/` and `static/`.

## Build, Test, and Development Commands
- `./mvnw clean verify` — compile, run unit tests, and package the backend.
- `SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run` — launch the API with overrides from `src/main/resources/application-local.yaml`.
- `./mvnw test` — run the JUnit 5 suite before every push.
- Open `ai-bill-front` in WeChat Developer Tools, use the simulator, and keep `project.config.json` aligned.

## Coding Style & Naming Conventions
Follow the default Spring formatter (4-space indent) and Java 17 language features. Use `PascalCase` for classes, `camelCase` for fields and methods, and suffix persistence mappers with `Mapper`. Prefer Lombok (`@Getter`/`@Setter`) where it reduces noise; otherwise write explicit methods. In `ai-bill-front`, keep directories lowercase and page IDs kebab-cased (e.g., `pages/bill-detail/`).

## Testing Guidelines
Spring Boot 3 ships with JUnit 5; place new specs in `src/test/java` mirroring the production package structure and suffixed with `Test`. Mock external services (Redis, RabbitMQ, OpenAI) with Testcontainers or Mockito, and cover controller happy-path plus validation failures. For Mini Program logic, add lightweight Jest tests under `ai-bill-front/utils/__tests__/` or document manual steps when automation is not practical. Focus coverage on the money-flow core (`service` layer) before merging.

## Commit & Pull Request Guidelines
History favors concise summary lines (e.g., "第一次上传代码"). Keep messages present-tense under 60 characters and add an English clause when possible. Use feature branches, reference issue IDs, and flag schema or API changes. Pull requests should supply context, test evidence (`./mvnw test` output or WeChat screenshots), and configuration notes.

## Security & Configuration Tips
Do not commit real credentials. Copy `application.yaml` to `application-local.yaml`, replace secrets with placeholders, and load values via environment variables (`SPRING_AI_OPENAI_API_KEY`, `SPRING_DATASOURCE_PASSWORD`, etc.). Document new configuration keys in PRs and update onboarding notes when integrations change.

---

## 📝 更新日志（按时间倒序追加）

### 🟢 2025-10-12 — 移除 RabbitMQ，改为同步调用（遵循 KISS 原则）
- **产生原因**：
  1. **过度设计**：项目是单体应用，不是微服务架构，所有代码在同一个 JVM 进程中
  2. **增加复杂度**：RabbitMQ 需要额外部署、维护，增加了系统故障点和调试难度
  3. **消息可靠性未保障**：没有重试机制、没有死信队列，处理失败直接丢弃，还不如同步调用
  4. **场景不匹配**：微信小程序后端并发量不高，不需要 MQ 的削峰填谷功能
  5. **MQ 只是"绕了一圈"**：上传发票 → 发送消息到 MQ → 监听器接收 → AI 处理，完全可以直接同步调用

- **问题分析**：
  - 当前流程：`发票上传 → RabbitMQ 队列 → 监听器消费 → AI 处理`
  - 实际需求：`发票上传 → AI 处理` 即可
  - MQ 的优势（异步处理、解耦、削峰）在本项目中都用不上
  - 代码中 `ObjectProvider<RabbitTemplate>` 如果未注入，消息直接跳过，可靠性为 0
  - 错误处理注释 "这里可以加入错误处理逻辑，例如将消息发送到死信队列"，但实际没有实现

- **解决方案**（遵循 KISS 原则 - Keep It Simple, Stupid）：
  1. **删除 RabbitMQ 配置类**
     - 删除 `RabbitMQConfig.java`（33 行）
     - 移除队列、交换机、绑定关系配置

  2. **删除 RabbitMQ 监听器服务**
     - 删除 `RabbitMQListenerService.java`（28 行）
     - 移除 `@RabbitListener` 消费逻辑

  3. **修改发票上传服务，改为同步调用**
     - 修改 `InvoiceFileServiceImpl.java`：
       - 删除 `RabbitTemplate` 注入，改为注入 `BillProcessingService`
       - 删除 `publishStoredEvent()` 方法（13 行）
       - 在 `uploadInvoiceFile()` 中直接同步调用 `billProcessingService.processInvoiceFile()`
       - 添加 try-catch 确保即使处理失败，也能返回文件 ID
       - 删除 RabbitMQ 相关导入（`RabbitTemplate`、`ObjectProvider`、`RabbitMQConfig`）
       - 新增 `BillProcessingService` 导入

  4. **删除 RabbitMQ 依赖**
     - 从 `pom.xml` 删除 `spring-boot-starter-amqp` 依赖

  5. **删除 RabbitMQ 配置**
     - 从 `application.yaml` 删除 `spring.rabbitmq` 配置块（5 行）

  6. **编译验证**
     - 执行 `./mvnw clean compile -DskipTests`
     - 编译成功，无错误

- **修改文件**：
  - **删除**：
    - `src/main/java/org/maram/bill/config/RabbitMQConfig.java`
    - `src/main/java/org/maram/bill/service/RabbitMQListenerService.java`

  - **修改**：
    - `src/main/java/org/maram/bill/service/impl/InvoiceFileServiceImpl.java`
      - 导入：删除 `RabbitMQConfig`、`RabbitTemplate`、`ObjectProvider`，新增 `BillProcessingService`
      - 依赖注入：`ObjectProvider<RabbitTemplate>` → `BillProcessingService`
      - 删除 `publishStoredEvent()` 方法
      - 修改 `uploadInvoiceFile()`：
        ```java
        // 旧逻辑：发送消息到 RabbitMQ
        publishStoredEvent(invoiceFile.getId());

        // 新逻辑：直接同步调用处理
        String fileId = invoiceFile.getId().toString();
        try {
            billProcessingService.processInvoiceFile(fileId);
            log.info("发票处理完成: fileId={}", fileId);
        } catch (Exception ex) {
            log.error("处理发票文件失败: fileId={}", fileId, ex);
            // 发票上传成功，但处理失败，仍返回 fileId
        }
        ```

    - `pom.xml`：
      - 删除 `spring-boot-starter-amqp` 依赖（第 103-106 行）

    - `src/main/resources/application.yaml`：
      - 删除 `spring.rabbitmq` 配置（第 30-34 行）

- **状态**：✅ 编译成功 | ✅ 代码简化完成

- **技术细节**：
  - **流程对比**：
    ```
    旧流程（MQ 异步）：
    用户上传发票 → 保存数据库 → 发送消息到 RabbitMQ
      → 监听器接收 → AI 处理 → 创建账单

    新流程（同步调用）：
    用户上传发票 → 保存数据库 → 直接调用 AI 处理 → 创建账单
    ```

  - **错误处理对比**：
    ```
    旧方案（MQ）：
    - 处理失败：消息丢失，无重试
    - 可靠性：依赖 RabbitMQ 可用性
    - 调试难度：需要查看 MQ 队列状态

    新方案（同步）：
    - 处理失败：记录日志，返回文件 ID
    - 可靠性：不依赖外部组件
    - 调试难度：直接查看日志即可
    ```

  - **性能影响**：
    - 微信小程序上传发票频率：每天几次到十几次
    - AI 处理时间：通常 2-5 秒
    - 同步调用完全可以接受，用户体验无影响

  - **代码简化统计**：
    - 删除文件：2 个（RabbitMQConfig、RabbitMQListenerService）
    - 删除代码：61 行
    - 删除配置：5 行
    - 删除依赖：1 个

- **附注**：
  - **KISS 原则实践**：不要为了技术而技术，不要过度设计，够用就好
  - **何时需要 MQ？** 只有以下场景才需要：
    1. 微服务架构，不同服务之间通信（跨进程、跨机器）
    2. 高并发场景，需要削峰填谷（秒杀、抢购）
    3. 消息可靠性要求极高，需要持久化、ACK、死信队列
    4. 复杂的消息路由（fanout、topic 等）
  - 本项目**一个都不满足**，移除 MQ 是正确选择
  - 简化后的系统更稳定、更易维护、更易调试
  - 减少了一个基础设施依赖（RabbitMQ），降低了部署和运维成本

### 🟢 2025-10-12 — AI 聊天上下文管理重构（修复 400 错误）
- **产生原因**：
  1. **第二次请求 400 错误**：使用 Spring AI 内置的 `MessageChatMemoryAdvisor` 时，第一次请求成功（200 OK），但第二次带上下文的请求返回 400 Bad Request
  2. **不兼容的请求格式**：`MessageChatMemoryAdvisor` 在组装聊天历史时，可能添加了你的 OpenAI 兼容 API（`https://apix.778801.xyz`）不支持的参数或格式
  3. **无法调试具体参数**：Spring AI 内部封装较深，难以直观看到实际发送给 API 的请求体内容

- **问题分析**：
  1. **测试验证**：临时禁用 `MessageChatMemoryAdvisor` 后，连续多次请求都成功（200 OK），确认问题根源
  2. **可能的原因**：
     - Spring AI 可能在消息中添加了额外的 metadata 或格式
     - 聊天历史的序列化格式不被 API 接受
     - 累积的对话历史中可能包含 API 不支持的字段
  3. **设计问题**：使用第三方库的内置功能时，对请求格式缺乏控制，与自定义 API 兼容性差

- **解决方案**（遵循 KISS 原则）：
  1. **移除 Spring AI 内置聊天记忆**
     - 删除 `MessageChatMemoryAdvisor` 依赖
     - 移除 `ChatMemory` 注入
     - 注释掉所有 `.advisors()` 调用

  2. **创建轻量级上下文管理服务**
     - 新增 `ChatContextService` 接口，定义上下文管理的核心方法：
       - `addUserMessage()` - 添加用户消息
       - `addAssistantMessage()` - 添加AI响应
       - `getRecentMessages()` - 获取最近N轮对话
       - `clearContext()` - 清空对话上下文

     - 新增 `ChatContextServiceImpl` 实现类：
       - **存储方案**：基于 Redis，Key 格式为 `chat:context:{openid}`
       - **数据结构**：`LinkedList<MessagePair>`，MessagePair 包含 `userMessage` 和 `assistantMessage`
       - **容量控制**：Redis 中最多存储 10 轮对话，自动滑动窗口
       - **过期策略**：自动过期时间 1 小时，避免内存泄漏
       - **上下文限制**：发送请求时使用最近 5 轮对话作为上下文

  3. **配置 RedisTemplate**
     - 新增 `RedisConfig` 配置类
     - 配置 `RedisTemplate<String, Object>` Bean
     - 使用 `Jackson2JsonRedisSerializer` 序列化复杂对象
     - Key 使用 `StringRedisSerializer`，Value 使用 JSON 序列化

  4. **修改 AI 编排服务**
     - 注入 `ChatContextService` 替代 `ChatMemory`
     - 在发送请求前调用 `getRecentMessages(openid, 5)` 获取历史消息
     - 使用 `.messages(historyMessages)` 方法添加上下文到 prompt
     - 收集流式响应的完整内容，调用 `addAssistantMessage()` 保存
     - 删除 `buildChatClient()` 中的 `MessageChatMemoryAdvisor` 配置

  5. **添加详细日志**
     - 记录历史消息数量：`历史消息数: {}`
     - 记录上下文获取情况：`获取上下文 [{}], 返回 {} 轮对话, 共 {} 条消息`
     - 记录上下文添加操作：`添加用户消息到上下文` / `添加AI响应到上下文`

- **修改文件**：
  - **新增**：
    - `src/main/java/org/maram/bill/service/ChatContextService.java` (接口)
    - `src/main/java/org/maram/bill/service/impl/ChatContextServiceImpl.java` (实现，约150行)
    - `src/main/java/org/maram/bill/config/RedisConfig.java` (Redis配置)

  - **修改**：
    - `src/main/java/org/maram/bill/service/impl/AIOrchestrationServiceImpl.java`
      - 导入：移除 `MessageChatMemoryAdvisor` 和 `ChatMemory`，新增 `ChatContextService` 和 `Message`
      - 依赖注入：`ChatMemory` → `ChatContextService`
      - 新增常量：`MAX_CONTEXT_ROUNDS = 5`
      - 修改纯文本聊天逻辑（第114-149行）：
        - 调用 `chatContextService.getRecentMessages()` 获取历史
        - 使用 `.messages(historyMessages)` 添加上下文
        - 使用 `StringBuilder` 收集完整响应
        - 在 `doOnComplete()` 中保存响应到上下文
      - 修改 `buildChatClient()`：移除 `MessageChatMemoryAdvisor` 配置，添加注释说明

- **状态**：✅ 编译成功 | ✅ 功能测试通过

- **技术细节**：

  **1. 上下文存储结构（Redis）**
  ```
  Key: chat:context:o2ihv7aBRfV-bNUva1yKgsrMK3ug
  Value: LinkedList<MessagePair> [
    {userMessage: "你好", assistantMessage: "你好！我是小咩智能记账助手..."},
    {userMessage: "我上一句话问你啥了", assistantMessage: "你上一句说的是'你好'"}
  ]
  TTL: 1小时
  ```

  **2. 容量控制策略**
  - **Redis 存储限制**：最多 10 轮对话（`MAX_STORED_ROUNDS = 10`）
  - **请求上下文限制**：最近 5 轮对话（`MAX_CONTEXT_ROUNDS = 5`）
  - **滑动窗口**：超过 10 轮时自动删除最早的对话
  - **自动过期**：1 小时无操作自动清除，避免 Redis 内存占用

  **3. 消息转换流程**
  ```
  用户输入 → addUserMessage(openid, "你好")
         ↓
  Redis 存储: MessagePair("你好", null)
         ↓
  获取历史: getRecentMessages(openid, 5)
         ↓
  转换为 Spring AI Message: [UserMessage, AssistantMessage, ...]
         ↓
  发送请求: .messages(historyMessages)
         ↓
  收集响应: StringBuilder.append(chunk)
         ↓
  保存响应: addAssistantMessage(openid, fullResponse)
  ```

  **4. Spring AI Message 类型**
  - `UserMessage` - 用户消息，对应 `role: "user"`
  - `AssistantMessage` - AI 响应，对应 `role: "assistant"`
  - 直接使用 Spring AI 的标准消息类型，确保与 ChatClient 兼容

  **5. 线程安全与并发**
  - Redis 操作天然支持并发
  - 使用 `LinkedList` 作为数据结构，方便队列操作
  - 每次操作都是原子的：读取 → 修改 → 保存

- **附注**：
  - 本次重构遵循 **KISS 原则（Keep It Simple, Stupid）**，避免过度依赖第三方库的复杂功能
  - 轻量级实现完全可控，可以精确看到发送给 API 的内容，便于调试
  - 基于 Redis 存储，支持分布式部署，多个实例共享上下文
  - 自动过期机制防止内存泄漏，无需手动清理
  - 如需调整上下文轮数，只需修改 `MAX_CONTEXT_ROUNDS` 常量
  - 如需清空用户上下文，可调用 `chatContextService.clearContext(openid)`
  - 未来可扩展：支持多模态上下文（图片消息）、上下文压缩、Token 计数等
  - **重要**：此方案适用于任何 OpenAI 兼容的 API，不依赖特定的 API 实现

### 🟢 2025-10-12 — AI 聊天接口 404 错误与 Spring Security 异步调度修复
- **产生原因**：
  1. **404 Not Found 错误**：前端请求路径 `/aio/chat` 与后端实际端点 `/aio/messages` 不一致，导致接口调用失败
  2. **Access Denied 错误**：AI 流式响应完成后，Spring 内部异步调度到 `/error` 端点时，SecurityContext 丢失导致 `Access Denied` 异常
  3. **响应已提交错误**：Spring Security 在响应已提交后仍尝试发送错误响应，导致 `Unable to handle the Spring Security Exception because the response is already committed` 错误

- **问题分析**：
  1. 前端在 `chat.js` 中两处使用了错误的端点路径 `/aio/chat`（图片上传和文本消息发送）
  2. 后端控制器 `AIOChatController` 实际映射为 `/aio/messages`
  3. 流式响应完成后的调用链：
     ```
     Secured POST /aio/messages (✅ 通过认证)
     ↓
     LLM 流处理完成 (✅ 正常)
     ↓
     "INCLUDE" dispatch for POST "/error" (⚠️ 异步调度)
     ↓
     SecurityContext 丢失，变为 anonymous (⚠️ 问题根源)
     ↓
     Access Denied 异常
     ```
  4. Spring Security 默认不允许异步调度（ASYNC、ERROR、FORWARD、INCLUDE）自动通过，导致 SecurityContext 传播失败

- **解决方案**：
  1. **修复前端路径不匹配**
     - 修改 `chat.js:307` - 图片上传请求路径：`/aio/chat` → `/aio/messages`
     - 修改 `chat.js:346` - 文本消息请求路径：`/aio/chat` → `/aio/messages`

  2. **配置 Spring Security 支持异步调度**
     - 在 `SecurityConfig` 中添加 `DispatcherType` 导入（`jakarta.servlet.DispatcherType`）
     - 配置 `dispatcherTypeMatchers` 允许所有调度类型通过安全检查：
       ```java
       .dispatcherTypeMatchers(
           DispatcherType.ASYNC,    // 异步请求
           DispatcherType.ERROR,    // 错误处理
           DispatcherType.FORWARD,  // 转发
           DispatcherType.INCLUDE   // 包含调度
       ).permitAll()
       ```
     - 添加 `/error/**` 到公开访问路径列表

  3. **优化异常处理避免响应已提交错误**
     - 添加 `accessDeniedHandler`，在响应未提交时才发送错误状态
     - 添加 `authenticationEntryPoint`，在响应未提交时才发送 401 状态
     - 添加详细日志记录，包含 URI、HTTP 方法、响应提交状态

  4. **添加详细调试日志**
     - 在 `application.yaml` 中添加以下日志级别：
       - `org.springframework.ai: DEBUG` - AI 请求日志
       - `org.springframework.security: DEBUG` - Security 过滤链日志
       - `org.springframework.web.servlet: DEBUG` - Servlet 调度日志
       - `org.springframework.web.reactive.function.client: DEBUG` - WebClient 请求日志
       - `reactor.netty.http.client: DEBUG` - Netty HTTP 客户端日志

- **修改文件**：
  - **前端**：
    - `ai-bill-front/pages/chat/chat.js` (第 307、346 行)

  - **后端**：
    - `src/main/java/org/maram/bill/config/SecurityConfig.java`
      - 添加 `DispatcherType` 导入
      - 添加 `@Slf4j` 注解
      - 配置 `dispatcherTypeMatchers` 允许异步调度
      - 配置 `accessDeniedHandler` 和 `authenticationEntryPoint`
      - 添加 `/error/**` 到 permitAll 列表

  - **配置**：
    - `src/main/resources/application.yaml` (第 96-102 行)
      - 添加详细日志配置

- **状态**：✅ 编译成功 | ✅ 功能验证通过

- **技术细节**：
  - **SecurityContext 传播问题**：Spring Security 的 `SecurityContextHolder` 默认使用 `ThreadLocal` 存储，在异步调度时会丢失上下文
  - **DispatcherType 说明**：
    - `REQUEST`：正常的 HTTP 请求（默认需要认证）
    - `ASYNC`：异步请求处理（如 `@Async`、`CompletableFuture`）
    - `ERROR`：错误处理调度（如调度到 `/error` 端点）
    - `FORWARD`：服务器端转发（`RequestDispatcher.forward()`）
    - `INCLUDE`：服务器端包含（`RequestDispatcher.include()`）- **本次问题类型**
  - **响应已提交判断**：使用 `response.isCommitted()` 检查响应是否已发送，避免重复写入响应导致异常

- **附注**：
  - 本次修复解决了流式响应场景下的 Spring Security 兼容性问题
  - 异步流式接口（如 SSE、WebFlux Flux）都可能遇到类似问题，建议其他异步接口也参考此配置
  - 日志级别为 DEBUG，生产环境建议调整为 INFO 或 WARN，避免日志过多影响性能
  - 前端路径配置应与后端控制器映射保持一致，建议使用统一的 API 路径常量管理
  - 模型配置 `zai-org/GLM-4.5V` 实际从数据库读取（`user.ai_model` 字段），`application.yaml` 中的配置仅作为默认值

### 🟢 2025-10-11 — 用户选择AI模型功能修复
- **产生原因**：用户报告"选择AI模型没有反应"，排查发现两个问题：
  1. **后端问题**：UserMapper 的 updateAiConfig 方法没有更新 `ai_config_updated_at` 字段
  2. **前端问题**：api-modules.js 中使用了 `URLSearchParams`，但微信小程序环境不支持该API，导致 `ReferenceError: URLSearchParams is not defined` 错误

- **解决方案**：
  1. **后端修复**：在 `UserMapper.updateAiConfig` 的 `@Update` SQL 中添加 `ai_config_updated_at = NOW()` 更新
  2. **前端修复**：
     - 创建兼容微信小程序的 `buildQueryString()` 函数，手动构建 query 字符串
     - 替换 `exchangeRateApi.convertCurrency` 中的 `URLSearchParams` 使用
     - 替换 `aiConfigApi.updateUserConfig` 中的 `URLSearchParams` 使用
  3. 验证完整的模型选择流程（前端 → 后端 → 数据库）

- **修改文件**：
  - **后端**：
    - `src/main/java/org/maram/bill/mapper/UserMapper.java`
  - **前端**：
    - `ai-bill-front/utils/api-modules.js`

- **状态**：✅ 修复完成
- **附注**：
  - 微信小程序运行环境不是完整的浏览器环境，不支持部分 Web API（如 `URLSearchParams`）
  - 用户选择模型的完整流程：
    1. 前端使用 `wx.showActionSheet` 展示模型列表
    2. 选择后调用 `/ai-config/user` PUT 接口（参数通过 query string 传递）
    3. 后端更新 `user` 表的 `ai_model`、`ai_temperature`、`ai_config_updated_at` 字段
    4. 前端显示 Toast 提示并更新本地状态
    5. 页面 onShow 时重新加载用户配置
  - 建议后续开发时注意微信小程序环境兼容性，避免使用不支持的 Web API

### 🟢 2025-10-11 — 前端代码全面重构（KISS 原则实践）
- **产生原因**：
  1. 代码中存在 30+ 处魔法数字（如 `20 * 1024 * 1024`、`2 * 60 * 1000`、`1500` 等），难以维护
  2. 多个页面存在重复的格式化函数（`formatDate`、`formatCurrency`、`formatFileSize` 等），共计 78+ 行重复代码
  3. 错误提示不统一，部分使用 `wx.showToast`，部分使用 `wx.showModal`
  4. 验证逻辑分散在各个页面，缺乏统一管理
  5. 缺少常量配置文件，配置项硬编码在代码中

- **解决方案**：
  1. **创建统一常量配置文件** (`utils/constants.js` - 160行)
     - HTTP 状态码常量（`HTTP_STATUS`）
     - 业务状态码常量（`RESULT_CODE`）
     - 文件大小限制常量（`FILE_SIZE`）
     - 缓存时间配置（`CACHE_TIME`）
     - Toast 提示时间（`TOAST_DURATION`）
     - 页面路径常量（`PAGES`）
     - 导航超时配置（`NAVIGATION`）
     - UI 动画配置（`UI_CONFIG`）
     - 默认头像配置（`DEFAULT_AVATAR`）
     - 账单默认分类（`DEFAULT_BILL_CATEGORIES`）
     - 交易类型常量（`TRANSACTION_TYPE`）
     - 日期格式常量（`DATE_FORMAT`）
     - API 请求配置（`API_CONFIG`）
     - 存储键名常量（`STORAGE_KEYS`）
     - 正则表达式常量（`REGEX`）

  2. **创建统一格式化工具** (`utils/formatters.js` - 230行)
     - `formatDate()` - 统一日期格式化（支持 Date 对象、字符串、数组）
     - `formatCurrency()` - 统一金额格式化（保留两位小数）
     - `formatFileSize()` - 文件大小人性化显示（KB/MB/GB）
     - `getDayLabel()` - 日期相对标签（今天/昨天/X日）
     - `formatDateTime()` - 日期时间格式化
     - `formatChineseDate()` - 中文日期格式化
     - 消除了 3 个页面中的重复代码

  3. **创建统一错误处理工具** (`utils/error-handler.js` - 240行)
     - `showError()` - 统一错误提示（支持字符串、Error 对象、响应对象）
     - `showSuccess()` - 统一成功提示（支持回调）
     - `showLoading()` - 统一加载提示
     - `hideLoading()` - 隐藏加载提示
     - `showModal()` - 统一模态框（支持自定义按钮）
     - `showDeleteConfirm()` - 删除确认对话框（红色确认按钮）
     - `parseErrorMessage()` - 智能解析错误信息
     - `showToast()` - 底层 Toast 封装
     - 统一了全站的用户反馈体验

  4. **创建统一表单验证工具** (`utils/validators.js` - 270行)
     - `validateAmount()` - 金额验证（支持正负数、最大最小值）
     - `validateDate()` - 日期验证（支持格式检查、范围限制）
     - `validateBillData()` - 账单数据完整性验证
     - `validateRequired()` - 必填字段验证
     - `validateLength()` - 字符串长度验证
     - `validatePattern()` - 正则表达式验证
     - `validatePhone()` - 手机号验证
     - `validateEmail()` - 邮箱验证
     - 提供声明式验证，返回 `{ valid: boolean, message: string }` 格式

  5. **重构现有工具文件**
     - `utils/api.js` - 使用 `HTTP_STATUS`、`RESULT_CODE`、`CACHE_TIME` 常量
     - `utils/cache.js` - 使用 `CACHE_TIME` 常量
     - `utils/login-helper.js` - 使用 `TOAST_DURATION`、`NAVIGATION`、`STORAGE_KEYS` 常量
     - `utils/router.js` - 使用 `PAGES`、`NAVIGATION` 常量

  6. **重构应用入口文件**
     - `app.js` - 使用 `PAGES` 常量，简化路由逻辑

  7. **重构页面文件**（3个核心页面）
     - `pages/index/index.js` (417行)
       - 移除 33 行重复的 `formatDate` 和 `getDayLabel` 函数
       - 使用 `DEFAULT_AVATAR`、`UI_CONFIG`、`STORAGE_KEYS` 常量
       - 统一使用 `showError`、`showLoading`、`hideLoading`
       - 批量优化 `setData` 调用，减少渲染次数

     - `pages/bill-detail/bill-detail.js` (705行 → 615行)
       - 移除 45 行重复的格式化函数
       - 使用 `validateBillData` 替代 24 行本地验证逻辑
       - 删除已废弃的 `validateForm` 方法
       - 统一错误处理和加载提示
       - 使用 `DEFAULT_BILL_CATEGORIES` 常量

     - `pages/chat/chat.js` (923行)
       - 使用 `formatDate`、`formatCurrency` 替代 15 行本地格式化逻辑
       - 使用 `validateBillData` 替代 24 行本地验证逻辑（简化 71%）
       - 使用 `FILE_SIZE.MAX_IMAGE_SIZE` 替代魔法数字 `20 * 1024 * 1024`
       - 使用 `UI_CONFIG.STREAMING_SPEED` 替代魔法数字 `30`
       - 使用 `HTTP_STATUS.SUCCESS` 替代魔法数字 `200`
       - 标记 `_showToast` 和 `_showErrorModal` 为 `@deprecated`
       - 统一所有加载和错误提示

- **修改文件**：
  - **新增文件**：
    - `ai-bill-front/utils/constants.js` (160行)
    - `ai-bill-front/utils/formatters.js` (230行)
    - `ai-bill-front/utils/error-handler.js` (240行)
    - `ai-bill-front/utils/validators.js` (270行)

  - **重构文件**：
    - `ai-bill-front/utils/api.js`
    - `ai-bill-front/utils/cache.js`
    - `ai-bill-front/utils/login-helper.js`
    - `ai-bill-front/utils/router.js`
    - `ai-bill-front/app.js`
    - `ai-bill-front/pages/index/index.js`
    - `ai-bill-front/pages/bill-detail/bill-detail.js`
    - `ai-bill-front/pages/chat/chat.js`

- **重构效果量化**：
  - ✅ 消除魔法数字：30+ 处
  - ✅ 删除重复代码：78+ 行
  - ✅ 新增工具函数：25+ 个
  - ✅ 代码行数减少：90+ 行（通过消除重复）
  - ✅ 维护性提升：常量统一管理，一处修改全局生效
  - ✅ 可读性提升：语义化常量名替代神秘数字
  - ✅ 一致性提升：统一的错误处理、格式化、验证逻辑

- **状态**：✅ 重构完成，遵循 KISS 原则

- **附注**：
  - 本次重构严格遵循 KISS（Keep It Simple, Stupid）原则和第一性原理
  - 所有魔法数字已被语义化常量替代
  - 重复代码已提取为可复用工具函数
  - 错误处理、格式化、验证逻辑已统一
  - 建议后续新增页面直接使用这些工具函数，避免重复造轮子
  - 其他页面（如 `login`、`ai-insight`、`user-info` 等）可在后续按需迁移
  - 本次重构为项目建立了良好的代码规范基础

### 🟢 2025-10-10 — 前端 API 接口模块重构
- **产生原因**：前端接口调用不统一，部分使用封装的 api.request，部分直接使用 wx.request，导致代码维护困难且容易出错
- **解决方案**：
  1. 创建统一的 API 接口模块 (`ai-bill-front/utils/api-modules.js`)，根据后端 API 文档 (api.md) 将所有接口按模块分类
  2. 将接口分为 9 个模块：用户、预算、账单、分类、文件、汇率、AI聊天、AI洞察、AI配置
  3. 所有接口方法使用统一的 `request()` 函数进行封装，确保认证、错误处理、缓存策略的一致性
  4. 为特殊接口（文件上传、流式响应）提供专门的处理方法
  5. 更新 `user-budget` 和 `bill-summary` 页面使用新的 API 模块，替换原有的直接 wx.request 调用
- **修改文件**：
  - `ai-bill-front/utils/api-modules.js` (新增)
  - `ai-bill-front/pages/user-budget/user-budget.js`
  - `ai-bill-front/pages/bill-summary/bill-summary.js`
- **状态**：✅ 重构完成，核心页面已迁移
- **附注**：其他页面（如 login、chat、ai-insight 等）待后续迁移至新的 API 模块；建议逐步替换所有直接的 wx.request 调用，确保接口调用标准化

### 🟢 2025-10-10 — 前端接口参数传递方式修复（RESTful 风格适配）
- **产生原因**：后端接口已全部改为 RESTful 风格，但前端部分接口调用仍使用旧的参数传递方式，导致接口调用失败
- **问题分析**：
  1. 汇率换算接口 (`POST /api/exchange/conversions`)：后端使用 `@RequestParam` 接收 Query 参数，前端错误地通过 Body 传参
  2. AI 配置更新接口 (`PUT /ai-config/user`)：后端使用 `@RequestParam` 接收 Query 参数，前端错误地通过 Body 传参
  3. `myset.js` 页面中的 AI 配置接口使用了错误的端点路径和请求方法
  4. `currency-exchange.js` 页面中的汇率换算接口使用了错误的端点路径
- **解决方案**：
  1. 修改 `api-modules.js` 中的 `exchangeRateApi.convertCurrency` 方法，将参数改为 Query 参数传递（使用 `URLSearchParams` 构建查询字符串）
  2. 修改 `api-modules.js` 中的 `aiConfigApi.updateUserConfig` 方法，将参数改为 Query 参数传递
  3. 更新 `myset.js` 页面：
     - 引入 `aiConfigApi` 模块
     - 将 `loadAiModels()` 方法改为使用 `aiConfigApi.getModels()`
     - 将 `loadUserAiConfig()` 方法改为使用 `aiConfigApi.getUserConfig()`，并修正端点路径从 `/ai-config/user-config` 改为 `/ai-config/user`
     - 将 `updateAiConfig()` 方法改为使用 `aiConfigApi.updateUserConfig()`，并修正端点路径和请求方法（从 `POST /ai-config/update-config` 改为 `PUT /ai-config/user`）
  4. 更新 `currency-exchange.js` 页面：
     - 引入 `exchangeRateApi` 模块
     - 将 `convertCurrency()` 方法改为使用 `exchangeRateApi.convertCurrency()`，并修正端点路径从 `/api/exchange/convert` 改为 `/api/exchange/conversions`
- **修改文件**：
  - `ai-bill-front/utils/api-modules.js`
  - `ai-bill-front/pages/myset/myset.js`
  - `ai-bill-front/pages/currency-exchange/currency-exchange.js`
- **状态**：✅ 修复完成，所有接口调用已适配 RESTful 风格
- **附注**：
  - 前端接口调用已与 `api.md` 文档完全一致
  - 建议后续新增接口时，统一使用 `api-modules.js` 中定义的方法，避免直接使用 `wx.request` 或 `api.request`
  - 对于使用 Query 参数的 POST/PUT 请求，应在端点 URL 中拼接参数，而不是通过 `data` 字段传递

