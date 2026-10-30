<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Camera, Delete, Picture, Refresh, Upload } from '@element-plus/icons-vue'
import {
  deleteCheckinPhoto,
  uploadCheckinPhoto,
} from '@/api/health-checkin'
import {
  CHECKIN_MAX_PHOTOS,
  CHECKIN_PHOTO_TYPE_OPTIONS,
  photoTypeLabel,
} from '@/utils/checkin'
import type { CheckinPhoto } from '@/types/health'
import CameraCaptureDialog from './CameraCaptureDialog.vue'

const props = defineProps<{
  checkinId?: number
  photos?: CheckinPhoto[]
  maxPhotos?: number
}>()

const emit = defineEmits<{
  changed: []
  refresh: []
  'need-checkin': []
}>()

const photoType = ref(1)
const cameraVisible = ref(false)
const uploading = ref(false)
const fileInputRef = ref<HTMLInputElement>()
const photoErrorRefreshing = ref(false)

const photos = computed(() => props.photos ?? [])
const maxPhotos = computed(() => props.maxPhotos ?? CHECKIN_MAX_PHOTOS)
const canUpload = computed(
  () => Boolean(props.checkinId) && photos.value.length < maxPhotos.value,
)
const previewList = computed(() => photos.value.map((item) => item.photoUrl))

function triggerFile(): void {
  if (!props.checkinId) {
    emit('need-checkin')
    return
  }
  if (photos.value.length >= maxPhotos.value) {
    ElMessage.warning(`每条打卡最多上传 ${maxPhotos.value} 张照片`)
    return
  }
  fileInputRef.value?.click()
}

function openCamera(): void {
  if (!props.checkinId) {
    emit('need-checkin')
    return
  }
  if (photos.value.length >= maxPhotos.value) {
    ElMessage.warning(`每条打卡最多上传 ${maxPhotos.value} 张照片`)
    return
  }
  cameraVisible.value = true
}

function validateFile(file: File): boolean {
  if (!/\.(jpg|jpeg|png)$/i.test(file.name)) {
    ElMessage.error('仅支持 JPG/PNG 格式照片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('单张照片不能超过 5MB')
    return false
  }
  return true
}

async function upload(file: File): Promise<void> {
  if (!props.checkinId) {
    emit('need-checkin')
    return
  }
  if (!validateFile(file)) return
  if (photos.value.length >= maxPhotos.value) {
    ElMessage.warning(`每条打卡最多上传 ${maxPhotos.value} 张照片`)
    return
  }

  uploading.value = true
  try {
    await uploadCheckinPhoto(props.checkinId, file, photoType.value)
    ElMessage.success('照片上传成功，服务端已添加水印')
    emit('changed')
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    uploading.value = false
  }
}

function handleCameraCaptured(file: File): void {
  upload(file)
}

function handleFileChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (file) upload(file)
}

async function handleDelete(photo: CheckinPhoto): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '确定删除这张打卡照片吗？删除后文件也会被移除。',
      '删除照片',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return
  }

  try {
    await deleteCheckinPhoto(photo.id)
    ElMessage.success('照片已删除')
    emit('changed')
  } catch {
    // 请求错误由拦截器统一提示
  }
}

function handleImageError(): void {
  if (photoErrorRefreshing.value) return
  photoErrorRefreshing.value = true
  emit('refresh')
  window.setTimeout(() => {
    photoErrorRefreshing.value = false
  }, 3000)
}

function refreshPhotos(): void {
  emit('refresh')
}
</script>

<template>
  <section v-loading="uploading" class="panel-card photo-card">
    <div class="photo-head">
      <div>
        <h3>打卡照片</h3>
        <p>照片由服务端增加水印；OSS 签名 URL 过期后可点击刷新</p>
      </div>
      <div class="photo-head-right">
        <span class="photo-count">{{ photos.length }} / {{ maxPhotos }}</span>
        <el-button link :icon="Refresh" @click="refreshPhotos">刷新</el-button>
      </div>
    </div>

    <div class="photo-toolbar">
      <el-select
        v-model="photoType"
        size="small"
        style="width: 110px"
        :disabled="!checkinId"
      >
        <el-option
          v-for="item in CHECKIN_PHOTO_TYPE_OPTIONS"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-button
        size="small"
        :icon="Camera"
        :disabled="!canUpload"
        @click="openCamera"
      >
        拍照
      </el-button>
      <el-button
        size="small"
        :icon="Upload"
        :disabled="!canUpload"
        @click="triggerFile"
      >
        从相册选择
      </el-button>
      <span v-if="!checkinId" class="photo-hint">
        先完成任意一项打卡或手动填写后即可上传照片
      </span>
      <el-button
        v-else-if="photos.length >= maxPhotos"
        link
        type="warning"
        @click="ElMessage.warning(`每条打卡最多 ${maxPhotos} 张照片`)"
      >
        已达上限
      </el-button>
    </div>

    <div v-if="photos.length" class="photo-grid">
      <div
        v-for="(photo, index) in photos"
        :key="photo.id"
        class="photo-item"
      >
        <el-image
          :src="photo.photoUrl"
          :preview-src-list="previewList"
          :initial-index="index"
          fit="cover"
          preview-teleported
          class="photo-image"
          @error="handleImageError"
        />
        <div class="photo-meta">
          <el-tag size="small" effect="light">
            {{ photo.photoTypeName || photoTypeLabel(photo.photoType) }}
          </el-tag>
          <el-button
            link
            type="danger"
            :icon="Delete"
            class="photo-delete"
            @click.stop="handleDelete(photo)"
          >
            删除
          </el-button>
        </div>
      </div>
    </div>

    <div v-else class="photo-empty">
      <el-icon :size="30"><Picture /></el-icon>
      <p>还没有打卡照片</p>
      <span v-if="checkinId">可以用摄像头拍摄，或从相册上传一张照片</span>
      <el-button
        v-if="!checkinId"
        type="primary"
        plain
        size="small"
        @click="emit('need-checkin')"
      >
        去填写今日打卡
      </el-button>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      accept="image/jpeg,image/png"
      class="hidden-file-input"
      @change="handleFileChange"
    />

    <CameraCaptureDialog
      v-model="cameraVisible"
      @captured="handleCameraCaptured"
    />
  </section>
</template>

<style scoped>
.photo-card {
  padding: 20px 22px;
}

.photo-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.photo-head h3 {
  margin-bottom: 4px;
  font-size: 15px;
  font-weight: 700;
  color: var(--hp-text);
}

.photo-head p {
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.photo-head-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.photo-count {
  padding: 2px 8px;
  background: #f0f6f3;
  border-radius: 999px;
}

.photo-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.photo-hint {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.photo-item {
  overflow: hidden;
  background: #f7faf9;
  border: 1px solid #e7f0ec;
  border-radius: 10px;
}

.photo-image {
  display: block;
  width: 100%;
  height: 130px;
}

.photo-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 8px;
}

.photo-delete {
  font-size: 11px;
}

.photo-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 26px 16px;
  color: #a4b9b0;
  background: #fbfdfc;
  border: 1px dashed #dbe9e3;
  border-radius: 12px;
}

.photo-empty p {
  font-size: 13px;
  color: var(--hp-text-secondary);
}

.photo-empty span {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.hidden-file-input {
  display: none;
}

@media (max-width: 768px) {
  .photo-card {
    padding: 16px 14px;
  }

  .photo-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .photo-image {
    height: 115px;
  }
}
</style>
