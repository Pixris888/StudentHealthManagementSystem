<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getMaterialDetail } from '@/api/knowledge'
import { auditStatusTagType, enableStatusTagType } from '@/utils/knowledge'
import { formatDateTime } from '@/utils/format'
import type { MaterialVO } from '@/types/knowledge'

const props = defineProps<{
  modelValue: boolean
  materialId?: number | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const loading = ref(false)
const material = ref<MaterialVO | null>(null)

const keywords = computed(() =>
  (material.value?.keyword ?? '')
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean),
)

async function loadMaterial(): Promise<void> {
  if (!props.materialId) {
    material.value = null
    return
  }
  loading.value = true
  material.value = null
  try {
    material.value = await getMaterialDetail(props.materialId)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.materialId] as const,
  ([open]) => {
    if (open) loadMaterial()
  },
)
</script>

<template>
  <el-drawer
    v-model="visible"
    title="AI 知识素材预览"
    size="680px"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading" class="preview-body">
      <template v-if="material">
        <div class="preview-tags">
          <el-tag
            :type="enableStatusTagType(material.status)"
            effect="light"
          >
            {{ material.statusName }}
          </el-tag>
          <el-tag
            :type="auditStatusTagType(material.auditStatus)"
            effect="plain"
          >
            {{ material.auditStatusName }}
          </el-tag>
          <el-tag v-if="material.categoryName" type="primary" effect="plain">
            {{ material.categoryName }}
          </el-tag>
        </div>

        <h2>{{ material.title }}</h2>
        <div class="preview-meta">
          <span>来源：{{ material.source || '—' }}</span>
          <span>更新时间：{{ formatDateTime(material.updateTime) }}</span>
        </div>

        <el-alert
          v-if="material.auditStatus === 2 && material.auditRemark"
          :title="`审核不通过：${material.auditRemark}`"
          type="error"
          :closable="false"
          show-icon
          class="preview-alert"
        />

        <div v-if="keywords.length" class="keyword-list">
          <span class="keyword-label">关键词</span>
          <el-tag
            v-for="keyword in keywords"
            :key="keyword"
            type="info"
            effect="plain"
            size="small"
          >
            {{ keyword }}
          </el-tag>
        </div>

        <div class="material-content">{{ material.content ?? '（暂无内容）' }}</div>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="审核意见">
            {{ material.auditRemark || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(material.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="备注">
            {{ material.remark || '—' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <el-empty
        v-else-if="!loading"
        description="未获取到素材内容"
        :image-size="90"
      />
    </div>
  </el-drawer>
</template>

<style scoped>
.preview-body {
  min-height: 240px;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-body h2 {
  margin: 12px 0 10px;
  font-size: 22px;
  line-height: 1.45;
  color: var(--hp-text);
}

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 12.5px;
  color: var(--hp-text-muted);
}

.preview-alert {
  margin-top: 18px;
}

.keyword-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  padding: 14px 16px;
  margin-top: 20px;
  background: #f6faf8;
  border-radius: 10px;
}

.keyword-label {
  margin-right: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--hp-text-secondary);
}

.material-content {
  padding: 22px 0;
  font-size: 14px;
  line-height: 1.95;
  color: var(--hp-text);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
