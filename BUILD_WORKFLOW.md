# Eddie AI 构建工作流说明

## 概述

Eddie AI 项目支持灵活的构建工作流，可以根据需要选择性构建不同平台的产物，节省 CI 算力和时间。

## 产物清单

| 类型 | 平台 | 架构 | 数量 |
|---|---|---|---|
| 原生二进制 | Linux | amd64 | 1 |
| 原生二进制 | macOS | arm64 (Apple Silicon) | 1 |
| 原生二进制 | Windows | amd64 | 1 |
| JAR 包 | 跨平台 | — | 1 |
| Electron 套壳 | Linux | x64 (AppImage) | 1 |
| Electron 套壳 | macOS | arm64 (DMG) | 1 |
| Electron 套壳 | Windows | x64 (NSIS) | 1 |

**总计 7 个产物**

## Release 工作流

### 触发方式

在 GitHub Actions 页面手动触发 `Release` workflow，支持以下参数：

```
Version * _____________________ v1.0.0

☑ 📦 JAR 包
☑ 🐧 Linux 原生二进制
☑ 🍎 macOS 原生二进制 (Apple Silicon)
☑ 🪟 Windows 原生二进制
☑ 🐧 Linux 桌面安装包 (AppImage)
☑ 🍎 macOS 桌面安装包 (DMG)
☑ 🪟 Windows 桌面安装包 (NSIS)

[Run workflow] [Cancel]
```

### 使用场景

1. **完整发布**：默认全选，构建所有 7 个产物
2. **快速测试**：只勾选 JAR + Linux 原生二进制，快速验证功能
3. **平台特定发布**：只勾选目标平台的产物（如只发布 macOS 版本）
4. **仅 Electron 发布**：如果已有原生二进制，只勾选 Electron 安装包

### 依赖关系

- Electron 安装包需要**同一平台的原生二进制**才能打包
- 如果勾选了某平台的 Electron 但没勾选对应原生二进制，构建会失败

## CI 工作流

### 一键开关

在 GitHub Settings → Actions → Variables 中设置 `CI_ENABLED` 变量：

- `CI_ENABLED: true` 或未设置 → 正常触发 CI
- `CI_ENABLED: false` → push/PR 时跳过 CI
- 手动触发 workflow → 强制执行 CI（即使已禁用）

### 使用场景

1. **正常开发**：保持 `CI_ENABLED: true`，每次 push/PR 自动跑 CI
2. **临时跳过**：设为 `false`，临时关闭 CI（如大量提交时）
3. **强制运行**：在 Actions 页面手动触发，勾选 `force-run`

## 构建优化

### 复用机制

- Electron 构建会**复用**前面步骤构建好的原生二进制产物
- 不重复编译，节省时间和算力
- 每个平台独立构建，避免交叉编译问题

### 矩阵策略

- 原生二进制：3 个 runner（ubuntu-latest, macos-latest, windows-latest）
- Electron 安装包：3 个 runner（同上）
- 并行执行，最大化效率

## 注意事项

1. **macOS 签名**：CI 中构建的 macOS DMG 未签名，如需分发需配置 Apple Developer 证书
2. **Linux ARM**：目前只支持 x64，如需 ARM 需要自托管 runner
3. **Windows 签名**：NSIS 安装包未签名，如需分发需配置代码签名证书
4. **依赖检查**：Electron 构建会检查对应平台的原生二进制是否存在，不存在会提前报错

## 故障排查

### Electron 构建失败

检查是否勾选了对应平台的原生二进制：
- Linux Electron → 需要 Linux 原生二进制
- macOS Electron → 需要 macOS 原生二进制  
- Windows Electron → 需要 Windows 原生二进制

### CI 被跳过

检查 `CI_ENABLED` 变量是否设为 `false`，或手动触发 workflow 强制执行。

### 产物缺失

检查对应的复选框是否勾选，以及构建日志是否有错误信息。