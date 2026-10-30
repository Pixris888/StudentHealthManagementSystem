<script setup lang="ts">
import { useRouter } from 'vue-router'
import { HomeFilled, Lock } from '@element-plus/icons-vue'

const router = useRouter()
</script>

<template>
  <div class="error-page">
    <div class="error-card">
      <div class="error-icon">
        <el-icon :size="38"><Lock /></el-icon>
      </div>
      <span class="error-code">403</span>
      <h1>没有访问权限</h1>
      <p>当前账号缺少访问该页面所需的权限，请联系管理员分配对应角色或菜单权限。</p>
      <div class="error-actions">
        <el-button type="primary" :icon="HomeFilled" @click="router.replace('/home')">
          返回首页
        </el-button>
        <el-button @click="router.back()">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.error-page {
  display: grid;
  min-height: 100vh;
  padding: 24px;
  place-items: center;
  background:
    radial-gradient(circle at 15% 15%, rgba(61, 220, 151, 0.12), transparent 30%),
    radial-gradient(circle at 85% 85%, rgba(31, 157, 122, 0.1), transparent 32%),
    #f4faf7;
}

.error-card {
  width: min(480px, 100%);
  padding: 42px 36px 34px;
  text-align: center;
  background: #fff;
  border: 1px solid #e4efe9;
  border-radius: 24px;
  box-shadow: 0 24px 70px rgba(15, 74, 64, 0.1);
}

.error-icon {
  display: grid;
  place-items: center;
  width: 76px;
  height: 76px;
  margin: 0 auto 18px;
  color: var(--hp-primary);
  background: #e9f8f2;
  border-radius: 22px;
}

.error-code {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 4px;
  color: #9bb6ac;
}

.error-card h1 {
  margin: 8px 0 12px;
  font-size: 26px;
  font-weight: 700;
  color: var(--hp-text);
}

.error-card p {
  font-size: 13px;
  line-height: 1.8;
  color: var(--hp-text-secondary);
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 26px;
}
</style>
