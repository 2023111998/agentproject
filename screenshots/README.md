# 截图清单

本目录用于存放课程报告所需的截图。请按以下清单逐一截取。

## 截图方法

- **Windows**: `Win + Shift + S`（矩形截图）
- **浏览器**: 按 F12 打开开发者工具 → Ctrl+Shift+P → "Capture screenshot"
- **终端**: 直接调整窗口大小后截图

所有服务必须先确保运行中：`docker compose up -d`（在 campus-assistant-java 目录下）

---

## 10 张必须截图

| 编号 | 文件名 | 截图内容 | 截图方法 |
|------|--------|---------|---------|
| #1 | `prometheus-targets.png` | Prometheus Targets 页面，3个 scrape 全部 UP | 打开 http://localhost:9091/targets |
| #2 | `grafana-overview.png` | Grafana 仪表盘概览（4个Stat面板） | 打开 http://localhost:3000（admin/campus123），进入 campus-sla-dashboard |
| #3 | `grafana-service-metrics.png` | Grafana 服务层指标（请求速率/延迟/状态码/断路器） | 同一仪表盘，向下滚动截取服务层 |
| #4 | `grafana-jvm-resources.png` | Grafana JVM系统资源（内存/GC/CPU） | 同一仪表盘，继续向下滚动 |
| #5 | `grafana-sla-table.png` | Grafana SLA 明细表 | 同一仪表盘，截取最底部的表格 |
| #6 | `jenkins-pipeline.png` | Jenkins Pipeline Stage View | 打开 http://localhost:9090（admin/初始密码），进入 campus-assistant-java 任务 |
| #7 | `maven-build-success.png` | Maven BUILD SUCCESS | 在项目目录执行 `mvn clean test`，截取 BUILD SUCCESS 输出 |
| #8 | `docker-ps.png` | Docker 容器运行列表 | 终端执行 `docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}` |
| #9 | `evaluate-result.png` | 离线评测 8/8 = 100% 通过 | 浏览器打开 http://localhost:80/api/evaluate |
| #10 | `sla-report.png` | SLA Report API JSON 响应 | 浏览器打开 http://localhost:80/api/sla/report |

## 可选补充截图

| 编号 | 文件名 | 截图内容 | 截图方法 |
|------|--------|---------|---------|
| B1 | `customer-app.png` | 用户端页面（选商家+购物车+智能助理） | http://localhost:80 |
| B2 | `merchant-app.png` | 商家端页面（订单管理+商品管理） | http://localhost:80/merchant |
| B3 | `rider-app.png` | 骑手端页面（接单+配送） | http://localhost:80/rider |
| B4 | `docker-stats.png` | Docker 资源占用统计 | 终端执行 `docker stats --no-stream` |
| B5 | `swagger-api.png` | Swagger API 文档 | http://localhost:80/swagger-ui.html |
