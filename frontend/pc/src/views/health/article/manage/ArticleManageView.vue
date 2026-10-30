<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  Delete,
  Download,
  Edit,
  Plus,
  Promotion,
  Refresh,
  Search,
  View,
} from '@element-plus/icons-vue'
import {
  auditArticle,
  deleteArticles,
  getManageCategoryTree,
  offlineArticle,
  pageManageArticles,
  publishArticle,
} from '@/api/knowledge'
import { useUserStore } from '@/stores/user'
import {
  ARTICLE_STATUS_OPTIONS,
  AUDIT_STATUS_OPTIONS,
  articleStatusTagType,
  auditStatusTagType,
} from '@/utils/knowledge'
import { formatDateTime } from '@/utils/format'
import type {
  ArticleStatus,
  ArticleVO,
  AuditDTO,
  AuditStatus,
  CategoryVO,
} from '@/types/knowledge'
import ArticleEditorDialog from '../components/ArticleEditorDialog.vue'
import ArticlePreviewDrawer from '../components/ArticlePreviewDrawer.vue'
import AuditDialog from '../components/AuditDialog.vue'

const userStore = useUserStore()

const canCreate = computed(() => userStore.hasPermission('health:article:add'))
const canEdit = computed(() => userStore.hasPermission('health:article:edit'))
const canRemove = computed(() =>
  userStore.hasPermission('health:article:remove'),
)
const canAudit = computed(() =>
  userStore.hasPermission('health:article:audit'),
)

/* ---------------- 列表与查询 ---------------- */
const records = ref<ArticleVO[]>([])
const total = ref(0)
const loading = ref(false)
const categoryTree = ref<CategoryVO[]>([])
const query = reactive({ page: 1, size: 10 })
const filters = reactive<{
  keyword: string
  categoryId: number | undefined
  status: ArticleStatus | undefined
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
    const page = await pageManageArticles({
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

function openEdit(row: ArticleVO): void {
  editingId.value = row.id
  editorVisible.value = true
}

function handleSaved(): void {
  loadRecords()
}

const previewVisible = ref(false)
const previewId = ref<number | null>(null)

function openPreview(row: ArticleVO): void {
  previewId.value = row.id
  previewVisible.value = true
}

/* ---------------- 发布 / 下架 / 删除 / 审核 ---------------- */
async function handlePublish(row: ArticleVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `发布后《${row.title}》将在学生端可见，确定发布吗？`,
      '发布文章',
      {
        type: 'warning',
        confirmButtonText: '发布',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  try {
    await publishArticle(row.id)
    ElMessage.success('文章已发布')
    loadRecords()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

async function handleOffline(row: ArticleVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `下架后《${row.title}》将对学生隐藏，确定下架吗？`,
      '下架文章',
      {
        type: 'warning',
        confirmButtonText: '下架',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  try {
    await offlineArticle(row.id)
    ElMessage.success('文章已下架，如需修改请编辑后重新送审')
    loadRecords()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

async function handleDelete(row: ArticleVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除文章《${row.title}》吗？删除后不可恢复。`,
      '删除文章',
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
    await deleteArticles([row.id])
    ElMessage.success('文章已删除')
    loadRecords()
  } catch {
    // 已发布文章需先下架，后端会返回具体原因
  }
}

const auditVisible = ref(false)
const auditTarget = ref<ArticleVO | null>(null)
const auditSubmitting = ref(false)

function openAudit(row: ArticleVO): void {
  auditTarget.value = row
  auditVisible.value = true
}

async function submitAudit(data: AuditDTO): Promise<void> {
  if (!auditTarget.value) return
  auditSubmitting.value = true
  try {
    await auditArticle(auditTarget.value.id, data)
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
            placeholder="搜索标题或摘要"
            clearable
            maxlength="100"
            style="width: 220px"
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
        <el-form-item label="文章状态">
          <el-select
            v-model="filters.status"
            placeholder="全部状态"
            clearable
            style="width: 140px"
            @change="handleSearch"
          >
            <el-option
              v-for="item in ARTICLE_STATUS_OPTIONS"
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
          <span class="table-title">文章列表</span>
          <el-tag type="info" effect="plain">共 {{ total }} 篇</el-tag>
        </div>
        <el-button
          v-if="canCreate"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新增文章
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="records"
        row-key="id"
        style="width: 100%"
      >
        <el-table-column label="文章" min-width="300">
          <template #default="{ row }">
            <div class="article-cell">
              <button
                type="button"
                class="title-link"
                @click="openPreview(row)"
              >
                {{ row.title }}
              </button>
              <span class="article-sub">
                {{ row.categoryName || '未分类' }} · {{ row.author || '—' }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="articleStatusTagType(row.status)" effect="light">
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

        <el-table-column
          prop="viewCount"
          label="浏览量"
          width="86"
          align="center"
        />

        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">
            <span class="time-cell">{{ formatDateTime(row.updateTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="286" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                link
                type="primary"
                :icon="View"
                @click="openPreview(row)"
              >
                预览
              </el-button>

              <el-button
                v-if="row.status !== 1 && canEdit"
                link
                type="primary"
                :icon="Edit"
                @click="openEdit(row)"
              >
                编辑
              </el-button>

              <el-button
                v-if="row.auditStatus === 1 && row.status !== 1 && canEdit"
                link
                type="success"
                :icon="Promotion"
                @click="handlePublish(row)"
              >
                发布
              </el-button>

              <el-button
                v-if="row.status === 1 && canEdit"
                link
                type="warning"
                :icon="Download"
                @click="handleOffline(row)"
              >
                下架
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
                v-if="row.status !== 1 && canRemove"
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
          <el-empty description="暂无文章，试试调整筛选条件" :image-size="88" />
        </template>
      </el-table>

      <div class="table-footer">
        <span class="selection-tip">
          已发布文章需先下架才能编辑或删除；文章必须审核通过后才能发布。
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

    <ArticleEditorDialog
      v-model="editorVisible"
      :article-id="editingId"
      :category-tree="categoryTree"
      @saved="handleSaved"
    />

    <ArticlePreviewDrawer
      v-model="previewVisible"
      :article-id="previewId"
    />

    <AuditDialog
      v-model="auditVisible"
      title="文章审核"
      :target-label="auditTarget ? `文章：${auditTarget.title}` : ''"
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

.article-cell {
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

.article-sub {
  overflow: hidden;
  font-size: 11.5px;
  color: var(--hp-text-muted);
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
