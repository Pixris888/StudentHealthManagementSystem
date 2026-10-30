<template>
  <view class="page">
    <view class="message-summary">
      <view class="summary-left">
        <view class="summary-icon">
          <uni-icons type="mail-open" size="24" color="#ffffff" />
        </view>
        <view>
          <text class="summary-count">{{ messageStore.unread }}</text>
          <text class="summary-label">条未读消息</text>
        </view>
      </view>
      <view
        class="read-all-btn"
        :class="{ disabled: !messageStore.unread }"
        @click="handleMarkAllRead"
      >
        <uni-icons type="checkmarkempty" size="14" color="#ffffff" />
        <text>全部已读</text>
      </view>
    </view>

    <view class="tabs-wrap">
      <SegTabs v-model="activeTab" :options="tabOptions" @change="onTabChange" />
    </view>

    <view v-if="loading && !messages.length" class="card loading-card">
      <uni-load-more status="loading" color="#0f9d8a" />
    </view>

    <template v-else>
      <view
        v-for="message in messages"
        :key="message.id"
        class="card message-card"
        :class="{ unread: message.readStatus !== 1 }"
        @click="openDetail(message)"
      >
        <view class="message-head">
          <view class="row" style="gap: 10rpx">
            <view v-if="message.readStatus !== 1" class="unread-dot" />
            <text class="tag" :class="messageTagClass(message.messageType)">
              {{ message.messageTypeName || '消息' }}
            </text>
            <text class="message-send-type">{{ message.sendTypeName || '' }}</text>
          </view>
          <text class="message-time">{{ formatDateTimeShort(message.publishTime || message.createTime) }}</text>
        </view>

        <text class="message-title">{{ message.title }}</text>
        <text class="message-content text-clamp-2">{{ message.content }}</text>

        <view class="message-foot">
          <text class="message-sender">来自 {{ message.senderName || '系统' }}</text>
          <view class="row" style="gap: 4rpx">
            <text class="message-more">查看详情</text>
            <uni-icons type="arrowright" size="12" color="#0f9d8a" />
          </view>
        </view>
      </view>

      <EmptyState
        v-if="!messages.length"
        icon="mail-open"
        :title="emptyTitle"
        desc="校园公告、健康关怀和预警消息会出现在这里"
        action-text="刷新"
        @action="load(true)"
      />

      <view v-if="messages.length" class="load-more">
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
import SegTabs from '@/components/SegTabs.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getMessagePage } from '@/api/message'
import { useMessageStore } from '@/stores/message'
import { getAccessToken } from '@/utils/auth'
import { formatDateTimeShort } from '@/utils/format'
import type { MessageVO, ReadStatus } from '@/types/message'

type TabValue = 'all' | 'unread' | 'read'

const tabOptions = [
  { value: 'all', label: '全部' },
  { value: 'unread', label: '未读' },
  { value: 'read', label: '已读' },
]

const messageStore = useMessageStore()
const activeTab = ref<TabValue>('all')
const messages = ref<MessageVO[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

const hasMore = computed(() => messages.value.length < total.value)
const emptyTitle = computed(() => {
  if (activeTab.value === 'unread') return '没有未读消息'
  if (activeTab.value === 'read') return '没有已读消息'
  return '暂无消息'
})

function ensureLogin(): boolean {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

function readStatusParam(): ReadStatus | undefined {
  if (activeTab.value === 'unread') return 0
  if (activeTab.value === 'read') return 1
  return undefined
}

function messageTagClass(type: number): string {
  if (type === 1) return 'tag-danger'
  if (type === 3) return 'tag-primary'
  if (type === 4) return 'tag-warning'
  return 'tag-info'
}

async function load(reset = false): Promise<void> {
  if (loading.value) return
  if (!ensureLogin()) return
  if (reset) {
    page.value = 1
    total.value = 0
    messages.value = []
  }
  loading.value = true
  try {
    const result = await getMessagePage({
      page: page.value,
      size,
      readStatus: readStatusParam(),
    })
    const list = result.records ?? []
    messages.value = reset ? list : [...messages.value, ...list]
    total.value = result.total ?? messages.value.length
  } catch {
    // 请求层已提示
  } finally {
    loading.value = false
  }
}

function onTabChange(): void {
  load(true)
}

function openDetail(message: MessageVO): void {
  uni.navigateTo({ url: `/pages/messages/detail?id=${message.id}` })
}

function handleMarkAllRead(): void {
  if (!messageStore.unread) {
    uni.showToast({ title: '没有未读消息', icon: 'none' })
    return
  }
  uni.showModal({
    title: '全部已读',
    content: '确定将所有未读消息标记为已读吗？',
    confirmText: '全部已读',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await messageStore.markAllRead()
        uni.showToast({ title: '已全部标记为已读', icon: 'success' })
        await load(true)
      } catch {
        // 请求层已提示
      }
    },
  })
}

function loadMore(): void {
  if (loading.value || !hasMore.value) return
  page.value += 1
  load(false)
}

onShow(() => {
  messageStore.refresh()
  load(true)
})

onPullDownRefresh(async () => {
  messageStore.refresh()
  await load(true)
  uni.stopPullDownRefresh()
})

onReachBottom(loadMore)
</script>

<style scoped>
.message-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26rpx 28rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #12b39c 0%, #0b7a6b 100%);
  box-shadow: 0 14rpx 34rpx rgba(13, 148, 136, 0.18);
}

.summary-left {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.summary-icon {
  width: 78rpx;
  height: 78rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-count {
  font-size: 46rpx;
  font-weight: 700;
  color: #ffffff;
  line-height: 1;
}

.summary-label {
  margin-left: 6rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.72);
}

.read-all-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  height: 60rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
  font-size: 22rpx;
}

.read-all-btn.disabled {
  opacity: 0.5;
}

.tabs-wrap {
  margin: 24rpx 0 22rpx;
}

.loading-card {
  min-height: 280rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-card {
  margin-bottom: 18rpx;
}

.message-card.unread {
  border-left: 6rpx solid #0f9d8a;
}

.message-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.unread-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #ef4444;
}

.message-send-type {
  font-size: 19rpx;
  color: #a8b7b2;
}

.message-time {
  font-size: 20rpx;
  color: #a8b7b2;
  white-space: nowrap;
}

.message-title {
  display: block;
  margin-top: 16rpx;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 1.45;
  color: #0f172a;
}

.message-content {
  display: block;
  margin-top: 10rpx;
  font-size: 23rpx;
  line-height: 1.65;
  color: #7b8f89;
}

.message-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f5f3;
}

.message-sender {
  font-size: 20rpx;
  color: #a8b7b2;
}

.message-more {
  font-size: 20rpx;
  color: #0f9d8a;
}

.load-more {
  padding: 10rpx 0 20rpx;
}
</style>
