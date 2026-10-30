<template>
  <view class="page">
    <view class="tip-card">
      <view class="tip-icon">
        <uni-icons type="locked" size="22" color="#0f766e" />
      </view>
      <view class="grow">
        <text class="tip-title">设置一个更安全的密码</text>
        <text class="tip-desc">
          密码需为 8-32 位，且包含大写字母、小写字母、数字、特殊字符中的至少三类。修改成功后当前登录状态会失效。
        </text>
      </view>
    </view>

    <view class="card">
      <view class="form-item">
        <text class="form-label">原密码 <text class="required">*</text></text>
        <view class="input-box">
          <input
            v-model="form.oldPassword"
            class="input"
            :password="!showPassword"
            maxlength="72"
            placeholder="请输入原密码"
            placeholder-class="input-placeholder"
            autocomplete="current-password"
          />
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">新密码 <text class="required">*</text></text>
        <view class="input-box">
          <input
            v-model="form.newPassword"
            class="input"
            :password="!showPassword"
            maxlength="32"
            placeholder="请输入新密码"
            placeholder-class="input-placeholder"
            autocomplete="new-password"
          />
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">确认新密码 <text class="required">*</text></text>
        <view class="input-box">
          <input
            v-model="form.confirmPassword"
            class="input"
            :password="!showPassword"
            maxlength="32"
            placeholder="请再次输入新密码"
            placeholder-class="input-placeholder"
            autocomplete="new-password"
          />
        </view>
      </view>

      <view class="show-password-row" @click="showPassword = !showPassword">
        <view class="checkbox" :class="{ checked: showPassword }">
          <uni-icons v-if="showPassword" type="checkmarkempty" size="12" color="#ffffff" />
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
        <text class="strength-hint">至少满足 3 类字符，且不能包含用户名或学号</text>
      </view>
    </view>

    <button class="btn btn-primary btn-block submit-btn" :disabled="submitting" @click="handleSubmit">
      {{ submitting ? '提交中…' : '确认修改' }}
    </button>

    <view class="security-tip">
      <uni-icons type="info" size="14" color="#94a3b8" />
      <text>请勿使用生日、手机号或连续数字作为密码。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { changePasswordApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { validatePassword } from '@/utils/password'

const userStore = useUserStore()
const messageStore = useMessageStore()

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const showPassword = ref(false)
const submitting = ref(false)

const strengthLevel = computed(() => {
  const password = form.newPassword
  if (!password) return 0
  const categories = [/[a-z]/, /[A-Z]/, /\d/, /[^A-Za-z0-9]/].filter((pattern) =>
    pattern.test(password),
  ).length
  if (password.length < 8) return 0
  if (categories <= 1) return 1
  if (categories === 2) return 2
  return 3
})

const strengthText = computed(() => {
  if (!form.newPassword) return '未输入'
  if (strengthLevel.value === 3) return '强'
  if (strengthLevel.value === 2) return '中'
  return '弱'
})

function handleSubmit(): void {
  if (submitting.value) return
  if (!form.oldPassword) {
    uni.showToast({ title: '请输入原密码', icon: 'none' })
    return
  }
  const message = validatePassword(form.newPassword, [
    userStore.userInfo?.username,
    userStore.studentNo,
  ])
  if (message) {
    uni.showToast({ title: message, icon: 'none', duration: 2800 })
    return
  }
  if (!form.confirmPassword) {
    uni.showToast({ title: '请再次输入新密码', icon: 'none' })
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return
  }

  submitting.value = true
  uni.showModal({
    title: '确认修改密码',
    content: '修改成功后当前登录状态会失效，需要使用新密码重新登录。',
    confirmText: '确认修改',
    success: async (res) => {
      if (!res.confirm) {
        submitting.value = false
        return
      }
      try {
        await changePasswordApi({
          oldPassword: form.oldPassword,
          newPassword: form.newPassword,
        })
        messageStore.reset()
        userStore.reset()
        uni.showToast({ title: '密码修改成功', icon: 'success' })
        setTimeout(() => {
          uni.reLaunch({ url: '/pages/login/index' })
        }, 600)
      } catch {
        // 请求层已提示
      } finally {
        submitting.value = false
      }
    },
    fail: () => {
      submitting.value = false
    },
  })
}
</script>

<style scoped>
.tip-card {
  display: flex;
  align-items: flex-start;
  gap: 18rpx;
  padding: 26rpx;
  border-radius: 26rpx;
  background: linear-gradient(120deg, #eaf9f4, #e2f5ef);
}

.tip-icon {
  width: 74rpx;
  height: 74rpx;
  border-radius: 22rpx;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tip-title {
  display: block;
  font-size: 27rpx;
  font-weight: 700;
  color: #0f766e;
}

.tip-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.7;
  color: #5d877d;
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
  width: 34rpx;
  height: 34rpx;
  border-radius: 10rpx;
  border: 2rpx solid #d3dfdb;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
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
  color: #a8b7b2;
}

.submit-btn {
  margin-top: 28rpx;
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
