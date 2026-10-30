import type { permission } from '@/directives/permission'

declare module 'vue' {
  interface GlobalDirectives {
    vPermission: typeof permission
  }
}

export {}
