# 校园电商/外卖智能服务平台 — Java 重构版

> Spring Boot 3.2 + MySQL 8.0 + Nginx 负载均衡 + Docker Compose

## 架构对比

| 维度 | Python 版 | Java 版 |
|------|----------|---------|
| 语言 | Python 3.11 | Java 17 |
| 框架 | http.server (标准库) | Spring Boot 3.2 |
| 数据库 | SQLite | MySQL 8.0 |
| 负载均衡 | 无 | Nginx (round-robin, weight=3) |
| 容器化 | 单容器 | 多容器 (docker compose) |
| 微服务通信 | requests HTTP | Spring RestTemplate |
| Agent | MockLLM 规则 | Java 规则引擎 |
| 构建 | 无 | Maven 多模块 |

## 项目结构

```
campus-assistant-java/
├── pom.xml                      # Maven 父 POM (多模块)
├── docker-compose.yml           # 7 服务编排
├── README.md
│
├── mysql/
│   └── init.sql                 # 建表 + 种子数据
│
├── nginx/
│   └── nginx.conf               # 负载均衡 (主应用x2 + 3微服务)
│
├── campus-common/               # 共享模块 (DTO/工具)
│
├── order-service/               # 订单微服务 :8001
│   ├── Dockerfile
│   └── src/.../OrderController.java
│
├── product-service/             # 商品微服务 :8002
│   ├── Dockerfile
│   └── src/.../ProductController.java
│
├── logistics-service/           # 物流微服务 :8003
│   ├── Dockerfile
│   └── src/.../LogisticsController.java
│
├── campus-server/               # 主应用 :8000
│   ├── Dockerfile
│   └── src/.../
│       ├── controller/ServerController.java  # 15+ REST API
│       └── agent/
│           ├── AgentOrchestrator.java        # 路由+编排+smart_*
│           ├── Guardrails.java               # 输入/授权/输出护栏
│           └── MemoryStore.java              # 会话记忆
│
└── campus-server/src/main/resources/
    ├── static/web/               # Vue 3 前端 (4页面)
    └── flows/                    # BPMN 流程文件
```

## 快速启动

```bash
# 1. 构建所有模块
mvn clean package -DskipTests

# 2. Docker Compose 一键启动
docker compose up --build -d

# 3. 访问
#  用户端:   http://localhost
#  商家端:   http://localhost/merchant
#  骑手端:   http://localhost/rider
#  智能助理: http://localhost/chat
```

## 水平扩缩

```bash
# 主应用扩展到 5 实例
docker compose up --scale campus-server=5 -d

# Nginx 自动轮询分发请求到 5 个实例
```

## 技术栈

- **后端**: Spring Boot 3.2, Spring JDBC, RestTemplate
- **数据库**: MySQL 8.0 (InnoDB, UTF-8mb4)
- **负载均衡**: Nginx Alpine (反向代理 + upstream 轮询)
- **容器**: Docker (eclipse-temurin:17-jre-alpine)
- **构建**: Maven 3.9+, Java 17
- **前端**: Vue 3 CDN + Leaflet (与 Python 版相同)
