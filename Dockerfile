FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=builder /workspace/target/safevault-backend-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
