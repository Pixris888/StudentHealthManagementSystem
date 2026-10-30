<template>
  <view class="login-page">
    <view v-if="checking" class="checking-mask">
      <image src="/static/logo-192.png" class="checking-logo" mode="aspectFit" />
      <text class="checking-text">正在恢复登录状态…</text>
    </view>

    <view class="login-hero">
      <view class="hero-orb orb-one" />
      <view class="hero-orb orb-two" />
      <view class="hero-orb orb-three" />

      <view :style="{ height: `${statusBarHeight}px` }" />

      <view class="brand-row">
        <image src="/static/logo-192.png" class="brand-logo" mode="aspectFit" />
        <view class="brand-copy">
          <text class="brand-name">大学生健康管理平台</text>
          <text class="brand-sub">STUDENT HEALTH · 学生端</text>
        </view>
      </view>

      <view class="hero-copy">
        <text class="hero-title">把每一天的健康，</text>
        <text class="hero-title">变成看得见的成长</text>
        <text class="hero-desc">健康档案、每日打卡、AI 助手与健康科普，装进口袋里。</text>
      </view>

      <view class="feature-row">
        <view class="feature-pill">
          <uni-icons type="calendar" size="15" color="#d9fff7" />
          <text>每日打卡</text>
        </view>
        <view class="feature-pill">
          <uni-icons type="chatbubble" size="15" color="#d9fff7" />
          <text>AI 问答</text>
        </view>
        <view class="feature-pill">
          <uni-icons type="list" size="15" color="#d9fff7" />
          <text>健康知识</text>
        </view>
      </view>
    </view>

    <view class="login-body">
      <view class="login-card fade-in">
        <view class="login-heading">
          <text class="login-eyebrow">WELCOME BACK</text>
          <text class="login-title">欢迎回来</text>
          <text class="login-sub">登录后继续记录你的健康生活</text>
        </view>

        <view class="form-item">
          <text class="form-label">登录账号</text>
          <view class="input-box">
            <uni-icons type="person" size="18" color="#94a3b8" />
            <input
              v-model="form.username"
              class="input login-input"
              placeholder="请输入学号或登录账号"
              placeholder-class="input-placeholder"
              :maxlength="50"
              confirm-type="next"
            />
            <view v-if="form.username" class="clear-btn" @click="form.username = ''">
              <uni-icons type="clear" size="16" color="#c4d0cc" />
            </view>
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">登录密码</text>
          <view class="input-box">
            <uni-icons type="locked" size="18" color="#94a3b8" />
            <input
              v-model="form.password"
              class="input login-input"
              :password="!showPassword"
              placeholder="请输入登录密码"
              placeholder-class="input-placeholder"
              :maxlength="72"
              confirm-type="done"
              @confirm="handleLogin"
            />
            <view class="eye-btn" @click="showPassword = !showPassword">
              <uni-icons
                :type="showPassword ? 'eye-filled' : 'eye'"
                size="19"
                color="#94a3b8"
              />
            </view>
          </view>
        </view>

        <view class="login-options">
          <view class="remember-row" @click="rememberMe = !rememberMe">
            <view class="checkbox" :class="{ checked: rememberMe }">
              <uni-icons v-if="rememberMe" type="checkmarkempty" size="13" color="#ffffff" />
            </view>
            <text class="remember-text">记住账号</text>
          </view>
          <text class="helper-text">忘记密码请联系校医或管理员</text>
        </view>

        <button
          class="btn btn-primary btn-block login-btn"
          :disabled="loading"
          @click="handleLogin"
        >
          <text>{{ loading ? '登录中…' : '登录' }}</text>
          <uni-icons v-if="!loading" type="arrowright" size="18" color="#ffffff" />
        </button>

        <view class="register-row">
          <text class="register-hint">还没有账号？</text>
          <text class="register-link" @click="goRegister">立即注册</text>
        </view>

        <text class="login-tip">本 App 仅面向学生账号，管理端请使用电脑浏览器</text>
      </view>

      <view class="login-footer">
        <text>大学生健康管理平台 · v{{ version }}</text>
        <text>健康 · 学习 · 生活</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { getRememberedUsername, rememberUsername } from '@/utils/auth'
import { APP_VERSION } from '@/config'
import type { LoginForm } from '@/types/system'

const userStore = useUserStore()
const messageStore = useMessageStore()

const statusBarHeight = uni.getSystemInfoSync().statusBarHeight ?? 0
const remembered = getRememberedUsername()
const form = reactive<LoginForm>({
  username: remembered,
  password: '',
})
const rememberMe = ref(Boolean(remembered))
const showPassword = ref(false)
const loading = ref(false)
const checking = ref(false)
const version = APP_VERSION

onLoad(async () => {
  checking.value = true
  try {
    const restored = await userStore.bootstrap()
    if (restored) {
      messageStore.startPolling()
      uni.switchTab({ url: '/pages/home/index' })
      return
    }
  } catch {
    // bootstrap 内部已按 401 / 网络异常分别处理
  } finally {
    checking.value = false
  }
})

function goRegister(): void {
  uni.navigateTo({ url: '/pages/register/index' })
}

async function handleLogin(): Promise<void> {
  if (loading.value) return
  const username = form.username.trim()
  const password = form.password
  if (!username) {
    uni.showToast({ title: '请输入登录账号', icon: 'none' })
    return
  }
  if (!password) {
    uni.showToast({ title: '请输入登录密码', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await userStore.login({ username, password })
    rememberUsername(rememberMe.value ? username : '')
    messageStore.startPolling()
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/home/index' })
    }, 350)
  } catch (error) {
    const message = (error as Error | null)?.message || '登录失败，请稍后重试'
    if (message.includes('仅支持学生账号')) {
      uni.showModal({
        title: '账号类型受限',
        content: message,
        showCancel: false,
        confirmText: '我知道了',
      })
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: #f4f7f6;
}

.checking-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #0f9d8a, #0a6a61);
}

.checking-logo {
  width: 140rpx;
  height: 140rpx;
  border-radius: 34rpx;
  box-shadow: 0 20rpx 40rpx rgba(0, 0, 0, 0.18);
}

.checking-text {
  margin-top: 28rpx;
  color: rgba(255, 255, 255, 0.82);
  font-size: 26rpx;
}

.login-hero {
  position: relative;
  overflow: hidden;
  padding: 24rpx 40rpx 120rpx;
  background: linear-gradient(145deg, #12b39c 0%, #0f9d8a 42%, #0a6a61 100%);
  border-radius: 0 0 64rpx 64rpx;
}

.hero-orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.orb-one {
  width: 380rpx;
  height: 380rpx;
  top: -150rpx;
  right: -120rpx;
}

.orb-two {
  width: 220rpx;
  height: 220rpx;
  top: 220rpx;
  left: -100rpx;
  background: rgba(255, 255, 255, 0.06);
}

.orb-three {
  width: 120rpx;
  height: 120rpx;
  right: 70rpx;
  bottom: 120rpx;
  background: rgba(255, 255, 255, 0.05);
}

.brand-row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.brand-logo {
  width: 88rpx;
  height: 88rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.14);
}

.brand-copy {
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-size: 31rpx;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1rpx;
}

.brand-sub {
  margin-top: 6rpx;
  font-size: 19rpx;
  letter-spacing: 2rpx;
  color: rgba(255, 255, 255, 0.62);
}

.hero-copy {
  position: relative;
  z-index: 1;
  margin-top: 56rpx;
}

.hero-title {
  display: block;
  font-size: 46rpx;
  font-weight: 700;
  line-height: 1.35;
  color: #ffffff;
  white-space: pre-line;
}

.hero-desc {
  display: block;
  margin-top: 18rpx;
  font-size: 25rpx;
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.76);
}

.feature-row {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 34rpx;
}

.feature-pill {
  display: flex;
  align-items: center;
  gap: 8rpx;
  height: 56rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.13);
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  color: #e8fffa;
  font-size: 23rpx;
}

.login-body {
  position: relative;
  z-index: 2;
  margin-top: -72rpx;
  padding: 0 28rpx 48rpx;
}

.login-card {
  padding: 38rpx 32rpx 32rpx;
  border-radius: 36rpx;
  background: #ffffff;
  box-shadow: 0 24rpx 60rpx rgba(8, 74, 66, 0.12);
}

.login-eyebrow {
  display: block;
  font-size: 19rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
  color: #9ec9c0;
}

.login-title {
  display: block;
  margin-top: 8rpx;
  font-size: 44rpx;
  font-weight: 700;
  color: #0f172a;
}

.login-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 25rpx;
  color: #94a3b8;
}

.login-input {
  margin-left: 16rpx;
}

.clear-btn,
.eye-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 22rpx;
}

.remember-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
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
  transition: all 0.2s ease;
}

.checkbox.checked {
  border-color: #0f9d8a;
  background: #0f9d8a;
}

.remember-text {
  font-size: 24rpx;
  color: #64748b;
}

.helper-text {
  font-size: 22rpx;
  color: #a8b7b2;
}

.login-btn {
  margin-top: 34rpx;
  gap: 10rpx;
  height: 94rpx;
  font-size: 30rpx;
}

.register-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: 30rpx;
  font-size: 24rpx;
}

.register-hint {
  color: #94a3b8;
}

.register-link {
  color: #0f9d8a;
  font-weight: 600;
}

.login-tip {
  display: block;
  margin-top: 24rpx;
  text-align: center;
  font-size: 21rpx;
  color: #b1bfba;
}

.login-footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  padding: 40rpx 0 10rpx;
  font-size: 21rpx;
  color: #a8b7b2;
}
</style>
