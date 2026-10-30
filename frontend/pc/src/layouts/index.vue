<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Close,
  Expand,
  Fold,
  HomeFilled,
  Iphone,
  Lock,
  Refresh,
  SwitchButton,
  User,
} from '@element-plus/icons-vue'
import SidebarItem from './components/SidebarItem.vue'
import ChangePasswordDialog from './components/ChangePasswordDialog.vue'
import AppDownloadDialog from '@/components/app/AppDownloadDialog.vue'
import MessageBell from '@/components/message/MessageBell.vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { userTypeTagType } from '@/utils/dict'
import {
  resolveAppDownloadUrl,
  shouldOpenDownloadDirectly,
} from '@/utils/app-download'
import { logoutAllApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const messageStore = useMessageStore()

const isMobile = ref(false)
const mobileOpen = ref(false)
const refreshKey = ref(0)
const refreshing = ref(false)
const passwordVisible = ref(false)
const profileVisible = ref(false)
const downloadVisible = ref(false)

const collapsed = computed(() => appStore.sidebarCollapsed && !isMobile.value)
const activeMenu = computed(
  () => (route.meta.activeMenu as string | undefined) ?? route.path,
)
const displayName = computed(() => userStore.displayName)

const breadcrumbs = computed(() => {
  const items = route.matched
    .filter((item) => item.meta.title)
    .map((item) => ({ title: item.meta.title as string, path: item.path }))
  if (!items.length || items[0]?.path !== '/home') {
    items.unshift({ title: '首页', path: '/home' })
  }
  return items
})

function updateViewport(): void {
  isMobile.value = window.innerWidth <= 900
  if (!isMobile.value) {
    mobileOpen.value = false
  }
}

function toggleSidebar(): void {
  if (isMobile.value) {
    mobileOpen.value = !mobileOpen.value
  } else {
    appStore.toggleSidebar()
  }
}

function handleRefresh(): void {
  refreshing.value = true
  refreshKey.value += 1
  window.setTimeout(() => {
    refreshing.value = false
  }, 450)
}

function handleDownloadApp(): void {
  if (shouldOpenDownloadDirectly()) {
    window.location.href = resolveAppDownloadUrl()
    return
  }
  downloadVisible.value = true
}

async function handleCommand(command: string | number | object): Promise<void> {
  const action = String(command)
  if (action === 'profile') {
    profileVisible.value = true
    return
  }
  if (action === 'password') {
    passwordVisible.value = true
    return
  }
  if (action === 'logout') {
    try {
      await ElMessageBox.confirm('退出后需要重新登录，确定要退出吗？', '退出登录', {
        type: 'warning',
        confirmButtonText: '退出登录',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }
    await userStore.logout()
    messageStore.clear()
    ElMessage.success('已安全退出')
    await router.replace('/login')
    return
  }
  if (action === 'logoutAll') {
    try {
      await ElMessageBox.confirm(
        '将退出当前账号在所有设备上的登录，确定继续吗？',
        '退出全部设备',
        {
          type: 'warning',
          confirmButtonText: '全部退出',
          cancelButtonText: '取消',
        },
      )
    } catch {
      return
    }
    try {
      await logoutAllApi()
    } catch {
      // 即使接口失败也完成本地退出
    }
    userStore.reset()
    messageStore.clear()
    ElMessage.success('已退出全部设备')
    await router.replace('/login')
  }
}

function handlePasswordChanged(): void {
  userStore.reset()
  messageStore.clear()
  router.replace({ path: '/login', query: { redirect: '/home' } })
}

watch(
  () => route.fullPath,
  () => {
    mobileOpen.value = false
  },
)

watch(
  () => userStore.accessToken,
  (token) => {
    if (!token) {
      messageStore.clear()
    }
  },
)

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
  if (userStore.hasPermission('message:list')) {
    messageStore.startPolling()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
  messageStore.stopPolling()
})
</script>

<template>
  <div class="app-layout">
    <div
      v-if="isMobile && mobileOpen"
      class="sidebar-mask"
      @click="mobileOpen = false"
    />

    <aside
      class="app-sidebar"
      :class="{ collapsed, 'mobile-open': mobileOpen }"
    >
      <router-link to="/home" class="sidebar-brand">
        <img src="/health-logo.svg" alt="健康管理平台" />
        <div v-show="!collapsed" class="brand-text">
          <span class="brand-title">健康管理平台</span>
          <span class="brand-sub">HEALTH MANAGEMENT</span>
        </div>
      </router-link>

      <el-scrollbar class="sidebar-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          unique-opened
          class="sidebar-menu"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <template #title>
              <span>首页</span>
            </template>
          </el-menu-item>

          <div v-if="!collapsed" class="menu-divider" />

          <SidebarItem
            v-for="menu in userStore.menus"
            :key="menu.id"
            :item="menu"
          />
        </el-menu>
      </el-scrollbar>

      <div v-if="!collapsed" class="sidebar-footer">
        <span class="footer-dot" />
        <span>健康管理 · 2026</span>
      </div>
    </aside>

    <div class="app-body" :class="{ collapsed }">
      <header class="app-header">
        <div class="header-left">
          <el-button
            text
            circle
            class="icon-button"
            :title="isMobile ? '打开菜单' : collapsed ? '展开菜单' : '收起菜单'"
            @click="toggleSidebar"
          >
            <el-icon :size="18">
              <Expand v-if="collapsed || (isMobile && !mobileOpen)" />
              <Fold v-else />
            </el-icon>
          </el-button>

          <div v-if="isMobile" class="mobile-brand">
            <img src="/health-logo.svg" alt="" />
            <span>健康管理平台</span>
          </div>

          <div v-else class="breadcrumb-wrap">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item
                v-for="(item, index) in breadcrumbs"
                :key="`${item.path}-${index}`"
                :to="index < breadcrumbs.length - 1 ? item.path : undefined"
              >
                {{ item.title }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>
        </div>

        <div class="header-right">
          <el-tooltip content="下载学生端 App" placement="bottom">
            <el-button
              text
              circle
              class="icon-button"
              @click="handleDownloadApp"
            >
              <el-icon :size="17"><Iphone /></el-icon>
            </el-button>
          </el-tooltip>

          <MessageBell v-if="userStore.hasPermission('message:list')" />

          <el-tooltip content="刷新当前页面" placement="bottom">
            <el-button
              text
              circle
              class="icon-button"
              :class="{ refreshing }"
              @click="handleRefresh"
            >
              <el-icon :size="17"><Refresh /></el-icon>
            </el-button>
          </el-tooltip>

          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-trigger">
              <el-avatar
                :size="36"
                :src="userStore.userInfo?.avatar || undefined"
                class="user-avatar"
              >
                {{ userStore.avatarText }}
              </el-avatar>
              <div class="user-meta">
                <span class="user-name">{{ displayName }}</span>
                <span class="user-role">
                  {{ userStore.userInfo?.userTypeName || '平台用户' }}
                </span>
              </div>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人信息
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon>
                  修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
                <el-dropdown-item command="logoutAll">
                  <el-icon><Close /></el-icon>
                  退出全部设备
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="app-main">
        <router-view v-slot="{ Component, route: viewRoute }">
          <transition name="fade-slide" mode="out-in">
            <component
              :is="Component"
              :key="`${viewRoute.path}-${refreshKey}`"
            />
          </transition>
        </router-view>
      </main>
    </div>

    <change-password-dialog
      v-model="passwordVisible"
      @changed="handlePasswordChanged"
    />

    <AppDownloadDialog v-model="downloadVisible" />

    <el-dialog
      v-model="profileVisible"
      title="个人信息"
      width="520px"
      append-to-body
    >
      <div class="profile-head">
        <el-avatar
          :size="64"
          :src="userStore.userInfo?.avatar || undefined"
          class="profile-avatar"
        >
          {{ userStore.avatarText }}
        </el-avatar>
        <div class="profile-title">
          <h3>{{ displayName }}</h3>
          <el-tag
            :type="userTypeTagType(userStore.userInfo?.userType)"
            effect="light"
            size="small"
          >
            {{ userStore.userInfo?.userTypeName || '平台用户' }}
          </el-tag>
        </div>
      </div>

      <el-descriptions :column="1" border class="profile-descriptions">
        <el-descriptions-item label="登录账号">
          {{ userStore.userInfo?.username || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="真实姓名">
          {{ userStore.userInfo?.realName || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="所属院系">
          {{ userStore.userInfo?.deptName || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          <div class="profile-tags">
            <el-tag
              v-for="role in userStore.roleNames"
              :key="role"
              type="success"
              effect="plain"
              size="small"
            >
              {{ role }}
            </el-tag>
            <span v-if="!userStore.roleNames.length" class="muted-text">—</span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="可访问菜单">
          {{ userStore.pageMenuCount }} 个
        </el-descriptions-item>
        <el-descriptions-item label="权限标识">
          {{ userStore.permissions.length }} 个
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button type="primary" @click="profileVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
  background: var(--hp-bg);
}

/* ---------------- 侧边栏 ---------------- */
.app-sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  width: var(--sidebar-width);
  overflow: hidden;
  color: #d9eee8;
  background:
    radial-gradient(circle at 0% 0%, rgba(61, 220, 151, 0.16), transparent 42%),
    linear-gradient(180deg, #0c3b33 0%, #0f4a40 55%, #0b352e 100%);
  transition: width 0.25s ease, transform 0.25s ease;
}

.app-sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
}

.sidebar-brand {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 12px;
  height: var(--header-height);
  padding: 0 18px;
  color: #fff;
  text-decoration: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar-brand img {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 10px;
  box-shadow: 0 6px 16px rgba(61, 220, 151, 0.22);
}

.collapsed .sidebar-brand {
  justify-content: center;
  padding: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  white-space: nowrap;
}

.brand-title {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.brand-sub {
  margin-top: 2px;
  font-size: 9px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.45);
}

.sidebar-scroll {
  flex: 1;
  padding: 12px 10px;
}

/* 折叠后 Element 菜单宽度为 64px，收紧侧栏内边距避免挤压 */
.app-sidebar.collapsed .sidebar-scroll {
  padding: 12px 2px;
}

.app-sidebar.collapsed .sidebar-menu :deep(.el-menu-item),
.app-sidebar.collapsed .sidebar-menu :deep(.el-sub-menu__title) {
  margin: 4px 0;
}

.sidebar-menu {
  background: transparent;
  border-right: none;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 46px;
  margin: 4px 6px;
  font-size: 13.5px;
  line-height: 46px;
  color: rgba(230, 246, 240, 0.72);
  border-radius: 10px;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.07);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  color: #7bf0bb;
  background: linear-gradient(
    90deg,
    rgba(61, 220, 151, 0.24),
    rgba(61, 220, 151, 0.05)
  );
  box-shadow: inset 3px 0 0 #3ddc97;
}

.sidebar-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #a9f3ce;
}

.sidebar-menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  padding-left: 46px !important;
  font-size: 13px;
}

.sidebar-menu :deep(.el-icon) {
  font-size: 17px;
}

.menu-divider {
  height: 1px;
  margin: 10px 14px;
  background: rgba(255, 255, 255, 0.08);
}

.sidebar-footer {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  white-space: nowrap;
}

.footer-dot {
  width: 6px;
  height: 6px;
  background: #3ddc97;
  border-radius: 50%;
  box-shadow: 0 0 8px rgba(61, 220, 151, 0.8);
}

/* ---------------- 主体区域 ---------------- */
.app-body {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  margin-left: var(--sidebar-width);
  transition: margin-left 0.25s ease;
}

.app-body.collapsed {
  margin-left: var(--sidebar-collapsed-width);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: var(--header-height);
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1px solid var(--hp-border);
  backdrop-filter: blur(14px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.icon-button {
  color: #5f7a72;
}

.icon-button:hover {
  color: var(--hp-primary);
  background: var(--hp-mint);
}

.icon-button.refreshing :deep(.el-icon) {
  transition: transform 0.45s ease;
  transform: rotate(360deg);
}

.breadcrumb-wrap {
  min-width: 0;
}

.breadcrumb-wrap :deep(.el-breadcrumb__inner) {
  font-size: 13px;
  color: var(--hp-text-muted);
}

.breadcrumb-wrap :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--hp-text);
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  cursor: pointer;
  border-radius: 10px;
  transition: background 0.2s ease;
}

.user-trigger:hover {
  background: #f1f7f4;
}

.user-avatar,
.profile-avatar {
  color: #fff;
  font-weight: 600;
  background: linear-gradient(135deg, #1f9d7a, #3ddc97);
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.user-name {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--hp-text);
}

.user-role {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.dropdown-arrow {
  color: var(--hp-text-muted);
  font-size: 12px;
}

.mobile-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.mobile-brand img {
  width: 28px;
  height: 28px;
  border-radius: 8px;
}

.app-main {
  flex: 1;
  width: 100%;
  max-width: 1680px;
  padding: 24px;
  margin: 0 auto;
}

/* ---------------- 个人信息弹窗 ---------------- */
.profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 4px 0 20px;
}

.profile-title {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.profile-title h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--hp-text);
}

.profile-descriptions :deep(.el-descriptions__label) {
  width: 100px;
  color: var(--hp-text-secondary);
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* ---------------- 响应式 ---------------- */
.sidebar-mask {
  position: fixed;
  inset: 0;
  z-index: 99;
  background: rgba(8, 29, 25, 0.45);
  backdrop-filter: blur(2px);
}

@media (max-width: 900px) {
  .app-sidebar {
    width: var(--sidebar-width) !important;
    transform: translateX(-100%);
  }

  .app-sidebar.mobile-open {
    transform: translateX(0);
    box-shadow: 0 0 40px rgba(0, 0, 0, 0.28);
  }

  .app-body,
  .app-body.collapsed {
    margin-left: 0;
  }

  .app-header {
    padding: 0 16px;
  }

  .app-main {
    padding: 16px;
  }

  .breadcrumb-wrap,
  .user-meta {
    display: none;
  }
}
</style>
