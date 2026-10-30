<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Calendar,
  Check,
  Delete,
  MagicStick,
  Refresh,
  View,
} from '@element-plus/icons-vue'
import {
  adoptPlan,
  deleteMyPlans,
  generatePlan,
  pageMyPlans,
} from '@/api/ai'
import {
  PLAN_STATUS_OPTIONS,
  PLAN_TYPE_OPTIONS,
  planTypeLabel,
  providerLabel,
  resolveAiError,
} from '@/utils/ai'
import { formatDate, formatDateTime } from '@/utils/format'
import type {
  AiHealthPlanVO,
  PlanStatus,
  PlanType,
} from '@/types/ai'
import AiPlanDetailDrawer from './AiPlanDetailDrawer.vue'
import AiStatusTag from './AiStatusTag.vue'

const records = ref<AiHealthPlanVO[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 9 })
const typeFilter = ref<number>(0)
const statusFilter = ref<PlanStatus | undefined>(undefined)

const generateVisible = ref(false)
const generateType = ref<PlanType>(3)
const generating = ref(false)
const generateError = ref('')

const detailVisible = ref(false)
const detailPlanId = ref<number | null>(null)
const detailPlan = ref<AiHealthPlanVO | null>(null)

async function loadPlans(): Promise<void> {
  loading.value = true
  try {
    const page = await pageMyPlans({
      page: query.page,
      size: query.size,
      planType: typeFilter.value
        ? (typeFilter.value as PlanType)
        : undefined,
      status: statusFilter.value,
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
  loadPlans()
}

function resetFilters(): void {
  typeFilter.value = 0
  statusFilter.value = undefined
  query.page = 1
  query.size = 9
  loadPlans()
}

function handlePageChange(page: number): void {
  query.page = page
  loadPlans()
}

function handleSizeChange(size: number): void {
  query.size = size
  query.page = 1
  loadPlans()
}

function periodText(plan: AiHealthPlanVO): string {
  if (plan.startDate && plan.endDate) {
    return `${formatDate(plan.startDate)} ~ ${formatDate(plan.endDate)}`
  }
  return '周期未设置'
}

function baseText(plan: AiHealthPlanVO): string {
  return plan.baseCheckinDate
    ? `基于 ${formatDate(plan.baseCheckinDate)} 的最近打卡生成`
    : '通用计划（未使用近期打卡数据）'
}

function modelText(plan: AiHealthPlanVO): string {
  const parts = [providerLabel(plan.provider), plan.modelName].filter(Boolean)
  return parts.join(' · ')
}

function openGenerate(): void {
  generateType.value = 3
  generateError.value = ''
  generateVisible.value = true
}

function handleGenerateClose(done: () => void): void {
  if (!generating.value) done()
}

async function submitGenerate(): Promise<void> {
  if (generating.value) return
  generating.value = true
  generateError.value = ''
  try {
    const plan = await generatePlan({ planType: generateType.value })
    ElMessage.success('健康计划已生成')
    generateVisible.value = false
    await loadPlans()
    openDetail(plan)
  } catch (error) {
    generateError.value = resolveAiError(
      error,
      '计划生成失败，请稍后重试',
    )
  } finally {
    generating.value = false
  }
}

function openDetail(plan: AiHealthPlanVO): void {
  detailPlanId.value = plan.id
  detailPlan.value = plan
  detailVisible.value = true
}

function handleUpdated(updated: AiHealthPlanVO): void {
  const row = records.value.find((item) => item.id === updated.id)
  if (row) Object.assign(row, updated)
  if (statusFilter.value === 0) {
    loadPlans()
  }
}

function handleDeleted(): void {
  loadPlans()
}

async function handleAdopt(plan: AiHealthPlanVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '采纳后会标记为已采纳，方便你按计划执行。是否继续？',
      '采纳计划',
      {
        type: 'info',
        confirmButtonText: '采纳计划',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  try {
    const updated = await adoptPlan(plan.id)
    Object.assign(plan, updated)
    ElMessage.success('计划已采纳')
    if (statusFilter.value === 0) {
      loadPlans()
    }
  } catch {
    // 请求错误由拦截器统一提示
  }
}

async function handleDelete(plan: AiHealthPlanVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除计划“${plan.planTitle || planTypeLabel(plan.planType, plan.planTypeName)}”吗？删除后不可恢复。`,
      '删除计划',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return
  }
  try {
    await deleteMyPlans([plan.id])
    ElMessage.success('计划已删除')
    if (records.value.length === 1 && query.page > 1) {
      query.page -= 1
    }
    await loadPlans()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

onMounted(loadPlans)
</script>

<template>
  <section class="panel-card plan-panel">
    <div class="plan-toolbar">
      <div class="toolbar-left">
        <span class="panel-title">计划列表</span>
        <el-tag type="info" effect="plain">共 {{ total }} 个</el-tag>
      </div>
      <el-button type="primary" :icon="MagicStick" @click="openGenerate">
        生成新计划
      </el-button>
    </div>

    <div class="plan-filters">
      <el-radio-group
        v-model="typeFilter"
        size="small"
        @change="handleSearch"
      >
        <el-radio-button :value="0">全部</el-radio-button>
        <el-radio-button
          v-for="item in PLAN_TYPE_OPTIONS"
          :key="item.value"
          :value="item.value"
        >
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>

      <el-select
        v-model="statusFilter"
        placeholder="全部状态"
        clearable
        size="small"
        style="width: 130px"
        @change="handleSearch"
      >
        <el-option
          v-for="item in PLAN_STATUS_OPTIONS"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>

      <el-button
        :icon="Refresh"
        circle
        size="small"
        title="刷新计划列表"
        :loading="loading"
        @click="loadPlans"
      />
      <el-button
        v-if="typeFilter !== 0 || statusFilter !== undefined"
        link
        type="primary"
        size="small"
        @click="resetFilters"
      >
        重置筛选
      </el-button>
    </div>

    <div v-loading="loading" class="plan-grid">
      <template v-if="records.length">
        <article
          v-for="plan in records"
          :key="plan.id"
          class="plan-card"
        >
          <header class="plan-card-head">
            <h3>{{ plan.planTitle || plan.planTypeName }}</h3>
            <AiStatusTag :status="plan.status" kind="plan" />
          </header>

          <div class="plan-card-tags">
            <el-tag type="primary" effect="plain" size="small">
              {{ plan.planTypeName || planTypeLabel(plan.planType) }}
            </el-tag>
            <span class="plan-period">
              <el-icon><Calendar /></el-icon>
              {{ periodText(plan) }}
            </span>
          </div>

          <p class="plan-basis">{{ baseText(plan) }}</p>

          <div class="plan-card-meta">
            <span>生成于 {{ formatDateTime(plan.createTime) }}</span>
            <span v-if="plan.status === 1 && plan.adoptedTime">
              采纳于 {{ formatDateTime(plan.adoptedTime) }}
            </span>
            <span v-if="modelText(plan)" class="plan-model">
              {{ modelText(plan) }}
            </span>
          </div>

          <footer class="plan-card-actions">
            <el-button
              link
              type="primary"
              :icon="View"
              @click="openDetail(plan)"
            >
              查看
            </el-button>
            <el-button
              v-if="plan.status === 0"
              link
              type="success"
              :icon="Check"
              @click="handleAdopt(plan)"
            >
              采纳
            </el-button>
            <el-tag
              v-else
              type="success"
              size="small"
              effect="plain"
              class="adopted-tag"
            >
              已采纳
            </el-tag>
            <el-button
              link
              type="danger"
              :icon="Delete"
              @click="handleDelete(plan)"
            >
              删除
            </el-button>
          </footer>
        </article>
      </template>

      <el-empty
        v-else-if="!loading"
        description="暂无健康计划，点击“生成新计划”试试"
        :image-size="92"
        class="plan-empty"
      />
    </div>

    <div class="plan-footer">
      <span class="selection-tip">
        计划会结合健康档案和最近 7 天打卡生成；没有数据时会生成通用建议。
      </span>
      <el-pagination
        :current-page="query.page"
        :page-size="query.size"
        :page-sizes="[9, 12, 24]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </section>

  <el-dialog
    v-model="generateVisible"
    title="生成健康计划"
    width="540px"
    append-to-body
    :show-close="!generating"
    :close-on-click-modal="false"
    :close-on-press-escape="!generating"
    :before-close="handleGenerateClose"
  >
    <div class="generate-body">
      <p class="generate-desc">
        计划会结合你的健康档案和最近 7 天打卡生成；没有数据时生成通用计划。
      </p>

      <el-radio-group
        v-model="generateType"
        class="generate-types"
        :disabled="generating"
      >
        <el-radio-button
          v-for="item in PLAN_TYPE_OPTIONS"
          :key="item.value"
          :value="item.value"
        >
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="生成需要一些时间"
        description="AI 正在同步生成计划，最长可能需要 2 分钟。请保持页面打开，不要重复提交。"
        class="generate-alert"
      />

      <el-alert
        v-if="generateError"
        type="error"
        :closable="false"
        show-icon
        :title="generateError"
        class="generate-alert"
      />
    </div>

    <template #footer>
      <el-button :disabled="generating" @click="generateVisible = false">
        取消
      </el-button>
      <el-button
        type="primary"
        :loading="generating"
        @click="submitGenerate"
      >
        {{ generating ? '正在生成…' : '开始生成' }}
      </el-button>
    </template>
  </el-dialog>

  <AiPlanDetailDrawer
    v-model="detailVisible"
    :plan-id="detailPlanId"
    :initial-plan="detailPlan"
    @updated="handleUpdated"
    @deleted="handleDeleted"
  />
</template>

<style scoped>
.plan-panel {
  padding: 18px 20px 12px;
}

.plan-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--hp-text);
}

.plan-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 14px;
  margin-bottom: 16px;
  border-bottom: 1px solid #eef4f1;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
  min-height: 180px;
}

.plan-card {
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding: 16px 17px 12px;
  background:
    linear-gradient(180deg, #ffffff, #fbfdfc);
  border: 1px solid var(--hp-border);
  border-radius: 13px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.plan-card:hover {
  border-color: #cbe8dc;
  box-shadow: 0 10px 26px rgba(23, 51, 44, 0.06);
}

.plan-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.plan-card-head h3 {
  flex: 1;
  font-size: 14.5px;
  font-weight: 700;
  line-height: 1.5;
  color: var(--hp-text);
  word-break: break-word;
}

.plan-card-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.plan-period {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  color: var(--hp-text-secondary);
}

.plan-basis {
  font-size: 12px;
  line-height: 1.6;
  color: var(--hp-text-secondary);
}

.plan-card-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 10.5px;
  color: var(--hp-text-muted);
}

.plan-model {
  color: #9ab0a8;
}

.plan-card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-top: 8px;
  margin-top: auto;
  border-top: 1px dashed #eef4f1;
}

.plan-card-actions :deep(.el-button + .el-button) {
  margin-left: 4px;
}

.adopted-tag {
  margin: 0 4px;
}

.plan-empty {
  grid-column: 1 / -1;
  padding: 30px 0;
}

.plan-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 2px 8px;
}

.generate-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.generate-desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--hp-text-secondary);
}

.generate-types {
  display: flex;
  flex-wrap: wrap;
}

.generate-alert {
  margin-top: 2px;
}

@media (max-width: 768px) {
  .plan-panel {
    padding: 14px 12px 8px;
  }

  .plan-grid {
    grid-template-columns: 1fr;
  }

  .plan-footer {
    justify-content: center;
  }
}
</style>
