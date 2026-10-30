<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import {
  Delete,
  Edit,
  Lock,
  Plus,
  Refresh,
  Search,
} from '@element-plus/icons-vue'
import {
  createConfig,
  deleteConfigs,
  pageConfigs,
  updateConfig,
} from '@/api/config'
import {
  CONFIG_TYPE_OPTIONS,
  configTypeLabel,
  configTypeTagType,
} from '@/utils/system'
import { formatDateTime } from '@/utils/format'
import type {
  ConfigType,
  SysConfig,
  SysConfigForm,
} from '@/types/system'

const KEY_PATTERN = /^[A-Za-z][A-Za-z0-9._-]{2,99}$/

const records = ref<SysConfig[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })
const filters = reactive<{
  keyword: string
  configType: ConfigType | undefined
}>({
  keyword: '',
  configType: undefined,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增参数')
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingConfig = ref<SysConfig | null>(null)

function createDefaultForm(): SysConfigForm {
  return {
    configName: '',
    configKey: '',
    configValue: '',
    configType: 1,
    remark: '',
  }
}

const form = reactive<SysConfigForm>(createDefaultForm())

const isBuiltIn = computed(
  () => editingConfig.value?.configType === 0,
)

/** 新增参数固定为自定义；编辑内置参数时类型也不可修改 */
const typeDisabled = computed(() => isBuiltIn.value || !form.id)

const rules = computed<FormRules<SysConfigForm>>(() => ({
  configName: [
    { required: true, message: '请输入参数名称', trigger: 'blur' },
    { max: 100, message: '参数名称不能超过 100 个字符', trigger: 'blur' },
  ],
  configKey: [
    { required: true, message: '请输入参数键名', trigger: 'blur' },
    {
      pattern: KEY_PATTERN,
      message:
        '必须以字母开头，只能包含字母、数字、点、下划线、短横线，长度 3-100',
      trigger: 'blur',
    },
  ],
  configType: [
    { required: true, message: '请选择参数类型', trigger: 'change' },
  ],
  configValue: [
    { max: 500, message: '参数值不能超过 500 个字符', trigger: 'blur' },
  ],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}))

async function loadConfigs(): Promise<void> {
  loading.value = true
  try {
    const page = await pageConfigs({
      page: query.page,
      size: query.size,
      keyword: filters.keyword.trim() || undefined,
      configType: filters.configType,
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
  loadConfigs()
}

function resetQuery(): void {
  filters.keyword = ''
  filters.configType = undefined
  query.page = 1
  query.size = 10
  loadConfigs()
}

function handlePageChange(page: number): void {
  query.page = page
  loadConfigs()
}

function handleSizeChange(size: number): void {
  query.size = size
  query.page = 1
  loadConfigs()
}

function resetForm(): void {
  Object.assign(form, createDefaultForm())
  delete form.id
  formRef.value?.clearValidate()
}

function openCreate(): void {
  editingConfig.value = null
  resetForm()
  dialogTitle.value = '新增参数'
  dialogVisible.value = true
}

function openEdit(row: SysConfig): void {
  editingConfig.value = row
  resetForm()
  Object.assign(form, {
    id: row.id,
    configName: row.configName,
    configKey: row.configKey,
    configValue: row.configValue ?? '',
    configType: row.configType,
    remark: row.remark ?? '',
  })
  dialogTitle.value = '编辑参数'
  dialogVisible.value = true
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload: SysConfigForm = {
    configName: form.configName.trim(),
    configKey: form.configKey.trim(),
    configValue: form.configValue?.trim() || '',
    configType: form.configType,
    remark: form.remark?.trim() || undefined,
  }
  if (form.id) payload.id = form.id

  saving.value = true
  try {
    if (form.id) {
      await updateConfig(payload)
      ElMessage.success('参数修改成功')
    } else {
      await createConfig(payload)
      ElMessage.success('参数新增成功')
    }
    dialogVisible.value = false
    await loadConfigs()
  } catch {
    // 键名重复、内置参数保护等由拦截器统一提示
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: SysConfig): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除参数“${row.configName}”吗？删除后不可恢复。`,
      '删除参数',
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
    await deleteConfigs([row.id])
    ElMessage.success('参数已删除')
    if (records.value.length === 1 && query.page > 1) {
      query.page -= 1
    }
    await loadConfigs()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

onMounted(loadConfigs)
</script>

<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <span class="page-eyebrow">SYSTEM PARAMETERS</span>
        <h2>参数设置</h2>
        <p>
          维护系统运行参数与健康预警阈值；系统内置参数不可删除，键名和类型不可修改。
        </p>
      </div>
      <el-button
        v-permission="'system:config:add'"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        新增参数
      </el-button>
    </div>

    <section class="panel-card search-card">
      <el-form :model="filters" inline class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="参数名称 / 键名 / 参数值"
            clearable
            maxlength="100"
            style="width: 250px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="参数类型">
          <el-select
            v-model="filters.configType"
            placeholder="全部类型"
            clearable
            style="width: 150px"
            @change="handleSearch"
          >
            <el-option
              v-for="item in CONFIG_TYPE_OPTIONS"
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
          <span class="table-title">系统参数</span>
          <el-tag type="info" effect="plain">共 {{ total }} 条</el-tag>
          <el-tag type="warning" effect="plain" size="small">
            内置参数受保护
          </el-tag>
        </div>
        <span class="toolbar-hint">
          修改健康预警参数后，将在下一次打卡判定或预警扫描时生效
        </span>
      </div>

      <el-table
        v-loading="loading"
        :data="records"
        row-key="id"
        style="width: 100%"
      >
        <el-table-column label="参数名称" min-width="160">
          <template #default="{ row }">
            <div class="config-name-cell">
              <span class="config-name" :title="row.configName">
                {{ row.configName }}
              </span>
              <el-tooltip
                v-if="row.configType === 0"
                content="系统内置参数：不可删除，不可修改键名和类型"
                placement="top"
              >
                <el-icon class="lock-icon"><Lock /></el-icon>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="参数键名" min-width="190">
          <template #default="{ row }">
            <code class="key-code">{{ row.configKey }}</code>
          </template>
        </el-table-column>

        <el-table-column
          prop="configValue"
          label="参数值"
          min-width="150"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="value-cell">{{ row.configValue || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="参数类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              :type="configTypeTagType(row.configType)"
              effect="light"
              size="small"
            >
              {{ configTypeLabel(row.configType) }}
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

        <el-table-column label="更新时间" width="136">
          <template #default="{ row }">
            <span class="time-cell">
              {{ formatDateTime(row.updateTime ?? row.createTime) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column
          label="操作"
          width="150"
          fixed="right"
          align="center"
        >
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                v-permission="'system:config:edit'"
                link
                type="primary"
                :icon="Edit"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-if="row.configType === 1"
                v-permission="'system:config:remove'"
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
            description="暂无系统参数，试试调整筛选条件"
            :image-size="88"
          />
        </template>
      </el-table>

      <div class="table-footer">
        <span class="selection-tip">
          系统内置参数由平台初始化生成；自定义参数可正常增删改。
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="640px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-alert
        v-if="isBuiltIn"
        type="info"
        :closable="false"
        show-icon
        title="系统内置参数"
        description="只能修改参数名称、参数值和备注；参数键名与参数类型不可修改。"
        class="config-alert"
      />

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="96px"
      >
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="参数名称" prop="configName">
              <el-input
                v-model="form.configName"
                placeholder="如：健康预警-熬夜判定时间"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="参数键名" prop="configKey">
              <el-input
                v-model="form.configKey"
                placeholder="如：health.warn.stayuplate.time"
                maxlength="100"
                :disabled="isBuiltIn"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="参数类型" prop="configType">
          <el-radio-group
            v-model="form.configType"
            :disabled="typeDisabled"
          >
            <el-radio-button
              v-for="item in CONFIG_TYPE_OPTIONS"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
          <span class="form-tip">
            新增参数固定为自定义类型；系统内置参数由平台预置。
          </span>
        </el-form-item>

        <el-form-item label="参数值" prop="configValue">
          <el-input
            v-model="form.configValue"
            type="textarea"
            :rows="2"
            placeholder="请输入参数值"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="说明参数用途或取值范围"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button :disabled="saving" @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="submitForm"
        >
          {{ form.id ? '保存修改' : '新增参数' }}
        </el-button>
      </template>
    </el-dialog>
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

.config-name-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.config-name {
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  color: var(--hp-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lock-icon {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--hp-warning);
}

.key-code {
  padding: 2px 6px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11.5px;
  color: var(--hp-primary-dark);
  background: #f1f8f5;
  border-radius: 5px;
  word-break: break-all;
}

.value-cell,
.remark-cell {
  font-size: 12.5px;
  color: var(--hp-text-secondary);
}

.time-cell {
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.config-alert {
  margin-bottom: 16px;
}

.form-tip {
  display: block;
  width: 100%;
  margin-top: 4px;
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--hp-text-muted);
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}
</style>
