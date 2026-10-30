<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search, View } from '@element-plus/icons-vue'
import { pageOperLogs } from '@/api/oper-log'
import {
  BUSINESS_TYPE_OPTIONS,
  OPER_LOG_STATUS_OPTIONS,
  businessTypeLabel,
  businessTypeTagType,
  operLogStatusLabel,
  operLogStatusTagType,
  requestMethodTagType,
} from '@/utils/system'
import { formatDateTime } from '@/utils/format'
import type { OperLog, OperLogStatus } from '@/types/system'
import OperLogDetailDrawer from './components/OperLogDetailDrawer.vue'

const records = ref<OperLog[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })
const filters = reactive<{
  keyword: string
  businessType: number | undefined
  status: OperLogStatus | undefined
  operUserId: number | undefined
}>({
  keyword: '',
  businessType: undefined,
  status: undefined,
  operUserId: undefined,
})
const dateRange = ref<[string, string] | null>(null)

const detailVisible = ref(false)
const detailId = ref<number | null>(null)

async function loadLogs(): Promise<void> {
  loading.value = true
  try {
    const range = dateRange.value
    const page = await pageOperLogs({
      page: query.page,
      size: query.size,
      keyword: filters.keyword.trim() || undefined,
      businessType: filters.businessType,
      status: filters.status,
      startDate: range?.[0],
      endDate: range?.[1],
      operUserId: filters.operUserId || undefined,
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
  loadLogs()
}

function resetQuery(): void {
  filters.keyword = ''
  filters.businessType = undefined
  filters.status = undefined
  filters.operUserId = undefined
  dateRange.value = null
  query.page = 1
  query.size = 10
  loadLogs()
}

function handlePageChange(page: number): void {
  query.page = page
  loadLogs()
}

function handleSizeChange(size: number): void {
  query.size = size
  query.page = 1
  loadLogs()
}

function openDetail(row: OperLog): void {
  detailId.value = row.id
  detailVisible.value = true
}

onMounted(loadLogs)
</script>

<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <span class="page-eyebrow">OPERATION AUDIT</span>
        <h2>操作日志</h2>
        <p>
          查询登录、用户与角色维护、参数设置、字典管理、报表导出、数据备份等安全审计记录。
        </p>
      </div>
      <el-tag type="info" effect="plain">只读审计记录</el-tag>
    </div>

    <section class="panel-card search-card">
      <el-form :model="filters" inline class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="标题 / 操作人 / IP"
            clearable
            maxlength="100"
            style="width: 210px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="业务类型">
          <el-select
            v-model="filters.businessType"
            placeholder="全部类型"
            clearable
            style="width: 136px"
            @change="handleSearch"
          >
            <el-option
              v-for="item in BUSINESS_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="filters.status"
            placeholder="全部状态"
            clearable
            style="width: 120px"
            @change="handleSearch"
          >
            <el-option
              v-for="item in OPER_LOG_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="操作人ID">
          <el-input-number
            v-model="filters.operUserId"
            :min="1"
            :controls="false"
            placeholder="可选"
            style="width: 120px"
          />
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
          <span class="table-title">审计记录</span>
          <el-tag type="info" effect="plain">共 {{ total }} 条</el-tag>
          <el-tag type="warning" effect="plain" size="small">
            不可修改 / 删除
          </el-tag>
        </div>
        <span class="toolbar-hint">
          操作日志按时间倒序排列；详情中可查看请求参数与返回结果
        </span>
      </div>

      <el-table
        v-loading="loading"
        :data="records"
        row-key="id"
        style="width: 100%"
      >
        <el-table-column label="操作时间" width="140">
          <template #default="{ row }">
            <span class="time-cell">
              {{ formatDateTime(row.operTime ?? row.createTime) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column
          prop="title"
          label="模块标题"
          min-width="140"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="title-cell">{{ row.title || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="业务类型" width="88" align="center">
          <template #default="{ row }">
            <el-tag
              :type="businessTypeTagType(row.businessType)"
              effect="light"
              size="small"
            >
              {{ businessTypeLabel(row.businessType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作人" min-width="124">
          <template #default="{ row }">
            <div class="operator-cell">
              <span class="operator-name">
                {{ row.operUserName || '系统' }}
              </span>
              <span v-if="row.operUserId" class="operator-id">
                ID {{ row.operUserId }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column
          prop="operIp"
          label="IP"
          width="120"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="ip-cell">{{ row.operIp || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="请求方式" width="82" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.requestMethod"
              :type="requestMethodTagType(row.requestMethod)"
              effect="plain"
              size="small"
            >
              {{ row.requestMethod }}
            </el-tag>
            <span v-else class="muted-text">—</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="76" align="center">
          <template #default="{ row }">
            <el-tag
              :type="operLogStatusTagType(row.status)"
              effect="light"
              size="small"
            >
              {{ operLogStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          prop="errorMsg"
          label="错误信息"
          min-width="150"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span
              v-if="row.status === 1 && row.errorMsg"
              class="error-cell"
            >
              {{ row.errorMsg }}
            </span>
            <span v-else class="muted-text">—</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="84" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :icon="View"
              @click="openDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty
            description="暂无操作日志，试试调整筛选条件"
            :image-size="88"
          />
        </template>
      </el-table>

      <div class="table-footer">
        <span class="selection-tip">
          操作日志为只读安全审计数据，系统不提供删除接口。
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

    <OperLogDetailDrawer
      v-model="detailVisible"
      :log-id="detailId"
    />
  </div>
</template>

<style scoped>
.page-eyebrow {
  display: block;
  margin-bottom: 6px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2.2px;
  color: #9cb6ac;
}

.time-cell,
.ip-cell {
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.title-cell {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--hp-text);
}

.operator-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.operator-name {
  overflow: hidden;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--hp-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.operator-id {
  font-size: 10.5px;
  color: var(--hp-text-muted);
}

.error-cell {
  font-size: 12px;
  color: var(--hp-danger);
}
</style>
