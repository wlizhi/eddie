# GraalVM native-image 在 GitHub Actions macOS runner 上因内存不足死锁

## 关键词
GraalVM, native-image, GitHub Actions, macOS, 内存不足, 死锁, ConcurrentHashMap, self-hosted runner, OOM

## 问题描述
在 GitHub Actions 的 macOS runner（免费版，总内存约 7GB）上执行 `native:compile` 时，`native-image` 进程会进入**死锁状态**，构建永远不会完成（卡死数小时后超时）。

Linux/Windows runner（同为免费版）上无此问题。

## 排查过程
1. 怀疑是 GraalVM 版本问题 — 尝试降级/升级 GraalVM 版本，问题依旧
2. 怀疑是 Maven 插件配置问题 — 调整 `-J-Xmx` 参数，发现减小堆内存后死锁概率降低但未消除
3. 查看 GraalVM GitHub Issues，发现类似报告指向 **macOS runner 物理内存过小**
4. macOS free runner 只有 7GB 物理内存，而 GraalVM native-image 在构建时需要大量堆外内存（native memory），在内存紧张时触发 JVM 内部 `ConcurrentHashMap` 并发写入死锁

## 根因分析
GraalVM native-image 在 **AOT 编译阶段** 会启动一个 JVM 来执行编译分析和字节码处理。当物理内存不足时，JVM 的 GC 和 native memory 分配产生竞争条件，在特定并发路径下触发 `ConcurrentHashMap` 的 **链表转红黑树（treeify）过程中的死锁**。

- **Linux/Windows runner**：同为免费版但内存分配策略不同，或系统 swap 机制不同，未触发此竞态
- **macOS runner**：7GB 物理内存 + 系统内存压力大 → 触发死锁

此问题在 GraalVM 官方 Issue 中已有记录：
- https://github.com/graalvm/graalvm-ce-builds/issues
- 社区建议：分配至少 10GB 以上物理内存给 macOS native-image 构建

## 解决方案
在 macOS 上使用**自托管 runner（self-hosted）**，直接利用本地机器的 GraalVM 和 Node.js 环境，跳过 GitHub 的 JDK/Node.js 下载步骤，同时分配足够的内存。

### GitHub Actions 配置示例
```yaml
native-build-macos:
  name: Native Build (macOS arm64, Self-Hosted)
  if: inputs.build-native-macos
  runs-on: [self-hosted, macOS, ARM64]

  steps:
    - uses: actions/checkout@v5

    # 使用自托管 runner 环境变量中的 GRAALVM_HOME 配置 JDK
    - name: Set up GraalVM JDK 25 (local)
      run: |
        echo "JAVA_HOME=$GRAALVM_HOME" >> "$GITHUB_ENV"
        echo "GRAALVM_HOME=$GRAALVM_HOME" >> "$GITHUB_ENV"
        echo "$GRAALVM_HOME/bin" >> "$GITHUB_PATH"
        java --version
        native-image --version

    # 使用自托管 runner 环境变量中的 NODE_HOME 配置 Node.js
    - name: Set up Node.js 24.16 (local)
      run: |
        echo "$NODE_HOME/bin" >> "$GITHUB_PATH"
        node --version
        npm --version

    # ... 后续步骤不变

    - name: Native compile (macOS arm64)
      run: |
        mvn -Pnative native:compile -pl ai-app -DskipTests \
          -Dnative-image.buildArgs="-J-Xmx10g"
```

### 关键要点
- `runs-on: [self-hosted, macOS, ARM64]` — 指定自托管 runner
- 不通过 `setup-graalvm` action，而是直接从本地环境变量读取 `GRAALVM_HOME`
- `native-image.buildArgs="-J-Xmx10g"` — 本地机器内存充裕，分配 10GB 堆内存
- Linux/Windows runner 仍使用官方云端 runner（内存需求更低，分配 `-J-Xmx4g` 即可）

## 涉及文件
- [`.github/workflows/release-mixed.yml`](.github/workflows/release-mixed.yml:150) — macOS 自托管 runner 完整配置
- [`.github/workflows/release-mixed.yml`](.github/workflows/release-mixed.yml:103) — Linux/Windows 云端 runner 对比配置（`-J-Xmx4g`）

## 参考资料
- [GraalVM GitHub Issues — native-image deadlock on memory-constrained systems](https://github.com/graalvm/graalvm-ce-builds/issues)
- [GraalVM Native Image 内存配置文档](https://www.graalvm.org/latest/reference-manual/native-image/overview/BuildOptions/)
- [GitHub Actions 自托管 Runner 配置](https://docs.github.com/zh/actions/hosting-your-own-runners/managing-self-hosted-runners/about-self-hosted-runners)
