<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  Delete,
  Edit,
  Plus,
  Refresh,
  Search,
  VideoPause,
  VideoPlay,
} from '@element-plus/icons-vue'
import {
  auditMaterial,
  changeMaterialStatus,
  deleteMaterials,
  getManageCategoryTree,
  pageMaterials,
} from '@/api/knowledge'
import { useUserStore } from '@/stores/user'
import {
  AUDIT_STATUS_OPTIONS,
  ENABLE_STATUS_OPTIONS,
  auditStatusTagType,
  enableStatusTagType,
} from '@/utils/knowledge'
import { formatDateTime } from '@/utils/format'
import type {
  AuditDTO,
  AuditStatus,
  CategoryVO,
  EnableStatus,
  MaterialVO,
} from '@/types/knowledge'
import MaterialEditDialog from '../components/MaterialEditDialog.vue'
import MaterialPreviewDrawer from '../components/MaterialPreviewDrawer.vue'
import AuditDialog from '../components/AuditDialog.vue'

const userStore = useUserStore()
const canAudit = computed(() =>
  userStore.hasPermission('health:article:audit'),
)

/* ---------------- 列表与查询 ---------------- */
const records = ref<MaterialVO[]>([])
const total = ref(0)
const loading = ref(false)
const categoryTree = ref<CategoryVO[]>([])
const query = reactive({ page: 1, size: 10 })
const filters = reactive<{
  keyword: string
  categoryId: number | undefined
  status: EnableStatus | undefined
  auditStatus: AuditStatus | undefined
}>({
  keyword: '',
  categoryId: undefined,
  status: undefined,
  auditStatus: undefined,
})

async function loadCategories(): Promise<void> {
  try {
    categoryTree.value = await getManageCategoryTree()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

async function loadRecords(): Promise<void> {
  loading.value = true
  try {
    const page = await pageMaterials({
      page: query.page,
      size: query.size,
      keyword: filters.keyword.trim() || undefined,
      categoryId: filters.categoryId,
      status: filters.status,
      auditStatus: filters.auditStatus,
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
  filters.categoryId = undefined
  filters.status = undefined
  filters.auditStatus = undefined
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

/* ---------------- 新增 / 编辑 / 预览 ---------------- */
const editorVisible = ref(false)
const editingId = ref<number | null>(null)

function openCreate(): void {
  editingId.value = null
  editorVisible.value = true
}

function openEdit(row: MaterialVO): void {
  editingId.value = row.id
  editorVisible.value = true
}

function handleSaved(): void {
  loadRecords()
}

const previewVisible = ref(false)
const previewId = ref<number | null>(null)

function openPreview(row: MaterialVO): void {
  previewId.value = row.id
  previewVisible.value = true
}

/* ---------------- 删除 / 审核 / 启停 ---------------- */
async function handleDelete(row: MaterialVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除知识素材“${row.title}”吗？删除后不可恢复。`,
      '删除素材',
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
    await deleteMaterials([row.id])
    ElMessage.success('素材已删除')
    loadRecords()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

async function handleToggleStatus(row: MaterialVO): Promise<void> {
  const target: EnableStatus = row.status === 1 ? 0 : 1
  const action = target === 1 ? '启用' : '停用'
  const tip =
    target === 1
      ? `启用后“${row.title}”将进入 AI 知识召回范围，确定启用吗？`
      : `停用后“${row.title}”将不再被 AI 健康助手召回，确定停用吗？`
  try {
    await ElMessageBox.confirm(tip, `${action}素材`, {
      type: 'warning',
      confirmButtonText: action,
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await changeMaterialStatus(row.id, target)
    ElMessage.success(`素材已${action}`)
    loadRecords()
  } catch {
    // 未审核通过时启用会被后端拒绝，并展示具体原因
  }
}

const auditVisible = ref(false)
const auditTarget = ref<MaterialVO | null>(null)
const auditSubmitting = ref(false)

function openAudit(row: MaterialVO): void {
  auditTarget.value = row
  auditVisible.value = true
}

async function submitAudit(data: AuditDTO): Promise<void> {
  if (!auditTarget.value) return
  auditSubmitting.value = true
  try {
    await auditMaterial(auditTarget.value.id, data)
    ElMessage.success(data.approved ? '审核已通过' : '已退回修改')
    auditVisible.value = false
    loadRecords()
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    auditSubmitting.value = false
  }
}

onMounted(() => {
  loadCategories()
  loadRecords()
})
</script>

<template>
  <div class="manage-view">
    <section class="panel-card search-card">
      <el-form :model="filters" inline class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索标题、内容或关键词"
            clearable
            maxlength="100"
            style="width: 230px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="filters.categoryId"
            :data="categoryTree"
            node-key="id"
            :props="{ label: 'categoryName', children: 'children' }"
            check-strictly
            :render-after-expand="false"
            default-expand-all
            clearable
            placeholder="全部分类"
            style="width: 190px"
            @change="handleSearch"
          />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-select
            v-model="filters.status"
            placeholder="全部状态"
            clearable
            style="width: 140px"
            @change="handleSearch"
          >
            <el-option
              v-for="item in ENABLE_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select
            v-model="filters.auditStatus"
            placeholder="全部状态"
            clearable
            style="width: 150px"
            @change="handleSearch"
          >
            <el-option
              v-for="item in AUDIT_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
          <span class="table-title">AI 知识素材</span>
          <el-tag type="info" effect="plain">共 {{ total }} 条</el-tag>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">
          新增素材
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="records"
        row-key="id"
        style="width: 100%"
      >
        <el-table-column label="素材" min-width="280">
          <template #default="{ row }">
            <div class="material-cell">
              <button
                type="button"
                class="title-link"
                @click="openPreview(row)"
              >
                {{ row.title }}
              </button>
              <span class="material-sub">
                {{ row.keyword ? `关键词：${row.keyword}` : '未设置关键词' }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="分类" width="130">
          <template #default="{ row }">
            <span class="category-cell">{{ row.categoryName || '未分类' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="94" align="center">
          <template #default="{ row }">
            <el-tag :type="enableStatusTagType(row.status)" effect="light">
              {{ row.statusName }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="审核状态" width="118" align="center">
          <template #default="{ row }">
            <el-tag
              :type="auditStatusTagType(row.auditStatus)"
              effect="plain"
            >
              {{ row.auditStatusName }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="来源" min-width="170">
          <template #default="{ row }">
            <span class="source-cell">{{ row.source || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">
            <span class="time-cell">{{ formatDateTime(row.updateTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="264" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                link
                type="primary"
                :icon="Edit"
                @click="openEdit(row)"
              >
                编辑
              </el-button>

              <el-button
                v-if="row.auditStatus === 0 && canAudit"
                link
                type="warning"
                :icon="CircleCheck"
                @click="openAudit(row)"
              >
                审核
              </el-button>

              <el-button
                v-if="row.status === 0 && row.auditStatus === 1"
                link
                type="success"
                :icon="VideoPlay"
                @click="handleToggleStatus(row)"
              >
                启用
              </el-button>

              <el-button
                v-if="row.status === 1"
                link
                type="warning"
                :icon="VideoPause"
                @click="handleToggleStatus(row)"
              >
                停用
              </el-button>

              <el-button
                link
                type="danger"
                :icon="Delete"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty
            description="暂无 AI 知识素材，试试调整筛选条件"
            :image-size="88"
          />
        </template>
      </el-table>

      <div class="table-footer">
        <span class="selection-tip">
          新增或编辑后素材会重新进入待审核；素材必须审核通过后才能启用。
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

    <MaterialEditDialog
      v-model="editorVisible"
      :material-id="editingId"
      :category-tree="categoryTree"
      @saved="handleSaved"
    />

    <MaterialPreviewDrawer
      v-model="previewVisible"
      :material-id="previewId"
    />

    <AuditDialog
      v-model="auditVisible"
      title="素材审核"
      :target-label="auditTarget ? `素材：${auditTarget.title}` : ''"
      :submitting="auditSubmitting"
      @submit="submitAudit"
    />
  </div>
</template>

<style scoped>
.manage-view {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.search-card {
  padding: 18px 20px 4px;
}

.table-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.material-cell {
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

.material-sub {
  overflow: hidden;
  font-size: 11.5px;
  color: var(--hp-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-cell,
.source-cell {
  font-size: 12.5px;
  color: var(--hp-text-secondary);
}

.source-cell {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-cell {
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.table-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
}

.table-actions :deep(.el-button + .el-button) {
  margin-left: 4px;
}

@media (max-width: 768px) {
  .search-card {
    padding: 14px 12px 0;
  }
}
</style>
