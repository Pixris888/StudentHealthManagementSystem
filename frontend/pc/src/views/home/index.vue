<script setup lang="ts">
import { computed, onMounted, reactive, ref, type Component } from 'vue'
import { useRouter } from 'vue-router'
import {
  Apple,
  ArrowRight,
  Bicycle,
  Calendar,
  CircleCheck,
  DataAnalysis,
  Document,
  FirstAidKit,
  Grid,
  Key,
  MagicStick,
  Menu,
  Message,
  Moon,
  Reading,
  Setting,
  Trophy,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { pageRoles } from '@/api/role'
import { getMenuTree } from '@/api/menu'
import { countMenuTypes } from '@/utils/menu'
import { userTypeTagType } from '@/utils/dict'

interface StatCard {
  key: string
  label: string
  value: number
  hint: string
  icon: Component
  tone: string
}

interface QuickAction {
  title: string
  desc: string
  path: string
  icon: Component
  perm: string
  status: '已完成' | '开发中'
  tone: string
}

interface ModuleItem {
  name: string
  desc: string
  icon: Component
  done: boolean
}

interface ProgressItem {
  label: string
  value: number
  color: string
}

interface TipItem {
  title: string
  text: string
  icon: Component
}

const router = useRouter()
const userStore = useUserStore()

const statsLoading = ref(false)
const statData = reactive({
  roleTotal: 0,
  menuTotal: 0,
  pageTotal: 0,
  directoryTotal: 0,
  permTotal: 0,
  roleFromApi: false,
  menuFromApi: false,
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const today = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())

const stats = computed<StatCard[]>(() => [
  {
    key: 'role',
    label: '系统角色',
    value: statData.roleTotal,
    hint: statData.roleFromApi ? '系统全部角色' : '当前账号角色',
    icon: UserFilled,
    tone: 'teal',
  },
  {
    key: 'menu',
    label: '菜单节点',
    value: statData.menuTotal,
    hint: statData.menuFromApi
      ? `${statData.directoryTotal} 个目录 · ${statData.pageTotal} 个菜单`
      : '当前账号可访问',
    icon: Menu,
    tone: 'blue',
  },
  {
    key: 'page',
    label: '功能页面',
    value: statData.pageTotal,
    hint: statData.menuFromApi ? '侧边栏可访问页面' : '当前账号可访问',
    icon: Grid,
    tone: 'orange',
  },
  {
    key: 'perm',
    label: '权限标识',
    value: statData.permTotal,
    hint: '当前账号可用权限',
    icon: Key,
    tone: 'purple',
  },
])

const quickActionsAll: QuickAction[] = [
  {
    title: '健康档案',
    desc: '维护个人健康信息、BMI 与紧急联系人',
    path: '/health/profile',
    icon: Document,
    perm: 'health:profile:list',
    status: '已完成',
    tone: 'teal',
  },
  {
    title: '体测成绩',
    desc: '录入、导入体测成绩并查看指标趋势',
    path: '/health/physical',
    icon: Trophy,
    perm: 'health:physical:list',
    status: '已完成',
    tone: 'blue',
  },
  {
    title: '角色管理',
    desc: '新增角色、配置数据范围与菜单授权',
    path: '/system/role',
    icon: UserFilled,
    perm: 'system:role:list',
    status: '已完成',
    tone: 'purple',
  },
  {
    title: '菜单管理',
    desc: '维护目录、菜单与按钮权限标识',
    path: '/system/menu',
    icon: Menu,
    perm: 'system:menu:list',
    status: '已完成',
    tone: 'orange',
  },
  {
    title: '用户管理',
    desc: '账号启停、密码重置与角色分配',
    path: '/system/user',
    icon: User,
    perm: 'system:user:list',
    status: '已完成',
    tone: 'blue',
  },
  {
    title: '健康打卡',
    desc: '睡眠、运动、饮食与情绪每日记录',
    path: '/health/checkin',
    icon: Calendar,
    perm: 'health:checkin:list',
    status: '已完成',
    tone: 'teal',
  },
  {
    title: '健康知识库',
    desc: '校园科普文章浏览、发布与 AI 知识素材维护',
    path: '/health/article',
    icon: Reading,
    perm: 'health:article:list',
    status: '已完成',
    tone: 'purple',
  },
  {
    title: '消息中心',
    desc: '健康预警、校园公告与关怀留言',
    path: '/health/message',
    icon: Message,
    perm: 'message:list',
    status: '已完成',
    tone: 'orange',
  },
  {
    title: '统计报表',
    desc: '个人月报、健康看板与健康台账导出',
    path: '/health/report',
    icon: DataAnalysis,
    perm: 'report:list',
    status: '已完成',
    tone: 'blue',
  },
]

const quickActions = computed(() =>
  quickActionsAll.filter((action) => userStore.hasPermission(action.perm)),
)

const modules: ModuleItem[] = [
  {
    name: '健康档案',
    desc: '个人健康信息、体测成绩与指标趋势',
    icon: Document,
    done: true,
  },
  {
    name: '健康打卡',
    desc: '睡眠、运动、饮食、情绪打卡与照片',
    icon: Calendar,
    done: true,
  },
  {
    name: 'AI 健康助手',
    desc: '健康问答与个性化作息运动计划',
    icon: MagicStick,
    done: false,
  },
  {
    name: '健康知识库',
    desc: '校园科普文章分类、发布与浏览',
    icon: Reading,
    done: true,
  },
  {
    name: '消息通知',
    desc: '健康预警、校园公告与关怀留言',
    icon: Message,
    done: true,
  },
  {
    name: '统计报表',
    desc: '个人月报与脱敏健康看板',
    icon: DataAnalysis,
    done: true,
  },
  {
    name: '系统管理',
    desc: '用户、角色、菜单与权限体系',
    icon: Setting,
    done: true,
  },
]

const progressItems: ProgressItem[] = [
  { label: '登录与认证', value: 100, color: '#1f9d7a' },
  { label: '用户与权限管理', value: 100, color: '#1f9d7a' },
  { label: '健康档案', value: 100, color: '#1f9d7a' },
  { label: '体测成绩与趋势', value: 100, color: '#1f9d7a' },
  { label: '健康打卡', value: 100, color: '#1f9d7a' },
  { label: '健康知识库', value: 100, color: '#1f9d7a' },
  { label: '消息通知', value: 100, color: '#1f9d7a' },
  { label: '统计报表', value: 100, color: '#1f9d7a' },
  { label: '其他健康业务模块', value: 40, color: '#3f7ff0' },
]

const healthTips: TipItem[] = [
  {
    title: '规律作息',
    text: '尽量在 23:00 前入睡，保证 7-8 小时优质睡眠。',
    icon: Moon,
  },
  {
    title: '适量运动',
    text: '每周累计至少 150 分钟中等强度有氧运动。',
    icon: Bicycle,
  },
  {
    title: '均衡饮食',
    text: '三餐规律、少糖少油，保证蔬果与蛋白质摄入。',
    icon: Apple,
  },
]

async function loadDashboard(): Promise<void> {
  const counts = countMenuTypes(userStore.menus)
  statData.menuTotal = counts.total
  statData.pageTotal = counts.pages
  statData.directoryTotal = counts.directories
  statData.roleTotal = userStore.roles.length
  statData.permTotal = userStore.permissions.length

  statsLoading.value = true
  try {
    if (userStore.hasPermission('system:role:list')) {
      const page = await pageRoles({ page: 1, size: 1 })
      statData.roleTotal = page.total
      statData.roleFromApi = true
    }
  } catch {
    // 无角色管理权限或接口异常时保留本地统计
  }

  try {
    if (userStore.hasPermission('system:menu:list')) {
      const menuTree = await getMenuTree()
      const full = countMenuTypes(menuTree)
      statData.menuTotal = full.total
      statData.pageTotal = full.pages
      statData.directoryTotal = full.directories
      statData.menuFromApi = true
    }
  } catch {
    // 无菜单管理权限或接口异常时保留本地统计
  } finally {
    statsLoading.value = false
  }
}

function openQuickAction(action: QuickAction): void {
  router.push(action.path)
}

onMounted(loadDashboard)
</script>

<template>
  <div class="page-container home-page">
    <!-- 欢迎横幅 -->
    <section class="welcome-banner">
      <div class="banner-orb orb-a" />
      <div class="banner-orb orb-b" />
      <div class="banner-grid" />

      <div class="welcome-content">
        <p class="welcome-date">
          <el-icon><Calendar /></el-icon>
          {{ today }}
        </p>
        <h2>{{ greeting }}，{{ userStore.displayName }}</h2>
        <p class="welcome-desc">
          欢迎进入大学生健康管理平台。当前版本已完成登录认证、用户、角色与菜单权限管理、
          健康档案、体测成绩和每日健康打卡模块，其余健康业务模块正在持续建设中。
        </p>
        <div class="welcome-tags">
          <span class="welcome-tag">
            <el-icon><UserFilled /></el-icon>
            {{ userStore.userInfo?.userTypeName || '平台用户' }}
          </span>
          <span class="welcome-tag">
            <el-icon><CircleCheck /></el-icon>
            健康打卡已上线
          </span>
        </div>
      </div>

      <div class="welcome-visual">
        <div class="pulse-ring ring-one" />
        <div class="pulse-ring ring-two" />
        <div class="visual-core">
          <el-icon :size="46"><FirstAidKit /></el-icon>
        </div>
        <div class="visual-caption">
          <span class="caption-dot" />
          STUDENT HEALTH
        </div>
      </div>
    </section>

    <!-- 数据统计 -->
    <section v-loading="statsLoading" class="stats-grid">
      <article
        v-for="stat in stats"
        :key="stat.key"
        class="stat-card panel-card"
      >
        <div class="stat-icon" :class="`tone-${stat.tone}`">
          <el-icon :size="22"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-label">{{ stat.label }}</span>
          <strong class="stat-value">{{ stat.value }}</strong>
          <span class="stat-hint">{{ stat.hint }}</span>
        </div>
      </article>
    </section>

    <div class="home-grid">
      <div class="home-main">
        <!-- 快捷入口 -->
        <section class="panel-card section-card">
          <div class="section-head">
            <div>
              <h3>快捷入口</h3>
              <p>常用功能一键直达</p>
            </div>
            <span class="section-en">QUICK ACCESS</span>
          </div>

          <div v-if="quickActions.length" class="quick-grid">
            <button
              v-for="action in quickActions"
              :key="action.path"
              type="button"
              class="quick-item"
              @click="openQuickAction(action)"
            >
              <div class="quick-icon" :class="`tone-${action.tone}`">
                <el-icon :size="20"><component :is="action.icon" /></el-icon>
              </div>
              <div class="quick-info">
                <div class="quick-title">
                  <span>{{ action.title }}</span>
                  <el-tag
                    size="small"
                    :type="action.status === '已完成' ? 'success' : 'info'"
                    effect="plain"
                  >
                    {{ action.status }}
                  </el-tag>
                </div>
                <p>{{ action.desc }}</p>
              </div>
              <el-icon class="quick-arrow"><ArrowRight /></el-icon>
            </button>
          </div>
          <el-empty
            v-else
            description="当前账号暂无可用快捷入口"
            :image-size="72"
          />
        </section>

        <!-- 平台模块 -->
        <section class="panel-card section-card">
          <div class="section-head">
            <div>
              <h3>平台功能模块</h3>
              <p>围绕大学生日常健康管理规划的七大模块</p>
            </div>
            <el-tag type="success" effect="plain">核心健康模块已上线</el-tag>
          </div>

          <div class="module-grid">
            <div
              v-for="module in modules"
              :key="module.name"
              class="module-item"
              :class="{ done: module.done }"
            >
              <div class="module-icon">
                <el-icon :size="19"><component :is="module.icon" /></el-icon>
              </div>
              <div class="module-info">
                <strong>{{ module.name }}</strong>
                <span>{{ module.desc }}</span>
              </div>
              <el-tag
                size="small"
                :type="module.done ? 'success' : 'info'"
                effect="plain"
              >
                {{ module.done ? '已完成' : '规划中' }}
              </el-tag>
            </div>
          </div>
        </section>
      </div>

      <aside class="home-side">
        <!-- 账号信息 -->
        <section class="panel-card account-card">
          <div class="account-head">
            <el-avatar
              :size="58"
              :src="userStore.userInfo?.avatar || undefined"
              class="account-avatar"
            >
              {{ userStore.avatarText }}
            </el-avatar>
            <div class="account-name">
              <h3>{{ userStore.displayName }}</h3>
              <p>{{ userStore.userInfo?.username }}</p>
            </div>
            <el-tag
              :type="userTypeTagType(userStore.userInfo?.userType)"
              effect="light"
              size="small"
            >
              {{ userStore.userInfo?.userTypeName || '平台用户' }}
            </el-tag>
          </div>

          <div class="account-info">
            <div>
              <span>所属院系</span>
              <strong>{{ userStore.userInfo?.deptName || '—' }}</strong>
            </div>
            <div>
              <span>角色</span>
              <strong>{{ userStore.roleNames.join(' / ') || '—' }}</strong>
            </div>
            <div>
              <span>可访问菜单</span>
              <strong>{{ userStore.pageMenuCount }} 个</strong>
            </div>
            <div>
              <span>权限标识</span>
              <strong>{{ userStore.permissions.length }} 个</strong>
            </div>
          </div>
        </section>

        <!-- 建设进度 -->
        <section class="panel-card section-card progress-card">
          <div class="section-head compact">
            <div>
              <h3>平台建设进度</h3>
              <p>当前版本已完成登录、系统管理与健康档案基础模块</p>
            </div>
          </div>

          <div class="progress-list">
            <div
              v-for="item in progressItems"
              :key="item.label"
              class="progress-row"
            >
              <div class="progress-label">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}%</strong>
              </div>
              <el-progress
                :percentage="item.value"
                :show-text="false"
                :stroke-width="7"
                :color="item.color"
              />
            </div>
          </div>
        </section>

        <!-- 健康小贴士 -->
        <section class="panel-card section-card tips-card">
          <div class="section-head compact">
            <div>
              <h3>健康小贴士</h3>
              <p>从今天开始，照顾好自己</p>
            </div>
          </div>

          <div class="tips-list">
            <div v-for="tip in healthTips" :key="tip.title" class="tip-item">
              <div class="tip-icon">
                <el-icon :size="18"><component :is="tip.icon" /></el-icon>
              </div>
              <div class="tip-text">
                <strong>{{ tip.title }}</strong>
                <p>{{ tip.text }}</p>
              </div>
            </div>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  gap: 18px;
}

/* ---------------- 欢迎横幅 ---------------- */
.welcome-banner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  min-height: 218px;
  padding: 34px 40px;
  overflow: hidden;
  color: #eafff7;
  background:
    radial-gradient(circle at 82% 18%, rgba(61, 220, 151, 0.24), transparent 38%),
    linear-gradient(120deg, #0a352d 0%, #0f4f42 52%, #14745a 100%);
  border-radius: 20px;
  box-shadow: 0 18px 44px rgba(12, 59, 51, 0.18);
}

.banner-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: linear-gradient(90deg, #000 0%, transparent 70%);
}

.banner-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.orb-a {
  top: -90px;
  right: 18%;
  width: 260px;
  height: 260px;
  background: radial-gradient(circle, rgba(61, 220, 151, 0.22), transparent 68%);
}

.orb-b {
  right: -70px;
  bottom: -130px;
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(123, 240, 187, 0.14), transparent 66%);
}

.welcome-content {
  position: relative;
  z-index: 1;
  max-width: 700px;
}

.welcome-date {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 12px;
  font-size: 12px;
  color: rgba(234, 255, 247, 0.62);
}

.welcome-content h2 {
  margin-bottom: 12px;
  font-size: clamp(24px, 2.5vw, 32px);
  font-weight: 700;
  line-height: 1.3;
  color: #fff;
}

.welcome-desc {
  max-width: 640px;
  font-size: 13.5px;
  line-height: 1.85;
  color: rgba(234, 255, 247, 0.65);
}

.welcome-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.welcome-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  font-size: 12px;
  color: #bff7dc;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
}

.welcome-visual {
  position: relative;
  z-index: 1;
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 190px;
  height: 170px;
}

.pulse-ring {
  position: absolute;
  border: 1px solid rgba(123, 240, 187, 0.22);
  border-radius: 50%;
}

.ring-one {
  width: 120px;
  height: 120px;
  animation: pulse 3.6s ease-in-out infinite;
}

.ring-two {
  width: 166px;
  height: 166px;
  animation: pulse 3.6s ease-in-out 1.2s infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.4;
    transform: scale(0.96);
  }
  50% {
    opacity: 0.85;
    transform: scale(1.02);
  }
}

.visual-core {
  display: grid;
  place-items: center;
  width: 92px;
  height: 92px;
  color: #7bf0bb;
  background: rgba(61, 220, 151, 0.13);
  border: 1px solid rgba(123, 240, 187, 0.25);
  border-radius: 28px;
  box-shadow:
    inset 0 0 30px rgba(61, 220, 151, 0.1),
    0 16px 40px rgba(0, 0, 0, 0.14);
  backdrop-filter: blur(8px);
}

.visual-caption {
  position: absolute;
  bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 9px;
  letter-spacing: 2px;
  color: rgba(234, 255, 247, 0.45);
}

.caption-dot {
  width: 5px;
  height: 5px;
  background: #3ddc97;
  border-radius: 50%;
  box-shadow: 0 0 8px #3ddc97;
}

/* ---------------- 数据卡片 ---------------- */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.stat-card:hover {
  box-shadow: 0 14px 34px rgba(23, 51, 44, 0.08);
  transform: translateY(-2px);
}

.stat-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 13px;
}

.tone-teal {
  color: #1f9d7a;
  background: #e3f6ef;
}

.tone-blue {
  color: #3f7ff0;
  background: #eaf1ff;
}

.tone-orange {
  color: #d98b2b;
  background: #fff3e2;
}

.tone-purple {
  color: #8a63e8;
  background: #f1ecff;
}

.stat-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.3;
}

.stat-label {
  font-size: 12.5px;
  color: var(--hp-text-secondary);
}

.stat-value {
  margin: 4px 0 3px;
  font-size: 27px;
  font-weight: 700;
  color: var(--hp-text);
}

.stat-hint {
  overflow: hidden;
  font-size: 11px;
  color: var(--hp-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------------- 主区域布局 ---------------- */
.home-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.75fr) minmax(320px, 1fr);
  gap: 18px;
  align-items: start;
}

.home-main,
.home-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.section-card {
  padding: 22px 24px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.section-head.compact {
  margin-bottom: 18px;
}

.section-head h3 {
  margin-bottom: 4px;
  font-size: 16px;
  font-weight: 700;
  color: var(--hp-text);
}

.section-head p {
  font-size: 12px;
  color: var(--hp-text-muted);
}

.section-en {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #b3c7bf;
}

/* ---------------- 快捷入口 ---------------- */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 13px;
  width: 100%;
  padding: 15px 16px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  background: #fbfdfc;
  border: 1px solid #e9f1ee;
  border-radius: 13px;
  transition: all 0.22s ease;
}

.quick-item:hover {
  background: #fff;
  border-color: #cbe8de;
  box-shadow: 0 10px 26px rgba(23, 51, 44, 0.07);
  transform: translateY(-2px);
}

.quick-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
}

.quick-info {
  flex: 1;
  min-width: 0;
}

.quick-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
}

.quick-title > span {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--hp-text);
}

.quick-info p {
  overflow: hidden;
  font-size: 11.5px;
  color: var(--hp-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-arrow {
  flex-shrink: 0;
  color: #b3c7bf;
  transition: transform 0.2s ease, color 0.2s ease;
}

.quick-item:hover .quick-arrow {
  color: var(--hp-primary);
  transform: translateX(3px);
}

/* ---------------- 平台模块 ---------------- */
.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.module-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  background: #fbfdfc;
  border: 1px solid #edf3f0;
  border-radius: 12px;
}

.module-item.done {
  background: #f4fbf8;
  border-color: #d7ece4;
}

.module-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 36px;
  height: 36px;
  color: #7e9a91;
  background: #f0f5f3;
  border-radius: 10px;
}

.module-item.done .module-icon {
  color: var(--hp-primary);
  background: #e3f6ef;
}

.module-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.module-info strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--hp-text);
}

.module-info span {
  font-size: 11px;
  line-height: 1.5;
  color: var(--hp-text-muted);
}

/* ---------------- 账号卡片 ---------------- */
.account-card {
  padding: 24px;
}

.account-head {
  display: flex;
  align-items: center;
  gap: 13px;
  padding-bottom: 18px;
  border-bottom: 1px solid #edf3f0;
}

.account-avatar {
  flex-shrink: 0;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #1f9d7a, #3ddc97);
}

.account-name {
  flex: 1;
  min-width: 0;
}

.account-name h3 {
  overflow: hidden;
  font-size: 16px;
  font-weight: 700;
  color: var(--hp-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-name p {
  margin-top: 3px;
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.account-info {
  display: flex;
  flex-direction: column;
  gap: 13px;
  padding-top: 18px;
}

.account-info > div {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  font-size: 12.5px;
}

.account-info span {
  flex-shrink: 0;
  color: var(--hp-text-muted);
}

.account-info strong {
  font-weight: 500;
  color: var(--hp-text);
  text-align: right;
  word-break: break-word;
}

/* ---------------- 进度 & 贴士 ---------------- */
.progress-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 7px;
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.progress-label strong {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--hp-text);
}

.tips-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.tip-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 34px;
  height: 34px;
  color: var(--hp-primary);
  background: #e9f7f2;
  border-radius: 10px;
}

.tip-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.tip-text strong {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--hp-text);
}

.tip-text p {
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--hp-text-muted);
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .home-grid {
    grid-template-columns: 1fr;
  }

  .home-side {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-items: start;
  }

  .tips-card {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .welcome-banner {
    min-height: auto;
    padding: 26px 24px;
  }

  .welcome-visual {
    display: none;
  }
}

@media (max-width: 700px) {
  .quick-grid,
  .module-grid,
  .home-side {
    grid-template-columns: 1fr;
  }

  .tips-card {
    grid-column: auto;
  }

  .section-card {
    padding: 18px 16px;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-value {
    font-size: 23px;
  }
}
</style>
