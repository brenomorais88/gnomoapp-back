FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./
COPY src src

RUN chmod +x gradlew && ./gradlew clean installDist -x test

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV APP_HOST=0.0.0.0
ENV APP_PORT=8080

COPY --from=builder /app/build/install/daily-back /app

EXPOSE 8080

CMD ["./bin/daily-back"]
