import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册账号' },
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/index.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled', affix: true },
      },
      /*
       * 健康知识库管理端使用静态路由：后端菜单只预置“健康知识库”一个入口，
       * 菜单管理、分类管理、AI 素材与内容审核需要通过模块内的二级导航进入。
       */
      {
        path: 'health/article/manage',
        name: 'KnowledgeManage',
        component: () => import('@/views/health/article/manage/index.vue'),
        redirect: '/health/article/manage/articles',
        meta: {
          title: '健康知识库管理',
          roles: ['DOCTOR_ADMIN', 'SUPER_ADMIN'],
          activeMenu: '/health/article',
        },
        children: [
          {
            path: 'articles',
            name: 'KnowledgeArticleManage',
            component: () =>
              import('@/views/health/article/manage/ArticleManageView.vue'),
            meta: { title: '文章管理' },
          },
          {
            path: 'categories',
            name: 'KnowledgeCategoryManage',
            component: () =>
              import('@/views/health/article/manage/CategoryManageView.vue'),
            meta: { title: '分类管理' },
          },
          {
            path: 'materials',
            name: 'KnowledgeMaterialManage',
            component: () =>
              import('@/views/health/article/manage/MaterialManageView.vue'),
            meta: { title: 'AI 知识素材' },
          },
          {
            path: 'audit',
            name: 'KnowledgeAudit',
            component: () =>
              import('@/views/health/article/manage/AuditView.vue'),
            meta: { title: '内容审核', perms: 'health:article:audit' },
          },
        ],
      },
      {
        path: 'health/article/:id(\\d+)',
        name: 'HealthArticleDetail',
        component: () => import('@/views/health/article/detail.vue'),
        meta: {
          title: '文章详情',
          perms: 'health:article:list',
          activeMenu: '/health/article',
        },
      },
      /*
       * 消息模块：后端菜单只预置“消息通知”入口（/health/message），
       * 已发送与预警任务使用静态子路由，并通过 meta.activeMenu 保持侧边栏高亮。
       */
      {
        path: 'health/message/sent',
        name: 'MessageSent',
        component: () => import('@/views/health/message/sent.vue'),
        meta: {
          title: '已发送消息',
          roles: ['DOCTOR_ADMIN', 'SUPER_ADMIN'],
          perms: 'message:send',
          activeMenu: '/health/message',
        },
      },
      {
        path: 'health/message/warning',
        name: 'MessageWarning',
        component: () => import('@/views/health/message/warning.vue'),
        meta: {
          title: '预警任务',
          roles: ['SUPER_ADMIN'],
          activeMenu: '/health/message',
        },
      },
      // 兼容文档中的 /messages 路径
      { path: 'messages', redirect: '/health/message' },
      { path: 'messages/sent', redirect: '/health/message/sent' },
      { path: 'messages/warning', redirect: '/health/message/warning' },
      /*
       * 统计报表：后端菜单只预置“统计报表”入口（/health/report），
       * 学生个人报告、健康看板与台账使用静态路由 + 角色守卫。
       */
      {
        path: 'health/report/monthly',
        name: 'ReportMonthly',
        component: () => import('@/views/health/report/MonthlyReportView.vue'),
        meta: {
          title: '个人健康报告',
          roles: ['STUDENT'],
          perms: 'report:list',
          activeMenu: '/health/report',
        },
      },
      {
        path: 'health/report/dashboard',
        name: 'ReportDashboard',
        component: () => import('@/views/health/report/DashboardView.vue'),
        meta: {
          title: '健康看板',
          roles: ['DOCTOR_ADMIN', 'SUPER_ADMIN'],
          perms: 'report:list',
          activeMenu: '/health/report',
        },
      },
      {
        path: 'health/report/ledger',
        name: 'ReportLedger',
        component: () => import('@/views/health/report/LedgerView.vue'),
        meta: {
          title: '健康台账',
          roles: ['DOCTOR_ADMIN', 'SUPER_ADMIN'],
          perms: 'report:list',
          activeMenu: '/health/report',
        },
      },
      // 兼容文档中的 /reports 路径
      { path: 'reports', redirect: '/health/report' },
      { path: 'reports/monthly', redirect: '/health/report/monthly' },
      { path: 'reports/dashboard', redirect: '/health/report/dashboard' },
      { path: 'reports/ledger', redirect: '/health/report/ledger' },
      /*
       * AI 健康助手：
       * - 后端菜单实际挂在“健康管理”目录下，动态路由为 /health/ai；
       * - 文档约定的 /ai 同时保留为静态入口，二者渲染同一页面；
       * - 计划页与管理端页面使用静态路由，并通过 meta.activeMenu 高亮侧边栏。
       */
      {
        path: 'ai',
        name: 'AiAssistant',
        component: () => import('@/views/health/ai/index.vue'),
        meta: {
          title: 'AI健康助手',
          perms: 'ai:consult:list',
          activeMenu: '/health/ai',
        },
      },
      {
        path: 'ai/plans',
        name: 'AiPlans',
        component: () => import('@/views/ai/AiPlanView.vue'),
        meta: {
          title: '我的健康计划',
          roles: ['STUDENT'],
          perms: 'ai:plan:generate',
          activeMenu: '/health/ai',
        },
      },
      {
        path: 'ai/manage',
        redirect: '/ai/manage/consultations',
      },
      {
        path: 'ai/manage/consultations',
        name: 'AiConsultationManage',
        component: () => import('@/views/ai/AiConsultationManageView.vue'),
        meta: {
          title: 'AI 问答管理',
          roles: ['DOCTOR_ADMIN', 'SUPER_ADMIN'],
          perms: 'ai:consult:manage',
          activeMenu: '/health/ai',
        },
      },
      {
        path: 'ai/manage/stats',
        name: 'AiStatistics',
        component: () => import('@/views/ai/AiStatsView.vue'),
        meta: {
          title: 'AI 使用统计',
          roles: ['DOCTOR_ADMIN', 'SUPER_ADMIN'],
          perms: 'ai:consult:stats',
          activeMenu: '/health/ai',
        },
      },
    ],
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无访问权限' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

export default router
