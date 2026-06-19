mvn clean
mvn install -Pnative -pl ai-app -am -DskipTests
mvn -Pnative native:compile -pl ai-app -DskipTests