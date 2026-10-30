<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Edit,
  Lock,
  Plus,
  Refresh,
  Search,
  UserFilled,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { deleteUsers, pageUsers, updateUserStatus } from '@/api/user'
import { useIsMobile } from '@/utils/useIsMobile'
import { listEnabledRoles } from '@/api/role'
import {
  STATUS_OPTIONS,
  USER_TYPE_OPTIONS,
  userTypeLabel,
  userTypeTagType,
} from '@/utils/dict'
import type { Role, User, UserQuery } from '@/types/system'
import UserFormDialog from './components/UserFormDialog.vue'
import UserRoleDialog from './components/UserRoleDialog.vue'
import ResetPasswordDialog from './components/ResetPasswordDialog.vue'

const userStore = useUserStore()
const isMobile = useIsMobile(900)
const currentUserId = computed(() => userStore.userInfo?.userId)

const loading = ref(false)
const list = ref<User[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])
const query = reactive<UserQuery>({
  page: 1,
  size: 10,
  username: '',
  realName: '',
  userType: undefined,
  status: undefined,
  deptName: '',
})

const enabledRoles = ref<Role[]>([])

const formVisible = ref(false)
const editingUser = ref<User | null>(null)
const roleVisible = ref(false)
const roleTarget = ref<User | null>(null)
const resetVisible = ref(false)
const resetTarget = ref<User | null>(null)

async function loadUsers(): Promise<void> {
  loading.value = true
  try {
    const page = await pageUsers({
      page: query.page,
      size: query.size,
      username: query.username?.trim() || undefined,
      realName: query.realName?.trim() || undefined,
      userType: query.userType,
      status: query.status,
      deptName: query.deptName?.trim() || undefined,
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

async function ensureRoles(): Promise<void> {
  if (enabledRoles.value.length) return
  try {
    enabledRoles.value = await listEnabledRoles()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

function handleSearch(): void {
  query.page = 1
  loadUsers()
}

function resetQuery(): void {
  query.page = 1
  query.size = 10
  query.username = ''
  query.realName = ''
  query.userType = undefined
  query.status = undefined
  query.deptName = ''
  loadUsers()
}

function handleSizeChange(): void {
  query.page = 1
  loadUsers()
}

function handleSelectionChange(rows: User[]): void {
  selectedIds.value = rows.map((row) => row.id)
}

function selectableUser(row: User): boolean {
  return row.id !== currentUserId.value
}

function isSelf(row: User): boolean {
  return row.id === currentUserId.value
}

async function openCreate(): Promise<void> {
  await ensureRoles()
  editingUser.value = null
  formVisible.value = true
}

async function openEdit(row: User): Promise<void> {
  await ensureRoles()
  editingUser.value = row
  formVisible.value = true
}

async function openRoles(row: User): Promise<void> {
  if (isSelf(row)) {
    ElMessage.warning('不能修改当前登录账号的角色')
    return
  }
  await ensureRoles()
  roleTarget.value = row
  roleVisible.value = true
}

function openResetPassword(row: User): void {
  if (isSelf(row)) {
    ElMessage.warning('请使用右上角的“修改密码”')
    return
  }
  resetTarget.value = row
  resetVisible.value = true
}

async function handleStatusChange(row: User): Promise<void> {
  const nextStatus = row.status
  try {
    await updateUserStatus(row.id, nextStatus)
    ElMessage.success(
      nextStatus === 1
        ? '账号已启用'
        : '账号已禁用，该用户已被强制下线',
    )
  } catch {
    row.status = nextStatus === 1 ? 0 : 1
  }
}

async function handleDelete(row: User): Promise<void> {
  if (isSelf(row)) return
  try {
    await ElMessageBox.confirm(
      `确定删除用户“${row.realName}（${row.username}）”吗？删除后不可恢复。`,
      '删除用户',
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
    await deleteUsers([row.id])
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.page > 1) {
      query.page -= 1
    }
    await loadUsers()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

async function handleBatchDelete(): Promise<void> {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 个用户吗？删除后不可恢复。`,
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
    const count = selectedIds.value.length
    await deleteUsers(selectedIds.value)
    ElMessage.success('批量删除成功')
    if (list.value.length === count && query.page > 1) {
      query.page -= 1
    }
    await loadUsers()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

function handleSaved(): void {
  loadUsers()
}

function formatLoginTime(value?: string): string {
  if (!value) return '从未登录'
  return value.replace('T', ' ').slice(0, 16)
}

function genderLabel(value?: number): string {
  const labels = ['未知', '男', '女']
  return value == null ? '未知' : (labels[value] ?? '未知')
}

function rolesText(row: User): string {
  return (row.roles ?? []).map((role) => role.roleName).join('、')
}

function avatarText(row: User): string {
  return (row.realName || row.username || '?').charAt(0)
}

onMounted(() => {
  loadUsers()
  ensureRoles()
})
</script>

<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2>用户管理</h2>
        <p>维护全站账号信息、角色分配、账号状态与登录密码</p>
      </div>
      <el-button
        v-permission="'system:user:add'"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        新增用户
      </el-button>
    </div>

    <el-alert
      title="禁用账号、重置密码或调整角色后，目标用户会被强制下线；当前登录账号不能修改自己的状态、角色，也不能删除自己"
      type="info"
      :closable="false"
      show-icon
    />

    <!-- 查询条件 -->
    <section class="panel-card search-card">
      <el-form :model="query" inline class="search-form" @submit.prevent>
        <el-form-item label="用户名">
          <el-input
            v-model="query.username"
            placeholder="请输入用户名"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input
            v-model="query.realName"
            placeholder="请输入姓名"
            clearable
            style="width: 140px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select
            v-model="query.userType"
            placeholder="全部"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in USER_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              v-for="item in STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="院系/部门">
          <el-input
            v-model="query.deptName"
            placeholder="请输入院系或部门"
            clearable
            style="width: 170px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            查询
          </el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <!-- 用户列表 -->
    <section class="panel-card table-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="table-title">用户列表</span>
          <el-tag type="info" effect="plain">共 {{ total }} 个账号</el-tag>
        </div>
        <div class="toolbar-right">
          <el-button
            v-if="selectedIds.length"
            v-permission="'system:user:remove'"
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
          :selectable="selectableUser"
        />
        <el-table-column label="用户" min-width="250">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar
                :size="36"
                :src="row.avatar || undefined"
                class="user-avatar"
              >
                {{ avatarText(row) }}
              </el-avatar>
              <div class="user-meta">
                <div class="user-name-row">
                  <strong>{{ row.realName }}</strong>
                  <el-tag
                    :type="userTypeTagType(row.userType)"
                    size="small"
                    effect="light"
                  >
                    {{ row.userTypeName || userTypeLabel(row.userType) }}
                  </el-tag>
                </div>
                <span class="user-sub">
                  {{ row.username }} · {{ genderLabel(row.gender) }}
                  <template v-if="row.studentNo"> · {{ row.studentNo }}</template>
                </span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="deptName"
          label="院系/部门"
          min-width="130"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.deptName || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <div class="roles-cell">
              <el-tag
                v-for="role in (row.roles ?? []).slice(0, 2)"
                :key="role.id"
                size="small"
                type="info"
                effect="plain"
              >
                {{ role.roleName }}
              </el-tag>
              <el-tooltip
                v-if="(row.roles?.length ?? 0) > 2"
                :content="rolesText(row)"
                placement="top"
              >
                <el-tag size="small" type="info" effect="plain">
                  +{{ (row.roles?.length ?? 0) - 2 }}
                </el-tag>
              </el-tooltip>
              <span v-if="!row.roles?.length" class="muted-text">—</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="
                isSelf(row) || !userStore.hasPermission('system:user:status')
              "
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="150">
          <template #default="{ row }">
            <div class="login-cell">
              <span>{{ formatLoginTime(row.loginTime) }}</span>
              <small v-if="row.loginIp">{{ row.loginIp }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="270"
          :fixed="isMobile ? false : 'right'"
        >
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                v-permission="'system:user:edit'"
                link
                type="primary"
                :icon="Edit"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'system:user:assignRole'"
                link
                type="primary"
                :icon="UserFilled"
                :disabled="isSelf(row)"
                :title="isSelf(row) ? '不能修改自己的角色' : '分配角色'"
                @click="openRoles(row)"
              >
                角色
              </el-button>
              <el-button
                v-permission="'system:user:resetpwd'"
                link
                type="warning"
                :icon="Lock"
                :disabled="isSelf(row)"
                :title="isSelf(row) ? '请使用右上角修改密码' : '重置密码'"
                @click="openResetPassword(row)"
              >
                重置
              </el-button>
              <el-button
                v-permission="'system:user:remove'"
                link
                type="danger"
                :icon="Delete"
                :disabled="isSelf(row)"
                :title="isSelf(row) ? '不能删除当前登录账号' : '删除用户'"
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
          新增用户默认启用；用户名和学号在系统内必须唯一
        </span>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="loadUsers"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <UserFormDialog
      v-model="formVisible"
      :user="editingUser"
      :roles="enabledRoles"
      @saved="handleSaved"
    />
    <UserRoleDialog
      v-model="roleVisible"
      :user="roleTarget"
      :roles="enabledRoles"
      @saved="handleSaved"
    />
    <ResetPasswordDialog v-model="resetVisible" :user="resetTarget" />
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

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.user-avatar {
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #1f9d7a, #3ddc97);
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.user-name-row strong {
  overflow: hidden;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--hp-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-sub {
  overflow: hidden;
  font-size: 11px;
  color: var(--hp-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.roles-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  align-items: center;
}

.login-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--hp-text-secondary);
}

.login-cell small {
  font-size: 10px;
  color: var(--hp-text-muted);
}

.table-actions {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 2px;
}

.table-actions :deep(.el-button + .el-button) {
  margin-left: 5px;
}

.table-footer :deep(.el-pagination) {
  margin-left: auto;
}

@media (max-width: 768px) {
  .search-card {
    padding: 14px 12px 0;
  }

  .table-footer :deep(.el-pagination) {
    margin-left: 0;
  }
}
</style>
