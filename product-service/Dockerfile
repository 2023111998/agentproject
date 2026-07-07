# ============================================================================
# 校园电商/外卖智能服务平台 — 商品微服务 (product-service)
# 单阶段构建: JAR 已本地 Maven 打好
# ============================================================================
FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY product-service/target/*.jar app.jar

EXPOSE 8002
ENTRYPOINT ["java", "-jar", "app.jar"]
