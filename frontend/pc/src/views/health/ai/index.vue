<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import AiAssistantView from '@/views/ai/AiAssistantView.vue'
import AiConsultationManageView from '@/views/ai/AiConsultationManageView.vue'

const userStore = useUserStore()

/** 后端菜单入口 /ai 按角色渲染学生端或管理端 */
const isManager = computed(() =>
  userStore.roles.some((role) =>
    ['DOCTOR_ADMIN', 'SUPER_ADMIN'].includes(role),
  ),
)

const managerEnabled = computed(
  () =>
    userStore.hasPermission('ai:consult:manage') ||
    userStore.hasPermission('ai:consult:stats'),
)
</script>

<template>
  <AiConsultationManageView v-if="isManager && managerEnabled" />
  <AiAssistantView v-else-if="!isManager" />

  <div v-else class="page-container">
    <section class="panel-card permission-empty">
      <el-result
        icon="warning"
        title="暂无 AI 管理权限"
        sub-title="当前管理账号未开通 AI 问答管理或使用统计权限，请联系超级管理员分配权限后重新登录。"
      />
    </section>
  </div>
</template>

<style scoped>
.permission-empty {
  padding: 40px 20px;
}
</style>
