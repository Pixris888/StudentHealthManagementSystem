<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { resetUserPassword } from '@/api/user'
import { validatePassword } from '@/utils/password'
import type { User } from '@/types/system'

interface ResetPasswordForm {
  newPassword: string
  confirmPassword: string
}

const props = defineProps<{
  user: User | null
}>()

const visible = defineModel<boolean>({ required: true })

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive<ResetPasswordForm>({
  newPassword: '',
  confirmPassword: '',
})

watch(visible, (open) => {
  if (open) {
    form.newPassword = ''
    form.confirmPassword = ''
    formRef.value?.clearValidate()
  }
})

function validateNewPassword(
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void,
): void {
  const message = validatePassword(value, [
    props.user?.username,
    props.user?.studentNo,
  ])
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
    callback(new Error('请再次输入新密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules<ResetPasswordForm> = {
  newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
}

async function handleSubmit(): Promise<void> {
  if (!props.user) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await resetUserPassword(props.user.id, form.newPassword)
    ElMessage.success('密码已重置，该用户需要重新登录')
    visible.value = false
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
    title="重置密码"
    width="480px"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
  >
    <div v-if="user" class="target-user">
      <el-avatar :size="38" class="target-avatar">
        {{ (user.realName || user.username).charAt(0) }}
      </el-avatar>
      <div class="target-info">
        <strong>{{ user.realName }}</strong>
        <span>{{ user.username }}</span>
      </div>
    </div>

    <el-alert
      title="密码需为 8-32 位，且包含大写字母、小写字母、数字、特殊字符中的至少三类。重置后该用户在所有设备上的登录会失效。"
      type="info"
      :closable="false"
      show-icon
      class="reset-tip"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="88px"
      @keyup.enter="handleSubmit"
    >
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          autocomplete="new-password"
          placeholder="请输入新密码"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          autocomplete="new-password"
          placeholder="请再次输入新密码"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">
        确认重置
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.target-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 16px;
  background: #f7fbf9;
  border: 1px solid #e6f0ec;
  border-radius: 10px;
}

.target-avatar {
  flex-shrink: 0;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #1f9d7a, #3ddc97);
}

.target-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.target-info strong {
  font-size: 13px;
  color: var(--hp-text);
}

.target-info span {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.reset-tip {
  margin-bottom: 18px;
  line-height: 1.6;
}
</style>
