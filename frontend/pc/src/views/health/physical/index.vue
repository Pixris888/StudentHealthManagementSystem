<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  CircleCheck,
  Edit,
  Notebook,
  Odometer,
  Plus,
  Refresh,
  Search,
  Trophy,
  Upload,
  View,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getPhysicalTestDetail,
  getPhysicalTrend,
  pageAllPhysicalTests,
  pageMyPhysicalTests,
} from '@/api/health-physical'
import {
  bmiLevelOf,
  bmiLevelTagType,
  formatDate,
  formatNumber,
  formatValue,
  gradeLevelTagType,
} from '@/utils/health'
import type { PhysicalTest } from '@/types/health'
import PhysicalFormDialog from './components/PhysicalFormDialog.vue'
import PhysicalImportDialog from './components/PhysicalImportDialog.vue'
import PhysicalDetailDrawer from './components/PhysicalDetailDrawer.vue'
import PhysicalTrendCard from './components/PhysicalTrendCard.vue'

const userStore = useUserStore()
const roles = computed(() => userStore.roles)
const isManageMode = computed(
  () =>
    roles.value.includes('DOCTOR_ADMIN') ||
    roles.value.includes('SUPER_ADMIN'),
)
const isDoctor = computed(
  () =>
    roles.value.includes('DOCTOR_ADMIN') &&
    !roles.value.includes('SUPER_ADMIN'),
)
const isStudentMode = computed(
  () => !isManageMode.value && userStore.userInfo?.userType === 1,
)

/* ---------------- 列表与查询 ---------------- */
const records = ref<PhysicalTest[]>([])
const total = ref(0)
const listLoading = ref(false)
const query = reactive({ page: 1, size: 10 })
const filters = reactive({
  testYear: null as number | null,
  testTerm: '',
  studentNo: '',
})

function buildQuery() {
  return {
    page: query.page,
    size: query.size,
    testYear: filters.testYear ?? undefined,
    testTerm: filters.testTerm.trim() || undefined,
    studentNo: isManageMode.value
      ? filters.studentNo.trim() || undefined
      : undefined,
  }
}

async function loadRecords(): Promise<void> {
  listLoading.value = true
  try {
    const page = isManageMode.value
      ? await pageAllPhysicalTests(buildQuery())
      : await pageMyPhysicalTests(buildQuery())
    records.value = page.records
    total.value = page.total
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    listLoading.value = false
  }
}

/* ---------------- 学生趋势 ---------------- */
const trendData = ref<PhysicalTest[]>([])
const trendLoading = ref(false)

const latestRecord = computed(() => {
  const sorted = [...trendData.value].sort(
    (a, b) => a.testYear - b.testYear || a.id - b.id,
  )
  return sorted.length ? sorted[sorted.length - 1]! : null
})
const latestBmiLevel = computed(() => bmiLevelOf(latestRecord.value?.bmi))

async function loadTrend(): Promise<void> {
  if (!isStudentMode.value) return
  trendLoading.value = true
  try {
    trendData.value = await getPhysicalTrend()
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    trendLoading.value = false
  }
}

async function loadStudentData(): Promise<void> {
  await Promise.all([loadRecords(), loadTrend()])
}

function handleSearch(): void {
  query.page = 1
  loadRecords()
}

function resetFilters(): void {
  filters.testYear = null
  filters.testTerm = ''
  filters.studentNo = ''
  query.page = 1
  loadRecords()
}

function handleSizeChange(): void {
  query.page = 1
  loadRecords()
}

/* ---------------- 新增、编辑、导入、详情 ---------------- */
const formVisible = ref(false)
const editingRecord = ref<PhysicalTest | null>(null)
const importVisible = ref(false)
const detailVisible = ref(false)
const detailRecord = ref<PhysicalTest | null>(null)
const detailLoading = ref(false)

function openAdd(): void {
  editingRecord.value = null
  formVisible.value = true
}

function openEdit(row: PhysicalTest): void {
  editingRecord.value = row
  formVisible.value = true
}

function openStudentDetail(row: PhysicalTest): void {
  detailRecord.value = row
  detailLoading.value = false
  detailVisible.value = true
}

async function openManageDetail(row: PhysicalTest): Promise<void> {
  detailVisible.value = true
  detailRecord.value = null
  detailLoading.value = true
  try {
    detailRecord.value = await getPhysicalTestDetail(row.id)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    detailLoading.value = false
  }
}

function handleChanged(): void {
  loadStudentData()
}

onMounted(() => {
  if (isManageMode.value) {
    loadRecords()
  } else if (isStudentMode.value) {
    loadStudentData()
  }
})
</script>

<template>
  <div class="page-container physical-page">
    <!-- 校医 / 超级管理员 -->
    <template v-if="isManageMode">
      <div class="page-heading">
        <div>
          <h2>体测成绩管理</h2>
          <p>查看学生历年体测成绩、身体指标与体能项目明细</p>
        </div>
        <el-tag type="info" effect="plain">共 {{ total }} 条记录</el-tag>
      </div>

      <el-alert
        v-if="isDoctor"
        title="校医视角下学号已脱敏，体测指标保持完整"
        type="info"
        :closable="false"
        show-icon
      />

      <section class="panel-card search-card">
        <el-form :model="filters" inline class="search-form" @submit.prevent>
          <el-form-item label="学号">
            <el-input
              v-model="filters.studentNo"
              placeholder="请输入学号"
              clearable
              style="width: 170px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="测试年份">
            <el-input-number
              v-model="filters.testYear"
              :min="2000"
              :max="2100"
              :precision="0"
              controls-position="right"
              placeholder="如 2025"
              style="width: 150px"
            />
          </el-form-item>
          <el-form-item label="测试学期">
            <el-input
              v-model="filters.testTerm"
              placeholder="如 上学期"
              clearable
              style="width: 150px"
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
            <span class="table-title">体测成绩列表</span>
            <el-tag type="info" effect="plain">按学年倒序</el-tag>
          </div>
        </div>

        <el-table
          v-loading="listLoading"
          :data="records"
          row-key="id"
          style="width: 100%"
        >
          <el-table-column label="学号" min-width="140">
            <template #default="{ row }">
              {{ row.studentNo || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="学年学期" width="150">
            <template #default="{ row }">
              <div class="term-cell">
                <strong>{{ row.testYear }}</strong>
                <span>{{ row.testTerm || '—' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="身高" width="100" align="center">
            <template #default="{ row }">
              {{ formatValue(row.height, 'cm') }}
            </template>
          </el-table-column>
          <el-table-column label="体重" width="100" align="center">
            <template #default="{ row }">
              {{ formatValue(row.weight, 'kg') }}
            </template>
          </el-table-column>
          <el-table-column label="BMI" width="130" align="center">
            <template #default="{ row }">
              <div class="bmi-cell">
                <strong>{{ formatNumber(row.bmi) }}</strong>
                <el-tag
                  v-if="bmiLevelOf(row.bmi)"
                  :type="bmiLevelTagType(bmiLevelOf(row.bmi))"
                  size="small"
                  effect="plain"
                >
                  {{ bmiLevelOf(row.bmi) }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="总分" width="90" align="center">
            <template #default="{ row }">
              {{ formatNumber(row.totalScore) }}
            </template>
          </el-table-column>
          <el-table-column label="等级" width="90" align="center">
            <template #default="{ row }">
              <el-tag
                v-if="row.gradeLevel"
                :type="gradeLevelTagType(row.gradeLevel)"
                size="small"
                effect="light"
              >
                {{ row.gradeLevel }}
              </el-tag>
              <span v-else class="muted-text">—</span>
            </template>
          </el-table-column>
          <el-table-column label="测试日期" width="120">
            <template #default="{ row }">
              {{ formatDate(row.testDate) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openManageDetail(row)">
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <span class="selection-tip">
            体测成绩由学生本人录入或通过 Excel 导入
          </span>
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="loadRecords"
            @size-change="handleSizeChange"
          />
        </div>
      </section>

      <PhysicalDetailDrawer
        v-model="detailVisible"
        :record="detailRecord"
        :loading="detailLoading"
        :desensitized="isDoctor"
      />
    </template>

    <!-- 学生：我的体测成绩 -->
    <template v-else-if="isStudentMode">
      <div class="page-heading">
        <div>
          <h2>我的体测成绩</h2>
          <p>记录每学年体测成绩，查看身体指标与体能项目变化趋势</p>
        </div>
        <div class="heading-actions">
          <el-button :icon="Upload" @click="importVisible = true">
            Excel 导入
          </el-button>
          <el-button type="primary" :icon="Plus" @click="openAdd">
            新增记录
          </el-button>
        </div>
      </div>

      <section class="summary-grid">
        <article class="summary-card panel-card">
          <div class="summary-icon tone-teal">
            <el-icon :size="21"><Trophy /></el-icon>
          </div>
          <div class="summary-body">
            <span>最近总分</span>
            <strong>{{ formatNumber(latestRecord?.totalScore) }}</strong>
            <small>
              {{ latestRecord ? `${latestRecord.testYear} ${latestRecord.testTerm}` : '暂无记录' }}
            </small>
          </div>
        </article>

        <article class="summary-card panel-card">
          <div class="summary-icon tone-blue">
            <el-icon :size="21"><CircleCheck /></el-icon>
          </div>
          <div class="summary-body">
            <span>最近等级</span>
            <div class="summary-tag-row">
              <el-tag
                v-if="latestRecord?.gradeLevel"
                :type="gradeLevelTagType(latestRecord.gradeLevel)"
                effect="light"
              >
                {{ latestRecord.gradeLevel }}
              </el-tag>
              <strong v-else class="summary-empty">—</strong>
            </div>
            <small>体测等级评定</small>
          </div>
        </article>

        <article class="summary-card panel-card">
          <div class="summary-icon tone-orange">
            <el-icon :size="21"><Odometer /></el-icon>
          </div>
          <div class="summary-body">
            <span>最近 BMI</span>
            <div class="summary-tag-row">
              <strong>{{ formatNumber(latestRecord?.bmi) }}</strong>
              <el-tag
                v-if="latestBmiLevel"
                :type="bmiLevelTagType(latestBmiLevel)"
                size="small"
                effect="plain"
              >
                {{ latestBmiLevel }}
              </el-tag>
            </div>
            <small>身高体重指数</small>
          </div>
        </article>

        <article class="summary-card panel-card">
          <div class="summary-icon tone-purple">
            <el-icon :size="21"><Notebook /></el-icon>
          </div>
          <div class="summary-body">
            <span>历史记录</span>
            <strong>{{ trendData.length }}</strong>
            <small>有效体测记录</small>
          </div>
        </article>
      </section>

      <PhysicalTrendCard :data="trendData" :loading="trendLoading" />

      <section class="panel-card table-card">
        <div class="toolbar">
          <div class="toolbar-left">
            <span class="table-title">体测记录</span>
            <el-tag type="info" effect="plain">共 {{ total }} 条</el-tag>
          </div>
          <div class="toolbar-right">
            <el-form :model="filters" inline class="inline-filter" @submit.prevent>
              <el-form-item label="年份">
                <el-input-number
                  v-model="filters.testYear"
                  :min="2000"
                  :max="2100"
                  :precision="0"
                  controls-position="right"
                  placeholder="全部"
                  style="width: 130px"
                />
              </el-form-item>
              <el-form-item label="学期">
                <el-input
                  v-model="filters.testTerm"
                  placeholder="全部"
                  clearable
                  style="width: 120px"
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item class="filter-actions">
                <el-button type="primary" :icon="Search" @click="handleSearch">
                  查询
                </el-button>
                <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <el-table
          v-loading="listLoading"
          :data="records"
          row-key="id"
          style="width: 100%"
        >
          <el-table-column label="学年学期" width="150">
            <template #default="{ row }">
              <div class="term-cell">
                <strong>{{ row.testYear }}</strong>
                <span>{{ row.testTerm || '—' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="身高 / 体重" width="140">
            <template #default="{ row }">
              <div class="height-weight">
                <span>{{ formatValue(row.height, 'cm') }}</span>
                <span>{{ formatValue(row.weight, 'kg') }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="BMI" width="120" align="center">
            <template #default="{ row }">
              <div class="bmi-cell">
                <strong>{{ formatNumber(row.bmi) }}</strong>
                <el-tag
                  v-if="bmiLevelOf(row.bmi)"
                  :type="bmiLevelTagType(bmiLevelOf(row.bmi))"
                  size="small"
                  effect="plain"
                >
                  {{ bmiLevelOf(row.bmi) }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="总分" width="90" align="center">
            <template #default="{ row }">
              {{ formatNumber(row.totalScore) }}
            </template>
          </el-table-column>
          <el-table-column label="等级" width="90" align="center">
            <template #default="{ row }">
              <el-tag
                v-if="row.gradeLevel"
                :type="gradeLevelTagType(row.gradeLevel)"
                size="small"
                effect="light"
              >
                {{ row.gradeLevel }}
              </el-tag>
              <span v-else class="muted-text">—</span>
            </template>
          </el-table-column>
          <el-table-column label="测试日期" width="120">
            <template #default="{ row }">
              {{ formatDate(row.testDate) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                :icon="View"
                @click="openStudentDetail(row)"
              >
                详情
              </el-button>
              <el-button
                link
                type="primary"
                :icon="Edit"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <span class="selection-tip">
            相同学年学期重复新增会提示记录已存在，请直接编辑原记录
          </span>
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="loadRecords"
            @size-change="handleSizeChange"
          />
        </div>
      </section>

      <PhysicalFormDialog
        v-model="formVisible"
        :record="editingRecord"
        @saved="handleChanged"
      />
      <PhysicalImportDialog
        v-model="importVisible"
        @imported="handleChanged"
      />
      <PhysicalDetailDrawer
        v-model="detailVisible"
        :record="detailRecord"
        :desensitized="false"
      />
    </template>

    <el-result
      v-else
      icon="warning"
      title="暂无访问权限"
      sub-title="当前账号角色无法访问体测成绩模块"
    />
  </div>
</template>

<style scoped>
.physical-page {
  gap: 18px;
}

.heading-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.search-card {
  padding: 18px 20px 4px;
}

.table-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.inline-filter {
  display: flex;
  flex-wrap: wrap;
}

.inline-filter :deep(.el-form-item) {
  margin-right: 10px;
  margin-bottom: 0;
}

.inline-filter :deep(.el-form-item__label) {
  color: var(--hp-text-muted);
}

.table-footer :deep(.el-pagination) {
  margin-left: auto;
}

.term-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.3;
}

.term-cell strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--hp-text);
}

.term-cell span {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.height-weight {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.bmi-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.bmi-cell strong {
  font-size: 14px;
  font-weight: 700;
  color: var(--hp-text);
}

/* ---------------- 学生统计卡片 ---------------- */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
}

.summary-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 13px;
}

.tone-teal {
  color: #1f9d7a;
  background: #e3f6ef;
}

.tone-blue {
  color: #3f7ff0;
  background: #eaf1ff;
}

.tone-orange {
  color: #d98b2b;
  background: #fff3e2;
}

.tone-purple {
  color: #8a63e8;
  background: #f1ecff;
}

.summary-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.3;
}

.summary-body > span {
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.summary-body > strong {
  margin: 3px 0 2px;
  font-size: 26px;
  font-weight: 700;
  color: var(--hp-text);
}

.summary-body small {
  overflow: hidden;
  font-size: 10.5px;
  color: var(--hp-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-tag-row {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 34px;
  margin: 3px 0 2px;
}

.summary-tag-row strong {
  font-size: 24px;
  font-weight: 700;
  color: var(--hp-text);
}

.summary-empty {
  margin: 0 !important;
  color: var(--hp-text-muted) !important;
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-right {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .search-card {
    padding: 14px 12px 0;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .summary-card {
    padding: 15px 16px;
  }

  .inline-filter :deep(.el-form-item) {
    margin-right: 0;
    margin-bottom: 10px;
  }

  .inline-filter {
    width: 100%;
  }

  .table-footer :deep(.el-pagination) {
    margin-left: 0;
  }
}
</style>
