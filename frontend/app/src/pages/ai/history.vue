<template>
  <view class="page">
    <view class="history-head">
      <view>
        <text class="history-title">我的问答历史</text>
        <text class="history-sub">按会话分组，点击可继续查看完整对话</text>
      </view>
      <view class="history-count">{{ total }} 条</view>
    </view>

    <view v-if="loading && !records.length" class="card loading-card">
      <uni-load-more status="loading" color="#0f9d8a" />
    </view>

    <template v-else>
      <view
        v-for="session in sessions"
        :key="session.key"
        class="card session-card"
        @click="openSession(session)"
      >
        <view class="session-head">
          <view class="session-icon">
            <uni-icons type="chatbubble" size="20" color="#0f9d8a" />
          </view>
          <view class="grow">
            <text class="session-title text-clamp-2">{{ session.title }}</text>
            <view class="session-meta">
              <text>{{ session.lastTime ? formatDateTimeShort(session.lastTime) : '—' }}</text>
              <text>·</text>
              <text>{{ session.records.length }} 轮对话</text>
              <text v-if="session.feedback === 2" class="session-feedback">· 有待改进反馈</text>
            </view>
          </view>
          <view class="session-arrow">
            <uni-load-more
              v-if="openingKey === session.key"
              status="loading"
              color="#0f9d8a"
              style="width: 60rpx"
            />
            <uni-icons v-else type="arrowright" size="15" color="#c4d0cc" />
          </view>
        </view>
      </view>

      <EmptyState
        v-if="!sessions.length"
        icon="chatbubble"
        title="还没有问答记录"
        desc="去 AI 健康助手问一个健康问题吧"
        action-text="去提问"
        @action="goAsk"
      />

      <view v-if="sessions.length" class="load-more">
        <uni-load-more
          :status="loading ? 'loading' : hasMore ? 'more' : 'noMore'"
          color="#94a3b8"
        />
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import EmptyState from '@/components/EmptyState.vue'
import {
  getSessionMessages,
  pageMyConsultations,
} from '@/api/ai'
import { useAiStore } from '@/stores/ai'
import { getAccessToken } from '@/utils/auth'
import { formatDateTimeShort } from '@/utils/format'
import type { AiConsultationVO } from '@/types/ai'

interface SessionGroup {
  key: string
  sessionId: string
  title: string
  lastTime?: string
  feedback: number
  records: AiConsultationVO[]
}

const aiStore = useAiStore()
const records = ref<AiConsultationVO[]>([])
const total = ref(0)
const page = ref(1)
const size = 20
const loading = ref(false)
const openingKey = ref('')

const hasMore = computed(() => records.value.length < total.value)

const sessions = computed<SessionGroup[]>(() => {
  const map = new Map<string, AiConsultationVO[]>()
  for (const item of records.value) {
    const key = item.sessionId || `single-${item.id}`
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(item)
  }
  return Array.from(map.entries())
    .map(([key, list]) => {
      const sorted = [...list].sort((a, b) =>
        (b.consultTime || '').localeCompare(a.consultTime || ''),
      )
      const latest = sorted[0]
      return {
        key,
        sessionId: latest?.sessionId || '',
        title: latest?.question || '未命名会话',
        lastTime: latest?.consultTime || latest?.createTime,
        feedback: latest?.feedback ?? 0,
        records: sorted,
      }
    })
    .sort((a, b) => (b.lastTime || '').localeCompare(a.lastTime || ''))
})

function ensureLogin(): boolean {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

async function load(reset = false): Promise<void> {
  if (loading.value) return
  if (!ensureLogin()) return
  if (reset) {
    page.value = 1
    total.value = 0
    records.value = []
  }
  loading.value = true
  try {
    const result = await pageMyConsultations({ page: page.value, size })
    const list = result.records ?? []
    records.value = reset ? list : [...records.value, ...list]
    total.value = result.total ?? records.value.length
  } catch {
    // 请求层已提示
  } finally {
    loading.value = false
  }
}

function backToAi(): void {
  const pages = getCurrentPages()
  const previous = pages[pages.length - 2]?.route ?? ''
  if (previous.includes('pages/ai/index')) {
    uni.navigateBack({ delta: 1 })
    return
  }
  uni.switchTab({ url: '/pages/ai/index' })
}

async function openSession(session: SessionGroup): Promise<void> {
  if (openingKey.value) return
  openingKey.value = session.key
  try {
    let list = session.records
    if (session.sessionId) {
      list = await getSessionMessages(session.sessionId)
    }
    aiStore.setPending(session.sessionId, list)
    backToAi()
  } catch {
    // 请求层已提示
  } finally {
    openingKey.value = ''
  }
}

function goAsk(): void {
  uni.switchTab({ url: '/pages/ai/index' })
}

function loadMore(): void {
  if (loading.value || !hasMore.value) return
  page.value += 1
  load(false)
}

onShow(() => {
  load(true)
})

onPullDownRefresh(async () => {
  await load(true)
  uni.stopPullDownRefresh()
})

onReachBottom(loadMore)
</script>

<style scoped>
.history-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 10rpx 6rpx 24rpx;
}

.history-title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #0f172a;
}

.history-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.history-count {
  height: 52rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #e6f7f3;
  color: #0f766e;
  font-size: 22rpx;
  line-height: 52rpx;
}

.loading-card {
  min-height: 260rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.session-card {
  margin-bottom: 18rpx;
}

.session-head {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.session-icon {
  width: 76rpx;
  height: 76rpx;
  border-radius: 24rpx;
  background: #e6f7f3;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.session-title {
  font-size: 27rpx;
  font-weight: 600;
  line-height: 1.5;
  color: #1e293b;
}

.session-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6rpx;
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #a8b7b2;
}

.session-feedback {
  color: #d97706;
}

.session-arrow {
  width: 60rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.load-more {
  padding: 10rpx 0 20rpx;
}
</style>
