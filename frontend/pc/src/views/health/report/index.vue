<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const isManager = computed(() =>
  userStore.roles.some((role) =>
    ['DOCTOR_ADMIN', 'SUPER_ADMIN'].includes(role),
  ),
)
const isStudent = computed(() => userStore.roles.includes('STUDENT'))

onMounted(() => {
  if (isManager.value) {
    router.replace('/health/report/dashboard')
  } else if (isStudent.value) {
    router.replace('/health/report/monthly')
  } else {
    router.replace('/home')
  }
})
</script>

<template>
  <div class="panel-card redirect-card">
    <el-skeleton animated :rows="4" />
    <p>正在进入统计报表…</p>
  </div>
</template>

<style scoped>
.redirect-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 42px;
  font-size: 13px;
  color: var(--hp-text-muted);
}

.redirect-card :deep(.el-skeleton) {
  width: min(520px, 100%);
}
</style>
