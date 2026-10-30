<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageAdminConsultations } from '@/api/ai'
import {
  ANSWER_STATUS_OPTIONS,
  FEEDBACK_OPTIONS,
  feedbackLabel,
  feedbackTagType,
  formatLatency,
  providerLabel,
} from '@/utils/ai'
import { formatDateTime } from '@/utils/format'
import type {
  AiConsultationVO,
  AnswerStatus,
  FeedbackStatus,
} from '@/types/ai'
import AiConsultationDetailDrawer from './AiConsultationDetailDrawer.vue'
import AiStatusTag from './AiStatusTag.vue'

const records = ref<AiConsultationVO[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })
const filters = reactive<{
  keyword: string
  answerStatus: AnswerStatus | undefined
  feedback: FeedbackStatus | undefined
}>({
  keyword: '',
  answerStatus: undefined,
  feedback: undefined,
})
const dateRange = ref<[string, string] | null>(null)

const detailVisible = ref(false)
const detailId = ref<number | null>(null)

async function loadRecords(): Promise<void> {
  loading.value = true
  try {
    const range = dateRange.value
    const page = await pageAdminConsultations({
      page: query.page,
      size: query.size,
      keyword: filters.keyword.trim() || undefined,
      answerStatus: filters.answerStatus,
      feedback: filters.feedback,
      startDate: range?.[0],
      endDate: range?.[1],
    })
    records.value = page.records
    total.value = page.total
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  query.page = 1
  loadRecords()
}

function resetQuery(): void {
  filters.keyword = ''
  filters.answerStatus = undefined
  filters.feedback = undefined
  dateRange.value = null
  query.page = 1
  query.size = 10
  loadRecords()
}

function handlePageChange(page: number): void {
  query.page = page
  loadRecords()
}

function handleSizeChange(size: number): void {
  query.size = size
  query.page = 1
  loadRecords()
}

function openDetail(row: AiConsultationVO): void {
  detailId.value = row.id
  detailVisible.value = true
}

onMounted(loadRecords)
</script>

<template>
  <div class="manage-panel">
    <section class="panel-card search-card">
      <el-form :model="filters" inline class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索问题或回答"
            clearable
            maxlength="100"
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="回答状态">
          <el-select
            v-model="filters.answerStatus"
            placeholder="全部状态"
            clearable
            style="width: 136px"
            @change="handleSearch"
          >
            <el-option
              v-for="item in ANSWER_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="反馈">
          <el-select
            v-model="filters.feedback"
            placeholder="全部反馈"
            clearable
            style="width: 136px"
            @change="handleSearch"
          >
            <el-option
              v-for="item in FEEDBACK_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            clearable
            style="width: 250px"
            @change="handleSearch"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            查询
          </el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="panel-card table-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="table-title">问答记录</span>
          <el-tag type="info" effect="plain">共 {{ total }} 条</el-tag>
        </div>
        <div class="toolbar-right">
          <el-button
            :icon="Refresh"
            circle
            :loading="loading"
            title="刷新记录"
            @click="loadRecords"
          />
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="records"
        row-key="id"
        style="width: 100%"
      >
        <el-table-column label="时间" width="136">
          <template #default="{ row }">
            <span class="time-cell">
              {{ formatDateTime(row.consultTime) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="学生" min-width="124">
          <template #default="{ row }">
            <div class="student-cell">
              <span class="student-name">
                {{ row.realName || (row.userId ? `用户 #${row.userId}` : '—') }}
              </span>
              <span class="student-no">
                {{ row.studentNo || '学号未提供' }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column
          label="问题"
          min-width="220"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="question-cell">{{ row.question }}</span>
          </template>
        </el-table-column>

        <el-table-column label="模型" min-width="124">
          <template #default="{ row }">
            <div class="model-cell">
              <span>{{ providerLabel(row.provider) || '—' }}</span>
              <small>{{ row.modelName || '未记录模型' }}</small>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="84" align="center">
          <template #default="{ row }">
            <AiStatusTag :status="row.answerStatus" />
          </template>
        </el-table-column>

        <el-table-column label="反馈" width="84" align="center">
          <template #default="{ row }">
            <el-tag
              :type="feedbackTagType(row.feedback)"
              effect="plain"
              size="small"
            >
              {{ feedbackLabel(row.feedback) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="耗时" width="76" align="right">
          <template #default="{ row }">
            <span class="latency-cell">{{ formatLatency(row.latencyMs) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="紧急" width="70" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.emergency === 1"
              type="danger"
              effect="dark"
              size="small"
            >
              紧急
            </el-tag>
            <span v-else class="muted-text">—</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="88" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty
            description="暂无 AI 问答记录"
            :image-size="88"
          />
        </template>
      </el-table>

      <div class="table-footer">
        <span class="selection-tip">
          校医账号展示脱敏后的学号 / 姓名，超级管理员展示完整信息。
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

    <AiConsultationDetailDrawer
      v-model="detailVisible"
      :consultation-id="detailId"
    />
  </div>
</template>

<style scoped>
.manage-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card {
  padding: 18px 20px 4px;
}

.table-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.time-cell,
.latency-cell {
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.student-cell,
.model-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.student-name {
  overflow: hidden;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--hp-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.student-no,
.model-cell small {
  overflow: hidden;
  font-size: 10.5px;
  color: var(--hp-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-cell span {
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.question-cell {
  font-size: 12.5px;
  color: var(--hp-text);
}

@media (max-width: 768px) {
  .search-card {
    padding: 14px 12px 0;
  }
}
</style>
