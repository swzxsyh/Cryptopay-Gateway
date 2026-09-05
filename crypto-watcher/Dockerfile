# Build from the repository root:
# docker build -f crypto-watcher/Dockerfile -t crypto-watcher:local .
FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /workspace

# Copy POMs first to let Docker cache dependency resolution.
COPY pom.xml ./
COPY crypto-core/pom.xml crypto-core/pom.xml
COPY crypto-payment/pom.xml crypto-payment/pom.xml
COPY crypto-watcher/pom.xml crypto-watcher/pom.xml
COPY crypto-manager/pom.xml crypto-manager/pom.xml

COPY crypto-core/src crypto-core/src
COPY crypto-watcher/src crypto-watcher/src

RUN mvn -pl crypto-watcher -am -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS=""

COPY --from=build /workspace/crypto-watcher/target/crypto-watcher-*.jar /app/app.jar

EXPOSE 9890

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
