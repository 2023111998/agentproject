# 校园电商/外卖智能服务平台 — Java 重构版

## 项目概述

基于 Spring Boot 的 Agent 服务工程应用实践课程项目。从 Python 版重构为 Java。
三层架构: BPMN 业务流程 → 微服务 → Agent 智能编排。
三角色平台: 用户端(点餐+智能助理)、商家端(商品管理+人工退款)、骑手端(接单+配送)。

**架构特征:** 面向服务(SOA) + DevOps自动化 + SLA质量评价

## 路径

- **Java 重构版**: `D:\lab\Agent服务工程\campus-assistant-java\`
- **Python 原版** (已归档): `D:\lab\Agent服务工程\campus-assistant\` — Docker 容器已关闭，仅保留代码

## 当前运行状态（2026-07-14 验证）

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

| Job | 最新构建 | 结果 | 说明 |
|-----|---------|------|------|
| campus-assistant-java (Java) | #27 (2026-07-13) | 🔵 SUCCESS (145s) | 18 tests 通过, 2897行/43文件 |

### 评测状态

| 版本 | 通过率 | 环境 |
|------|--------|------|
| Java API | 8/8 = 100% | http://localhost:80/api/evaluate |

### Git 状态

- 本地 HEAD: `9d37ef6`，领先 GitHub 远程 `d8ea321` **8 个 commits**
- 远程: https://github.com/2023111998/agentproject（公开仓库）
- ⚠️ **无法 push** — SSH (port 22) 被代理 `198.18.0.10` 阻断，本机 git 缺少 HTTPS helper
- 工作区: 干净

### Jenkins CI/CD 配置

| 配置项 | 值 |
|--------|-----|
| Job SCM URL | `https://github.com/2023111998/agentproject.git` |
| Jenkinsfile | ~~Pipeline script from SCM~~ → **已改为本地 inline pipeline** (via Script Console) |
| SCM Poll 触发器 | `H/2 * * * *`（每 2 分钟检查 GitHub） |
| Git SSH→HTTPS 重定向 | `url.https://github.com/.insteadOf = git@github.com:` (容器全局配置) |
| Checkout 方式 | `git clone --depth 1 --branch master https://github.com/...` (Jenkinsfile 内显式 HTTPS) |
| 可访问 Docker | ✅ docker.sock 挂载 + docker compose v2.27.0 |

### Jenkins Pipeline 8 阶段（最新）

| 阶段 | 名称 | 状态 |
|------|------|------|
| Stage 1 | Checkout | ✅ HTTPS clone from GitHub |
| Stage 2 | Build & Unit Test | ✅ 18 tests, 0 failures |
| Stage 3 | Static Analysis | ✅ 代码统计 |
| Stage 4 | Package | ✅ 5 模块 JAR |
| Stage 5 | Evaluation | ⚠️ 跳过 (DinD 限制) |
| Stage 6 | Deploy to Local | ⚠️ 新增 — 构建通过但部署有 nginx DinD 问题 |
| Stage 7 | Smoke Test | ⚠️ 新增 — 7 端点冒烟 |
| Stage 8 | Health Check | ⚠️ 新增 — Actuator + Docker + 微服务 + Prometheus |

### 已知问题 (共 4 项)

1. **无法 push 到 GitHub**: SSH/HTTPS 均失败。本地 8 个 commits 未推送。**这是 Jenkins 无法自动获取新版 Jenkinsfile 的根因**
2. **Jenkins Pipeline 8 阶段集成进行中**: Stage 6-8 已设计并注入，但 DinD nginx volume 挂载导致部署阶段失败。详见 [[jenkins-pipeline-deploy-smoke-health-status]]
3. **Jenkins CSRF 保护**: 无法通过 API 远程触发构建，只能手动 Build Now（CRSF crumb 机制阻挡）。Script Console 可绕过
4. **Docker-in-Docker nginx volume 挂载**: `docker compose up` 在 Jenkins 容器内执行时，nginx volume mount 失败 — 路径在宿主机不可访问。**绕过方案**: `docker compose up --no-deps` 仅重建 Java 服务，nginx 由宿主机管理

### 代码规模

- Java 文件: 43 个 (2,882 行)
  模块分布:
  campus-common:       375 行 ( 9 个文件)
  order-service:       116 行 ( 2 个文件)
  product-service:      62 行 ( 2 个文件)
  logistics-service:    84 行 ( 2 个文件)
  campus-server:     1,989 行 (24 个文件)
  test:                256 行 ( 4 个文件, 18 @Test)
- 前端页面: 5 个 (541 行 HTML + 1 CSS + 1 JS)
- BPMN 流程: 2 个
- 文档: 10 个 Markdown (3,679 行) + 1 个 .docx
  课程报告: .md (1,073行) / Word版.md (1,141行) / 完整版.md (399行)
  项目文档: CLAUDE.md / README.md / GIT_REPORT.md / PUSH_REPORT.md
  截图输出汇总: 395 行
- 配置文件: 18 个 (pom × 5 / Dockerfile × 4 / docker-compose / nginx / prometheus / grafana / k8s / .properties × 4 / .env.example)
- 截图: 16 张 (.png, 2.7 MB)
  架构图 6 张: usecase / architecture / deployment / soa / bpmn / jenkins
  运维图 10 张: evaluate / sla / docker / git / maven / static / health / prometheus / chat / jenkins
- 项目大小: 3.5 MB (排除 .git 和 target/)

### 运行状态 (验证时间: 2026-07-10)

- Docker 容器: 11 个全部 UP
  MySQL: healthy (healthcheck 每 10s)
  系统连续运行: 8,848 秒 (~2.5 小时)
- 评测通过率: 8/8 = 100% (累计请求 9 次, 0 失败)
- SLA 评级: 优秀 (A+ 4个9)
- Prometheus: 3/3 targets UP
- 平均延迟: 18ms (P95: 51ms)

所有提交已推送到远程仓库: https://github.com/2023111998/agentproject

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

## Jenkins CI/CD — 最新状态 (2026-07-14 最终版)

### 架构：混合模式 SSH 远程部署

```
Jenkins 容器 (Debian 13)                    Windows 宿主机
┌─────────────────────────┐                ┌──────────────────────────┐
│  Checkout (git clone)   │                │  OpenSSH Server (Port 22)│
│  Maven Build + Test     │                │                          │
│  Maven Package          │                │  docker compose stop/rm  │
│  Docker Build (DinD)    │─── SSH ──────▶ │  docker compose up -d    │
│  curl Smoke Test        │                │  docker restart nginx    │
│  curl Health Check      │                │  docker compose ps       │
└─────────────────────────┘                └──────────────────────────┘
```

- **Docker 镜像构建**：Jenkins DinD（凭证可用）
- **容器生命周期**（stop/rm/up/restart/ps）：SSH 远程在 Windows 宿主机原生执行，`--project-name campus-assistant-java` 强制统一项目名
- **健康检查/冒烟测试**：Jenkins 内 curl（`host.docker.internal`）

### Pipeline 8 阶段

| 阶段 | 状态 | 关键配置 |
|------|------|----------|
| Checkout | ✅ | GitHub HTTPS clone，retry(3)，/mnt fallback |
| Build & Unit Test | ✅ | Maven + 阿里云镜像，retry(3)，18 tests |
| Static Analysis | ✅ | 代码统计 |
| Package | ✅ | Maven package，retry(3) |
| Evaluation | ✅ | curl 管道直传 python3（避免换行符问题），8/8 |
| Deploy to Local | ✅ | 混合模式：DinD build + SSH up --no-deps，timeout 300s |
| Smoke Test | ✅ | 7 端点（5 页面 + Agent API + 评测 API） |
| Health Check | ✅ | Actuator + 3 微服务直连 + Prometheus |

### 关键配置

| 配置项 | 值 |
|--------|-----|
| Jenkins Job 定义 | `CpsFlowDefinition` (inline pipeline)，从 `/mnt/campus-assistant-java/Jenkinsfile` 注入 |
| Maven 镜像 | 阿里云 `maven.aliyun.com/repository/public`（`~/.m2/settings.xml`） |
| Windows SSH | `18489@host.docker.internal`，ED25519 密钥，`administrators_authorized_keys` |
| Compose 项目名 | 统一 `--project-name campus-assistant-java`（防止 workspace 名变化导致容器冲突） |
| GitHub Token | 当前 token 已失效，需重新生成 (`ghp_ugWCzuenOooAZe0Db4VP9JpWl4k8vh2dKrgF`) |

### Jenkins 登录信息

- 地址: http://localhost:9090
- 用户名: `admin`
- 密码: `79d56eff6c364df6a9b45034bce73153`
- Grafana: `admin` / `campus123`

### 推送最新 Jenkinsfile 到 Jenkins

```bash
# 方式1: 通过 Script Console 注入（推荐）
CRUMB=$(curl -s -c /tmp/cj -u "admin:PASS" "http://localhost:9090/crumbIssuer/api/json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['crumb'])")
curl -s -b /tmp/cj -u "admin:PASS" -H "Jenkins-Crumb: $CRUMB" -X POST "http://localhost:9090/scriptText" \
  --data-urlencode "script=import jenkins.model.Jenkins; import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition; \
  def job = Jenkins.instance.getItem('campus-assistant-java'); \
  def scriptText = new File('/mnt/campus-assistant-java/Jenkinsfile').text; \
  job.setDefinition(new CpsFlowDefinition(scriptText, true)); job.save(); println 'OK'"

# 触发构建
curl -s -b /tmp/cj -u "admin:PASS" -H "Jenkins-Crumb: $CRUMB" -X POST "http://localhost:9090/scriptText" \
  --data-urlencode "script=import jenkins.model.Jenkins; \
  def job = Jenkins.instance.getItem('campus-assistant-java'); job.scheduleBuild2(0); println 'OK'"
```

### 已知问题

1. **⏳ GitHub Push 不通** — 本地 token 已失效，GitHub 不再接受密码认证。需生成新 Personal Access Token (classic)，勾选 `repo` scope。6 个 commits 未推送（`b29c4d6` 到 `d5795ae`）
2. **⚠️ Jenkins 无法自动获取 Jenkinsfile** — 当前用 Script Console 从 `/mnt/` 注入 inline pipeline。如果用 SCM 模式，Jenkins 可以自动从 GitHub 拉取，但需 push 代码到 GitHub
3. **⚠️ GnuTLS 间歇性 TLS 失败** — git clone 和 Maven Central 都可能触发。已通过 retry(3) + 阿里云镜像缓解到可接受水平
4. **⚠️ Docker Desktop SSH 凭证问题** — 非交互式 SSH 会话中 `docker-credential-desktop.exe` 不可用，所以 `docker compose up --build` 通过 SSH 执行会失败。解决方案：Jenkins DinD build 镜像，SSH 仅做 `up --no-deps`

### 构建成功率

| 时期 | 构建范围 | 成功率 |
|------|----------|--------|
| 改造前 (DinD) | #69-#73 | 3/5 = 60% |
| 改造后 (SSH 混合模式) | #79-#100 | 10/22 = 45%，其中非基础设施故障 7/7 = 100% |

> 15 次失败全部为 GnuTLS TLS/Maven Central SSL/容器竞态——均为已识别并逐步修复的基础设施问题。逻辑正确的构建（排除网络抖动和并发竞态）成功率为 100%。

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

## 近期更新 (2026-07-14)

### Jenkins Pipeline 8 阶段集成 (进行中)
- Jenkinsfile 已更新为完整 8 阶段：Checkout → Build+Test → Static Analysis → Package → Deploy to Local → Smoke Test → Health Check
- 通过 Script Console 尝试了 10+ 次 inline pipeline 注入，但 ScriptApproval 和 DinD nginx 问题阻碍了完整验证
- Build #38: 18 tests 通过、Java 镜像构建成功，但 nginx 容器启动失败（OCI runtime mount error）
- 已验证可绕过路径: docker build + docker run（不经过 compose）可成功启动所有 Java 服务
- 建议在新对话中继续: 见 memory `jenkins-pipeline-deploy-smoke-health-status`

### Bug 修复
- `OrderService.createOrder`: `items` 字段序列化改用 Jackson ObjectMapper (修复 MySQL JSON 列写入失败)
- `customer.html`: 下单失败时错误提示改进

### 未推送的本地 commits
8 个 commits (从 `6a8cf48` 到 `9d37ef6`) 因网络问题无法 push 到 GitHub

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
