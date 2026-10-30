# PC 管理端

Vue 3 + TypeScript + Vite + Element Plus 管理端。

## 环境要求

- Node.js 20.19+ 或 22.12+
- 后端默认运行在 `http://127.0.0.1:8888`

## 开发

```bash
npm install
npm run dev
```

默认地址：`http://localhost:5173`。Vite 会把 `/api`、`/uploads` 代理到后端，可用 `VITE_PROXY_TARGET` 覆盖。

## 构建

```bash
npm run build
```

产物：`dist/`。部署时上传到 Nginx 的 `health-pc` 目录，并确保 history 路由有 `try_files $uri $uri/ /index.html`。

## 页面

- `/login` 登录
- `/register` 学生自助注册
- `/home` 首页
- 其余页面由后端菜单动态注册

## 学生端下载

登录页和顶部栏都有“下载学生端 App”入口：

- 桌面端弹出二维码
- 手机端直接跳下载页
- 默认地址：当前站点同源 `/download/`
- 需要其他地址时，复制 `.env.example` 为 `.env.local` 并设置：

```env
VITE_APP_DOWNLOAD_URL=https://example.com/download/
```

二维码在浏览器本地生成，不调用第三方二维码接口。

## 演示账号

`admin / 123456`，仅用于本地演示，部署后请立即修改。
