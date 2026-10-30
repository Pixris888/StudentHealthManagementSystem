<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Setting } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import ArticleBrowseView from './components/ArticleBrowseView.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isManager = computed(() =>
  userStore.roles.some((role) =>
    ['DOCTOR_ADMIN', 'SUPER_ADMIN'].includes(role),
  ),
)
const previewMode = computed(() => route.query.preview === '1')

onMounted(() => {
  if (isManager.value && !previewMode.value) {
    router.replace('/health/article/manage/articles')
  }
})

function backToManage(): void {
  router.push('/health/article/manage/articles')
}
</script>

<template>
  <div class="knowledge-entry">
    <div
      v-if="isManager && previewMode"
      class="panel-card preview-bar"
    >
      <div class="preview-text">
        <el-icon><Setting /></el-icon>
        <span>当前为管理员视角的学生端预览，文章列表只展示已发布且审核通过的内容。</span>
      </div>
      <el-button size="small" @click="backToManage">返回管理台</el-button>
    </div>

    <ArticleBrowseView v-if="!isManager || previewMode" />

    <div v-else class="panel-card redirect-card">
      <el-skeleton animated :rows="4" />
      <p>正在进入健康知识库管理台…</p>
    </div>
  </div>
</template>

<style scoped>
.knowledge-entry {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.preview-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 18px;
  background: #f4fbf8;
  border-color: #d8ece4;
}

.preview-text {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--hp-text-secondary);
}

.preview-text .el-icon {
  flex-shrink: 0;
  color: var(--hp-primary);
}

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
