<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
  Check,
  EditPen,
  Refresh,
} from '@element-plus/icons-vue'
import {
  getMessagePage,
  markAllMessagesRead,
} from '@/api/message'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import {
  MESSAGE_TYPE_OPTIONS,
  messageTypeTagType,
} from '@/utils/message'
import { formatDateTime } from '@/utils/format'
import type {
  MessageType,
  MessageVO,
  ReadStatus,
} from '@/types/message'
import MessageNav from './components/MessageNav.vue'
import MessageDetailDrawer from './components/MessageDetailDrawer.vue'
import MessageSendDialog from './components/MessageSendDialog.vue'

type ReadFilter = 'all' | 'unread' | 'read'

const userStore = useUserStore()
const messageStore = useMessageStore()

const hasSend = computed(
  () =>
    userStore.hasPermission('message:send') &&
    userStore.roles.some((role) =>
      ['DOCTOR_ADMIN', 'SUPER_ADMIN'].includes(role),
    ),
)

const records = ref<MessageVO[]>([])
const total = ref(0)
const pages = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })
const readFilter = ref<ReadFilter>('all')
const filters = reactive<{ messageType?: MessageType }>({})

const readStatus = computed<ReadStatus | undefined>(() => {
  if (readFilter.value === 'unread') return 0
  if (readFilter.value === 'read') return 1
  return undefined
})

async function loadMessages(): Promise<void> {
  loading.value = true
  try {
    const page = await getMessagePage({
      page: query.page,
      size: query.size,
      readStatus: readStatus.value,
      messageType: filters.messageType,
    })
    records.value = page.records
    total.value = page.total
    pages.value = page.pages
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

function scrollToTop(): void {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function setReadFilter(filter: ReadFilter): void {
  if (readFilter.value === filter) return
  readFilter.value = filter
  query.page = 1
  loadMessages()
}

function handleSearch(): void {
  query.page = 1
  loadMessages()
}

function resetFilters(): void {
  readFilter.value = 'all'
  filters.messageType = undefined
  query.page = 1
  query.size = 10
  loadMessages()
}

function handlePageChange(page: number): void {
  query.page = page
  loadMessages()
  scrollToTop()
}

function handleSizeChange(size: number): void {
  query.size = size
  query.page = 1
  loadMessages()
}

/* ---------------- 详情 ---------------- */
const detailVisible = ref(false)
const detailId = ref<number | null>(null)

function openMessage(row: MessageVO): void {
  detailId.value = row.id
  detailVisible.value = true
}

function handleMessageRead(message: MessageVO): void {
  const row = records.value.find((item) => item.id === message.id)
  if (row) {
    row.readStatus = 1
    row.readTime = message.readTime
  }
  if (readFilter.value === 'unread') {
    loadMessages()
  }
}

/* ---------------- 全部已读 ---------------- */
const markingAll = ref(false)

async function markAllRead(): Promise<void> {
  if (messageStore.unreadCount === 0) return
  try {
    await ElMessageBox.confirm(
      `确定将全部 ${messageStore.unreadCount} 条未读消息标记为已读吗？`,
      '全部已读',
      {
        type: 'warning',
        confirmButtonText: '全部标记已读',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  markingAll.value = true
  try {
    const result = await markAllMessagesRead()
    messageStore.setUnreadCount(result.count)
    ElMessage.success('全部消息已标记为已读')
    query.page = 1
    loadMessages()
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    markingAll.value = false
  }
}

/* ---------------- 发送 ---------------- */
const sendVisible = ref(false)

function handleSent(): void {
  void messageStore.refreshUnread()
  query.page = 1
  loadMessages()
}

onMounted(() => {
  void messageStore.refreshUnread()
  loadMessages()
})
</script>

<template>
  <div class="message-page">
    <div class="page-heading">
      <div>
        <span class="message-eyebrow">MESSAGE CENTER</span>
        <h2>消息中心</h2>
        <p>查看健康预警、校园公告与健康关怀留言。</p>
      </div>
      <div class="heading-actions">
        <el-button
          :icon="Check"
          :disabled="messageStore.unreadCount === 0"
          :loading="markingAll"
          @click="markAllRead"
        >
          全部已读
        </el-button>
        <el-button
          v-if="hasSend"
          type="primary"
          :icon="EditPen"
          @click="sendVisible = true"
        >
          发送消息
        </el-button>
      </div>
    </div>

    <MessageNav />

    <section class="panel-card inbox-panel">
      <div class="inbox-toolbar">
        <div class="read-tabs">
          <button
            type="button"
            class="read-tab"
            :class="{ active: readFilter === 'all' }"
            @click="setReadFilter('all')"
          >
            全部
          </button>
          <button
            type="button"
            class="read-tab"
            :class="{ active: readFilter === 'unread' }"
            @click="setReadFilter('unread')"
          >
            未读
            <span
              v-if="messageStore.unreadCount > 0"
              class="tab-count"
            >
              {{ messageStore.unreadCount > 99 ? '99+' : messageStore.unreadCount }}
            </span>
          </button>
          <button
            type="button"
            class="read-tab"
            :class="{ active: readFilter === 'read' }"
            @click="setReadFilter('read')"
          >
            已读
          </button>
        </div>

        <div class="toolbar-filters">
          <el-select
            v-model="filters.messageType"
            placeholder="全部类型"
            clearable
            style="width: 156px"
            @change="handleSearch"
          >
            <el-option
              v-for="item in MESSAGE_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-button
            circle
            :icon="Refresh"
            title="刷新消息列表"
            @click="loadMessages"
          />
        </div>
      </div>

      <div v-if="loading && !records.length" class="skeleton-list">
        <div v-for="index in 4" :key="index" class="skeleton-row">
          <el-skeleton animated :rows="2" />
        </div>
      </div>

      <div
        v-else-if="records.length"
        v-loading="loading"
        class="message-list"
      >
        <article
          v-for="message in records"
          :key="message.id"
          class="message-item"
          :class="{ unread: message.readStatus === 0 }"
          role="button"
          tabindex="0"
          @click="openMessage(message)"
          @keyup.enter="openMessage(message)"
        >
          <span
            class="unread-dot"
            :class="{ visible: message.readStatus === 0 }"
          />

          <div class="message-main">
            <div class="message-top">
              <el-tag
                :type="messageTypeTagType(message.messageType)"
                effect="light"
                size="small"
                class="type-tag"
              >
                {{ message.messageTypeName }}
              </el-tag>
              <h3>{{ message.title }}</h3>
              <time class="message-time">
                {{ formatDateTime(message.publishTime) }}
              </time>
            </div>

            <p class="message-preview">{{ message.content }}</p>

            <div class="message-meta">
              <span>发送人：{{ message.senderName || '系统' }}</span>
              <span v-if="message.sendType === 2">全体广播</span>
              <span v-if="message.readStatus === 0" class="unread-label">
                未读
              </span>
            </div>
          </div>

          <el-icon class="item-arrow"><ArrowRight /></el-icon>
        </article>
      </div>

      <div v-else class="empty-wrap">
        <el-empty
          :description="
            readFilter === 'unread'
              ? '没有未读消息'
              : readFilter === 'read'
                ? '还没有已读消息'
                : '暂无消息'
          "
          :image-size="96"
        >
          <el-button
            v-if="readFilter !== 'all' || filters.messageType"
            @click="resetFilters"
          >
            清空筛选条件
          </el-button>
        </el-empty>
      </div>

      <div v-if="total > 0" class="inbox-footer">
        <span class="total-hint">
          第 {{ query.page }} / {{ pages }} 页，共 {{ total }} 条消息
        </span>
        <el-pagination
          :current-page="query.page"
          :page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <MessageDetailDrawer
      v-model="detailVisible"
      :message-id="detailId"
      @read="handleMessageRead"
    />

    <MessageSendDialog
      v-model="sendVisible"
      @sent="handleSent"
    />
  </div>
</template>

<style scoped>
.message-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.message-eyebrow {
  display: block;
  margin-bottom: 6px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2.2px;
  color: #9cb6ac;
}

.heading-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.inbox-panel {
  padding: 18px 20px 12px;
}

.inbox-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding-bottom: 14px;
  border-bottom: 1px solid #eef4f1;
}

.read-tabs {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  background: #f5f9f7;
  border-radius: 10px;
}

.read-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  font-family: inherit;
  font-size: 12.5px;
  color: var(--hp-text-secondary);
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 7px;
  transition: all 0.18s ease;
}

.read-tab:hover {
  color: var(--hp-primary-dark);
}

.read-tab.active {
  font-weight: 600;
  color: var(--hp-primary-dark);
  background: #fff;
  box-shadow: 0 2px 8px rgba(23, 51, 44, 0.08);
}

.tab-count {
  min-width: 18px;
  padding: 1px 5px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  text-align: center;
  background: var(--hp-danger);
  border-radius: 999px;
}

.toolbar-filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-list {
  display: flex;
  flex-direction: column;
  padding-top: 4px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 15px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f5f3;
  border-radius: 10px;
  outline: none;
  transition: background 0.18s ease;
}

.message-item:last-child {
  border-bottom-color: transparent;
}

.message-item:hover,
.message-item:focus-visible {
  background: #f8fcfa;
}

.message-item.unread {
  background: #f7fbff;
}

.message-item.unread:hover {
  background: #f1f8ff;
}

.unread-dot {
  width: 7px;
  height: 7px;
  margin-top: 8px;
  flex-shrink: 0;
  background: transparent;
  border-radius: 50%;
}

.unread-dot.visible {
  background: #3f7ff0;
  box-shadow: 0 0 0 3px rgba(63, 127, 240, 0.12);
}

.message-main {
  flex: 1;
  min-width: 0;
}

.message-top {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.type-tag {
  flex-shrink: 0;
}

.message-top h3 {
  flex: 1;
  overflow: hidden;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--hp-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-item.unread .message-top h3 {
  font-weight: 700;
}

.message-time {
  flex-shrink: 0;
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.message-preview {
  display: -webkit-box;
  overflow: hidden;
  margin: 7px 0 8px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--hp-text-muted);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.message-item.unread .message-preview {
  color: var(--hp-text-secondary);
}

.message-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 14px;
  font-size: 11px;
  color: var(--hp-text-muted);
}

.unread-label {
  color: #3f7ff0;
  font-weight: 600;
}

.item-arrow {
  margin-top: 9px;
  color: #c3d3cd;
  opacity: 0;
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.message-item:hover .item-arrow {
  opacity: 1;
  transform: translateX(2px);
}

.skeleton-list {
  padding: 12px 0;
}

.skeleton-row {
  padding: 14px 12px;
  border-bottom: 1px solid #f2f6f4;
}

.empty-wrap {
  padding: 20px 0 12px;
}

.inbox-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 2px 6px;
}

.total-hint {
  font-size: 12px;
  color: var(--hp-text-muted);
}

@media (max-width: 768px) {
  .inbox-panel {
    padding: 14px 12px 10px;
  }

  .inbox-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-filters {
    justify-content: space-between;
  }

  .message-top {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .message-top h3 {
    flex-basis: 100%;
    order: 3;
    white-space: normal;
  }

  .message-time {
    margin-left: auto;
  }

  .item-arrow {
    display: none;
  }

  .inbox-footer {
    justify-content: center;
  }
}
</style>
