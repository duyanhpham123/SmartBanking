# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -Pdocker dependency:go-offline

COPY src ./src
RUN mvn -B -Pdocker clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache wget \
    && addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /build/target/shop-*.war /app/app.war

EXPOSE 8080

ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=docker

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -qO- http://127.0.0.1:${SERVER_PORT}/v3/api-docs > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.war"]
