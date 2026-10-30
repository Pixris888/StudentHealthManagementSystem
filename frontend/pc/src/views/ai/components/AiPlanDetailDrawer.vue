<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Check,
  Delete,
} from '@element-plus/icons-vue'
import MarkdownView from '@/components/markdown/MarkdownView.vue'
import {
  adoptPlan,
  deleteMyPlans,
  getMyPlan,
} from '@/api/ai'
import { useIsMobile } from '@/utils/useIsMobile'
import {
  planTypeLabel,
  providerLabel,
  resolveAiError,
} from '@/utils/ai'
import { formatDate, formatDateTime } from '@/utils/format'
import type { AiHealthPlanVO } from '@/types/ai'
import AiStatusTag from './AiStatusTag.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    planId?: number | null
    initialPlan?: AiHealthPlanVO | null
  }>(),
  {
    planId: null,
    initialPlan: null,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'updated', plan: AiHealthPlanVO): void
  (e: 'deleted', id: number): void
}>()

const isMobile = useIsMobile()
const drawerSize = computed(() => (isMobile.value ? '94%' : '68%'))

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const plan = ref<AiHealthPlanVO | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const adopting = ref(false)
const deleting = ref(false)

const typeText = computed(() =>
  plan.value
    ? plan.value.planTypeName || planTypeLabel(plan.value.planType)
    : '—',
)

const periodText = computed(() => {
  if (!plan.value) return '—'
  if (plan.value.startDate && plan.value.endDate) {
    return `${formatDate(plan.value.startDate)} ~ ${formatDate(plan.value.endDate)}`
  }
  return '未设置'
})

const baseCheckinText = computed(() => {
  if (!plan.value) return '—'
  return plan.value.baseCheckinDate
    ? `基于 ${formatDate(plan.value.baseCheckinDate)} 的打卡数据`
    : '通用计划（未使用近期打卡数据）'
})

const modelText = computed(() => {
  if (!plan.value) return '—'
  const parts = [
    providerLabel(plan.value.provider),
    plan.value.modelName,
  ].filter(Boolean)
  return parts.length ? parts.join(' · ') : '—'
})

async function loadPlan(): Promise<void> {
  if (!props.planId) {
    plan.value = null
    return
  }
  if (
    props.initialPlan &&
    props.initialPlan.id === props.planId &&
    props.initialPlan.planContent
  ) {
    plan.value = { ...props.initialPlan }
    errorMessage.value = ''
    loading.value = false
    return
  }
  loading.value = true
  errorMessage.value = ''
  plan.value = null
  try {
    plan.value = await getMyPlan(props.planId)
  } catch (error) {
    errorMessage.value = resolveAiError(error, '计划加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function handleAdopt(): Promise<void> {
  if (!plan.value || adopting.value) return
  try {
    await ElMessageBox.confirm(
      '采纳后会标记为已采纳，方便你按计划执行。是否继续？',
      '采纳计划',
      {
        type: 'info',
        confirmButtonText: '采纳计划',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  adopting.value = true
  try {
    const updated = await adoptPlan(plan.value.id)
    plan.value = updated
    ElMessage.success('计划已采纳')
    emit('updated', updated)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    adopting.value = false
  }
}

async function handleDelete(): Promise<void> {
  if (!plan.value || deleting.value) return
  try {
    await ElMessageBox.confirm(
      `确定删除计划“${plan.value.planTitle || typeText.value}”吗？删除后不可恢复。`,
      '删除计划',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return
  }
  deleting.value = true
  try {
    await deleteMyPlans([plan.value.id])
    ElMessage.success('计划已删除')
    emit('deleted', plan.value.id)
    visible.value = false
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    deleting.value = false
  }
}

watch(
  () => [props.modelValue, props.planId] as const,
  ([open]) => {
    if (open) loadPlan()
  },
)
</script>

<template>
  <el-drawer
    v-model="visible"
    title="健康计划详情"
    :size="drawerSize"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading" class="plan-drawer-body">
      <div v-if="errorMessage" class="plan-error">
        <el-result
          icon="warning"
          title="计划无法查看"
          :sub-title="errorMessage"
        >
          <template #extra>
            <el-button type="primary" @click="loadPlan">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <template v-else-if="plan">
        <div class="plan-hero">
          <div class="plan-hero-main">
            <span class="plan-eyebrow">PERSONAL HEALTH PLAN</span>
            <h3>{{ plan.planTitle || typeText }}</h3>
            <div class="plan-tags">
              <el-tag type="primary" effect="light">{{ typeText }}</el-tag>
              <AiStatusTag :status="plan.status" kind="plan" />
              <el-tag v-if="plan.provider" type="info" effect="plain">
                {{ providerLabel(plan.provider) }}
              </el-tag>
            </div>
          </div>
        </div>

        <el-descriptions
          :column="isMobile ? 1 : 2"
          border
          class="plan-meta"
        >
          <el-descriptions-item label="计划周期">
            {{ periodText }}
          </el-descriptions-item>
          <el-descriptions-item label="生成依据">
            {{ baseCheckinText }}
          </el-descriptions-item>
          <el-descriptions-item label="生成时间">
            {{ formatDateTime(plan.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="采纳时间">
            {{ formatDateTime(plan.adoptedTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="生成模型">
            {{ modelText }}
          </el-descriptions-item>
          <el-descriptions-item label="计划状态">
            <AiStatusTag :status="plan.status" kind="plan" effect="plain" />
          </el-descriptions-item>
        </el-descriptions>

        <section class="plan-content">
          <h4>计划内容</h4>
          <div class="plan-markdown">
            <MarkdownView
              v-if="plan.planContent"
              :content="plan.planContent"
            />
            <el-empty
              v-else
              description="该计划没有可展示的正文内容"
              :image-size="80"
            />
          </div>
        </section>

        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="温馨提示"
          description="健康计划由 AI 结合健康档案与近期打卡生成，仅供日常健康管理参考；如身体不适，请及时就医。"
          class="plan-tip"
        />
      </template>
    </div>

    <template v-if="plan && !errorMessage" #footer>
      <div class="plan-footer">
        <el-button
          type="danger"
          plain
          :icon="Delete"
          :loading="deleting"
          @click="handleDelete"
        >
          删除计划
        </el-button>
        <div class="plan-footer-right">
          <el-button @click="visible = false">关闭</el-button>
          <el-button
            v-if="plan.status === 0"
            type="primary"
            :icon="Check"
            :loading="adopting"
            @click="handleAdopt"
          >
            采纳计划
          </el-button>
          <el-tag v-else type="success" effect="light">已采纳</el-tag>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.plan-drawer-body {
  min-height: 240px;
}

.plan-hero {
  padding: 18px 20px;
  margin-bottom: 18px;
  background:
    radial-gradient(circle at 100% 0%, rgba(61, 220, 151, 0.16), transparent 45%),
    linear-gradient(135deg, #f3fbf7, #eef8f3);
  border: 1px solid #dcefe7;
  border-radius: 14px;
}

.plan-eyebrow {
  display: block;
  margin-bottom: 6px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #8caaa0;
}

.plan-hero h3 {
  margin-bottom: 10px;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--hp-text);
}

.plan-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.plan-meta {
  margin-bottom: 20px;
}

.plan-meta :deep(.el-descriptions__label) {
  width: 96px;
  color: var(--hp-text-secondary);
}

.plan-content h4 {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.plan-markdown {
  padding: 18px 20px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid var(--hp-border);
  border-radius: 12px;
}

.plan-tip {
  margin-bottom: 8px;
}

.plan-error {
  padding-top: 30px;
}

.plan-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  width: 100%;
}

.plan-footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

@media (max-width: 520px) {
  .plan-markdown {
    padding: 14px;
  }

  .plan-footer {
    justify-content: center;
  }

  .plan-footer-right {
    margin-left: 0;
  }
}
</style>
