# 截图清单

## 已自动生成（6张架构图）

| 文件名 | 内容 | 对应报告位置 |
|--------|------|-------------|
| `usecase-diagram.png` | 系统用例图（用户/商家/骑手 + 智能助理） | 1.2.1 |
| `architecture-diagram.png` | 系统四层架构图（前端/应用/领域/基础设施） | 1.2.2 |
| `deployment-diagram.png` | K8s 部署架构图（含 Jenkins CI/CD） | 1.2.3 |
| `soa-architecture.png` | SOA 服务架构图（Feign + Resilience4j） | 1.3 |
| `bpmn-diagram.png` | BPMN 业务流程图（售后退款 + 物流地图） | 2.1 |
| `jenkins-pipeline-diagram.png` | Jenkins CI/CD 流水线（8阶段） | 4.1 |

> 重新生成: `python generate_diagrams.py`

---

## 需手动截取（8张运维截图）

| 编号 | 文件名 | 截图内容 | 截图方法 |
|------|--------|---------|---------|
| #1 | `prometheus-targets.png` | Prometheus Targets 页面，3个 scrape 全部 UP | http://localhost:9091/targets |
| #2 | `grafana-overview.png` | Grafana 仪表盘概览（4个Stat面板） | http://localhost:3000 (admin/campus123) |
| #3 | `grafana-service-metrics.png` | Grafana 服务层指标 | 同上，向下滚动 |
| #4 | `grafana-jvm-resources.png` | Grafana JVM 系统资源 | 同上，继续滚动 |
| #5 | `grafana-sla-table.png` | Grafana SLA 明细表 | 同上，最底部 |
| #6 | `docker-ps.png` | Docker 容器列表 | `docker ps` |
| #7 | `evaluate-result.png` | 离线评测 8/8 PASS | http://localhost:80/api/evaluate |
| #8 | `sla-report.png` | SLA Report API 响应 | http://localhost:80/api/sla/report |

## 可选补充（5张）

| 编号 | 文件名 | 截图内容 | 截图方法 |
|------|--------|---------|---------|
| B1 | `customer-app.png` | 用户端页面 | http://localhost:80 |
| B2 | `merchant-app.png` | 商家端页面 | http://localhost:80/merchant |
| B3 | `rider-app.png` | 骑手端页面 | http://localhost:80/rider |
| B4 | `docker-stats.png` | Docker 资源占用 | `docker stats --no-stream` |
| B5 | `swagger-api.png` | Swagger API 文档 | http://localhost:80/swagger-ui.html |

---

## 快速截图工具

- **Windows**: `Win + Shift + S`（矩形截图）
- **浏览器**: F12 → Ctrl+Shift+P → "Capture screenshot"
