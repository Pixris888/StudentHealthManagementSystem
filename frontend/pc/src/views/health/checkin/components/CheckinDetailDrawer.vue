<script setup lang="ts">
import { computed } from 'vue'
import { useIsMobile } from '@/utils/useIsMobile'
import {
  dataQualityTagType,
  dietBalanceLabel,
  emotionEmoji,
  emotionLabel,
  exerciseIntensityLabel,
  formatClockTime,
  formatDurationMinutes,
  formatMoodScore,
  formatSleepHours,
  formatWaterIntake,
  photoTypeLabel,
  yesNoTagType,
} from '@/utils/checkin'
import { formatDate, formatDateTime } from '@/utils/health'
import type { HealthCheckin } from '@/types/health'

const visible = defineModel<boolean>({ required: true })

const props = defineProps<{
  record: HealthCheckin | null
  loading?: boolean
  desensitized?: boolean
  showPhotos?: boolean
}>()

const isMobile = useIsMobile()
const drawerSize = computed(() => (isMobile.value ? '92%' : '560px'))
const completionCount = computed(() =>
  [
    props.record?.sleepCompleted,
    props.record?.exerciseCompleted,
    props.record?.dietCompleted,
    props.record?.emotionCompleted,
  ].filter(Boolean).length,
)
const photos = computed(() => props.record?.photos ?? [])
const previewList = computed(() => photos.value.map((item) => item.photoUrl))
</script>

<template>
  <el-drawer
    v-model="visible"
    title="打卡记录详情"
    :size="drawerSize"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading" class="checkin-drawer-body">
      <template v-if="record">
        <div class="drawer-hero">
          <div class="hero-main">
            <span class="hero-eyebrow">DAILY CHECK-IN</span>
            <h3>
              {{ formatDate(record.checkinDate) }}
              <span class="hero-emoji">{{ emotionEmoji(record.emotionType) }}</span>
            </h3>
            <p>
              {{ record.realName || '—' }}
              <span v-if="record.studentNo"> · {{ record.studentNo }}</span>
            </p>
            <div class="hero-tags">
              <el-tag
                :type="dataQualityTagType(record.dataQuality)"
                size="small"
                effect="light"
              >
                {{ record.dataQualityName || (record.dataQuality ? '异常待复核' : '正常') }}
              </el-tag>
              <el-tag v-if="record.backfilled" type="warning" size="small" effect="plain">
                补录
              </el-tag>
              <el-tag type="info" size="small" effect="plain">
                {{ completionCount }} / 4 板块
              </el-tag>
            </div>
          </div>
          <div class="hero-completion">
            <span>{{ completionCount }}</span>
            <small>/ 4</small>
          </div>
        </div>

        <el-alert
          v-if="desensitized"
          title="校医视角下学号、姓名已脱敏，照片不展示"
          type="info"
          :closable="false"
          show-icon
          class="mask-alert"
        />

        <el-descriptions title="基础信息" :column="2" border class="drawer-section">
          <el-descriptions-item label="打卡日期">
            {{ formatDate(record.checkinDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="学号">
            {{ record.studentNo || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="姓名">
            {{ record.realName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="数据质量">
            {{ record.dataQualityName || (record.dataQuality ? '异常待复核' : '正常') }}
          </el-descriptions-item>
          <el-descriptions-item label="照片数量">
            {{ record.photoCount ?? photos.length }} 张
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDateTime(record.updateTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="睡眠" :column="2" border class="drawer-section">
          <el-descriptions-item label="入睡 / 起床">
            {{ formatClockTime(record.sleepTime) }} -
            {{ formatClockTime(record.wakeTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="睡眠时长">
            {{ formatSleepHours(record.sleepDuration) }}
          </el-descriptions-item>
          <el-descriptions-item label="是否熬夜">
            <el-tag
              v-if="record.stayUpLate != null"
              :type="yesNoTagType(record.stayUpLate)"
              size="small"
              effect="light"
            >
              {{ record.stayUpLateName || (record.stayUpLate ? '是' : '否') }}
            </el-tag>
            <span v-else>未填写</span>
          </el-descriptions-item>
          <el-descriptions-item label="数据来源">
            {{ record.sleepSourceName || '未填写' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="运动" :column="2" border class="drawer-section">
          <el-descriptions-item label="运动类型">
            {{ record.exerciseType || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="运动时长">
            {{ formatDurationMinutes(record.exerciseDuration) }}
          </el-descriptions-item>
          <el-descriptions-item label="运动强度">
            {{ record.exerciseIntensityName || exerciseIntensityLabel(record.exerciseIntensity) }}
          </el-descriptions-item>
          <el-descriptions-item label="是否达标">
            <el-tag
              v-if="record.exerciseSufficient != null"
              :type="yesNoTagType(record.exerciseSufficient)"
              size="small"
              effect="light"
            >
              {{ record.exerciseSufficientName || (record.exerciseSufficient ? '是' : '否') }}
            </el-tag>
            <span v-else>未填写</span>
          </el-descriptions-item>
          <el-descriptions-item label="数据来源" :span="2">
            {{ record.exerciseSourceName || '未填写' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="饮食" :column="2" border class="drawer-section">
          <el-descriptions-item label="吃早餐">
            <el-tag
              v-if="record.dietBreakfast != null"
              :type="yesNoTagType(record.dietBreakfast)"
              size="small"
              effect="light"
            >
              {{ record.dietBreakfast ? '是' : '否' }}
            </el-tag>
            <span v-else>未填写</span>
          </el-descriptions-item>
          <el-descriptions-item label="饮食均衡度">
            {{ record.dietBalanceName || dietBalanceLabel(record.dietBalance) }}
          </el-descriptions-item>
          <el-descriptions-item label="饮水量" :span="2">
            {{ formatWaterIntake(record.waterIntake) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="情绪" :column="2" border class="drawer-section">
          <el-descriptions-item label="情绪类型">
            {{ record.emotionTypeName || emotionLabel(record.emotionType) }}
          </el-descriptions-item>
          <el-descriptions-item label="心情评分">
            {{ formatMoodScore(record.moodScore) }}
          </el-descriptions-item>
          <el-descriptions-item label="打卡备注" :span="2">
            {{ record.content || '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions
          v-if="record.abnormalReason"
          title="异常说明"
          :column="1"
          border
          class="drawer-section"
        >
          <el-descriptions-item label="异常原因">
            {{ record.abnormalReason }}
          </el-descriptions-item>
        </el-descriptions>

        <section
          v-if="showPhotos"
          class="drawer-section photo-section"
        >
          <h4 class="photo-title">打卡照片</h4>
          <div v-if="photos.length" class="photo-grid">
            <el-image
              v-for="(photo, index) in photos"
              :key="photo.id"
              :src="photo.photoUrl"
              :preview-src-list="previewList"
              :initial-index="index"
              fit="cover"
              preview-teleported
              class="photo-image"
            >
              <template #error>
                <div class="photo-error">照片加载失败</div>
              </template>
            </el-image>
            <div class="photo-type-row">
              <el-tag
                v-for="photo in photos"
                :key="`type-${photo.id}`"
                size="small"
                type="info"
                effect="plain"
              >
                {{ photo.photoTypeName || photoTypeLabel(photo.photoType) }}
              </el-tag>
            </div>
          </div>
          <el-empty
            v-else
            :description="
              (record.photoCount ?? 0) > 0
                ? '照片信息未加载，请刷新后重试'
                : '本次打卡没有上传照片'
            "
            :image-size="70"
          />
        </section>

        <el-alert
          v-else-if="(record.photoCount ?? 0) > 0"
          title="校医/管理员查看打卡详情时不展示照片，仅保留照片数量"
          type="info"
          :closable="false"
          show-icon
        />
      </template>

      <el-empty v-else description="暂无打卡记录" :image-size="90" />
    </div>
  </el-drawer>
</template>

<style scoped>
.checkin-drawer-body {
  min-height: 260px;
}

.drawer-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 18px;
  color: #eafff7;
  background:
    radial-gradient(circle at 100% 0%, rgba(61, 220, 151, 0.2), transparent 36%),
    linear-gradient(120deg, #0d4439, #14745a);
  border-radius: 16px;
}

.hero-eyebrow {
  font-size: 8.5px;
  font-weight: 600;
  letter-spacing: 2px;
  color: rgba(234, 255, 247, 0.5);
}

.hero-main h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 6px 0 4px;
  font-size: 23px;
  font-weight: 700;
  color: #fff;
}

.hero-emoji {
  font-size: 22px;
}

.hero-main p {
  margin-bottom: 10px;
  font-size: 12px;
  color: rgba(234, 255, 247, 0.6);
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.hero-completion {
  display: flex;
  flex-shrink: 0;
  align-items: baseline;
  gap: 3px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
}

.hero-completion span {
  font-size: 30px;
  font-weight: 700;
  color: #fff;
}

.hero-completion small {
  font-size: 11px;
  color: rgba(234, 255, 247, 0.5);
}

.mask-alert {
  margin-bottom: 18px;
}

.drawer-section {
  margin-bottom: 20px;
}

.drawer-section :deep(.el-descriptions__title) {
  font-size: 14px;
  font-weight: 700;
  color: var(--hp-text);
}

.drawer-section :deep(.el-descriptions__label) {
  width: 100px;
  color: var(--hp-text-secondary);
}

.drawer-section :deep(.el-descriptions__content) {
  color: var(--hp-text);
}

.photo-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 700;
  color: var(--hp-text);
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.photo-image {
  width: 100%;
  height: 110px;
  overflow: hidden;
  border-radius: 8px;
}

.photo-error {
  display: grid;
  width: 100%;
  height: 110px;
  place-items: center;
  font-size: 11px;
  color: var(--hp-text-muted);
  background: #f6faf8;
}

.photo-type-row {
  display: flex;
  grid-column: 1 / -1;
  flex-wrap: wrap;
  gap: 5px;
}

@media (max-width: 500px) {
  .drawer-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-completion {
    align-self: flex-end;
  }

  .photo-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
