<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { saveMyHealthProfile } from '@/api/health-profile'
import { useUserStore } from '@/stores/user'
import {
  BLOOD_TYPE_OPTIONS,
  bmiLevelOf,
  bmiLevelTagType,
  calcBmi,
  formatNumber,
  isFutureDate,
} from '@/utils/health'
import type { HealthProfile, HealthProfileSave } from '@/types/health'

interface ProfileForm {
  gender: number | null
  birthDate: string | null
  bloodType: string | null
  height: number | null
  weight: number | null
  college: string
  major: string
  grade: string
  className: string
  chronicDisease: string
  allergyHistory: string
  emergencyContact: string
  emergencyPhone: string
  remark: string
}

const props = defineProps<{
  profile: HealthProfile | null
}>()

const emit = defineEmits<{
  cancel: []
  saved: [profile: HealthProfile]
}>()

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const saving = ref(false)

function buildForm(profile: HealthProfile | null): ProfileForm {
  return {
    gender: profile?.gender ?? null,
    birthDate: profile?.birthDate ?? null,
    bloodType: profile?.bloodType ?? null,
    height: profile?.height ?? null,
    weight: profile?.weight ?? null,
    college: profile?.college ?? userStore.userInfo?.deptName ?? '',
    major: profile?.major ?? '',
    grade: profile?.grade ?? '',
    className: profile?.className ?? '',
    chronicDisease: profile?.chronicDisease ?? '',
    allergyHistory: profile?.allergyHistory ?? '',
    emergencyContact: profile?.emergencyContact ?? '',
    emergencyPhone: profile?.emergencyPhone ?? '',
    remark: profile?.remark ?? '',
  }
}

const form = reactive<ProfileForm>(buildForm(props.profile))
const bmiPreview = computed(() => calcBmi(form.height, form.weight))
const bmiLevel = computed(() => bmiLevelOf(bmiPreview.value))
const isEditing = computed(() => Boolean(props.profile?.id))

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

function validateBirthDate(
  _rule: unknown,
  value: string | null,
  callback: (error?: Error) => void,
): void {
  if (value && isFutureDate(new Date(value))) {
    callback(new Error('出生日期必须早于今天'))
  } else {
    callback()
  }
}

const rules: FormRules<ProfileForm> = {
  birthDate: [{ validator: validateBirthDate, trigger: 'change' }],
  height: [{ validator: validateHeightWeight, trigger: 'change' }],
  weight: [{ validator: validateHeightWeight, trigger: 'change' }],
  bloodType: [{ max: 10, message: '血型长度不能超过 10 个字符', trigger: 'blur' }],
  college: [
    { max: 100, message: '学院名称不能超过 100 个字符', trigger: 'blur' },
  ],
  major: [
    { max: 100, message: '专业名称不能超过 100 个字符', trigger: 'blur' },
  ],
  grade: [{ max: 20, message: '年级不能超过 20 个字符', trigger: 'blur' }],
  className: [
    { max: 50, message: '班级名称不能超过 50 个字符', trigger: 'blur' },
  ],
  chronicDisease: [
    { max: 500, message: '慢性病史不能超过 500 个字符', trigger: 'blur' },
  ],
  allergyHistory: [
    { max: 500, message: '过敏史不能超过 500 个字符', trigger: 'blur' },
  ],
  emergencyContact: [
    { max: 50, message: '紧急联系人不能超过 50 个字符', trigger: 'blur' },
  ],
  emergencyPhone: [
    {
      pattern: /^$|^[0-9+\- ]{5,20}$/,
      message: '紧急联系电话格式不正确',
      trigger: 'blur',
    },
  ],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload: HealthProfileSave = {
    gender: form.gender,
    birthDate: form.birthDate || null,
    bloodType: form.bloodType?.trim() || null,
    height: form.height,
    weight: form.weight,
    college: form.college.trim() || null,
    major: form.major.trim() || null,
    grade: form.grade.trim() || null,
    className: form.className.trim() || null,
    chronicDisease: form.chronicDisease.trim() || null,
    allergyHistory: form.allergyHistory.trim() || null,
    emergencyContact: form.emergencyContact.trim() || null,
    emergencyPhone: form.emergencyPhone.trim() || null,
    remark: form.remark.trim() || null,
  }

  saving.value = true
  try {
    const updated = await saveMyHealthProfile(payload)
    ElMessage.success(isEditing.value ? '健康档案已更新' : '健康档案创建成功')
    emit('saved', updated)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="panel-card profile-form-card">
    <div class="form-head">
      <div>
        <h3>{{ isEditing ? '编辑健康档案' : '完善健康档案' }}</h3>
        <p>学号、姓名、年龄由系统维护，BMI 会根据身高体重自动计算</p>
      </div>
      <div class="bmi-preview">
        <span>BMI 预览</span>
        <strong>{{ bmiPreview == null ? '—' : formatNumber(bmiPreview) }}</strong>
        <el-tag
          v-if="bmiLevel"
          :type="bmiLevelTagType(bmiLevel)"
          size="small"
          effect="light"
        >
          {{ bmiLevel }}
        </el-tag>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="96px"
      class="profile-form"
    >
      <div class="form-section">
        <div class="section-title">
          <span class="section-index">01</span>
          <div>
            <h4>基础信息</h4>
            <p>用于计算年龄与基础健康指标</p>
          </div>
        </div>
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :value="0">未知</el-radio>
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                placeholder="请选择出生日期"
                :disabled-date="isFutureDate"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="血型" prop="bloodType">
              <el-select
                v-model="form.bloodType"
                filterable
                allow-create
                clearable
                default-first-option
                placeholder="请选择或输入血型"
                style="width: 100%"
              >
                <el-option
                  v-for="item in BLOOD_TYPE_OPTIONS"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="联系电话">
              <el-input :model-value="profile?.phone || '—'" disabled />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-title">
          <span class="section-index">02</span>
          <div>
            <h4>学籍信息</h4>
            <p>便于校医查看与统计归档</p>
          </div>
        </div>
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="学院" prop="college">
              <el-input v-model="form.college" maxlength="100" placeholder="请输入学院" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="专业" prop="major">
              <el-input v-model="form.major" maxlength="100" placeholder="请输入专业" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="年级" prop="grade">
              <el-input v-model="form.grade" maxlength="20" placeholder="如 2023 级" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="班级" prop="className">
              <el-input v-model="form.className" maxlength="50" placeholder="请输入班级" />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-title">
          <span class="section-index">03</span>
          <div>
            <h4>身体指标</h4>
            <p>身高和体重需同时填写，用于计算 BMI</p>
          </div>
        </div>
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="身高" prop="height">
              <el-input-number
                v-model="form.height"
                :min="50"
                :max="250"
                :step="0.5"
                :precision="2"
                controls-position="right"
                placeholder="cm"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="体重" prop="weight">
              <el-input-number
                v-model="form.weight"
                :min="10"
                :max="500"
                :step="0.5"
                :precision="2"
                controls-position="right"
                placeholder="kg"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-title">
          <span class="section-index">04</span>
          <div>
            <h4>健康风险</h4>
            <p>如无相关情况可留空</p>
          </div>
        </div>
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="慢性病史" prop="chronicDisease">
              <el-input
                v-model="form.chronicDisease"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="如高血压、糖尿病等，没有可留空"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="过敏史" prop="allergyHistory">
              <el-input
                v-model="form.allergyHistory"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="如药物、食物过敏等，没有可留空"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-title">
          <span class="section-index">05</span>
          <div>
            <h4>紧急联系人</h4>
            <p>用于突发健康情况下的联系</p>
          </div>
        </div>
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="联系人" prop="emergencyContact">
              <el-input
                v-model="form.emergencyContact"
                maxlength="50"
                placeholder="请输入紧急联系人姓名"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="联系电话" prop="emergencyPhone">
              <el-input
                v-model="form.emergencyPhone"
                maxlength="20"
                placeholder="请输入紧急联系电话"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
                placeholder="其他需要说明的健康信息"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </div>
    </el-form>

    <div class="form-actions">
      <el-button @click="emit('cancel')">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">
        {{ isEditing ? '保存修改' : '创建档案' }}
      </el-button>
    </div>
  </section>
</template>

<style scoped>
.profile-form-card {
  padding: 24px 26px;
}

.form-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 20px;
  margin-bottom: 6px;
  border-bottom: 1px solid #edf3f0;
}

.form-head h3 {
  margin-bottom: 5px;
  font-size: 17px;
  font-weight: 700;
  color: var(--hp-text);
}

.form-head p {
  font-size: 12px;
  color: var(--hp-text-muted);
}

.bmi-preview {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 14px;
  font-size: 11.5px;
  color: var(--hp-text-muted);
  background: #f2faf6;
  border: 1px solid #dcefe7;
  border-radius: 10px;
  white-space: nowrap;
}

.bmi-preview strong {
  font-size: 20px;
  font-weight: 700;
  color: var(--hp-primary-dark);
}

.form-section {
  padding: 22px 0 4px;
}

.form-section + .form-section {
  border-top: 1px dashed #e8f0ed;
}

.section-title {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
}

.section-index {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 28px;
  height: 28px;
  font-size: 10px;
  font-weight: 700;
  color: var(--hp-primary);
  background: #e8f6f1;
  border-radius: 8px;
}

.section-title h4 {
  margin-bottom: 3px;
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.section-title p {
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.profile-form :deep(.el-form-item__label) {
  color: var(--hp-text-secondary);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 20px;
  margin-top: 8px;
  border-top: 1px solid #edf3f0;
}

@media (max-width: 768px) {
  .profile-form-card {
    padding: 18px 14px;
  }

  .form-head {
    flex-direction: column;
  }

  .bmi-preview {
    align-self: stretch;
    justify-content: center;
  }
}
</style>
