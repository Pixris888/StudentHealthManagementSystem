import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    perms?: string | null
    roles?: string[]
    activeMenu?: string
    menuId?: number
    affix?: boolean
  }
}
