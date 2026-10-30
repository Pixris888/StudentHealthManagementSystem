<template>
  <view class="home-page">
    <view class="home-hero">
      <view class="hero-top">
        <view class="grow">
          <text class="hero-greeting">{{ greetingText }}</text>
          <text class="hero-date">{{ dateText }} · 今天也要照顾好自己</text>
        </view>
        <view class="hero-actions">
          <view class="hero-icon-btn" @click="openMessages">
            <uni-icons type="notification" size="21" color="#ffffff" />
            <view v-if="messageStore.unread > 0" class="hero-badge">
              {{ messageStore.unread > 99 ? '99+' : messageStore.unread }}
            </view>
          </view>
          <view class="hero-avatar" @click="goMine">
            <text>{{ userStore.avatarText }}</text>
          </view>
        </view>
      </view>
      <view class="hero-slogan">
        <text class="hero-slogan-title">健康不是一次冲刺</text>
        <text class="hero-slogan-sub">而是每天多一点点坚持</text>
      </view>
    </view>

    <view class="home-body">
      <view v-if="loading && !today" class="card loading-card">
        <uni-load-more status="loading" color="#0f9d8a" />
      </view>

      <template v-else>
        <view class="card today-card">
          <view class="between">
            <view>
              <text class="card-title">今日打卡</text>
              <text class="today-hint">完成 {{ completionCount }} / 4 个板块</text>
            </view>
            <view class="today-percent">
              <text class="today-percent-value">{{ completionCount * 25 }}</text>
              <text class="today-percent-unit">%</text>
            </view>
          </view>

          <view class="progress-track">
            <view class="progress-fill" :style="{ width: `${completionCount * 25}%` }" />
          </view>

          <view class="module-row">
            <view
              v-for="item in todayModules"
              :key="item.key"
              class="module-item"
              @click="goCheckin"
            >
              <view class="module-icon" :class="{ done: item.done }">
                <text class="module-emoji">{{ item.emoji }}</text>
                <view v-if="item.done" class="module-check">
                  <uni-icons type="checkmarkempty" size="10" color="#ffffff" />
                </view>
              </view>
              <text class="module-title">{{ item.title }}</text>
              <text class="module-detail" :class="{ done: item.done }">
                {{ item.detail }}
              </text>
            </view>
          </view>

          <button class="btn btn-primary btn-block checkin-btn" @click="goCheckin">
            <text>{{ completionCount === 4 ? '查看今日记录' : '去完成今日打卡' }}</text>
            <uni-icons type="arrowright" size="17" color="#ffffff" />
          </button>
        </view>

        <view class="card summary-card">
          <view class="between">
            <text class="card-title">本月概览</text>
            <text class="section-link" @click="goCheckin">打卡详情</text>
          </view>
          <view class="summary-grid">
            <view class="summary-item">
              <text class="summary-value">{{ formatPercent(summary.checkinRate) }}</text>
              <text class="summary-label">打卡率</text>
            </view>
            <view class="summary-item">
              <text class="summary-value">{{ summary.consecutiveDays ?? 0 }}<text class="summary-unit">天</text></text>
              <text class="summary-label">连续打卡</text>
            </view>
            <view class="summary-item">
              <text class="summary-value">{{ formatNumber(summary.avgSleepDuration, 1) }}<text class="summary-unit">h</text></text>
              <text class="summary-label">平均睡眠</text>
            </view>
            <view class="summary-item">
              <text class="summary-value">{{ summary.exerciseDays ?? 0 }}<text class="summary-unit">天</text></text>
              <text class="summary-label">运动天数</text>
            </view>
          </view>
        </view>

        <view class="ai-banner" @click="goAi">
          <view class="ai-banner-icon">
            <uni-icons type="chatbubble-filled" size="24" color="#ffffff" />
          </view>
          <view class="grow">
            <text class="ai-banner-title">有健康疑问？问问 AI 助手</text>
            <text class="ai-banner-desc">作息、运动、饮食，随时给你参考建议</text>
          </view>
          <uni-icons type="arrowright" size="18" color="rgba(255,255,255,0.82)" />
        </view>

        <view class="card snapshot-card">
          <view class="between">
            <text class="card-title">健康快照</text>
            <text class="section-link" @click="goProfile">
              {{ profile ? '完善档案' : '立即建档' }}
            </text>
          </view>

          <view v-if="profile" class="snapshot-grid">
            <view class="snapshot-item">
              <text class="snapshot-label">BMI</text>
              <text class="snapshot-value">{{ formatNumber(profile.bmi, 1) }}</text>
              <text v-if="profile.bmiLevel" class="tag" :class="bmiTagClass">{{ profile.bmiLevel }}</text>
              <text v-else class="snapshot-hint">完善身高体重后计算</text>
            </view>
            <view class="snapshot-divider" />
            <view class="snapshot-item">
              <text class="snapshot-label">最新体测总分</text>
              <text class="snapshot-value">{{ latestPhysical?.totalScore ?? '—' }}</text>
              <text
                v-if="latestPhysical?.gradeLevel"
                class="tag"
                :class="gradeTagClass(latestPhysical.gradeLevel)"
              >
                {{ latestPhysical.gradeLevel }}
              </text>
              <text v-else class="snapshot-hint">暂无体测记录</text>
            </view>
          </view>
          <view v-else class="snapshot-empty" @click="goProfile">
            <view class="snapshot-empty-icon">
              <uni-icons type="compose" size="22" color="#0f9d8a" />
            </view>
            <view class="grow">
              <text class="snapshot-empty-title">建立你的健康档案</text>
              <text class="snapshot-empty-desc">记录身高、体重、血型与紧急联系人</text>
            </view>
            <uni-icons type="arrowright" size="16" color="#b1bfba" />
          </view>
        </view>

        <view class="section-head">
          <view>
            <text class="section-title">快捷入口</text>
            <text class="section-desc">常用健康功能，一步直达</text>
          </view>
        </view>

        <view class="quick-grid">
          <view
            v-for="item in quickActions"
            :key="item.key"
            class="quick-item"
            @click="item.action"
          >
            <view class="quick-icon" :style="{ background: item.bg }">
              <uni-icons :type="item.icon" size="22" :color="item.color" />
            </view>
            <text class="quick-title">{{ item.title }}</text>
            <text class="quick-desc">{{ item.desc }}</text>
          </view>
        </view>

        <view class="section-head">
          <view>
            <text class="section-title">健康知识</text>
            <text class="section-desc">来自校园健康知识库</text>
          </view>
          <text class="section-link" @click="goKnowledge">查看全部</text>
        </view>

        <view v-if="hotArticles.length" class="card article-card">
          <view
            v-for="(article, index) in hotArticles"
            :key="article.id"
            class="article-row"
            :class="{ 'article-row-border': index !== hotArticles.length - 1 }"
            @click="openArticle(article.id)"
          >
            <view class="article-index">{{ String(index + 1).padStart(2, '0') }}</view>
            <view class="grow">
              <text class="article-title text-clamp-2">{{ article.title }}</text>
              <view class="article-meta">
                <text>{{ article.categoryName || '健康科普' }}</text>
                <text>·</text>
                <text>{{ article.viewCount }} 次阅读</text>
              </view>
            </view>
            <uni-icons type="arrowright" size="15" color="#c4d0cc" />
          </view>
        </view>

        <view v-else class="card">
          <EmptyState
            icon="list"
            title="暂无健康文章"
            desc="知识库正在准备内容，稍后再来看看吧"
          />
        </view>

        <view class="home-footer">
          <text>数据仅用于校园健康管理</text>
          <text>大学生健康管理平台 · v{{ version }}</text>
        </view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import EmptyState from '@/components/EmptyState.vue'
import { getTodayCheckin, getCheckinSummary } from '@/api/health-checkin'
import { getMyHealthProfile } from '@/api/health-profile'
import { pageMyPhysicalTests } from '@/api/health-physical'
import { getHotArticles } from '@/api/knowledge'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { getAccessToken } from '@/utils/auth'
import {
  checkinCompletionCount,
  currentMonthString,
  emotionEmoji,
  formatClockTime,
} from '@/utils/checkin'
import {
  formatNumber,
  formatPercent,
  greeting,
  todayLabel,
} from '@/utils/format'
import { bmiLevelTone } from '@/utils/dict'
import type { HealthCheckin, CheckinSummary, HealthProfile, PhysicalTest } from '@/types/health'
import type { ArticleVO } from '@/types/knowledge'
import { APP_VERSION } from '@/config'

const userStore = useUserStore()
const messageStore = useMessageStore()

const loading = ref(true)
const today = ref<HealthCheckin | null>(null)
const summary = ref<CheckinSummary>({})
const profile = ref<HealthProfile | null>(null)
const latestPhysical = ref<PhysicalTest | null>(null)
const hotArticles = ref<ArticleVO[]>([])
const version = APP_VERSION

const greetingText = computed(() => `${greeting()}，${userStore.displayName}`)
const dateText = computed(() => todayLabel())
const completionCount = computed(() => checkinCompletionCount(today.value))
const bmiTagClass = computed(() => `tag-${bmiLevelTone(profile.value?.bmiLevel)}`)

function gradeTagClass(level?: string | null): string {
  if (!level) return 'tag-info'
  if (level === '优秀' || level === '良好') return 'tag-success'
  if (level === '及格') return 'tag-warning'
  if (level === '不及格') return 'tag-danger'
  return 'tag-info'
}

const todayModules = computed(() => {
  const record = today.value
  return [
    {
      key: 'sleep',
      title: '睡眠',
      emoji: '🌙',
      done: Boolean(record?.sleepCompleted),
      detail: record?.sleepDuration
        ? `${formatNumber(record.sleepDuration, 1)} 小时`
        : record?.sleepTime
          ? `${formatClockTime(record.sleepTime)} 入睡`
          : '记录睡眠',
    },
    {
      key: 'exercise',
      title: '运动',
      emoji: '🏃',
      done: Boolean(record?.exerciseCompleted),
      detail: record?.exerciseDuration
        ? `${record.exerciseDuration} 分钟`
        : '记录运动',
    },
    {
      key: 'diet',
      title: '饮食',
      emoji: '🥗',
      done: Boolean(record?.dietCompleted),
      detail: record?.dietBreakfast === 1 ? '已吃早餐' : '记录饮食',
    },
    {
      key: 'emotion',
      title: '情绪',
      emoji: record?.emotionType ? emotionEmoji(record.emotionType) : '😊',
      done: Boolean(record?.emotionCompleted),
      detail: record?.emotionTypeName || '记录心情',
    },
  ]
})

const quickActions = [
  {
    key: 'profile',
    title: '健康档案',
    desc: 'BMI 与基础信息',
    icon: 'person',
    color: '#0f9d8a',
    bg: '#e6f7f3',
    action: () => uni.navigateTo({ url: '/pages/profile/index' }),
  },
  {
    key: 'physical',
    title: '体测成绩',
    desc: '成绩与指标趋势',
    icon: 'medal',
    color: '#3b82f6',
    bg: '#eaf2ff',
    action: () => uni.navigateTo({ url: '/pages/physical/index' }),
  },
  {
    key: 'report',
    title: '我的月报',
    desc: '月度健康总结',
    icon: 'list',
    color: '#8b5cf6',
    bg: '#f2ecff',
    action: () => uni.navigateTo({ url: '/pages/report/index' }),
  },
  {
    key: 'messages',
    title: '消息中心',
    desc: '预警与校园公告',
    icon: 'mail-open',
    color: '#f59e0b',
    bg: '#fff6e6',
    action: () => uni.navigateTo({ url: '/pages/messages/index' }),
  },
]

function goCheckin(): void {
  uni.switchTab({ url: '/pages/checkin/index' })
}

function goAi(): void {
  uni.switchTab({ url: '/pages/ai/index' })
}

function goKnowledge(): void {
  uni.switchTab({ url: '/pages/knowledge/index' })
}

function goMine(): void {
  uni.switchTab({ url: '/pages/mine/index' })
}

function goProfile(): void {
  uni.navigateTo({ url: '/pages/profile/index' })
}

function openMessages(): void {
  uni.navigateTo({ url: '/pages/messages/index' })
}

function openArticle(id: number): void {
  uni.navigateTo({ url: `/pages/knowledge/detail?id=${id}` })
}

function redirectToLogin(): void {
  uni.reLaunch({ url: '/pages/login/index' })
}

async function ensureUser(): Promise<boolean> {
  if (!getAccessToken()) {
    redirectToLogin()
    return false
  }
  if (!userStore.userInfo) {
    try {
      await userStore.bootstrap()
    } catch {
      redirectToLogin()
      return false
    }
  }
  return true
}

async function loadData(withLoading = true): Promise<void> {
  if (withLoading) loading.value = true
  const results = await Promise.allSettled([
    getTodayCheckin(),
    getCheckinSummary(currentMonthString()),
    getMyHealthProfile(),
    pageMyPhysicalTests({ page: 1, size: 1 }),
    getHotArticles(4),
    messageStore.refresh(),
  ])

  if (results[0].status === 'fulfilled') today.value = results[0].value
  if (results[1].status === 'fulfilled') summary.value = results[1].value ?? {}
  if (results[2].status === 'fulfilled') profile.value = results[2].value ?? null
  if (results[3].status === 'fulfilled') {
    latestPhysical.value = results[3].value?.records?.[0] ?? null
  }
  if (results[4].status === 'fulfilled') hotArticles.value = results[4].value ?? []

  loading.value = false
}

onShow(async () => {
  const ready = await ensureUser()
  if (!ready) return
  messageStore.startPolling()
  await loadData()
})

onPullDownRefresh(async () => {
  await loadData(false)
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f4f7f6;
}

.home-hero {
  position: relative;
  overflow: hidden;
  padding: 34rpx 32rpx 120rpx;
  background: linear-gradient(145deg, #12b39c 0%, #0f9d8a 45%, #0a6a61 100%);
  border-radius: 0 0 44rpx 44rpx;
}

.home-hero::after {
  content: '';
  position: absolute;
  width: 320rpx;
  height: 320rpx;
  border-radius: 50%;
  right: -120rpx;
  top: -110rpx;
  background: rgba(255, 255, 255, 0.08);
}

.hero-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.hero-greeting {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #ffffff;
}

.hero-date {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: rgba(255, 255, 255, 0.68);
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.hero-icon-btn {
  position: relative;
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-badge {
  position: absolute;
  top: 4rpx;
  right: 0;
  min-width: 28rpx;
  height: 28rpx;
  padding: 0 7rpx;
  border-radius: 999rpx;
  background: #ef4444;
  color: #ffffff;
  font-size: 18rpx;
  line-height: 28rpx;
  text-align: center;
}

.hero-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #ffffff;
  color: #0f766e;
  font-size: 30rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3rpx solid rgba(255, 255, 255, 0.5);
}

.hero-slogan {
  position: relative;
  z-index: 1;
  margin-top: 44rpx;
  display: flex;
  flex-direction: column;
}

.hero-slogan-title {
  font-size: 25rpx;
  color: rgba(255, 255, 255, 0.9);
  letter-spacing: 1rpx;
}

.hero-slogan-sub {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.58);
}

.home-body {
  position: relative;
  z-index: 2;
  margin-top: -84rpx;
  padding: 0 24rpx calc(140rpx + env(safe-area-inset-bottom));
}

.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.today-hint {
  display: block;
  margin-top: 6rpx;
  font-size: 23rpx;
  color: #94a3b8;
}

.today-percent {
  display: flex;
  align-items: baseline;
  color: #0f9d8a;
}

.today-percent-value {
  font-size: 50rpx;
  font-weight: 700;
  line-height: 1;
}

.today-percent-unit {
  margin-left: 2rpx;
  font-size: 22rpx;
}

.progress-track {
  width: 100%;
  height: 12rpx;
  margin-top: 24rpx;
  border-radius: 999rpx;
  background: #edf5f2;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #12b39c, #0b7f70);
  transition: width 0.4s ease;
}

.module-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-top: 30rpx;
}

.module-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
}

.module-icon {
  position: relative;
  width: 82rpx;
  height: 82rpx;
  border-radius: 26rpx;
  background: #f2f7f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.module-icon.done {
  background: #e4f7f1;
}

.module-emoji {
  font-size: 36rpx;
  line-height: 1;
}

.module-check {
  position: absolute;
  right: -4rpx;
  bottom: -4rpx;
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  background: #0f9d8a;
  border: 3rpx solid #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.module-title {
  margin-top: 12rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #334155;
}

.module-detail {
  margin-top: 4rpx;
  max-width: 140rpx;
  font-size: 20rpx;
  color: #a8b7b2;
  text-align: center;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.module-detail.done {
  color: #0f9d8a;
}

.checkin-btn {
  margin-top: 30rpx;
  gap: 10rpx;
  height: 84rpx;
  font-size: 27rpx;
}

.summary-card {
  margin-top: 24rpx;
}

.summary-grid {
  display: flex;
  align-items: stretch;
  margin-top: 26rpx;
}

.summary-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.summary-item + .summary-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10rpx;
  bottom: 10rpx;
  width: 1rpx;
  background: #edf3f1;
}

.summary-value {
  font-size: 38rpx;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
}

.summary-unit {
  margin-left: 3rpx;
  font-size: 20rpx;
  font-weight: 400;
  color: #94a3b8;
}

.summary-label {
  margin-top: 8rpx;
  font-size: 21rpx;
  color: #94a3b8;
}

.ai-banner {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 24rpx;
  padding: 26rpx 26rpx;
  border-radius: 28rpx;
  background: linear-gradient(120deg, #243b53 0%, #1e4d4b 55%, #0f766e 100%);
  box-shadow: 0 14rpx 34rpx rgba(15, 118, 110, 0.2);
}

.ai-banner-icon {
  width: 76rpx;
  height: 76rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-banner-title {
  display: block;
  font-size: 27rpx;
  font-weight: 600;
  color: #ffffff;
}

.ai-banner-desc {
  display: block;
  margin-top: 4rpx;
  font-size: 21rpx;
  color: rgba(255, 255, 255, 0.6);
}

.snapshot-grid {
  display: flex;
  align-items: stretch;
  margin-top: 24rpx;
}

.snapshot-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.snapshot-divider {
  width: 1rpx;
  background: #edf3f1;
  margin: 4rpx 28rpx;
}

.snapshot-label {
  font-size: 22rpx;
  color: #94a3b8;
}

.snapshot-value {
  margin-top: 8rpx;
  font-size: 44rpx;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
}

.snapshot-hint {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #b1bfba;
}

.snapshot-item .tag {
  margin-top: 10rpx;
}

.snapshot-empty {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 22rpx;
  padding: 22rpx;
  border-radius: 20rpx;
  background: #f4faf8;
}

.snapshot-empty-icon {
  width: 68rpx;
  height: 68rpx;
  border-radius: 20rpx;
  background: #e0f5ef;
  display: flex;
  align-items: center;
  justify-content: center;
}

.snapshot-empty-title {
  display: block;
  font-size: 25rpx;
  font-weight: 600;
  color: #0f766e;
}

.snapshot-empty-desc {
  display: block;
  margin-top: 4rpx;
  font-size: 21rpx;
  color: #94a3b8;
}

.quick-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
}

.quick-item {
  width: calc(50% - 9rpx);
  padding: 24rpx;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 26rpx rgba(15, 118, 110, 0.05);
}

.quick-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-title {
  display: block;
  margin-top: 16rpx;
  font-size: 27rpx;
  font-weight: 600;
  color: #0f172a;
}

.quick-desc {
  display: block;
  margin-top: 4rpx;
  font-size: 21rpx;
  color: #94a3b8;
}

.article-card {
  padding: 8rpx 24rpx;
}

.article-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 24rpx 0;
}

.article-row-border {
  border-bottom: 1rpx solid #f0f5f3;
}

.article-index {
  width: 54rpx;
  height: 54rpx;
  border-radius: 16rpx;
  background: #f2f7f5;
  color: #0f9d8a;
  font-size: 22rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.article-title {
  font-size: 26rpx;
  font-weight: 600;
  line-height: 1.5;
  color: #1e293b;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #a8b7b2;
}

.home-footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  padding: 44rpx 0 10rpx;
  font-size: 20rpx;
  color: #b1bfba;
}
</style>
