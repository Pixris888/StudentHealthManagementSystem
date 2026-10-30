<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  addMyPhysicalTest,
  updateMyPhysicalTest,
} from '@/api/health-physical'
import {
  calcBmi,
  bmiLevelOf,
  bmiLevelTagType,
  formatNumber,
  GRADE_LEVEL_OPTIONS,
  isFutureDate,
  TEST_TERM_OPTIONS,
} from '@/utils/health'
import type { PhysicalTest, PhysicalTestSave } from '@/types/health'

interface PhysicalTestForm {
  testYear: number | null
  testTerm: string
  height: number | null
  weight: number | null
  vitalCapacity: number | null
  run50m: number | null
  sitAndReach: number | null
  standingJump: number | null
  enduranceRun: number | null
  strengthItem: number | null
  totalScore: number | null
  gradeLevel: string
  testDate: string | null
}

const visible = defineModel<boolean>({ required: true })

const props = defineProps<{
  record?: PhysicalTest | null
}>()

const emit = defineEmits<{ saved: [] }>()

const formRef = ref<FormInstance>()
const saving = ref(false)

function todayString(): string {
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function buildForm(record?: PhysicalTest | null): PhysicalTestForm {
  return {
    testYear: record?.testYear ?? new Date().getFullYear(),
    testTerm: record?.testTerm ?? '上学期',
    height: record?.height ?? null,
    weight: record?.weight ?? null,
    vitalCapacity: record?.vitalCapacity ?? null,
    run50m: record?.run50m ?? null,
    sitAndReach: record?.sitAndReach ?? null,
    standingJump: record?.standingJump ?? null,
    enduranceRun: record?.enduranceRun ?? null,
    strengthItem: record?.strengthItem ?? null,
    totalScore: record?.totalScore ?? null,
    gradeLevel: record?.gradeLevel ?? '',
    testDate: record?.testDate ?? todayString(),
  }
}

const form = reactive<PhysicalTestForm>(buildForm(props.record))
const isEditing = computed(() => props.record?.id != null)
const bmiPreview = computed(() => calcBmi(form.height, form.weight))
const bmiLevel = computed(() => bmiLevelOf(bmiPreview.value))

function resetForm(): void {
  Object.assign(form, buildForm(props.record))
  nextTick(() => formRef.value?.clearValidate())
}

watch(
  [visible, () => props.record],
  ([open]) => {
    if (open) resetForm()
  },
)

function validateHeightWeight(
  _rule: unknown,
  _value: unknown,
  callback: (error?: Error) => void,
): void {
  const hasHeight = form.height != null
  const hasWeight = form.weight != null
  if (hasHeight !== hasWeight) {
    callback(new Error('身高和体重需同时填写'))
  } else {
    callback()
  }
}

function validateTerm(
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void,
): void {
  if (!value || !value.trim()) {
    callback(new Error('请选择或输入测试学期'))
  } else {
    callback()
  }
}

function validateTestDate(
  _rule: unknown,
  value: string | null,
  callback: (error?: Error) => void,
): void {
  if (value && isFutureDate(new Date(value))) {
    callback(new Error('测试日期不能晚于今天'))
  } else {
    callback()
  }
}

const rules: FormRules<PhysicalTestForm> = {
  testYear: [{ required: true, message: '请输入测试年份', trigger: 'change' }],
  testTerm: [{ validator: validateTerm, trigger: 'change' }],
  height: [{ validator: validateHeightWeight, trigger: 'change' }],
  weight: [{ validator: validateHeightWeight, trigger: 'change' }],
  testDate: [{ validator: validateTestDate, trigger: 'change' }],
  gradeLevel: [
    { max: 10, message: '体测等级不能超过 10 个字符', trigger: 'blur' },
  ],
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload: PhysicalTestSave = {
    testYear: form.testYear as number,
    testTerm: form.testTerm.trim(),
    height: form.height,
    weight: form.weight,
    vitalCapacity: form.vitalCapacity,
    run50m: form.run50m,
    sitAndReach: form.sitAndReach,
    standingJump: form.standingJump,
    enduranceRun: form.enduranceRun,
    strengthItem: form.strengthItem,
    totalScore: form.totalScore,
    gradeLevel: form.gradeLevel.trim() || null,
    testDate: form.testDate || null,
  }

  saving.value = true
  try {
    if (isEditing.value && props.record) {
      await updateMyPhysicalTest(props.record.id, payload)
      ElMessage.success('体测记录已更新')
    } else {
      await addMyPhysicalTest(payload)
      ElMessage.success('体测记录新增成功')
    }
    visible.value = false
    emit('saved')
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
    :title="isEditing ? '编辑体测记录' : '新增体测记录'"
    width="760px"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-alert
      title="测试年份和学期必填；身高与体重需同时填写；同一学年学期只能保留一条记录。"
      type="info"
      :closable="false"
      show-icon
      class="form-tip"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="96px"
      class="physical-form"
    >
      <div class="form-section">
        <div class="section-head">
          <span>01</span>
          <div>
            <h4>测试信息</h4>
            <p>确定本次体测所属的学年学期</p>
          </div>
        </div>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="测试年份" prop="testYear">
              <el-input-number
                v-model="form.testYear"
                :min="2000"
                :max="2100"
                :precision="0"
                controls-position="right"
                placeholder="如 2025"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="测试学期" prop="testTerm">
              <el-select
                v-model="form.testTerm"
                filterable
                allow-create
                default-first-option
                placeholder="请选择或输入学期"
                style="width: 100%"
              >
                <el-option
                  v-for="item in TEST_TERM_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="测试日期" prop="testDate">
              <el-date-picker
                v-model="form.testDate"
                type="date"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                placeholder="请选择测试日期"
                :disabled-date="isFutureDate"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="体测等级" prop="gradeLevel">
              <el-select
                v-model="form.gradeLevel"
                clearable
                placeholder="请选择等级"
                style="width: 100%"
              >
                <el-option
                  v-for="item in GRADE_LEVEL_OPTIONS"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="体测总分">
              <el-input-number
                v-model="form.totalScore"
                :min="0"
                :max="200"
                :precision="2"
                :step="1"
                controls-position="right"
                placeholder="0-200"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-head">
          <span>02</span>
          <div>
            <h4>身体指标</h4>
            <p>BMI 将根据身高和体重自动计算</p>
          </div>
        </div>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="身高" prop="height">
              <el-input-number
                v-model="form.height"
                :min="50"
                :max="250"
                :precision="2"
                :step="0.5"
                controls-position="right"
                placeholder="cm"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="体重" prop="weight">
              <el-input-number
                v-model="form.weight"
                :min="10"
                :max="500"
                :precision="2"
                :step="0.5"
                controls-position="right"
                placeholder="kg"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="BMI">
              <div class="bmi-preview">
                <strong>{{ formatNumber(bmiPreview) }}</strong>
                <el-tag
                  v-if="bmiLevel"
                  :type="bmiLevelTagType(bmiLevel)"
                  size="small"
                  effect="light"
                >
                  {{ bmiLevel }}
                </el-tag>
                <span>自动计算</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="肺活量">
              <el-input-number
                v-model="form.vitalCapacity"
                :min="0"
                :max="20000"
                :precision="0"
                :step="50"
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
          <span>03</span>
          <div>
            <h4>体能项目</h4>
            <p>未参加的项目可以留空</p>
          </div>
        </div>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="50 米跑">
              <el-input-number
                v-model="form.run50m"
                :min="0"
                :max="120"
                :precision="2"
                :step="0.1"
                controls-position="right"
                placeholder="秒"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="坐位体前屈">
              <el-input-number
                v-model="form.sitAndReach"
                :min="-50"
                :max="100"
                :precision="2"
                :step="0.5"
                controls-position="right"
                placeholder="cm"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="立定跳远">
              <el-input-number
                v-model="form.standingJump"
                :min="0"
                :max="500"
                :precision="2"
                :step="1"
                controls-position="right"
                placeholder="cm"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="耐力跑">
              <el-input-number
                v-model="form.enduranceRun"
                :min="0"
                :max="3600"
                :precision="2"
                :step="1"
                controls-position="right"
                placeholder="秒"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="力量项目">
              <el-input-number
                v-model="form.strengthItem"
                :min="0"
                :max="300"
                :precision="0"
                :step="1"
                controls-position="right"
                placeholder="个"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-tip {
  margin-bottom: 20px;
}

.form-section {
  padding: 4px 0 6px;
}

.form-section + .form-section {
  padding-top: 18px;
  margin-top: 4px;
  border-top: 1px dashed #e8f0ed;
}

.section-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 16px;
}

.section-head > span {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 26px;
  height: 26px;
  font-size: 9.5px;
  font-weight: 700;
  color: var(--hp-primary);
  background: #e8f6f1;
  border-radius: 8px;
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

/* 字段较多，限制表单区域高度，保证底部按钮始终可见 */
.physical-form {
  max-height: 58vh;
  padding-right: 8px;
  overflow-y: auto;
}

.physical-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.bmi-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  color: var(--hp-text-muted);
  font-size: 11px;
}

.bmi-preview strong {
  font-size: 20px;
  font-weight: 700;
  color: var(--hp-primary-dark);
}

@media (max-width: 600px) {
  .physical-form {
    max-height: 52vh;
  }

  .physical-form :deep(.el-form-item__label) {
    width: 86px !important;
  }
}
</style>
