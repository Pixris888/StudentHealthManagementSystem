<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Edit,
  Expand,
  Fold,
  Plus,
  Refresh,
  Search,
} from '@element-plus/icons-vue'
import {
  deleteCategory,
  getManageCategoryTree,
} from '@/api/knowledge'
import {
  ENABLE_STATUS_OPTIONS,
  enableStatusTagType,
} from '@/utils/knowledge'
import { formatDateTime } from '@/utils/format'
import type { CategoryVO, EnableStatus } from '@/types/knowledge'
import CategoryEditDialog from '../components/CategoryEditDialog.vue'

const treeData = ref<CategoryVO[]>([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<EnableStatus | undefined>(undefined)
const expandAll = ref(true)
const tableKey = ref(0)

const totalCount = computed(() => {
  const count = (nodes: CategoryVO[]): number =>
    nodes.reduce(
      (sum, node) => sum + 1 + count(node.children ?? []),
      0,
    )
  return count(treeData.value)
})

function filterTree(
  nodes: CategoryVO[],
  lowerKeyword: string,
  status?: EnableStatus,
): CategoryVO[] {
  const result: CategoryVO[] = []
  for (const node of nodes) {
    const children = filterTree(node.children ?? [], lowerKeyword, status)
    const textMatched =
      !lowerKeyword ||
      node.categoryName.toLowerCase().includes(lowerKeyword)
    const statusMatched = status == null || node.status === status
    if ((textMatched && statusMatched) || children.length) {
      result.push({ ...node, children })
    }
  }
  return result
}

const filteredTree = computed(() =>
  filterTree(treeData.value, keyword.value.trim().toLowerCase(), statusFilter.value),
)

async function loadCategories(): Promise<void> {
  loading.value = true
  try {
    treeData.value = await getManageCategoryTree()
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  if (keyword.value.trim() || statusFilter.value != null) {
    expandAll.value = true
    tableKey.value += 1
  }
}

function resetQuery(): void {
  keyword.value = ''
  statusFilter.value = undefined
  expandAll.value = true
  tableKey.value += 1
}

function toggleExpand(): void {
  expandAll.value = !expandAll.value
  tableKey.value += 1
}

/* ---------------- 新增 / 编辑 ---------------- */
const dialogVisible = ref(false)
const editingCategory = ref<CategoryVO | null>(null)
const defaultParentId = ref(0)

function openCreateRoot(): void {
  editingCategory.value = null
  defaultParentId.value = 0
  dialogVisible.value = true
}

function openAddChild(row: CategoryVO): void {
  editingCategory.value = null
  defaultParentId.value = row.id
  dialogVisible.value = true
}

function openEdit(row: CategoryVO): void {
  editingCategory.value = row
  defaultParentId.value = row.parentId ?? 0
  dialogVisible.value = true
}

function handleSaved(): void {
  loadCategories()
}

/* ---------------- 删除 ---------------- */
async function handleDelete(row: CategoryVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除分类“${row.categoryName}”吗？删除后不可恢复。`,
      '删除分类',
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
    await deleteCategory(row.id)
    ElMessage.success('分类已删除')
    loadCategories()
  } catch {
    // 分类下有子分类 / 文章 / 素材时由后端返回具体原因
  }
}

onMounted(loadCategories)
</script>

<template>
  <div class="category-view">
    <section class="panel-card search-card">
      <el-form :model="{ keyword, statusFilter }" inline class="search-form">
        <el-form-item label="分类名称">
          <el-input
            v-model="keyword"
            placeholder="搜索分类名称"
            clearable
            maxlength="50"
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="statusFilter"
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
          <span class="table-title">分类结构</span>
          <el-tag type="info" effect="plain">
            共 {{ totalCount }} 个分类
          </el-tag>
        </div>
        <div class="toolbar-right">
          <el-button :icon="expandAll ? Fold : Expand" @click="toggleExpand">
            {{ expandAll ? '折叠全部' : '展开全部' }}
          </el-button>
          <el-button type="primary" :icon="Plus" @click="openCreateRoot">
            新增顶级分类
          </el-button>
        </div>
      </div>

      <el-alert
        title="停用分类会导致该分类下文章不能新发布；分类下存在子分类、文章或 AI 素材时不能删除。"
        type="info"
        :closable="false"
        show-icon
        class="category-tip"
      />

      <el-table
        :key="tableKey"
        v-loading="loading"
        :data="filteredTree"
        row-key="id"
        :default-expand-all="expandAll"
        :tree-props="{ children: 'children' }"
        style="width: 100%"
      >
        <el-table-column label="分类名称" min-width="240">
          <template #default="{ row }">
            <div class="category-name-cell">
              <span class="category-dot" :class="{ disabled: row.status === 0 }" />
              <span class="category-name">{{ row.categoryName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column
          prop="orderNum"
          label="排序"
          width="82"
          align="center"
        />

        <el-table-column label="状态" width="94" align="center">
          <template #default="{ row }">
            <el-tag :type="enableStatusTagType(row.status)" effect="light">
              {{ row.statusName }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="文章数量" width="104" align="center">
          <template #default="{ row }">
            <span class="count-cell">{{ row.articleCount ?? 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            <span class="time-cell">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="备注" min-width="180">
          <template #default="{ row }">
            <span class="remark-cell">{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="238" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                link
                type="primary"
                :icon="Plus"
                @click="openAddChild(row)"
              >
                新增下级
              </el-button>
              <el-button
                link
                type="primary"
                :icon="Edit"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                :icon="Delete"
                :disabled="Boolean(row.children?.length)"
                :title="
                  row.children?.length ? '请先删除子分类' : '删除分类'
                "
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty
            description="暂无分类，先新增一个顶级分类"
            :image-size="88"
          />
        </template>
      </el-table>

      <div class="table-footer">
        <span class="selection-tip">
          分类名称在同一父级下不能重复；父分类不能选择自己或自己的子分类。
        </span>
      </div>
    </section>

    <CategoryEditDialog
      v-model="dialogVisible"
      :category="editingCategory"
      :parent-id="defaultParentId"
      :tree="treeData"
      @saved="handleSaved"
    />
  </div>
</template>

<style scoped>
.category-view {
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

.category-tip {
  margin-bottom: 16px;
}

.category-name-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.category-dot {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  background: var(--hp-primary);
  border-radius: 50%;
}

.category-dot.disabled {
  background: #b9c9c3;
}

.category-name {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--hp-text);
}

.count-cell {
  font-size: 13px;
  font-weight: 600;
  color: var(--hp-primary-dark);
}

.time-cell {
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.remark-cell {
  display: block;
  overflow: hidden;
  font-size: 12px;
  color: var(--hp-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
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
