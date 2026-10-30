<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Edit,
  Plus,
  Refresh,
  Search,
} from '@element-plus/icons-vue'
import {
  deleteDictData,
  deleteDictTypes,
  pageDictData,
  pageDictTypes,
} from '@/api/dict'
import {
  STATUS_OPTIONS,
  statusLabel,
  statusTagType,
} from '@/utils/dict'
import type {
  SysDictData,
  SysDictType,
  SystemEnableStatus,
} from '@/types/system'
import DictDataFormDialog from './components/DictDataFormDialog.vue'
import DictTypeFormDialog from './components/DictTypeFormDialog.vue'

const typeList = ref<SysDictType[]>([])
const typeTotal = ref(0)
const typeLoading = ref(false)
const typeQuery = reactive({ page: 1, size: 10 })
const typeFilters = reactive<{
  keyword: string
  status: SystemEnableStatus | undefined
}>({
  keyword: '',
  status: undefined,
})
const selectedType = ref<SysDictType | null>(null)

const dataList = ref<SysDictData[]>([])
const dataTotal = ref(0)
const dataLoading = ref(false)
const dataQuery = reactive({ page: 1, size: 10 })
const dataFilters = reactive<{
  keyword: string
  status: SystemEnableStatus | undefined
}>({
  keyword: '',
  status: undefined,
})

const typeDialogVisible = ref(false)
const editingType = ref<SysDictType | null>(null)
const dataDialogVisible = ref(false)
const editingData = ref<SysDictData | null>(null)

const selectedTypeName = computed(() => selectedType.value?.dictName || '字典数据')

async function loadData(): Promise<void> {
  if (!selectedType.value) {
    dataList.value = []
    dataTotal.value = 0
    return
  }
  dataLoading.value = true
  try {
    const page = await pageDictData({
      page: dataQuery.page,
      size: dataQuery.size,
      dictType: selectedType.value.dictType,
      keyword: dataFilters.keyword.trim() || undefined,
      status: dataFilters.status,
    })
    dataList.value = page.records
    dataTotal.value = page.total
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    dataLoading.value = false
  }
}

async function loadTypes(preferredId?: number): Promise<void> {
  typeLoading.value = true
  try {
    const page = await pageDictTypes({
      page: typeQuery.page,
      size: typeQuery.size,
      keyword: typeFilters.keyword.trim() || undefined,
      status: typeFilters.status,
    })
    typeList.value = page.records
    typeTotal.value = page.total

    const previousId = selectedType.value?.id
    let target: SysDictType | undefined
    if (preferredId != null) {
      target = typeList.value.find((item) => item.id === preferredId)
    }
    if (!target && previousId != null) {
      target = typeList.value.find((item) => item.id === previousId)
    }
    if (!target) {
      target = typeList.value[0]
    }

    if (!target) {
      selectedType.value = null
      dataList.value = []
      dataTotal.value = 0
      return
    }

    selectedType.value = target
    if (previousId !== target.id) {
      dataQuery.page = 1
      dataFilters.keyword = ''
      dataFilters.status = undefined
    }
    await loadData()
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    typeLoading.value = false
  }
}

function handleTypeSearch(): void {
  typeQuery.page = 1
  loadTypes()
}

function resetTypeFilters(): void {
  typeFilters.keyword = ''
  typeFilters.status = undefined
  typeQuery.page = 1
  typeQuery.size = 10
  loadTypes()
}

function handleTypePage(page: number): void {
  typeQuery.page = page
  loadTypes()
}

function selectType(type: SysDictType): void {
  if (selectedType.value?.id === type.id) return
  selectedType.value = type
  dataQuery.page = 1
  dataFilters.keyword = ''
  dataFilters.status = undefined
  loadData()
}

function handleDataSearch(): void {
  dataQuery.page = 1
  loadData()
}

function resetDataFilters(): void {
  dataFilters.keyword = ''
  dataFilters.status = undefined
  dataQuery.page = 1
  loadData()
}

function handleDataPage(page: number): void {
  dataQuery.page = page
  loadData()
}

function openCreateType(): void {
  editingType.value = null
  typeDialogVisible.value = true
}

function openEditType(type: SysDictType): void {
  editingType.value = type
  typeDialogVisible.value = true
}

async function handleTypeSaved(): Promise<void> {
  const preferredId = editingType.value?.id
  editingType.value = null
  await loadTypes(preferredId)
}

async function handleDeleteType(type: SysDictType): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除字典类型“${type.dictName}”吗？若该类型下存在字典数据，后端会拒绝删除。`,
      '删除字典类型',
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
    await deleteDictTypes([type.id])
    ElMessage.success('字典类型已删除')
    if (typeList.value.length === 1 && typeQuery.page > 1) {
      typeQuery.page -= 1
    }
    if (selectedType.value?.id === type.id) {
      selectedType.value = null
    }
    await loadTypes()
  } catch {
    // 类型下存在数据等由拦截器统一提示
  }
}

function openCreateData(): void {
  if (!selectedType.value) return
  editingData.value = null
  dataDialogVisible.value = true
}

function openEditData(data: SysDictData): void {
  editingData.value = data
  dataDialogVisible.value = true
}

async function handleDataSaved(): Promise<void> {
  editingData.value = null
  await loadData()
}

async function handleDeleteData(data: SysDictData): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除字典数据“${data.dictLabel}”吗？删除后不可恢复。`,
      '删除字典数据',
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
    await deleteDictData([data.id])
    ElMessage.success('字典数据已删除')
    if (dataList.value.length === 1 && dataQuery.page > 1) {
      dataQuery.page -= 1
    }
    await loadData()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

onMounted(loadTypes)
</script>

<template>
  <div class="page-container dict-page">
    <div class="page-heading">
      <div>
        <span class="page-eyebrow">DICTIONARY MANAGEMENT</span>
        <h2>字典管理</h2>
        <p>
          维护系统字典类型与字典数据；左侧选择类型，右侧维护对应数据，停用的数据不会出现在下拉接口中。
        </p>
      </div>
      <el-tag type="info" effect="plain">
        左侧选择类型 · 右侧维护数据
      </el-tag>
    </div>

    <div class="dict-layout">
      <section class="panel-card type-panel">
        <div class="type-panel-head">
          <div class="panel-title-wrap">
            <span class="panel-title">字典类型</span>
            <el-tag type="info" effect="plain" size="small">
              共 {{ typeTotal }} 个
            </el-tag>
          </div>
          <el-button
            v-permission="'system:dict:add'"
            type="primary"
            size="small"
            :icon="Plus"
            @click="openCreateType"
          >
            新增类型
          </el-button>
        </div>

        <div class="type-filters">
          <el-input
            v-model="typeFilters.keyword"
            placeholder="搜索名称或编码"
            clearable
            size="small"
            maxlength="100"
            @keyup.enter="handleTypeSearch"
            @clear="handleTypeSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <div class="type-filter-row">
            <el-select
              v-model="typeFilters.status"
              placeholder="全部状态"
              clearable
              size="small"
              style="width: 118px"
              @change="handleTypeSearch"
            >
              <el-option
                v-for="item in STATUS_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button
              :icon="Refresh"
              circle
              size="small"
              title="刷新字典类型"
              :loading="typeLoading"
              @click="handleTypeSearch"
            />
            <el-button
              v-if="
                typeFilters.keyword || typeFilters.status !== undefined
              "
              link
              type="primary"
              size="small"
              @click="resetTypeFilters"
            >
              重置
            </el-button>
          </div>
        </div>

        <div v-loading="typeLoading" class="type-list">
          <div
            v-for="item in typeList"
            :key="item.id"
            class="dict-type-item"
            :class="{ active: selectedType?.id === item.id }"
            role="button"
            tabindex="0"
            @click="selectType(item)"
            @keydown.enter="selectType(item)"
          >
            <div class="type-main">
              <span class="type-name">{{ item.dictName }}</span>
              <code class="type-code">{{ item.dictType }}</code>
            </div>
            <div class="type-meta">
              <el-tag
                :type="statusTagType(item.status)"
                effect="light"
                size="small"
              >
                {{ statusLabel(item.status) }}
              </el-tag>
              <span class="type-actions" @click.stop @keydown.stop>
                <el-tooltip content="编辑字典类型" placement="top">
                  <el-button
                    v-permission="'system:dict:edit'"
                    link
                    type="primary"
                    size="small"
                    :icon="Edit"
                    @click="openEditType(item)"
                  />
                </el-tooltip>
                <el-tooltip content="删除字典类型" placement="top">
                  <el-button
                    v-permission="'system:dict:remove'"
                    link
                    type="danger"
                    size="small"
                    :icon="Delete"
                    @click="handleDeleteType(item)"
                  />
                </el-tooltip>
              </span>
            </div>
          </div>

          <el-empty
            v-if="!typeList.length && !typeLoading"
            description="暂无字典类型"
            :image-size="76"
          />
        </div>

        <div v-if="typeTotal > typeQuery.size" class="type-pagination">
          <el-pagination
            :current-page="typeQuery.page"
            :page-size="typeQuery.size"
            :total="typeTotal"
            layout="prev, pager, next"
            small
            background
            @current-change="handleTypePage"
          />
        </div>
      </section>

      <section class="panel-card data-panel">
        <div class="data-panel-head">
          <div class="data-title-wrap">
            <span class="panel-title">字典数据</span>
            <template v-if="selectedType">
              <el-tag type="primary" effect="plain" size="small">
                {{ selectedType.dictName }}
              </el-tag>
              <code class="type-code">{{ selectedType.dictType }}</code>
              <el-tag
                :type="statusTagType(selectedType.status)"
                effect="light"
                size="small"
              >
                {{ statusLabel(selectedType.status) }}
              </el-tag>
            </template>
          </div>
          <el-button
            v-permission="'system:dict:add'"
            type="primary"
            size="small"
            :icon="Plus"
            :disabled="!selectedType"
            @click="openCreateData"
          >
            新增数据
          </el-button>
        </div>

        <template v-if="selectedType">
          <el-form :model="dataFilters" inline class="data-filters">
            <el-form-item label="关键词">
              <el-input
                v-model="dataFilters.keyword"
                placeholder="标签或键值"
                clearable
                size="small"
                maxlength="100"
                style="width: 170px"
                @keyup.enter="handleDataSearch"
                @clear="handleDataSearch"
              />
            </el-form-item>
            <el-form-item label="状态">
              <el-select
                v-model="dataFilters.status"
                placeholder="全部状态"
                clearable
                size="small"
                style="width: 118px"
                @change="handleDataSearch"
              >
                <el-option
                  v-for="item in STATUS_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="small"
                :icon="Search"
                @click="handleDataSearch"
              >
                查询
              </el-button>
              <el-button
                size="small"
                :icon="Refresh"
                @click="resetDataFilters"
              >
                重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table
            v-loading="dataLoading"
            :data="dataList"
            row-key="id"
            size="small"
            style="width: 100%"
          >
            <el-table-column
              prop="dictLabel"
              label="字典标签"
              min-width="132"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <span class="dict-label">{{ row.dictLabel }}</span>
              </template>
            </el-table-column>

            <el-table-column
              prop="dictValue"
              label="字典键值"
              min-width="120"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <code class="dict-value">{{ row.dictValue }}</code>
              </template>
            </el-table-column>

            <el-table-column
              prop="dictSort"
              label="排序"
              width="72"
              align="center"
            />

            <el-table-column label="状态" width="78" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="statusTagType(row.status)"
                  effect="light"
                  size="small"
                >
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
              prop="remark"
              label="备注"
              min-width="140"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <span class="remark-cell">{{ row.remark || '—' }}</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button
                    v-permission="'system:dict:edit'"
                    link
                    type="primary"
                    :icon="Edit"
                    @click="openEditData(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-permission="'system:dict:remove'"
                    link
                    type="danger"
                    :icon="Delete"
                    @click="handleDeleteData(row)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty
                description="该字典类型下暂无数据"
                :image-size="80"
              />
            </template>
          </el-table>

          <div class="data-footer">
            <span class="selection-tip">
              当前类型：{{ selectedTypeName }}，数据共 {{ dataTotal }} 条
            </span>
            <el-pagination
              :current-page="dataQuery.page"
              :page-size="dataQuery.size"
              :total="dataTotal"
              layout="total, prev, pager, next"
              small
              background
              @current-change="handleDataPage"
            />
          </div>
        </template>

        <el-empty
          v-else
          description="请从左侧选择字典类型，再维护对应字典数据"
          :image-size="96"
          class="data-empty"
        />
      </section>
    </div>

    <DictTypeFormDialog
      v-model="typeDialogVisible"
      :dict-type="editingType"
      @saved="handleTypeSaved"
    />

    <DictDataFormDialog
      v-model="dataDialogVisible"
      :dict-type="selectedType?.dictType || ''"
      :data="editingData"
      @saved="handleDataSaved"
    />
  </div>
</template>

<style scoped>
.dict-page {
  gap: 16px;
}

.page-eyebrow {
  display: block;
  margin-bottom: 6px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2.2px;
  color: #9cb6ac;
}

.dict-layout {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  min-width: 0;
}

.type-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  min-height: 520px;
  max-height: calc(100vh - 240px);
  padding: 16px;
}

.type-panel-head,
.data-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.panel-title-wrap,
.data-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.type-filters {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.type-filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.type-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 120px;
  overflow-y: auto;
}

.dict-type-item {
  padding: 10px 11px;
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--hp-border);
  border-radius: 10px;
  transition: all 0.18s ease;
}

.dict-type-item:hover {
  border-color: #c8e6da;
  background: #f8fcfa;
}

.dict-type-item.active {
  background: var(--hp-mint);
  border-color: #a9ddc9;
  box-shadow: inset 3px 0 0 var(--hp-primary);
}

.type-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.type-name {
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  color: var(--hp-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-code {
  display: inline-block;
  overflow: hidden;
  max-width: 100%;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10.5px;
  color: var(--hp-text-muted);
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

.type-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-top: 7px;
}

.type-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.type-actions :deep(.el-button + .el-button) {
  margin-left: 2px;
}

.type-pagination {
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

.data-panel {
  min-width: 0;
  min-height: 520px;
  padding: 16px 18px 10px;
}

.data-filters {
  padding: 12px 0 4px;
  border-bottom: 1px solid #eef4f1;
}

.data-filters :deep(.el-form-item) {
  margin-bottom: 10px;
}

.dict-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--hp-text);
}

.dict-value {
  padding: 2px 6px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11px;
  color: var(--hp-primary-dark);
  background: #f1f8f5;
  border-radius: 5px;
}

.remark-cell {
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.table-actions :deep(.el-button + .el-button) {
  margin-left: 2px;
}

.data-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 2px 6px;
}

.data-empty {
  padding: 60px 0;
}

@media (max-width: 1000px) {
  .dict-layout {
    grid-template-columns: 280px minmax(0, 1fr);
  }
}

@media (max-width: 820px) {
  .dict-layout {
    grid-template-columns: 1fr;
  }

  .type-panel {
    max-height: 360px;
    min-height: 300px;
  }

  .data-panel {
    min-height: 420px;
    padding: 14px 12px 8px;
  }
}
</style>
