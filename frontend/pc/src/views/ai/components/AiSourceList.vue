<script setup lang="ts">
import { computed } from 'vue'
import { Document } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    sources?: string[]
    /** 历史记录 / 管理端只有 knowledgeSource 字符串 */
    knowledgeSource?: string
    title?: string
  }>(),
  {
    sources: () => [],
    knowledgeSource: '',
    title: '参考来源',
  },
)

const items = computed(() => {
  const list = (props.sources ?? []).map((item) => item.trim()).filter(Boolean)
  if (list.length) return list
  const single = props.knowledgeSource?.trim()
  return single ? [single] : []
})
</script>

<template>
  <div v-if="items.length" class="ai-sources">
    <div class="sources-heading">
      <el-icon><Document /></el-icon>
      <span>{{ title }}</span>
    </div>
    <ul class="sources-list">
      <li
        v-for="(item, index) in items"
        :key="`${index}-${item}`"
        class="source-item"
      >
        <span class="source-index">{{ index + 1 }}</span>
        <span class="source-text">{{ item }}</span>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.ai-sources {
  padding-top: 12px;
  margin-top: 14px;
  border-top: 1px dashed #dfeae5;
}

.sources-heading {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--hp-text-secondary);
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.source-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 7px 10px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--hp-text-secondary);
  background: #f5faf8;
  border-radius: 8px;
}

.source-index {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 17px;
  height: 17px;
  margin-top: 1px;
  font-size: 10px;
  font-weight: 700;
  color: var(--hp-primary);
  background: #dff2ea;
  border-radius: 50%;
}

.source-text {
  word-break: break-word;
}
</style>
