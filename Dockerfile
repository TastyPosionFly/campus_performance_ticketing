# ===== 第一阶段：编译打包 =====
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 复制项目文件
COPY pom.xml .
COPY src ./src

# Maven 编译打包（跳过测试以加速构建）
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# ===== 第二阶段：运行应用 =====
FROM eclipse-temurin:17-jre-alpine

# 元数据标签
LABEL maintainer="TastyPosionFly"
LABEL description="Campus Performance Ticketing System Backend"
LABEL version="1.0.0"

WORKDIR /app

# 安装运行时依赖
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 创建非特权用户
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup

# 从构建阶段复制编译好的 jar 文件
COPY --from=builder /build/target/*.jar app.jar

# 创建应用所需的目录
RUN mkdir -p /app/data /app/logs && \
    chown -R appuser:appgroup /app

# 声明匿名卷
VOLUME ["/tmp", "/app/data", "/app/logs"]

# 暴露应用端口
EXPOSE 8080

# 环境变量
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=prod

# 健康检查配置
HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=40s \
            --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 切换到非 root 用户运行
USER appuser

# 容器启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]