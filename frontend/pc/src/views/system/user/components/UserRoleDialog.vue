<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { assignUserRoles, getUserRoleIds } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { isBuiltInRole, userTypeTagType } from '@/utils/dict'
import type { Role, User } from '@/types/system'

const props = defineProps<{
  user: User | null
  roles: Role[]
}>()

const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ saved: [] }>()

const userStore = useUserStore()
const selectedIds = ref<number[]>([])
const loading = ref(false)
const saving = ref(false)
const errorText = ref('')

const isSelf = computed(() => props.user?.id === userStore.userInfo?.userId)
const availableRoles = computed(() =>
  props.roles.filter(
    (role) => role.userType === props.user?.userType && role.status === 1,
  ),
)
const selectedRoles = computed(() =>
  availableRoles.value.filter((role) => selectedIds.value.includes(role.id)),
)

watch(
  [visible, () => props.user],
  async ([open]) => {
    if (!open || !props.user) return
    errorText.value = ''
    selectedIds.value = [...(props.user.roleIds ?? [])]
    loading.value = true
    try {
      const ids = await getUserRoleIds(props.user.id)
      const availableIds = new Set(availableRoles.value.map((role) => role.id))
      selectedIds.value = ids.filter((id) => availableIds.has(id))
    } catch {
      // 请求失败时保留列表行中的 roleIds
    } finally {
      loading.value = false
    }
  },
)

watch(
  selectedIds,
  () => {
    errorText.value = ''
  },
  { deep: true },
)

function validateSelection(): string {
  if (isSelf.value) return '不能修改自己的角色'
  if (!selectedIds.value.length) return '请至少选择一个角色'
  if (selectedRoles.value.length !== selectedIds.value.length) {
    return '包含无效角色，请刷新页面后重试'
  }
  if (!selectedRoles.value.some((role) => isBuiltInRole(role.roleCode))) {
    return '至少选择一个内置角色（学生 / 校医 / 超级管理员）'
  }
  return ''
}

async function handleSave(): Promise<void> {
  if (!props.user) return
  const message = validateSelection()
  errorText.value = message
  if (message) return

  saving.value = true
  try {
    await assignUserRoles(props.user.id, selectedIds.value)
    ElMessage.success('角色分配成功，该用户需要重新登录')
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
    :title="`分配角色 · ${user?.realName ?? ''}`"
    width="620px"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
  >
    <div v-if="user" class="target-user">
      <el-avatar :size="42" class="target-avatar">
        {{ (user.realName || user.username).charAt(0) }}
      </el-avatar>
      <div class="target-info">
        <strong>{{ user.realName }}</strong>
        <span>{{ user.username }}</span>
      </div>
      <el-tag :type="userTypeTagType(user.userType)" effect="light">
        {{ user.userTypeName || '' }}
      </el-tag>
    </div>

    <el-alert
      v-if="isSelf"
      title="不能修改当前登录账号的角色"
      type="warning"
      :closable="false"
      show-icon
      class="role-tip"
    />
    <el-alert
      v-else
      title="只能分配与用户当前类型一致的角色；保存后该用户在所有设备上的登录会失效"
      type="info"
      :closable="false"
      show-icon
      class="role-tip"
    />

    <div v-loading="loading" class="role-select-wrap">
      <el-checkbox-group v-if="availableRoles.length" v-model="selectedIds">
        <el-checkbox
          v-for="role in availableRoles"
          :key="role.id"
          :value="role.id"
          :disabled="isSelf"
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
      <el-empty
        v-else
        description="没有可分配的角色，请先在角色管理中创建"
        :image-size="70"
      />
    </div>

    <el-alert
      v-if="errorText"
      :title="errorText"
      type="error"
      :closable="false"
      show-icon
      class="role-error"
    />

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="isSelf"
        @click="handleSave"
      >
        保存角色
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.target-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 16px;
  background: #f7fbf9;
  border: 1px solid #e6f0ec;
  border-radius: 12px;
}

.target-avatar {
  flex-shrink: 0;
  font-size: 17px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #1f9d7a, #3ddc97);
}

.target-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.target-info strong {
  font-size: 14px;
  color: var(--hp-text);
}

.target-info span {
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.role-tip {
  margin-bottom: 14px;
  line-height: 1.6;
}

.role-select-wrap {
  max-height: 360px;
  min-height: 120px;
  padding: 4px 2px;
  overflow-y: auto;
}

.role-select-wrap :deep(.el-checkbox-group) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
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

.role-error {
  margin-top: 12px;
}

@media (max-width: 600px) {
  .role-select-wrap :deep(.el-checkbox-group) {
    grid-template-columns: 1fr;
  }
}
</style>
