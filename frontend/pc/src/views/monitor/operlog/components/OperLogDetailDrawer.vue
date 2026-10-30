<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getOperLog } from '@/api/oper-log'
import { useIsMobile } from '@/utils/useIsMobile'
import {
  businessTypeLabel,
  businessTypeTagType,
  operLogStatusLabel,
  operLogStatusTagType,
  operatorTypeLabel,
  prettyJson,
  requestMethodTagType,
} from '@/utils/system'
import { formatDateTime } from '@/utils/format'
import type { OperLog } from '@/types/system'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    logId?: number | null
  }>(),
  {
    logId: null,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const isMobile = useIsMobile()
const drawerSize = computed(() => (isMobile.value ? '94%' : '760px'))

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const log = ref<OperLog | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const requestParamText = computed(() => prettyJson(log.value?.operParam))
const responseText = computed(() => prettyJson(log.value?.jsonResult))

async function loadLog(): Promise<void> {
  if (!props.logId) {
    log.value = null
    return
  }
  loading.value = true
  errorMessage.value = ''
  log.value = null
  try {
    log.value = await getOperLog(props.logId)
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '操作日志加载失败'
  } finally {
    loading.value = false
  }
}

function retry(): void {
  loadLog()
}

watch(
  () => [props.modelValue, props.logId] as const,
  ([open]) => {
    if (open) loadLog()
  },
)
</script>

<template>
  <el-drawer
    v-model="visible"
    title="操作日志详情"
    :size="drawerSize"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading" class="oper-log-detail">
      <div v-if="errorMessage" class="detail-error">
        <el-result
          icon="warning"
          title="日志无法查看"
          :sub-title="errorMessage"
        >
          <template #extra>
            <el-button type="primary" @click="retry">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <template v-else-if="log">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="只读审计记录"
          description="操作日志用于安全审计，不支持修改或删除。请求参数与返回结果可能包含敏感信息，请勿外传。"
          class="readonly-alert"
        />

        <div class="detail-tags">
          <el-tag
            :type="businessTypeTagType(log.businessType)"
            effect="light"
          >
            {{ businessTypeLabel(log.businessType) }}
          </el-tag>
          <el-tag
            :type="operLogStatusTagType(log.status)"
            effect="dark"
          >
            {{ operLogStatusLabel(log.status) }}
          </el-tag>
          <el-tag
            v-if="log.requestMethod"
            :type="requestMethodTagType(log.requestMethod)"
            effect="plain"
          >
            {{ log.requestMethod }}
          </el-tag>
        </div>

        <el-alert
          v-if="log.status === 1 && log.errorMsg"
          type="error"
          :closable="false"
          show-icon
          title="操作失败原因"
          :description="log.errorMsg"
          class="error-alert"
        />

        <el-descriptions
          :column="isMobile ? 1 : 2"
          border
          class="log-meta"
        >
          <el-descriptions-item label="日志 ID">
            {{ log.id }}
          </el-descriptions-item>
          <el-descriptions-item label="操作时间">
            {{ formatDateTime(log.operTime ?? log.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="模块标题">
            {{ log.title || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="业务类型">
            {{ businessTypeLabel(log.businessType) }}
          </el-descriptions-item>
          <el-descriptions-item label="操作人">
            {{ log.operUserName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="操作人 ID">
            {{ log.operUserId ?? '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="操作类别">
            {{ operatorTypeLabel(log.operatorType) }}
          </el-descriptions-item>
          <el-descriptions-item label="操作 IP">
            {{ log.operIp || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="操作地点">
            {{ log.operLocation || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="请求方式">
            {{ log.requestMethod || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="方法名称" :span="2">
            <code class="method-code">{{ log.method || '—' }}</code>
          </el-descriptions-item>
          <el-descriptions-item
            v-if="log.status === 0 && log.errorMsg"
            label="备注"
            :span="2"
          >
            {{ log.errorMsg }}
          </el-descriptions-item>
        </el-descriptions>

        <section class="json-section">
          <div class="json-head">
            <h4>请求参数</h4>
            <el-tag type="info" effect="plain" size="small">operParam</el-tag>
          </div>
          <pre class="json-block">{{ requestParamText }}</pre>
        </section>

        <section class="json-section">
          <div class="json-head">
            <h4>返回参数</h4>
            <el-tag type="info" effect="plain" size="small">jsonResult</el-tag>
          </div>
          <pre class="json-block">{{ responseText }}</pre>
        </section>
      </template>
    </div>

    <template v-if="log && !errorMessage" #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.oper-log-detail {
  min-height: 220px;
}

.readonly-alert,
.error-alert {
  margin-bottom: 14px;
}

.detail-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.log-meta {
  margin-bottom: 18px;
}

.log-meta :deep(.el-descriptions__label) {
  width: 102px;
  color: var(--hp-text-secondary);
}

.method-code {
  padding: 2px 6px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11px;
  color: var(--hp-primary-dark);
  word-break: break-all;
  background: #f1f8f5;
  border-radius: 5px;
}

.json-section {
  margin-bottom: 16px;
}

.json-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.json-head h4 {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--hp-text);
}

.json-block {
  max-height: 260px;
  padding: 12px 14px;
  margin: 0;
  overflow: auto;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11.5px;
  line-height: 1.65;
  color: #3d5750;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f7faf9;
  border: 1px solid #e7f0ec;
  border-radius: 9px;
}

.detail-error {
  padding-top: 24px;
}
</style>
