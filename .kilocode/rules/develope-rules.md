# develope-rules.md

这是一些通用编码规范，其中 frontend 模块是前端代码，不适用此规范。

## 代码开发规范

此项目只有一个启动包 ap-app , 其他子模块都是按不同业务拆分的。

以下是编码规范：

- 由于要打包本地二进制文件，全项目业务代码禁止使用反射。
- 非必要不要擅自导入第三方依赖包，导包很大概率会导致无法打包本地二进制文件。
- ai-common 模块放一些通用实体类、常量、枚举里、工具类。
- ai-app 模块是启动聚合模块，一般不存放业务功能代码。
- 其他业务模块是传统三层架构，controller 是接口定义， service 是服务接口， service/impl 是具体服务实现类，mapper 中是数据库访问层。
- controller 中层严禁写业务代码。
- 编码时尽量最小粒度原则，尽可能减少类依赖调用。
- lombok使用的时候，实体类不要直接@Data注解标记，使用 @Getter @Setter @ToString这种写法。
- entity 中存储表结构映射类，entity/request 存放请求参数类，entity/response 存放响应参数类。entity/dto 存放一些中间计算使用的实体类。
- sqllite 时间字段注意时区问题。
