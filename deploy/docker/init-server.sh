#!/usr/bin/env bash
# 服务器初始化：swap + 目录。用 root 执行。
# 位置：/opt/project/init-server.sh
set -euo pipefail

if ! swapon --show 2>/dev/null | grep -q .; then
    if [ ! -f /swapfile ]; then
        echo "==> 创建 2G swap"
        fallocate -l 2G /swapfile 2>/dev/null || dd if=/dev/zero of=/swapfile bs=1M count=2048
        chmod 600 /swapfile
        mkswap /swapfile
    fi
    swapon /swapfile
    grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
else
    echo "==> 已存在 swap，跳过"
fi

mkdir -p /opt/project/backend/config
mkdir -p /opt/project/backup /opt/project/sql /opt/project/logs /opt/project/files /opt/project/jar
mkdir -p /opt/project/docker/nginx/conf.d
mkdir -p /opt/project/docker/nginx/snippets
mkdir -p /opt/project/docker/nginx/ssl
mkdir -p /opt/project/docker/nginx/logs
mkdir -p /opt/project/docker/nginx/html/health-pc
mkdir -p /opt/project/docker/nginx/html/health-app
mkdir -p /opt/project/docker/nginx/html/download

chmod 755 /opt/project
chmod 700 /opt/project/backend

echo "==> 完成"
free -h
df -h /opt/project
