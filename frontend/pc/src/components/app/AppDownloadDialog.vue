<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Iphone, Link } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { isLocalHost, resolveAppDownloadUrl } from '@/utils/app-download'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})
const canvasRef = ref<HTMLCanvasElement | null>(null)
const qrError = ref('')
const downloadUrl = resolveAppDownloadUrl()
const showWarning = import.meta.env.DEV || isLocalHost()

async function renderQr(): Promise<void> {
  if (!canvasRef.value) return
  qrError.value = ''
  try {
    await QRCode.toCanvas(canvasRef.value, downloadUrl, {
      width: 220,
      margin: 1,
      errorCorrectionLevel: 'M',
      color: { dark: '#0f766e', light: '#ffffff' },
    })
  } catch {
    qrError.value = '二维码生成失败，请直接复制下方链接'
  }
}

watch(
  () => props.modelValue,
  async (value) => {
    if (!value) return
    await nextTick()
    await renderQr()
  },
  { immediate: true },
)

async function copyLink(): Promise<void> {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(downloadUrl)
    } else {
      const input = document.createElement('textarea')
      input.value = downloadUrl
      input.style.position = 'fixed'
      input.style.opacity = '0'
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
    }
    ElMessage.success('下载链接已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制链接')
  }
}

function openDownloadPage(): void {
  window.open(downloadUrl, '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="下载学生端 App"
    width="430px"
    append-to-body
  >
    <div class="download-body">
      <div class="download-head">
        <div class="download-icon">
          <el-icon :size="22"><Iphone /></el-icon>
        </div>
        <div>
          <p class="download-title">用手机扫码下载</p>
          <p class="download-desc">
            打开下载页后点击“下载 Android 安装包”；微信内请选择“在浏览器打开”。
          </p>
        </div>
      </div>

      <div class="qr-card">
        <canvas ref="canvasRef" class="qr-canvas" />
        <p v-if="qrError" class="qr-error">{{ qrError }}</p>
      </div>

      <div class="url-row">
        <el-icon class="url-icon"><Link /></el-icon>
        <span class="url-text" :title="downloadUrl">{{ downloadUrl }}</span>
        <el-button text type="primary" :icon="CopyDocument" @click="copyLink">
          复制
        </el-button>
      </div>

      <el-alert
        v-if="showWarning"
        class="download-warning"
        type="warning"
        :closable="false"
        show-icon
        title="当前是本机或开发地址"
        description="手机扫码可能无法访问，请通过公网 IP/域名打开 PC 页面，或设置 VITE_APP_DOWNLOAD_URL。"
      />

      <div class="download-actions">
        <el-button @click="visible = false">关闭</el-button>
        <el-button type="primary" @click="openDownloadPage">
          直接打开下载页
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.download-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.download-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.download-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  color: var(--hp-primary);
  background: var(--hp-mint);
  border-radius: 14px;
}

.download-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--hp-text);
}

.download-desc {
  margin-top: 4px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--hp-text-muted);
}

.qr-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 252px;
  padding: 16px;
  background: #f8fbfa;
  border: 1px solid var(--hp-border);
  border-radius: 16px;
}

.qr-canvas {
  display: block;
  width: 220px;
  height: 220px;
}

.qr-error {
  margin-top: 8px;
  font-size: 12px;
  color: var(--hp-danger);
}

.url-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px 8px 12px;
  background: #f8fbfa;
  border-radius: 10px;
}

.url-icon {
  flex-shrink: 0;
  color: var(--hp-text-muted);
}

.url-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-size: 12px;
  color: var(--hp-text-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.download-warning {
  margin: 0;
}

.download-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
