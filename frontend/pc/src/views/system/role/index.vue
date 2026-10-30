<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  ElTree,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import { Delete, Edit, Key, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  assignRoleMenus,
  createRole,
  deleteRoles,
  getRoleDetail,
  pageRoles,
  updateRole,
} from '@/api/role'
import { getMenuTree } from '@/api/menu'
import { useUserStore } from '@/stores/user'
import {
  DATA_SCOPE_OPTIONS,
  STATUS_OPTIONS,
  USER_TYPE_OPTIONS,
  dataScopeLabel,
  isBuiltInRole,
  userTypeLabel,
  userTypeTagType,
} from '@/utils/dict'
import type { MenuTree, Role, RoleForm, RoleQuery } from '@/types/system'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const list = ref<Role[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])

const query = reactive<RoleQuery>({
  page: 1,
  size: 10,
  roleCode: '',
  roleName: '',
  status: undefined,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const editingBuiltIn = ref(false)
const editingOriginalRoleCode = ref('')
const formRef = ref<FormInstance>()

function createDefaultForm(): RoleForm {
  return {
    roleCode: '',
    roleName: '',
    userType: 1,
    roleSort: 1,
    dataScope: 3,
    status: 1,
    remark: '',
  }
}

const form = reactive<RoleForm>(createDefaultForm())

const rules: FormRules<RoleForm> = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    {
      pattern: /^[A-Z][A-Z0-9_]{2,49}$/,
      message: '需以大写字母开头，只能包含大写字母、数字、下划线，长度 3-50',
      trigger: 'blur',
    },
  ],
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 50, message: '角色名称不能超过 50 个字符', trigger: 'blur' },
  ],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
  roleSort: [{ required: true, message: '请输入显示顺序', trigger: 'change' }],
  dataScope: [{ required: true, message: '请选择数据权限范围', trigger: 'change' }],
  status: [{ required: true, message: '请选择角色状态', trigger: 'change' }],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}

/* ---------------- 列表 ---------------- */
async function loadRoles(): Promise<void> {
  loading.value = true
  try {
    const page = await pageRoles({
      page: query.page,
      size: query.size,
      roleCode: query.roleCode?.trim() || undefined,
      roleName: query.roleName?.trim() || undefined,
      status: query.status,
    })
    list.value = page.records
    total.value = page.total
    selectedIds.value = []
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  query.page = 1
  loadRoles()
}

function resetQuery(): void {
  query.page = 1
  query.size = 10
  query.roleCode = ''
  query.roleName = ''
  query.status = undefined
  loadRoles()
}

function handleSizeChange(): void {
  query.page = 1
  loadRoles()
}

function handleSelectionChange(rows: Role[]): void {
  selectedIds.value = rows.map((row) => row.id)
}

function selectableRole(row: Role): boolean {
  return !isBuiltInRole(row.roleCode)
}

/* ---------------- 新增 / 编辑 ---------------- */
function resetForm(): void {
  Object.assign(form, createDefaultForm())
  delete form.id
  delete form.menuIds
  formRef.value?.clearValidate()
}

function openCreate(): void {
  resetForm()
  editingBuiltIn.value = false
  editingOriginalRoleCode.value = ''
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

function openEdit(row: Role): void {
  resetForm()
  Object.assign(form, {
    id: row.id,
    roleCode: row.roleCode,
    roleName: row.roleName,
    userType: row.userType,
    roleSort: row.roleSort,
    dataScope: row.dataScope,
    status: row.status,
    remark: row.remark ?? '',
  })
  editingBuiltIn.value = isBuiltInRole(row.roleCode)
  editingOriginalRoleCode.value = row.roleCode
  dialogTitle.value = '编辑角色'
  dialogVisible.value = true
}

async function forceRelogin(message: string, redirect: string): Promise<void> {
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
  await router.replace({ path: '/login', query: { redirect } })
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: RoleForm = { ...form }
    if (form.id) {
      await updateRole(payload)
      ElMessage.success('角色修改成功')
      dialogVisible.value = false
      if (
        userStore.roles.includes(form.roleCode) ||
        userStore.roles.includes(editingOriginalRoleCode.value)
      ) {
        await forceRelogin(
          '该角色已更新。根据后端权限刷新策略，当前账号需要重新登录以获取最新权限。',
          '/system/role',
        )
        return
      }
      await loadRoles()
    } else {
      await createRole(payload)
      ElMessage.success('角色新增成功')
      dialogVisible.value = false
      await loadRoles()
    }
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    submitting.value = false
  }
}

/* ---------------- 状态切换 ---------------- */
async function handleStatusChange(row: Role): Promise<void> {
  const newStatus = row.status
  try {
    await updateRole({
      id: row.id,
      roleCode: row.roleCode,
      roleName: row.roleName,
      userType: row.userType,
      roleSort: row.roleSort,
      dataScope: row.dataScope,
      status: newStatus,
      remark: row.remark,
    })
    ElMessage.success(newStatus === 1 ? '角色已启用' : '角色已停用')
    if (userStore.roles.includes(row.roleCode)) {
      await forceRelogin(
        '角色状态已变更。根据后端权限刷新策略，当前账号需要重新登录。',
        '/system/role',
      )
      return
    }
    await loadRoles()
  } catch {
    row.status = newStatus === 1 ? 0 : 1
  }
}

/* ---------------- 删除 ---------------- */
async function handleDelete(row: Role): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除角色“${row.roleName}”吗？删除后不可恢复。`,
      '删除角色',
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
    await deleteRoles([row.id])
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.page > 1) {
      query.page -= 1
    }
    await loadRoles()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

async function handleBatchDelete(): Promise<void> {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 个角色吗？删除后不可恢复。`,
      '批量删除',
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
    await deleteRoles(selectedIds.value)
    ElMessage.success('批量删除成功')
    if (list.value.length === selectedIds.value.length && query.page > 1) {
      query.page -= 1
    }
    await loadRoles()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

/* ---------------- 菜单授权 ---------------- */
const authVisible = ref(false)
const authLoading = ref(false)
const authSaving = ref(false)
const authRole = ref<Role | null>(null)
const menuTree = ref<MenuTree[]>([])
const menuTreeRef = ref<InstanceType<typeof ElTree>>()
const checkStrictly = ref(true)
const linkage = computed({
  get: () => !checkStrictly.value,
  set: (value: boolean) => {
    checkStrictly.value = !value
  },
})

const authIsSuperAdmin = computed(() => authRole.value?.roleCode === 'SUPER_ADMIN')

async function openAuth(row: Role): Promise<void> {
  authRole.value = row
  authVisible.value = true
  authLoading.value = true
  checkStrictly.value = true
  menuTree.value = []

  try {
    const [tree, detail] = await Promise.all([getMenuTree(), getRoleDetail(row.id)])
    menuTree.value = tree
    await nextTick()
    menuTreeRef.value?.setCheckedKeys(detail.menuIds ?? [], false)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    authLoading.value = false
  }
}

async function saveAuth(): Promise<void> {
  if (!authRole.value || !menuTreeRef.value) return
  const checked = menuTreeRef.value.getCheckedKeys(false) as Array<string | number>
  const halfChecked = menuTreeRef.value.getHalfCheckedKeys() as Array<string | number>
  const ids = Array.from(new Set([...checked, ...halfChecked].map(Number)))

  if (!ids.length) {
    ElMessage.warning('请至少选择一个菜单或按钮权限')
    return
  }

  authSaving.value = true
  try {
    await assignRoleMenus(authRole.value.id, ids)
    ElMessage.success('菜单权限分配成功')
    const roleCode = authRole.value.roleCode
    authVisible.value = false
    if (userStore.roles.includes(roleCode)) {
      await forceRelogin(
        '角色权限已更新。根据后端权限刷新策略，当前账号需要重新登录以获取最新权限。',
        '/system/role',
      )
      return
    }
    await loadRoles()
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    authSaving.value = false
  }
}

onMounted(loadRoles)
</script>

<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2>角色管理</h2>
        <p>维护系统角色、数据权限范围与菜单授权关系</p>
      </div>
      <el-button
        v-permission="'system:role:add'"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        新增角色
      </el-button>
    </div>

    <!-- 查询条件 -->
    <section class="panel-card search-card">
      <el-form :model="query" inline class="search-form" @submit.prevent>
        <el-form-item label="角色编码">
          <el-input
            v-model="query.roleCode"
            placeholder="请输入角色编码"
            clearable
            style="width: 190px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input
            v-model="query.roleName"
            placeholder="请输入角色名称"
            clearable
            style="width: 190px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
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
        </el-form-item>
      </el-form>
    </section>

    <!-- 角色列表 -->
    <section class="panel-card table-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="table-title">角色列表</span>
          <el-tag type="info" effect="plain">共 {{ total }} 个角色</el-tag>
        </div>
        <div class="toolbar-right">
          <el-button
            v-if="selectedIds.length"
            v-permission="'system:role:remove'"
            type="danger"
            plain
            :icon="Delete"
            @click="handleBatchDelete"
          >
            批量删除（{{ selectedIds.length }}）
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          type="selection"
          width="48"
          :selectable="selectableRole"
        />
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="角色编码" min-width="160">
          <template #default="{ row }">
            <el-tag effect="plain" type="info">{{ row.roleCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="roleName"
          label="角色名称"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column label="用户类型" width="150" align="center">
          <template #default="{ row }">
            <el-tag :type="userTypeTagType(row.userType)" effect="light">
              {{ userTypeLabel(row.userType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="roleSort"
          label="排序"
          width="80"
          align="center"
        />
        <el-table-column label="数据范围" width="120" align="center">
          <template #default="{ row }">
            {{ dataScopeLabel(row.dataScope) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="
                !userStore.hasPermission('system:role:edit') ||
                row.roleCode === 'SUPER_ADMIN'
              "
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                v-permission="'system:role:edit'"
                link
                type="primary"
                :icon="Edit"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'system:role:auth'"
                link
                type="primary"
                :icon="Key"
                @click="openAuth(row)"
              >
                分配权限
              </el-button>
              <el-button
                v-permission="'system:role:remove'"
                link
                type="danger"
                :icon="Delete"
                :disabled="isBuiltInRole(row.roleCode)"
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
          内置角色不可删除；修改角色或分配权限后，角色下的用户需重新登录。
        </span>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="loadRoles"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <!-- 新增 / 编辑角色 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="620px"
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-alert
        v-if="editingBuiltIn"
        title="内置角色的编码和用户类型不允许修改"
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
        class="role-form"
      >
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model.trim="form.roleCode"
            :disabled="editingBuiltIn"
            maxlength="50"
            placeholder="如 HEALTH_COUNSELOR"
          />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input
            v-model.trim="form.roleName"
            maxlength="50"
            placeholder="请输入角色名称"
          />
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select
            v-model="form.userType"
            :disabled="editingBuiltIn"
            style="width: 100%"
          >
            <el-option
              v-for="item in USER_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="显示顺序" prop="roleSort">
          <el-input-number
            v-model="form.roleSort"
            :min="0"
            :max="9999"
            controls-position="right"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option
              v-for="item in DATA_SCOPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="角色状态" prop="status">
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
            placeholder="请输入角色说明"
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

    <!-- 分配菜单权限 -->
    <el-dialog
      v-model="authVisible"
      :title="`分配权限 · ${authRole?.roleName ?? ''}`"
      width="620px"
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-alert
        v-if="authIsSuperAdmin"
        title="超级管理员默认拥有全部菜单和按钮权限，无需手动分配"
        type="info"
        :closable="false"
        show-icon
        class="form-tip"
      />

      <div class="auth-toolbar">
        <div class="auth-hint">
          <el-tag size="small" type="info" effect="plain">
            {{ authRole?.roleCode }}
          </el-tag>
          <span>菜单与按钮可分别授权，保存后立即生效</span>
        </div>
        <el-tooltip
          content="开启后勾选目录会自动勾选全部下级；关闭后可精确控制每个菜单和按钮"
          placement="top"
        >
          <div class="linkage-switch">
            <span>父子联动</span>
            <el-switch v-model="linkage" />
          </div>
        </el-tooltip>
      </div>

      <div
        v-loading="authLoading"
        class="auth-tree-wrap"
        :class="{ disabled: authIsSuperAdmin }"
      >
        <el-tree
          ref="menuTreeRef"
          :data="menuTree"
          node-key="id"
          show-checkbox
          default-expand-all
          :check-strictly="checkStrictly"
          :props="{ label: 'menuName', children: 'children' }"
          :empty-text="authLoading ? '加载中...' : '暂无菜单数据'"
        >
          <template #default="{ data }">
            <div class="menu-node">
              <span class="node-name">{{ data.menuName }}</span>
              <el-tag
                v-if="data.menuType === 3"
                size="small"
                type="warning"
                effect="plain"
              >
                按钮
              </el-tag>
              <code v-if="data.perms" class="node-perm">{{ data.perms }}</code>
            </div>
          </template>
        </el-tree>
      </div>

      <template #footer>
        <el-button @click="authVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="authSaving"
          :disabled="authIsSuperAdmin"
          @click="saveAuth"
        >
          保存权限
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

.table-footer :deep(.el-pagination) {
  margin-left: auto;
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
  margin-bottom: 18px;
}

.role-form {
  padding-right: 10px;
}

.auth-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.auth-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--hp-text-muted);
}

.linkage-switch {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.auth-tree-wrap {
  max-height: 440px;
  padding: 10px;
  overflow: auto;
  background: #fbfdfc;
  border: 1px solid var(--hp-border);
  border-radius: 12px;
}

.auth-tree-wrap.disabled {
  pointer-events: none;
  opacity: 0.75;
}

.auth-tree-wrap :deep(.el-tree) {
  background: transparent;
}

.auth-tree-wrap :deep(.el-tree-node__content) {
  height: 34px;
  border-radius: 7px;
}

.menu-node {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.node-name {
  font-size: 13px;
  color: var(--hp-text);
}

.node-perm {
  padding: 1px 6px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 10.5px;
  color: #7b948b;
  background: #eef5f2;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .search-card {
    padding: 14px 12px 0;
  }

  .auth-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .table-footer {
    justify-content: flex-start;
  }

  .table-footer :deep(.el-pagination) {
    margin-left: 0;
  }
}
</style>
