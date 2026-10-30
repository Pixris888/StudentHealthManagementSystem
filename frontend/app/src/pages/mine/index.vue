<template>
  <view class="mine-page">
    <view class="mine-hero">
      <view class="mine-hero-bg" />
      <view class="profile-row">
        <view class="avatar">{{ userStore.avatarText }}</view>
        <view class="grow profile-copy">
          <text class="profile-name">{{ displayName }}</text>
          <text class="profile-role">{{ roleText }}</text>
          <text class="profile-meta">{{ identityText }}</text>
        </view>
        <view class="edit-btn" @click="go('/pages/profile/index')">
          <uni-icons type="compose" size="15" color="#0f766e" />
          <text>档案</text>
        </view>
      </view>

      <view class="mine-stats">
        <view class="mine-stat">
          <text class="mine-stat-value">{{ profile ? '已建档' : '待完善' }}</text>
          <text class="mine-stat-label">健康档案</text>
        </view>
        <view class="mine-stat-divider" />
        <view class="mine-stat">
          <text class="mine-stat-value">{{ formatNumber(profile?.bmi, 1) }}</text>
          <text class="mine-stat-label">BMI</text>
        </view>
        <view class="mine-stat-divider" />
        <view class="mine-stat">
          <text class="mine-stat-value">{{ summary.checkinDays ?? 0 }}<text class="mine-stat-unit">天</text></text>
          <text class="mine-stat-label">本月打卡</text>
        </view>
      </view>
    </view>

    <view class="mine-body">
      <view class="card menu-card">
        <view
          v-for="(item, index) in menuItems"
          :key="item.key"
          class="menu-row"
          :class="{ 'menu-row-border': index !== menuItems.length - 1 }"
          @click="item.action"
        >
          <view class="menu-icon" :style="{ background: item.bg }">
            <uni-icons :type="item.icon" size="20" :color="item.color" />
          </view>
          <view class="grow">
            <text class="menu-title">{{ item.title }}</text>
            <text class="menu-desc">{{ item.desc }}</text>
          </view>
          <text v-if="item.key === 'messages' && messageStore.unread > 0" class="menu-badge">
            {{ messageStore.unread > 99 ? '99+' : messageStore.unread }}
          </text>
          <uni-icons type="arrowright" size="15" color="#c4d0cc" />
        </view>
      </view>

      <view class="card menu-card">
        <view class="menu-row" @click="showAbout">
          <view class="menu-icon" style="background: #eef2f7">
            <uni-icons type="info" size="20" color="#64748b" />
          </view>
          <view class="grow">
            <text class="menu-title">关于本应用</text>
            <text class="menu-desc">版本信息与使用说明</text>
          </view>
          <text class="menu-version">v{{ version }}</text>
          <uni-icons type="arrowright" size="15" color="#c4d0cc" />
        </view>
      </view>

      <button class="logout-btn" @click="handleLogout">
        <uni-icons type="undo" size="18" color="#dc2626" />
        <text>退出登录</text>
      </button>

      <view class="mine-footer">
        <text>大学生健康管理平台 · 学生端</text>
        <text>数据仅用于校园健康管理</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { getMyHealthProfile } from '@/api/health-profile'
import { getCheckinSummary } from '@/api/health-checkin'
import { getAccessToken } from '@/utils/auth'
import { currentMonthString } from '@/utils/checkin'
import { formatNumber } from '@/utils/format'
import { APP_VERSION } from '@/config'
import type { HealthProfile, CheckinSummary } from '@/types/health'

const userStore = useUserStore()
const messageStore = useMessageStore()

const profile = ref<HealthProfile | null>(null)
const summary = ref<CheckinSummary>({})
const version = APP_VERSION

const displayName = computed(
  () => profile.value?.realName || userStore.displayName,
)
const roleText = computed(() => userStore.roleNames.join(' / ') || '学生')
const identityText = computed(() => {
  const parts = [
    profile.value?.studentNo ? `学号 ${profile.value.studentNo}` : '',
    profile.value?.college || '',
    profile.value?.className || '',
  ].filter(Boolean)
  return parts.join(' · ') || '完善健康档案，展示更多信息'
})

const menuItems = computed(() => [
  {
    key: 'profile',
    title: '健康档案',
    desc: '身高体重、BMI、紧急联系人',
    icon: 'person',
    color: '#0f9d8a',
    bg: '#e6f7f3',
    action: () => go('/pages/profile/index'),
  },
  {
    key: 'physical',
    title: '体测成绩',
    desc: '历次成绩与指标趋势',
    icon: 'medal',
    color: '#3b82f6',
    bg: '#eaf2ff',
    action: () => go('/pages/physical/index'),
  },
  {
    key: 'report',
    title: '我的健康报告',
    desc: '月度数据总结与建议',
    icon: 'list',
    color: '#8b5cf6',
    bg: '#f2ecff',
    action: () => go('/pages/report/index'),
  },
  {
    key: 'messages',
    title: '消息中心',
    desc: '健康预警、校园公告与关怀',
    icon: 'mail-open',
    color: '#f59e0b',
    bg: '#fff6e6',
    action: () => go('/pages/messages/index'),
  },
  {
    key: 'password',
    title: '修改密码',
    desc: '定期更换密码，保护账号安全',
    icon: 'locked',
    color: '#64748b',
    bg: '#eef2f7',
    action: () => go('/pages/change-password/index'),
  },
])

function go(url: string): void {
  uni.navigateTo({ url })
}

async function loadData(): Promise<void> {
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
  messageStore.refresh()
  const results = await Promise.allSettled([
    getMyHealthProfile(),
    getCheckinSummary(currentMonthString()),
  ])
  if (results[0].status === 'fulfilled') profile.value = results[0].value ?? null
  if (results[1].status === 'fulfilled') summary.value = results[1].value ?? {}
}

function showAbout(): void {
  uni.showModal({
    title: '关于本应用',
    content: `大学生健康管理平台 · 学生端\n版本 v${version}\n\n用于健康档案、每日打卡、体测成绩、健康知识与 AI 健康助手。`,
    showCancel: false,
    confirmText: '知道了',
  })
}

function handleLogout(): void {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    confirmText: '退出',
    confirmColor: '#dc2626',
    success: async (res) => {
      if (!res.confirm) return
      await userStore.logout()
      messageStore.reset()
      uni.reLaunch({ url: '/pages/login/index' })
    },
  })
}

onShow(loadData)
</script>

<style scoped>
.mine-page {
  min-height: 100vh;
  background: #f4f7f6;
}

.mine-hero {
  position: relative;
  overflow: hidden;
  padding: 46rpx 32rpx 34rpx;
  background: linear-gradient(145deg, #12b39c 0%, #0f9d8a 45%, #0a6a61 100%);
  border-radius: 0 0 44rpx 44rpx;
}

.mine-hero-bg {
  position: absolute;
  width: 360rpx;
  height: 360rpx;
  border-radius: 50%;
  right: -140rpx;
  top: -170rpx;
  background: rgba(255, 255, 255, 0.08);
}

.profile-row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 22rpx;
}

.avatar {
  width: 116rpx;
  height: 116rpx;
  border-radius: 34rpx;
  background: rgba(255, 255, 255, 0.95);
  color: #0f766e;
  font-size: 46rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4rpx solid rgba(255, 255, 255, 0.42);
  box-shadow: 0 14rpx 30rpx rgba(4, 58, 52, 0.16);
}

.profile-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.profile-name {
  font-size: 38rpx;
  font-weight: 700;
  color: #ffffff;
}

.profile-role {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.72);
}

.profile-meta {
  margin-top: 8rpx;
  font-size: 21rpx;
  color: rgba(255, 255, 255, 0.52);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.edit-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  height: 58rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.9);
  color: #0f766e;
  font-size: 22rpx;
  font-weight: 600;
}

.mine-stats {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  margin-top: 34rpx;
  padding: 22rpx 12rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.12);
  border: 1rpx solid rgba(255, 255, 255, 0.14);
}

.mine-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.mine-stat-divider {
  width: 1rpx;
  height: 42rpx;
  background: rgba(255, 255, 255, 0.14);
}

.mine-stat-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #ffffff;
}

.mine-stat-unit {
  margin-left: 2rpx;
  font-size: 18rpx;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.6);
}

.mine-stat-label {
  margin-top: 4rpx;
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.56);
}

.mine-body {
  padding: 24rpx 24rpx calc(140rpx + env(safe-area-inset-bottom));
}

.menu-card {
  padding: 4rpx 24rpx;
}

.menu-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 0;
}

.menu-row-border {
  border-bottom: 1rpx solid #f0f5f3;
}

.menu-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.menu-title {
  display: block;
  font-size: 27rpx;
  font-weight: 600;
  color: #1e293b;
}

.menu-desc {
  display: block;
  margin-top: 4rpx;
  font-size: 21rpx;
  color: #94a3b8;
}

.menu-badge {
  min-width: 34rpx;
  height: 34rpx;
  padding: 0 9rpx;
  border-radius: 999rpx;
  background: #ef4444;
  color: #ffffff;
  font-size: 19rpx;
  line-height: 34rpx;
  text-align: center;
}

.menu-version {
  font-size: 22rpx;
  color: #a8b7b2;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  width: 100%;
  height: 92rpx;
  margin-top: 28rpx;
  border-radius: 24rpx;
  background: #ffffff;
  color: #dc2626;
  font-size: 27rpx;
  font-weight: 600;
  box-shadow: 0 8rpx 26rpx rgba(15, 118, 110, 0.05);
}

.logout-btn::after {
  border: none;
}

.mine-footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  padding: 44rpx 0 10rpx;
  font-size: 20rpx;
  color: #b1bfba;
}
</style>
