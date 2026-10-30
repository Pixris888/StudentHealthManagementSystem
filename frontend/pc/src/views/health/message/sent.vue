<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  EditPen,
  Refresh,
  View,
} from '@element-plus/icons-vue'
import { getSentMessagePage } from '@/api/message'
import { useMessageStore } from '@/stores/message'
import {
  SEND_MESSAGE_TYPE_OPTIONS,
  messageTypeTagType,
} from '@/utils/message'
import { formatDateTime } from '@/utils/format'
import type { MessageType, MessageVO } from '@/types/message'
import MessageNav from './components/MessageNav.vue'
import MessageDetailDrawer from './components/MessageDetailDrawer.vue'
import MessageSendDialog from './components/MessageSendDialog.vue'

const messageStore = useMessageStore()

const records = ref<MessageVO[]>([])
const total = ref(0)
const pages = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })
const filters = reactive<{ messageType?: MessageType }>({})

async function loadMessages(): Promise<void> {
  loading.value = true
  try {
    const page = await getSentMessagePage({
      page: query.page,
      size: query.size,
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

function handleSearch(): void {
  query.page = 1
  loadMessages()
}

function resetFilters(): void {
  filters.messageType = undefined
  query.page = 1
  query.size = 10
  loadMessages()
}

function handlePageChange(page: number): void {
  query.page = page
  loadMessages()
}

function handleSizeChange(size: number): void {
  query.size = size
  query.page = 1
  loadMessages()
}

/* ---------------- 详情 ---------------- */
const detailVisible = ref(false)
const selectedMessage = ref<MessageVO | null>(null)

function openDetail(row: MessageVO): void {
  selectedMessage.value = row
  detailVisible.value = true
}

/* ---------------- 发送 ---------------- */
const sendVisible = ref(false)

function handleSent(): void {
  void messageStore.refreshUnread()
  query.page = 1
  loadMessages()
}

onMounted(loadMessages)
</script>

<template>
  <div class="message-page">
    <div class="page-heading">
      <div>
        <span class="message-eyebrow">SENT MESSAGES</span>
        <h2>已发送消息</h2>
        <p>查看自己发送过的校园公告、健康关怀与其他消息。</p>
      </div>
      <div class="heading-actions">
        <el-button type="primary" :icon="EditPen" @click="sendVisible = true">
          发送消息
        </el-button>
      </div>
    </div>

    <MessageNav />

    <section class="panel-card table-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="table-title">发送记录</span>
          <el-tag type="info" effect="plain">共 {{ total }} 条</el-tag>
          <el-select
            v-model="filters.messageType"
            placeholder="全部类型"
            clearable
            style="width: 156px"
            @change="handleSearch"
          >
            <el-option
              v-for="item in SEND_MESSAGE_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </div>
        <el-button :icon="Refresh" circle title="刷新" @click="loadMessages" />
      </div>

      <el-table
        v-loading="loading"
        :data="records"
        row-key="id"
        style="width: 100%"
      >
        <el-table-column label="消息类型" width="126" align="center">
          <template #default="{ row }">
            <el-tag
              :type="messageTypeTagType(row.messageType)"
              effect="light"
            >
              {{ row.messageTypeName }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="标题" min-width="300">
          <template #default="{ row }">
            <div class="title-cell">
              <button
                type="button"
                class="title-link"
                @click="openDetail(row)"
              >
                {{ row.title }}
              </button>
              <span class="content-preview">{{ row.content }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="接收人" width="170">
          <template #default="{ row }">
            <span class="receiver-cell">
              {{ row.receiverName || (row.sendType === 2 ? '全体学生' : '—') }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="发送方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.sendType === 2 ? 'warning' : 'info'"
              effect="plain"
              size="small"
            >
              {{ row.sendTypeName }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="发送时间" width="160">
          <template #default="{ row }">
            <span class="time-cell">{{ formatDateTime(row.publishTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :icon="View"
              @click="openDetail(row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty
            description="暂无发送记录，点击右上角发送第一条消息"
            :image-size="88"
          />
        </template>
      </el-table>

      <div class="table-footer">
        <span class="selection-tip">
          校园公告会显示为“全体学生”，健康关怀与其他消息显示接收人姓名。
        </span>
        <el-pagination
          v-if="total > 0"
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
      :message="selectedMessage"
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

.table-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.title-link {
  overflow: hidden;
  padding: 0;
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--hp-text);
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: none;
  border: none;
}

.title-link:hover {
  color: var(--hp-primary);
}

.content-preview {
  overflow: hidden;
  font-size: 11.5px;
  color: var(--hp-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.receiver-cell {
  font-size: 12.5px;
  color: var(--hp-text-secondary);
}

.time-cell {
  font-size: 12px;
  color: var(--hp-text-secondary);
}

@media (max-width: 768px) {
  .toolbar-left {
    width: 100%;
  }
}
</style>
