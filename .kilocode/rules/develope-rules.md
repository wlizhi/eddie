# develope-rules.md

这是一些通用编码规范，其中 frontend 模块是前端代码，不适用此规范。

## 代码开发规范

此项目只有一个启动包 ap-app , 其他子模块都是按不同业务拆分的。

### 模块说明

- ai-app 启动聚合模块：全局配置、启动类
- ai-common 通用模块：通用实体类、常量、枚举里、工具类
- ai-memory 记忆模块：三层记忆（短期，中期压缩，长期摘要）
- ai-settings 全局配置模块：全局配置、系统设置、模型服务管理
- ai-role 角色模块：助手、智能体角色自定义
- ai-chat 聊天模块：聊天会话、聊天记录、聊天消息
- ai-agent 智能体模块：智能体各接口入口、任务编排

### 编码规范

- 全项目业务代码禁止使用反射，非必要不导入第三方依赖包，兼容AOT
- 接口采用三层结构，controller -> 接口定义， service -> 业务逻辑接口， service/impl -> 业务逻辑实现，mapper -> 数据库访问（一般查询返回映射类）。
- 符合常见开发范式。
- 实体类一般不手动写get/set方法，使用lombok的 @Getter @Setter @ToString这种写法。
- 关于实体类的包结构：entity -> 表结构映射类，entity/request -> 请求参数类，entity/response -> 响应参数类。entity/dto -> 中间计算使用的实体类。
- 将数据更新到数据库之前，涉及时间的注意时区问题。
