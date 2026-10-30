<script setup lang="ts">
import { computed } from 'vue'
import {
  Bicycle,
  Coffee,
  Edit,
  Moon,
  Sunny,
  Timer,
  VideoPause,
  VideoPlay,
} from '@element-plus/icons-vue'
import {
  checkinCompletionCount,
  dietBalanceLabel,
  emotionEmoji,
  emotionLabel,
  exerciseIntensityLabel,
  formatClockTime,
  formatDurationMinutes,
  formatElapsedClock,
  formatElapsedHuman,
  formatMoodScore,
  formatSleepHours,
  formatWaterIntake,
  todayDateString,
  yesNoTagType,
} from '@/utils/checkin'
import type { CheckinSession, HealthCheckin } from '@/types/health'

const props = defineProps<{
  today: HealthCheckin | null
  todayLoading?: boolean
  activeSleep: CheckinSession | null
  activeExercise: CheckinSession | null
  sleepElapsed: number
  exerciseElapsed: number
  sleepStarting?: boolean
  sleepEnding?: boolean
  exerciseStarting?: boolean
  exerciseEnding?: boolean
}>()

const emit = defineEmits<{
  'start-sleep': []
  'end-sleep': []
  'start-exercise': []
  'end-exercise': []
  manual: []
}>()

const completion = computed(() => checkinCompletionCount(props.today))
const todayLabel = computed(() => props.today?.checkinDate || todayDateString())
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const sleepCompleted = computed(() => Boolean(props.today?.sleepCompleted))
const exerciseCompleted = computed(() => Boolean(props.today?.exerciseCompleted))
const dietCompleted = computed(() => Boolean(props.today?.dietCompleted))
const emotionCompleted = computed(() => Boolean(props.today?.emotionCompleted))
const sleepManualDisabled = computed(() => props.today?.sleepSource === 1)
const exerciseManualDisabled = computed(() => props.today?.exerciseSource === 1)
const sleepRecordedManually = computed(() => props.today?.sleepSource === 2)
const exerciseRecordedManually = computed(() => props.today?.exerciseSource === 2)
</script>

<template>
  <div v-loading="todayLoading" class="today-dashboard">
    <section class="today-hero panel-card">
      <div class="hero-copy">
        <span class="hero-eyebrow">TODAY · {{ todayLabel }}</span>
        <h2>{{ greeting }}，今天也要好好照顾自己</h2>
        <p>记录睡眠、运动、饮食和情绪，让每一天都成为更好的自己。</p>
        <div class="hero-hint">
          <el-icon><Timer /></el-icon>
          完成任意一项即可开始今日打卡，之后可继续补全其他板块
        </div>
      </div>
      <div class="hero-progress">
        <el-progress
          type="circle"
          :percentage="completion * 25"
          :width="88"
          :stroke-width="7"
          color="#1f9d7a"
        >
          <template #default>
            <span class="progress-count">{{ completion }}/4</span>
            <span class="progress-label">已完成</span>
          </template>
        </el-progress>
        <div class="hero-status">
          <el-tag
            :type="today?.checkedIn ? 'success' : 'info'"
            effect="light"
          >
            {{ today?.checkedIn ? '今日已打卡' : '今日未打卡' }}
          </el-tag>
          <el-tag
            v-if="today?.dataQuality === 1"
            type="danger"
            effect="plain"
            size="small"
          >
            数据异常待复核
          </el-tag>
          <el-tag
            v-if="today?.backfilled"
            type="warning"
            effect="plain"
            size="small"
          >
            补录
          </el-tag>
        </div>
      </div>
    </section>

    <div class="dashboard-grid">
      <!-- 睡眠 -->
      <section
        class="dashboard-card panel-card"
        :class="{ completed: sleepCompleted, active: Boolean(activeSleep) }"
      >
        <div class="card-head">
          <div class="card-icon sleep-icon">
            <el-icon :size="20"><Moon /></el-icon>
          </div>
          <div class="card-title">
            <h3>睡眠</h3>
            <p>记录入睡与起床时间</p>
          </div>
          <el-tag
            :type="activeSleep ? 'primary' : sleepCompleted ? 'success' : 'info'"
            size="small"
            effect="light"
          >
            {{ activeSleep ? '计时中' : sleepCompleted ? '已记录' : '未记录' }}
          </el-tag>
        </div>

        <div v-if="activeSleep" class="timer-block sleep-timer">
          <div class="timer-top">
            <span class="timer-status">
              <i class="pulse-dot" />
              睡眠计时中
            </span>
            <span>{{ formatClockTime(activeSleep.startTime) }} 开始</span>
          </div>
          <strong class="timer-value">{{ formatElapsedClock(sleepElapsed) }}</strong>
          <span class="timer-human">
            已睡 {{ formatElapsedHuman(sleepElapsed) }}
          </span>
          <el-button
            type="primary"
            :icon="VideoPause"
            :loading="sleepEnding"
            class="timer-button"
            @click="emit('end-sleep')"
          >
            我起床了
          </el-button>
        </div>

        <template v-else>
          <div v-if="sleepCompleted" class="metric-rows">
            <div class="metric-row">
              <span>入睡 / 起床</span>
              <strong>
                {{ formatClockTime(today?.sleepTime) }} -
                {{ formatClockTime(today?.wakeTime) }}
              </strong>
            </div>
            <div class="metric-row">
              <span>睡眠时长</span>
              <strong>{{ formatSleepHours(today?.sleepDuration) }}</strong>
            </div>
            <div class="metric-row">
              <span>是否熬夜</span>
              <el-tag
                v-if="today?.stayUpLate != null"
                :type="yesNoTagType(today.stayUpLate)"
                size="small"
                effect="plain"
              >
                {{ today.stayUpLate ? '是' : '否' }}
              </el-tag>
              <span v-else class="muted-text">未填写</span>
            </div>
            <div class="metric-row">
              <span>数据来源</span>
              <span class="source-text">{{ today?.sleepSourceName || '—' }}</span>
            </div>
          </div>
          <div v-else class="empty-block">
            <el-icon :size="24"><Moon /></el-icon>
            <span>还没有记录今天的睡眠</span>
          </div>

          <div class="card-actions">
            <el-button
              v-if="!sleepRecordedManually"
              type="primary"
              plain
              :icon="VideoPlay"
              :loading="sleepStarting"
              @click="emit('start-sleep')"
            >
              开始睡眠
            </el-button>
            <span v-else class="disabled-hint">今日已手动记录睡眠</span>
            <el-button
              v-if="!sleepManualDisabled"
              link
              type="primary"
              :icon="Edit"
              @click="emit('manual')"
            >
              手动填写
            </el-button>
            <span v-else class="disabled-hint">已有计时记录，可补充其他板块</span>
          </div>
        </template>
      </section>

      <!-- 运动 -->
      <section
        class="dashboard-card panel-card"
        :class="{ completed: exerciseCompleted, active: Boolean(activeExercise) }"
      >
        <div class="card-head">
          <div class="card-icon exercise-icon">
            <el-icon :size="20"><Bicycle /></el-icon>
          </div>
          <div class="card-title">
            <h3>运动</h3>
            <p>记录运动类型、时长与强度</p>
          </div>
          <el-tag
            :type="activeExercise ? 'primary' : exerciseCompleted ? 'success' : 'info'"
            size="small"
            effect="light"
          >
            {{ activeExercise ? '计时中' : exerciseCompleted ? '已记录' : '未记录' }}
          </el-tag>
        </div>

        <div v-if="activeExercise" class="timer-block exercise-timer">
          <div class="timer-top">
            <span class="timer-status">
              <i class="pulse-dot" />
              {{ activeExercise.exerciseType || '运动' }}计时中
            </span>
            <span>{{ formatClockTime(activeExercise.startTime) }} 开始</span>
          </div>
          <strong class="timer-value">{{ formatElapsedClock(exerciseElapsed) }}</strong>
          <span class="timer-human">
            已运动 {{ formatElapsedHuman(exerciseElapsed) }} ·
            {{ exerciseIntensityLabel(activeExercise.exerciseIntensity) }}
          </span>
          <el-button
            type="primary"
            :icon="VideoPause"
            :loading="exerciseEnding"
            class="timer-button"
            @click="emit('end-exercise')"
          >
            结束运动
          </el-button>
        </div>

        <template v-else>
          <div v-if="exerciseCompleted" class="metric-rows">
            <div class="metric-row">
              <span>运动类型</span>
              <strong>{{ today?.exerciseType || '—' }}</strong>
            </div>
            <div class="metric-row">
              <span>运动时长</span>
              <strong>{{ formatDurationMinutes(today?.exerciseDuration) }}</strong>
            </div>
            <div class="metric-row">
              <span>运动强度</span>
              <strong>{{ today?.exerciseIntensityName || exerciseIntensityLabel(today?.exerciseIntensity) }}</strong>
            </div>
            <div class="metric-row">
              <span>是否达标</span>
              <el-tag
                v-if="today?.exerciseSufficient != null"
                :type="yesNoTagType(today.exerciseSufficient)"
                size="small"
                effect="plain"
              >
                {{ today.exerciseSufficient ? '是' : '否' }}
              </el-tag>
              <span v-else class="muted-text">未填写</span>
            </div>
          </div>
          <div v-else class="empty-block">
            <el-icon :size="24"><Bicycle /></el-icon>
            <span>还没有记录今天的运动</span>
          </div>

          <div class="card-actions">
            <el-button
              v-if="!exerciseRecordedManually"
              type="primary"
              plain
              :icon="VideoPlay"
              :loading="exerciseStarting"
              @click="emit('start-exercise')"
            >
              开始运动
            </el-button>
            <span v-else class="disabled-hint">今日已手动记录运动</span>
            <el-button
              v-if="!exerciseManualDisabled"
              link
              type="primary"
              :icon="Edit"
              @click="emit('manual')"
            >
              手动填写
            </el-button>
            <span v-else class="disabled-hint">已有计时记录，可补充其他板块</span>
          </div>
        </template>
      </section>

      <!-- 饮食 -->
      <section
        class="dashboard-card panel-card"
        :class="{ completed: dietCompleted }"
      >
        <div class="card-head">
          <div class="card-icon diet-icon">
            <el-icon :size="20"><Coffee /></el-icon>
          </div>
          <div class="card-title">
            <h3>饮食</h3>
            <p>早餐、均衡度与饮水量</p>
          </div>
          <el-tag
            :type="dietCompleted ? 'success' : 'info'"
            size="small"
            effect="light"
          >
            {{ dietCompleted ? '已记录' : '未记录' }}
          </el-tag>
        </div>

        <div class="diet-stats">
          <div class="diet-stat">
            <span>早餐</span>
            <span
              v-if="today?.dietBreakfast != null"
              class="diet-value-tag"
              :class="today.dietBreakfast ? 'is-yes' : 'is-no'"
            >
              {{ today.dietBreakfast ? '吃了' : '没吃' }}
            </span>
            <strong v-else class="muted-text">未填写</strong>
          </div>
          <div class="diet-stat">
            <span>饮食均衡</span>
            <strong>{{ dietBalanceLabel(today?.dietBalance) }}</strong>
          </div>
          <div class="diet-stat">
            <span>饮水量</span>
            <strong>{{ formatWaterIntake(today?.waterIntake) }}</strong>
          </div>
        </div>

        <div class="card-actions">
          <el-button link type="primary" :icon="Edit" @click="emit('manual')">
            {{ dietCompleted ? '修改饮食记录' : '填写饮食记录' }}
          </el-button>
        </div>
      </section>

      <!-- 情绪 -->
      <section
        class="dashboard-card panel-card"
        :class="{ completed: emotionCompleted }"
      >
        <div class="card-head">
          <div class="card-icon emotion-icon">
            <el-icon :size="20"><Sunny /></el-icon>
          </div>
          <div class="card-title">
            <h3>情绪</h3>
            <p>记录今天的心情与感受</p>
          </div>
          <el-tag
            :type="emotionCompleted ? 'success' : 'info'"
            size="small"
            effect="light"
          >
            {{ emotionCompleted ? '已记录' : '未记录' }}
          </el-tag>
        </div>

        <div v-if="emotionCompleted" class="emotion-block">
          <span class="emotion-emoji">{{ emotionEmoji(today?.emotionType) }}</span>
          <div class="emotion-info">
            <strong>{{ today?.emotionTypeName || emotionLabel(today?.emotionType) }}</strong>
            <span>心情评分 {{ formatMoodScore(today?.moodScore) }}</span>
            <p v-if="today?.content">{{ today.content }}</p>
          </div>
        </div>
        <div v-else class="empty-block emotion-empty">
          <el-icon :size="24"><Sunny /></el-icon>
          <span>还没有记录今天的心情</span>
        </div>

        <div class="card-actions">
          <el-button link type="primary" :icon="Edit" @click="emit('manual')">
            {{ emotionCompleted ? '修改情绪记录' : '填写情绪记录' }}
          </el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.today-dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.today-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 26px;
  padding: 24px 28px;
  overflow: hidden;
  background:
    radial-gradient(circle at 92% 0%, rgba(61, 220, 151, 0.18), transparent 34%),
    linear-gradient(120deg, #f4fbf8 0%, #e8f7f1 55%, #dff4eb 100%);
  border-color: #d8ede5;
}

.hero-copy {
  min-width: 0;
}

.hero-eyebrow {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 2px;
  color: var(--hp-primary);
}

.hero-copy h2 {
  margin: 7px 0 7px;
  font-size: clamp(21px, 2vw, 27px);
  font-weight: 700;
  color: var(--hp-text);
}

.hero-copy > p {
  font-size: 12.5px;
  color: var(--hp-text-secondary);
}

.hero-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 11px;
  color: var(--hp-text-muted);
}

.hero-progress {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 18px;
  padding-left: 24px;
  border-left: 1px solid rgba(31, 157, 122, 0.16);
}

.hero-progress :deep(.el-progress__text) {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.progress-count {
  font-size: 18px;
  font-weight: 700;
  color: var(--hp-primary-dark);
}

.progress-label {
  margin-top: 2px;
  font-size: 9px;
  color: var(--hp-text-muted);
}

.hero-status {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.dashboard-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 22px;
  transition: box-shadow 0.22s ease, transform 0.22s ease;
}

.dashboard-card:hover {
  box-shadow: 0 14px 34px rgba(23, 51, 44, 0.07);
  transform: translateY(-2px);
}

.dashboard-card.completed {
  border-color: #d6ebe3;
}

.dashboard-card.active {
  border-color: #c6e8db;
  box-shadow: 0 12px 30px rgba(31, 157, 122, 0.09);
}

.card-head {
  display: flex;
  align-items: flex-start;
  gap: 11px;
}

.card-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
}

.sleep-icon {
  color: #7c6ce0;
  background: #efedff;
}

.exercise-icon {
  color: #1f9d7a;
  background: #e3f6ef;
}

.diet-icon {
  color: #d98b2b;
  background: #fff3e2;
}

.emotion-icon {
  color: #d966a8;
  background: #fdeef6;
}

.card-title {
  flex: 1;
  min-width: 0;
}

.card-title h3 {
  margin-bottom: 3px;
  font-size: 15px;
  font-weight: 700;
  color: var(--hp-text);
}

.card-title p {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.metric-rows {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 13px 14px;
  background: #fbfdfc;
  border: 1px solid #edf3f0;
  border-radius: 11px;
}

.metric-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
}

.metric-row > span:first-child {
  color: var(--hp-text-muted);
}

.metric-row strong {
  font-weight: 500;
  color: var(--hp-text);
  text-align: right;
}

.source-text {
  font-size: 11px;
  color: var(--hp-text-secondary);
}

.empty-block {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 100px;
  font-size: 12px;
  color: var(--hp-text-muted);
  background: #fbfdfc;
  border: 1px dashed #e3ece8;
  border-radius: 11px;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
}

.disabled-hint {
  font-size: 10.5px;
  color: var(--hp-text-muted);
}

/* ---------------- 计时中 ---------------- */
.timer-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 18px 16px;
  color: #eafff7;
  border-radius: 14px;
}

.sleep-timer {
  background:
    radial-gradient(circle at 85% 10%, rgba(124, 108, 224, 0.35), transparent 40%),
    linear-gradient(135deg, #28316a, #3f4c9a);
}

.exercise-timer {
  background:
    radial-gradient(circle at 85% 10%, rgba(61, 220, 151, 0.3), transparent 40%),
    linear-gradient(135deg, #0d4439, #14745a);
}

.timer-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  font-size: 10.5px;
  color: rgba(234, 255, 247, 0.6);
}

.timer-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #eafff7;
}

.pulse-dot {
  width: 6px;
  height: 6px;
  background: #7bf0bb;
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(123, 240, 187, 0.14);
  animation: pulse-dot 1.8s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%,
  100% {
    opacity: 0.65;
    transform: scale(0.9);
  }
  50% {
    opacity: 1;
    transform: scale(1.15);
  }
}

.timer-value {
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 1px;
  line-height: 1.2;
  color: #fff;
  font-variant-numeric: tabular-nums;
}

.timer-human {
  font-size: 10.5px;
  color: rgba(234, 255, 247, 0.55);
}

.timer-button {
  width: 100%;
  margin-top: 10px;
}

/* ---------------- 饮食 / 情绪 ---------------- */
.diet-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.diet-stat {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 11px 12px;
  background: #fbfdfc;
  border: 1px solid #edf3f0;
  border-radius: 10px;
}

.diet-stat > span:first-child {
  font-size: 10.5px;
  color: var(--hp-text-muted);
}

.diet-value-tag {
  display: inline-flex;
  align-self: flex-start;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  padding: 0 8px;
  font-size: 11px;
  font-weight: 500;
  line-height: 20px;
  border-radius: 5px;
}

.diet-value-tag.is-yes {
  color: #1f9d7a;
  background: #e5f5ef;
  border: 1px solid #c6e9dc;
}

.diet-value-tag.is-no {
  color: #c58a28;
  background: #fff4e2;
  border: 1px solid #f2dfb8;
}

.diet-stat strong {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--hp-text);
}

.emotion-block {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 100px;
  padding: 14px 16px;
  background: #fff9fc;
  border: 1px solid #f8e6ef;
  border-radius: 11px;
}

.emotion-emoji {
  font-size: 38px;
}

.emotion-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.emotion-info strong {
  font-size: 14px;
  color: var(--hp-text);
}

.emotion-info > span {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.emotion-info p {
  display: -webkit-box;
  overflow: hidden;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--hp-text-secondary);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.emotion-empty {
  background: #fffafc;
  border-color: #f4e5ec;
}

@media (max-width: 1100px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .today-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-progress {
    width: 100%;
    padding-left: 0;
    border-left: none;
  }
}

@media (max-width: 600px) {
  .today-hero {
    padding: 20px 18px;
  }

  .hero-status {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .dashboard-card {
    padding: 17px 15px;
  }

  .diet-stats {
    grid-template-columns: 1fr;
  }

  .timer-value {
    font-size: 28px;
  }
}
</style>
