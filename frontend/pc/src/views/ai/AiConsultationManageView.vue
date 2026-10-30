<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AiConsultationManagePanel from './components/AiConsultationManagePanel.vue'
import AiStatsPanel from './components/AiStatsPanel.vue'
import AiUnansweredPanel from './components/AiUnansweredPanel.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const canManage = computed(() => userStore.hasPermission('ai:consult:manage'))
const canStats = computed(() => userStore.hasPermission('ai:consult:stats'))
const isSuperAdmin = computed(() =>
  userStore.roles.includes('SUPER_ADMIN'),
)

const tabs = computed(() => {
  const list: { name: string; label: string }[] = []
  if (canManage.value) {
    list.push(
      { name: 'records', label: '问答记录' },
      { name: 'unanswered', label: '未命中 / 差评' },
    )
  }
  if (canStats.value) {
    list.push({ name: 'stats', label: '使用统计' })
  }
  return list
})

function queryTab(): string {
  return typeof route.query.tab === 'string' ? route.query.tab : ''
}

function resolveTab(): string {
  const requested = queryTab()
  if (tabs.value.some((item) => item.name === requested)) {
    return requested
  }
  return tabs.value[0]?.name || ''
}

const activeTab = ref(resolveTab())

watch(
  tabs,
  () => {
    if (!tabs.value.some((item) => item.name === activeTab.value)) {
      activeTab.value = resolveTab()
    }
  },
  { immediate: true },
)

watch(
  () => route.query.tab,
  () => {
    const requested = queryTab()
    if (
      tabs.value.some((item) => item.name === requested) &&
      requested !== activeTab.value
    ) {
      activeTab.value = requested
    }
  },
)

function handleTabChange(name: string | number): void {
  router.replace({
    query: { ...route.query, tab: String(name) },
  })
}
</script>

<template>
  <div class="page-container manage-page">
    <div class="page-heading">
      <div>
        <span class="page-eyebrow">AI CONSULTATION MANAGEMENT</span>
        <h2>AI 问答管理</h2>
        <p>
          查看 AI 问答记录、未命中 / 差评问题与使用统计，及时补充健康知识素材。
        </p>
      </div>
      <div class="heading-side">
        <el-tag v-if="isSuperAdmin" type="danger" effect="plain">
          超级管理员
        </el-tag>
        <el-tag v-else type="warning" effect="plain">
          校医科普管理员
        </el-tag>
      </div>
    </div>

    <el-tabs
      v-if="tabs.length"
      v-model="activeTab"
      class="manage-tabs"
      @tab-change="handleTabChange"
    >
      <el-tab-pane
        v-if="canManage"
        label="问答记录"
        name="records"
      >
        <AiConsultationManagePanel />
      </el-tab-pane>

      <el-tab-pane
        v-if="canManage"
        label="未命中 / 差评"
        name="unanswered"
        lazy
      >
        <AiUnansweredPanel />
      </el-tab-pane>

      <el-tab-pane
        v-if="canStats"
        label="使用统计"
        name="stats"
        lazy
      >
        <AiStatsPanel />
      </el-tab-pane>
    </el-tabs>

    <section v-else class="panel-card permission-empty">
      <el-empty
        description="当前账号未开通 AI 问答管理或使用统计权限，请联系管理员。"
        :image-size="100"
      />
    </section>
  </div>
</template>

<style scoped>
.manage-page {
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

.manage-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.manage-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #e3ece8;
}

.manage-tabs :deep(.el-tabs__item) {
  height: 42px;
  font-size: 13.5px;
  font-weight: 500;
}

.manage-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.permission-empty {
  padding: 40px 20px;
}
</style>
