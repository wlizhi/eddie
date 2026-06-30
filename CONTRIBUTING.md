# Contributing to Eddie

感谢你对 Eddie 的兴趣！以下是参与贡献的指南。

## 开发环境

- **JDK 25**（GraalVM，推荐使用 [SDKMAN](https://sdkman.io) 管理）
- **Node.js** 24+
- **Maven** 3.9+
- **IntelliJ IDEA**（推荐，已配置项目设置）

## 分支策略

- `main` — 稳定版，对应 GitHub Releases
- `develop` — 开发分支，所有 Pull Request 合并到此

## 提 PR 流程

1. **Fork** 本仓库
2. 从 `develop` 创建特性分支：
   - 新功能：`feat/简短描述`（如 `feat/image-generation`）
   - Bug 修复：`fix/简短描述`（如 `fix/sse-disconnect`）
   - 重构：`refactor/简短描述`
3. 提交代码，确保编译通过：

   ```shell
   cd frontend && npm run build
   cd ..
   rm -rf ai-app/src/main/resources/static/*
   cp -r frontend/dist/* ai-app/src/main/resources/static/
   mvn clean verify -DskipTests
   ```

4. 提交 Pull Request 到 `develop` 分支
5. 等待 Code Review

## 代码规范

### 后端

- 遵循项目现有包结构：`controller` / `service` / `service/impl` / `dao` / `entity`
- 使用 `@Getter` / `@Setter` / `@Slf4j` 等 Lombok 注解
- 数据库操作禁止表关联，注意查询效率
- 更新数据库时注意时区问题
- 根据业务操作重要性添加适量日志
- 新增代码需兼容 Spring AOT（GraalVM Native Image）

### 前端

- TypeScript，使用 Naive UI 组件库
- CSS 不允许硬编码颜色值，必须通过 CSS 变量引用
- 间距/尺寸使用 `var(--space-*)` / `var(--size-*)` 变量
- 单文件不超过 500 行

## Commit 规范

使用中文或英文写 commit message，建议格式：

```
类型: 简短描述

详细说明（可选）
```

类型参考：`feat` / `fix` / `refactor` / `docs` / `style` / `chore`

## 反馈与讨论

- [Issues](https://github.com/wlizhi/eddie/issues) — Bug 报告 / 功能建议
- [Discussions](https://github.com/wlizhi/eddie/discussions) — 技术讨论 / 使用问题
