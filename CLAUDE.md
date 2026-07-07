# 校园电商/外卖智能服务平台 — Java 重构版

## 项目概述

基于 Spring Boot 的 Agent 服务工程应用实践课程项目。从 Python 版重构为 Java。
三层架构: BPMN 业务流程 → 微服务 → Agent 智能编排。
三角色平台: 用户端(点餐+智能助理)、商家端(商品管理+人工退款)、骑手端(接单+配送)。

**架构特征:** 面向服务(SOA) + DevOps自动化 + SLA质量评价

## 路径

- **Java 重构版**: `D:\lab\Agent服务工程\campus-assistant-java\`
- **Python 原版**: `D:\lab\Agent服务工程\campus-assistant\`

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.2 + Spring JDBC + Spring Cloud |
| 数据库 | MySQL 8.0 (7表, InnoDB, UTF-8mb4) |
| 服务间通信 | Spring Cloud OpenFeign (替代 RestTemplate) |
| 容错 | Resilience4j 断路器 + 重试 + 超时 |
| 负载均衡 | Nginx Alpine + Spring Cloud LoadBalancer |
| 会话共享 | InMemorySessionStore (支持 Redis 扩展) |
| 构建 | Maven 3.9+ 多模块 (6模块) |
| 容器 | Docker Compose 8服务 |
| 编排 | Kubernetes (Deployment + Service + HPA) |
| CI/CD | Jenkins Pipeline (9阶段) |
| 前端 | Vue 3 CDN + Leaflet |
| 测试 | JUnit 5 + 规则评测 (8用例) |
| 监控 | Spring Actuator + SLA 质量评价 |
| API文档 | Springdoc OpenAPI (Swagger UI) |

## 项目结构

```
campus-assistant-java/
├── pom.xml                            # Maven 父 POM (6模块 + Spring Cloud BOM)
├── docker-compose.yml                 # Docker Compose 8服务编排
├── Jenkinsfile                        # Jenkins CI/CD 流水线 (9阶段)
├── CLAUDE.md                          # 本文件
│
├── mysql/init.sql                     # 建表 + 种子数据 (含 SET NAMES utf8mb4)
├── nginx/nginx.conf                   # 负载均衡 (主应用x2 + 3微服务 upstream)
│
├── k8s/
│   └── deployment.yaml                # K8s 完整部署 (4微服务 + MySQL + Redis + HPA)
│
├── scripts/
│   └── sla_evaluate.sh                # SLA 服务质量评测脚本
│
├── campus-common/                     # 共享模块
│   └── src/main/java/com/campus/common/
│       ├── model/                     # Product, Order, User, Logistics, Policy
│       └── dto/                       # ApiResponse, OrderResponse, ProductResponse, LogisticsResponse
│
├── order-service/                     # 订单微服务 :8001
│   ├── Dockerfile
│   └── src/.../controller/OrderController.java  # 8 REST端点 + JSON序列化修复
│
├── product-service/                   # 商品微服务 :8002
│   ├── Dockerfile
│   └── src/.../controller/ProductController.java  # 5 REST端点
│
├── logistics-service/                 # 物流微服务 :8003
│   ├── Dockerfile
│   └── src/.../controller/LogisticsController.java  # track + route地图
│
└── campus-server/                     # 主应用 :8000
    ├── Dockerfile
    └── src/main/java/com/campus/server/
        ├── CampusServerApplication.java     # @EnableFeignClients
        ├── controller/
        │   └── ServerController.java        # 15+ REST API + 页面路由 + /api/chat
        ├── agent/
        │   └── AgentOrchestrator.java       # 路由+6种对话模式+Feign集成
        ├── bpmn/
        │   ├── BpmnEngine.java              # BPMN XML解析执行引擎(StAX)
        │   └── BpmnHandlers.java            # 8个BPMN处理器(Feign调用微服务)
        ├── rag/
        │   └── RagService.java              # n-gram TF向量RAG + SQL回退
        ├── guardrails/
        │   └── Guardrails.java              # 输入注入/授权越权/输出PII脱敏
        ├── memory/
        │   └── MemoryStore.java             # 会话记忆(滑动窗口+摘要, 保留兼容)
        ├── service/
        │   ├── OrderService.java            # 订单业务逻辑
        │   ├── ProductService.java          # 商品业务逻辑
        │   ├── LogisticsService.java        # 物流远程调用(Feign)
        │   └── PolicyService.java           # 政策CRUD + RAG
        ├── client/                          # ── SOA Feign 客户端 ──
        │   ├── OrderServiceClient.java      # @FeignClient 订单服务契约
        │   ├── ProductServiceClient.java    # @FeignClient 商品服务契约
        │   └── LogisticsServiceClient.java  # @FeignClient 物流服务契约
        ├── session/
        │   ├── SessionStore.java            # 会话存储接口
        │   └── InMemorySessionStore.java    # 内存实现(支持Redis扩展)
        ├── config/
        │   ├── AppConfig.java               # RagService + SessionStore + ObjectMapper Bean
        │   ├── WebConfig.java               # CORS 配置
        │   └── RequestLoggingFilter.java    # 请求日志过滤器
        ├── exception/
        │   └── GlobalExceptionHandler.java  # 全局异常处理
        ├── evaluate/
        │   └── EvaluateController.java      # 评测框架 (GET /api/evaluate, 8用例)
        ├── sla/                             # ── SLA 服务质量管理 ──
        │   ├── SlaRecorder.java             # 请求记录器(AtomicLong计数器)
        │   └── SlaController.java           # SLA评价API (GET /api/sla/report)
        └── resources/
            ├── application.properties       # 服务URL + Resilience4j + Actuator
            ├── static/                      # Vue 3 前端 (4页面)
            │   ├── customer.html            # 用户端
            │   ├── merchant.html            # 商家端
            │   ├── rider.html               # 骑手端
            │   ├── index.html               # 智能助理独立页
            │   ├── app.js                   # 共享Vue工具模块
            │   └── shared.css               # 统一样式
            └── flows/
                ├── aftersale_refund.bpmn    # 售后退款BPMN流程(10节点)
                └── logistics_map.bpmn       # 物流地图BPMN流程(5节点)
```

## 构建与运行

```bash
# 编译+测试
cd campus-assistant-java
mvn clean test

# Docker 部署 (需 Docker Desktop 运行中)
mvn clean package -DskipTests
docker compose up --build -d

# 扩缩
docker compose up --scale campus-server=5 -d

# 停止 (含数据卷清理)
docker compose down -v

# 访问
# http://localhost             用户端 (点餐+智能助理)
# http://localhost/merchant    商家端 (商品管理+退款)
# http://localhost/rider       骑手端 (接单+配送)
# http://localhost/chat        智能助理独立页
# http://localhost/api/evaluate   评测结果 (GET)
# http://localhost/api/sla/report SLA质量报告 (GET)
# http://localhost/actuator/health 健康检查
# http://localhost/swagger-ui.html API文档 (Springdoc)
```

## SOA 面向服务架构

### 服务间通信（OpenFeign）

```
重构前: AgentOrchestrator → new RestTemplate().getForObject("http://order-service:8001/..." + oid, Map.class)
                             └── URL硬编码, 无容错

重构后: AgentOrchestrator → @FeignClient(url="${order.service.url}") → orderClient.getOrder(oid)
                             └── Resilience4j: 断路器 + 重试(3次) + 超时(5s) + 配置外部化
```

### 服务契约

| 契约类型 | 实现 |
|---------|------|
| 统一响应体 | `ApiResponse<T>` (code, message, data) |
| 订单DTO | `OrderResponse` (强类型, 替代Map<String,Object>) |
| 商品DTO | `ProductResponse` |
| 物流DTO | `LogisticsResponse` |
| API文档 | Springdoc OpenAPI + Swagger UI |

### Feign 客户端清单

| 客户端 | 目标服务 | Fallback |
|--------|---------|----------|
| OrderServiceClient | order-service:8001 | 返回error Map |
| ProductServiceClient | product-service:8002 | 返回空列表 |
| LogisticsServiceClient | logistics-service:8003 | 返回has_map_data=false |

### 容错配置 (Resilience4j)

```properties
# 断路器: 滑动窗口10次, 失败率50%→开闸, 30s后半开
resilience4j.circuitbreaker.configs.default.slidingWindowSize=10
resilience4j.circuitbreaker.configs.default.failureRateThreshold=50
resilience4j.circuitbreaker.configs.default.waitDurationInOpenState=30s

# 重试: 最多3次, 间隔500ms
resilience4j.retry.configs.default.maxAttempts=3

# 超时: 5s
resilience4j.timelimiter.configs.default.timeoutDuration=5s
```

### 配置外部化

```properties
order.service.url=http://order-service:8001
product.service.url=http://product-service:8002
logistics.service.url=http://logistics-service:8003
```

## DevOps 自动化流程

### CI/CD 流水线 (Jenkins)

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Stage 1  │ → │ Stage 2  │ → │ Stage 3  │ → │ Stage 4  │ → │ Stage 5  │
│ Checkout │    │ Build &  │    │ Static   │    │ Eval     │    │ Package  │
│ 代码检出 │    │ UnitTest │    │ Analysis │    │ 离线评测  │    │ Maven打包 │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
                                                                     │
     ┌───────────────────────────────────────────────────────────────┘
     │
     ▼
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Stage 6  │ → │ Stage 7  │ → │ Stage 8  │ → │ Stage 9  │
│ Docker   │    │ Docker   │    │ Deploy   │    │ Post     │
│ Build    │    │ Push     │    │ to K8s   │    │ 通知+清理 │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
```

### Pipeline 阶段说明

| 阶段 | 操作 | 工具 | 产出 |
|------|------|------|------|
| 1. Checkout | 代码拉取 + commit记录 | Git | 源码 + GIT_COMMIT |
| 2. Build & Test | 编译 + JUnit 19测试 | Maven + JUnit5 | target/ |
| 3. Static Analysis | 代码行数/文件数统计 | shell | 质量报告 |
| 4. Evaluation | 启动Docker → 运行评测 | curl + Docker | eval_result.json |
| 5. Package | Maven打包(跳过测试) | Maven | *.jar (4个) |
| 6. Docker Build | Docker Compose构建镜像 | Docker Compose | 4 Docker镜像 |
| 7. Docker Push | 推送镜像到仓库 | docker push | 远程仓库 |
| 8. Deploy to K8s | kubectl滚动更新 | kubectl | K8s集群 |
| 9. Post | 邮件通知 + 工作区清理 | Email | 构建报告 |

### 一键部署

```bash
# Docker Compose 一键部署
docker compose up --build -d

# K8s 一键部署
kubectl apply -f k8s/

# Jenkins 一键触发
curl -X POST http://jenkins:8080/job/campus-assistant-java/build \
  --user admin:token
```

## Kubernetes 部署架构

```
┌──────────────────────────────────────────────────────────┐
│                  Kubernetes Cluster                       │
│                                                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │ Namespace: campus-prod                          │    │
│  │                                                 │    │
│  │  campus-server (x2+)  ← LoadBalancer :80→:8000 │    │
│  │  order-service (x2)   ← ClusterIP :8001         │    │
│  │  product-service (x2) ← ClusterIP :8002         │    │
│  │  logistics-service (x2) ← ClusterIP :8003       │    │
│  │  mysql (x1)           ← ClusterIP :3306         │    │
│  │  redis (x1)           ← ClusterIP :6379         │    │
│  │                                                 │    │
│  │  HPA: campus-server (min=2, max=10, CPU>70%)    │    │
│  └─────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────┘
```

## SLA 服务质量评价

### 监控指标

| 维度 | 指标 | 采集方式 |
|------|------|---------|
| 可用性 | 成功率 % (200/总请求) | SlaRecorder + AtomicLong |
| 响应时间 | avg/min/max/P50/P95/P99 | SlaRecorder + 百分位计算 |
| 吞吐量 | 请求/秒 (RPS) | 累计请求/运行时间 |
| 服务健康 | UP/DOWN | 定时探测微服务端口 |

### SLA 评级标准

| 评级 | 可用性 | 响应时间 |
|------|--------|---------|
| A+ (优秀) | ≥99.99% | <100ms |
| A (良好) | ≥99.9% | <300ms |
| B (及格) | ≥99.0% | <1000ms |
| C (需改进) | ≥95.0% | <3000ms |
| D (瓶颈) | <95.0% | ≥3000ms |

### 评测结果 (当前)

| 指标 | 值 | 评级 |
|------|-----|------|
| 评测通过率 | 100% (8/8) | A+ |
| 4微服务健康 | 全部UP | A+ |
| 可用性 | 100.0% | A+ (4个9) |

### SLA API

| 端点 | 方法 | 说明 |
|------|------|------|
| /api/sla/report | GET | 完整SLA报告 |
| /api/sla/score | GET | 简要评分 |

## 智能助理对话模式 (6种)

路由逻辑: `AgentOrchestrator.orchestrate()` → `router()` → 分派

| 模式 | 触发词 | 示例输入 | 处理方式 |
|------|--------|---------|---------|
| 自然语言下单 | 点一份/下一单/来一杯 | "帮我点一份黄焖鸡米饭送到三号宿舍楼" | smartOrder() → Feign创建订单 |
| 售后BPMN流程 | 退/赔/补偿 + 订单号 | "订单20260601001超时有补偿吗" | BpmnEngine.run() → 8步BPMN |
| 开放式售后 | 退/不满意 + 无订单号 | "我买的耳机不满意能退吗" | smartResolve() + RAG兜底 |
| 物流地图 | 物流/配送/路线 + 订单号 | "查物流20260601001的配送路线" | BpmnEngine.run() → 地图数据 |
| 导购查询 | 多少钱/想买/推荐 | "蓝牙耳机多少钱" | reactAgent() → Feign查商品 |
| 兜底 | 其他 | "你好" | 通用回复 + RAG政策检索 |

## BPMN 引擎

Java版 BPMN 引擎实际解析 `.bpmn` XML 文件并按流程执行：

```
BpmnEngine.load() → StAX解析XML提取节点和顺序流
BpmnEngine.run()  → 从startEvent开始 → 按任务/网关/结束事件顺序执行
                    每个serviceTask调用handler (delegateExpression → Feign调用微服务)
                    exclusiveGateway条件分支 (safeEval安全求值)
```

## RAG 检索

基于字符级 n-gram (1,2) TF向量 + 余弦相似度的政策检索：
- `RagService.init()` 从数据库加载政策文档 → 构建 VectorStore
- `VectorStore.search()` n-gram向量化查询 → 余弦相似度排序 → top-k
- 回退机制：向量为空时自动回退 SQL 查询

## 评测框架

`GET /api/evaluate` 返回 8 个固定用例的评测结果 (100%通过率)：
- 覆盖：意图路由、BPMN售后、BPMN物流、导购、RAG政策、护栏拦截、开放式售后、物流地址
- 基于关键词的规则判断器（无需LLM）

## API 清单

| 端点 | 方法 | 用途 |
|------|------|------|
| /api/chat | POST | 智能助理 (核心) |
| /api/products | GET/POST | 商品列表/上架 |
| /api/products/{id} | PUT/DELETE | 商品修改/下架 |
| /api/orders | GET/POST | 用户订单/下单 |
| /api/store/orders | GET | 商家订单 |
| /api/store/stats | GET | 商家统计 |
| /api/store/orders/{id}/manual-refund | POST | 人工退款 (金额≥100) |
| /api/rider/available | GET | 骑手可接订单 |
| /api/rider/accept | POST | 骑手接单 |
| /api/rider/status | POST | 配送状态更新 |
| /api/rider/history | GET | 骑手历史 |
| /api/evaluate | GET | 离线评测结果 |
| /api/health | GET | 健康检查 |
| /api/sla/report | GET | SLA完整报告 |
| /api/sla/score | GET | SLA简要评分 |
| /actuator/health | GET | Spring Actuator |

## 护栏三层

1. **输入护栏** `Guardrails.inputGuard()`: 提示注入检测 (严格关键词直接拦截, 普通≥2个触发)
2. **授权护栏** `Guardrails.authzGuard()`: 订单归属校验
3. **输出护栏** `Guardrails.piiMask()`: 手机号+身份证脱敏

## 与Python版差异

| 维度 | Python 版 | Java 版 |
|------|----------|---------|
| 数据库 | SQLite 单文件 | MySQL 8.0 (外键+JSON类型) |
| 并发 | threading (单进程) | Spring Boot (Tomcat线程池) |
| 负载均衡 | 无 | Nginx upstream + Spring LoadBalancer |
| Agent | MockLLM Python | Java 规则引擎 |
| 容器 | 单容器 | 8容器 (主应用可水平扩缩) |
| BPMN | xml.etree 解析执行 | StAX 解析执行 |
| RAG | numpy n-gram 向量 | 纯Java n-gram TF向量 + SQL回退 |
| 服务通信 | requests HTTP | Spring Cloud OpenFeign + Resilience4j |
| 构建 | 无 | Maven 多模块 (6模块) |
| 测试 | evaluate.py (LLM-as-judge) | JUnit 5 (19测试) + 规则判断器 (8/8=100%) |
| 服务层 | 直接耦合在控制器 | @Service层 + @FeignClient + 依赖注入 |
| 会话 | dict 单机内存 | SessionStore接口 (支持Redis) |
| CI/CD | 无 | Jenkins Pipeline (9阶段) |
| K8s | 基础部署 (Python版) | 完整部署 (ConfigMap+Secret+HPA) |
| 监控 | 无 | Actuator + SLA质量评价 (SlaRecorder) |
| 全局异常 | 无 | GlobalExceptionHandler |
| 请求日志 | 无 | RequestLoggingFilter |
| API文档 | 无 | Springdoc OpenAPI (Swagger UI) |
| 页面路由 | Python server路由 | @GetMapping (/,/merchant,/rider,/chat) |

---

## 近期更新 (2026-07-07)

### 新增监控基础设施 (代码+配置)
- `monitoring/prometheus/prometheus.yml` — 2 scrape job (campus-server + prometheus)
- `monitoring/grafana/datasources/prometheus.yml` — Grafana 数据源
- `monitoring/grafana/dashboards/campus-sla-dashboard.json` — 11 面板 SLA 仪表盘
- `monitoring/grafana/dashboards/provider.yml` — 仪表盘自动加载
- `monitoring/docker-compose.monitoring.yml` — Prometheus+Grafana 编排

### 代码修改
- `campus-server/pom.xml`: 新增 `micrometer-registry-prometheus`

### CI/CD 修改
- `Jenkinsfile`: 精简为 8 阶段，Evaluation/Docker 阶段在 Jenkins Docker 环境中跳过（nginx volume 挂载兼容问题），Markdown/WORD 报告中已移除 ELK/Zipkin 引用

### 报告产出

### 当前状态
- ✅ 全 10 容器运行中（Nginx+Server×2+3 微服务+MySQL+Redis+Prometheus+Grafana）
- ✅ Docker Hub 拉取已通过 `docker.m.daocloud.io` 镜像源解决
- ✅ Jenkins `campus-assistant-java` 构建成功 (#20)
- ✅ Jenkins `campus-assistant` 构建成功 (#1)
