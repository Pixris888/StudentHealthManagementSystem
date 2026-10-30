<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps<{ content?: string | null }>()
const html = computed(() => renderMarkdown(props.content))
</script>

<template>
  <div class="markdown-body" v-html="html" />
</template>

<style scoped>
.markdown-body {
  font-size: 14px;
  line-height: 1.8;
  color: var(--hp-text);
  word-break: break-word;
}

.markdown-body :deep(p) {
  margin: 8px 0;
}

.markdown-body :deep(p:first-child) {
  margin-top: 0;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 16px 0 8px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--hp-text);
}

.markdown-body :deep(h1) {
  font-size: 20px;
}

.markdown-body :deep(h2) {
  font-size: 17px;
}

.markdown-body :deep(h3) {
  font-size: 15px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 22px;
  margin: 8px 0;
}

.markdown-body :deep(li) {
  margin: 4px 0;
}

.markdown-body :deep(table) {
  display: block;
  width: 100%;
  margin: 12px 0;
  overflow-x: auto;
  border-collapse: collapse;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 8px 10px;
  text-align: left;
  white-space: nowrap;
  border: 1px solid #dcdfe6;
}

.markdown-body :deep(th) {
  font-weight: 600;
  background: #f6faf8;
}

.markdown-body :deep(blockquote) {
  padding: 8px 12px;
  margin: 12px 0;
  color: #607a72;
  background: #f5f7fa;
  border-left: 3px solid var(--hp-primary);
}

.markdown-body :deep(blockquote p) {
  margin: 0;
}

.markdown-body :deep(code) {
  padding: 2px 4px;
  font-size: 12.5px;
  background: #f2f3f5;
  border-radius: 4px;
}

.markdown-body :deep(pre) {
  padding: 12px 14px;
  overflow-x: auto;
  background: #f5f7fa;
  border-radius: 8px;
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: transparent;
}

.markdown-body :deep(a) {
  color: var(--hp-primary);
  word-break: break-all;
}

.markdown-body :deep(hr) {
  height: 1px;
  margin: 16px 0;
  background: var(--hp-border);
  border: none;
}

.markdown-body :deep(img) {
  max-width: 100%;
  height: auto;
}
</style>
