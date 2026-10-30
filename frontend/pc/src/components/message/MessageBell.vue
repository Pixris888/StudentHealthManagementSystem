<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { useMessageStore } from '@/stores/message'

const router = useRouter()
const route = useRoute()
const messageStore = useMessageStore()

function openInbox(): void {
  void messageStore.refreshUnread()
  if (route.path !== '/health/message') {
    router.push('/health/message')
  }
}
</script>

<template>
  <el-tooltip content="消息中心" placement="bottom">
    <el-badge
      :value="messageStore.unreadCount"
      :max="99"
      :hidden="messageStore.unreadCount === 0"
      class="message-bell-badge"
    >
      <button
        type="button"
        class="message-bell-button"
        aria-label="消息中心"
        @click="openInbox"
      >
        <el-icon :size="18"><Bell /></el-icon>
      </button>
    </el-badge>
  </el-tooltip>
</template>

<style scoped>
.message-bell-badge {
  display: inline-flex;
  align-items: center;
}

.message-bell-button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
  color: #5f7a72;
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 50%;
  transition: color 0.2s ease, background 0.2s ease;
}

.message-bell-button:hover {
  color: var(--hp-primary);
  background: var(--hp-mint);
}

.message-bell-badge :deep(.el-badge__content) {
  top: 4px;
  right: 6px;
  font-size: 10px;
  font-weight: 600;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(224, 91, 91, 0.24);
}
</style>
