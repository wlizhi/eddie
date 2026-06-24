# Eddie

个人电脑AI助手 — 助手聊天 + 智能体 + 多模型支持

---

## 模块

| 模块                           | 包名                           | 说明                   |
|------------------------------|------------------------------|----------------------|
| [`ai-common`](ai-common)     | `cc.wlizhi.eddieai.common`   | 公共定义：DTO、枚举、工具类      |
| [`ai-chat`](ai-chat)         | `cc.wlizhi.eddieai.chat`     | 助手聊天：对话管理、上下文构建      |
| [`ai-agent`](ai-agent)       | `cc.wlizhi.eddieai.agent`    | 智能体：任务规划、逐步执行        |
| [`ai-role`](ai-role)         | `cc.wlizhi.eddieai.role`     | 角色管理：助手 / 智能体角色 CRUD |
| [`ai-memory`](ai-memory)     | `cc.wlizhi.eddieai.memory`   | 记忆：短期、中期压缩、长期摘要      |
| [`ai-settings`](ai-settings) | `cc.wlizhi.eddieai.settings` | 全局设置：模型提供商、MCP、显示配置  |
| [`ai-app`](ai-app)           | `cc.wlizhi.eddieai.app`      | 启动入口 + GraalVM 打包    |

## 依赖关系

依赖方向自上而下，下层不依赖上层。

```
ai-common
    ↑           ↑            ↑
ai-role   ai-settings   ai-memory
    ↑           ↑            ↑
    └─────┬─────┘            │
          │                  │
     ai-chat  ←──────── ai-memory
          ↑
     ai-agent
          ↑
     ai-app
```

## 技术栈

| 类别      | 技术                                         |
|---------|--------------------------------------------|
| 语言      | Java 25（GraalVM Native Image 打包）           |
| 后端框架    | Spring Boot 4.1.0 + Spring AI 2.0.0        |
| 数据库     | SQLite（`~/.eddie/eddie.db`）                |
| 持久层     | Spring JDBC Template（HikariCP 连接池，最大 1 连接） |
| AI 协议   | OpenAI 兼容协议（DeepSeek API / OpenAI API）     |
| MCP 客户端 | `spring-ai-starter-mcp-client`             |
| 前端      | Vue 3 + Vite + TypeScript                  |
| 构建工具    | Maven 多模块                                  |
| 打包方式    | JAR（源码构建）/ GraalVM Native Image（AOT 编译）    |

## 打包构建

### 源码构建（JAR）

构建可执行 JAR 包，依赖 JRE 25 运行。

```shell
# 1. 构建前端
cd frontend && npm run build

# 2. 复制前端产物到后端静态资源目录
rm -rf ai-app/src/main/resources/static/*
cp -r frontend/dist/* ai-app/src/main/resources/static/

# 3. 打包后端
mvn clean package -DskipTests
```

产物：`ai-app/target/ai-app-1.0.0.jar`

运行：

```shell
java -jar ai-app/target/ai-app-1.0.0.jar
```

> 也可使用 `mvn install -DskipTests` 将模块安装到本地 Maven 仓库。

### AOT 编译（GraalVM Native Image）

编译为本地二进制文件，无需 JRE，启动快、资源占用低。需要所有依赖包全部兼容 AOT 注册，否则会编译失败或运行异常。

```shell
# 1. 构建前端
cd frontend && npm run build

# 2. 复制前端产物
rm -rf ai-app/src/main/resources/static/*
cp -r frontend/dist/* ai-app/src/main/resources/static/

# 3. AOT 预处理 + 本地编译
mvn clean
mvn install -Pnative -pl ai-app -am -DskipTests
mvn -Pnative native:compile -pl ai-app -DskipTests
```

> 可通过 `-Dnative-image.buildArgs` 传递额外参数给 `native-image`，例如限制内存：
> `-Dnative-image.buildArgs="-J-Xmx10g"`

产物：`ai-app/target/ai-app`

也可以直接使用项目根目录的一键构建脚本：

```shell
./build-native.sh
```

#### Native Image 构建参数

配置在 [`ai-app/pom.xml`](ai-app/pom.xml) 的 `native` profile 中：

| 参数                                         | 说明          |
|--------------------------------------------|-------------|
| `-Os`                                      | 优化二进制体积     |
| `-H:GenerateDebugInfo=0`                   | 不生成调试信息     |
| `--initialize-at-build-time=...`           | 指定类在构建时初始化  |
| `--allow-incomplete-classpath`             | 允许不完整的类路径   |
| `--report-unsupported-elements-at-runtime` | 运行时报告不支持的元素 |

## 项目结构

完整项目结构说明请查看 [`PROJECT_STRUCTURE.md`](PROJECT_STRUCTURE.md)。

## 快速启动

1. 确保已安装 Java 25、Node.js
2. 源码构建并运行：

```shell
# 构建前端
cd frontend && npm install && npm run build
cd ..

# 复制静态资源
cp -r frontend/dist/* ai-app/src/main/resources/static/

# 打包并启动
mvn clean package -DskipTests
java -jar ai-app/target/ai-app-1.0.0.jar
```

3. 访问：`http://localhost:11520`

## 数据库

- 文件路径：`~/.eddie/eddie.db`
- 建表脚本：[`ai-app/src/main/resources/schema.sql`](ai-app/src/main/resources/schema.sql)
