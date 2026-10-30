<template>
  <view class="page register-page">
    <view class="intro-card">
      <view class="intro-icon">
        <uni-icons type="personadd" size="24" color="#0f766e" />
      </view>
      <view class="grow">
        <text class="intro-title">创建学生账号</text>
        <text class="intro-desc">
          请填写真实信息，注册成功后账号将自动绑定学生角色并完成登录。
        </text>
      </view>
    </view>

    <view class="card">
      <view class="form-item">
        <text class="form-label">登录账号 <text class="required">*</text></text>
        <view class="input-box">
          <input
            v-model="form.username"
            class="input"
            placeholder="4-20 位，以字母开头"
            placeholder-class="input-placeholder"
            maxlength="20"
            autocomplete="username"
          />
        </view>
      </view>

      <view class="double-row">
        <view class="form-item half">
          <text class="form-label">真实姓名 <text class="required">*</text></text>
          <view class="input-box">
            <input
              v-model="form.realName"
              class="input"
              placeholder="请输入真实姓名"
              placeholder-class="input-placeholder"
              maxlength="50"
            />
          </view>
        </view>
        <view class="form-item half">
          <text class="form-label">学号 <text class="required">*</text></text>
          <view class="input-box">
            <input
              v-model="form.studentNo"
              class="input"
              placeholder="4-30 位字母或数字"
              placeholder-class="input-placeholder"
              maxlength="30"
            />
          </view>
        </view>
      </view>

      <view class="double-row">
        <view class="form-item half">
          <text class="form-label">性别</text>
          <picker
            :range="genderLabels"
            :value="genderIndex"
            @change="onGenderChange"
          >
            <view class="input-box picker-box">
              <text :class="genderIndex === 0 ? 'picker-placeholder' : 'picker-value'">
                {{ genderLabels[genderIndex] }}
              </text>
              <uni-icons type="arrowdown" size="16" color="#94a3b8" />
            </view>
          </picker>
        </view>
        <view class="form-item half">
          <text class="form-label">手机号（选填）</text>
          <view class="input-box">
            <input
              v-model="form.phone"
              class="input"
              type="number"
              placeholder="请输入手机号"
              placeholder-class="input-placeholder"
              maxlength="20"
            />
          </view>
        </view>
      </view>

      <view class="double-row">
        <view class="form-item half">
          <text class="form-label">登录密码 <text class="required">*</text></text>
          <view class="input-box">
            <input
              v-model="form.password"
              class="input"
              :password="!showPassword"
              placeholder="8-32 位，至少三类字符"
              placeholder-class="input-placeholder"
              maxlength="32"
              autocomplete="new-password"
            />
          </view>
        </view>
        <view class="form-item half">
          <text class="form-label">确认密码 <text class="required">*</text></text>
          <view class="input-box">
            <input
              v-model="form.confirmPassword"
              class="input"
              :password="!showPassword"
              placeholder="请再次输入密码"
              placeholder-class="input-placeholder"
              maxlength="32"
              autocomplete="new-password"
            />
          </view>
        </view>
      </view>

      <view class="show-password-row" @click="showPassword = !showPassword">
        <view class="checkbox" :class="{ checked: showPassword }">
          <uni-icons
            v-if="showPassword"
            type="checkmarkempty"
            size="12"
            color="#ffffff"
          />
        </view>
        <text>显示密码</text>
      </view>

      <view class="strength-card">
        <view class="strength-row">
          <text class="strength-label">密码强度提示</text>
          <text class="strength-value">{{ strengthText }}</text>
        </view>
        <view class="strength-bars">
          <view
            v-for="index in 3"
            :key="index"
            class="strength-bar"
            :class="{ filled: strengthLevel >= index }"
          />
        </view>
        <text class="strength-hint">
          至少满足大写字母、小写字母、数字、特殊字符中的三类，且不能包含账号或学号
        </text>
      </view>

      <view class="form-item">
        <text class="form-label">邮箱（选填）</text>
        <view class="input-box">
          <input
            v-model="form.email"
            class="input"
            type="text"
            placeholder="请输入常用邮箱"
            placeholder-class="input-placeholder"
            maxlength="100"
            autocomplete="email"
          />
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">院系（选填）</text>
        <view class="input-box">
          <input
            v-model="form.deptName"
            class="input"
            placeholder="如 计算机学院"
            placeholder-class="input-placeholder"
            maxlength="100"
          />
        </view>
      </view>
    </view>

    <button
      class="btn btn-primary btn-block submit-btn"
      :disabled="submitting"
      @click="handleRegister"
    >
      {{ submitting ? '注册中…' : '注册并登录' }}
    </button>

    <view class="back-row" @click="goLogin">
      <text>已有账号？</text>
      <text class="back-link">返回登录</text>
    </view>

    <view class="security-tip">
      <uni-icons type="info" size="14" color="#94a3b8" />
      <text>页面不会展示或记录密码明文，请妥善保管账号信息。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { validatePassword } from '@/utils/password'
import type { RegisterForm } from '@/types/system'

const USERNAME_PATTERN = /^[A-Za-z][A-Za-z0-9_]{3,19}$/
const STUDENT_NO_PATTERN = /^[A-Za-z0-9]{4,30}$/
const PHONE_PATTERN = /^1[3-9]\d{9}$/
const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const userStore = useUserStore()
const messageStore = useMessageStore()

const form = reactive<RegisterForm>({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  studentNo: '',
  gender: 0,
  phone: '',
  email: '',
  deptName: '',
})
const showPassword = ref(false)
const submitting = ref(false)
const genderIndex = ref(0)
const genderLabels = ['未填写', '男', '女']
const genderValues = [0, 1, 2]

const strengthLevel = computed(() => {
  const password = form.password || ''
  if (password.length < 8) return 0
  const categories = [/[a-z]/, /[A-Z]/, /\d/, /[^A-Za-z0-9]/].filter((pattern) =>
    pattern.test(password),
  ).length
  if (categories <= 1) return 1
  if (categories === 2) return 2
  return 3
})

const strengthText = computed(() => {
  if (!form.password) return '未输入'
  if (strengthLevel.value === 3) return '强'
  if (strengthLevel.value === 2) return '中'
  return '弱'
})

function trim(value?: string): string {
  return (value ?? '').trim()
}

function onGenderChange(event: { detail: { value: string | number } }): void {
  const index = Number(event.detail.value)
  genderIndex.value = Number.isInteger(index) && index >= 0 && index < genderValues.length
    ? index
    : 0
  form.gender = genderValues[genderIndex.value]
}

function firstError(): string | null {
  const username = trim(form.username)
  const studentNo = trim(form.studentNo)
  if (!username) return '请输入登录账号'
  if (!USERNAME_PATTERN.test(username)) {
    return '账号需为 4-20 位，以字母开头，仅包含字母、数字、下划线'
  }
  if (!trim(form.realName)) return '请输入真实姓名'
  if (trim(form.realName).length > 50) return '真实姓名不能超过 50 个字符'
  if (!studentNo) return '请输入学号'
  if (!STUDENT_NO_PATTERN.test(studentNo)) return '学号需为 4-30 位字母或数字'

  const passwordMessage = validatePassword(form.password || '', [username, studentNo])
  if (passwordMessage) return passwordMessage
  if (!form.confirmPassword) return '请再次输入密码'
  if (form.confirmPassword !== form.password) return '两次输入的密码不一致'
  if (trim(form.phone) && !PHONE_PATTERN.test(trim(form.phone))) {
    return '手机号格式不正确'
  }
  if (trim(form.email) && !EMAIL_PATTERN.test(trim(form.email))) {
    return '邮箱格式不正确'
  }
  if (trim(form.deptName).length > 100) return '院系名称不能超过 100 个字符'
  return null
}

async function handleRegister(): Promise<void> {
  if (submitting.value) return
  const error = firstError()
  if (error) {
    uni.showToast({ title: error, icon: 'none', duration: 2800 })
    return
  }

  submitting.value = true
  try {
    await userStore.register({
      username: trim(form.username),
      password: form.password,
      confirmPassword: form.confirmPassword,
      realName: trim(form.realName),
      studentNo: trim(form.studentNo),
      gender: form.gender ?? 0,
      phone: trim(form.phone) || undefined,
      email: trim(form.email) || undefined,
      deptName: trim(form.deptName) || undefined,
    })
    messageStore.startPolling()
    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/home/index' })
    }, 350)
  } catch {
    // 请求层已统一提示错误
  } finally {
    submitting.value = false
  }
}

function goLogin(): void {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<style scoped>
.register-page {
  padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
}

.intro-card {
  display: flex;
  align-items: flex-start;
  gap: 18rpx;
  padding: 26rpx;
  border-radius: 26rpx;
  background: linear-gradient(120deg, #eaf9f4, #e2f5ef);
}

.intro-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 74rpx;
  height: 74rpx;
  flex-shrink: 0;
  border-radius: 22rpx;
  background: #ffffff;
}

.intro-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #0f766e;
}

.intro-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.7;
  color: #5d877d;
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
  justify-content: space-between;
}

.picker-placeholder {
  font-size: 28rpx;
  color: #b1bfba;
}

.picker-value {
  font-size: 28rpx;
  color: #0f172a;
}

.show-password-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 24rpx;
  font-size: 23rpx;
  color: #64748b;
}

.checkbox {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34rpx;
  height: 34rpx;
  border: 2rpx solid #d3dfdb;
  border-radius: 10rpx;
}

.checkbox.checked {
  border-color: #0f9d8a;
  background: #0f9d8a;
}

.strength-card {
  margin-top: 26rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: #f7faf9;
}

.strength-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.strength-label {
  font-size: 22rpx;
  color: #64748b;
}

.strength-value {
  font-size: 22rpx;
  font-weight: 600;
  color: #0f766e;
}

.strength-bars {
  display: flex;
  gap: 8rpx;
  margin-top: 12rpx;
}

.strength-bar {
  flex: 1;
  height: 10rpx;
  border-radius: 999rpx;
  background: #e4ece9;
}

.strength-bar.filled {
  background: linear-gradient(90deg, #12b39c, #0b7f70);
}

.strength-hint {
  display: block;
  margin-top: 10rpx;
  font-size: 19rpx;
  line-height: 1.6;
  color: #a8b7b2;
}

.submit-btn {
  margin-top: 30rpx;
}

.back-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  margin-top: 26rpx;
  font-size: 24rpx;
  color: #94a3b8;
}

.back-link {
  color: #0f9d8a;
  font-weight: 600;
}

.security-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  padding: 28rpx 0 10rpx;
  font-size: 20rpx;
  color: #a8b7b2;
}
</style>
