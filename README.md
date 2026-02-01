# 校园演出票务系统

本项目为毕业设计——**校园演出票务系统**，旨在为高校演出活动的票务管理与购票流程提供便捷、高效的信息化解决方案。系统基于 **Java** 开发，采用现代后端技术架构，支持容器化部署，方便维护与扩展。

## 项目简介

校园演出票务系统主要服务于高校师生，解决演出活动门票的发布、销售、验证及统计等问题。用户可通过系统进行在线选座、购票，并支持多种票务管理功能，提升演出活动的运行效率和观演体验。

## 主要功能模块

- **用户管理**：支持观众注册、登录、个人信息维护等功能。
- **演出活动管理**：演出信息发布、编辑，场次管理，座位分配。
- **在线选座购票**：座位图展示，选座购票、订单生成、支付（可集成第三方支付接口）。
- **票务验证**：二维码/验证码电子票，入场核验功能。
- **票务统计与报表**：票务销售统计、用户数据分析、财务报表导出。
- **后台管理系统**：演出管理者和系统管理员的权限分级及操作后台。

## 技术栈

- **主语言**：Java（99.7%）
    - Spring Boot 框架
    - MyBatis / JPA 持久层
    - RESTful API 设计
- **容器化部署**：Dockerfile（0.3%）
- **数据库**：MySQL / PostgreSQL（可选，需配置）
- **前端部分**：请根据实际项目代码补充，如 Vue.js / React，或生成式页面

## 项目结构示例

```
campus_performance_ticketing/
├── src/
│   ├── main/
│   │   ├── java/       # 核心后端代码
│   │   ├── resources/  # 配置文件、模板资源
│   ├── test/           # 测试代码
├── Dockerfile          # 容器部署脚本
├── docs/               # 项目文档
```

## 环境要求

- JDK 8+
- Maven 3+
- MySQL 或 PostgreSQL 数据库
- Docker（可选，推荐用于部署）

## 快速启动

1. 克隆代码仓库

    ```bash
    git clone https://github.com/TastyPosionFly/campus_performance_ticketing.git
    ```

2. 修改数据库配置信息（`src/main/resources/application.yml`）

3. 编译并运行

    ```bash
    mvn clean package
    java -jar target/campus_performance_ticketing.jar
    ```

4. 或使用 Docker 容器部署

    ```bash
    docker build -t campus-ticketing .
    docker run -p 8080:8080 campus-ticketing
    ```

## 贡献指南

1. Fork 本仓库
2. 提交 Pull Request
3. 建议在 issues 区提出需求或反馈

## License

本毕业设计仅供学习交流，若需商业使用请联系作者。

---

如需详细文档、演示视频或更多资料，请联系 [TastyPosionFly](https://github.com/TastyPosionFly)。
