<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getReceiverPage } from '@/api/message'
import type { MessageReceiverVO } from '@/types/message'

const props = withDefaults(
  defineProps<{
    modelValue: number[]
    max?: number
  }>(),
  { max: 100 },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: number[]): void
}>()

const options = ref<MessageReceiverVO[]>([])
const known = reactive(new Map<number, MessageReceiverVO>())
const loading = ref(false)
const loaded = ref(false)
let searchTimer: number | null = null

const selected = computed({
  get: () => props.modelValue,
  set: (value: number[]) => emit('update:modelValue', value),
})

const selectedOptions = computed(() =>
  props.modelValue
    .map((id) => known.get(id))
    .filter((item): item is MessageReceiverVO => Boolean(item)),
)

const displayOptions = computed(() => {
  const merged = new Map<number, MessageReceiverVO>()
  for (const item of options.value) merged.set(item.id, item)
  for (const item of selectedOptions.value) merged.set(item.id, item)
  return Array.from(merged.values())
})

function receiverLabel(item: MessageReceiverVO): string {
  const code = item.studentNo || item.username
  const dept = item.deptName ? ` - ${item.deptName}` : ''
  return `${item.realName}（${code}）${dept}`
}

function secondaryText(item: MessageReceiverVO): string {
  return [item.studentNo || item.username, item.deptName]
    .filter(Boolean)
    .join(' · ')
}

async function loadOptions(keyword = ''): Promise<void> {
  loading.value = true
  try {
    const page = await getReceiverPage({
      page: 1,
      size: 20,
      keyword: keyword.trim() || undefined,
    })
    options.value = page.records
    for (const item of page.records) {
      known.set(item.id, item)
    }
    loaded.value = true
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

function handleSearch(keyword: string): void {
  if (searchTimer !== null) {
    window.clearTimeout(searchTimer)
  }
  searchTimer = window.setTimeout(() => {
    loadOptions(keyword)
  }, 300)
}

function handleVisibleChange(visible: boolean): void {
  if (visible && !loaded.value) {
    loadOptions('')
  }
}

onMounted(() => {
  loadOptions('')
})

onBeforeUnmount(() => {
  if (searchTimer !== null) {
    window.clearTimeout(searchTimer)
  }
})
</script>

<template>
  <el-select
    v-model="selected"
    multiple
    filterable
    remote
    reserve-keyword
    collapse-tags
    collapse-tags-tooltip
    :multiple-limit="props.max"
    :remote-method="handleSearch"
    :loading="loading"
    placeholder="搜索并选择学生"
    no-data-text="没有找到匹配的学生"
    style="width: 100%"
    @visible-change="handleVisibleChange"
  >
    <el-option
      v-for="item in displayOptions"
      :key="item.id"
      :value="item.id"
      :label="receiverLabel(item)"
    >
      <div class="receiver-option">
        <span class="receiver-name">{{ item.realName }}</span>
        <span class="receiver-meta">{{ secondaryText(item) }}</span>
      </div>
    </el-option>
  </el-select>

  <div class="receiver-hint">
    <span>已选 <strong>{{ props.modelValue.length }}</strong> / {{ props.max }} 人</span>
    <span>支持按姓名、学号或登录账号搜索</span>
  </div>
</template>

<style scoped>
.receiver-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.receiver-name {
  font-weight: 500;
  color: var(--hp-text);
}

.receiver-meta {
  overflow: hidden;
  font-size: 11px;
  color: var(--hp-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.receiver-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  margin-top: 5px;
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.receiver-hint strong {
  color: var(--hp-primary-dark);
}
</style>
