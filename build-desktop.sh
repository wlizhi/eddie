#!/bin/bash
#
# Eddie — 一键桌面应用打包脚本
# 所有构建产物汇总输出到 project-root/dist/ 目录下：
#   dist/eddie-app          — 原生二进制 (Native Image)
#   dist/eddie-app.jar      — JAR 包
#   dist/frontend/          — 前端静态资源
#   dist/electron/          — Electron 桌面安装包 (DMG/NSIS/AppImage)
# 支持多参数组合
#
# @author Eddie
# {@code @date} 2026-06-30
#
# 使用方式:
#   ./build-desktop.sh                              # 完整构建
#   ./build-desktop.sh --native --jar               # 构建 Native + JAR
#   ./build-desktop.sh --native --electron          # 构建 Native + Electron
#   ./build-desktop.sh --frontend --electron        # 构建前端 + Electron
#   ./build-desktop.sh --clean                      # 清理
#   ./build-desktop.sh --clean --all                # 清理后完整构建
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"
DIST_DIR="$PROJECT_DIR/dist"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo "${RED}[ERROR]${NC} $1"; }

# 构建结果日志数组 — 每个构建函数自行追加产物信息
BUILD_RESULTS=()
BUILD_INSTRUCTIONS=()

# ============================================================
# 清理
# ============================================================
clean() {
    log_info "清理构建产物..."
    rm -rf "$DIST_DIR"
    rm -rf "$PROJECT_DIR/target"
    rm -rf "$PROJECT_DIR/ai-app/target"
    rm -rf "$PROJECT_DIR/ai-app/src/main/resources/static/"*
    rm -rf "$PROJECT_DIR/frontend/dist"
    rm -rf "$PROJECT_DIR/electron/dist"
    rm -rf "$PROJECT_DIR/electron/node_modules"
    log_info "清理完成"
}

# ============================================================
# 构建 Native Image
# ============================================================
build_native() {
    log_info "===== 构建 Native Image ====="

    sh "$PROJECT_DIR/frontend/build.sh"

    cd "$PROJECT_DIR"
    mvn clean
    mvn install -Pnative -pl ai-app -am -DskipTests
    mvn -Pnative native:compile -pl ai-app -DskipTests -Dnative-image.buildArgs="-J-Xmx10g"

    mkdir -p "$DIST_DIR"
    cp "$PROJECT_DIR/ai-app/target/eddie-app" "$DIST_DIR/eddie-app"

    BUILD_RESULTS+=("✅ 原生二进制    → $DIST_DIR/eddie-app")
    BUILD_RESULTS+=("      大小: $(ls -lh "$DIST_DIR/eddie-app" | awk '{print $5}')")
    BUILD_INSTRUCTIONS+=("  原生二进制:  ./dist/eddie-app")
}

# ============================================================
# 构建 JAR
# ============================================================
build_jar() {
    log_info "===== 构建 JAR ====="

    # JAR 需要包含前端静态资源，先构建前端并复制到 static/
    sh "$PROJECT_DIR/frontend/build.sh"

    cd "$PROJECT_DIR"
    mvn package -DskipTests

    mkdir -p "$DIST_DIR"
    cp "$PROJECT_DIR/ai-app/target/eddie-app.jar" "$DIST_DIR/"

    BUILD_RESULTS+=("✅ JAR 包        → $DIST_DIR/eddie-app.jar")
    BUILD_RESULTS+=("      大小: $(ls -lh "$DIST_DIR/eddie-app.jar" | awk '{print $5}')")
    BUILD_INSTRUCTIONS+=("  JAR:         java -jar dist/eddie-app.jar")
}

# ============================================================
# 构建前端
# ============================================================
build_frontend() {
    log_info "===== 构建前端 ====="

    # 调用 frontend/build.sh，内部包含 npm build + 复制静态资源到后端 static/
    sh "$PROJECT_DIR/frontend/build.sh"

    # 复制到 dist 前先清理旧版本，确保完整替换
    rm -rf "$DIST_DIR/frontend"
    mkdir -p "$DIST_DIR/frontend"
    cp -r "$PROJECT_DIR/frontend/dist/"* "$DIST_DIR/frontend/"

    BUILD_RESULTS+=("✅ 前端资源      → $DIST_DIR/frontend/")
}

# ============================================================
# 打包 Electron
# ============================================================
build_electron() {
    log_info "===== 打包 Electron 桌面应用 ====="

    if [ ! -d "$PROJECT_DIR/frontend/dist" ]; then
        log_error "前端构建产物不存在，请先构建前端 (--frontend)"
        exit 1
    fi

    if [ ! -f "$PROJECT_DIR/target/eddie-app" ]; then
        if [ -f "$PROJECT_DIR/ai-app/target/eddie-app" ]; then
            mkdir -p "$PROJECT_DIR/target"
            cp "$PROJECT_DIR/ai-app/target/eddie-app" "$PROJECT_DIR/target/eddie-app"
            chmod +x "$PROJECT_DIR/target/eddie-app"
        else
            log_error "Native Image 不存在，请先构建 (--native)"
            exit 1
        fi
    fi

    cd "$PROJECT_DIR/electron"
    npm install
    npx electron-builder

    # 复制到 dist 前先清理旧版本，确保完整替换（避免 DMG 增量覆盖导致图标/文件残留）
    rm -rf "$DIST_DIR/electron"
    mkdir -p "$DIST_DIR/electron"
    cp -r "$PROJECT_DIR/electron/dist/"* "$DIST_DIR/electron/"

    BUILD_RESULTS+=("✅ 桌面安装包    → $DIST_DIR/electron/")
    local dmg_file
    dmg_file=$(ls "$DIST_DIR/electron/"*.dmg 2>/dev/null | head -1)
    if [ -n "$dmg_file" ]; then
        BUILD_RESULTS+=("      安装包: $(ls -lh "$dmg_file" | awk '{print $5}')")
    fi
    BUILD_INSTRUCTIONS+=("  桌面应用:    打开 dist/electron/ 目录下的 DMG 安装包")
}

# ============================================================
# 完整构建（优化版：避免重复编译）
# ============================================================
build_all() {
    log_info "===== 完整构建 ====="

    # 1. 构建前端，复制到 static/（JAR 和 Native 共用）
    sh "$PROJECT_DIR/frontend/build.sh"

    # 2. 编译 Native Image（含前端 + AOT）
    log_info "--- 2/4 编译 Native Image ---"
    cd "$PROJECT_DIR"
    mvn clean
    mvn install -Pnative -pl ai-app -am -DskipTests
    mvn -Pnative native:compile -pl ai-app -DskipTests -Dnative-image.buildArgs="-J-Xmx10g"
    mkdir -p "$DIST_DIR"
    cp "$PROJECT_DIR/ai-app/target/eddie-app" "$DIST_DIR/eddie-app"
    log_info "Native Image → $DIST_DIR/eddie-app"

    BUILD_RESULTS+=("✅ 原生二进制    → $DIST_DIR/eddie-app")
    BUILD_RESULTS+=("      大小: $(ls -lh "$DIST_DIR/eddie-app" | awk '{print $5}')")
    BUILD_INSTRUCTIONS+=("  原生二进制:  ./dist/eddie-app")

    # 3. 打包 JAR（前端已就位，只 package 很快）
    log_info "--- 3/4 打包 JAR ---"
    mvn package -DskipTests
    cp "$PROJECT_DIR/ai-app/target/eddie-app.jar" "$DIST_DIR/"
    log_info "JAR → $DIST_DIR/"

    BUILD_RESULTS+=("✅ JAR 包        → $DIST_DIR/eddie-app.jar")
    BUILD_RESULTS+=("      大小: $(ls -lh "$DIST_DIR/eddie-app.jar" | awk '{print $5}')")
    BUILD_INSTRUCTIONS+=("  JAR:         java -jar dist/eddie-app.jar")

    # 4. 打包 Electron（用已有的 frontend/dist/ 和 ai-app/target/eddie-app）
    log_info "--- 4/4 打包 Electron ---"
    build_electron
}

# ============================================================
# 构建产物清单
# ============================================================
print_summary() {
    echo ""
    log_info "========================================="
    log_info "构建完成！产物目录：$DIST_DIR"
    log_info "========================================="
    echo ""

    for result in "${BUILD_RESULTS[@]}"; do
        log_info "  $result"
    done

    if [ ${#BUILD_INSTRUCTIONS[@]} -gt 0 ]; then
        echo ""
        log_info "运行方式："
        for inst in "${BUILD_INSTRUCTIONS[@]}"; do
            log_info "$inst"
        done
    fi
    echo ""
}

# ============================================================
# 显示帮助
# ============================================================
print_help() {
    echo "用法: $0 [选项...]"
    echo ""
    echo "选项："
    echo "  (无参数)      完整构建（等同于 --all）"
    echo "  --all         完整构建（Native + JAR + 前端 + Electron）"
    echo "  --native      仅构建 Native Image"
    echo "  --jar         仅构建 JAR"
    echo "  --frontend    仅构建前端"
    echo "  --electron    仅打包 Electron（需要先构建 Native + 前端）"
    echo "  --clean       清理构建产物"
    echo "  --help        显示帮助"
    echo ""
    echo "示例："
    echo "  $0                              # 完整构建"
    echo "  $0 --native --jar               # 构建 Native + JAR"
    echo "  $0 --native --electron          # 构建 Native + 前端 + Electron"
    echo "  $0 --frontend --electron        # 构建前端 + Electron"
    echo "  $0 --clean --all                # 清理后完整构建"
}

# ============================================================
# 主流程 — 支持多参数
# ============================================================
main() {
    mkdir -p "$DIST_DIR"

    # 无参数 = 全部构建
    if [ $# -eq 0 ]; then
        set -- --all
    fi

    # 检查是否包含 --clean
    HAS_CLEAN=false
    for arg in "$@"; do
        if [ "$arg" = "--clean" ]; then
            HAS_CLEAN=true
            break
        fi
    done

    # 如果有 --help，显示帮助并退出
    for arg in "$@"; do
        if [ "$arg" = "--help" ] || [ "$arg" = "-h" ]; then
            print_help
            exit 0
        fi
    done

    # 如果有 --clean 且只有 --clean，清理后退出
    if [ "$HAS_CLEAN" = true ] && [ $# -eq 1 ]; then
        clean
        exit 0
    fi

    # 如果有 --clean 且还有其他参数，先清理
    if [ "$HAS_CLEAN" = true ]; then
        clean
    fi

    NEED_SUMMARY=false

    for arg in "$@"; do
        case "$arg" in
            --clean) ;;  # 已处理
            --native)
                build_native
                NEED_SUMMARY=true
                ;;
            --jar)
                build_jar
                NEED_SUMMARY=true
                ;;
            --frontend)
                build_frontend
                NEED_SUMMARY=true
                ;;
            --electron)
                # Electron 需要前端和 Native，自动检查前置
                build_electron
                NEED_SUMMARY=true
                ;;
            --all)
                build_all
                NEED_SUMMARY=true
                ;;
            --help|-h)
                # 已处理
                ;;
            *)
                log_error "未知选项: $arg"
                echo "用法: $0 [--native|--jar|--frontend|--electron|--clean|--all|--help]"
                exit 1
                ;;
        esac
    done

    if [ "$NEED_SUMMARY" = true ]; then
        print_summary
    fi
}

main "$@"
