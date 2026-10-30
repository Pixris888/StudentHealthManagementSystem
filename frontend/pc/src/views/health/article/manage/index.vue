<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CircleCheck,
  Collection,
  Document,
  FolderOpened,
  View,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

interface ManageNavItem {
  path: string
  label: string
  desc: string
  icon: Component
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = computed<ManageNavItem[]>(() => {
  const items: ManageNavItem[] = [
    {
      path: '/health/article/manage/articles',
      label: '文章管理',
      desc: '新增、编辑、发布与下架',
      icon: Document,
    },
    {
      path: '/health/article/manage/categories',
      label: '分类管理',
      desc: '维护文章分类树',
      icon: FolderOpened,
    },
    {
      path: '/health/article/manage/materials',
      label: 'AI 知识素材',
      desc: '维护 AI 问答知识源',
      icon: Collection,
    },
  ]
  if (userStore.hasPermission('health:article:audit')) {
    items.push({
      path: '/health/article/manage/audit',
      label: '内容审核',
      desc: '待审核文章与素材',
      icon: CircleCheck,
    })
  }
  return items
})

function isActive(path: string): boolean {
  return route.path === path
}

function openStudentPreview(): void {
  router.push({ path: '/health/article', query: { preview: '1' } })
}
</script>

<template>
  <div class="manage-page">
    <div class="page-heading">
      <div>
        <span class="manage-eyebrow">KNOWLEDGE MANAGEMENT</span>
        <h2>健康知识库管理</h2>
        <p>维护校园健康科普文章、内容分类与 AI 健康助手知识素材。</p>
      </div>
      <div class="manage-actions">
        <el-tag
          v-if="userStore.roles.includes('SUPER_ADMIN')"
          type="danger"
          effect="plain"
        >
          超级管理员
        </el-tag>
        <el-tag v-else type="warning" effect="plain">校医科普管理员</el-tag>
        <el-button :icon="View" @click="openStudentPreview">
          学生端预览
        </el-button>
      </div>
    </div>

    <nav class="panel-card manage-nav">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="manage-nav-item"
        :class="{ active: isActive(item.path) }"
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

    <router-view v-slot="{ Component }">
      <transition name="fade-slide" mode="out-in">
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>
  </div>
</template>

<style scoped>
.manage-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.manage-eyebrow {
  display: block;
  margin-bottom: 6px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2.2px;
  color: #9cb6ac;
}

.manage-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.manage-nav {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  padding: 8px;
}

.manage-nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 14px;
  color: var(--hp-text-secondary);
  border-radius: 11px;
  transition: all 0.2s ease;
}

.manage-nav-item:hover {
  color: var(--hp-primary-dark);
  background: #f4faf7;
}

.manage-nav-item.active {
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

.manage-nav-item.active .nav-icon {
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

@media (max-width: 1000px) {
  .manage-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .manage-nav {
    grid-template-columns: 1fr;
  }

  .manage-actions {
    width: 100%;
  }
}
</style>
