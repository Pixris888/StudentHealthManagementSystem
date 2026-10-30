#!/usr/bin/env bash
# 启动 Nginx 容器。用 root 执行。
# 位置：/opt/project/start-nginx.sh
set -euo pipefail

NGINX_DIR=/opt/project/docker/nginx
CONF_DIR="$NGINX_DIR/conf.d"
SNIPPET_DIR="$NGINX_DIR/snippets"
HTML_DIR="$NGINX_DIR/html"

[ -f "$CONF_DIR/health.conf" ] || { echo "缺少 $CONF_DIR/health.conf"; exit 1; }
[ -f "$SNIPPET_DIR/health-api.conf" ] || { echo "缺少 $SNIPPET_DIR/health-api.conf"; exit 1; }
[ -f "$HTML_DIR/health-pc/index.html" ] || { echo "PC 前端还没上传：$HTML_DIR/health-pc/index.html"; exit 1; }
[ -f "$HTML_DIR/health-app/index.html" ] || { echo "App H5 还没上传：$HTML_DIR/health-app/index.html"; exit 1; }

MOUNT=""
if command -v getenforce >/dev/null 2>&1 && [ "$(getenforce)" = "Enforcing" ]; then
    MOUNT=",z"
    echo "==> SELinux Enforcing，挂载加 z 标签"
fi

echo "==> 拉取 Nginx 镜像"
docker pull nginx:1.26

echo "==> 检查 Nginx 配置"
docker run --rm \
    -v "$CONF_DIR:/etc/nginx/conf.d:ro${MOUNT}" \
    -v "$SNIPPET_DIR:/etc/nginx/snippets:ro${MOUNT}" \
    -v "$HTML_DIR:/var/www:ro${MOUNT}" \
    -v "$NGINX_DIR/ssl:/etc/nginx/ssl:ro${MOUNT}" \
    nginx:1.26 nginx -t

echo "==> 重建 health-nginx 容器"
docker rm -f health-nginx >/dev/null 2>&1 || true

docker run -d \
    --name health-nginx \
    --network host \
    --restart unless-stopped \
    -e TZ=Asia/Shanghai \
    -v "$CONF_DIR:/etc/nginx/conf.d:ro${MOUNT}" \
    -v "$SNIPPET_DIR:/etc/nginx/snippets:ro${MOUNT}" \
    -v "$HTML_DIR:/var/www:ro${MOUNT}" \
    -v "$NGINX_DIR/ssl:/etc/nginx/ssl:ro${MOUNT}" \
    -v "$NGINX_DIR/logs:/var/log/nginx${MOUNT}" \
    nginx:1.26

sleep 2
docker ps --filter name=health-nginx
echo "==> 本机检查"
curl -sI http://127.0.0.1/ | head -1 || true
curl -sI http://127.0.0.1:8080/ | head -1 || true
