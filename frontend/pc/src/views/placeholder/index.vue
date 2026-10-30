<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, Tools } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const title = computed(() => route.meta.title || '功能模块')
</script>

<template>
  <div class="page-container placeholder-page">
    <div class="placeholder-card panel-card">
      <div class="placeholder-decoration">
        <span class="dot dot-one" />
        <span class="dot dot-two" />
        <span class="dot dot-three" />
      </div>

      <div class="placeholder-icon">
        <el-icon :size="40"><Tools /></el-icon>
      </div>

      <span class="placeholder-eyebrow">MODULE IN PROGRESS</span>
      <h2>{{ title }}</h2>
      <p class="placeholder-desc">
        该模块正在开发中。当前版本已经完成登录认证、角色管理与菜单权限管理，
        后续业务页面完成后会自动接入现有菜单和权限体系。
      </p>

      <div class="placeholder-meta">
        <span>当前路由</span>
        <code>{{ route.path }}</code>
      </div>

      <div class="placeholder-actions">
        <el-button type="primary" :icon="HomeFilled" @click="router.push('/home')">
          返回首页
        </el-button>
        <el-button @click="router.back()">返回上一页</el-button>
      </div>

      <div class="placeholder-footer">
        <span>已完成：登录认证</span>
        <span>角色管理</span>
        <span>菜单管理</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.placeholder-page {
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - var(--header-height) - 48px);
}

.placeholder-card {
  position: relative;
  width: min(560px, 100%);
  padding: 46px 44px 34px;
  overflow: hidden;
  text-align: center;
}

.placeholder-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 100% 0%, rgba(61, 220, 151, 0.12), transparent 34%),
    radial-gradient(circle at 0% 100%, rgba(31, 157, 122, 0.08), transparent 32%);
}

.dot {
  position: absolute;
  border-radius: 50%;
  background: #d9f2e8;
}

.dot-one {
  top: 28px;
  right: 42px;
  width: 8px;
  height: 8px;
}

.dot-two {
  top: 52px;
  right: 68px;
  width: 5px;
  height: 5px;
  opacity: 0.7;
}

.dot-three {
  bottom: 42px;
  left: 46px;
  width: 6px;
  height: 6px;
  opacity: 0.6;
}

.placeholder-icon {
  display: grid;
  place-items: center;
  width: 82px;
  height: 82px;
  margin: 0 auto 22px;
  color: var(--hp-primary);
  background: linear-gradient(145deg, #e8f8f2, #d6f0e7);
  border: 1px solid #cbe9de;
  border-radius: 24px;
  box-shadow: 0 16px 32px rgba(31, 157, 122, 0.12);
}

.placeholder-eyebrow {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2.5px;
  color: #9cb6ac;
}

.placeholder-card h2 {
  margin: 10px 0 14px;
  font-size: 26px;
  font-weight: 700;
  color: var(--hp-text);
}

.placeholder-desc {
  max-width: 440px;
  margin: 0 auto;
  font-size: 13px;
  line-height: 1.9;
  color: var(--hp-text-secondary);
}

.placeholder-meta {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px;
  margin: 24px 0;
  font-size: 11.5px;
  color: var(--hp-text-muted);
  background: #f6faf8;
  border: 1px solid #e7f0ec;
  border-radius: 9px;
}

.placeholder-meta code {
  font-family: 'Consolas', 'Monaco', monospace;
  color: var(--hp-primary-dark);
}

.placeholder-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 28px;
}

.placeholder-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding-top: 20px;
  font-size: 11px;
  color: #9cb2a9;
  border-top: 1px dashed #e4eee9;
}

.placeholder-footer span + span::before {
  margin: 0 12px;
  content: '·';
  color: #c8d9d2;
}

@media (max-width: 600px) {
  .placeholder-card {
    padding: 34px 22px 26px;
  }

  .placeholder-card h2 {
    font-size: 22px;
  }

  .placeholder-actions {
    flex-direction: column;
  }

  .placeholder-footer {
    flex-wrap: wrap;
    gap: 6px;
  }

  .placeholder-footer span + span::before {
    margin: 0 6px;
  }
}
</style>
