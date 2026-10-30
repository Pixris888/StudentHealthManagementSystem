<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Refresh, Search, View } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useIsMobile } from '@/utils/useIsMobile'
import {
  endCheckinSession,
  getActiveCheckinSession,
  getCheckinDetail,
  getCheckinSummary,
  getTodayCheckin,
  pageAllCheckins,
  pageMyCheckins,
  pageMyCheckinSessions,
  startCheckinSession,
} from '@/api/health-checkin'
import {
  canEditCheckinDate,
  CHECKIN_DATA_QUALITY_OPTIONS,
  CHECKIN_SESSION_STATUS_OPTIONS,
  CHECKIN_SESSION_TYPE_OPTIONS,
  CHECKIN_YES_NO_OPTIONS,
  currentMonthString,
  dataQualityTagType,
  elapsedSecondsSince,
  emotionEmoji,
  formatClockTime,
  formatDateTimeShort,
  formatDurationMinutes,
  formatSleepHours,
  formatWaterIntake,
  monthBounds,
  sessionStatusTagType,
  sessionTypeTagType,
  todayDateString,
  yesNoTagType,
} from '@/utils/checkin'
import { formatDate } from '@/utils/health'
import type {
  CheckinSession,
  CheckinSummary,
  HealthCheckin,
} from '@/types/health'
import CheckinTodayDashboard from './components/CheckinTodayDashboard.vue'
import CheckinFormDialog from './components/CheckinFormDialog.vue'
import CheckinPhotoPanel from './components/CheckinPhotoPanel.vue'
import CheckinMonthlyOverview from './components/CheckinMonthlyOverview.vue'
import CheckinDetailDrawer from './components/CheckinDetailDrawer.vue'
import ExerciseStartDialog from './components/ExerciseStartDialog.vue'

interface ExerciseStartPayload {
  exerciseType: string
  exerciseIntensity: number
}

const userStore = useUserStore()
const isMobile = useIsMobile(900)
const roles = computed(() => userStore.roles)
const isManageMode = computed(
  () =>
    roles.value.includes('DOCTOR_ADMIN') ||
    roles.value.includes('SUPER_ADMIN'),
)
const isDoctor = computed(
  () =>
    roles.value.includes('DOCTOR_ADMIN') &&
    !roles.value.includes('SUPER_ADMIN'),
)
const isStudentMode = computed(
  () => !isManageMode.value && userStore.userInfo?.userType === 1,
)

/* ---------------- 学生：今日与计时 ---------------- */
const today = ref<HealthCheckin | null>(null)
const todayLoading = ref(false)
const activeSleep = ref<CheckinSession | null>(null)
const activeExercise = ref<CheckinSession | null>(null)
const sleepElapsed = ref(0)
const exerciseElapsed = ref(0)
const sleepStarting = ref(false)
const sleepEnding = ref(false)
const exerciseStarting = ref(false)
const exerciseEnding = ref(false)
const exerciseStartVisible = ref(false)

let timerId: number | undefined

function updateElapsed(): void {
  sleepElapsed.value = activeSleep.value
    ? elapsedSecondsSince(activeSleep.value.startTime)
    : 0
  exerciseElapsed.value = activeExercise.value
    ? elapsedSecondsSince(activeExercise.value.startTime)
    : 0
}

function startTimerLoop(): void {
  updateElapsed()
  timerId = window.setInterval(updateElapsed, 1000)
}

async function loadToday(): Promise<void> {
  if (!isStudentMode.value) return
  todayLoading.value = true
  try {
    today.value = await getTodayCheckin()
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    todayLoading.value = false
  }
}

async function loadActiveSessions(): Promise<void> {
  if (!isStudentMode.value) return
  const [sleepResult, exerciseResult] = await Promise.allSettled([
    getActiveCheckinSession(1),
    getActiveCheckinSession(2),
  ])
  if (sleepResult.status === 'fulfilled') {
    activeSleep.value = sleepResult.value
  }
  if (exerciseResult.status === 'fulfilled') {
    activeExercise.value = exerciseResult.value
  }
  updateElapsed()
}

/* ---------------- 学生：月度概览 ---------------- */
const overviewMonth = ref(currentMonthString())
const monthRecords = ref<HealthCheckin[]>([])
const summary = ref<CheckinSummary | null>(null)
const overviewLoading = ref(false)

async function loadMonth(month: string): Promise<void> {
  if (!isStudentMode.value) return
  overviewMonth.value = month
  overviewLoading.value = true
  const { startDate, endDate } = monthBounds(month)
  const [summaryResult, recordsResult] = await Promise.allSettled([
    getCheckinSummary(month),
    pageMyCheckins({ page: 1, size: 100, startDate, endDate }),
  ])
  if (summaryResult.status === 'fulfilled') {
    summary.value = summaryResult.value
  }
  if (recordsResult.status === 'fulfilled') {
    monthRecords.value = recordsResult.value.records
  }
  overviewLoading.value = false
}

function handleMonthChange(month: string): void {
  loadMonth(month)
}

/* ---------------- 学生：历史打卡 ---------------- */
const historyTab = ref('checkins')
const historyDateRange = ref<[string, string] | null>(null)
const historyStayUpLate = ref<number | undefined>(undefined)
const historyExerciseSufficient = ref<number | undefined>(undefined)
const historyDataQuality = ref<number | undefined>(undefined)
const historyQuery = reactive({ page: 1, size: 10 })
const historyRecords = ref<HealthCheckin[]>([])
const historyTotal = ref(0)
const historyLoading = ref(false)

function optionalNumber(value: unknown): number | undefined {
  if (value === '' || value == null) return undefined
  const num = Number(value)
  return Number.isNaN(num) ? undefined : num
}

async function loadHistory(): Promise<void> {
  if (!isStudentMode.value) return
  historyLoading.value = true
  try {
    const range = historyDateRange.value
    const page = await pageMyCheckins({
      page: historyQuery.page,
      size: historyQuery.size,
      startDate: Array.isArray(range) ? range[0] : undefined,
      endDate: Array.isArray(range) ? range[1] : undefined,
      stayUpLate: optionalNumber(historyStayUpLate.value),
      exerciseSufficient: optionalNumber(historyExerciseSufficient.value),
      dataQuality: optionalNumber(historyDataQuality.value),
    })
    historyRecords.value = page.records
    historyTotal.value = page.total
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    historyLoading.value = false
  }
}

function handleHistorySearch(): void {
  historyQuery.page = 1
  loadHistory()
}

function resetHistoryFilters(): void {
  historyDateRange.value = null
  historyStayUpLate.value = undefined
  historyExerciseSufficient.value = undefined
  historyDataQuality.value = undefined
  historyQuery.page = 1
  loadHistory()
}

function handleHistorySizeChange(): void {
  historyQuery.page = 1
  loadHistory()
}

/* ---------------- 学生：计时会话历史 ---------------- */
const sessionDateRange = ref<[string, string] | null>(null)
const sessionTypeFilter = ref<number | undefined>(undefined)
const sessionStatusFilter = ref<number | undefined>(undefined)
const sessionQuery = reactive({ page: 1, size: 10 })
const sessions = ref<CheckinSession[]>([])
const sessionTotal = ref(0)
const sessionLoading = ref(false)

async function loadSessions(): Promise<void> {
  if (!isStudentMode.value) return
  sessionLoading.value = true
  try {
    const range = sessionDateRange.value
    const page = await pageMyCheckinSessions({
      page: sessionQuery.page,
      size: sessionQuery.size,
      sessionType: optionalNumber(sessionTypeFilter.value),
      status: optionalNumber(sessionStatusFilter.value),
      startDate: Array.isArray(range) ? range[0] : undefined,
      endDate: Array.isArray(range) ? range[1] : undefined,
    })
    sessions.value = page.records
    sessionTotal.value = page.total
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    sessionLoading.value = false
  }
}

function handleSessionSearch(): void {
  sessionQuery.page = 1
  loadSessions()
}

function resetSessionFilters(): void {
  sessionDateRange.value = null
  sessionTypeFilter.value = undefined
  sessionStatusFilter.value = undefined
  sessionQuery.page = 1
  loadSessions()
}

function handleSessionSizeChange(): void {
  sessionQuery.page = 1
  loadSessions()
}

async function refreshStudentData(): Promise<void> {
  await Promise.allSettled([
    loadToday(),
    loadActiveSessions(),
    loadMonth(overviewMonth.value),
    loadHistory(),
    loadSessions(),
  ])
}

/* ---------------- 学生：计时操作 ---------------- */
async function handleStartSleep(): Promise<void> {
  if (sleepStarting.value || activeSleep.value) return
  sleepStarting.value = true
  try {
    activeSleep.value = await startCheckinSession({ sessionType: 1 })
    updateElapsed()
    ElMessage.success('睡眠计时已开始，晚安')
    loadSessions()
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    sleepStarting.value = false
  }
}

async function handleEndSleep(): Promise<void> {
  if (!activeSleep.value || sleepEnding.value) return
  try {
    await ElMessageBox.confirm('确定结束本次睡眠计时吗？', '我起床了', {
      type: 'info',
      confirmButtonText: '结束计时',
      cancelButtonText: '再睡会',
    })
  } catch {
    return
  }

  sleepEnding.value = true
  try {
    const result = await endCheckinSession(activeSleep.value.id)
    activeSleep.value = null
    sleepElapsed.value = 0
    if (result.session.status === 2) {
      ElMessage.warning(
        result.session.abnormalReason || '本次睡眠计时已作废',
      )
    } else {
      ElMessage.success(
        `睡眠打卡完成，本次时长 ${formatDurationMinutes(result.session.durationMinutes)}`,
      )
    }
    await refreshStudentData()
  } catch {
    await loadActiveSessions()
  } finally {
    sleepEnding.value = false
  }
}

function handleOpenExerciseStart(): void {
  if (activeExercise.value) return
  exerciseStartVisible.value = true
}

async function handleStartExercise(payload: ExerciseStartPayload): Promise<void> {
  if (exerciseStarting.value || activeExercise.value) return
  exerciseStarting.value = true
  try {
    activeExercise.value = await startCheckinSession({
      sessionType: 2,
      exerciseType: payload.exerciseType,
      exerciseIntensity: payload.exerciseIntensity,
    })
    exerciseStartVisible.value = false
    updateElapsed()
    ElMessage.success('运动计时已开始')
    loadSessions()
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    exerciseStarting.value = false
  }
}

async function handleEndExercise(): Promise<void> {
  if (!activeExercise.value || exerciseEnding.value) return
  try {
    await ElMessageBox.confirm('确定结束本次运动计时吗？', '结束运动', {
      type: 'info',
      confirmButtonText: '结束计时',
      cancelButtonText: '继续运动',
    })
  } catch {
    return
  }

  exerciseEnding.value = true
  try {
    const result = await endCheckinSession(activeExercise.value.id)
    activeExercise.value = null
    exerciseElapsed.value = 0
    if (result.session.status === 2) {
      ElMessage.warning(
        result.session.abnormalReason || '本次运动计时已作废',
      )
    } else {
      ElMessage.success(
        `运动打卡完成，本次时长 ${formatDurationMinutes(result.session.durationMinutes)}`,
      )
    }
    await refreshStudentData()
  } catch {
    await loadActiveSessions()
  } finally {
    exerciseEnding.value = false
  }
}

/* ---------------- 学生：手动填写 / 编辑 / 详情 ---------------- */
const formVisible = ref(false)
const formMode = ref<'manual' | 'edit'>('manual')
const formRecord = ref<HealthCheckin | null>(null)
const formDate = ref(todayDateString())

function openManual(date = todayDateString()): void {
  formMode.value = 'manual'
  formRecord.value = null
  formDate.value = date
  formVisible.value = true
}

function openEdit(record: HealthCheckin): void {
  if (!record.id) return
  if (!canEditCheckinDate(record.checkinDate)) {
    ElMessage.warning('只能修改最近 7 天的打卡记录')
    return
  }
  formMode.value = 'edit'
  formRecord.value = record
  formDate.value = record.checkinDate ?? todayDateString()
  formVisible.value = true
}

function handleFormSaved(): void {
  refreshStudentData()
}

function handlePhotoChanged(): void {
  Promise.allSettled([loadToday(), loadHistory()])
}

function handleCalendarSelect(
  date: string,
  record: HealthCheckin | null,
): void {
  if (record?.id) {
    openDetail(record.id)
    return
  }
  if (canEditCheckinDate(date)) {
    openManual(date)
  } else {
    ElMessage.info('只能补录最近 7 天的打卡')
  }
}

/* ---------------- 详情 ---------------- */
const detailVisible = ref(false)
const detailRecord = ref<HealthCheckin | null>(null)
const detailLoading = ref(false)

async function openDetail(id: number): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  detailRecord.value = null
  try {
    detailRecord.value = await getCheckinDetail(id)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    detailLoading.value = false
  }
}

function openRowDetail(row: HealthCheckin): void {
  if (row.id) openDetail(row.id)
}

function canEdit(row: HealthCheckin): boolean {
  return canEditCheckinDate(row.checkinDate)
}

/* ---------------- 校医 / 超级管理员 ---------------- */
const adminStudentNo = ref('')
const adminDateRange = ref<[string, string] | null>(null)
const adminStayUpLate = ref<number | undefined>(undefined)
const adminExerciseSufficient = ref<number | undefined>(undefined)
const adminDataQuality = ref<number | undefined>(undefined)
const adminQuery = reactive({ page: 1, size: 10 })
const adminRecords = ref<HealthCheckin[]>([])
const adminTotal = ref(0)
const adminLoading = ref(false)

async function loadAdmin(): Promise<void> {
  if (!isManageMode.value) return
  adminLoading.value = true
  try {
    const range = adminDateRange.value
    const page = await pageAllCheckins({
      page: adminQuery.page,
      size: adminQuery.size,
      studentNo: adminStudentNo.value.trim() || undefined,
      startDate: Array.isArray(range) ? range[0] : undefined,
      endDate: Array.isArray(range) ? range[1] : undefined,
      stayUpLate: optionalNumber(adminStayUpLate.value),
      exerciseSufficient: optionalNumber(adminExerciseSufficient.value),
      dataQuality: optionalNumber(adminDataQuality.value),
    })
    adminRecords.value = page.records
    adminTotal.value = page.total
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    adminLoading.value = false
  }
}

function handleAdminSearch(): void {
  adminQuery.page = 1
  loadAdmin()
}

function resetAdminFilters(): void {
  adminStudentNo.value = ''
  adminDateRange.value = null
  adminStayUpLate.value = undefined
  adminExerciseSufficient.value = undefined
  adminDataQuality.value = undefined
  adminQuery.page = 1
  loadAdmin()
}

function handleAdminSizeChange(): void {
  adminQuery.page = 1
  loadAdmin()
}

function sessionDate(row: CheckinSession): string {
  return formatDate(row.checkinDate || row.startTime?.slice(0, 10))
}

function sessionExerciseText(row: CheckinSession): string {
  if (row.sessionType !== 2) return '—'
  const type = row.exerciseType || '运动'
  const intensity = row.exerciseIntensityName || '—'
  return `${type} · ${intensity}`
}

onMounted(() => {
  if (isManageMode.value) {
    loadAdmin()
  } else if (isStudentMode.value) {
    loadToday()
    loadActiveSessions()
    loadMonth(overviewMonth.value)
    loadHistory()
    loadSessions()
    startTimerLoop()
  }
})

onBeforeUnmount(() => {
  if (timerId) {
    window.clearInterval(timerId)
    timerId = undefined
  }
})
</script>

<template>
  <div class="page-container checkin-page">
    <!-- 校医 / 超级管理员 -->
    <template v-if="isManageMode">
      <div class="page-heading">
        <div>
          <h2>打卡记录管理</h2>
          <p>查看学生每日健康打卡、数据质量与异常情况</p>
        </div>
        <el-tag type="info" effect="plain">共 {{ adminTotal }} 条记录</el-tag>
      </div>

      <el-alert
        v-if="isDoctor"
        title="校医视角下学号、姓名已脱敏，打卡照片不展示"
        type="info"
        :closable="false"
        show-icon
      />

      <section class="panel-card search-card">
        <el-form :model="{}" inline class="search-form" @submit.prevent>
          <el-form-item label="学号">
            <el-input
              v-model="adminStudentNo"
              placeholder="请输入学号"
              clearable
              style="width: 160px"
              @keyup.enter="handleAdminSearch"
            />
          </el-form-item>
          <el-form-item label="日期">
            <el-date-picker
              v-model="adminDateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              clearable
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item label="熬夜">
            <el-select
              v-model="adminStayUpLate"
              placeholder="全部"
              clearable
              style="width: 110px"
            >
              <el-option
                v-for="item in CHECKIN_YES_NO_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="运动达标">
            <el-select
              v-model="adminExerciseSufficient"
              placeholder="全部"
              clearable
              style="width: 110px"
            >
              <el-option
                v-for="item in CHECKIN_YES_NO_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="数据质量">
            <el-select
              v-model="adminDataQuality"
              placeholder="全部"
              clearable
              style="width: 130px"
            >
              <el-option
                v-for="item in CHECKIN_DATA_QUALITY_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleAdminSearch">
              查询
            </el-button>
            <el-button :icon="Refresh" @click="resetAdminFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="panel-card table-card">
        <div class="toolbar">
          <div class="toolbar-left">
            <span class="table-title">打卡记录</span>
            <el-tag type="info" effect="plain">按日期倒序</el-tag>
          </div>
        </div>

        <el-table
          v-loading="adminLoading"
          :data="adminRecords"
          row-key="id"
          style="width: 100%"
        >
          <el-table-column label="学生" min-width="150">
            <template #default="{ row }">
              <div class="student-cell">
                <strong>{{ row.realName || '—' }}</strong>
                <span>{{ row.studentNo || '—' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="日期" width="110">
            <template #default="{ row }">
              <div class="date-cell">
                <span>{{ formatDate(row.checkinDate) }}</span>
                <el-tag
                  v-if="row.backfilled"
                  size="small"
                  type="warning"
                  effect="plain"
                >
                  补录
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="睡眠" min-width="150">
            <template #default="{ row }">
              <div v-if="row.sleepCompleted" class="cell-stack">
                <strong>{{ formatSleepHours(row.sleepDuration) }}</strong>
                <span class="cell-sub">
                  {{ formatClockTime(row.sleepTime) }} -
                  {{ formatClockTime(row.wakeTime) }}
                </span>
                <div class="tag-row">
                  <el-tag
                    v-if="row.stayUpLate != null"
                    :type="yesNoTagType(row.stayUpLate)"
                    size="small"
                    effect="plain"
                  >
                    {{ row.stayUpLate ? '熬夜' : '未熬夜' }}
                  </el-tag>
                  <span v-else class="muted-text">未填写</span>
                </div>
              </div>
              <span v-else class="muted-text">未记录</span>
            </template>
          </el-table-column>
          <el-table-column label="运动" min-width="170">
            <template #default="{ row }">
              <div v-if="row.exerciseCompleted" class="cell-stack">
                <strong>
                  {{ row.exerciseType || '运动' }} ·
                  {{ formatDurationMinutes(row.exerciseDuration) }}
                </strong>
                <div class="tag-row">
                  <el-tag size="small" type="info" effect="plain">
                    {{ row.exerciseIntensityName || '—' }}
                  </el-tag>
                  <el-tag
                    v-if="row.exerciseSufficient != null"
                    :type="yesNoTagType(row.exerciseSufficient)"
                    size="small"
                    effect="plain"
                  >
                    {{ row.exerciseSufficient ? '达标' : '未达标' }}
                  </el-tag>
                </div>
              </div>
              <span v-else class="muted-text">未记录</span>
            </template>
          </el-table-column>
          <el-table-column label="饮食" min-width="140">
            <template #default="{ row }">
              <div v-if="row.dietCompleted" class="cell-stack">
                <span class="cell-sub">
                  早餐：{{ row.dietBreakfast == null ? '未填写' : row.dietBreakfast ? '是' : '否' }}
                </span>
                <span class="cell-sub">{{ row.dietBalanceName || '均衡度未填写' }}</span>
                <span class="cell-sub">饮水 {{ formatWaterIntake(row.waterIntake) }}</span>
              </div>
              <span v-else class="muted-text">未记录</span>
            </template>
          </el-table-column>
          <el-table-column label="情绪" min-width="120">
            <template #default="{ row }">
              <div v-if="row.emotionCompleted" class="emotion-cell">
                <span class="table-emoji">{{ emotionEmoji(row.emotionType) }}</span>
                <div class="cell-stack">
                  <strong>{{ row.emotionTypeName || '—' }}</strong>
                  <span class="cell-sub">心情 {{ row.moodScore ?? '—' }} / 5</span>
                </div>
              </div>
              <span v-else class="muted-text">未记录</span>
            </template>
          </el-table-column>
          <el-table-column label="数据质量" width="110" align="center">
            <template #default="{ row }">
              <el-tooltip
                v-if="row.dataQuality === 1 && row.abnormalReason"
                :content="row.abnormalReason"
                placement="top"
              >
                <el-tag type="danger" size="small" effect="light">
                  {{ row.dataQualityName || '异常待复核' }}
                </el-tag>
              </el-tooltip>
              <el-tag
                v-else
                :type="dataQualityTagType(row.dataQuality)"
                size="small"
                effect="light"
              >
                {{ row.dataQualityName || '正常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="照片" width="60" align="center">
            <template #default="{ row }">
              {{ row.photoCount ?? 0 }}
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="100"
            :fixed="isMobile ? false : 'right'"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openRowDetail(row)">
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <span class="selection-tip">
            打卡数据由学生本人记录，校医和超级管理员仅做查看
          </span>
          <el-pagination
            v-model:current-page="adminQuery.page"
            v-model:page-size="adminQuery.size"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="adminTotal"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="loadAdmin"
            @size-change="handleAdminSizeChange"
          />
        </div>
      </section>
    </template>

    <!-- 学生 -->
    <template v-else-if="isStudentMode">
      <div class="page-heading">
        <div>
          <h2>健康打卡</h2>
          <p>记录睡眠、运动、饮食和情绪，让健康成为每天的小习惯</p>
        </div>
        <div class="heading-actions">
          <el-button :icon="Refresh" @click="refreshStudentData">
            刷新
          </el-button>
          <el-button type="primary" :icon="Edit" @click="openManual()">
            补录 / 手动填写
          </el-button>
        </div>
      </div>

      <CheckinTodayDashboard
        :today="today"
        :today-loading="todayLoading"
        :active-sleep="activeSleep"
        :active-exercise="activeExercise"
        :sleep-elapsed="sleepElapsed"
        :exercise-elapsed="exerciseElapsed"
        :sleep-starting="sleepStarting"
        :sleep-ending="sleepEnding"
        :exercise-starting="exerciseStarting"
        :exercise-ending="exerciseEnding"
        @start-sleep="handleStartSleep"
        @end-sleep="handleEndSleep"
        @start-exercise="handleOpenExerciseStart"
        @end-exercise="handleEndExercise"
        @manual="openManual()"
      />

      <CheckinPhotoPanel
        :checkin-id="today?.id"
        :photos="today?.photos"
        @changed="handlePhotoChanged"
        @refresh="loadToday"
        @need-checkin="openManual()"
      />

      <CheckinMonthlyOverview
        :month="overviewMonth"
        :records="monthRecords"
        :summary="summary"
        :loading="overviewLoading"
        @month-change="handleMonthChange"
        @select="handleCalendarSelect"
      />

      <section class="panel-card history-card">
        <el-tabs v-model="historyTab" class="history-tabs">
          <el-tab-pane label="每日打卡" name="checkins">
            <el-form :model="{}" inline class="search-form history-filter">
              <el-form-item label="日期">
                <el-date-picker
                  v-model="historyDateRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  clearable
                  style="width: 240px"
                />
              </el-form-item>
              <el-form-item label="熬夜">
                <el-select
                  v-model="historyStayUpLate"
                  placeholder="全部"
                  clearable
                  style="width: 110px"
                >
                  <el-option
                    v-for="item in CHECKIN_YES_NO_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="运动达标">
                <el-select
                  v-model="historyExerciseSufficient"
                  placeholder="全部"
                  clearable
                  style="width: 110px"
                >
                  <el-option
                    v-for="item in CHECKIN_YES_NO_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="数据质量">
                <el-select
                  v-model="historyDataQuality"
                  placeholder="全部"
                  clearable
                  style="width: 130px"
                >
                  <el-option
                    v-for="item in CHECKIN_DATA_QUALITY_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleHistorySearch">
                  查询
                </el-button>
                <el-button :icon="Refresh" @click="resetHistoryFilters">
                  重置
                </el-button>
              </el-form-item>
            </el-form>

            <el-table
              v-loading="historyLoading"
              :data="historyRecords"
              row-key="id"
              style="width: 100%"
            >
              <el-table-column label="日期" width="120">
                <template #default="{ row }">
                  <div class="date-cell">
                    <span>{{ formatDate(row.checkinDate) }}</span>
                    <el-tag
                      v-if="row.backfilled"
                      size="small"
                      type="warning"
                      effect="plain"
                    >
                      补录
                    </el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="睡眠" min-width="155">
                <template #default="{ row }">
                  <div v-if="row.sleepCompleted" class="cell-stack">
                    <strong>{{ formatSleepHours(row.sleepDuration) }}</strong>
                    <span class="cell-sub">
                      {{ formatClockTime(row.sleepTime) }} -
                      {{ formatClockTime(row.wakeTime) }}
                    </span>
                    <div class="tag-row">
                      <el-tag
                        v-if="row.stayUpLate != null"
                        :type="yesNoTagType(row.stayUpLate)"
                        size="small"
                        effect="plain"
                      >
                        {{ row.stayUpLate ? '熬夜' : '未熬夜' }}
                      </el-tag>
                      <span v-else class="muted-text">未填写</span>
                    </div>
                  </div>
                  <span v-else class="muted-text">未记录</span>
                </template>
              </el-table-column>
              <el-table-column label="运动" min-width="170">
                <template #default="{ row }">
                  <div v-if="row.exerciseCompleted" class="cell-stack">
                    <strong>
                      {{ row.exerciseType || '运动' }} ·
                      {{ formatDurationMinutes(row.exerciseDuration) }}
                    </strong>
                    <div class="tag-row">
                      <el-tag size="small" type="info" effect="plain">
                        {{ row.exerciseIntensityName || '—' }}
                      </el-tag>
                      <el-tag
                        v-if="row.exerciseSufficient != null"
                        :type="yesNoTagType(row.exerciseSufficient)"
                        size="small"
                        effect="plain"
                      >
                        {{ row.exerciseSufficient ? '达标' : '未达标' }}
                      </el-tag>
                    </div>
                  </div>
                  <span v-else class="muted-text">未记录</span>
                </template>
              </el-table-column>
              <el-table-column label="饮食" min-width="145">
                <template #default="{ row }">
                  <div v-if="row.dietCompleted" class="cell-stack">
                    <span class="cell-sub">
                      早餐：{{ row.dietBreakfast == null ? '未填写' : row.dietBreakfast ? '是' : '否' }}
                    </span>
                    <span class="cell-sub">{{ row.dietBalanceName || '均衡度未填写' }}</span>
                    <span class="cell-sub">饮水 {{ formatWaterIntake(row.waterIntake) }}</span>
                  </div>
                  <span v-else class="muted-text">未记录</span>
                </template>
              </el-table-column>
              <el-table-column label="情绪" min-width="130">
                <template #default="{ row }">
                  <div v-if="row.emotionCompleted" class="emotion-cell">
                    <span class="table-emoji">{{ emotionEmoji(row.emotionType) }}</span>
                    <div class="cell-stack">
                      <strong>{{ row.emotionTypeName || '—' }}</strong>
                      <span class="cell-sub">心情 {{ row.moodScore ?? '—' }} / 5</span>
                    </div>
                  </div>
                  <span v-else class="muted-text">未记录</span>
                </template>
              </el-table-column>
              <el-table-column label="数据质量" width="120" align="center">
                <template #default="{ row }">
                  <el-tooltip
                    v-if="row.dataQuality === 1 && row.abnormalReason"
                    :content="row.abnormalReason"
                    placement="top"
                  >
                    <el-tag type="danger" size="small" effect="light">
                      {{ row.dataQualityName || '异常待复核' }}
                    </el-tag>
                  </el-tooltip>
                  <el-tag
                    v-else
                    :type="dataQualityTagType(row.dataQuality)"
                    size="small"
                    effect="light"
                  >
                    {{ row.dataQualityName || '正常' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="照片" width="70" align="center">
                <template #default="{ row }">
                  {{ row.photoCount ?? 0 }}
                </template>
              </el-table-column>
              <el-table-column
                label="操作"
                width="150"
                :fixed="isMobile ? false : 'right'"
              >
                <template #default="{ row }">
                  <el-button
                    link
                    type="primary"
                    :icon="View"
                    @click="openRowDetail(row)"
                  >
                    详情
                  </el-button>
                  <el-button
                    link
                    type="primary"
                    :icon="Edit"
                    :disabled="!canEdit(row)"
                    :title="canEdit(row) ? '编辑打卡' : '只能修改最近 7 天的打卡'"
                    @click="openEdit(row)"
                  >
                    编辑
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="table-footer">
              <span class="selection-tip">
                只能修改最近 7 天的打卡；更早的记录仅供查看
              </span>
              <el-pagination
                v-model:current-page="historyQuery.page"
                v-model:page-size="historyQuery.size"
                background
                layout="total, sizes, prev, pager, next, jumper"
                :total="historyTotal"
                :page-sizes="[10, 20, 50, 100]"
                @current-change="loadHistory"
                @size-change="handleHistorySizeChange"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="睡眠 / 运动计时" name="sessions">
            <el-form :model="{}" inline class="search-form history-filter">
              <el-form-item label="日期">
                <el-date-picker
                  v-model="sessionDateRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  clearable
                  style="width: 240px"
                />
              </el-form-item>
              <el-form-item label="类型">
                <el-select
                  v-model="sessionTypeFilter"
                  placeholder="全部"
                  clearable
                  style="width: 110px"
                >
                  <el-option
                    v-for="item in CHECKIN_SESSION_TYPE_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select
                  v-model="sessionStatusFilter"
                  placeholder="全部"
                  clearable
                  style="width: 120px"
                >
                  <el-option
                    v-for="item in CHECKIN_SESSION_STATUS_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleSessionSearch">
                  查询
                </el-button>
                <el-button :icon="Refresh" @click="resetSessionFilters">
                  重置
                </el-button>
              </el-form-item>
            </el-form>

            <el-table
              v-loading="sessionLoading"
              :data="sessions"
              row-key="id"
              style="width: 100%"
            >
              <el-table-column label="日期" width="115">
                <template #default="{ row }">
                  {{ sessionDate(row) }}
                </template>
              </el-table-column>
              <el-table-column label="类型" width="85" align="center">
                <template #default="{ row }">
                  <el-tag
                    :type="sessionTypeTagType(row.sessionType)"
                    size="small"
                    effect="light"
                  >
                    {{ row.sessionTypeName || (row.sessionType === 1 ? '睡眠' : '运动') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="开始时间" width="125">
                <template #default="{ row }">
                  {{ formatDateTimeShort(row.startTime) }}
                </template>
              </el-table-column>
              <el-table-column label="结束时间" width="125">
                <template #default="{ row }">
                  {{ formatDateTimeShort(row.endTime) }}
                </template>
              </el-table-column>
              <el-table-column label="时长" width="115">
                <template #default="{ row }">
                  {{ formatDurationMinutes(row.durationMinutes) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="95" align="center">
                <template #default="{ row }">
                  <el-tag
                    :type="sessionStatusTagType(row.status)"
                    size="small"
                    effect="light"
                  >
                    {{ row.statusName || '—' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="运动信息" min-width="140">
                <template #default="{ row }">
                  {{ sessionExerciseText(row) }}
                </template>
              </el-table-column>
              <el-table-column label="数据质量" width="120" align="center">
                <template #default="{ row }">
                  <el-tooltip
                    v-if="row.dataQuality === 1 && row.abnormalReason"
                    :content="row.abnormalReason"
                    placement="top"
                  >
                    <el-tag type="danger" size="small" effect="light">
                      {{ row.dataQualityName || '异常待复核' }}
                    </el-tag>
                  </el-tooltip>
                  <el-tag
                    v-else
                    :type="dataQualityTagType(row.dataQuality)"
                    size="small"
                    effect="light"
                  >
                    {{ row.dataQualityName || '正常' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>

            <div class="table-footer">
              <span class="selection-tip">
                计时开始和结束时间由服务端记录，结束时会自动同步到每日打卡
              </span>
              <el-pagination
                v-model:current-page="sessionQuery.page"
                v-model:page-size="sessionQuery.size"
                background
                layout="total, sizes, prev, pager, next, jumper"
                :total="sessionTotal"
                :page-sizes="[10, 20, 50, 100]"
                @current-change="loadSessions"
                @size-change="handleSessionSizeChange"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>

      <CheckinFormDialog
        v-model="formVisible"
        :mode="formMode"
        :record="formRecord"
        :initial-date="formDate"
        :active-sleep="Boolean(activeSleep)"
        :active-exercise="Boolean(activeExercise)"
        @saved="handleFormSaved"
      />
      <ExerciseStartDialog
        v-model="exerciseStartVisible"
        :loading="exerciseStarting"
        @submit="handleStartExercise"
      />
    </template>

    <el-result
      v-else
      icon="warning"
      title="暂无访问权限"
      sub-title="当前账号角色无法访问健康打卡模块"
    />

    <CheckinDetailDrawer
      v-model="detailVisible"
      :record="detailRecord"
      :loading="detailLoading"
      :desensitized="isDoctor"
      :show-photos="isStudentMode"
    />
  </div>
</template>

<style scoped>
.checkin-page {
  gap: 18px;
}

.heading-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.search-card {
  padding: 18px 20px 4px;
}

.table-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.student-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.4;
}

.student-cell strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--hp-text);
}

.student-cell span {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.date-cell {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--hp-text-secondary);
}

.cell-stack {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.cell-stack strong {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--hp-text);
}

.cell-sub {
  font-size: 11px;
  line-height: 1.4;
  color: var(--hp-text-muted);
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.emotion-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-emoji {
  font-size: 22px;
}

.table-footer :deep(.el-pagination) {
  margin-left: auto;
}

/* ---------------- 历史卡片 ---------------- */
.history-card {
  padding: 6px 20px 12px;
}

.history-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.history-tabs :deep(.el-tabs__item) {
  font-size: 13.5px;
  font-weight: 500;
}

.history-filter {
  padding-top: 4px;
}

@media (max-width: 768px) {
  .search-card {
    padding: 14px 12px 0;
  }

  .history-card {
    padding: 4px 12px 10px;
  }

  .table-footer :deep(.el-pagination) {
    margin-left: 0;
  }
}
</style>
