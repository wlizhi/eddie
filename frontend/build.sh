SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

rm -rf "$SCRIPT_DIR/dist"
cd "$SCRIPT_DIR" && npm run build
rm -rf "$PROJECT_DIR/ai-app/target"
rm -rf "$PROJECT_DIR/ai-app/src/main/resources/static/"*
cp -r "$SCRIPT_DIR/dist/"* "$PROJECT_DIR/ai-app/src/main/resources/static/"