<template>
  <view class="page page-with-fixed">
    <view v-if="loading" class="card loading-card">
      <uni-load-more status="loading" color="#0f9d8a" />
    </view>

    <template v-else>
      <view class="card intro-card">
        <view class="between">
          <view>
            <text class="intro-title">{{ recordId ? '编辑体测记录' : '新增体测记录' }}</text>
            <text class="intro-sub">同一学年学期只保留一条记录，重复提交会覆盖更新</text>
          </view>
          <view class="tag tag-primary">{{ testYear || '—' }} 年</view>
        </view>
      </view>

      <view class="card">
        <view class="card-title-row">
          <text class="card-title">测试信息</text>
        </view>

        <view class="double-row">
          <view class="form-item half">
            <text class="form-label">测试年份 <text class="required">*</text></text>
            <view class="input-box">
              <input
                v-model="form.testYearText"
                class="input"
                type="number"
                maxlength="4"
                placeholder="如 2025"
                placeholder-class="input-placeholder"
              />
            </view>
          </view>
          <view class="form-item half">
            <text class="form-label">测试学期 <text class="required">*</text></text>
            <picker :range="termOptions" :value="termIndex" @change="onTermChange">
              <view class="picker-box">
                <text :class="form.testTerm ? 'picker-value' : 'picker-placeholder'">
                  {{ form.testTerm || '请选择学期' }}
                </text>
                <uni-icons type="arrowdown" size="14" color="#b1bfba" />
              </view>
            </picker>
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">测试日期</text>
          <picker
            mode="date"
            :value="form.testDate || today"
            start="2000-01-01"
            :end="today"
            @change="onDateChange"
          >
            <view class="picker-box">
              <text :class="form.testDate ? 'picker-value' : 'picker-placeholder'">
                {{ form.testDate || '请选择测试日期' }}
              </text>
              <uni-icons type="calendar" size="15" color="#b1bfba" />
            </view>
          </picker>
        </view>
      </view>

      <view class="card">
        <view class="card-title-row">
          <text class="card-title">身高体重</text>
          <text class="muted">用于计算 BMI</text>
        </view>

        <view class="double-row">
          <view class="form-item half">
            <text class="form-label">身高（cm）</text>
            <view class="input-box">
              <input
                v-model="form.heightText"
                class="input"
                type="digit"
                placeholder="如 170"
                placeholder-class="input-placeholder"
              />
            </view>
          </view>
          <view class="form-item half">
            <text class="form-label">体重（kg）</text>
            <view class="input-box">
              <input
                v-model="form.weightText"
                class="input"
                type="digit"
                placeholder="如 60"
                placeholder-class="input-placeholder"
              />
            </view>
          </view>
        </view>

        <view class="bmi-preview">
          <view class="grow">
            <text class="bmi-preview-label">BMI 预览</text>
            <text class="bmi-preview-hint">由后端最终计算并保存</text>
          </view>
          <text class="bmi-preview-value">{{ formatNumber(bmiPreview, 1) }}</text>
          <view class="tag" :class="`tag-${bmiTone}`">{{ bmiLevelPreview }}</view>
        </view>
      </view>

      <view class="card">
        <view class="card-title-row">
          <text class="card-title">体测项目</text>
          <text class="muted">留空表示未测</text>
        </view>

        <view class="double-row">
          <view class="form-item half">
            <text class="form-label">肺活量（ml）</text>
            <view class="input-box">
              <input
                v-model="form.vitalCapacityText"
                class="input"
                type="number"
                placeholder="如 3200"
                placeholder-class="input-placeholder"
              />
            </view>
          </view>
          <view class="form-item half">
            <text class="form-label">50 米跑（秒）</text>
            <view class="input-box">
              <input
                v-model="form.run50mText"
                class="input"
                type="digit"
                placeholder="如 7.8"
                placeholder-class="input-placeholder"
              />
            </view>
          </view>
        </view>

        <view class="double-row">
          <view class="form-item half">
            <text class="form-label">坐位体前屈（cm）</text>
            <view class="input-box">
              <input
                v-model="form.sitAndReachText"
                class="input"
                type="digit"
                placeholder="如 12.5"
                placeholder-class="input-placeholder"
              />
            </view>
          </view>
          <view class="form-item half">
            <text class="form-label">立定跳远（cm）</text>
            <view class="input-box">
              <input
                v-model="form.standingJumpText"
                class="input"
                type="digit"
                placeholder="如 210"
                placeholder-class="input-placeholder"
              />
            </view>
          </view>
        </view>

        <view class="double-row">
          <view class="form-item half">
            <text class="form-label">耐力跑（秒）</text>
            <view class="input-box">
              <input
                v-model="form.enduranceRunText"
                class="input"
                type="digit"
                placeholder="如 240"
                placeholder-class="input-placeholder"
              />
            </view>
          </view>
          <view class="form-item half">
            <text class="form-label">力量项目</text>
            <view class="input-box">
              <input
                v-model="form.strengthItemText"
                class="input"
                type="digit"
                placeholder="请填写项目名称"
                placeholder-class="input-placeholder"
              />
            </view>
          </view>
        </view>

        <view class="double-row">
          <view class="form-item half">
            <text class="form-label">总分</text>
            <view class="input-box">
              <input
                v-model="form.totalScoreText"
                class="input"
                type="digit"
                placeholder="如 85.5"
                placeholder-class="input-placeholder"
              />
            </view>
          </view>
          <view class="form-item half">
            <text class="form-label">等级</text>
            <view class="chip-row">
              <view
                v-for="grade in gradeOptions"
                :key="grade"
                class="choice-chip"
                :class="{ active: form.gradeLevel === grade }"
                @click="toggleGrade(grade)"
              >
                {{ grade }}
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="card">
        <view class="card-title-row">
          <text class="card-title">备注</text>
        </view>
        <view class="textarea-box" style="margin-top: 18rpx">
          <textarea
            v-model="form.remark"
            class="textarea"
            maxlength="300"
            placeholder="其他需要说明的情况（选填）"
            placeholder-class="input-placeholder"
          />
        </view>
      </view>

      <view class="hint-text">
        <uni-icons type="info" size="14" color="#94a3b8" />
        <text>体测成绩由本人录入，如有误请联系校医核实。移动端暂不支持 Excel 导入。</text>
      </view>

      <view class="footer-bar">
        <button class="btn btn-primary btn-block" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中…' : recordId ? '保存修改' : '新增体测记录' }}
        </button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  addMyPhysicalTest,
  pageMyPhysicalTests,
  updateMyPhysicalTest,
} from '@/api/health-physical'
import { getAccessToken } from '@/utils/auth'
import { GRADE_LEVEL_OPTIONS, TERM_OPTIONS } from '@/utils/dict'
import { formatNumber, todayString } from '@/utils/format'
import {
  bmiLevelOf,
  calcBmi,
  isFutureDate,
} from '@/utils/health'
import type { PhysicalTestSave } from '@/types/health'

interface PhysicalForm {
  testYearText: string
  testTerm: string
  testDate: string
  heightText: string
  weightText: string
  vitalCapacityText: string
  run50mText: string
  sitAndReachText: string
  standingJumpText: string
  enduranceRunText: string
  strengthItemText: string
  totalScoreText: string
  gradeLevel: string
  remark: string
}

const today = todayString()
const termOptions = TERM_OPTIONS
const gradeOptions = GRADE_LEVEL_OPTIONS
const recordId = ref<number | null>(null)
const loading = ref(false)
const saving = ref(false)
const termIndex = ref(0)

const form = reactive<PhysicalForm>({
  testYearText: String(new Date().getFullYear()),
  testTerm: TERM_OPTIONS[0] || '',
  testDate: today,
  heightText: '',
  weightText: '',
  vitalCapacityText: '',
  run50mText: '',
  sitAndReachText: '',
  standingJumpText: '',
  enduranceRunText: '',
  strengthItemText: '',
  totalScoreText: '',
  gradeLevel: '',
  remark: '',
})

const testYear = computed(() => Number(form.testYearText))
const bmiPreview = computed(() =>
  calcBmi(numberOrNull(form.heightText), numberOrNull(form.weightText)),
)
const bmiLevelPreview = computed(() => bmiLevelOf(bmiPreview.value) || '待计算')
const bmiTone = computed(() => {
  const level = bmiLevelPreview.value
  if (level === '正常') return 'success'
  if (level === '偏瘦') return 'info'
  if (level === '超重') return 'warning'
  if (level === '肥胖') return 'danger'
  return 'info'
})

function numberOrNull(value: string): number | null {
  const text = value.trim()
  if (!text) return null
  const number = Number(text)
  return Number.isFinite(number) ? number : null
}

function onTermChange(event: { detail: { value: string | number } }): void {
  const index = Number(event.detail.value)
  termIndex.value = index
  form.testTerm = termOptions[index] || ''
}

function onDateChange(event: { detail: { value: string } }): void {
  form.testDate = event.detail.value || ''
}

function toggleGrade(grade: string): void {
  form.gradeLevel = form.gradeLevel === grade ? '' : grade
}

function fillRecord(record: {
  id: number
  testYear: number
  testTerm: string
  testDate?: string | null
  height?: number | null
  weight?: number | null
  vitalCapacity?: number | null
  run50m?: number | null
  sitAndReach?: number | null
  standingJump?: number | null
  enduranceRun?: number | null
  strengthItem?: number | null
  totalScore?: number | null
  gradeLevel?: string | null
  remark?: string | null
}): void {
  recordId.value = record.id
  form.testYearText = String(record.testYear ?? new Date().getFullYear())
  form.testTerm = record.testTerm || TERM_OPTIONS[0] || ''
  form.testDate = record.testDate || today
  form.heightText = record.height != null ? String(record.height) : ''
  form.weightText = record.weight != null ? String(record.weight) : ''
  form.vitalCapacityText = record.vitalCapacity != null ? String(record.vitalCapacity) : ''
  form.run50mText = record.run50m != null ? String(record.run50m) : ''
  form.sitAndReachText = record.sitAndReach != null ? String(record.sitAndReach) : ''
  form.standingJumpText = record.standingJump != null ? String(record.standingJump) : ''
  form.enduranceRunText = record.enduranceRun != null ? String(record.enduranceRun) : ''
  form.strengthItemText = record.strengthItem != null ? String(record.strengthItem) : ''
  form.totalScoreText = record.totalScore != null ? String(record.totalScore) : ''
  form.gradeLevel = record.gradeLevel || ''
  form.remark = record.remark || ''
  termIndex.value = Math.max(0, termOptions.findIndex((item) => item === form.testTerm))
}

async function loadRecord(id: number): Promise<void> {
  loading.value = true
  try {
    const result = await pageMyPhysicalTests({ page: 1, size: 100 })
    const record = (result.records ?? []).find((item) => item.id === id)
    if (!record) {
      uni.showToast({ title: '体测记录不存在', icon: 'none' })
      setTimeout(() => uni.navigateBack({ delta: 1 }), 600)
      return
    }
    fillRecord(record)
  } catch {
    // 请求层已提示
  } finally {
    loading.value = false
  }
}

function validate(): boolean {
  const year = Number(form.testYearText)
  if (!form.testYearText.trim() || !Number.isInteger(year) || year < 2000 || year > 2100) {
    uni.showToast({ title: '请输入 2000-2100 之间的测试年份', icon: 'none' })
    return false
  }
  if (!form.testTerm.trim()) {
    uni.showToast({ title: '请选择测试学期', icon: 'none' })
    return false
  }
  if (isFutureDate(form.testDate)) {
    uni.showToast({ title: '测试日期不能晚于今天', icon: 'none' })
    return false
  }
  const height = numberOrNull(form.heightText)
  const weight = numberOrNull(form.weightText)
  if ((height == null) !== (weight == null)) {
    uni.showToast({ title: '身高和体重需同时填写', icon: 'none' })
    return false
  }
  if (height != null && (height < 50 || height > 260)) {
    uni.showToast({ title: '请输入有效的身高', icon: 'none' })
    return false
  }
  if (weight != null && (weight < 10 || weight > 300)) {
    uni.showToast({ title: '请输入有效的体重', icon: 'none' })
    return false
  }
  if (form.gradeLevel.length > 10) {
    uni.showToast({ title: '体测等级不能超过 10 个字符', icon: 'none' })
    return false
  }
  return true
}

function buildPayload(): PhysicalTestSave {
  return {
    testYear: Number(form.testYearText),
    testTerm: form.testTerm.trim(),
    height: numberOrNull(form.heightText),
    weight: numberOrNull(form.weightText),
    vitalCapacity: numberOrNull(form.vitalCapacityText),
    run50m: numberOrNull(form.run50mText),
    sitAndReach: numberOrNull(form.sitAndReachText),
    standingJump: numberOrNull(form.standingJumpText),
    enduranceRun: numberOrNull(form.enduranceRunText),
    strengthItem: numberOrNull(form.strengthItemText),
    totalScore: numberOrNull(form.totalScoreText),
    gradeLevel: form.gradeLevel.trim() || null,
    testDate: form.testDate || null,
    remark: form.remark.trim() || null,
  }
}

async function handleSave(): Promise<void> {
  if (saving.value) return
  if (!validate()) return
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  saving.value = true
  try {
    if (recordId.value) {
      await updateMyPhysicalTest(recordId.value, buildPayload())
    } else {
      await addMyPhysicalTest(buildPayload())
    }
    uni.showToast({ title: recordId.value ? '体测记录已更新' : '体测记录已新增', icon: 'success' })
    setTimeout(() => uni.navigateBack({ delta: 1 }), 500)
  } catch {
    // 请求层已提示
  } finally {
    saving.value = false
  }
}

onLoad((options) => {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  const id = Number(options?.id)
  if (id) loadRecord(id)
})
</script>

<style scoped>
.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.intro-card {
  margin-bottom: 20rpx;
}

.intro-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.intro-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 21rpx;
  color: #94a3b8;
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

.bmi-preview {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 24rpx;
  padding: 20rpx 22rpx;
  border-radius: 18rpx;
  background: linear-gradient(120deg, #f1fbf8, #e8f8f2);
}

.bmi-preview-label {
  display: block;
  font-size: 23rpx;
  font-weight: 600;
  color: #0f766e;
}

.bmi-preview-hint {
  display: block;
  margin-top: 2rpx;
  font-size: 19rpx;
  color: #8fb0a8;
}

.bmi-preview-value {
  font-size: 40rpx;
  font-weight: 700;
  color: #0f172a;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  padding-top: 4rpx;
}

.choice-chip {
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  background: #f4f8f6;
  color: #64748b;
  font-size: 22rpx;
  border: 2rpx solid transparent;
}

.choice-chip.active {
  background: #e6f7f3;
  border-color: #7fd9c8;
  color: #0f766e;
  font-weight: 600;
}

.hint-text {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  padding: 8rpx 8rpx 0;
  font-size: 20rpx;
  line-height: 1.6;
  color: #a8b7b2;
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
