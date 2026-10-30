<template>
  <view class="page page-with-tab checkin-page">
    <view class="checkin-head">
      <view class="between">
        <view>
          <text class="head-date">{{ todayLabelText }}</text>
          <text class="head-sub">记录睡眠、运动、饮食与情绪</text>
        </view>
        <view class="head-progress">
          <text class="head-progress-value">{{ completionCount }}</text>
          <text class="head-progress-total">/4</text>
        </view>
      </view>
      <view class="progress-track">
        <view class="progress-fill" :style="{ width: `${completionCount * 25}%` }" />
      </view>
    </view>

    <view class="tabs-wrap">
      <SegTabs v-model="activeTab" :options="tabOptions" @change="onTabChange" />
    </view>

    <template v-if="activeTab === 'today'">
      <view v-if="activeSleep || activeExercise" class="card session-card">
        <view class="between">
          <view class="row" style="gap: 14rpx">
            <view class="session-icon">
              <uni-icons type="sound" size="20" color="#0f9d8a" />
            </view>
            <view>
              <text class="session-title">计时进行中</text>
              <text class="session-desc">结束后会自动同步到今日打卡</text>
            </view>
          </view>
        </view>
        <view v-if="activeSleep" class="session-row">
          <view class="grow">
            <text class="session-name">睡眠计时</text>
            <text class="session-time">{{ formatDurationSeconds(sleepElapsed) }}</text>
          </view>
          <button class="btn btn-danger btn-sm" @click="endSession('sleep')">我起床了</button>
        </view>
        <view v-if="activeExercise" class="session-row">
          <view class="grow">
            <text class="session-name">运动计时 · {{ activeExercise.exerciseType || '运动' }}</text>
            <text class="session-time">{{ formatDurationSeconds(exerciseElapsed) }}</text>
          </view>
          <button class="btn btn-danger btn-sm" @click="endSession('exercise')">结束运动</button>
        </view>
      </view>

      <view v-if="today?.dataQuality === 1" class="abnormal-banner">
        <uni-icons type="info" size="16" color="#dc2626" />
        <text>数据异常待复核：{{ today.abnormalReason || '部分数据缺失或异常' }}</text>
      </view>

      <view
        v-for="item in moduleCards"
        :key="item.key"
        class="card module-card"
      >
        <view class="module-card-head">
          <view class="module-visual" :style="{ background: item.bg }">
            <text class="module-visual-emoji">{{ item.emoji }}</text>
            <view v-if="item.done" class="module-visual-check">
              <uni-icons type="checkmarkempty" size="11" color="#ffffff" />
            </view>
          </view>
          <view class="grow">
            <view class="row" style="gap: 10rpx">
              <text class="module-name">{{ item.title }}</text>
              <text class="tag" :class="item.done ? 'tag-success' : 'tag-info'">
                {{ item.done ? '已完成' : '待完成' }}
              </text>
            </view>
            <text class="module-summary">{{ item.summary }}</text>
          </view>
        </view>

        <view class="module-actions">
          <button
            v-if="item.key === 'sleep'"
            class="btn btn-ghost btn-sm"
            @click="handleSleepAction"
          >
            {{ activeSleep ? '结束睡眠计时' : '开始睡眠计时' }}
          </button>
          <button
            v-if="item.key === 'exercise'"
            class="btn btn-ghost btn-sm"
            @click="handleExerciseAction"
          >
            {{ activeExercise ? '结束运动计时' : '开始运动计时' }}
          </button>
          <button class="btn btn-plain btn-sm" @click="openEdit(item.date)">
            {{ item.done ? '修改' : '手动填写' }}
          </button>
        </view>
      </view>

      <view class="card photo-card">
        <view class="between">
          <view>
            <text class="card-title">打卡照片</text>
            <text class="card-subtitle">服务端自动加水印，每条最多 {{ MAX_PHOTOS }} 张</text>
          </view>
          <view class="photo-head-actions">
            <text class="photo-count">{{ photos.length }}/{{ MAX_PHOTOS }}</text>
            <picker
              :range="photoTypeLabels"
              :value="photoTypeIndex"
              @change="onPhotoTypeChange"
            >
              <view class="photo-type-picker">
                <text>{{ photoTypeLabel(photoType) }}</text>
                <uni-icons type="arrowdown" size="13" color="#0f9d8a" />
              </view>
            </picker>
            <view class="photo-refresh" @click="refreshPhotos">
              <uni-icons type="refreshempty" size="17" color="#0f766e" />
            </view>
          </view>
        </view>

        <view v-if="photos.length" class="photo-grid">
          <view
            v-for="(photo, index) in photos"
            :key="photo.id"
            class="photo-item"
            @click="previewPhoto(index)"
          >
            <image :src="resolveFileUrl(photo.photoUrl)" class="photo-image" mode="aspectFill" />
            <view class="photo-remove" @click.stop="removePhoto(photo)">
              <uni-icons type="closeempty" size="14" color="#ffffff" />
            </view>
          </view>
          <view v-if="photos.length < MAX_PHOTOS" class="photo-add" @click="addPhoto">
            <uni-icons type="camera" size="26" color="#9fb6ae" />
            <text>添加</text>
          </view>
        </view>

        <view v-else class="photo-empty" @click="addPhoto">
          <view class="photo-empty-icon">
            <uni-icons type="camera" size="26" color="#0f9d8a" />
          </view>
          <view class="grow">
            <text class="photo-empty-title">拍一张今日照片</text>
            <text class="photo-empty-desc">
              {{ today?.id ? '支持拍照或从相册选择' : '先保存今日打卡后即可上传' }}
            </text>
          </view>
          <uni-icons type="arrowright" size="15" color="#b1bfba" />
        </view>
      </view>

      <button class="btn btn-primary btn-block edit-all-btn" @click="openEdit(today?.checkinDate)">
        <uni-icons type="compose" size="17" color="#ffffff" />
        <text>填写 / 修改今日打卡</text>
      </button>
    </template>

    <template v-else-if="activeTab === 'history'">
      <view v-if="historyLoading && !history.length" class="card loading-card">
        <uni-load-more status="loading" color="#0f9d8a" />
      </view>
      <template v-else>
        <view
          v-for="record in history"
          :key="record.id || record.checkinDate"
          class="card history-card"
          @click="openEdit(record.checkinDate)"
        >
          <view class="between">
            <view>
              <text class="history-date">{{ record.checkinDate }}</text>
              <text class="history-sub">{{ record.backfilled ? '补录记录' : '当日记录' }}</text>
            </view>
            <view class="tag" :class="record.dataQuality === 1 ? 'tag-danger' : 'tag-success'">
              {{ record.dataQuality === 1 ? '异常待复核' : '数据正常' }}
            </view>
          </view>
          <view class="history-chips">
            <text class="mini-chip" :class="{ done: record.sleepCompleted }">睡眠</text>
            <text class="mini-chip" :class="{ done: record.exerciseCompleted }">运动</text>
            <text class="mini-chip" :class="{ done: record.dietCompleted }">饮食</text>
            <text class="mini-chip" :class="{ done: record.emotionCompleted }">情绪</text>
          </view>
          <view class="history-meta">
            <text>睡眠 {{ record.sleepDuration != null ? formatNumber(record.sleepDuration, 1) + ' h' : '—' }}</text>
            <text>运动 {{ record.exerciseDuration != null ? record.exerciseDuration + ' min' : '—' }}</text>
            <text>心情 {{ record.emotionTypeName || '—' }}</text>
          </view>
          <text v-if="record.dataQuality === 1" class="history-abnormal">
            {{ record.abnormalReason || '该条记录存在异常，建议补充或修改' }}
          </text>
        </view>

        <EmptyState
          v-if="!history.length"
          icon="calendar"
          title="暂无打卡记录"
          desc="从今天开始，记录你的健康生活"
          action-text="去打卡"
          @action="activeTab = 'today'"
        />

        <view v-if="history.length" class="load-more">
          <uni-load-more
            :status="historyLoading ? 'loading' : historyHasMore ? 'more' : 'noMore'"
            color="#94a3b8"
            @click="loadMoreHistory"
          />
        </view>
      </template>
    </template>

    <template v-else>
      <view class="card summary-head">
        <view class="between">
          <view>
            <text class="card-title">月度概览</text>
            <text class="card-subtitle">看看这个月的坚持成果</text>
          </view>
          <picker mode="date" fields="month" :value="summaryMonth" @change="onMonthChange">
            <view class="month-picker">
              <uni-icons type="calendar" size="15" color="#0f9d8a" />
              <text>{{ formatMonthLabel(summaryMonth) }}</text>
              <uni-icons type="arrowdown" size="13" color="#0f9d8a" />
            </view>
          </picker>
        </view>
        <view class="summary-hero">
          <view class="summary-rate">
            <text class="summary-rate-value">{{ formatPercent(summary.checkinRate) }}</text>
            <text class="summary-rate-label">本月打卡率</text>
          </view>
          <view class="summary-rate-info">
            <text>已打卡 {{ summary.checkinDays ?? 0 }} 天</text>
            <text>连续 {{ summary.consecutiveDays ?? 0 }} 天</text>
          </view>
        </view>
      </view>

      <view class="summary-grid">
        <view class="summary-tile">
          <text class="summary-tile-icon">🌙</text>
          <text class="summary-tile-value">{{ formatNumber(summary.avgSleepDuration, 1) }}<text class="summary-tile-unit">h</text></text>
          <text class="summary-tile-label">平均睡眠</text>
        </view>
        <view class="summary-tile">
          <text class="summary-tile-icon">🏃</text>
          <text class="summary-tile-value">{{ summary.totalExerciseMinutes ?? 0 }}<text class="summary-tile-unit">min</text></text>
          <text class="summary-tile-label">运动总时长</text>
        </view>
        <view class="summary-tile">
          <text class="summary-tile-icon">🌙</text>
          <text class="summary-tile-value">{{ summary.lateCount ?? 0 }}<text class="summary-tile-unit">次</text></text>
          <text class="summary-tile-label">熬夜次数</text>
        </view>
        <view class="summary-tile">
          <text class="summary-tile-icon">🥗</text>
          <text class="summary-tile-value">{{ summary.breakfastDays ?? 0 }}<text class="summary-tile-unit">天</text></text>
          <text class="summary-tile-label">吃早餐</text>
        </view>
        <view class="summary-tile">
          <text class="summary-tile-icon">😊</text>
          <text class="summary-tile-value">{{ formatNumber(summary.avgMoodScore, 1) }}<text class="summary-tile-unit">/5</text></text>
          <text class="summary-tile-label">平均心情</text>
        </view>
        <view class="summary-tile">
          <text class="summary-tile-icon">💧</text>
          <text class="summary-tile-value">{{ formatNumber(summary.avgWaterIntake, 0) }}<text class="summary-tile-unit">ml</text></text>
          <text class="summary-tile-label">平均饮水</text>
        </view>
      </view>

      <view class="card summary-note">
        <view class="row" style="gap: 12rpx">
          <uni-icons type="info" size="17" color="#0f9d8a" />
          <text class="summary-note-title">健康提示</text>
        </view>
        <text class="summary-note-text">
          {{ summary.abnormalCount ? `本月有 ${summary.abnormalCount} 条异常记录，建议回到历史记录中补充或修改。` : '本月打卡数据整体良好，继续保持规律作息与适量运动。' }}
        </text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onHide, onPullDownRefresh, onReachBottom, onShow, onUnload } from '@dcloudio/uni-app'
import SegTabs from '@/components/SegTabs.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  deleteCheckinPhoto,
  endCheckinSession,
  getActiveCheckinSession,
  getCheckinPhotos,
  getCheckinSummary,
  getTodayCheckin,
  pageMyCheckins,
  startCheckinSession,
  uploadCheckinPhoto,
} from '@/api/health-checkin'
import { useUserStore } from '@/stores/user'
import { getAccessToken } from '@/utils/auth'
import {
  CHECKIN_EXERCISE_INTENSITY_OPTIONS,
  CHECKIN_EXERCISE_TYPES,
  CHECKIN_MAX_PHOTOS,
  CHECKIN_PHOTO_TYPE_OPTIONS,
  checkinCompletionCount,
  currentMonthString,
  dietBalanceLabel,
  emotionEmoji,
  emotionLabel,
  exerciseIntensityLabel,
  formatClockTime,
  formatMonthLabel,
  isExerciseTimerSource,
  isSleepTimerSource,
  photoTypeLabel,
  todayDateString,
} from '@/utils/checkin'
import {
  formatDurationSeconds,
  formatNumber,
  formatPercent,
  todayLabel,
} from '@/utils/format'
import { resolveFileUrl } from '@/utils/url'
import type {
  CheckinSession,
  CheckinSummary,
  HealthCheckin,
  CheckinPhoto,
} from '@/types/health'

const MAX_PHOTOS = CHECKIN_MAX_PHOTOS
const tabOptions = [
  { value: 'today', label: '今日打卡' },
  { value: 'history', label: '历史记录' },
  { value: 'summary', label: '月度概览' },
]

const userStore = useUserStore()
const activeTab = ref<'today' | 'history' | 'summary'>('today')
const loading = ref(true)
const today = ref<HealthCheckin | null>(null)
const summary = ref<CheckinSummary>({})
const summaryMonth = ref(currentMonthString())
const history = ref<HealthCheckin[]>([])
const historyPage = ref(1)
const historyTotal = ref(0)
const historyLoading = ref(false)
const photoType = ref(1)
const activeSleep = ref<CheckinSession | null>(null)
const activeExercise = ref<CheckinSession | null>(null)
const sleepElapsed = ref(0)
const exerciseElapsed = ref(0)
let elapsedTimer: ReturnType<typeof setInterval> | null = null

const todayLabelText = todayLabel()
const completionCount = computed(() => checkinCompletionCount(today.value))
const photos = computed<CheckinPhoto[]>(() => today.value?.photos ?? [])
const photoTypeLabels = CHECKIN_PHOTO_TYPE_OPTIONS.map((item) => item.label)
const photoTypeIndex = computed(() =>
  Math.max(0, CHECKIN_PHOTO_TYPE_OPTIONS.findIndex((item) => item.value === photoType.value)),
)
const historyHasMore = computed(
  () => history.value.length < historyTotal.value,
)

const moduleCards = computed(() => {
  const record = today.value
  return [
    {
      key: 'sleep',
      title: '睡眠',
      emoji: '🌙',
      bg: '#eef2ff',
      done: Boolean(record?.sleepCompleted),
      date: record?.checkinDate || todayDateString(),
      summary: record?.sleepDuration
        ? `${formatClockTime(record.sleepTime)} 入睡 · ${formatClockTime(record.wakeTime)} 起床 · ${formatNumber(record.sleepDuration, 1)} 小时${isSleepTimerSource(record) ? '（计时记录）' : ''}`
        : '记录入睡和起床时间，帮助了解作息规律',
    },
    {
      key: 'exercise',
      title: '运动',
      emoji: '🏃',
      bg: '#e8f8ef',
      done: Boolean(record?.exerciseCompleted),
      date: record?.checkinDate || todayDateString(),
      summary: record?.exerciseDuration
        ? `${record.exerciseType || '运动'} · ${record.exerciseDuration} 分钟 · ${exerciseIntensityLabel(record.exerciseIntensity)}${isExerciseTimerSource(record) ? '（计时记录）' : ''}`
        : '记录运动类型、时长和强度，保持适量运动',
    },
    {
      key: 'diet',
      title: '饮食',
      emoji: '🥗',
      bg: '#fff7e6',
      done: Boolean(record?.dietCompleted),
      date: record?.checkinDate || todayDateString(),
      summary: record?.dietBreakfast != null
        ? `${record.dietBreakfast === 1 ? '已吃早餐' : '未吃早餐'} · 饮食${dietBalanceLabel(record.dietBalance)} · 饮水 ${record.waterIntake ?? '—'} ml`
        : '记录早餐、饮食均衡和饮水量',
    },
    {
      key: 'emotion',
      title: '情绪',
      emoji: record?.emotionType ? emotionEmoji(record.emotionType) : '😊',
      bg: '#f2ecff',
      done: Boolean(record?.emotionCompleted),
      date: record?.checkinDate || todayDateString(),
      summary: record?.emotionType
        ? `${emotionLabel(record.emotionType)} · 心情评分 ${record.moodScore ?? '—'} / 5`
        : '记录今天的心情状态，关注心理健康',
    },
  ]
})

function updateElapsed(): void {
  sleepElapsed.value = activeSleep.value
    ? Math.max(0, Math.floor((Date.now() - new Date(activeSleep.value.startTime.replace(' ', 'T')).getTime()) / 1000))
    : 0
  exerciseElapsed.value = activeExercise.value
    ? Math.max(0, Math.floor((Date.now() - new Date(activeExercise.value.startTime.replace(' ', 'T')).getTime()) / 1000))
    : 0
}

function startElapsedTimer(): void {
  if (elapsedTimer) return
  updateElapsed()
  elapsedTimer = setInterval(updateElapsed, 1000)
}

function stopElapsedTimer(): void {
  if (!elapsedTimer) return
  clearInterval(elapsedTimer)
  elapsedTimer = null
}

function ensureLogin(): boolean {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

async function loadToday(): Promise<void> {
  try {
    const record = await getTodayCheckin()
    today.value = record ?? null
    if (record?.id && (!record.photos || record.photos.length < (record.photoCount ?? 0))) {
      try {
        const list = await getCheckinPhotos(record.id)
        if (list) today.value = { ...record, photos: list }
      } catch {
        // 照片刷新失败不影响打卡数据
      }
    }
  } catch {
    // 请求层已提示
  }
}

async function loadSummary(): Promise<void> {
  try {
    summary.value = (await getCheckinSummary(summaryMonth.value)) ?? {}
  } catch {
    summary.value = {}
  }
}

async function loadHistory(reset = false): Promise<void> {
  if (historyLoading.value) return
  if (reset) {
    historyPage.value = 1
    historyTotal.value = 0
  }
  historyLoading.value = true
  try {
    const page = await pageMyCheckins({ page: historyPage.value, size: 10 })
    const records = page.records ?? []
    history.value = reset ? records : [...history.value, ...records]
    historyTotal.value = page.total ?? history.value.length
  } catch {
    // 请求层已提示
  } finally {
    historyLoading.value = false
  }
}

async function loadActiveSessions(): Promise<void> {
  const [sleepResult, exerciseResult] = await Promise.allSettled([
    getActiveCheckinSession(1),
    getActiveCheckinSession(2),
  ])
  activeSleep.value = sleepResult.status === 'fulfilled' ? sleepResult.value : null
  activeExercise.value = exerciseResult.status === 'fulfilled' ? exerciseResult.value : null
  if (activeSleep.value || activeExercise.value) {
    startElapsedTimer()
  } else {
    stopElapsedTimer()
  }
}

async function loadAll(): Promise<void> {
  if (!ensureLogin()) return
  if (!userStore.userInfo) {
    try {
      await userStore.bootstrap()
    } catch {
      uni.reLaunch({ url: '/pages/login/index' })
      return
    }
  }
  loading.value = true
  await Promise.all([loadToday(), loadSummary(), loadActiveSessions()])
  if (activeTab.value === 'history' && !history.value.length) {
    await loadHistory(true)
  }
  loading.value = false
}

function onTabChange(value: string | number): void {
  if (value === 'history' && !history.value.length) {
    loadHistory(true)
  }
  if (value === 'summary') {
    loadSummary()
  }
}

function loadMoreHistory(): void {
  if (historyLoading.value || !historyHasMore.value) return
  historyPage.value += 1
  loadHistory(false)
}

function onMonthChange(event: { detail: { value: string } }): void {
  summaryMonth.value = event.detail.value || currentMonthString()
  loadSummary()
}

function onPhotoTypeChange(event: { detail: { value: string | number } }): void {
  const index = Number(event.detail.value)
  photoType.value = CHECKIN_PHOTO_TYPE_OPTIONS[index]?.value ?? 1
}

function openEdit(date?: string | null): void {
  const target = date && date <= todayDateString() ? date : todayDateString()
  uni.navigateTo({ url: `/pages/checkin/edit?date=${encodeURIComponent(target)}` })
}

function chooseActionSheet(itemList: string[]): Promise<number> {
  return new Promise((resolve, reject) => {
    uni.showActionSheet({
      itemList,
      success: (res) => resolve(res.tapIndex),
      fail: reject,
    })
  })
}

function confirm(content: string, title = '提示'): Promise<boolean> {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      success: (res) => resolve(Boolean(res.confirm)),
      fail: () => resolve(false),
    })
  })
}

async function handleSleepAction(): Promise<void> {
  if (activeSleep.value) {
    if (!(await confirm('确定结束睡眠计时吗？结束后会自动同步今日打卡。', '我起床了'))) return
    try {
      await endCheckinSession(activeSleep.value.id)
      activeSleep.value = null
      stopElapsedTimer()
      uni.showToast({ title: '睡眠记录已同步', icon: 'success' })
      await Promise.all([loadToday(), loadSummary()])
      await loadActiveSessions()
    } catch {
      // 请求层已提示
    }
    return
  }
  if (!(await confirm('现在开始睡眠计时？睡眠计时会一直运行，第二天起床后点击“我起床了”结束。', '开始睡眠'))) {
    return
  }
  try {
    const session = await startCheckinSession({ sessionType: 1 })
    activeSleep.value = session
    startElapsedTimer()
    uni.showToast({ title: '睡眠计时已开始', icon: 'success' })
  } catch {
    // 请求层已提示
  }
}

async function handleExerciseAction(): Promise<void> {
  if (activeExercise.value) {
    if (!(await confirm('确定结束运动计时吗？结束后会自动同步今日打卡。', '结束运动'))) return
    try {
      await endCheckinSession(activeExercise.value.id)
      activeExercise.value = null
      stopElapsedTimer()
      uni.showToast({ title: '运动记录已同步', icon: 'success' })
      await Promise.all([loadToday(), loadSummary()])
      await loadActiveSessions()
    } catch {
      // 请求层已提示
    }
    return
  }

  try {
    const typeIndex = await chooseActionSheet(CHECKIN_EXERCISE_TYPES)
    const intensityIndex = await chooseActionSheet(
      CHECKIN_EXERCISE_INTENSITY_OPTIONS.map((item) => item.label),
    )
    const session = await startCheckinSession({
      sessionType: 2,
      exerciseType: CHECKIN_EXERCISE_TYPES[typeIndex],
      exerciseIntensity: CHECKIN_EXERCISE_INTENSITY_OPTIONS[intensityIndex]?.value,
    })
    activeExercise.value = session
    startElapsedTimer()
    uni.showToast({ title: '运动计时已开始', icon: 'success' })
  } catch {
    // 取消选择时静默返回
  }
}

async function endSession(type: 'sleep' | 'exercise'): Promise<void> {
  const session = type === 'sleep' ? activeSleep.value : activeExercise.value
  if (!session) return
  const label = type === 'sleep' ? '睡眠' : '运动'
  if (!(await confirm(`确定结束${label}计时吗？结束后会自动同步今日打卡。`, `结束${label}`))) return
  try {
    await endCheckinSession(session.id)
    if (type === 'sleep') activeSleep.value = null
    else activeExercise.value = null
    stopElapsedTimer()
    uni.showToast({ title: `${label}记录已同步`, icon: 'success' })
    await Promise.all([loadToday(), loadSummary()])
    await loadActiveSessions()
  } catch {
    // 请求层已提示
  }
}

async function addPhoto(): Promise<void> {
  if (!today.value?.id) {
    uni.showModal({
      title: '请先保存今日打卡',
      content: '打卡记录保存后才能上传照片，是否现在去填写？',
      confirmText: '去填写',
      success: (res) => {
        if (res.confirm) openEdit(today.value?.checkinDate)
      },
    })
    return
  }
  if (photos.value.length >= MAX_PHOTOS) {
    uni.showToast({ title: `每条打卡最多上传 ${MAX_PHOTOS} 张照片`, icon: 'none' })
    return
  }

  uni.chooseImage({
    count: MAX_PHOTOS - photos.value.length,
    sizeType: ['compressed'],
    sourceType: ['camera', 'album'],
    success: async (res) => {
      const files = res.tempFilePaths ?? []
      if (!files.length) return
      const tempFiles = (res as unknown as { tempFiles?: Array<{ size?: number }> }).tempFiles ?? []
      for (let index = 0; index < files.length; index += 1) {
        const size = tempFiles[index]?.size
        if (size && size > 5 * 1024 * 1024) {
          uni.showToast({ title: '单张照片不能超过 5MB', icon: 'none' })
          continue
        }
        uni.showLoading({ title: '上传中…', mask: true })
        try {
          await uploadCheckinPhoto(today.value!.id!, files[index], photoType.value)
        } catch {
          // 请求层已提示
        } finally {
          uni.hideLoading()
        }
      }
      await loadToday()
      uni.showToast({ title: '照片已上传', icon: 'success' })
    },
  })
}

function previewPhoto(index: number): void {
  const urls = photos.value.map((item) => resolveFileUrl(item.photoUrl))
  if (!urls.length) return
  uni.previewImage({ urls, current: urls[index] })
}

async function refreshPhotos(): Promise<void> {
  await loadToday()
  uni.showToast({ title: '已刷新打卡数据', icon: 'none' })
}

function removePhoto(photo: CheckinPhoto): void {
  uni.showModal({
    title: '删除照片',
    content: '确定删除这张打卡照片吗？删除后文件也会被移除。',
    confirmColor: '#dc2626',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteCheckinPhoto(photo.id)
        uni.showToast({ title: '照片已删除', icon: 'success' })
        await loadToday()
      } catch {
        // 请求层已提示
      }
    },
  })
}

onShow(loadAll)

onPullDownRefresh(async () => {
  if (activeTab.value === 'history') {
    await loadHistory(true)
  } else if (activeTab.value === 'summary') {
    await loadSummary()
  } else {
    await loadAll()
  }
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (activeTab.value === 'history') loadMoreHistory()
})

onHide(stopElapsedTimer)
onUnload(stopElapsedTimer)
</script>

<style scoped>
.checkin-page {
  padding-top: 20rpx;
}

.checkin-head {
  padding: 10rpx 6rpx 20rpx;
}

.head-date {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #0f172a;
}

.head-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.head-progress {
  display: flex;
  align-items: baseline;
  color: #0f9d8a;
}

.head-progress-value {
  font-size: 48rpx;
  font-weight: 700;
  line-height: 1;
}

.head-progress-total {
  margin-left: 2rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.progress-track {
  width: 100%;
  height: 10rpx;
  margin-top: 18rpx;
  border-radius: 999rpx;
  background: #e8f1ee;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #12b39c, #0b7f70);
  transition: width 0.35s ease;
}

.tabs-wrap {
  margin-bottom: 22rpx;
}

.session-card {
  margin-bottom: 22rpx;
  background: linear-gradient(120deg, #f0fbf8, #e5f7f2);
}

.session-icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 18rpx;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.session-title {
  display: block;
  font-size: 27rpx;
  font-weight: 700;
  color: #0f766e;
}

.session-desc {
  display: block;
  margin-top: 3rpx;
  font-size: 21rpx;
  color: #7b9a92;
}

.session-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #d7f0e9;
}

.session-name {
  display: block;
  font-size: 23rpx;
  color: #417a70;
}

.session-time {
  display: block;
  margin-top: 2rpx;
  font-size: 38rpx;
  font-weight: 700;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}

.abnormal-banner {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  margin-bottom: 20rpx;
  padding: 18rpx 22rpx;
  border-radius: 18rpx;
  background: #fef2f2;
  font-size: 22rpx;
  line-height: 1.6;
  color: #b91c1c;
}

.module-card {
  margin-bottom: 20rpx;
}

.module-card-head {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
}

.module-visual {
  position: relative;
  width: 88rpx;
  height: 88rpx;
  border-radius: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.module-visual-emoji {
  font-size: 38rpx;
  line-height: 1;
}

.module-visual-check {
  position: absolute;
  right: -4rpx;
  bottom: -4rpx;
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  background: #0f9d8a;
  border: 3rpx solid #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.module-name {
  font-size: 29rpx;
  font-weight: 700;
  color: #0f172a;
}

.module-summary {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: #7b8f89;
}

.module-actions {
  display: flex;
  gap: 14rpx;
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #f0f5f3;
}

.photo-head-actions {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.photo-count {
  font-size: 20rpx;
  color: #a8b7b2;
}

.photo-type-picker {
  display: flex;
  align-items: center;
  gap: 6rpx;
  height: 56rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: #e6f7f3;
  color: #0f766e;
  font-size: 22rpx;
}

.photo-refresh {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #f2faf8;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 22rpx;
}

.photo-item {
  position: relative;
  width: calc(33.33% - 11rpx);
  height: 180rpx;
  border-radius: 18rpx;
  overflow: hidden;
  background: #f1f5f4;
}

.photo-image {
  width: 100%;
  height: 100%;
}

.photo-remove {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  width: 38rpx;
  height: 38rpx;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-add {
  width: calc(33.33% - 11rpx);
  height: 180rpx;
  border-radius: 18rpx;
  border: 2rpx dashed #d7e5df;
  background: #f8fbfa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  color: #9fb6ae;
  font-size: 21rpx;
}

.photo-empty {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 22rpx;
  padding: 22rpx;
  border-radius: 20rpx;
  background: #f4faf8;
}

.photo-empty-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 22rpx;
  background: #e0f5ef;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-empty-title {
  display: block;
  font-size: 25rpx;
  font-weight: 600;
  color: #0f766e;
}

.photo-empty-desc {
  display: block;
  margin-top: 4rpx;
  font-size: 21rpx;
  color: #94a3b8;
}

.edit-all-btn {
  gap: 10rpx;
  margin-top: 26rpx;
}

.loading-card {
  min-height: 260rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.history-card {
  margin-bottom: 20rpx;
}

.history-date {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.history-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 21rpx;
  color: #a8b7b2;
}

.history-chips {
  display: flex;
  gap: 12rpx;
  margin-top: 18rpx;
}

.mini-chip {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: #f1f5f4;
  color: #a8b7b2;
  font-size: 21rpx;
}

.mini-chip.done {
  background: #e6f7f3;
  color: #0f766e;
}

.history-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
  margin-top: 16rpx;
  font-size: 21rpx;
  color: #94a3b8;
}

.history-abnormal {
  display: block;
  margin-top: 12rpx;
  padding: 12rpx 16rpx;
  border-radius: 12rpx;
  background: #fef2f2;
  font-size: 21rpx;
  color: #b91c1c;
}

.load-more {
  padding: 10rpx 0 20rpx;
}

.summary-head {
  margin-bottom: 22rpx;
}

.month-picker {
  display: flex;
  align-items: center;
  gap: 6rpx;
  height: 58rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: #e6f7f3;
  color: #0f766e;
  font-size: 22rpx;
}

.summary-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 26rpx;
  padding: 24rpx;
  border-radius: 22rpx;
  background: linear-gradient(120deg, #f1fbf8, #e6f7f2);
}

.summary-rate {
  display: flex;
  flex-direction: column;
}

.summary-rate-value {
  font-size: 52rpx;
  font-weight: 700;
  color: #0f766e;
  line-height: 1;
}

.summary-rate-label {
  margin-top: 8rpx;
  font-size: 21rpx;
  color: #7b9a92;
}

.summary-rate-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6rpx;
  font-size: 23rpx;
  color: #417a70;
}

.summary-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.summary-tile {
  width: calc(33.33% - 11rpx);
  padding: 22rpx 16rpx;
  border-radius: 22rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(15, 118, 110, 0.05);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.summary-tile-icon {
  font-size: 30rpx;
}

.summary-tile-value {
  margin-top: 10rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
}

.summary-tile-unit {
  margin-left: 2rpx;
  font-size: 18rpx;
  font-weight: 400;
  color: #94a3b8;
}

.summary-tile-label {
  margin-top: 6rpx;
  font-size: 20rpx;
  color: #94a3b8;
}

.summary-note {
  margin-top: 22rpx;
}

.summary-note-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #0f766e;
}

.summary-note-text {
  display: block;
  margin-top: 12rpx;
  font-size: 23rpx;
  line-height: 1.7;
  color: #64748b;
}
</style>
