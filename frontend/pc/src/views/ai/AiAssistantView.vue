<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AiChatPanel from './components/AiChatPanel.vue'
import AiPlanPanel from './components/AiPlanPanel.vue'

const route = useRoute()
const userStore = useUserStore()

const canAsk = computed(() => userStore.hasPermission('ai:consult:ask'))
const canPlan = computed(() => userStore.hasPermission('ai:plan:generate'))

const tabs = computed(() => {
  const list: { name: string; label: string }[] = []
  if (canAsk.value) list.push({ name: 'chat', label: '健康问答' })
  if (canPlan.value) list.push({ name: 'plans', label: '我的计划' })
  return list
})

function resolveInitialTab(): string {
  const requested =
    typeof route.query.tab === 'string' ? route.query.tab : ''
  if (tabs.value.some((item) => item.name === requested)) return requested
  return tabs.value[0]?.name || ''
}

const activeTab = ref(resolveInitialTab())

watch(
  () => route.query.tab,
  (value) => {
    if (typeof value !== 'string') return
    if (tabs.value.some((item) => item.name === value)) {
      activeTab.value = value
    }
  },
)
</script>

<template>
  <div class="page-container assistant-page">
    <div class="page-heading">
      <div>
        <span class="page-eyebrow">AI HEALTH ASSISTANT</span>
        <h2>AI 健康助手</h2>
        <p>
          睡眠、运动、饮食、情绪等健康问题都可以问我；回答仅作健康参考，不能替代医生诊断。
        </p>
      </div>
      <div class="heading-side">
        <el-tag type="success" effect="plain">学生端</el-tag>
        <el-tag type="info" effect="plain">健康隐私保护</el-tag>
      </div>
    </div>

    <el-tabs
      v-if="tabs.length"
      v-model="activeTab"
      class="assistant-tabs"
    >
      <el-tab-pane
        v-if="canAsk"
        label="健康问答"
        name="chat"
      >
        <AiChatPanel />
      </el-tab-pane>

      <el-tab-pane
        v-if="canPlan"
        label="我的计划"
        name="plans"
        lazy
      >
        <AiPlanPanel />
      </el-tab-pane>
    </el-tabs>

    <section v-else class="panel-card permission-empty">
      <el-empty
        description="当前账号未开通 AI 健康问答或健康计划权限，请联系管理员。"
        :image-size="100"
      />
    </section>
  </div>
</template>

<style scoped>
.assistant-page {
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

.assistant-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.assistant-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #e3ece8;
}

.assistant-tabs :deep(.el-tabs__item) {
  height: 42px;
  font-size: 13.5px;
  font-weight: 500;
}

.assistant-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.permission-empty {
  padding: 40px 20px;
}

@media (max-width: 768px) {
  .assistant-page {
    gap: 12px;
  }

  .assistant-page .page-heading p {
    display: none;
  }

  .assistant-page .heading-side {
    display: none;
  }
}
</style>
