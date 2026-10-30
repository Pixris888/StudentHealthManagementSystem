<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRoute } from 'vue-router'
import { Bell, Clock, Promotion } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'

interface MessageNavItem {
  path: string
  label: string
  desc: string
  icon: Component
}

const route = useRoute()
const userStore = useUserStore()
const messageStore = useMessageStore()

const navItems = computed<MessageNavItem[]>(() => {
  const items: MessageNavItem[] = [
    {
      path: '/health/message',
      label: '收件箱',
      desc: '个人消息与校园公告',
      icon: Bell,
    },
  ]
  const isDoctorOrAdmin = userStore.roles.some((role) =>
    ['DOCTOR_ADMIN', 'SUPER_ADMIN'].includes(role),
  )
  if (isDoctorOrAdmin && userStore.hasPermission('message:send')) {
    items.push({
      path: '/health/message/sent',
      label: '已发送',
      desc: '查看自己发送的消息',
      icon: Promotion,
    })
  }
  if (userStore.roles.includes('SUPER_ADMIN')) {
    items.push({
      path: '/health/message/warning',
      label: '预警任务',
      desc: '手动扫描生成健康预警',
      icon: Clock,
    })
  }
  return items
})
</script>

<template>
  <nav v-if="navItems.length > 1" class="panel-card message-nav">
    <router-link
      v-for="item in navItems"
      :key="item.path"
      :to="item.path"
      class="message-nav-item"
      :class="{ active: route.path === item.path }"
    >
      <span class="nav-icon">
        <el-icon :size="18"><component :is="item.icon" /></el-icon>
      </span>
      <span class="nav-text">
        <strong>{{ item.label }}</strong>
        <small>{{ item.desc }}</small>
      </span>
      <span
        v-if="item.path === '/health/message' && messageStore.unreadCount > 0"
        class="nav-count"
      >
        {{ messageStore.unreadCount > 99 ? '99+' : messageStore.unreadCount }}
      </span>
    </router-link>
  </nav>
</template>

<style scoped>
.message-nav {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 6px;
  padding: 8px;
}

.message-nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 14px;
  color: var(--hp-text-secondary);
  border-radius: 11px;
  transition: all 0.2s ease;
}

.message-nav-item:hover {
  color: var(--hp-primary-dark);
  background: #f4faf7;
}

.message-nav-item.active {
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

.message-nav-item.active .nav-icon {
  color: #fff;
  background: linear-gradient(135deg, #1f9d7a, #3ddc97);
}

.nav-text {
  display: flex;
  flex: 1;
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

.nav-count {
  flex-shrink: 0;
  min-width: 22px;
  padding: 2px 7px;
  font-size: 10.5px;
  font-weight: 700;
  color: #fff;
  text-align: center;
  background: var(--hp-danger);
  border-radius: 999px;
}

@media (max-width: 700px) {
  .message-nav {
    grid-template-columns: 1fr;
  }
}
</style>
