# 使用 Eclipse Temurin 提供的 Java 21 JDK 作为基础镜像
# UBI 9 minimal 基于 Red Hat，体积小、安全性高，适合生产环境
FROM eclipse-temurin:21-jdk-ubi9-minimal

# 设置容器内的工作目录，后续命令均在该目录下执行
WORKDIR /app

# 声明一个匿名数据卷，用于存放临时文件
# Spring Boot 在运行过程中会使用 /tmp 目录进行临时 IO 操作
VOLUME /tmp

# 定义构建参数，指定待拷贝的 Spring Boot Jar 文件路径
# 使用通配符可以避免因 jar 文件名变化导致构建失败
ARG JAR_FILE=target/*.jar

# 将本地打包好的 Jar 文件复制到容器的工作目录中
# 并统一重命名为 app.jar，便于后续启动
COPY ${JAR_FILE} app.jar

# 声明容器对外提供的服务端口
# Spring Boot 默认监听 8080 端口
EXPOSE 8080

# 容器启动时执行的命令
# 启动 Spring Boot 应用，对外提供 RESTful API 服务
ENTRYPOINT ["java","-jar","app.jar"]
