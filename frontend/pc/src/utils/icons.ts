import type { Component } from 'vue'
import {
  Bell,
  Calendar,
  Clock,
  Collection,
  DataAnalysis,
  Document,
  EditPen,
  FirstAidKit,
  Folder,
  Grid,
  HomeFilled,
  MagicStick,
  Menu as MenuIcon,
  Message,
  Monitor,
  Reading,
  Setting,
  Tickets,
  Trophy,
  User,
  UserFilled,
} from '@element-plus/icons-vue'

/**
 * 后端菜单 icon 字段沿用 RuoYi 图标名，这里映射到 Element Plus 图标组件。
 * 未识别的图标统一显示为菜单图标，不会影响菜单展示。
 */
const menuIconMap: Record<string, Component> = {
  system: Setting,
  health: FirstAidKit,
  user: User,
  peoples: UserFilled,
  'tree-table': Grid,
  profile: Document,
  tice: Trophy,
  clock: Clock,
  article: Reading,
  message: Message,
  report: DataAnalysis,
  ai: MagicStick,
  edit: EditPen,
  dict: Collection,
  log: Tickets,
  operlog: Tickets,
  monitor: Monitor,
  folder: Folder,
  home: HomeFilled,
  bell: Bell,
  calendar: Calendar,
}

export function resolveMenuIcon(icon?: string | null): Component {
  if (!icon) return MenuIcon
  return menuIconMap[icon] ?? MenuIcon
}

export interface IconOption {
  value: string
  label: string
  icon: Component
}

/** 菜单表单可选的图标 */
export const iconOptions: IconOption[] = [
  { value: 'system', label: '系统设置', icon: Setting },
  { value: 'health', label: '健康医疗', icon: FirstAidKit },
  { value: 'user', label: '用户', icon: User },
  { value: 'peoples', label: '角色人群', icon: UserFilled },
  { value: 'tree-table', label: '树形表格', icon: Grid },
  { value: 'profile', label: '健康档案', icon: Document },
  { value: 'tice', label: '体测成绩', icon: Trophy },
  { value: 'clock', label: '打卡记录', icon: Clock },
  { value: 'article', label: '科普文章', icon: Reading },
  { value: 'message', label: '消息通知', icon: Message },
  { value: 'report', label: '统计报表', icon: DataAnalysis },
  { value: 'ai', label: 'AI 助手', icon: MagicStick },
  { value: 'edit', label: '编辑', icon: EditPen },
  { value: 'dict', label: '字典', icon: Collection },
  { value: 'log', label: '日志', icon: Tickets },
  { value: 'monitor', label: '监控', icon: Monitor },
  { value: 'folder', label: '目录', icon: Folder },
  { value: 'home', label: '首页', icon: HomeFilled },
]
