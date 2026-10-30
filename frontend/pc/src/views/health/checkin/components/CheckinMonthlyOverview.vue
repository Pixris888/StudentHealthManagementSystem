<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  formatDurationMinutes,
  formatMonthLabel,
  formatSleepHours,
  formatWaterIntake,
} from '@/utils/checkin'
import type { CheckinSummary, HealthCheckin } from '@/types/health'

const props = defineProps<{
  month: string
  records: HealthCheckin[]
  summary: CheckinSummary | null
  loading?: boolean
}>()

const emit = defineEmits<{
  'month-change': [month: string]
  select: [date: string, record: HealthCheckin | null]
}>()

function dateToMonth(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

const calendarDate = ref(new Date())

const recordMap = computed(() => {
  const map = new Map<string, HealthCheckin>()
  for (const record of props.records) {
    if (record.checkinDate) {
      map.set(record.checkinDate, record)
    }
  }
  return map
})

const rate = computed(() => {
  const value = Number(props.summary?.checkinRate ?? 0)
  return Math.min(100, Math.max(0, value))
})

watch(
  () => props.month,
  (month) => {
    if (!month) return
    if (dateToMonth(calendarDate.value) === month) return
    const [year, monthIndex] = month.split('-').map(Number)
    if (!year || !monthIndex) return
    calendarDate.value = new Date(year, monthIndex - 1, 1)
  },
)

watch(calendarDate, (date) => {
  const month = dateToMonth(date)
  if (month !== props.month) {
    emit('month-change', month)
  }
})

function dayNumber(day: string): string {
  return day.slice(8, 10)
}

function recordOf(day: string, type: string): HealthCheckin | null {
  if (type !== 'current-month') return null
  return recordMap.value.get(day) ?? null
}

function handleCellClick(day: string, type: string, record: HealthCheckin | null): void {
  if (type !== 'current-month') return
  emit('select', day, record)
}
</script>

<template>
  <section v-loading="loading" class="monthly-grid">
    <div class="panel-card calendar-card">
      <div class="card-head">
        <div>
          <h3>月度打卡日历</h3>
          <p>{{ formatMonthLabel(month) }} · 点击有记录的日期查看详情</p>
        </div>
        <div class="legend">
          <span><i class="dot sleep" />睡眠</span>
          <span><i class="dot exercise" />运动</span>
          <span><i class="dot diet" />饮食</span>
          <span><i class="dot emotion" />情绪</span>
          <span><i class="dot abnormal" />异常</span>
        </div>
      </div>

      <el-calendar
        v-model="calendarDate"
        :first-day-of-week="1"
        class="checkin-calendar"
      >
        <template #date-cell="{ data }">
          <div
            class="calendar-cell"
            :class="{
              'other-month': data.type !== 'current-month',
              checked: Boolean(recordOf(data.day, data.type)),
              abnormal: recordOf(data.day, data.type)?.dataQuality === 1,
            }"
            @click="handleCellClick(data.day, data.type, recordOf(data.day, data.type))"
          >
            <span class="day-number">{{ dayNumber(data.day) }}</span>
            <template
              v-if="recordOf(data.day, data.type)"
            >
              <div class="day-dots">
                <i
                  v-if="recordOf(data.day, data.type)?.sleepCompleted"
                  class="dot sleep"
                />
                <i
                  v-if="recordOf(data.day, data.type)?.exerciseCompleted"
                  class="dot exercise"
                />
                <i
                  v-if="recordOf(data.day, data.type)?.dietCompleted"
                  class="dot diet"
                />
                <i
                  v-if="recordOf(data.day, data.type)?.emotionCompleted"
                  class="dot emotion"
                />
              </div>
              <span
                v-if="recordOf(data.day, data.type)?.stayUpLate === 1"
                class="cell-badge"
              >
                熬夜
              </span>
            </template>
          </div>
        </template>
      </el-calendar>
    </div>

    <div class="panel-card summary-card">
      <div class="card-head">
        <div>
          <h3>本月概览</h3>
          <p>{{ formatMonthLabel(month) }} 打卡统计</p>
        </div>
      </div>

      <div class="rate-block">
        <el-progress
          type="dashboard"
          :percentage="rate"
          :width="104"
          :stroke-width="8"
          color="#1f9d7a"
        >
          <template #default="{ percentage }">
            <span class="rate-value">{{ percentage }}%</span>
            <span class="rate-label">打卡率</span>
          </template>
        </el-progress>
        <div class="rate-meta">
          <strong>{{ summary?.checkinDays ?? 0 }}<small>/{{ summary?.monthDays ?? 0 }} 天</small></strong>
          <span>本月已打卡</span>
          <el-tag type="success" effect="plain" size="small">
            连续 {{ summary?.consecutiveDays ?? 0 }} 天
          </el-tag>
        </div>
      </div>

      <div class="metric-grid">
        <div class="metric-item">
          <span>平均睡眠</span>
          <strong>{{ formatSleepHours(summary?.avgSleepDuration) }}</strong>
        </div>
        <div class="metric-item">
          <span>运动天数</span>
          <strong>{{ summary?.exerciseDays ?? 0 }} 天</strong>
        </div>
        <div class="metric-item">
          <span>运动总时长</span>
          <strong>{{ formatDurationMinutes(summary?.totalExerciseMinutes) }}</strong>
        </div>
        <div class="metric-item">
          <span>异常待复核</span>
          <strong :class="{ danger: (summary?.abnormalCount ?? 0) > 0 }">
            {{ summary?.abnormalCount ?? 0 }} 条
          </strong>
        </div>
      </div>

      <div class="mini-list">
        <div>
          <span>熬夜次数</span>
          <strong>{{ summary?.lateCount ?? 0 }} 次</strong>
        </div>
        <div>
          <span>早餐天数</span>
          <strong>{{ summary?.breakfastDays ?? 0 }} 天</strong>
        </div>
        <div>
          <span>平均饮水</span>
          <strong>{{ formatWaterIntake(summary?.avgWaterIntake) }}</strong>
        </div>
        <div>
          <span>平均心情</span>
          <strong>{{ summary?.avgMoodScore == null ? '—' : `${summary.avgMoodScore} / 5` }}</strong>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.monthly-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(300px, 1fr);
  gap: 18px;
  align-items: stretch;
}

.calendar-card,
.summary-card {
  padding: 20px 22px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.card-head h3 {
  margin-bottom: 4px;
  font-size: 15.5px;
  font-weight: 700;
  color: var(--hp-text);
}

.card-head p {
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 10px;
  color: var(--hp-text-muted);
}

.legend span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot.sleep {
  background: #7c6ce0;
}

.dot.exercise {
  background: #1f9d7a;
}

.dot.diet {
  background: #d98b2b;
}

.dot.emotion {
  background: #d966a8;
}

.dot.abnormal {
  background: #e05b5b;
}

.checkin-calendar {
  --el-calendar-border: transparent;
  background: transparent;
}

.checkin-calendar :deep(.el-calendar__header) {
  padding: 0 0 10px;
}

.checkin-calendar :deep(.el-calendar__title) {
  font-size: 13px;
  font-weight: 600;
  color: var(--hp-text-secondary);
}

.checkin-calendar :deep(.el-calendar__button-group) {
  margin-left: auto;
}

.checkin-calendar :deep(.el-calendar-table thead th) {
  padding: 6px 0;
  font-size: 10.5px;
  font-weight: 500;
  color: var(--hp-text-muted);
}

.checkin-calendar :deep(.el-calendar-table .el-calendar-day) {
  height: 58px;
  padding: 3px;
}

.checkin-calendar :deep(.el-calendar-table td) {
  border-color: #f0f5f2;
}

.checkin-calendar :deep(.el-calendar-table td.is-selected) {
  background: transparent;
}

.checkin-calendar :deep(.el-calendar-table .el-calendar-day:hover) {
  background: #f3faf7;
}

.calendar-cell {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 100%;
  height: 100%;
  padding: 4px 5px;
  border-radius: 7px;
  transition: background 0.2s ease;
}

.calendar-cell.checked {
  background: #f1fbf7;
  box-shadow: inset 0 0 0 1px #d8f0e7;
}

.calendar-cell.abnormal {
  background: #fff7f7;
  box-shadow: inset 0 0 0 1px #f2dada;
}

.calendar-cell.other-month {
  opacity: 0.25;
  pointer-events: none;
}

.day-number {
  font-size: 11px;
  font-weight: 600;
  color: var(--hp-text-secondary);
}

.calendar-cell.checked .day-number {
  color: var(--hp-primary-dark);
}

.day-dots {
  display: flex;
  gap: 3px;
  align-items: center;
}

.day-dots .dot {
  width: 5px;
  height: 5px;
}

.cell-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  padding: 0 3px;
  font-size: 8px;
  line-height: 12px;
  color: #a86f16;
  background: #fff0d5;
  border-radius: 3px;
}

/* ---------------- 概览 ---------------- */
.rate-block {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px 16px;
  margin-bottom: 14px;
  background: #f7fbf9;
  border: 1px solid #e6f0ec;
  border-radius: 14px;
}

.rate-block :deep(.el-progress__text) {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.rate-value {
  font-size: 19px;
  font-weight: 700;
  color: var(--hp-primary-dark);
}

.rate-label {
  margin-top: 2px;
  font-size: 9px;
  color: var(--hp-text-muted);
}

.rate-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.rate-meta strong {
  font-size: 20px;
  font-weight: 700;
  color: var(--hp-text);
}

.rate-meta strong small {
  margin-left: 2px;
  font-size: 10px;
  font-weight: 400;
  color: var(--hp-text-muted);
}

.rate-meta > span {
  font-size: 10.5px;
  color: var(--hp-text-muted);
}

.rate-meta :deep(.el-tag) {
  align-self: flex-start;
  margin-top: 3px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 12px;
  background: #fbfdfc;
  border: 1px solid #edf3f0;
  border-radius: 9px;
}

.metric-item span {
  font-size: 10.5px;
  color: var(--hp-text-muted);
}

.metric-item strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--hp-text);
}

.metric-item strong.danger {
  color: var(--hp-danger);
}

.mini-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px dashed #e8f0ed;
}

.mini-list > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 11px;
  color: var(--hp-text-muted);
}

.mini-list strong {
  font-weight: 500;
  color: var(--hp-text-secondary);
}

@media (max-width: 1200px) {
  .monthly-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .calendar-card,
  .summary-card {
    padding: 16px 14px;
  }

  .legend {
    display: none;
  }

  .checkin-calendar :deep(.el-calendar-table .el-calendar-day) {
    height: 44px;
    padding: 1px;
  }

  .calendar-cell {
    padding: 2px 3px;
  }

  .cell-badge {
    display: none;
  }

  .rate-block {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
