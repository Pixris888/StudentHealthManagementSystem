import type { DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 按钮级权限指令：
 * <el-button v-permission="'system:role:add'">新增</el-button>
 */
export const permission = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>): void {
    const value = binding.value
    if (!value) return
    const userStore = useUserStore()
    if (!userStore.hasPermission(value)) {
      el.parentNode?.removeChild(el)
    }
  },
}
