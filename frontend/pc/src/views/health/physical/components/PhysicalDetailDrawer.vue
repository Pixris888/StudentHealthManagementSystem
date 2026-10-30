<script setup lang="ts">
import { computed } from 'vue'
import { useIsMobile } from '@/utils/useIsMobile'
import {
  bmiLevelOf,
  bmiLevelTagType,
  formatDate,
  formatDateTime,
  formatNumber,
  formatValue,
  gradeLevelTagType,
} from '@/utils/health'
import type { PhysicalTest } from '@/types/health'

const visible = defineModel<boolean>({ required: true })

const props = defineProps<{
  record: PhysicalTest | null
  loading?: boolean
  desensitized?: boolean
}>()

const isMobile = useIsMobile()
const drawerSize = computed(() => (isMobile.value ? '92%' : '560px'))
const bmiLevel = computed(() => bmiLevelOf(props.record?.bmi))
</script>

<template>
  <el-drawer
    v-model="visible"
    title="体测记录详情"
    :size="drawerSize"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading" class="physical-drawer-body">
      <template v-if="record">
        <div class="drawer-hero">
          <div class="hero-main">
            <span class="hero-eyebrow">PHYSICAL TEST</span>
            <h3>
              {{ record.testYear }}
              <small>{{ record.testTerm || '—' }}</small>
            </h3>
            <p>{{ record.studentNo || '—' }}</p>
            <div class="hero-tags">
              <el-tag
                v-if="record.gradeLevel"
                :type="gradeLevelTagType(record.gradeLevel)"
                size="small"
                effect="light"
              >
                {{ record.gradeLevel }}
              </el-tag>
              <el-tag
                v-if="bmiLevel"
                :type="bmiLevelTagType(bmiLevel)"
                size="small"
                effect="plain"
              >
                BMI {{ bmiLevel }}
              </el-tag>
            </div>
          </div>
          <div class="hero-score">
            <span>总分</span>
            <strong>{{ formatNumber(record.totalScore) }}</strong>
            <em>分</em>
          </div>
        </div>

        <el-alert
          v-if="desensitized"
          title="校医视角下学号已脱敏"
          type="info"
          :closable="false"
          show-icon
          class="mask-alert"
        />

        <el-descriptions title="测试信息" :column="2" border class="drawer-section">
          <el-descriptions-item label="测试年份">
            {{ record.testYear || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="测试学期">
            {{ record.testTerm || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="测试日期">
            {{ formatDate(record.testDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="等级">
            <el-tag
              v-if="record.gradeLevel"
              :type="gradeLevelTagType(record.gradeLevel)"
              size="small"
              effect="light"
            >
              {{ record.gradeLevel }}
            </el-tag>
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item label="总分" :span="2">
            {{ formatValue(record.totalScore, '分') }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="身体指标" :column="2" border class="drawer-section">
          <el-descriptions-item label="身高">
            {{ formatValue(record.height, 'cm') }}
          </el-descriptions-item>
          <el-descriptions-item label="体重">
            {{ formatValue(record.weight, 'kg') }}
          </el-descriptions-item>
          <el-descriptions-item label="BMI">
            {{ formatNumber(record.bmi) }}
          </el-descriptions-item>
          <el-descriptions-item label="肺活量">
            {{ formatValue(record.vitalCapacity, 'ml') }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="体能项目" :column="2" border class="drawer-section">
          <el-descriptions-item label="50 米跑">
            {{ formatValue(record.run50m, '秒') }}
          </el-descriptions-item>
          <el-descriptions-item label="坐位体前屈">
            {{ formatValue(record.sitAndReach, 'cm') }}
          </el-descriptions-item>
          <el-descriptions-item label="立定跳远">
            {{ formatValue(record.standingJump, 'cm') }}
          </el-descriptions-item>
          <el-descriptions-item label="耐力跑">
            {{ formatValue(record.enduranceRun, '秒') }}
          </el-descriptions-item>
          <el-descriptions-item label="力量项目" :span="2">
            {{ formatValue(record.strengthItem, '个') }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="记录信息" :column="1" border class="drawer-section">
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(record.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <el-empty v-else description="暂无体测记录" :image-size="90" />
    </div>
  </el-drawer>
</template>

<style scoped>
.physical-drawer-body {
  min-height: 240px;
}

.drawer-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 20px;
  margin-bottom: 18px;
  color: #eafff7;
  background:
    radial-gradient(circle at 100% 0%, rgba(61, 220, 151, 0.22), transparent 38%),
    linear-gradient(120deg, #0d4439, #14745a);
  border-radius: 16px;
}

.hero-eyebrow {
  font-size: 8.5px;
  font-weight: 600;
  letter-spacing: 2px;
  color: rgba(234, 255, 247, 0.5);
}

.hero-main h3 {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 6px 0 4px;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.hero-main h3 small {
  font-size: 12px;
  font-weight: 500;
  color: rgba(234, 255, 247, 0.6);
}

.hero-main p {
  margin-bottom: 10px;
  font-size: 12px;
  color: rgba(234, 255, 247, 0.55);
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.hero-score {
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  align-items: flex-end;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
}

.hero-score span {
  font-size: 10px;
  color: rgba(234, 255, 247, 0.5);
}

.hero-score strong {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
  color: #fff;
}

.hero-score em {
  font-size: 10px;
  font-style: normal;
  color: rgba(234, 255, 247, 0.5);
}

.mask-alert {
  margin-bottom: 18px;
}

.drawer-section {
  margin-bottom: 20px;
}

.drawer-section :deep(.el-descriptions__title) {
  font-size: 14px;
  font-weight: 700;
  color: var(--hp-text);
}

.drawer-section :deep(.el-descriptions__label) {
  width: 100px;
  color: var(--hp-text-secondary);
}

.drawer-section :deep(.el-descriptions__content) {
  color: var(--hp-text);
}

@media (max-width: 500px) {
  .drawer-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-score {
    flex-direction: row;
    align-items: baseline;
    width: 100%;
    gap: 5px;
  }
}
</style>
