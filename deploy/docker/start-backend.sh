#!/usr/bin/env bash
# 启动健康平台后端容器。用 root 执行。
# 位置：/opt/project/start-backend.sh
set -euo pipefail

JAR=/opt/project/jar/health-web-mng-1.0.0.jar
ENV_FILE=/opt/project/backend/.env
CONF=/opt/project/backend/config/application-prod.yml

for f in "$JAR" "$ENV_FILE" "$CONF"; do
    [ -f "$f" ] || { echo "缺少文件：$f"; exit 1; }
done

# CentOS 7 如果 SELinux 是 Enforcing，绑定挂载需要 z 标签
MOUNT=""
if command -v getenforce >/dev/null 2>&1 && [ "$(getenforce)" = "Enforcing" ]; then
    MOUNT=",z"
    echo "==> SELinux Enforcing，挂载加 z 标签"
fi

chmod 600 "$ENV_FILE" || true
chmod 640 "$CONF" || true

echo "==> 拉取 JDK 17 镜像"
docker pull eclipse-temurin:17-jre

echo "==> 重建 health-backend 容器"
docker rm -f health-backend >/dev/null 2>&1 || true

docker run -d \
    --name health-backend \
    --network host \
    --restart unless-stopped \
    -e TZ=Asia/Shanghai \
    --env-file "$ENV_FILE" \
    -v "/opt/project/jar:/opt/project/jar:ro${MOUNT}" \
    -v "/opt/project/backend/config:/opt/project/backend/config:ro${MOUNT}" \
    -v "/opt/project/files:/opt/project/files${MOUNT}" \
    -v "/opt/project/backup:/opt/project/backup${MOUNT}" \
    -v "/opt/project/logs:/opt/project/logs${MOUNT}" \
    -w /opt/project/backend \
    eclipse-temurin:17-jre \
    java -Xms256m -Xmx512m -XX:MaxMetaspaceSize=192m \
        -jar "$JAR" --spring.profiles.active=prod

echo "==> 等待后端启动（最多 90 秒）"
for i in $(seq 1 45); do
    if curl -s -o /dev/null "http://127.0.0.1:8888/api/auth/login"; then
        echo "==> 8888 端口已就绪"
        break
    fi
    if ! docker ps --format '{{.Names}}' | grep -qx health-backend; then
        echo "==> 容器已退出，最近日志："
        docker logs --tail 80 health-backend || true
        exit 1
    fi
    sleep 2
done

docker ps --filter name=health-backend
echo "==> 最近日志"
docker logs --tail 30 health-backend || true
