<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Collection,
  Refresh,
  View,
} from '@element-plus/icons-vue'
import { listUnanswered } from '@/api/ai'
import {
  feedbackLabel,
  feedbackTagType,
} from '@/utils/ai'
import { formatDateTime } from '@/utils/format'
import type { AiConsultationVO } from '@/types/ai'
import AiConsultationDetailDrawer from './AiConsultationDetailDrawer.vue'
import AiStatusTag from './AiStatusTag.vue'

const router = useRouter()

const records = ref<AiConsultationVO[]>([])
const loading = ref(false)
const limit = ref(20)
const detailVisible = ref(false)
const detailId = ref<number | null>(null)

async function loadRecords(): Promise<void> {
  loading.value = true
  try {
    records.value = await listUnanswered(limit.value)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

function studentLabel(row: AiConsultationVO): string {
  if (row.realName || row.studentNo) {
    const name = row.realName || '—'
    return row.studentNo ? `${name} · ${row.studentNo}` : name
  }
  return row.userId ? `用户 #${row.userId}` : '学生信息不可用'
}

function openDetail(row: AiConsultationVO): void {
  detailId.value = row.id
  detailVisible.value = true
}

function goMaterials(): void {
  // 复用健康知识库的素材管理页，不在 AI 模块内维护素材
  router.push('/health/article/manage/materials')
}

onMounted(loadRecords)
</script>

<template>
  <section class="panel-card unanswered-panel">
    <div class="toolbar">
      <div class="toolbar-left">
        <span class="panel-title">未命中 / 差评问题</span>
        <el-tag type="info" effect="plain">
          共 {{ records.length }} 条
        </el-tag>
      </div>
      <div class="toolbar-right">
        <el-select
          v-model="limit"
          size="small"
          style="width: 126px"
          @change="loadRecords"
        >
          <el-option :value="20" label="最近 20 条" />
          <el-option :value="50" label="最近 50 条" />
          <el-option :value="100" label="最近 100 条" />
        </el-select>
        <el-button
          :icon="Refresh"
          circle
          size="small"
          title="刷新列表"
          :loading="loading"
          @click="loadRecords"
        />
      </div>
    </div>

    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="这些问题需要知识库补充"
      description="列表包含使用备用方案的问答、回答失败的问题，以及被学生评价为“没帮助”的记录。可前往 AI 知识素材管理补充相应知识。"
      class="unanswered-alert"
    />

    <div v-loading="loading" class="unanswered-list">
      <template v-if="records.length">
        <article
          v-for="row in records"
          :key="row.id"
          class="unanswered-card"
        >
          <div class="card-head">
            <p class="card-question">{{ row.question }}</p>
            <AiStatusTag :status="row.answerStatus" />
          </div>

          <div class="card-meta">
            <span>{{ studentLabel(row) }}</span>
            <span>{{ formatDateTime(row.consultTime) }}</span>
            <el-tag
              :type="feedbackTagType(row.feedback)"
              effect="plain"
              size="small"
            >
              {{ feedbackLabel(row.feedback) }}
            </el-tag>
            <el-tag
              v-if="row.emergency === 1"
              type="danger"
              effect="dark"
              size="small"
            >
              紧急
            </el-tag>
            <el-tag v-if="row.fallback" type="warning" size="small">
              备用方案
            </el-tag>
          </div>

          <p v-if="row.errorMsg" class="card-error">
            错误信息：{{ row.errorMsg }}
          </p>

          <div class="card-actions">
            <el-button
              link
              type="primary"
              :icon="View"
              @click="openDetail(row)"
            >
              查看详情
            </el-button>
            <el-button
              link
              type="success"
              :icon="Collection"
              @click="goMaterials"
            >
              去补充知识素材
            </el-button>
          </div>
        </article>
      </template>

      <el-empty
        v-else-if="!loading"
        description="暂无未命中或差评问题，知识库表现不错"
        :image-size="92"
      />
    </div>

    <AiConsultationDetailDrawer
      v-model="detailVisible"
      :consultation-id="detailId"
    />
  </section>
</template>

<style scoped>
.unanswered-panel {
  padding: 18px 20px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.unanswered-alert {
  margin-bottom: 16px;
}

.unanswered-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 160px;
}

.unanswered-card {
  padding: 14px 16px;
  background: #fff;
  border: 1px solid var(--hp-border);
  border-radius: 12px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.unanswered-card:hover {
  border-color: #cbe8dc;
  box-shadow: 0 8px 22px rgba(23, 51, 44, 0.05);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 8px;
}

.card-question {
  flex: 1;
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.6;
  color: var(--hp-text);
  word-break: break-word;
}

.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
  color: var(--hp-text-muted);
}

.card-error {
  padding: 8px 10px;
  margin-top: 8px;
  font-size: 11.5px;
  line-height: 1.6;
  color: #b44a4a;
  background: #fdf3f3;
  border-radius: 7px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 9px;
  margin-top: 9px;
  border-top: 1px dashed #eef4f1;
}

.card-actions :deep(.el-button + .el-button) {
  margin-left: 4px;
}

@media (max-width: 768px) {
  .unanswered-panel {
    padding: 14px 12px;
  }

  .card-head {
    flex-direction: column;
    gap: 6px;
  }
}
</style>
