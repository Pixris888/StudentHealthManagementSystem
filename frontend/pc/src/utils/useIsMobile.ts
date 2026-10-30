import { onBeforeUnmount, onMounted, ref } from 'vue'

export function useIsMobile(maxWidth = 768) {
  const isMobile = ref(false)

  function update(): void {
    isMobile.value = window.innerWidth <= maxWidth
  }

  onMounted(() => {
    update()
    window.addEventListener('resize', update)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', update)
  })

  return isMobile
}
