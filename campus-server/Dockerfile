# ============================================================================
# 校园电商/外卖智能服务平台 — 主应用 (campus-server)
# 单阶段构建: JAR 已本地 Maven 打好，直接 COPY 到 JRE 镜像
# 国内: docker build --file campus-server/Dockerfile -t campus-server .
# ============================================================================
FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY campus-server/target/*.jar app.jar

EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
