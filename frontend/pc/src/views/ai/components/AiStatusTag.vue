<script setup lang="ts">
import { computed } from 'vue'
import {
  answerStatusLabel,
  answerStatusTagType,
  planStatusLabel,
  planStatusTagType,
} from '@/utils/ai'

const props = withDefaults(
  defineProps<{
    status?: number | null
    kind?: 'answer' | 'plan'
    effect?: 'dark' | 'light' | 'plain'
  }>(),
  {
    status: null,
    kind: 'answer',
    effect: 'light',
  },
)

const type = computed(() =>
  props.kind === 'plan'
    ? planStatusTagType(props.status)
    : answerStatusTagType(props.status),
)

const label = computed(() =>
  props.kind === 'plan'
    ? planStatusLabel(props.status)
    : answerStatusLabel(props.status),
)
</script>

<template>
  <el-tag :type="type" :effect="effect">{{ label }}</el-tag>
</template>
