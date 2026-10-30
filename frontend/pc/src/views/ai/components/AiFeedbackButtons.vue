<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'
import type { FeedbackStatus } from '@/types/ai'

const props = withDefaults(
  defineProps<{
    feedback?: FeedbackStatus
    disabled?: boolean
  }>(),
  {
    feedback: 0,
    disabled: false,
  },
)

const emit = defineEmits<{
  (e: 'feedback', helpful: boolean): void
}>()

const selected = computed(() => props.feedback ?? 0)
</script>

<template>
  <div class="feedback-actions">
    <span class="feedback-label">这个回答有帮助吗？</span>
    <el-button
      size="small"
      :type="selected === 1 ? 'success' : undefined"
      :plain="selected !== 1"
      :icon="CircleCheck"
      :disabled="disabled"
      @click="emit('feedback', true)"
    >
      有帮助
    </el-button>
    <el-button
      size="small"
      :type="selected === 2 ? 'danger' : undefined"
      :plain="selected !== 2"
      :icon="CircleClose"
      :disabled="disabled"
      @click="emit('feedback', false)"
    >
      没帮助
    </el-button>
  </div>
</template>

<style scoped>
.feedback-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.feedback-label {
  margin-right: 2px;
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.feedback-actions :deep(.el-button + .el-button) {
  margin-left: 2px;
}

.feedback-actions :deep(.el-button) {
  height: 26px;
  padding: 0 10px;
  font-size: 11.5px;
  border-radius: 7px;
}
</style>
