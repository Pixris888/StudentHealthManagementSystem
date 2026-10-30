<script setup lang="ts">
import { computed } from 'vue'
import { joinMenuPath } from '@/utils/menu'
import { resolveMenuIcon } from '@/utils/icons'
import type { MenuTree } from '@/types/system'

defineOptions({ name: 'SidebarItem' })

const props = defineProps<{
  item: MenuTree
  basePath?: string
}>()

const fullPath = computed(() => joinMenuPath(props.basePath ?? '', props.item.path))
const iconComponent = computed(() => resolveMenuIcon(props.item.icon))

function hasVisibleChild(node: MenuTree): boolean {
  return Boolean(
    node.children?.some(
      (child) =>
        child.menuType === 2 || (child.menuType === 1 && hasVisibleChild(child)),
    ),
  )
}

/** 只展示目录和菜单，按钮权限不进入侧边栏 */
const visibleChildren = computed(() =>
  (props.item.children ?? []).filter(
    (child) => child.menuType === 2 || (child.menuType === 1 && hasVisibleChild(child)),
  ),
)
</script>

<template>
  <el-sub-menu
    v-if="item.menuType === 1 && visibleChildren.length"
    :index="fullPath"
    popper-class="sidebar-popper"
  >
    <template #title>
      <el-icon><component :is="iconComponent" /></el-icon>
      <span>{{ item.menuName }}</span>
    </template>
    <SidebarItem
      v-for="child in visibleChildren"
      :key="child.id"
      :item="child"
      :base-path="fullPath"
    />
  </el-sub-menu>

  <el-menu-item v-else-if="item.menuType === 2" :index="fullPath">
    <el-icon><component :is="iconComponent" /></el-icon>
    <template #title>
      <span>{{ item.menuName }}</span>
    </template>
  </el-menu-item>
</template>
