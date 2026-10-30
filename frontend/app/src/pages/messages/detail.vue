<template>
  <view class="page">
    <view v-if="loading" class="card loading-card">
      <uni-load-more status="loading" color="#0f9d8a" />
    </view>

    <EmptyState
      v-else-if="error"
      icon="info"
      title="消息加载失败"
      :desc="error"
      action-text="返回"
      @action="goBack"
    />

    <template v-else-if="message">
      <view v-if="message.messageType === 1" class="warning-banner">
        <uni-icons type="info-filled" size="18" color="#dc2626" />
        <text>这是一条健康预警消息，请认真查看并关注自身健康情况。</text>
      </view>

      <view class="card message-head">
        <view class="row" style="gap: 10rpx">
          <text class="tag" :class="messageTagClass(message.messageType)">
            {{ message.messageTypeName || '消息' }}
          </text>
          <text class="tag tag-info">{{ message.sendTypeName || '系统消息' }}</text>
        </view>
        <text class="message-title">{{ message.title }}</text>
        <view class="message-meta">
          <text>来自 {{ message.senderName || '系统' }}</text>
          <text>·</text>
          <text>{{ formatDateTime(message.publishTime || message.createTime) }}</text>
        </view>
      </view>

      <view class="card message-content-card">
        <text class="message-content" selectable>{{ message.content }}</text>
      </view>

      <view v-if="message.readTime" class="read-time">
        <uni-icons type="checkmarkempty" size="13" color="#0f9d8a" />
        <text>已于 {{ formatDateTime(message.readTime) }} 阅读</text>
      </view>

      <button class="btn btn-ghost btn-block back-btn" @click="goBack">返回消息列表</button>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '@/components/EmptyState.vue'
import { getMessageDetail } from '@/api/message'
import { useMessageStore } from '@/stores/message'
import { getAccessToken } from '@/utils/auth'
import { formatDateTime } from '@/utils/format'
import type { MessageVO } from '@/types/message'

const messageStore = useMessageStore()
const message = ref<MessageVO | null>(null)
const loading = ref(true)
const error = ref('')

function messageTagClass(type: number): string {
  if (type === 1) return 'tag-danger'
  if (type === 3) return 'tag-primary'
  if (type === 4) return 'tag-warning'
  return 'tag-info'
}

function goBack(): void {
  if (getCurrentPages().length > 1) {
    uni.navigateBack({ delta: 1 })
  } else {
    uni.navigateTo({ url: '/pages/messages/index' })
  }
}

async function load(id: number): Promise<void> {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await getMessageDetail(id)
    message.value = data
    if (data && data.readStatus !== 1) {
      await messageStore.markRead(id)
      data.readStatus = 1
      data.readTime = data.readTime || new Date().toISOString()
    }
  } catch (err) {
    error.value = (err as Error | null)?.message || '消息不存在或无权查看'
  } finally {
    loading.value = false
  }
}

onLoad((options) => {
  const id = Number(options?.id)
  if (!id) {
    loading.value = false
    error.value = '缺少消息编号'
    return
  }
  load(id)
})
</script>

<style scoped>
.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.warning-banner {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  margin-bottom: 20rpx;
  padding: 20rpx 22rpx;
  border-radius: 18rpx;
  background: #fef2f2;
  font-size: 23rpx;
  line-height: 1.6;
  color: #b91c1c;
}

.message-title {
  display: block;
  margin-top: 20rpx;
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1.45;
  color: #0f172a;
}

.message-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6rpx;
  margin-top: 16rpx;
  font-size: 21rpx;
  color: #94a3b8;
}

.message-content-card {
  padding: 30rpx 28rpx;
}

.message-content {
  display: block;
  font-size: 28rpx;
  line-height: 1.85;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
}

.read-time {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  padding: 26rpx 0 4rpx;
  font-size: 20rpx;
  color: #a8b7b2;
}

.back-btn {
  margin-top: 28rpx;
  height: 84rpx;
  font-size: 26rpx;
}
</style>
