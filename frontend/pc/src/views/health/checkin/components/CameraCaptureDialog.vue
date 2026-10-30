<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Camera,
  Check,
  Refresh,
  Upload,
  VideoCamera,
} from '@element-plus/icons-vue'

const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ captured: [file: File] }>()

const videoRef = ref<HTMLVideoElement>()
const fileInputRef = ref<HTMLInputElement>()
const stream = ref<MediaStream | null>(null)
const facingMode = ref<'environment' | 'user'>('environment')
const cameraError = ref('')
const starting = ref(false)
const capturedFile = ref<File | null>(null)
const capturedUrl = ref('')

const hasCapture = computed(() => Boolean(capturedFile.value))

function stopStream(): void {
  stream.value?.getTracks().forEach((track) => track.stop())
  stream.value = null
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
}

function revokePreview(): void {
  if (capturedUrl.value) {
    URL.revokeObjectURL(capturedUrl.value)
    capturedUrl.value = ''
  }
}

async function startCamera(): Promise<void> {
  cameraError.value = ''
  if (!window.isSecureContext) {
    cameraError.value = '摄像头需要 HTTPS 或 localhost 环境，请改用文件上传'
    return
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    cameraError.value = '当前浏览器不支持摄像头，请改用文件上传'
    return
  }

  stopStream()
  starting.value = true
  try {
    const media = await navigator.mediaDevices.getUserMedia({
      video: {
        facingMode: facingMode.value,
        width: { ideal: 1280 },
        height: { ideal: 960 },
      },
      audio: false,
    })
    // 用户可能在授权期间关闭弹窗或改用文件上传
    if (!visible.value || capturedFile.value) {
      media.getTracks().forEach((track) => track.stop())
      return
    }
    stream.value = media
    if (videoRef.value) {
      videoRef.value.srcObject = media
      await videoRef.value.play().catch(() => undefined)
    }
  } catch (error) {
    const name = (error as DOMException)?.name
    if (name === 'NotAllowedError') {
      cameraError.value = '摄像头权限被拒绝，请允许权限或改用文件上传'
    } else if (name === 'NotFoundError') {
      cameraError.value = '未找到可用摄像头，请改用文件上传'
    } else {
      cameraError.value = '无法启动摄像头，请改用文件上传'
    }
  } finally {
    starting.value = false
  }
}

function capture(): void {
  const video = videoRef.value
  if (!video || !video.videoWidth || !video.videoHeight) {
    ElMessage.warning('摄像头尚未准备好，请稍后重试')
    return
  }
  const canvas = document.createElement('canvas')
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  const context = canvas.getContext('2d')
  if (!context) {
    ElMessage.error('拍照失败，请重试')
    return
  }
  context.drawImage(video, 0, 0, canvas.width, canvas.height)
  canvas.toBlob(
    (blob) => {
      if (!blob) {
        ElMessage.error('拍照失败，请重试')
        return
      }
      const file = new File([blob], `checkin-${Date.now()}.jpg`, {
        type: 'image/jpeg',
      })
      if (file.size > 5 * 1024 * 1024) {
        ElMessage.error('照片不能超过 5MB，请重新拍摄')
        return
      }
      stopStream()
      revokePreview()
      capturedFile.value = file
      capturedUrl.value = URL.createObjectURL(file)
    },
    'image/jpeg',
    0.9,
  )
}

function retake(): void {
  revokePreview()
  capturedFile.value = null
  startCamera()
}

function confirmCapture(): void {
  if (!capturedFile.value) return
  emit('captured', capturedFile.value)
  visible.value = false
}

function switchCamera(): void {
  facingMode.value = facingMode.value === 'environment' ? 'user' : 'environment'
  startCamera()
}

function chooseFile(): void {
  fileInputRef.value?.click()
}

function handleFileChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!/\.(jpg|jpeg|png)$/i.test(file.name)) {
    ElMessage.error('仅支持 JPG/PNG 格式照片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('照片不能超过 5MB')
    return
  }
  stopStream()
  revokePreview()
  capturedFile.value = file
  capturedUrl.value = URL.createObjectURL(file)
}

watch(visible, (open) => {
  if (open) {
    capturedFile.value = null
    revokePreview()
  } else {
    stopStream()
    revokePreview()
    capturedFile.value = null
  }
})

onBeforeUnmount(() => {
  stopStream()
  revokePreview()
})
</script>

<template>
  <el-dialog
    v-model="visible"
    title="拍照打卡"
    width="640px"
    append-to-body
    :close-on-click-modal="false"
    @opened="startCamera"
  >
    <div class="camera-shell">
      <div v-if="hasCapture" class="capture-preview">
        <img :src="capturedUrl" alt="打卡照片预览" />
        <div class="capture-badge">
          <el-icon><Check /></el-icon>
          照片已拍摄
        </div>
      </div>

      <div v-else-if="cameraError" class="camera-error">
        <el-result icon="warning" title="摄像头不可用" :sub-title="cameraError">
          <template #extra>
            <el-button type="primary" :icon="Upload" @click="chooseFile">
              从文件选择
            </el-button>
          </template>
        </el-result>
      </div>

      <div v-else v-loading="starting" class="camera-stage">
        <video
          ref="videoRef"
          autoplay
          muted
          playsinline
          class="camera-video"
        />
        <div class="camera-tip">
          <el-icon><VideoCamera /></el-icon>
          请将镜头对准打卡场景，拍摄后照片会由服务端添加水印
        </div>
      </div>

      <input
        ref="fileInputRef"
        type="file"
        accept="image/jpeg,image/png"
        class="hidden-file-input"
        @change="handleFileChange"
      />
    </div>

    <template #footer>
      <div class="camera-footer">
        <div class="footer-left">
          <el-button :icon="Upload" @click="chooseFile">从文件选择</el-button>
          <el-button
            v-if="!hasCapture && !cameraError"
            :icon="Refresh"
            :loading="starting"
            @click="switchCamera"
          >
            切换摄像头
          </el-button>
        </div>
        <div class="footer-right">
          <el-button @click="visible = false">取消</el-button>
          <template v-if="hasCapture">
            <el-button @click="retake">重拍</el-button>
            <el-button type="primary" :icon="Check" @click="confirmCapture">
              使用这张照片
            </el-button>
          </template>
          <el-button
            v-else
            type="primary"
            :icon="Camera"
            :disabled="Boolean(cameraError) || starting"
            @click="capture"
          >
            拍照
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.camera-shell {
  min-height: 300px;
}

.camera-stage,
.capture-preview {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 380px;
  overflow: hidden;
  background: #0b1f1a;
  border-radius: 16px;
}

.camera-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera-tip {
  position: absolute;
  bottom: 12px;
  left: 50%;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  white-space: nowrap;
  background: rgba(0, 0, 0, 0.42);
  border-radius: 999px;
  transform: translateX(-50%);
  backdrop-filter: blur(6px);
}

.capture-preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.capture-badge {
  position: absolute;
  bottom: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  font-size: 11px;
  color: #0b5c46;
  background: #d7f5e8;
  border-radius: 999px;
}

.camera-error {
  display: grid;
  place-items: center;
  min-height: 300px;
  background: #fbfdfc;
  border: 1px dashed #d9e9e3;
  border-radius: 16px;
}

.hidden-file-input {
  display: none;
}

.camera-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.footer-left,
.footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 640px) {
  .camera-stage,
  .capture-preview {
    height: 280px;
  }

  .camera-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .footer-left,
  .footer-right {
    width: 100%;
  }

  .footer-right {
    justify-content: flex-end;
  }
}
</style>
