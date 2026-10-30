<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Check } from '@element-plus/icons-vue'
import { getMessageDetail, markMessageRead } from '@/api/message'
import { useMessageStore } from '@/stores/message'
import { messageTypeTagType } from '@/utils/message'
import { formatDateTime, nowLocalDateTimeString } from '@/utils/format'
import type { MessageVO } from '@/types/message'

const props = defineProps<{
  modelValue: boolean
  messageId?: number | null
  /** 已发送列表直接传入行数据，避免再次请求详情 */
  message?: MessageVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'read', message: MessageVO): void
}>()

const messageStore = useMessageStore()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const loading = ref(false)
const errorMessage = ref('')
const detail = ref<MessageVO | null>(null)
const preloaded = computed(() => Boolean(props.message))

async function markAsRead(message: MessageVO): Promise<void> {
  try {
    const result = await markMessageRead(message.id)
    messageStore.setUnreadCount(result.count)
    // message 是 detail.value 的响应式代理，保证抽屉内已读状态同步刷新
    message.readStatus = 1
    message.readTime = nowLocalDateTimeString()
    emit('read', message)
  } catch {
    // 已读失败不影响正文展示，错误由拦截器提示
  }
}

async function loadDetail(): Promise<void> {
  if (props.message) {
    detail.value = { ...props.message }
    errorMessage.value = ''
    loading.value = false
    return
  }
  if (!props.messageId) {
    detail.value = null
    return
  }
  loading.value = true
  errorMessage.value = ''
  detail.value = null
  try {
    const data = await getMessageDetail(props.messageId)
    detail.value = data
    if (data.readStatus === 0) {
      // 传入 ref 中的响应式对象，而不是接口返回的原始对象
      await markAsRead(detail.value)
    }
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '消息加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.messageId, props.message] as const,
  ([open]) => {
    if (open) loadDetail()
  },
)
</script>

<template>
  <el-drawer
    v-model="visible"
    title="消息详情"
    size="640px"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading" class="detail-body">
      <template v-if="detail">
        <div class="detail-tags">
          <el-tag
            :type="messageTypeTagType(detail.messageType)"
            effect="light"
          >
            {{ detail.messageTypeName }}
          </el-tag>
          <el-tag
            v-if="preloaded && detail.receiverName"
            type="info"
            effect="plain"
          >
            接收人：{{ detail.receiverName }}
          </el-tag>
          <el-tag
            v-else-if="!preloaded && detail.readStatus === 0"
            type="danger"
            effect="plain"
          >
            未读
          </el-tag>
          <el-tag v-else-if="!preloaded" type="info" effect="plain">
            已读
          </el-tag>
        </div>

        <h2>{{ detail.title }}</h2>

        <div class="detail-meta">
          <span>发送人：{{ detail.senderName || '系统' }}</span>
          <span>发送时间：{{ formatDateTime(detail.publishTime) }}</span>
          <span v-if="detail.readTime">
            已读时间：{{ formatDateTime(detail.readTime) }}
          </span>
        </div>

        <div class="message-content">{{ detail.content }}</div>

        <el-descriptions :column="2" border class="detail-info">
          <el-descriptions-item label="消息类型">
            {{ detail.messageTypeName }}
          </el-descriptions-item>
          <el-descriptions-item label="发送方式">
            {{ detail.sendTypeName }}
          </el-descriptions-item>
          <template v-if="preloaded">
            <el-descriptions-item label="接收人">
              {{ detail.receiverName || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ formatDateTime(detail.createTime) }}
            </el-descriptions-item>
          </template>
          <template v-else>
            <el-descriptions-item label="发送时间">
              {{ formatDateTime(detail.publishTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="已读状态">
              {{ detail.readStatus === 1 ? '已读' : '未读' }}
            </el-descriptions-item>
          </template>
        </el-descriptions>

        <div
          v-if="!preloaded && detail.readStatus === 1"
          class="read-hint"
        >
          <el-icon><Check /></el-icon>
          <span>已自动标记为已读</span>
        </div>
      </template>

      <el-empty
        v-else-if="!loading"
        :description="errorMessage || '未获取到消息内容'"
        :image-size="90"
      />
    </div>
  </el-drawer>
</template>

<style scoped>
.detail-body {
  min-height: 240px;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-body h2 {
  margin: 14px 0 10px;
  font-size: 24px;
  line-height: 1.45;
  color: var(--hp-text);
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  font-size: 12.5px;
  color: var(--hp-text-muted);
}

.message-content {
  padding: 22px 0;
  margin: 20px 0;
  font-size: 14.5px;
  line-height: 1.85;
  color: var(--hp-text);
  white-space: pre-wrap;
  word-break: break-word;
  border-top: 1px solid #edf3f0;
  border-bottom: 1px solid #edf3f0;
}

.detail-info :deep(.el-descriptions__label) {
  width: 92px;
  color: var(--hp-text-secondary);
}

.read-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding-top: 18px;
  font-size: 12px;
  color: var(--hp-text-muted);
}

.read-hint .el-icon {
  color: var(--hp-primary);
}
</style>
