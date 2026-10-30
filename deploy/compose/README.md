# Docker Compose 一键部署

目标：把原来手工执行的 `docker run` 命令收敛成一份 Compose 文件，服务器上只需要执行 `docker compose up -d`。

Compose 只负责运行时编排，**不在服务器上编译**：后端 jar、PC dist、App H5 dist 仍在本地构建后上传。这样 2G 内存的服务器不需要安装 JDK/Maven/Node。

## 1. 服务器目录

把 `deploy/compose/` 整个目录上传到服务器的 `/opt/project/compose/`，最终目录如下：

```text
/opt/project/
├── compose/
│   ├── docker-compose.yml
│   ├── .env                  # 由 .env.example 复制并填写，chmod 600
│   └── nginx/
│       ├── conf.d/health.conf
│       └── snippets/health-api.conf
├── jar/
│   └── health-web-mng-1.0.0.jar
├── backend/
│   ├── .env                  # 旧版文件，可保留；Compose 使用 compose/.env
│   └── config/application-prod.yml
├── docker/
│   ├── mysql/data/           # MySQL 数据目录（沿用旧目录）
│   ├── redis/data/           # Redis AOF 数据目录
│   └── nginx/
│       ├── html/
│       │   ├── health-pc/    # PC dist
│       │   ├── health-app/   # App H5 dist
│       │   └── download/     # 下载页 + health-app.apk
│       ├── ssl/
│       └── logs/
├── files/                    # 上传的打卡照片
├── backup/                   # 数据备份
├── logs/                     # 后端日志
└── sql/
    ├── schema.sql            # 完整表结构（首次初始化使用）
    └── demo-data.sql         # 最小演示数据
```

## 2. 前置条件

- Linux 服务器，已安装 Docker 20.10+ 和 Docker Compose v2（`docker compose version`）
- 已放行安全组/防火墙：`22`、`80`、`8080`
- 不要放行 `3306`、`6379`、`8888`；Compose 也不会把它们发布到公网

如果没有 Compose v2：

```bash
# 以实际发行版安装方式为准；Docker CE 一般安装 docker-compose-plugin
docker compose version || sudo dnf install -y docker-compose-plugin
```

## 3. 首次部署

### 3.1 放置构建产物

| 本机文件 | 服务器位置 |
| --- | --- |
| `health-web-mng/target/health-web-mng-1.0.0.jar` | `/opt/project/jar/health-web-mng-1.0.0.jar` |
| `health-pc/dist/*` | `/opt/project/docker/nginx/html/health-pc/` |
| `health-app/dist/build/h5/*` | `/opt/project/docker/nginx/html/health-app/` |
| `health-app/deploy/download/*` | `/opt/project/docker/nginx/html/download/` |
| `health-app/dist/build/app/unpackage/release/apk/*.apk` | `/opt/project/docker/nginx/html/download/health-app.apk` |
| `deploy/docker/application-prod.yml` | `/opt/project/backend/config/application-prod.yml` |
| `sql/schema.sql`、`sql/demo-data.sql` | `/opt/project/sql/` |
| `deploy/compose/*` | `/opt/project/compose/` |

> APK 必须重命名为 `health-app.apk`，下载页和 PC 扫码入口都按这个固定文件名访问；以后升级直接覆盖该文件即可，二维码和下载链接不用变。
>
> PC 的扫码下载默认使用当前站点同源的 `/download/`；如果下载页在其他域名或端口，构建 PC 时设置 `VITE_APP_DOWNLOAD_URL`。

创建并授权目录：

```bash
sudo mkdir -p /opt/project/{compose,jar,backend/config,docker/mysql/data,docker/redis/data}
sudo mkdir -p /opt/project/docker/nginx/{html/health-pc,html/health-app,html/download,ssl,logs}
sudo mkdir -p /opt/project/{files,backup,logs,sql}
sudo chmod 700 /opt/project/backend /opt/project/compose
```

### 3.2 配置环境变量

```bash
cd /opt/project/compose
cp .env.example .env
chmod 600 .env
vi .env
```

必须填写的值：

- `DB_PASSWORD`：MySQL 中 `health` 用户的密码
- `MYSQL_ROOT_PASSWORD`：仅在全新数据目录初始化时使用
- `REDIS_PASSWORD`：Redis 密码
- `JWT_SECRET`：执行 `openssl rand -hex 32` 生成
- `DASHSCOPE_API_KEY`：没有可以留空，AI 会走规则兜底

> `.env` 里含有真实密码，不要提交到 Git，也不要发到群里。
>
> 如果沿用已有的 MySQL 数据目录，`MYSQL_ROOT_PASSWORD` 不会修改现有 root 密码，但 Compose 解析时仍要求填写，可以填任意占位值；`DB_USERNAME`/`DB_PASSWORD` 必须与现有 `health` 用户一致。

### 3.3 全新数据库初始化

Compose 会把 `sql/schema.sql` 和 `sql/demo-data.sql` 挂载到 MySQL 的初始化目录。**只有 `docker/mysql/data` 为空时才会自动导入**；已有数据目录不会重复导入，也不会覆盖数据。

`schema.sql` 包含 23 张表的完整结构，`demo-data.sql` 只包含内置角色、菜单、字典、系统参数和一个超级管理员。如果不需要旧数据，保持 `docker/mysql/data` 为空即可；需要旧数据则先把旧数据目录迁移到该位置。

### 3.4 启动

```bash
cd /opt/project/compose
docker compose up -d
docker compose ps
docker compose logs -f backend
```

## 4. 从旧的手工容器迁移

当前服务器上可能已经有手工创建的 `mysql`/`mysql8`、`redis`、`health-backend`、`health-nginx`。迁移前先确认旧容器的镜像版本和数据目录，**不要直接删除数据目录**。

> **重要**：启动 Compose 前必须先停掉旧的 MySQL/Redis 容器。两个 MySQL 进程同时写同一个数据目录会损坏数据；先备份再迁移。

```bash
# 迁移前备份（在旧 MySQL 容器仍运行时执行；把 <root密码> 换成实际密码）
docker exec -i mysql8 mysqldump -uroot -p'<root密码>' \
  --default-character-set=utf8mb4 health_management \
  > /opt/project/backup/health_management-before-compose.sql
```

```bash
docker ps -a
docker inspect mysql8 --format '镜像={{.Config.Image}} 数据目录={{range .Mounts}}{{.Source}}->{{.Destination}} {{end}}'
```

确认 MySQL 是 8.0 且数据目录是 `/opt/project/docker/mysql/data` 后，停止并删除旧容器（只删容器，不删目录）：

```bash
docker rm -f health-backend health-nginx
docker stop mysql8 redis 2>/dev/null || true
docker rm mysql8 redis 2>/dev/null || true
```

如果旧容器名不是 `mysql8`/`redis`，按 `docker ps` 的实际名字替换。旧 MySQL 的数据目录内容会由新的 `health-mysql` 容器继续使用。

> MySQL 大版本必须匹配：8.0 的数据目录不能挂给 5.7，反之亦然。不确定时先 `mysqldump` 备份，再新建数据目录导入。

然后启动：

```bash
cd /opt/project/compose
docker compose up -d
docker compose ps
```

## 5. 日常更新

```bash
cd /opt/project/compose

# 只更新后端 jar：替换 jar 后重启后端
docker compose restart backend

# 更新前端静态文件：替换 dist 内容即可，Nginx 直接读取挂载目录
# 如果改了 nginx 配置，再执行：
docker compose restart nginx

# 修改了 compose/.env 或 application-prod.yml，重建后端
docker compose up -d --force-recreate backend

# 更新镜像（例如 MySQL/Redis/Nginx 补丁版本）
docker compose pull
docker compose up -d
```

> 后端重启后，如果 Nginx 出现 502，先确认 `docker compose ps` 中 backend 已启动；必要时执行 `docker compose restart nginx` 让 Nginx 重新解析后端地址。

## 6. 验证

```bash
curl -I http://127.0.0.1/
curl -I http://127.0.0.1:8080/
curl -I http://127.0.0.1/download/
curl -I http://127.0.0.1/download/health-app.apk
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8888/
```

访问入口：

- PC：`http://<公网IP>/`
- App H5：`http://<公网IP>:8080/`
- 下载页：`http://<公网IP>/download/`（PC 同源，扫码用这个）或 `http://<公网IP>:8080/download/`

## 7. 安全与排错

安全：

- 公网只放行 `22`、`80`、`8080`
- MySQL/Redis 没有 `ports`，只在 Compose 内网可达
- 后端 `8888` 只绑定 `127.0.0.1`
- `.env` 权限保持 `600`
- 数据库 dump 里含演示账号，部署后请立即修改管理员密码，并清理不需要的演示账号
- 不要把 `.env`、`application-prod.yml`、数据库 dump 上传到 Nginx 静态目录

排错：

```bash
docker compose ps
docker compose logs -f mysql
docker compose logs -f redis
docker compose logs -f backend
docker compose logs -f nginx
ss -lntp | grep -E ':80|:8080|:8888|:3306|:6379'
```

常见问题：

| 现象 | 处理 |
| --- | --- |
| `port is already allocated` | 旧容器还占着 80/8080/8888，先 `docker rm -f` 旧容器 |
| MySQL 启动后退出 | 看 `docker compose logs mysql`；常见是数据目录权限或大版本不匹配 |
| 后端连不上 MySQL | 确认 `.env` 中 `DB_PASSWORD` 与 `health` 用户密码一致；`docker compose logs backend` 查看连接串 |
| 后端 502 | `docker compose ps` 确认 backend 已启动；`docker compose restart nginx` |
| SQL 文件挂载后变成目录 | `schema.sql` / `demo-data.sql` 不存在时 Docker 会创建目录；先 `rmdir` 再上传文件 |
| SELinux 导致权限拒绝 | Compose 已给绑定挂载加 `:z`；仍有问题可测试 `setenforce 0`，再按需配置 SELinux 策略 |
| MySQL/Redis 数据目录权限 | 容器内用户通常是 UID 999：`sudo chown -R 999:999 /opt/project/docker/mysql/data /opt/project/docker/redis/data` |

清空数据库从头再来（**会删除全部数据**）：

```bash
cd /opt/project/compose
docker compose down
sudo rm -rf /opt/project/docker/mysql/data/*
docker compose up -d
```
