FROM openjdk:17-jdk-slim

WORKDIR /app

# 复制 Maven wrapper 和 pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 构建项目
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# 复制 JAR 文件
COPY target/safevault-backend-*.jar app.jar

# 暴露端口
EXPOSE 8080

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
