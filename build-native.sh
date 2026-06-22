SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

sh "$SCRIPT_DIR/frontend/build.sh"

cd "$SCRIPT_DIR" || exit
mvn clean
mvn install -Pnative -pl ai-app -am -DskipTests
mvn -Pnative native:compile -pl ai-app -DskipTests -Dnative-image.buildArgs="-J-Xmx10g"