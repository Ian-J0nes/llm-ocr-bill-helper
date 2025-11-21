# 常用命令

## 后端 Maven 命令

### 构建和运行
```bash
# 清理并构建项目
./mvnw clean install

# 运行应用
./mvnw spring-boot:run

# 仅编译(跳过测试)
./mvnw clean compile -DskipTests
```

### 测试
```bash
# 运行所有测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=TestClassName

# 运行单个测试方法
./mvnw test -Dtest=TestClassName#testMethodName
```

## 前端命令

### 微信小程序
- **开发调试**: 使用微信开发者工具导入 `ai-bill-front` 目录
- **生产构建**: 在微信开发者工具中上传代码

## 系统命令 (Darwin/macOS)

### 常用命令
```bash
# 列出文件
ls -la

# 查找文件
find . -name "*.java"

# 搜索内容
grep -r "pattern" .

# 查看目录树
tree -L 2

# Git 操作
git status
git log --oneline
git diff
```

## 开发环境要求
- Java 17 或更高版本
- Maven 3.x (或使用项目自带的 Maven Wrapper)
- MySQL 数据库
- Redis 服务器
- 微信开发者工具 (用于前端开发)