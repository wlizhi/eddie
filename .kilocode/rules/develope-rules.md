# develope-rules.md

此项目后端只有一个启动包 ai-app, 其他子模块都是按不同业务拆分的。

## 模块说明

- ai-app 启动聚合模块：全局配置、启动类 → cc.wlizhi.eddieai.app
- ai-common 通用模块：通用实体类、常量、枚举、工具类、全量Dao操作类 → cc.wlizhi.eddieai.common
- ai-chat 聊天模块：聊天会话、聊天记录、聊天消息 → cc.wlizhi.eddieai.chat
- ai-agent 智能体模块：智能体各接口入口、任务编排 → cc.wlizhi.eddieai.agent
- ai-memory 记忆模块：三层记忆（短期，中期压缩，长期摘要） → cc.wlizhi.eddieai.memory
- ai-settings 全局配置模块：全局配置、系统设置、模型服务管理 → cc.wlizhi.eddieai.settings
- ai-role 角色模块：助手、智能体角色自定义 → cc.wlizhi.eddieai.role
- frontend 前端代码（Vue.js）

## 依赖关系（自上而下依赖）

ai-common（被所有模块依赖）
↑ ai-role / ai-settings / ai-memory（独立模块）
↑ ai-chat（依赖 ai-memory 的异步处理）
↑ ai-agent（依赖 ai-chat）
↑ ai-app（聚合所有模块，唯一启动入口）

## 技术栈

- Java 版本：25（GraalVM Native Image 打包）
- Spring Boot 4.1.0 + Spring AI 2.0.0
- 数据库：SQLite（文件路径：~/.eddie/eddie.db）
- 持久层：Spring JDBC Template（HikariCP 连接池，最大 1 连接）
- Mapper 工具：MapStruct 1.6.3
- AI 协议：OpenAI 兼容协议（DeepSeek API / OpenAI API）
- MCP 客户端：spring-ai-starter-mcp-client
- 前端：Vue 3 + Vite + TypeScript
- 打包方式：GraalVM Native Image（profile: native，-Os 优化）
- 构建工具：Maven 多模块

## 项目结构

- 配置文件：ai-app/src/main/resources/application.yml
- 数据库建表脚本：ai-app/src/main/resources/schema.sql
- 启动类：cc.wlizhi.eddie.app.EddieAiApplication
- 数据目录：${user.home}/.eddie/
- 启动端口：11520
- 扫描基础包：cc.wlizhi.eddie（Spring Boot scanBasePackages）

## 编码规范

后端：

- Java代码禁止使用反射，兼容AOT。
- 接口包名规范：`controller` -> 接口定义， `service` -> 业务逻辑接口， `service/impl` -> 业务逻辑实现，`dao` ->
  数据库访问（一般查询返回映射类）。
- 实体类包结构：`entity` -> 表结构映射类，`entity/request` -> 请求参数类，`entity/response` -> 响应参数类。`entity/dto` ->
  实体类的get/set方法使用 @Getter @Setter @ToString这种写法。
- 将数据更新到数据库时，时间字段注意时区问题。
- 查询语句禁止表关联，仅允许单表查询。

前端：

- UI 组件库默认使用 Naive UI，仅当此组件库没有提供对应的组件时才考虑其他方案
- 任何关于 CSS 类名或其他对原生组件 UI 扩展的操作，必须从官方文档或github源码中查找核实，禁止编造不存在的类名。
- 编写页面时要考虑哪些是需要全局通用的，而不是每个页面把通用逻辑都单独实现一遍。例如：样式、颜色、主题色、圆角等样式，应该从全局主题中获取，诸如此类都是如此。
- 当发现页面内容过多或一开始就预测到当前页面内容会很多时，应当提前规划内容拆分，比如js和css分离。超过500行即视为内容过大。

## 路径规则

- 严禁使用 ../../ 等相对路径
- 严禁使用 file:/ 开头的 URI 格式路径

## 开发工具

用户使用的开发工具是 `IntelliJ IDEA`，具体版本信息如下：

```text
IntelliJ IDEA 2026.1.3
Build #IU-261.25134.95, built on June 4, 2026
Runtime version: 25.0.3+9-b329.124 aarch64137.0.17-261-b86
VM: OpenJDK 64-Bit Server VM by JetBrains s.r.o.
Toolkit: sun.lwawt.macosx.LWCToolkit
macOS 14.8.7
GC: G1 Young Generation, G1 Concurrent GC, G1 Old Generation
Memory: 3072M
Cores: 8
Metal Rendering is ON
Kotlin: 261.25134.95-IJ
```

**注：前文提到的 `VSCode` 不准确，你是集成在 `IntelliJ IDEA` 中的 `Kilo Code` 插件**。