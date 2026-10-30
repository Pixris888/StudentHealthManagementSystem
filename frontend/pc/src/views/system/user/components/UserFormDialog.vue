<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createUser, updateUser } from '@/api/user'
import { useUserStore } from '@/stores/user'
import {
  USER_TYPE_OPTIONS,
  isBuiltInRole,
  userTypeTagType,
} from '@/utils/dict'
import { validatePassword } from '@/utils/password'
import type { Role, User, UserCreate, UserUpdate } from '@/types/system'

interface UserFormModel {
  userType: number
  username: string
  password: string
  confirmPassword: string
  realName: string
  nickName: string
  gender: number
  studentNo: string
  phone: string
  email: string
  deptName: string
  roleIds: number[]
  remark: string
}

const GENDER_OPTIONS = [
  { value: 0, label: '未知' },
  { value: 1, label: '男' },
  { value: 2, label: '女' },
]

const BUILT_IN_ROLE_BY_TYPE: Record<number, string> = {
  1: 'STUDENT',
  2: 'DOCTOR_ADMIN',
  3: 'SUPER_ADMIN',
}

const props = defineProps<{
  user?: User | null
  roles: Role[]
}>()

const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ saved: [] }>()

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const saving = ref(false)

const isEdit = computed(() => Boolean(props.user?.id))
const dialogTitle = computed(() => (isEdit.value ? '编辑用户' : '新增用户'))
const currentUserType = computed(() => userStore.userInfo?.userType ?? 1)
const userTypeOptions = computed(() =>
  USER_TYPE_OPTIONS.filter((item) => item.value <= currentUserType.value),
)
const currentRoles = computed(() => props.user?.roles ?? [])

function defaultRoleIds(type: number): number[] {
  const roleCode = BUILT_IN_ROLE_BY_TYPE[type]
  const role = props.roles.find(
    (item) => item.roleCode === roleCode && item.userType === type,
  )
  return role ? [role.id] : []
}

function buildForm(user?: User | null): UserFormModel {
  if (user?.id) {
    return {
      userType: user.userType,
      username: user.username,
      password: '',
      confirmPassword: '',
      realName: user.realName,
      nickName: user.nickName ?? '',
      gender: user.gender ?? 0,
      studentNo: user.studentNo ?? '',
      phone: user.phone ?? '',
      email: user.email ?? '',
      deptName: user.deptName ?? '',
      roleIds: [...(user.roleIds ?? [])],
      remark: user.remark ?? '',
    }
  }
  return {
    userType: 1,
    username: '',
    password: '',
    confirmPassword: '',
    realName: '',
    nickName: '',
    gender: 0,
    studentNo: '',
    phone: '',
    email: '',
    deptName: '',
    roleIds: defaultRoleIds(1),
    remark: '',
  }
}

const form = reactive<UserFormModel>(buildForm(props.user))

const availableRoles = computed(() =>
  props.roles.filter(
    (role) => role.userType === form.userType && role.status === 1,
  ),
)

function resetForm(): void {
  Object.assign(form, buildForm(props.user))
  nextTick(() => formRef.value?.clearValidate())
}

watch([visible, () => props.user], ([open]) => {
  if (open) resetForm()
})

function handleUserTypeChange(value: number): void {
  if (isEdit.value) return
  form.roleIds = defaultRoleIds(value)
  if (value !== 1) form.studentNo = ''
  formRef.value?.clearValidate(['roleIds', 'studentNo', 'password'])
}

function validateRoleSelection(
  _rule: unknown,
  _value: unknown,
  callback: (error?: Error) => void,
): void {
  if (!form.roleIds.length) {
    callback(new Error('请至少选择一个角色'))
    return
  }
  const selected = props.roles.filter((role) => form.roleIds.includes(role.id))
  if (selected.length !== form.roleIds.length) {
    callback(new Error('包含无效角色，请刷新页面后重试'))
    return
  }
  if (selected.some((role) => role.userType !== form.userType)) {
    callback(new Error('所选角色与用户类型不一致'))
    return
  }
  if (!selected.some((role) => isBuiltInRole(role.roleCode))) {
    callback(new Error('至少选择一个内置角色（学生 / 校医 / 超级管理员）'))
    return
  }
  callback()
}

function validatePasswordField(
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void,
): void {
  const message = validatePassword(value, [form.username, form.studentNo])
  if (message) {
    callback(new Error(message))
  } else {
    callback()
  }
}

function validateConfirmPassword(
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void,
): void {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = computed<FormRules<UserFormModel>>(() => ({
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
  username: isEdit.value
    ? []
    : [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        {
          pattern: /^[A-Za-z][A-Za-z0-9_]{3,19}$/,
          message: '用户名必须为4-20位，以字母开头，只能包含字母、数字、下划线',
          trigger: 'blur',
        },
      ],
  password: isEdit.value
    ? []
    : [{ validator: validatePasswordField, trigger: 'blur' }],
  confirmPassword: isEdit.value
    ? []
    : [{ validator: validateConfirmPassword, trigger: 'blur' }],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { max: 50, message: '真实姓名不能超过 50 个字符', trigger: 'blur' },
  ],
  nickName: [{ max: 50, message: '昵称不能超过 50 个字符', trigger: 'blur' }],
  studentNo:
    form.userType === 1
      ? [
          { required: true, message: '请输入学号', trigger: 'blur' },
          {
            pattern: /^[A-Za-z0-9]{4,30}$/,
            message: '学号必须为 4-30 位字母或数字',
            trigger: 'blur',
          },
        ]
      : [],
  phone: [
    {
      pattern: /^$|^1[3-9]\d{9}$/,
      message: '手机号格式不正确',
      trigger: 'blur',
    },
  ],
  email: [
    {
      pattern: /^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
      message: '邮箱格式不正确',
      trigger: 'blur',
    },
  ],
  deptName: [
    { max: 100, message: '院系/部门不能超过 100 个字符', trigger: 'blur' },
  ],
  roleIds: isEdit.value
    ? []
    : [{ validator: validateRoleSelection, trigger: 'change' }],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}))

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value && props.user) {
      const payload: UserUpdate = {
        id: props.user.id,
        realName: form.realName.trim(),
        nickName: form.nickName.trim() || undefined,
        gender: form.gender,
        phone: form.phone.trim() || undefined,
        email: form.email.trim() || undefined,
        deptName: form.deptName.trim() || undefined,
        avatar: props.user.avatar || undefined,
        remark: form.remark.trim() || undefined,
      }
      if (form.userType === 1) {
        payload.studentNo = form.studentNo.trim()
      }
      await updateUser(payload)
      ElMessage.success('用户信息已更新')
    } else {
      const payload: UserCreate = {
        username: form.username.trim(),
        password: form.password,
        realName: form.realName.trim(),
        nickName: form.nickName.trim() || undefined,
        userType: form.userType,
        gender: form.gender,
        phone: form.phone.trim() || undefined,
        email: form.email.trim() || undefined,
        deptName: form.deptName.trim() || undefined,
        roleIds: [...form.roleIds],
        remark: form.remark.trim() || undefined,
      }
      if (form.userType === 1) {
        payload.studentNo = form.studentNo.trim()
      }
      await createUser(payload)
      ElMessage.success('用户创建成功')
    }
    visible.value = false
    emit('saved')
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="760px"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-alert
      v-if="isEdit"
      title="用户名和用户类型不可修改；如需调整角色，请关闭弹窗后使用列表中的“角色”操作"
      type="info"
      :closable="false"
      show-icon
      class="form-tip"
    />
    <el-alert
      v-else
      title="新增用户必须分配至少一个角色；同一用户只能分配相同用户类型的角色，且至少包含一个内置角色"
      type="info"
      :closable="false"
      show-icon
      class="form-tip"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="96px"
      class="user-form"
    >
      <div class="form-section">
        <div class="section-head">
          <span>01</span>
          <div>
            <h4>账号信息</h4>
            <p>用户名创建后不可修改，密码需满足复杂度要求</p>
          </div>
        </div>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="用户类型" prop="userType">
              <el-select
                v-model="form.userType"
                :disabled="isEdit"
                placeholder="请选择用户类型"
                style="width: 100%"
                @change="handleUserTypeChange"
              >
                <el-option
                  v-for="item in userTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model.trim="form.username"
                :disabled="isEdit"
                maxlength="20"
                placeholder="4-20位，以字母开头"
              />
            </el-form-item>
          </el-col>
          <el-col v-if="!isEdit" :xs="24" :sm="12">
            <el-form-item label="登录密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="8-32位，至少三类字符"
              />
            </el-form-item>
          </el-col>
          <el-col v-if="!isEdit" :xs="24" :sm="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="请再次输入密码"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input
                v-model.trim="form.realName"
                maxlength="50"
                placeholder="请输入真实姓名"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="昵称" prop="nickName">
              <el-input
                v-model.trim="form.nickName"
                maxlength="50"
                placeholder="不填则默认使用真实姓名"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-head">
          <span>02</span>
          <div>
            <h4>个人信息</h4>
            <p>学生账号必须绑定学号，用于健康档案和体测数据</p>
          </div>
        </div>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio
                  v-for="item in GENDER_OPTIONS"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col v-if="form.userType === 1" :xs="24" :sm="12">
            <el-form-item label="学号" prop="studentNo">
              <el-input
                v-model.trim="form.studentNo"
                maxlength="30"
                placeholder="4-30位字母或数字"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="手机号" prop="phone">
              <el-input
                v-model.trim="form.phone"
                maxlength="20"
                placeholder="请输入手机号"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="邮箱" prop="email">
              <el-input
                v-model.trim="form.email"
                maxlength="100"
                placeholder="请输入邮箱"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="院系/部门" prop="deptName">
              <el-input
                v-model.trim="form.deptName"
                maxlength="100"
                placeholder="请输入院系或部门"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-head">
          <span>03</span>
          <div>
            <h4>角色与备注</h4>
            <p>角色决定账号的权限范围</p>
          </div>
        </div>

        <el-form-item v-if="!isEdit" label="角色" prop="roleIds">
          <div v-if="availableRoles.length" class="role-grid">
            <el-checkbox-group v-model="form.roleIds">
              <el-checkbox
                v-for="role in availableRoles"
                :key="role.id"
                :value="role.id"
                class="role-option"
              >
                <div class="role-card">
                  <div class="role-card-name">
                    <span>{{ role.roleName }}</span>
                    <el-tag
                      v-if="isBuiltInRole(role.roleCode)"
                      size="small"
                      type="success"
                      effect="plain"
                    >
                      内置
                    </el-tag>
                  </div>
                  <code>{{ role.roleCode }}</code>
                </div>
              </el-checkbox>
            </el-checkbox-group>
          </div>
          <el-empty
            v-else
            description="没有可分配的角色，请先在角色管理中创建"
            :image-size="60"
          />
        </el-form-item>

        <el-form-item v-else label="当前角色">
          <div class="current-roles">
            <el-tag
              v-for="role in currentRoles"
              :key="role.id"
              :type="userTypeTagType(role.userType)"
              effect="plain"
            >
              {{ role.roleName }}
            </el-tag>
            <span v-if="!currentRoles.length" class="muted-text">暂无角色</span>
          </div>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="请输入备注"
          />
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-tip {
  margin-bottom: 18px;
  line-height: 1.6;
}

/* 字段较多，限制表单区域高度，保证底部按钮始终可见 */
.user-form {
  max-height: 56vh;
  padding-right: 8px;
  overflow-y: auto;
}

.form-section {
  padding: 2px 0 6px;
}

.form-section + .form-section {
  padding-top: 18px;
  margin-top: 4px;
  border-top: 1px dashed #e8f0ed;
}

.section-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 16px;
}

.section-head > span {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 26px;
  height: 26px;
  font-size: 9.5px;
  font-weight: 700;
  color: var(--hp-primary);
  background: #e8f6f1;
  border-radius: 8px;
}

.section-head h4 {
  margin-bottom: 3px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--hp-text);
}

.section-head p {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.user-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.role-grid {
  width: 100%;
}

.role-grid :deep(.el-checkbox-group) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.role-option {
  width: 100%;
  height: auto;
  padding: 10px 12px;
  margin-right: 0;
  background: #fbfdfc;
  border: 1px solid #e7f0ec;
  border-radius: 10px;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.role-option:hover {
  background: #f5fbf8;
  border-color: #cbe8de;
}

.role-option :deep(.el-checkbox__label) {
  width: 100%;
  padding-left: 8px;
}

.role-card {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.role-card-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--hp-text);
}

.role-card code {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 10.5px;
  color: #8aa198;
}

.current-roles {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

@media (max-width: 600px) {
  .role-grid :deep(.el-checkbox-group) {
    grid-template-columns: 1fr;
  }

  .user-form :deep(.el-form-item__label) {
    width: 86px !important;
  }
}
</style>
