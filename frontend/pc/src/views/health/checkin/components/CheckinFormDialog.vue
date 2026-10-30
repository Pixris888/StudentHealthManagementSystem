<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Bicycle, Coffee, Moon, Sunny } from '@element-plus/icons-vue'
import {
  pageMyCheckins,
  saveCheckin,
  updateCheckin,
} from '@/api/health-checkin'
import {
  calculateSleepHours,
  canEditCheckinDate,
  CHECKIN_DIET_BALANCE_OPTIONS,
  CHECKIN_EMOTION_OPTIONS,
  CHECKIN_EXERCISE_INTENSITY_OPTIONS,
  CHECKIN_EXERCISE_TYPES,
  disabledBackfillDate,
  formatSleepHours,
  isExerciseTimerSource,
  isSleepTimerSource,
  todayDateString,
} from '@/utils/checkin'
import type { CheckinSave, HealthCheckin } from '@/types/health'

interface CheckinFormModel {
  sleepTime: string | null
  wakeTime: string | null
  exerciseType: string
  exerciseDuration: number | null
  exerciseIntensity: number | null
  dietBreakfast: number | null
  dietBalance: number | null
  waterIntake: number | null
  emotionType: number | null
  moodScore: number | null
  content: string
}

const props = defineProps<{
  mode?: 'manual' | 'edit'
  record?: HealthCheckin | null
  initialDate?: string
  activeSleep?: boolean
  activeExercise?: boolean
}>()

const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ saved: [record: HealthCheckin] }>()

const formRef = ref<FormInstance>()
const formDate = ref(todayDateString())
const dateRecord = ref<HealthCheckin | null>(null)
const dateLoading = ref(false)
const saving = ref(false)

const isEdit = computed(() => props.mode === 'edit')
const dialogTitle = computed(() =>
  isEdit.value ? '编辑打卡记录' : '填写 / 补录打卡',
)

const timerSleep = computed(
  () =>
    isSleepTimerSource(dateRecord.value) ||
    (formDate.value === todayDateString() && Boolean(props.activeSleep)),
)
const timerExercise = computed(
  () =>
    isExerciseTimerSource(dateRecord.value) ||
    (formDate.value === todayDateString() && Boolean(props.activeExercise)),
)

function emptyForm(): CheckinFormModel {
  return {
    sleepTime: null,
    wakeTime: null,
    exerciseType: '',
    exerciseDuration: null,
    exerciseIntensity: null,
    dietBreakfast: null,
    dietBalance: null,
    waterIntake: null,
    emotionType: null,
    moodScore: null,
    content: '',
  }
}

const form = reactive<CheckinFormModel>(emptyForm())
const sleepPreview = computed(() =>
  calculateSleepHours(form.sleepTime, form.wakeTime),
)
const moodScoreModel = computed({
  get: () => form.moodScore ?? 0,
  set: (value: number) => {
    form.moodScore = value > 0 ? value : null
  },
})

function applyRecord(record: HealthCheckin | null): void {
  if (!record) {
    Object.assign(form, emptyForm())
    return
  }
  Object.assign(form, {
    sleepTime: record.sleepTime ?? null,
    wakeTime: record.wakeTime ?? null,
    exerciseType: record.exerciseType ?? '',
    exerciseDuration: record.exerciseDuration ?? null,
    exerciseIntensity: record.exerciseIntensity ?? null,
    dietBreakfast: record.dietBreakfast ?? null,
    dietBalance: record.dietBalance ?? null,
    waterIntake: record.waterIntake ?? null,
    emotionType: record.emotionType ?? null,
    moodScore: record.moodScore ?? null,
    content: record.content ?? '',
  })
}

async function loadDateRecord(date: string): Promise<void> {
  dateLoading.value = true
  try {
    const page = await pageMyCheckins({
      page: 1,
      size: 1,
      startDate: date,
      endDate: date,
    })
    dateRecord.value = page.records[0] ?? null
    applyRecord(dateRecord.value)
  } catch {
    dateRecord.value = null
    applyRecord(null)
  } finally {
    dateLoading.value = false
  }
}

async function handleDateChange(value: string | null): Promise<void> {
  if (!value || isEdit.value) return
  await loadDateRecord(value)
  nextTick(() => formRef.value?.clearValidate())
}

async function initialize(): Promise<void> {
  if (isEdit.value && props.record) {
    formDate.value = props.record.checkinDate ?? todayDateString()
    dateRecord.value = props.record
    applyRecord(props.record)
    nextTick(() => formRef.value?.clearValidate())
    return
  }
  const initial = props.initialDate || todayDateString()
  formDate.value = canEditCheckinDate(initial)
    ? initial
    : todayDateString()
  dateRecord.value = null
  applyRecord(null)
  await loadDateRecord(formDate.value)
  nextTick(() => formRef.value?.clearValidate())
}

watch(
  [visible, () => props.mode, () => props.record, () => props.initialDate],
  ([open]) => {
    if (open) initialize()
  },
)

function validateSleep(
  _rule: unknown,
  _value: unknown,
  callback: (error?: Error) => void,
): void {
  if (timerSleep.value) {
    callback()
    return
  }
  const hasSleep = Boolean(form.sleepTime)
  const hasWake = Boolean(form.wakeTime)
  if (hasSleep !== hasWake) {
    callback(new Error('入睡时间和起床时间需同时填写'))
    return
  }
  if (hasSleep && hasWake) {
    const hours = calculateSleepHours(form.sleepTime, form.wakeTime)
    if (hours == null || hours < 0.5 || hours > 24) {
      callback(new Error('睡眠时长需在 0.5-24 小时之间'))
      return
    }
  }
  callback()
}

function validateExercise(
  _rule: unknown,
  _value: unknown,
  callback: (error?: Error) => void,
): void {
  if (timerExercise.value) {
    callback()
    return
  }
  const duration = form.exerciseDuration
  const hasType = Boolean(form.exerciseType.trim())
  const hasDuration = duration != null
  const hasIntensity = form.exerciseIntensity != null
  const hasAny = hasType || hasDuration || hasIntensity
  if (!hasAny) {
    callback()
    return
  }
  if (!hasType) {
    callback(new Error('请选择或输入运动类型'))
    return
  }
  if (duration == null) {
    callback(new Error('请填写运动时长'))
    return
  }
  if (!hasIntensity) {
    callback(new Error('请选择运动强度'))
    return
  }
  if (duration < 1 || duration > 600) {
    callback(new Error('运动时长需在 1-600 分钟之间'))
    return
  }
  callback()
}

function validateContent(
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void,
): void {
  if (value && value.length > 500) {
    callback(new Error('打卡内容不能超过 500 个字符'))
  } else {
    callback()
  }
}

const rules: FormRules<CheckinFormModel> = {
  sleepTime: [{ validator: validateSleep, trigger: 'change' }],
  wakeTime: [{ validator: validateSleep, trigger: 'change' }],
  exerciseType: [{ validator: validateExercise, trigger: 'change' }],
  exerciseDuration: [{ validator: validateExercise, trigger: 'change' }],
  exerciseIntensity: [{ validator: validateExercise, trigger: 'change' }],
  content: [{ validator: validateContent, trigger: 'blur' }],
}

function hasAnyInput(): boolean {
  return Boolean(
    (!timerSleep.value && (form.sleepTime || form.wakeTime)) ||
      (!timerExercise.value &&
        (form.exerciseType.trim() ||
          form.exerciseDuration != null ||
          form.exerciseIntensity != null)) ||
      form.dietBreakfast != null ||
      form.dietBalance != null ||
      form.waterIntake != null ||
      form.emotionType != null ||
      form.moodScore != null ||
      form.content.trim(),
  )
}

async function handleSubmit(): Promise<void> {
  if (dateLoading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (!isEdit.value && !dateRecord.value && !hasAnyInput()) {
    ElMessage.warning('请至少填写一项打卡内容')
    return
  }

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
    form.exerciseDuration != null &&
    form.exerciseIntensity != null
  ) {
    payload.exerciseType = form.exerciseType.trim()
    payload.exerciseDuration = form.exerciseDuration
    payload.exerciseIntensity = form.exerciseIntensity
  }
  if (form.dietBreakfast != null) {
    payload.dietBreakfast = form.dietBreakfast
  }
  if (form.dietBalance != null) {
    payload.dietBalance = form.dietBalance
  }
  if (form.waterIntake != null) {
    payload.waterIntake = form.waterIntake
  }
  if (form.emotionType != null) {
    payload.emotionType = form.emotionType
  }
  if (form.moodScore != null) {
    payload.moodScore = form.moodScore
  }

  saving.value = true
  try {
    let updated: HealthCheckin
    if (isEdit.value && props.record?.id) {
      updated = await updateCheckin(props.record.id, payload)
      ElMessage.success('打卡记录已更新')
    } else {
      payload.checkinDate = formDate.value
      updated = await saveCheckin(payload)
      ElMessage.success(dateRecord.value ? '打卡记录已更新' : '打卡记录已保存')
    }
    visible.value = false
    emit('saved', updated)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="760px"
    append-to-body
    :close-on-click-modal="false"
  >
    <el-alert
      title="只填写需要补录的板块；留空表示不修改。睡眠/运动计时产生的记录不可再手动填写对应板块。"
      type="info"
      :closable="false"
      show-icon
      class="form-tip"
    />

    <div class="date-row">
      <span class="date-label">打卡日期</span>
      <el-date-picker
        v-if="!isEdit"
        v-model="formDate"
        type="date"
        value-format="YYYY-MM-DD"
        format="YYYY-MM-DD"
        :disabled-date="disabledBackfillDate"
        :clearable="false"
        style="width: 190px"
        @change="handleDateChange"
      />
      <el-tag v-else type="info" effect="plain">{{ formDate }}</el-tag>
      <span v-if="dateRecord" class="date-record-tip">
        {{ dateRecord.backfilled ? '该日期为补录记录' : '该日期已有记录，提交后将更新' }}
      </span>
      <span v-else-if="!isEdit" class="date-record-tip">
        仅支持最近 7 天补录
      </span>
    </div>

    <div v-loading="dateLoading">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="96px"
        class="checkin-form"
      >
        <div class="form-section">
          <div class="section-head">
            <div class="section-icon sleep-icon">
              <el-icon><Moon /></el-icon>
            </div>
            <div>
              <h4>睡眠</h4>
              <p>入睡时间和起床时间需同时填写</p>
            </div>
            <el-tag
              v-if="sleepPreview != null"
              type="info"
              effect="plain"
              class="section-tag"
            >
              约 {{ formatSleepHours(sleepPreview) }}
            </el-tag>
          </div>
          <el-alert
            v-if="timerSleep"
            title="该日期已有睡眠计时记录，睡眠字段不可手动填写"
            type="warning"
            :closable="false"
            show-icon
            class="section-alert"
          />
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="入睡时间" prop="sleepTime">
                <el-time-picker
                  v-model="form.sleepTime"
                  value-format="HH:mm:ss"
                  format="HH:mm"
                  :disabled="timerSleep"
                  placeholder="选择入睡时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="起床时间" prop="wakeTime">
                <el-time-picker
                  v-model="form.wakeTime"
                  value-format="HH:mm:ss"
                  format="HH:mm"
                  :disabled="timerSleep"
                  placeholder="选择起床时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-head">
            <div class="section-icon exercise-icon">
              <el-icon><Bicycle /></el-icon>
            </div>
            <div>
              <h4>运动</h4>
              <p>填写运动类型、时长和强度后才会记录</p>
            </div>
          </div>
          <el-alert
            v-if="timerExercise"
            title="该日期已有运动计时记录，运动字段不可手动填写"
            type="warning"
            :closable="false"
            show-icon
            class="section-alert"
          />
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="运动类型" prop="exerciseType">
                <el-select
                  v-model="form.exerciseType"
                  filterable
                  allow-create
                  default-first-option
                  clearable
                  :disabled="timerExercise"
                  placeholder="如 跑步"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in CHECKIN_EXERCISE_TYPES"
                    :key="item"
                    :label="item"
                    :value="item"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="运动时长" prop="exerciseDuration">
                <el-input-number
                  v-model="form.exerciseDuration"
                  :min="1"
                  :max="600"
                  :precision="0"
                  :step="5"
                  :disabled="timerExercise"
                  controls-position="right"
                  placeholder="分钟"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="运动强度" prop="exerciseIntensity">
                <el-radio-group
                  v-model="form.exerciseIntensity"
                  :disabled="timerExercise"
                >
                  <el-radio
                    v-for="item in CHECKIN_EXERCISE_INTENSITY_OPTIONS"
                    :key="item.value"
                    :value="item.value"
                  >
                    {{ item.label }}
                  </el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-head">
            <div class="section-icon diet-icon">
              <el-icon><Coffee /></el-icon>
            </div>
            <div>
              <h4>饮食</h4>
              <p>记录早餐、饮食均衡度和饮水量</p>
            </div>
          </div>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="8">
              <el-form-item label="吃早餐">
                <el-radio-group v-model="form.dietBreakfast">
                  <el-radio :value="1">是</el-radio>
                  <el-radio :value="0">否</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="饮食均衡">
                <el-select
                  v-model="form.dietBalance"
                  clearable
                  placeholder="请选择"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in CHECKIN_DIET_BALANCE_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="饮水量">
                <el-input-number
                  v-model="form.waterIntake"
                  :min="0"
                  :max="10000"
                  :precision="0"
                  :step="100"
                  controls-position="right"
                  placeholder="ml"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-head">
            <div class="section-icon emotion-icon">
              <el-icon><Sunny /></el-icon>
            </div>
            <div>
              <h4>情绪</h4>
              <p>选择情绪类型或心情评分，只填一项时系统会自动补全</p>
            </div>
          </div>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="情绪类型">
                <el-select
                  v-model="form.emotionType"
                  clearable
                  placeholder="今天的心情"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in CHECKIN_EMOTION_OPTIONS"
                    :key="item.value"
                    :label="`${item.emoji} ${item.label}`"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="心情评分">
                <el-rate
                  v-model="moodScoreModel"
                  clearable
                  :max="5"
                  show-score
                  score-template="{value} / 5"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-head">
            <div class="section-icon content-icon">
              <el-icon><Coffee /></el-icon>
            </div>
            <div>
              <h4>打卡备注</h4>
              <p>可以记录今天的身体感受或想对校医说的话</p>
            </div>
          </div>
          <el-form-item label="备注" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="今天的身体感受、运动感受等"
            />
          </el-form-item>
        </div>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="dateLoading"
        @click="handleSubmit"
      >
        保存打卡
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-tip {
  margin-bottom: 14px;
  line-height: 1.6;
}

.date-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 12px 14px;
  margin-bottom: 14px;
  background: #f8fbfa;
  border: 1px solid #e7f0ec;
  border-radius: 10px;
}

.date-label {
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.date-record-tip {
  font-size: 11px;
  color: var(--hp-text-muted);
}

/* 字段较多，限制表单区域高度，保证底部按钮始终可见 */
.checkin-form {
  max-height: 55vh;
  padding-right: 8px;
  overflow-y: auto;
}

.form-section {
  padding: 4px 0 6px;
}

.form-section + .form-section {
  padding-top: 16px;
  margin-top: 4px;
  border-top: 1px dashed #e8f0ed;
}

.section-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
}

.section-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
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

.content-icon {
  color: #3f7ff0;
  background: #eaf1ff;
}

.section-head h4 {
  margin-bottom: 3px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--hp-text);
}

.section-head p {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.section-tag {
  margin-left: auto;
}

.section-alert {
  margin-bottom: 12px;
}

.checkin-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.checkin-form :deep(.el-form-item__label) {
  color: var(--hp-text-secondary);
}

@media (max-width: 600px) {
  .checkin-form {
    max-height: 52vh;
  }

  .checkin-form :deep(.el-form-item__label) {
    width: 86px !important;
  }
}
</style>
