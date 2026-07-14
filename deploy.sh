#!/bin/bash
# ============================================================================
# 校园电商/外卖智能服务平台 — 远程部署脚本
# 由 Jenkins SSH 调用，在 Windows 宿主机原生执行 (通过 Git Bash)
# 用法: bash deploy.sh
# ============================================================================
set -e

PROJECT_DIR="D:/lab/Agent服务工程/campus-assistant-java"
COMPOSE="docker compose --project-name campus-assistant-java"

cd "$PROJECT_DIR" || { echo "ERROR: 无法进入项目目录 $PROJECT_DIR"; exit 1; }

echo "=== [1/5] 停止旧的 Java 微服务 ==="
$COMPOSE stop campus-server-1 campus-server-2 order-service product-service logistics-service 2>/dev/null || true
$COMPOSE rm -f campus-server-1 campus-server-2 order-service product-service logistics-service 2>/dev/null || true
echo "旧容器已清理"

echo ""
echo "=== [2/5] 启动 Java 微服务（不构建镜像） ==="
$COMPOSE up -d --no-deps campus-server-1 campus-server-2 order-service product-service logistics-service
echo "Java 微服务已启动"

echo ""
echo "=== [3/5] 等待服务就绪 (最长 120s) ==="
for i in $(seq 1 24); do
    if curl -sf http://localhost:8000/actuator/health > /dev/null 2>&1; then
        echo "Java 服务就绪 ($((i*5))s)"
        break
    fi
    if [ $i -eq 24 ]; then
        echo "ERROR: 服务启动超时 (120s)"
        exit 1
    fi
    sleep 5
done

echo ""
echo "=== [4/5] 重新加载 nginx ==="
docker restart campus-nginx
sleep 3
echo "nginx 已重启"

echo ""
echo "=== [5/5] 容器运行状态 ==="
echo ""
$COMPOSE ps
echo ""
echo "=== 部署完成 ==="
