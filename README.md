# Eddie AI

个人电脑全能助手 — 助手聊天 + 智能体 + 多层记忆 + 多模型支持

## 模块

| 模块 | 说明 |
|------|------|
| ai-common | 公共定义：DTO、枚举 |
| ai-chat | 助手聊天：对话管理、上下文构建 |
| ai-agent | 智能体：任务规划、逐步执行 |
| ai-role | 角色管理：助手/智能体角色 CRUD |
| ai-memory | 三层记忆：短期/长期记忆、异步处理 |
| ai-settings | 全局设置：模型提供商、MCP、显示配置 |
| ai-app | 启动入口 + GraalVM 打包 |

## 技术栈

- 后端: Spring Boot 4.1 + Spring AI 2.0
- 数据库: SQLite + JdbcTemplate
- 前端: Vue 3 + Vite + TypeScript
- 打包: GraalVM Native Image

## 依赖关系

ai-common (零依赖)
↑         ↑         ↑
ai-role  ai-settings  ai-memory (独立，自己查 DB)
↑         ↑            ↑
└────┬────┘            │
│                 │
ai-chat  ←────────── memoryService.processAsync()
↑
ai-agent
↑
ai-app（依赖所有，唯一启动入口）

## AOT生成二进制文件

### 提取元数据

1. 先构建 fat JAR：`mvn package -DskipTests -pl ai-app -am`
2. 用 GraalVM Tracing Agent 启动应用，录制运行时行为
    ```shell
    java -agentlib:native-image-agent=config-output-dir=ai-app/src/main/resources/META-INF/native-image -jar ai-app/target/ai-app-1.0.0-SNAPSHOT.jar --server.port=11521
    ```
3. 正常操作 app（触发各种功能，访问 API），然后 Ctrl+C 停掉。Agent 会自动生成 reachability-metadata.json 到指定目录

### 打包构建本地文件

```shell
mvn clean package -Pnative -pl ai-app -am -DskipTests
mvn -Pnative native:compile -pl ai-app -DskipTests
```