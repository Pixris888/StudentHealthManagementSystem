<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    label: string
    value: string | number
    unit?: string
    hint?: string
    icon?: Component
    tone?: 'teal' | 'blue' | 'orange' | 'purple' | 'red' | 'green'
    loading?: boolean
  }>(),
  {
    unit: '',
    hint: '',
    tone: 'teal',
    loading: false,
  },
)
</script>

<template>
  <article class="kpi-card panel-card" :class="`tone-${tone}`">
    <div v-if="icon" class="kpi-icon">
      <el-icon :size="21"><component :is="icon" /></el-icon>
    </div>

    <div class="kpi-body">
      <span class="kpi-label">{{ label }}</span>
      <el-skeleton v-if="loading" animated :rows="1" class="kpi-skeleton" />
      <template v-else>
        <div class="kpi-value">
          {{ value }}
          <small v-if="unit">{{ unit }}</small>
        </div>
        <span v-if="hint" class="kpi-hint">{{ hint }}</span>
      </template>
    </div>
  </article>
</template>

<style scoped>
.kpi-card {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
  padding: 16px;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.kpi-card:hover {
  box-shadow: 0 14px 34px rgba(23, 51, 44, 0.08);
  transform: translateY(-2px);
}

.kpi-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
}

.tone-teal .kpi-icon {
  color: #1f9d7a;
  background: #e3f6ef;
}

.tone-blue .kpi-icon {
  color: #3f7ff0;
  background: #eaf1ff;
}

.tone-orange .kpi-icon {
  color: #d98b2b;
  background: #fff3e2;
}

.tone-purple .kpi-icon {
  color: #8a63e8;
  background: #f1ecff;
}

.tone-red .kpi-icon {
  color: #e05b5b;
  background: #fdeaea;
}

.tone-green .kpi-icon {
  color: #2f9d9d;
  background: #e6f6f6;
}

.kpi-body {
  min-width: 0;
  flex: 1;
}

.kpi-label {
  display: block;
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.kpi-value {
  overflow: hidden;
  margin-top: 4px;
  font-size: 21px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--hp-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kpi-value small {
  margin-left: 3px;
  font-size: 10.5px;
  font-weight: 500;
  color: var(--hp-text-muted);
}

.kpi-hint {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 4px;
  font-size: 10.5px;
  line-height: 1.45;
  color: var(--hp-text-muted);
  word-break: break-word;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.kpi-skeleton {
  margin-top: 6px;
}

.kpi-skeleton :deep(.el-skeleton__item) {
  height: 22px;
}
</style>
