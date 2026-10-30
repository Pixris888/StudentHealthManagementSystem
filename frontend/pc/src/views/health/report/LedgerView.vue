<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Download,
  Refresh,
  Search,
  TrendCharts,
} from '@element-plus/icons-vue'
import {
  downloadLedgerReport,
  getLedgerFilters,
  getLedgerPage,
} from '@/api/report'
import { useUserStore } from '@/stores/user'
import {
  bmiTagType,
  currentMonthValue,
  formatCount,
  formatDecimal,
  formatPercent,
  formatText,
  isFutureMonth,
  riskTagType,
} from '@/utils/report'
import type {
  FilterOptionsVO,
  LedgerQuery,
  LedgerRowVO,
} from '@/types/report'
import ReportNav from './components/ReportNav.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function routeText(value: unknown): string | undefined {
  return typeof value === 'string' && value ? value : undefined
}

const filters = reactive<{
  month: string
  college?: string
  grade?: string
  className?: string
  keyword: string
}>({
  month: routeText(route.query.month) ?? currentMonthValue(),
  college: routeText(route.query.college),
  grade: routeText(route.query.grade),
  className: routeText(route.query.className),
  keyword: '',
})

const query = reactive({ page: 1, size: 10 })
const filterOptions = ref<FilterOptionsVO>({
  colleges: [],
  grades: [],
  classes: [],
})
const filtersLoading = ref(false)
const rows = ref<LedgerRowVO[]>([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)

const isSuperAdmin = computed(() => userStore.roles.includes('SUPER_ADMIN'))
const canExport = computed(() => userStore.hasPermission('report:export'))

function buildQuery(): LedgerQuery {
  return {
    page: query.page,
    size: query.size,
    month: filters.month,
    college: filters.college || undefined,
    grade: filters.grade || undefined,
    className: filters.className || undefined,
    keyword: filters.keyword.trim() || undefined,
  }
}

async function loadFilters(): Promise<void> {
  filtersLoading.value = true
  try {
    filterOptions.value = await getLedgerFilters()
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    filtersLoading.value = false
  }
}

async function loadPage(): Promise<void> {
  loading.value = true
  try {
    const page = await getLedgerPage(buildQuery())
    rows.value = page.records
    total.value = page.total
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  query.page = 1
  loadPage()
}

function resetFilters(): void {
  filters.month = currentMonthValue()
  filters.college = undefined
  filters.grade = undefined
  filters.className = undefined
  filters.keyword = ''
  query.page = 1
  query.size = 10
  loadPage()
}

function handlePageChange(page: number): void {
  query.page = page
  loadPage()
}

function handleSizeChange(size: number): void {
  query.size = size
  query.page = 1
  loadPage()
}

function backToDashboard(): void {
  const queryParams: Record<string, string> = { month: filters.month }
  if (filters.college) queryParams.college = filters.college
  if (filters.grade) queryParams.grade = filters.grade
  router.push({ path: '/health/report/dashboard', query: queryParams })
}

async function handleExport(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `将按当前筛选条件导出 ${filters.month} 的校园健康台账，确定继续吗？`,
      '导出台账',
      {
        type: 'warning',
        confirmButtonText: '开始导出',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  exporting.value = true
  try {
    await downloadLedgerReport(buildQuery())
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadFilters()
  loadPage()
})
</script>

<template>
  <div class="ledger-page">
    <div class="page-heading report-heading">
      <div>
        <span class="report-eyebrow">HEALTH LEDGER</span>
        <h2>健康台账</h2>
        <p>按月份、学院、年级与班级查询学生健康统计，并按角色脱敏导出。</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="TrendCharts" @click="backToDashboard">
          健康看板
        </el-button>
        <el-button
          v-if="canExport"
          type="primary"
          :icon="Download"
          :loading="exporting"
          @click="handleExport"
        >
          导出台账
        </el-button>
      </div>
    </div>

    <ReportNav />

    <section class="panel-card search-card">
      <el-form :model="filters" inline class="search-form">
        <el-form-item label="月份">
          <el-date-picker
            v-model="filters.month"
            type="month"
            value-format="YYYY-MM"
            format="YYYY-MM"
            :disabled-date="isFutureMonth"
            :clearable="false"
            placeholder="选择月份"
            style="width: 146px"
          />
        </el-form-item>
        <el-form-item label="学院">
          <el-select
            v-model="filters.college"
            placeholder="全部学院"
            clearable
            filterable
            :loading="filtersLoading"
            style="width: 200px"
          >
            <el-option
              v-for="item in filterOptions.colleges"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select
            v-model="filters.grade"
            placeholder="全部年级"
            clearable
            filterable
            :loading="filtersLoading"
            style="width: 140px"
          >
            <el-option
              v-for="item in filterOptions.grades"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select
            v-model="filters.className"
            placeholder="全部班级"
            clearable
            filterable
            :loading="filtersLoading"
            style="width: 170px"
          >
            <el-option
              v-for="item in filterOptions.classes"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名/学号">
          <el-input
            v-model="filters.keyword"
            placeholder="输入姓名或学号"
            clearable
            maxlength="50"
            style="width: 190px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            查询
          </el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="panel-card table-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="table-title">学生健康台账</span>
          <el-tag type="info" effect="plain">共 {{ total }} 人</el-tag>
          <el-tag
            v-if="!isSuperAdmin"
            type="warning"
            effect="plain"
            size="small"
          >
            校医视角：姓名/学号已脱敏
          </el-tag>
        </div>
        <span class="toolbar-hint">
          比率字段为百分数，平均睡眠单位为小时，平均运动单位为分钟；表格可左右滚动
        </span>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        row-key="userId"
        size="small"
        style="width: 100%"
      >
        <el-table-column label="学生信息" align="center">
          <el-table-column
            prop="studentNo"
            label="学号"
            width="120"
            fixed="left"
            show-overflow-tooltip
          />
          <el-table-column
            prop="realName"
            label="姓名"
            width="90"
            fixed="left"
            show-overflow-tooltip
          />
          <el-table-column
            prop="college"
            label="学院"
            min-width="170"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ formatText(row.college) }}
            </template>
          </el-table-column>
          <el-table-column
            prop="major"
            label="专业"
            min-width="150"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ formatText(row.major) }}
            </template>
          </el-table-column>
          <el-table-column prop="grade" label="年级" width="84">
            <template #default="{ row }">
              {{ formatText(row.grade) }}
            </template>
          </el-table-column>
          <el-table-column
            prop="className"
            label="班级"
            width="112"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ formatText(row.className) }}
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="打卡情况" align="center">
          <el-table-column label="打卡" width="74" align="center">
            <template #default="{ row }">
              {{ formatCount(row.checkinDays) }}
            </template>
          </el-table-column>
          <el-table-column label="有效" width="74" align="center">
            <template #default="{ row }">
              {{ formatCount(row.validDays) }}
            </template>
          </el-table-column>
          <el-table-column label="异常" width="74" align="center">
            <template #default="{ row }">
              {{ formatCount(row.abnormalDays) }}
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="睡眠" align="center">
          <el-table-column label="平均(h)" width="86" align="center">
            <template #default="{ row }">
              {{ formatDecimal(row.avgSleepDuration, 1) }}
            </template>
          </el-table-column>
          <el-table-column label="熬夜次数" width="90" align="center">
            <template #default="{ row }">
              {{ formatCount(row.stayUpLateCount) }}
            </template>
          </el-table-column>
          <el-table-column label="熬夜率" width="88" align="center">
            <template #default="{ row }">
              {{ formatPercent(row.lateNightRate) }}
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="运动" align="center">
          <el-table-column label="平均(min)" width="90" align="center">
            <template #default="{ row }">
              {{ formatDecimal(row.avgExerciseDuration, 1) }}
            </template>
          </el-table-column>
          <el-table-column label="不足次数" width="90" align="center">
            <template #default="{ row }">
              {{ formatCount(row.exerciseInsufficientCount) }}
            </template>
          </el-table-column>
          <el-table-column label="达标率" width="88" align="center">
            <template #default="{ row }">
              {{ formatPercent(row.exerciseSufficientRate) }}
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="情绪与饮食" align="center">
          <el-table-column label="心情" width="76" align="center">
            <template #default="{ row }">
              {{ formatDecimal(row.avgMoodScore, 1) }}
            </template>
          </el-table-column>
          <el-table-column label="早餐率" width="88" align="center">
            <template #default="{ row }">
              {{ formatPercent(row.breakfastRate) }}
            </template>
          </el-table-column>
          <el-table-column label="饮水(ml)" width="96" align="center">
            <template #default="{ row }">
              {{ formatCount(row.avgWaterIntake) }}
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="体质与风险" align="center">
          <el-table-column label="BMI" width="76" align="center">
            <template #default="{ row }">
              {{ formatDecimal(row.bmi, 2) }}
            </template>
          </el-table-column>
          <el-table-column label="BMI 等级" width="100" align="center">
            <template #default="{ row }">
              <el-tag
                v-if="row.bmiLevel"
                :type="bmiTagType(row.bmiLevel)"
                effect="plain"
                size="small"
              >
                {{ row.bmiLevel }}
              </el-tag>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="风险等级" width="100" align="center">
            <template #default="{ row }">
              <el-tag
                v-if="row.riskLevel"
                :type="riskTagType(row.riskLevel)"
                effect="light"
                size="small"
              >
                {{ row.riskLevel }}
              </el-tag>
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table-column>

        <template #empty>
          <el-empty
            description="当前筛选条件下暂无台账数据"
            :image-size="88"
          />
        </template>
      </el-table>

      <div class="table-footer">
        <span class="selection-tip">
          校医导出的姓名/学号为脱敏值；超级管理员可导出完整身份信息。
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
  </div>
</template>

<style scoped>
.ledger-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.report-eyebrow {
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

.search-card {
  padding: 18px 20px 4px;
}

.table-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.toolbar-hint {
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.ledger-page :deep(.el-table--small) {
  font-size: 12px;
}

.ledger-page :deep(.el-table__header-wrapper th) {
  font-size: 11.5px;
}

@media (max-width: 768px) {
  .search-card {
    padding: 14px 12px 0;
  }

  .heading-actions {
    width: 100%;
  }

  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
