<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRoute } from 'vue-router'
import {
  Document,
  Tickets,
  TrendCharts,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

interface ReportNavItem {
  path: string
  label: string
  desc: string
  icon: Component
}

const route = useRoute()
const userStore = useUserStore()

const navItems = computed<ReportNavItem[]>(() => {
  const items: ReportNavItem[] = []
  const isStudent = userStore.roles.includes('STUDENT')
  const isManager = userStore.roles.some((role) =>
    ['DOCTOR_ADMIN', 'SUPER_ADMIN'].includes(role),
  )

  if (isStudent) {
    items.push({
      path: '/health/report/monthly',
      label: '个人报告',
      desc: '月度健康统计与历史报告',
      icon: Document,
    })
  }
  if (isManager) {
    items.push(
      {
        path: '/health/report/dashboard',
        label: '健康看板',
        desc: '学生健康指标与关注名单',
        icon: TrendCharts,
      },
      {
        path: '/health/report/ledger',
        label: '健康台账',
        desc: '按学生查询与导出',
        icon: Tickets,
      },
    )
  }
  return items
})
</script>

<template>
  <nav v-if="navItems.length > 1" class="panel-card report-nav">
    <router-link
      v-for="item in navItems"
      :key="item.path"
      :to="item.path"
      class="report-nav-item"
      :class="{ active: route.path === item.path }"
    >
      <span class="nav-icon">
        <el-icon :size="18"><component :is="item.icon" /></el-icon>
      </span>
      <span class="nav-text">
        <strong>{{ item.label }}</strong>
        <small>{{ item.desc }}</small>
      </span>
    </router-link>
  </nav>
</template>

<style scoped>
.report-nav {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 6px;
  padding: 8px;
}

.report-nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 14px;
  color: var(--hp-text-secondary);
  border-radius: 11px;
  transition: all 0.2s ease;
}

.report-nav-item:hover {
  color: var(--hp-primary-dark);
  background: #f4faf7;
}

.report-nav-item.active {
  color: var(--hp-primary-dark);
  background: var(--hp-mint);
  box-shadow: inset 0 0 0 1px #cfe9df;
}

.nav-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 36px;
  height: 36px;
  color: var(--hp-primary);
  background: #eaf7f2;
  border-radius: 10px;
}

.report-nav-item.active .nav-icon {
  color: #fff;
  background: linear-gradient(135deg, #1f9d7a, #3ddc97);
}

.nav-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.nav-text strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--hp-text);
}

.nav-text small {
  overflow: hidden;
  font-size: 10.5px;
  color: var(--hp-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 700px) {
  .report-nav {
    grid-template-columns: 1fr;
  }
}
</style>
