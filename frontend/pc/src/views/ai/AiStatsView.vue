<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Tickets } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import AiStatsPanel from './components/AiStatsPanel.vue'

const router = useRouter()
const userStore = useUserStore()

const canManage = computed(() => userStore.hasPermission('ai:consult:manage'))
const isSuperAdmin = computed(() => userStore.roles.includes('SUPER_ADMIN'))

function goRecords(): void {
  router.push('/ai/manage/consultations')
}
</script>

<template>
  <div class="page-container stats-page">
    <div class="page-heading">
      <div>
        <span class="page-eyebrow">AI USAGE STATISTICS</span>
        <h2>AI 使用统计</h2>
        <p>
          查看最近 7 / 30 / 90 天的 AI 问答使用情况、降级率、满意度与未命中问题。
        </p>
      </div>
      <div class="heading-side">
        <el-tag :type="isSuperAdmin ? 'danger' : 'warning'" effect="plain">
          {{ isSuperAdmin ? '超级管理员' : '校医科普管理员' }}
        </el-tag>
        <el-button
          v-if="canManage"
          :icon="Tickets"
          @click="goRecords"
        >
          查看问答记录
        </el-button>
      </div>
    </div>

    <AiStatsPanel />
  </div>
</template>

<style scoped>
.stats-page {
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

.heading-side {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
