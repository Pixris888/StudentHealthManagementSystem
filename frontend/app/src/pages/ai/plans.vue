<template>
  <view class="page">
    <view class="plan-intro">
      <text class="plan-intro-title">把建议变成可执行的计划</text>
      <text class="plan-intro-desc">
        根据健康档案和最近 7 天打卡，生成作息、运动或综合计划。生成后可随时回来查看或采纳。
      </text>
    </view>

    <view class="generate-grid">
      <view
        v-for="item in planOptions"
        :key="item.value"
        class="generate-card"
        :class="{ generating: generatingType === item.value }"
        @click="generate(item.value)"
      >
        <view class="generate-icon">{{ item.emoji }}</view>
        <text class="generate-title">{{ item.label }}</text>
        <text class="generate-desc">{{ item.desc }}</text>
        <view v-if="generatingType === item.value" class="generate-loading">
          <uni-load-more status="loading" color="#0f9d8a" :content-text="{ contentloading: '生成中' }" />
        </view>
      </view>
    </view>

    <view class="section-head">
      <view>
        <text class="section-title">历史计划</text>
        <text class="section-desc">共 {{ total }} 份计划</text>
      </view>
      <text class="section-link" @click="load(true)">刷新</text>
    </view>

    <view v-if="loading && !plans.length" class="card loading-card">
      <uni-load-more status="loading" color="#0f9d8a" />
    </view>

    <template v-else>
      <view
        v-for="plan in plans"
        :key="plan.id"
        class="card plan-card"
        @click="openDetail(plan)"
      >
        <view class="between">
          <view class="row" style="gap: 10rpx">
            <text class="plan-type-emoji">{{ planEmoji(plan.planType) }}</text>
            <text class="plan-type">{{ plan.planTypeName || planTypeLabel(plan.planType) }}</text>
          </view>
          <view class="tag" :class="plan.status === 1 ? 'tag-success' : 'tag-info'">
            {{ plan.status === 1 ? '已采纳' : '未采纳' }}
          </view>
        </view>
        <text class="plan-title text-clamp-2">
          {{ plan.planTitle || '健康计划' }}
        </text>
        <view class="plan-meta">
          <text>{{ plan.startDate ? formatDate(plan.startDate, 'M 月 D 日') : '—' }}</text>
          <text>至</text>
          <text>{{ plan.endDate ? formatDate(plan.endDate, 'M 月 D 日') : '—' }}</text>
          <text v-if="plan.createTime">· {{ formatDateTimeShort(plan.createTime) }}</text>
        </view>
        <view class="plan-actions">
          <button class="btn btn-plain btn-sm" @click.stop="openDetail(plan)">查看详情</button>
          <button
            v-if="plan.status !== 1"
            class="btn btn-ghost btn-sm"
            @click.stop="adopt(plan)"
          >
            采纳计划
          </button>
          <button class="btn btn-danger btn-sm" @click.stop="remove(plan)">删除</button>
        </view>
      </view>

      <EmptyState
        v-if="!plans.length"
        icon="star"
        title="还没有健康计划"
        desc="点击上方卡片，让 AI 帮你生成一份吧"
      />

      <view v-if="plans.length" class="load-more">
        <uni-load-more
          :status="loading ? 'loading' : hasMore ? 'more' : 'noMore'"
          color="#94a3b8"
        />
      </view>
    </template>

    <view v-if="detailPlan" class="detail-mask" @click="closeDetail">
      <view class="detail-sheet" @click.stop>
        <view class="detail-head">
          <view class="grow">
            <text class="detail-eyebrow">{{ detailPlan.planTypeName || planTypeLabel(detailPlan.planType) }}</text>
            <text class="detail-title">{{ detailPlan.planTitle || '健康计划' }}</text>
          </view>
          <view class="detail-close" @click="closeDetail">
            <uni-icons type="closeempty" size="20" color="#64748b" />
          </view>
        </view>
        <scroll-view scroll-y class="detail-body">
          <view class="detail-meta">
            <text>{{ detailPlan.startDate || '—' }} 至 {{ detailPlan.endDate || '—' }}</text>
            <text>{{ detailPlan.status === 1 ? '已采纳' : '未采纳' }}</text>
          </view>
          <rich-text
            v-if="detailPlan.planContent"
            class="detail-content"
            :nodes="renderMarkdown(detailPlan.planContent)"
          />
          <text v-else class="muted">暂无计划正文</text>
        </scroll-view>
        <view class="detail-footer">
          <button
            v-if="detailPlan.status !== 1"
            class="btn btn-primary btn-block"
            :disabled="adopting"
            @click="adopt(detailPlan)"
          >
            {{ adopting ? '采纳中…' : '采纳这份计划' }}
          </button>
          <button v-else class="btn btn-plain btn-block" @click="closeDetail">
            已采纳，关闭
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import EmptyState from '@/components/EmptyState.vue'
import {
  adoptPlan,
  deleteMyPlans,
  generatePlan,
  pageMyPlans,
} from '@/api/ai'
import { getAccessToken } from '@/utils/auth'
import { PLAN_TYPE_OPTIONS, planTypeLabel } from '@/utils/ai'
import { formatDate, formatDateTimeShort } from '@/utils/format'
import { renderMarkdown } from '@/utils/markdown'
import type { AiHealthPlanVO, PlanType } from '@/types/ai'

const planOptions = PLAN_TYPE_OPTIONS.map((item) => ({
  ...item,
  emoji: item.value === 1 ? '🌙' : item.value === 2 ? '🏃' : '🌟',
}))
const plans = ref<AiHealthPlanVO[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const generatingType = ref<PlanType | null>(null)
const detailPlan = ref<AiHealthPlanVO | null>(null)
const adopting = ref(false)

const hasMore = computed(() => plans.value.length < total.value)

function ensureLogin(): boolean {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

function planEmoji(type: PlanType): string {
  return type === 1 ? '🌙' : type === 2 ? '🏃' : '🌟'
}

async function load(reset = false): Promise<void> {
  if (loading.value) return
  if (!ensureLogin()) return
  if (reset) {
    page.value = 1
    total.value = 0
    plans.value = []
  }
  loading.value = true
  try {
    const result = await pageMyPlans({ page: page.value, size })
    const list = result.records ?? []
    plans.value = reset ? list : [...plans.value, ...list]
    total.value = result.total ?? plans.value.length
  } catch {
    // 请求层已提示
  } finally {
    loading.value = false
  }
}

function confirm(content: string, title = '提示'): Promise<boolean> {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      success: (res) => resolve(Boolean(res.confirm)),
      fail: () => resolve(false),
    })
  })
}

async function generate(type: PlanType): Promise<void> {
  if (generatingType.value) return
  const option = planOptions.find((item) => item.value === type)
  const confirmed = await confirm(
    `将根据你的健康档案和最近 7 天打卡生成「${option?.label || '健康'}」。生成过程可能需要 10-60 秒，请保持网络畅通。`,
    '生成健康计划',
  )
  if (!confirmed) return

  generatingType.value = type
  uni.showLoading({ title: 'AI 生成中…', mask: true })
  try {
    const created = await generatePlan({ planType: type })
    uni.hideLoading()
    uni.showToast({ title: '计划已生成', icon: 'success' })
    await load(true)
    if (created) detailPlan.value = created
  } catch {
    uni.hideLoading()
  } finally {
    generatingType.value = null
  }
}

function openDetail(plan: AiHealthPlanVO): void {
  detailPlan.value = plan
}

function closeDetail(): void {
  detailPlan.value = null
}

async function adopt(plan: AiHealthPlanVO): Promise<void> {
  if (adopting.value) return
  if (plan.status === 1) return
  const confirmed = await confirm('采纳后计划会标记为进行中，是否继续？', '采纳计划')
  if (!confirmed) return
  adopting.value = true
  try {
    const updated = await adoptPlan(plan.id)
    uni.showToast({ title: '计划已采纳', icon: 'success' })
    const index = plans.value.findIndex((item) => item.id === plan.id)
    if (index >= 0 && updated) plans.value[index] = updated
    if (detailPlan.value?.id === plan.id && updated) detailPlan.value = updated
  } catch {
    // 请求层已提示
  } finally {
    adopting.value = false
  }
}

async function remove(plan: AiHealthPlanVO): Promise<void> {
  const confirmed = await confirm('删除后不可恢复，确定删除这份计划吗？', '删除计划')
  if (!confirmed) return
  try {
    await deleteMyPlans([plan.id])
    uni.showToast({ title: '计划已删除', icon: 'success' })
    if (detailPlan.value?.id === plan.id) detailPlan.value = null
    await load(true)
  } catch {
    // 请求层已提示
  }
}

function loadMore(): void {
  if (loading.value || !hasMore.value) return
  page.value += 1
  load(false)
}

onShow(() => {
  if (!plans.value.length) load(true)
})

onPullDownRefresh(async () => {
  await load(true)
  uni.stopPullDownRefresh()
})

onReachBottom(loadMore)
</script>

<style scoped>
.plan-intro {
  padding: 10rpx 6rpx 22rpx;
}

.plan-intro-title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #0f172a;
}

.plan-intro-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.7;
  color: #94a3b8;
}

.generate-grid {
  display: flex;
  gap: 14rpx;
}

.generate-card {
  flex: 1;
  min-width: 0;
  padding: 22rpx 16rpx;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(15, 118, 110, 0.05);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.generate-card.generating {
  background: #f0fbf8;
}

.generate-icon {
  font-size: 34rpx;
  line-height: 1;
}

.generate-title {
  margin-top: 14rpx;
  font-size: 25rpx;
  font-weight: 700;
  color: #1e293b;
}

.generate-desc {
  margin-top: 6rpx;
  font-size: 19rpx;
  line-height: 1.5;
  color: #94a3b8;
}

.generate-loading {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
}

.loading-card {
  min-height: 240rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.plan-card {
  margin-bottom: 18rpx;
}

.plan-type-emoji {
  font-size: 26rpx;
}

.plan-type {
  font-size: 22rpx;
  font-weight: 600;
  color: #0f766e;
}

.plan-title {
  display: block;
  margin-top: 16rpx;
  font-size: 29rpx;
  font-weight: 700;
  line-height: 1.5;
  color: #0f172a;
}

.plan-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6rpx;
  margin-top: 10rpx;
  font-size: 21rpx;
  color: #94a3b8;
}

.plan-actions {
  display: flex;
  gap: 12rpx;
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #f0f5f3;
}

.load-more {
  padding: 10rpx 0 20rpx;
}

.detail-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  background: rgba(15, 23, 42, 0.42);
  display: flex;
  align-items: flex-end;
}

.detail-sheet {
  width: 100%;
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  background: #f6f9f8;
  border-radius: 36rpx 36rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.detail-head {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  padding: 30rpx 28rpx 20rpx;
  background: #ffffff;
  border-radius: 36rpx 36rpx 0 0;
}

.detail-eyebrow {
  display: block;
  font-size: 20rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
  color: #0f9d8a;
}

.detail-title {
  display: block;
  margin-top: 6rpx;
  font-size: 31rpx;
  font-weight: 700;
  line-height: 1.45;
  color: #0f172a;
}

.detail-close {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: #f1f5f4;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-body {
  flex: 1;
  height: 0;
  min-height: 0;
  padding: 24rpx 28rpx;
  background: #ffffff;
}

.detail-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 18rpx;
  border-radius: 14rpx;
  background: #f4faf8;
  font-size: 21rpx;
  color: #7b9a92;
}

.detail-content {
  display: block;
  margin-top: 18rpx;
}

.detail-footer {
  padding: 18rpx 28rpx calc(18rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  border-top: 1rpx solid #eef4f2;
}

.detail-footer .btn + .btn {
  margin-top: 12rpx;
}
</style>
