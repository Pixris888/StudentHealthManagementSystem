import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

/** 联调时可通过 VITE_PROXY_TARGET 切换后端地址，默认 8888 */
const backendTarget = process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:8888'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: backendTarget, // SpringBoot 后端地址
        changeOrigin: true,
      },
      '/uploads': {
        target: backendTarget, // 打卡照片本地存储目录
        changeOrigin: true,
      },
    },
  },
})
