SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

rm -rf "$SCRIPT_DIR/dist"
cd "$SCRIPT_DIR" && npm run build
rm -rf "$PROJECT_DIR/ai-app/target"
FRONTEND_DIR="$PROJECT_DIR/ai-app/src/main/resources/static"
mkdir -p "$FRONTEND_DIR"
rm -rf "${FRONTEND_DIR:?}/"*
cp -r "$SCRIPT_DIR/dist/"* "$FRONTEND_DIR/"