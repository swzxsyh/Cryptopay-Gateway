# Build from the repository root:
# docker build -f crypto-payment/Dockerfile -t crypto-payment:local .
FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /workspace

# Copy POMs first to let Docker cache dependency resolution.
COPY pom.xml ./
COPY crypto-core/pom.xml crypto-core/pom.xml
COPY crypto-payment/pom.xml crypto-payment/pom.xml
COPY crypto-watcher/pom.xml crypto-watcher/pom.xml
COPY crypto-manager/pom.xml crypto-manager/pom.xml

COPY crypto-core/src crypto-core/src
COPY crypto-payment/src crypto-payment/src

RUN mvn -pl crypto-payment -am -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS=""

COPY --from=build /workspace/crypto-payment/target/crypto-payment-*.jar /app/app.jar

EXPOSE 9888

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
