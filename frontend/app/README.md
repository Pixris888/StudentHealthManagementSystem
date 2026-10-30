# 学生端 App

uni-app（Vue 3）学生端，支持 H5 和 Android。

## 环境要求

- Node.js 20.19+ 或 22.12+
- HBuilderX（仅 Android APK 云打包需要）
- 后端默认运行在 `http://127.0.0.1:8888`

## H5 开发

```bash
npm install
npm run dev:h5
```

H5 默认走 Vite 代理 `/api`、`/uploads`，可用 `VITE_PROXY_TARGET` 覆盖后端地址。

## H5 构建

```bash
npm run build:h5
```

产物：`dist/build/h5/`。

## Android 构建

```powershell
$env:VITE_API_BASE_URL="https://example.com/api"
npm run build:app
```

产物：`dist/build/app/`。然后用 HBuilderX 导入该目录并云打包。

> `src/manifest.json` 的 `appid` 是占位符 `__UNI__XXXXXXX`，请替换为你自己的 DCloud appid。

## 下载页

`deploy/download/` 是学生端下载页，部署到 Nginx 的 `download` 目录：

```text
/opt/project/docker/nginx/html/download/
├── index.html
├── logo-192.png
└── health-app.apk
```

APK 必须命名为 `health-app.apk`。PC 的二维码默认指向当前站点的 `/download/`。

## 目录

```text
src/
├── api/          # 接口封装
├── components/   # 通用组件
├── config/       # API 地址等配置
├── pages/        # 页面（登录、注册、首页、打卡、AI、知识、消息、我的）
├── stores/       # Pinia
├── types/        # TypeScript 类型
└── utils/        # 请求、鉴权、格式化、密码校验
```
