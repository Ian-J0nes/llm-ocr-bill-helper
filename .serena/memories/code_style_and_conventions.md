# 代码风格和约定

## Java 代码风格

### 命名约定
- **类名**: PascalCase (大驼峰) - 例: `BillController`, `AIOrchestrationService`
- **方法名**: camelCase (小驼峰) - 例: `getUserById`, `createBill`
- **变量名**: camelCase - 例: `userId`, `totalAmount`
- **常量**: UPPER_SNAKE_CASE - 例: `MAX_FILE_SIZE`
- **包名**: 全小写 - 例: `org.maram.bill.service`

### 分层架构模式
1. **Controller 层**
   - 处理 HTTP 请求和响应
   - 调用 Service 层
   - 不包含业务逻辑
   - 使用 `@RestController` 注解
   - 返回统一的 `Result<T>` 格式

2. **Service 层**
   - 接口定义在 `service/` 包
   - 实现类在 `service/impl/` 包
   - 包含核心业务逻辑
   - 使用 `@Service` 注解

3. **Mapper/Entity 层**
   - JPA Entity: 使用 `@Entity` 注解
   - MyBatis Mapper: 使用 `@Mapper` 注解
   - 双重数据访问策略

### Lombok 使用
项目广泛使用 Lombok 来减少样板代码:
- `@Data` - getter/setter/toString/equals/hashCode
- `@Slf4j` - 日志记录
- `@Builder` - 构建器模式
- `@NoArgsConstructor`, `@AllArgsConstructor` - 构造函数

### API 响应格式
所有 REST 接口使用统一的 `Result<T>` 包装:
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "success": true
}
```

### 注释和文档
- 使用 SpringDoc/Swagger 注解生成 API 文档
- 重要业务逻辑添加注释
- 配置类使用 `@ConfigurationProperties` 实现类型安全

### 异常处理
- 使用统一的异常处理机制
- 业务异常返回适当的 HTTP 状态码
- 错误信息封装在 `Result` 对象中

## 微信小程序代码风格

### JavaScript 风格
- 使用 ES6+ 语法
- 模块化组织代码
- 工具函数独立封装在 `utils/` 目录

### 文件组织
- 每个页面包含: `.js`, `.wxml`, `.wxss`, `.json`
- 全局配置在 `app.*` 文件中
- 静态资源统一放在 `static/` 目录

## 配置管理

### 属性配置
- 使用 `application.yaml` 而非 properties 文件
- 类型安全的配置类放在 `config/properties/` 包
- 使用 `@ConfigurationProperties` 绑定配置
- 敏感信息(密钥、密码)应该外部化(环境变量或配置中心)

### 环境配置
- 支持多环境: dev, uat, prod
- 使用 Spring Profile 管理不同环境配置

## 设计原则

1. **保持简单**: 优先选择简单的解决方案
2. **避免重复**: 检查现有代码,避免重复实现
3. **迭代现有模式**: 在引入新模式前,先尝试迭代现有实现
4. **关注相关代码**: 只修改与任务相关的代码
5. **保持整洁**: 保持代码库干净和有组织
6. **移除旧实现**: 引入新方案时移除旧代码,避免重复逻辑