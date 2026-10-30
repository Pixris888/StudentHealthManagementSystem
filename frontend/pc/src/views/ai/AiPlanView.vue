<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import AiPlanPanel from './components/AiPlanPanel.vue'

const router = useRouter()
const userStore = useUserStore()

const canPlan = computed(() => userStore.hasPermission('ai:plan:generate'))
const canAsk = computed(() => userStore.hasPermission('ai:consult:ask'))

function backToAssistant(): void {
  router.push('/ai')
}
</script>

<template>
  <div class="page-container plan-page">
    <div class="page-heading">
      <div>
        <span class="page-eyebrow">PERSONAL HEALTH PLAN</span>
        <h2>我的健康计划</h2>
        <p>
          根据健康档案和最近 7 天打卡生成作息、运动或综合计划，采纳后可随时回来查看。
        </p>
      </div>
      <div class="heading-side">
        <el-tag type="success" effect="plain">学生端</el-tag>
        <el-button
          v-if="canAsk"
          :icon="ArrowLeft"
          @click="backToAssistant"
        >
          返回 AI 健康助手
        </el-button>
      </div>
    </div>

    <AiPlanPanel v-if="canPlan" />

    <section v-else class="panel-card permission-empty">
      <el-empty
        description="当前账号未开通健康计划权限，请联系管理员。"
        :image-size="100"
      />
    </section>
  </div>
</template>

<style scoped>
.plan-page {
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

.permission-empty {
  padding: 40px 20px;
}
</style>
