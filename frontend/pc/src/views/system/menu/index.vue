<script setup lang="ts">
import { computed, onMounted, reactive, ref, type Component } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from 'element-plus'
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
  createMenu,
  deleteMenu,
  getMenuDetail,
  getMenuTree,
  updateMenu,
} from '@/api/menu'
import { useUserStore } from '@/stores/user'
import { iconOptions, resolveMenuIcon } from '@/utils/icons'
import {
  MENU_TYPE_OPTIONS,
  STATUS_OPTIONS,
  menuTypeLabel,
  menuTypeTagType,
  statusLabel,
  statusTagType,
} from '@/utils/dict'
import { countMenuTypes } from '@/utils/menu'
import type { MenuForm, MenuTree } from '@/types/system'

interface SelectNode {
  value: number
  label: string
  children?: SelectNode[]
}

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const treeData = ref<MenuTree[]>([])
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const expandAll = ref(false)
const tableKey = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('新增目录')
const formRef = ref<FormInstance>()

function createDefaultForm(): MenuForm {
  return {
    menuName: '',
    parentId: 0,
    menuType: 1,
    orderNum: 1,
    path: '',
    component: '',
    perms: '',
    icon: '',
    status: 1,
    remark: '',
  }
}

const form = reactive<MenuForm>(createDefaultForm())

const totalCount = computed(() => countMenuTypes(treeData.value).total)

/* ---------------- 菜单树加载与过滤 ---------------- */
async function loadMenus(): Promise<void> {
  loading.value = true
  try {
    treeData.value = await getMenuTree()
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

function filterMenus(
  nodes: MenuTree[],
  lowerKeyword: string,
  status?: number,
): MenuTree[] {
  const result: MenuTree[] = []
  for (const node of nodes) {
    const children = filterMenus(node.children ?? [], lowerKeyword, status)
    const textMatched =
      !lowerKeyword ||
      [node.menuName, node.path, node.component, node.perms].some((value) =>
        (value ?? '').toLowerCase().includes(lowerKeyword),
      )
    const statusMatched = status == null || node.status === status
    if ((textMatched && statusMatched) || children.length) {
      result.push({ ...node, children })
    }
  }
  return result
}

const filteredTree = computed(() =>
  filterMenus(
    treeData.value,
    keyword.value.trim().toLowerCase(),
    statusFilter.value,
  ),
)

function handleSearch(): void {
  if (keyword.value.trim() || statusFilter.value != null) {
    expandAll.value = true
    tableKey.value += 1
  }
}

function resetQuery(): void {
  keyword.value = ''
  statusFilter.value = undefined
  expandAll.value = false
  tableKey.value += 1
}

function toggleExpand(): void {
  expandAll.value = !expandAll.value
  tableKey.value += 1
}

/* ---------------- 上级菜单选项 ---------------- */
function findMenu(nodes: MenuTree[], id: number): MenuTree | undefined {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children?.length) {
      const found = findMenu(node.children, id)
      if (found) return found
    }
  }
  return undefined
}

function collectSubtreeIds(node: MenuTree): number[] {
  return [node.id, ...(node.children ?? []).flatMap(collectSubtreeIds)]
}

const excludedParentIds = computed(() => {
  if (!form.id) return new Set<number>()
  const node = findMenu(treeData.value, form.id)
  return new Set<number>(node ? collectSubtreeIds(node) : [])
})

function buildDirectoryOptions(
  nodes: MenuTree[],
  excluded: Set<number>,
): SelectNode[] {
  const result: SelectNode[] = []
  for (const node of nodes) {
    if (excluded.has(node.id)) continue
    const children = buildDirectoryOptions(node.children ?? [], excluded)
    if (node.menuType === 1) {
      result.push({ value: node.id, label: node.menuName, children })
    } else {
      result.push(...children)
    }
  }
  return result
}

function collectMenuOptions(
  nodes: MenuTree[],
  excluded: Set<number>,
): SelectNode[] {
  const result: SelectNode[] = []
  for (const node of nodes) {
    if (excluded.has(node.id)) continue
    if (node.menuType === 2) {
      result.push({ value: node.id, label: node.menuName })
    }
    if (node.children?.length) {
      result.push(...collectMenuOptions(node.children, excluded))
    }
  }
  return result
}

const parentOptions = computed<SelectNode[]>(() => {
  const excluded = excludedParentIds.value
  if (form.menuType === 2) {
    return buildDirectoryOptions(treeData.value, excluded)
  }
  if (form.menuType === 3) {
    return collectMenuOptions(treeData.value, excluded)
  }
  return [
    {
      value: 0,
      label: '顶级目录',
      children: buildDirectoryOptions(treeData.value, excluded),
    },
  ]
})

/* ---------------- 表单规则 ---------------- */
const PERMS_PATTERN = /^[a-z][a-z0-9]*(:[a-z][a-z0-9]*)+$/

const rules = computed<FormRules<MenuForm>>(() => ({
  menuName: [
    { required: true, message: '请输入菜单名称', trigger: 'blur' },
    { max: 50, message: '菜单名称不能超过 50 个字符', trigger: 'blur' },
  ],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  parentId:
    form.menuType === 1
      ? []
      : [{ required: true, message: '请选择上级菜单', trigger: 'change' }],
  path:
    form.menuType === 3
      ? []
      : [
          { required: true, message: '请输入路由地址', trigger: 'blur' },
          { max: 100, message: '路由地址不能超过 100 个字符', trigger: 'blur' },
        ],
  component:
    form.menuType === 3
      ? []
      : [{ max: 100, message: '组件路径不能超过 100 个字符', trigger: 'blur' }],
  perms:
    form.menuType === 3
      ? [
          { required: true, message: '请输入权限标识', trigger: 'blur' },
          {
            pattern: PERMS_PATTERN,
            message: '格式如 system:user:list，只能包含小写字母、数字和冒号',
            trigger: 'blur',
          },
        ]
      : form.menuType === 2
        ? [
            {
              pattern: PERMS_PATTERN,
              message: '格式如 system:user:list，只能包含小写字母、数字和冒号',
              trigger: 'blur',
            },
          ]
        : [],
  orderNum: [{ required: true, message: '请输入显示顺序', trigger: 'change' }],
  icon: [{ max: 50, message: '图标长度不能超过 50 个字符', trigger: 'blur' }],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}))

function resetForm(): void {
  Object.assign(form, createDefaultForm())
  delete form.id
  formRef.value?.clearValidate()
}

function handleMenuTypeChange(): void {
  if (form.menuType === 1) {
    form.parentId = 0
  } else {
    form.parentId = undefined
  }
  formRef.value?.clearValidate(['parentId', 'path', 'component', 'perms'])
}

/* ---------------- 新增 / 编辑 ---------------- */
function openCreateRoot(): void {
  resetForm()
  form.menuType = 1
  form.parentId = 0
  dialogTitle.value = '新增目录'
  dialogVisible.value = true
}

function openAddChild(row: MenuTree): void {
  if (row.menuType !== 1 && row.menuType !== 2) return
  resetForm()
  if (row.menuType === 1) {
    form.menuType = 2
    form.parentId = row.id
    dialogTitle.value = `新增菜单 · ${row.menuName}`
  } else {
    form.menuType = 3
    form.parentId = row.id
    dialogTitle.value = `新增按钮 · ${row.menuName}`
  }
  dialogVisible.value = true
}

async function openEdit(row: MenuTree): Promise<void> {
  try {
    const detail = await getMenuDetail(row.id)
    resetForm()
    Object.assign(form, {
      id: detail.id,
      menuName: detail.menuName,
      parentId: detail.parentId ?? 0,
      menuType: detail.menuType,
      orderNum: detail.orderNum ?? 0,
      path: detail.path ?? '',
      component: detail.component ?? '',
      perms: detail.perms ?? '',
      icon: detail.icon ?? '',
      status: detail.status ?? 1,
      remark: detail.remark ?? '',
    })
    dialogTitle.value = '编辑菜单'
    dialogVisible.value = true
  } catch {
    // 请求错误由拦截器统一提示
  }
}

function treeContainsMenu(nodes: MenuTree[], id: number): boolean {
  return nodes.some(
    (node) =>
      node.id === id || (node.children ? treeContainsMenu(node.children, id) : false),
  )
}

/** 编辑该菜单是否会让当前账号的会话失效（角色引用了该菜单或按钮权限） */
function menuAffectsCurrentUser(id: number, perms?: string | null): boolean {
  if (treeContainsMenu(userStore.menus, id)) return true
  return Boolean(perms && userStore.permissions.includes(perms))
}

async function forceRelogin(message: string): Promise<void> {
  try {
    await ElMessageBox.alert(message, '需要重新登录', {
      type: 'warning',
      confirmButtonText: '重新登录',
      showClose: false,
      closeOnClickModal: false,
      closeOnPressEscape: false,
    })
  } catch {
    // 用户未关闭时继续执行清理
  }
  userStore.reset()
  await router.replace({ path: '/login', query: { redirect: '/system/menu' } })
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const originalMenu = form.id ? findMenu(treeData.value, form.id) : undefined
  const affectsCurrentUser = originalMenu
    ? menuAffectsCurrentUser(originalMenu.id, originalMenu.perms)
    : false

  const payload: MenuForm = {
    ...form,
    parentId: typeof form.parentId === 'number' ? form.parentId : 0,
    path: form.menuType === 3 ? undefined : form.path?.trim() || undefined,
    component:
      form.menuType === 3 ? undefined : form.component?.trim() || undefined,
    perms: form.menuType === 1 ? undefined : form.perms?.trim() || undefined,
    icon: form.icon?.trim() || undefined,
    remark: form.remark?.trim() || undefined,
  }

  submitting.value = true
  try {
    if (form.id) {
      await updateMenu(payload)
      ElMessage.success('菜单修改成功')
      dialogVisible.value = false
      if (affectsCurrentUser) {
        // 后端会刷新引用该菜单的角色权限并清理会话，当前账号需要重新登录
        await forceRelogin(
          '菜单已更新。根据后端权限刷新策略，相关用户的登录状态会失效，当前账号需要重新登录。',
        )
      } else {
        await loadMenus()
      }
    } else {
      await createMenu(payload)
      ElMessage.success('菜单新增成功')
      dialogVisible.value = false
      await loadMenus()
    }
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    submitting.value = false
  }
}

/* ---------------- 删除 ---------------- */
async function handleDelete(row: MenuTree): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除${menuTypeLabel(row.menuType)}“${row.menuName}”吗？删除后不可恢复。`,
      '删除菜单',
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
    await deleteMenu(row.id)
    ElMessage.success('删除成功')
    await loadMenus()
  } catch {
    // 菜单被角色引用或存在子菜单时由后端返回具体原因
  }
}

function rowIcon(row: MenuTree): Component {
  return resolveMenuIcon(row.icon)
}

onMounted(loadMenus)
</script>

<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2>菜单管理</h2>
        <p>维护系统目录、功能菜单和按钮级权限标识，侧边栏会按菜单实时生成</p>
      </div>
      <el-button
        v-permission="'system:menu:add'"
        type="primary"
        :icon="Plus"
        @click="openCreateRoot"
      >
        新增目录
      </el-button>
    </div>

    <!-- 查询条件 -->
    <section class="panel-card search-card">
      <el-form :model="{ keyword, statusFilter }" inline class="search-form">
        <el-form-item label="菜单名称">
          <el-input
            v-model="keyword"
            placeholder="搜索名称 / 路径 / 权限标识"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="statusFilter"
            placeholder="全部状态"
            clearable
            style="width: 140px"
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
          <el-button type="primary" :icon="Search" @click="handleSearch">
            查询
          </el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button :icon="expandAll ? Fold : Expand" @click="toggleExpand">
            {{ expandAll ? '折叠全部' : '展开全部' }}
          </el-button>
        </el-form-item>
      </el-form>
    </section>

    <!-- 菜单树表格 -->
    <section class="panel-card table-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="table-title">菜单结构</span>
          <el-tag type="info" effect="plain">
            共 {{ totalCount }} 个节点
          </el-tag>
        </div>
        <span class="toolbar-hint">
          目录承载菜单，菜单承载按钮；按钮必须设置权限标识
        </span>
      </div>

      <el-table
        :key="tableKey"
        v-loading="loading"
        :data="filteredTree"
        row-key="id"
        :default-expand-all="expandAll"
        :tree-props="{ children: 'children' }"
        style="width: 100%"
      >
        <el-table-column label="菜单名称" min-width="230">
          <template #default="{ row }">
            <div class="menu-name-cell">
              <div class="menu-icon" :class="{ button: row.menuType === 3 }">
                <el-icon :size="15"><component :is="rowIcon(row)" /></el-icon>
              </div>
              <span class="menu-name">{{ row.menuName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="menuTypeTagType(row.menuType)" effect="plain">
              {{ menuTypeLabel(row.menuType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="orderNum"
          label="排序"
          width="76"
          align="center"
        />
        <el-table-column label="路由地址" min-width="120">
          <template #default="{ row }">
            <code v-if="row.path" class="cell-code">{{ row.path }}</code>
            <span v-else class="muted-text">—</span>
          </template>
        </el-table-column>
        <el-table-column label="组件路径" min-width="180">
          <template #default="{ row }">
            <code v-if="row.component" class="cell-code">{{ row.component }}</code>
            <span v-else class="muted-text">—</span>
          </template>
        </el-table-column>
        <el-table-column label="权限标识" min-width="180">
          <template #default="{ row }">
            <code v-if="row.perms" class="perm-code">{{ row.perms }}</code>
            <span v-else class="muted-text">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="86" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                v-if="row.menuType !== 3"
                v-permission="'system:menu:add'"
                link
                type="primary"
                :icon="Plus"
                @click="openAddChild(row)"
              >
                新增下级
              </el-button>
              <el-button
                v-permission="'system:menu:edit'"
                link
                type="primary"
                :icon="Edit"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'system:menu:remove'"
                link
                type="danger"
                :icon="Delete"
                :disabled="Boolean(row.children?.length)"
                :title="row.children?.length ? '请先删除子菜单' : '删除菜单'"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <span class="selection-tip">
          编辑菜单会触发后端权限刷新，当前账号需要重新登录；被角色引用的菜单不能直接删除。
        </span>
      </div>
    </section>

    <!-- 新增 / 编辑菜单 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="680px"
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-alert
        title="菜单层级规则：目录可挂在目录下，菜单必须挂在目录下，按钮必须挂在菜单下。"
        type="info"
        :closable="false"
        show-icon
        class="form-tip"
      />

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="menu-form"
      >
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType" @change="handleMenuTypeChange">
            <el-radio-button
              v-for="item in MENU_TYPE_OPTIONS"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            node-key="value"
            :props="{ label: 'label', children: 'children' }"
            check-strictly
            :render-after-expand="false"
            clearable
            placeholder="请选择上级菜单"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="菜单名称" prop="menuName">
          <el-input
            v-model.trim="form.menuName"
            maxlength="50"
            placeholder="请输入菜单名称"
          />
        </el-form-item>

        <el-form-item label="显示顺序" prop="orderNum">
          <el-input-number
            v-model="form.orderNum"
            :min="0"
            :max="9999"
            controls-position="right"
            style="width: 180px"
          />
        </el-form-item>

        <template v-if="form.menuType !== 3">
          <el-form-item label="路由地址" prop="path">
            <el-input
              v-model.trim="form.path"
              maxlength="100"
              :placeholder="
                form.menuType === 1 ? '如 system' : '如 role，最终地址为 /system/role'
              "
            />
          </el-form-item>

          <el-form-item label="组件路径" prop="component">
            <el-input
              v-model.trim="form.component"
              maxlength="100"
              :placeholder="
                form.menuType === 1
                  ? '目录可留空或填写 Layout'
                  : '如 system/role/index，对应 src/views 下页面'
              "
            />
          </el-form-item>

          <el-form-item label="菜单图标" prop="icon">
            <el-select
              v-model="form.icon"
              clearable
              filterable
              placeholder="请选择菜单图标"
              style="width: 100%"
            >
              <el-option
                v-for="option in iconOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              >
                <div class="icon-option">
                  <el-icon><component :is="option.icon" /></el-icon>
                  <span>{{ option.label }}</span>
                  <code>{{ option.value }}</code>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </template>

        <el-form-item
          v-if="form.menuType !== 1"
          label="权限标识"
          prop="perms"
        >
          <el-input
            v-model.trim="form.perms"
            maxlength="100"
            placeholder="如 system:role:list"
          />
          <div class="form-hint">
            菜单权限用于控制页面访问，按钮权限用于控制页面内操作。
          </div>
        </el-form-item>

        <el-form-item label="菜单状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请输入菜单说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.search-card {
  padding: 18px 20px 4px;
}

.table-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.toolbar-hint {
  font-size: 12px;
  color: var(--hp-text-muted);
}

.menu-name-cell {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.menu-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 26px;
  height: 26px;
  color: var(--hp-primary);
  background: #e9f7f2;
  border-radius: 7px;
}

.menu-icon.button {
  color: #c58a28;
  background: #fff4e2;
}

.menu-name {
  overflow: hidden;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--hp-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-code,
.perm-code {
  padding: 2px 7px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11.5px;
  color: #4f6b62;
  background: #f1f6f4;
  border-radius: 5px;
}

.perm-code {
  color: #7a5fb5;
  background: #f3effc;
}

.table-actions {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 2px;
}

.table-actions :deep(.el-button + .el-button) {
  margin-left: 6px;
}

.form-tip {
  margin-bottom: 20px;
}

.menu-form {
  padding-right: 10px;
}

.form-hint {
  margin-top: 5px;
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--hp-text-muted);
}

.icon-option {
  display: flex;
  align-items: center;
  gap: 9px;
}

.icon-option code {
  margin-left: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 10.5px;
  color: #9bb0a8;
}

@media (max-width: 768px) {
  .search-card {
    padding: 14px 12px 0;
  }

  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .table-footer {
    justify-content: flex-start;
  }
}
</style>
