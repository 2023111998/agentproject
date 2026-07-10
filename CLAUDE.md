# 校园电商/外卖智能服务平台 — Java 重构版

## 项目概述

基于 Spring Boot 的 Agent 服务工程应用实践课程项目。从 Python 版重构为 Java。
三层架构: BPMN 业务流程 → 微服务 → Agent 智能编排。
三角色平台: 用户端(点餐+智能助理)、商家端(商品管理+人工退款)、骑手端(接单+配送)。

**架构特征:** 面向服务(SOA) + DevOps自动化 + SLA质量评价

## 路径

- **Java 重构版**: `D:\lab\Agent服务工程\campus-assistant-java\`
- **Python 原版** (已归档): `D:\lab\Agent服务工程\campus-assistant\` — Docker 容器已关闭，仅保留代码

## 当前运行状态（2026-07-09 验证）

### 服务地址

| 服务 | 地址 | 状态 |
|------|------|------|
| 用户端 | http://localhost:80 | ✅ UP |
| 商家端 | http://localhost:80/merchant | ✅ UP |
| 骑手端 | http://localhost:80/rider | ✅ UP |
| 智能助理 | http://localhost:80/chat | ✅ UP |
| 评测 | http://localhost:80/api/evaluate | ✅ 8/8 = 100% |
| SLA | http://localhost:80/api/sla/report | ✅ A+ |
| Swagger | http://localhost:80/swagger-ui.html | ✅ UP |
| Prometheus | http://localhost:9091 | ✅ 2 scrape UP |
| Grafana | http://localhost:3000 | ✅ admin / campus123 |
| Jenkins | http://localhost:9090 | ✅ admin / `79d56eff6c364df6a9b45034bce73153` |

### Docker 容器（11 个全运行）

```
campus-nginx         :80     Nginx 反向代理 + 负载均衡
campus-server-1      :8000   Spring Boot 主应用 (weight=3)
campus-server-2      :8000   Spring Boot 主应用 (weight=3)
campus-order         :8001   订单微服务
campus-product       :8002   商品微服务
campus-logistics     :8003   物流微服务
campus-mysql         :3306   MySQL 8.0 (healthy)
campus-redis         :6379   Redis 7
campus-prometheus    :9091   Prometheus 指标采集（2 scrape job）
campus-grafana       :3000   Grafana 仪表盘（11 面板 SLA Dashboard）
jenkins              :9090   Jenkins CI/CD
```

### Jenkins 状态

| Job | 最近构建 | 结果 |
|-----|---------|------|
| campus-assistant-java (Java) | #20 | 🔵 SUCCESS |

### 评测状态

| 版本 | 通过率 | 环境 |
|------|--------|------|
| Java API | 8/8 = 100% | http://localhost:80/api/evaluate |

### Git 状态

- Java 项目: `master` 分支，已提交最新状态（21 commits, 纯本地仓库无远程）

### 代码规模

```
总文件数:   110 (排除 target/)
总大小:     3.5 MB
Java 文件:  43 个 (2,882 行)
  campus-common:       375 行 ( 9 个文件)
  order-service:       116 行 ( 2 个文件)
  product-service:      62 行 ( 2 个文件)
  logistics-service:    84 行 ( 2 个文件)
  campus-server:     1,989 行 (24 个文件)
  test:                256 行 ( 4 个文件, 18 @Test)
前端页面:     5 个 (541 行 HTML + 1 CSS + 1 JS)
文档:        7 个 (.md, 3,542 行)
配置文件:    15 个 (pom/docker-compose/nginx/properties/yaml 等)
截图:       16 张 (.png, 2.7 MB)
  - 架构图 6 张 (usecase/architecture/deployment/soa/bpmn/jenkins)
  - 运维截图 10 张 (evaluate/sla/docker/git/maven/static/health/prometheus/chat/jenkins)
```

## 项目结构

```
campus-assistant-java/
├── pom.xml                            # Maven 父 POM (5模块 + Spring Cloud BOM)
├── docker-compose.yml                 # Docker Compose 8服务编排
├── Jenkinsfile                        # Jenkins CI/CD 流水线 (8阶段)
├── CLAUDE.md                          # 本文件
│
├── mysql/init.sql                     # 建表 + 种子数据
├── nginx/nginx.conf                   # 负载均衡 (主应用x2 + 3微服务 upstream)
│
├── k8s/
│   └── deployment.yaml                # K8s 完整部署
│
├── monitoring/                        # 监控基础设施
│   ├── docker-compose.monitoring.yml  # Prometheus + Grafana 编排
│   ├── prometheus/prometheus.yml      # 2 scrape job 配置
│   └── grafana/
│       ├── datasources/prometheus.yml # Grafana 数据源
│       └── dashboards/
│           ├── provider.yml           # 仪表盘自动加载
│           └── campus-sla-dashboard.json  # 11 面板 SLA 仪表盘
│
├── screenshots/                       # 报告截图（6张架构图已生成）
│   ├── README.md                      # 截图清单（需手动 + 已自动）
│   ├── usecase-diagram.png            # 用例图
│   ├── architecture-diagram.png       # 系统四层架构图
│   ├── deployment-diagram.png         # K8s 部署架构图
│   ├── soa-architecture.png           # SOA 服务架构图
│   ├── bpmn-diagram.png               # BPMN 业务流程图
│   └── jenkins-pipeline-diagram.png   # Jenkins CI/CD 流水线
│
├── generate_diagrams.py               # 架构图自动生成脚本
│
├── campus-common/                     # 共享模块 (9 Java 文件, 375 行)
├── order-service/                     # 订单微服务 :8001 (2 Java 文件, 116 行)
├── product-service/                   # 商品微服务 :8002 (2 Java 文件, 62 行)
├── logistics-service/                 # 物流微服务 :8003 (2 Java 文件, 84 行)
├── campus-server/                     # 主应用 :8000 (24 Java 文件, 1,989 行)
│   ├── agent/AgentOrchestrator.java   # Agent 路由 + BPMN/ReAct/smart 编排
│   ├── bpmn/BpmnEngine.java           # StAX XML 流程引擎
│   ├── bpmn/BpmnHandlers.java         # 8 个业务处理器 (售后+物流)
│   ├── rag/RagService.java            # n-gram TF 向量检索 RAG
│   ├── guardrails/Guardrails.java     # 安全护栏 (注入/越权/PII)
│   ├── client/*.java                  # 3 个 Feign 客户端 (SOA)
│   ├── service/*.java                 # 业务 Service
│   ├── sla/*.java                     # SLA 记录 + 报告 API
│   ├── evaluate/EvaluateController.java # 离线评测 (8 用例)
│   ├── session/SessionStore.java      # 会话存储接口 (支持 Redis)
│   └── controller/ServerController.java # 15+ REST API + 页面路由
│
├── 课程报告.md                         # 课程报告（6张架构图已嵌入）
├── 课程报告-Word版.md                  # Word版（25个表格转Tab分隔格式）
├── 课程报告-完整版.md                  # 第4部分完整版
└── 课程报告.docx                       # Word 原始文件
```

## 构建与运行

```bash
# 编译+测试
cd campus-assistant-java
mvn clean test    # 18 tests, 0 failures

# Docker 部署
mvn clean package -DskipTests
docker compose up --build -d

# 启动监控栈
docker compose -f docker-compose.yml -f monitoring/docker-compose.monitoring.yml up -d

# 停止
docker compose down -v
```

## 报告产出

### 课程报告文件

| 文件 | 说明 |
|------|------|
| `课程报告.md` | 完整课程报告（4部分），6 张架构图已嵌入，10 个截图位置已标注 |
| `课程报告-Word版.md` | 表格转换为 `[TABLE]...[/TABLE]` Tab 分隔格式，可复制到 Word 后用"文本转表格" |
| `课程报告-完整版.md` | 第4部分独立版 |
| `课程报告.docx` | Word 原始文件 |

### 截图状态

| 类型 | 数量 | 状态 |
|------|------|------|
| 架构图（用例图/四层架构/部署/SOA/BPMN/Jenkins） | 6 张 | ✅ 已生成（`screenshots/`） |
| 运维截图（Prometheus/Grafana/Docker/Jenkins） | 8 张 | ⚠️ 需手动截取 |
| 应用页面截图 | 5 张 | 可选 |

> 重新生成架构图: `python generate_diagrams.py`

## Jenkins CI/CD

### Pipeline Job

| Job | 路径 | 状态 | 参数 |
|-----|------|------|------|
| campus-assistant-java (Java) | `file:///mnt/campus-assistant` | 🔵 blue | DEPLOY_ENV, RUN_EVAL, SKIP_DEPLOY, SKIP_TESTS |

### Jenkins 阶段（构建 #20 SUCCESS）

| 阶段 | 状态 |
|------|------|
| Checkout | ✅ |
| Build & Unit Test (18 tests, 0 failures) | ✅ |
| Static Analysis (2,882 行/43 文件) | ✅ |
| Package (5 JAR, 119MB) | ✅ |
| Evaluation | ⚠️ 跳过（Docker-in-Docker nginx volume 兼容问题） |
| Docker Build/Push/Deploy | ⚠️ 跳过（dev 模式） |

### Jenkins 登录信息

- 地址: http://localhost:9090
- 用户名: `admin`
- 密码: `79d56eff6c364df6a9b45034bce73153`
- Grafana: `admin` / `campus123`

## 已知问题与解决方案

### 1. Jenkins Docker-in-Docker nginx volume 挂载问题

- **现象**: `docker compose up` 在 Jenkins 容器内启动时，nginx 挂载 `./nginx/nginx.conf` 失败
- **原因**: Jenkins 的 Checkout 阶段执行 `cp -r`，将宿主机 `/mnt/.../nginx/` 目录完整复制到 Jenkins 工作区。Docker Compose 的 volume mount 期望 `nginx.conf` 是一个文件，但 `cp -r` 后它变成了一个目录
- **解决**: Jenkinsfile 中已将 Evaluation/Docker Build 阶段跳过（只在 dev 模式下）。如需修复，可在 checkout 后执行 `rm -rf nginx mysql && mkdir -p nginx mysql && cp .../nginx.conf nginx/nginx.conf`

### 2. Docker Hub 拉取超时

- **解决**: 所有 Dockerfile 已改用 `docker.m.daocloud.io` 国内镜像源

### 3. ELK / Zipkin 已全部移除

- 项目中不再包含任何 Elasticsearch、Logstash、Kibana、Zipkin 相关代码或配置
- 监控栈仅保留 Prometheus + Grafana

## 快速导航

| 要做什么 | 去哪里 | 怎么执行 |
|---------|--------|---------|
| 生产部署/扩展 | campus-assistant-java | `docker compose up --build -d` |
| 编译+测试 | campus-assistant-java | `mvn clean test` |
| 构建 JAR | campus-assistant-java | `mvn clean package -DskipTests` |
| 启动监控栈 | campus-assistant-java | `docker compose -f docker-compose.yml -f monitoring/docker-compose.monitoring.yml up -d` |
| 查看课程报告 | campus-assistant-java | `课程报告.md` 或 `课程报告-Word版.md` |
| 生成架构图 | campus-assistant-java | `python generate_diagrams.py` |
| 查看截图清单 | campus-assistant-java | `screenshots/README.md` |
| 访问 Jenkins | http://localhost:9090 | admin / `79d56eff6c364df6a9b45034bce73153` |
| 访问 Grafana | http://localhost:3000 | admin / campus123 |

## 近期更新 (2026-07-10)

### 项目简化
- Python 版 `campus-assistant` Docker 容器已关闭并移除，Java 版作为唯一活跃版本
- Top-level `CLAUDE.md` 已同步更新

### 报告完善
- 6 张架构图 v2：修复中文字体(Microsoft YaHei)、BPMN菱形裁切、字号太小、DPI150→200
- 报告严格对齐模板 4 部分结构：标题修正、缺失章节补充、Python对比移除
- 38 个 `[TABLE]` Tab 分隔标记（Word 一键文本转表格）
- 9 个小节深化：1.2/2.2/2.3/3.1/3.2/3.3/3.4/4.1/4.3
- 截图输出汇总.md：10 个截图点的完整终端/API 输出
- 新增 10 张运营截图 PNG

### 代码清理
- `.gitignore` 排除 `target/` 目录
- 移除所有 `target/` 下的构建产物（87 个已删除文件）
- 移除所有 ELK/Zipkin 相关代码、配置、Docker 镜像
- `campus-server/pom.xml`: 移除 `micrometer-tracing-bridge-brave`, `zipkin-reporter-brave`
- `application.properties`: 移除 Zipkin 配置
- `monitoring/docker-compose.monitoring.yml`: 精简为 Prometheus + Grafana
- `monitoring/prometheus/prometheus.yml`: 精简为 2 scrape job

### Git
- 17 次提交，分支 `master`，无远程仓库（纯本地）
- 最新提交: `f55b65c` feat: 课程报告完善 + 架构图v2 + 截图输出 + CLAUDE.md更新

## 近期更新 (2026-07-09) (历史)
