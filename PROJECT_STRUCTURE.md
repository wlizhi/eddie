# 项目结构

```
eddie/
├── ai-app/                      # 启动聚合模块
│   └── src/main/
│       ├── java/cc/wlizhi/eddie/app/
│       │   ├── EddieApplication.java   # Spring Boot 启动类
│       │   └── config/                   # 全局配置（拦截器、异常处理、WebMVC 等）
│       └── resources/
│           ├── application.yml           # 全局配置
│           ├── schema.sql                # 数据库建表脚本
│           ├── static/                   # 前端构建产物（自动生成）
│           ├── prompts/                  # AI Prompt 模板
│           └── META-INF/spring/          # Spring AOT 注册配置
├── ai-common/                   # 通用模块
│   └── src/main/java/cc/wlizhi/eddie/common/
│       ├── entity/              # 通用实体类
│       ├── enums/               # 枚举定义
│       ├── util/                # 工具类
│       └── dao/                 # 全量 Dao 操作类
├── ai-chat/                     # 聊天模块
│   └── src/main/java/cc/wlizhi/eddie/chat/
│       ├── controller/          # REST 接口（会话、聊天、助手、模型）
│       ├── service/             # 业务逻辑
│       │   └── impl/            # 业务实现
│       ├── handler/             # 聊天处理器链（预处理、后处理、SSE 转换、思考链）
│       │   └── impl/            # 处理器实现
│       ├── context/             # 会话上下文
│       └── entity/
│           ├── dto/             # 中间计算 DTO
│           ├── request/         # 请求参数
│           └── response/        # 响应 VO
├── ai-agent/                    # 智能体模块
│   └── src/main/java/cc/wlizhi/eddie/agent/
│       ├── controller/          # 智能体接口入口
│       ├── service/             # 任务编排
│       ├── mapper/              # 数据映射
│       ├── dto/                 # DTO 定义
│       └── entity/              # 实体类
├── ai-memory/                     # 记忆模块（上下文记忆及缓存）
│   └── src/main/java/cc/wlizhi/eddie/memory/
│       ├── controller/          # 记忆接口
│       ├── service/             # 模型记忆处理逻辑
│       ├── context/             # 上下文及缓存
│       └── entity/              # 记忆实体
├── ai-tools/                     # 工具模块（内置工具注册与管理）
│   └── src/main/java/cc/wlizhi/eddie/tools/
│       ├── controller/          # 工具接口入口
│       ├── service/             # 工具注册、回调解析
│       └── tool/                # 内置工具实现（WebSearch、WebFetch）
├── ai-settings/                 # 全局配置模块
│   └── src/main/java/cc/wlizhi/eddie/settings/
│       ├── controller/          # 设置接口
│       ├── service/             # 配置管理、模型提供商、MCP 配置
│       └── entity/              # 设置实体
├── frontend/                    # 前端代码（Vue 3 + Vite + TypeScript）
├── build-native.sh              # Native Image 一键构建脚本
└── pom.xml                      # 父 POM
```

## 模块依赖关系

详见 [README.md 依赖关系](README.md#依赖关系)。
