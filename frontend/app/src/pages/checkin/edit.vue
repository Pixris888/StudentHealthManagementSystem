<template>
  <view class="page page-with-fixed">
    <view class="card date-card">
      <view class="between">
        <view>
          <text class="date-title">{{ formDate === today ? '今日打卡' : '补录打卡' }}</text>
          <text class="date-sub">仅支持最近 7 天补录，留空表示不修改该板块</text>
        </view>
        <picker
          mode="date"
          :value="formDate"
          :start="earliestDate"
          :end="today"
          @change="onDateChange"
        >
          <view class="date-picker">
            <uni-icons type="calendar" size="15" color="#0f9d8a" />
            <text>{{ formDate }}</text>
            <uni-icons type="arrowdown" size="12" color="#0f9d8a" />
          </view>
        </picker>
      </view>
      <view v-if="dateRecord" class="date-status">
        <uni-icons type="info" size="14" color="#0f766e" />
        <text>{{ dateRecord.backfilled ? '该日期已有补录记录，提交后更新' : '该日期已有记录，提交后更新' }}</text>
      </view>
    </view>

    <view v-if="timerSleep || timerExercise" class="timer-banner">
      <uni-icons type="sound" size="16" color="#b45309" />
      <text>
        {{ timerSleep ? '睡眠' : '运动' }}计时产生的数据由系统自动同步，本页不可手动修改对应板块。
      </text>
    </view>

    <view class="card">
      <view class="card-title-row">
        <text class="card-title">🌙 睡眠</text>
        <text v-if="sleepHours != null" class="tag tag-primary">
          {{ sleepHours }} 小时
        </text>
      </view>
      <text class="card-subtitle">记录入睡和起床时间，跨午夜会自动计算</text>

      <view class="double-row">
        <view class="form-item half">
          <text class="form-label">入睡时间</text>
          <picker
            mode="time"
            :value="form.sleepTime || '23:00'"
            :disabled="timerSleep"
            @change="onSleepTimeChange"
          >
            <view class="picker-box" :class="{ disabled: timerSleep }">
              <text :class="form.sleepTime ? 'picker-value' : 'picker-placeholder'">
                {{ form.sleepTime || '请选择' }}
              </text>
              <uni-icons type="arrowdown" size="14" color="#b1bfba" />
            </view>
          </picker>
        </view>
        <view class="form-item half">
          <text class="form-label">起床时间</text>
          <picker
            mode="time"
            :value="form.wakeTime || '07:00'"
            :disabled="timerSleep"
            @change="onWakeTimeChange"
          >
            <view class="picker-box" :class="{ disabled: timerSleep }">
              <text :class="form.wakeTime ? 'picker-value' : 'picker-placeholder'">
                {{ form.wakeTime || '请选择' }}
              </text>
              <uni-icons type="arrowdown" size="14" color="#b1bfba" />
            </view>
          </picker>
        </view>
      </view>
      <text v-if="timerSleep" class="section-tip">当前由睡眠计时自动记录，结束后自动填充。</text>
    </view>

    <view class="card">
      <view class="card-title-row">
        <text class="card-title">🏃 运动</text>
        <text v-if="form.exerciseDurationText" class="tag tag-primary">
          {{ form.exerciseDurationText }} 分钟
        </text>
      </view>
      <text class="card-subtitle">填写今天的运动类型、时长和强度</text>

      <view class="form-item">
        <text class="form-label">运动类型</text>
        <picker
          :range="exerciseTypeOptions"
          :value="exerciseTypeIndex < 0 ? 0 : exerciseTypeIndex"
          :disabled="timerExercise"
          @change="onExerciseTypeChange"
        >
          <view class="picker-box" :class="{ disabled: timerExercise }">
            <text :class="form.exerciseType ? 'picker-value' : 'picker-placeholder'">
              {{ form.exerciseType || '请选择运动类型' }}
            </text>
            <uni-icons type="arrowdown" size="14" color="#b1bfba" />
          </view>
        </picker>
      </view>

      <view class="double-row">
        <view class="form-item half">
          <text class="form-label">运动时长（分钟）</text>
          <view class="input-box" :class="{ disabled: timerExercise }">
            <input
              v-model="form.exerciseDurationText"
              class="input"
              type="number"
              :disabled="timerExercise"
              placeholder="如 30"
              placeholder-class="input-placeholder"
            />
          </view>
        </view>
        <view class="form-item half">
          <text class="form-label">运动强度</text>
          <picker
            :range="intensityLabels"
            :value="intensityIndex < 0 ? 0 : intensityIndex"
            :disabled="timerExercise"
            @change="onIntensityChange"
          >
            <view class="picker-box" :class="{ disabled: timerExercise }">
              <text :class="form.exerciseIntensity != null ? 'picker-value' : 'picker-placeholder'">
                {{ form.exerciseIntensity != null ? intensityLabel(form.exerciseIntensity) : '请选择' }}
              </text>
              <uni-icons type="arrowdown" size="14" color="#b1bfba" />
            </view>
          </picker>
        </view>
      </view>
      <text v-if="timerExercise" class="section-tip">当前由运动计时自动记录，结束后自动填充。</text>
    </view>

    <view class="card">
      <view class="card-title-row">
        <text class="card-title">🥗 饮食</text>
      </view>
      <text class="card-subtitle">记录早餐、饮食均衡与饮水量</text>

      <view class="form-item">
        <text class="form-label">今天吃早餐了吗</text>
        <view class="chip-row">
          <view
            v-for="item in yesNoOptions"
            :key="item.value"
            class="choice-chip"
            :class="{ active: form.dietBreakfast === item.value }"
            @click="form.dietBreakfast = item.value"
          >
            {{ item.label }}
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">饮食均衡程度</text>
        <view class="chip-row">
          <view
            v-for="item in dietBalanceOptions"
            :key="item.value"
            class="choice-chip"
            :class="{ active: form.dietBalance === item.value }"
            @click="form.dietBalance = item.value"
          >
            {{ item.label }}
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">饮水量（ml）</text>
        <view class="input-box">
          <input
            v-model="form.waterIntakeText"
            class="input"
            type="number"
            placeholder="如 1500"
            placeholder-class="input-placeholder"
          />
        </view>
      </view>
    </view>

    <view class="card">
      <view class="card-title-row">
        <text class="card-title">😊 情绪</text>
      </view>
      <text class="card-subtitle">记录今天的心情状态</text>

      <view class="form-item">
        <text class="form-label">心情</text>
        <view class="emotion-row">
          <view
            v-for="item in emotionOptions"
            :key="item.value"
            class="emotion-item"
            :class="{ active: form.emotionType === item.value }"
            @click="form.emotionType = item.value"
          >
            <text class="emotion-emoji">{{ item.emoji }}</text>
            <text class="emotion-label">{{ item.label }}</text>
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">心情评分</text>
        <view class="score-row">
          <view
            v-for="score in 5"
            :key="score"
            class="score-item"
            :class="{ active: form.moodScore === score }"
            @click="form.moodScore = form.moodScore === score ? null : score"
          >
            {{ score }}
          </view>
        </view>
        <text class="section-tip">1 分最低，5 分最好；再次点击可取消</text>
      </view>
    </view>

    <view class="card">
      <view class="card-title-row">
        <text class="card-title">今日感受</text>
        <text class="muted">{{ form.content.length }} / 500</text>
      </view>
      <view class="textarea-box" style="margin-top: 18rpx">
        <textarea
          v-model="form.content"
          class="textarea"
          maxlength="500"
          placeholder="今天有什么想记录的？身体状况、运动感受、心情变化……"
          placeholder-class="input-placeholder"
        />
      </view>
    </view>

    <view class="footer-bar">
      <button class="btn btn-primary btn-block" :disabled="saving || dateLoading" @click="handleSave">
        {{ saving ? '保存中…' : dateRecord ? '更新打卡记录' : '保存打卡记录' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  getActiveCheckinSession,
  pageMyCheckins,
  saveCheckin,
  updateCheckin,
} from '@/api/health-checkin'
import { getAccessToken } from '@/utils/auth'
import {
  calculateSleepHours,
  canEditCheckinDate,
  CHECKIN_DIET_BALANCE_OPTIONS,
  CHECKIN_EMOTION_OPTIONS,
  CHECKIN_EXERCISE_INTENSITY_OPTIONS,
  CHECKIN_EXERCISE_TYPES,
  CHECKIN_YES_NO_OPTIONS,
  dietBalanceLabel,
  earliestBackfillDate,
  exerciseIntensityLabel,
  formatClockTime,
  isExerciseTimerSource,
  isSleepTimerSource,
  todayDateString,
} from '@/utils/checkin'
import type { CheckinSave, CheckinSession, HealthCheckin } from '@/types/health'

interface EditForm {
  sleepTime: string
  wakeTime: string
  exerciseType: string
  exerciseDurationText: string
  exerciseIntensity: number | null
  dietBreakfast: number | null
  dietBalance: number | null
  waterIntakeText: string
  emotionType: number | null
  moodScore: number | null
  content: string
}

const today = todayDateString()
const earliestDate = earliestBackfillDate()
const formDate = ref(today)
const dateRecord = ref<HealthCheckin | null>(null)
const dateLoading = ref(false)
const saving = ref(false)
const activeSleep = ref<CheckinSession | null>(null)
const activeExercise = ref<CheckinSession | null>(null)
const exerciseTypeOptions = ref<string[]>([...CHECKIN_EXERCISE_TYPES])
const exerciseTypeIndex = ref(-1)
const intensityLabels = CHECKIN_EXERCISE_INTENSITY_OPTIONS.map((item) => item.label)
const intensityIndex = ref(-1)
const yesNoOptions = CHECKIN_YES_NO_OPTIONS
const dietBalanceOptions = CHECKIN_DIET_BALANCE_OPTIONS
const emotionOptions = CHECKIN_EMOTION_OPTIONS

const form = reactive<EditForm>({
  sleepTime: '',
  wakeTime: '',
  exerciseType: '',
  exerciseDurationText: '',
  exerciseIntensity: null,
  dietBreakfast: null,
  dietBalance: null,
  waterIntakeText: '',
  emotionType: null,
  moodScore: null,
  content: '',
})

const timerSleep = computed(
  () =>
    (formDate.value === today && Boolean(activeSleep.value)) ||
    isSleepTimerSource(dateRecord.value),
)
const timerExercise = computed(
  () =>
    (formDate.value === today && Boolean(activeExercise.value)) ||
    isExerciseTimerSource(dateRecord.value),
)
const sleepHours = computed(() =>
  form.sleepTime && form.wakeTime
    ? calculateSleepHours(form.sleepTime, form.wakeTime)
    : null,
)

function intensityLabel(value?: number | null): string {
  return exerciseIntensityLabel(value)
}

function resetForm(record: HealthCheckin | null): void {
  dateRecord.value = record
  form.sleepTime = record?.sleepTime ? formatClockTime(record.sleepTime) : ''
  form.wakeTime = record?.wakeTime ? formatClockTime(record.wakeTime) : ''
  form.exerciseType = record?.exerciseType || ''
  form.exerciseDurationText = record?.exerciseDuration != null ? String(record.exerciseDuration) : ''
  form.exerciseIntensity = record?.exerciseIntensity ?? null
  form.dietBreakfast = record?.dietBreakfast ?? null
  form.dietBalance = record?.dietBalance ?? null
  form.waterIntakeText = record?.waterIntake != null ? String(record.waterIntake) : ''
  form.emotionType = record?.emotionType ?? null
  form.moodScore = record?.moodScore ?? null
  form.content = record?.content || ''

  if (form.exerciseType && !exerciseTypeOptions.value.includes(form.exerciseType)) {
    exerciseTypeOptions.value = [...exerciseTypeOptions.value, form.exerciseType]
  }
  exerciseTypeIndex.value = exerciseTypeOptions.value.findIndex(
    (item) => item === form.exerciseType,
  )
  intensityIndex.value = CHECKIN_EXERCISE_INTENSITY_OPTIONS.findIndex(
    (item) => item.value === form.exerciseIntensity,
  )
}

async function loadDay(): Promise<void> {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  dateLoading.value = true
  try {
    const page = await pageMyCheckins({
      page: 1,
      size: 1,
      startDate: formDate.value,
      endDate: formDate.value,
    })
    resetForm(page.records?.[0] ?? null)
  } catch {
    resetForm(null)
  } finally {
    dateLoading.value = false
  }
}

async function loadActive(): Promise<void> {
  if (formDate.value !== today) {
    activeSleep.value = null
    activeExercise.value = null
    return
  }
  const results = await Promise.allSettled([
    getActiveCheckinSession(1),
    getActiveCheckinSession(2),
  ])
  activeSleep.value = results[0].status === 'fulfilled' ? results[0].value : null
  activeExercise.value = results[1].status === 'fulfilled' ? results[1].value : null
}

async function initialize(): Promise<void> {
  await Promise.all([loadDay(), loadActive()])
}

function onDateChange(event: { detail: { value: string } }): void {
  const value = event.detail.value
  if (!canEditCheckinDate(value)) {
    uni.showToast({ title: '仅支持补录最近 7 天', icon: 'none' })
    return
  }
  formDate.value = value
  initialize()
}

function onSleepTimeChange(event: { detail: { value: string } }): void {
  form.sleepTime = event.detail.value
}

function onWakeTimeChange(event: { detail: { value: string } }): void {
  form.wakeTime = event.detail.value
}

function onExerciseTypeChange(event: { detail: { value: string | number } }): void {
  const index = Number(event.detail.value)
  exerciseTypeIndex.value = index
  form.exerciseType = exerciseTypeOptions.value[index] || ''
}

function onIntensityChange(event: { detail: { value: string | number } }): void {
  const index = Number(event.detail.value)
  intensityIndex.value = index
  form.exerciseIntensity = CHECKIN_EXERCISE_INTENSITY_OPTIONS[index]?.value ?? null
}

function numberOrNull(value: string): number | null {
  const text = value.trim()
  if (!text) return null
  const number = Number(text)
  return Number.isFinite(number) ? number : null
}

function validate(): boolean {
  const hasSleep = Boolean(form.sleepTime)
  const hasWake = Boolean(form.wakeTime)
  if (!timerSleep.value && hasSleep !== hasWake) {
    uni.showToast({ title: '入睡时间和起床时间需同时填写', icon: 'none' })
    return false
  }
  if (!timerSleep.value && hasSleep && hasWake) {
    const hours = calculateSleepHours(form.sleepTime, form.wakeTime)
    if (hours == null || hours < 0.5 || hours > 24) {
      uni.showToast({ title: '睡眠时长需在 0.5-24 小时之间', icon: 'none' })
      return false
    }
  }

  if (!timerExercise.value) {
    const duration = numberOrNull(form.exerciseDurationText)
    const hasAny =
      Boolean(form.exerciseType.trim()) ||
      duration != null ||
      form.exerciseIntensity != null
    if (hasAny) {
      if (!form.exerciseType.trim()) {
        uni.showToast({ title: '请选择运动类型', icon: 'none' })
        return false
      }
      if (duration == null) {
        uni.showToast({ title: '请填写运动时长', icon: 'none' })
        return false
      }
      if (form.exerciseIntensity == null) {
        uni.showToast({ title: '请选择运动强度', icon: 'none' })
        return false
      }
      if (duration < 1 || duration > 600) {
        uni.showToast({ title: '运动时长需在 1-600 分钟之间', icon: 'none' })
        return false
      }
    }
  }

  if (form.content.length > 500) {
    uni.showToast({ title: '打卡内容不能超过 500 个字符', icon: 'none' })
    return false
  }

  if (!dateRecord.value && !hasAnyInput()) {
    uni.showToast({ title: '请至少填写一项打卡内容', icon: 'none' })
    return false
  }
  return true
}

function hasAnyInput(): boolean {
  return Boolean(
    (!timerSleep.value && (form.sleepTime || form.wakeTime)) ||
      (!timerExercise.value &&
        (form.exerciseType.trim() ||
          form.exerciseDurationText ||
          form.exerciseIntensity != null)) ||
      form.dietBreakfast != null ||
      form.dietBalance != null ||
      form.waterIntakeText ||
      form.emotionType != null ||
      form.moodScore != null ||
      form.content.trim(),
  )
}

function buildPayload(): CheckinSave {
  const payload: CheckinSave = {
    content: form.content.trim(),
  }
  if (!timerSleep.value && form.sleepTime && form.wakeTime) {
    payload.sleepTime = form.sleepTime
    payload.wakeTime = form.wakeTime
  }
  if (
    !timerExercise.value &&
    form.exerciseType.trim() &&
    numberOrNull(form.exerciseDurationText) != null &&
    form.exerciseIntensity != null
  ) {
    payload.exerciseType = form.exerciseType.trim()
    payload.exerciseDuration = numberOrNull(form.exerciseDurationText)
    payload.exerciseIntensity = form.exerciseIntensity
  }
  if (form.dietBreakfast != null) payload.dietBreakfast = form.dietBreakfast
  if (form.dietBalance != null) payload.dietBalance = form.dietBalance
  if (form.waterIntakeText.trim()) {
    payload.waterIntake = numberOrNull(form.waterIntakeText)
  }
  if (form.emotionType != null) payload.emotionType = form.emotionType
  if (form.moodScore != null) payload.moodScore = form.moodScore
  return payload
}

async function handleSave(): Promise<void> {
  if (saving.value || dateLoading.value) return
  if (!validate()) return
  saving.value = true
  try {
    const payload = buildPayload()
    if (dateRecord.value?.id) {
      await updateCheckin(dateRecord.value.id, payload)
    } else {
      await saveCheckin({ ...payload, checkinDate: formDate.value })
    }
    uni.showToast({ title: dateRecord.value ? '打卡已更新' : '打卡已保存', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack({ delta: 1 })
    }, 500)
  } catch {
    // 请求层已提示
  } finally {
    saving.value = false
  }
}

onLoad((options) => {
  const queryDate = options?.date
  if (queryDate && canEditCheckinDate(queryDate)) {
    formDate.value = queryDate
  }
  initialize()
})
</script>

<style scoped>
.date-card {
  margin-bottom: 20rpx;
}

.date-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.date-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 21rpx;
  color: #94a3b8;
}

.date-picker {
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

.date-status {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 18rpx;
  padding: 12rpx 16rpx;
  border-radius: 14rpx;
  background: #f2faf8;
  font-size: 21rpx;
  color: #417a70;
}

.timer-banner {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  margin-bottom: 20rpx;
  padding: 18rpx 22rpx;
  border-radius: 18rpx;
  background: #fff7e6;
  font-size: 22rpx;
  line-height: 1.6;
  color: #b45309;
}

.double-row {
  display: flex;
  gap: 18rpx;
}

.half {
  flex: 1;
  min-width: 0;
}

.picker-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 80rpx;
  padding: 0 22rpx;
  border-radius: 18rpx;
  background: #f6f9f8;
}

.picker-box.disabled,
.input-box.disabled {
  opacity: 0.55;
}

.section-tip {
  display: block;
  margin-top: 12rpx;
  font-size: 20rpx;
  line-height: 1.5;
  color: #a8b7b2;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.choice-chip {
  padding: 12rpx 28rpx;
  border-radius: 999rpx;
  background: #f4f8f6;
  color: #64748b;
  font-size: 24rpx;
  border: 2rpx solid transparent;
}

.choice-chip.active {
  background: #e6f7f3;
  border-color: #7fd9c8;
  color: #0f766e;
  font-weight: 600;
}

.emotion-row {
  display: flex;
  justify-content: space-between;
  gap: 8rpx;
}

.emotion-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  padding: 14rpx 4rpx;
  border-radius: 18rpx;
  border: 2rpx solid transparent;
  background: #f7faf9;
}

.emotion-item.active {
  background: #e6f7f3;
  border-color: #7fd9c8;
}

.emotion-emoji {
  font-size: 36rpx;
  line-height: 1;
}

.emotion-label {
  font-size: 20rpx;
  color: #64748b;
}

.emotion-item.active .emotion-label {
  color: #0f766e;
  font-weight: 600;
}

.score-row {
  display: flex;
  gap: 14rpx;
}

.score-item {
  width: 74rpx;
  height: 74rpx;
  border-radius: 50%;
  background: #f4f8f6;
  color: #64748b;
  font-size: 27rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid transparent;
}

.score-item.active {
  background: #0f9d8a;
  color: #ffffff;
  border-color: #0f9d8a;
}

.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -10rpx 30rpx rgba(15, 118, 110, 0.08);
}
</style>
