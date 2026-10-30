<template>
  <view class="page page-with-fixed">
    <view class="card identity-card">
      <view class="identity-avatar">{{ avatarText }}</view>
      <view class="grow">
        <text class="identity-name">{{ displayName }}</text>
        <text class="identity-meta">
          {{ profile?.studentNo ? `学号 ${profile.studentNo}` : '学号未绑定' }}
        </text>
      </view>
      <view class="tag" :class="profile ? 'tag-success' : 'tag-warning'">
        {{ profile ? '已建档' : '待建档' }}
      </view>
    </view>

    <view class="tip-banner">
      <uni-icons type="info" size="16" color="#0f766e" />
      <text>健康档案仅用于校园健康评估，紧急联系人信息请确保准确。</text>
    </view>

    <view class="card">
      <view class="card-title-row">
        <text class="card-title">基础信息</text>
        <text class="muted">带 * 为重要信息</text>
      </view>

      <view class="form-item">
        <text class="form-label">性别</text>
        <picker
          :range="genderLabels"
          :value="genderIndex < 0 ? 0 : genderIndex"
          @change="onGenderChange"
        >
          <view class="picker-box">
            <text :class="form.gender == null ? 'picker-placeholder' : 'picker-value'">
              {{ form.gender == null ? '请选择性别' : genderLabel(form.gender) }}
            </text>
            <uni-icons type="arrowdown" size="15" color="#b1bfba" />
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">出生日期</text>
        <picker
          mode="date"
          :value="form.birthDate || today"
          start="1930-01-01"
          :end="today"
          @change="onBirthDateChange"
        >
          <view class="picker-box">
            <text :class="form.birthDate ? 'picker-value' : 'picker-placeholder'">
              {{ form.birthDate || '请选择出生日期' }}
            </text>
            <uni-icons type="calendar" size="16" color="#b1bfba" />
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">血型</text>
        <view class="input-box">
          <input
            v-model="form.bloodType"
            class="input"
            maxlength="10"
            placeholder="请选择或输入血型"
            placeholder-class="input-placeholder"
          />
        </view>
        <view class="blood-chip-row">
          <view
            v-for="item in bloodTypeOptions"
            :key="item"
            class="blood-chip"
            :class="{ active: form.bloodType === item }"
            @click="form.bloodType = form.bloodType === item ? '' : item"
          >
            {{ item }}
          </view>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="card-title-row">
        <text class="card-title">身体数据</text>
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

      <view class="bmi-card">
        <view class="grow">
          <text class="bmi-label">BMI 预览</text>
          <text class="bmi-hint">体重 ÷ 身高²</text>
        </view>
        <text class="bmi-value">{{ bmiPreview ?? '—' }}</text>
        <view class="tag" :class="bmiTagClass">{{ bmiLevelPreview }}</view>
      </view>
    </view>

    <view class="card">
      <view class="card-title-row">
        <text class="card-title">院系信息</text>
      </view>

      <view class="form-item">
        <text class="form-label">学院</text>
        <view class="input-box">
          <input
            v-model="form.college"
            class="input"
            placeholder="如 计算机科学与技术学院"
            placeholder-class="input-placeholder"
          />
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">专业</text>
        <view class="input-box">
          <input
            v-model="form.major"
            class="input"
            placeholder="如 计算机科学与技术"
            placeholder-class="input-placeholder"
          />
        </view>
      </view>

      <view class="double-row">
        <view class="form-item half">
          <text class="form-label">年级</text>
          <view class="input-box">
            <input
              v-model="form.grade"
              class="input"
              placeholder="如 2023 级"
              placeholder-class="input-placeholder"
            />
          </view>
        </view>
        <view class="form-item half">
          <text class="form-label">班级</text>
          <view class="input-box">
            <input
              v-model="form.className"
              class="input"
              placeholder="如 计科 1 班"
              placeholder-class="input-placeholder"
            />
          </view>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="card-title-row">
        <text class="card-title">健康信息</text>
      </view>

      <view class="form-item">
        <text class="form-label">慢性疾病</text>
        <view class="textarea-box">
          <textarea
            v-model="form.chronicDisease"
            class="textarea"
            maxlength="200"
            placeholder="没有可留空；如有请填写名称（如哮喘、高血压）"
            placeholder-class="input-placeholder"
          />
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">过敏史</text>
        <view class="textarea-box">
          <textarea
            v-model="form.allergyHistory"
            class="textarea"
            maxlength="200"
            placeholder="如 青霉素过敏、花粉过敏；没有可留空"
            placeholder-class="input-placeholder"
          />
        </view>
      </view>
    </view>

    <view class="card">
      <view class="card-title-row">
        <text class="card-title">紧急联系人</text>
      </view>

      <view class="double-row">
        <view class="form-item half">
          <text class="form-label">姓名</text>
          <view class="input-box">
            <input
              v-model="form.emergencyContact"
              class="input"
              placeholder="联系人姓名"
              placeholder-class="input-placeholder"
            />
          </view>
        </view>
        <view class="form-item half">
          <text class="form-label">电话</text>
          <view class="input-box">
            <input
              v-model="form.emergencyPhone"
              class="input"
              type="number"
              maxlength="20"
              placeholder="联系电话"
              placeholder-class="input-placeholder"
            />
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">备注</text>
        <view class="textarea-box">
          <textarea
            v-model="form.remark"
            class="textarea"
            maxlength="300"
            placeholder="其他需要说明的健康信息"
            placeholder-class="input-placeholder"
          />
        </view>
      </view>
    </view>

    <view class="footer-bar">
      <button class="btn btn-primary btn-block" :disabled="saving" @click="handleSave">
        {{ saving ? '保存中…' : '保存健康档案' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getMyHealthProfile, saveMyHealthProfile } from '@/api/health-profile'
import { useUserStore } from '@/stores/user'
import { getAccessToken } from '@/utils/auth'
import { BLOOD_TYPE_OPTIONS, bmiLevelTone, GENDER_OPTIONS, genderLabel } from '@/utils/dict'
import { todayString } from '@/utils/format'
import type { HealthProfile, HealthProfileSave } from '@/types/health'

interface ProfileForm {
  gender: number | null
  birthDate: string | null
  bloodType: string | null
  heightText: string
  weightText: string
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

const userStore = useUserStore()
const profile = ref<HealthProfile | null>(null)
const loading = ref(true)
const saving = ref(false)
const today = todayString()
const genderOptions = GENDER_OPTIONS
const genderLabels = GENDER_OPTIONS.map((item) => item.label)
const bloodTypeOptions = BLOOD_TYPE_OPTIONS
const genderIndex = ref(-1)

const form = reactive<ProfileForm>({
  gender: null,
  birthDate: null,
  bloodType: null,
  heightText: '',
  weightText: '',
  college: '',
  major: '',
  grade: '',
  className: '',
  chronicDisease: '',
  allergyHistory: '',
  emergencyContact: '',
  emergencyPhone: '',
  remark: '',
})

const displayName = computed(
  () => profile.value?.realName || userStore.userInfo?.realName || userStore.displayName,
)
const avatarText = computed(() => displayName.value.slice(0, 1))

const bmiPreview = computed<number | null>(() => {
  const height = Number(form.heightText)
  const weight = Number(form.weightText)
  if (!height || !weight || height < 50 || height > 260) return null
  const value = weight / ((height / 100) * (height / 100))
  if (!Number.isFinite(value)) return null
  return Math.round(value * 10) / 10
})

const bmiLevelPreview = computed(() => {
  const bmi = bmiPreview.value
  if (bmi == null) return '待计算'
  if (bmi < 18.5) return '偏瘦'
  if (bmi < 24) return '正常'
  if (bmi < 28) return '超重'
  return '肥胖'
})

const bmiTagClass = computed(() => `tag-${bmiLevelTone(bmiLevelPreview.value)}`)

function numberOrNull(value: string): number | null {
  const text = value.trim()
  if (!text) return null
  const number = Number(text)
  return Number.isFinite(number) ? number : null
}

function textOrNull(value: string): string | null {
  const text = value.trim()
  return text || null
}

function onGenderChange(event: { detail: { value: string | number } }): void {
  const index = Number(event.detail.value)
  genderIndex.value = index
  form.gender = genderOptions[index]?.value ?? null
}

function onBirthDateChange(event: { detail: { value: string } }): void {
  form.birthDate = event.detail.value || null
}

function applyProfile(data: HealthProfile | null): void {
  profile.value = data
  if (!data) return
  form.gender = data.gender ?? null
  form.birthDate = data.birthDate || null
  form.bloodType = data.bloodType || null
  form.heightText = data.height != null ? String(data.height) : ''
  form.weightText = data.weight != null ? String(data.weight) : ''
  form.college = data.college || ''
  form.major = data.major || ''
  form.grade = data.grade || ''
  form.className = data.className || ''
  form.chronicDisease = data.chronicDisease || ''
  form.allergyHistory = data.allergyHistory || ''
  form.emergencyContact = data.emergencyContact || ''
  form.emergencyPhone = data.emergencyPhone || ''
  form.remark = data.remark || ''
  genderIndex.value = genderOptions.findIndex((item) => item.value === form.gender)
}

async function loadProfile(): Promise<void> {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (!userStore.userInfo) {
    try {
      await userStore.bootstrap()
    } catch {
      uni.reLaunch({ url: '/pages/login/index' })
      return
    }
  }
  loading.value = true
  try {
    const data = await getMyHealthProfile()
    applyProfile(data ?? null)
  } catch {
    applyProfile(null)
  } finally {
    loading.value = false
  }
}

function buildPayload(): HealthProfileSave {
  return {
    gender: form.gender,
    birthDate: form.birthDate,
    bloodType: form.bloodType,
    height: numberOrNull(form.heightText),
    weight: numberOrNull(form.weightText),
    college: textOrNull(form.college),
    major: textOrNull(form.major),
    grade: textOrNull(form.grade),
    className: textOrNull(form.className),
    chronicDisease: textOrNull(form.chronicDisease),
    allergyHistory: textOrNull(form.allergyHistory),
    emergencyContact: textOrNull(form.emergencyContact),
    emergencyPhone: textOrNull(form.emergencyPhone),
    remark: textOrNull(form.remark),
  }
}

async function handleSave(): Promise<void> {
  if (saving.value) return
  if (form.heightText && !numberOrNull(form.heightText)) {
    uni.showToast({ title: '请输入有效的身高', icon: 'none' })
    return
  }
  if (form.weightText && !numberOrNull(form.weightText)) {
    uni.showToast({ title: '请输入有效的体重', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const data = await saveMyHealthProfile(buildPayload())
    applyProfile(data ?? null)
    uni.showToast({ title: '档案已保存', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack({ delta: 1 })
    }, 500)
  } catch {
    // 错误已由请求层提示
  } finally {
    saving.value = false
  }
}

onLoad(loadProfile)
</script>

<style scoped>
.identity-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.identity-avatar {
  width: 92rpx;
  height: 92rpx;
  border-radius: 28rpx;
  background: #e6f7f3;
  color: #0f766e;
  font-size: 38rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.identity-name {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.identity-meta {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.tip-banner {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  margin-top: 20rpx;
  padding: 18rpx 22rpx;
  border-radius: 18rpx;
  background: #eaf9f4;
  font-size: 22rpx;
  line-height: 1.6;
  color: #417a70;
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

.blood-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 14rpx;
}

.blood-chip {
  height: 52rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: #f4f8f6;
  color: #64748b;
  font-size: 22rpx;
  display: flex;
  align-items: center;
  border: 2rpx solid transparent;
}

.blood-chip.active {
  background: #e6f7f3;
  border-color: #7fd9c8;
  color: #0f766e;
  font-weight: 600;
}

.double-row {
  display: flex;
  gap: 18rpx;
}

.half {
  flex: 1;
  min-width: 0;
}

.bmi-card {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 26rpx;
  padding: 22rpx;
  border-radius: 20rpx;
  background: linear-gradient(120deg, #f1fbf8, #e7f7f2);
}

.bmi-label {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: #0f766e;
}

.bmi-hint {
  display: block;
  margin-top: 2rpx;
  font-size: 20rpx;
  color: #8fb0a8;
}

.bmi-value {
  font-size: 42rpx;
  font-weight: 700;
  color: #0f172a;
  line-height: 1;
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
