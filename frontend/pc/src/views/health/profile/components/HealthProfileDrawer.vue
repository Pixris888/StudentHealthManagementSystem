<script setup lang="ts">
import { computed } from 'vue'
import { useIsMobile } from '@/utils/useIsMobile'
import {
  bmiLevelTagType,
  formatDate,
  formatDateTime,
  formatNumber,
  formatValue,
  genderLabel,
  genderTagType,
} from '@/utils/health'
import type { HealthProfile } from '@/types/health'

const visible = defineModel<boolean>({ required: true })

const props = defineProps<{
  profile: HealthProfile | null
  loading?: boolean
  desensitized?: boolean
}>()

const isMobile = useIsMobile()
const drawerSize = computed(() => (isMobile.value ? '92%' : '560px'))
const avatarText = computed(() => (props.profile?.realName || '?').charAt(0))
</script>

<template>
  <el-drawer
    v-model="visible"
    title="学生健康档案"
    :size="drawerSize"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading" class="profile-drawer-body">
      <template v-if="profile">
        <div class="drawer-hero">
          <el-avatar :size="54" class="drawer-avatar">
            {{ avatarText }}
          </el-avatar>
          <div class="drawer-identity">
            <h3>{{ profile.realName || '—' }}</h3>
            <p>{{ profile.studentNo || '—' }}</p>
            <div class="identity-tags">
              <el-tag :type="genderTagType(profile.gender)" size="small" effect="light">
                {{ genderLabel(profile.gender) }}
              </el-tag>
              <el-tag v-if="profile.age != null" size="small" type="info" effect="plain">
                {{ profile.age }} 岁
              </el-tag>
              <el-tag
                v-if="profile.bmiLevel"
                :type="bmiLevelTagType(profile.bmiLevel)"
                size="small"
                effect="light"
              >
                {{ profile.bmiLevel }}
              </el-tag>
            </div>
          </div>
          <div class="drawer-bmi">
            <span>BMI</span>
            <strong>{{ formatNumber(profile.bmi) }}</strong>
          </div>
        </div>

        <el-alert
          v-if="desensitized"
          title="校医视角下学号、姓名、联系电话等身份信息已脱敏"
          type="info"
          :closable="false"
          show-icon
          class="mask-alert"
        />

        <el-descriptions title="基础信息" :column="2" border class="drawer-section">
          <el-descriptions-item label="学号">
            {{ profile.studentNo || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="姓名">
            {{ profile.realName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ profile.genderName || genderLabel(profile.gender) }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄">
            {{ formatValue(profile.age, '岁') }}
          </el-descriptions-item>
          <el-descriptions-item label="出生日期">
            {{ formatDate(profile.birthDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="血型">
            {{ profile.bloodType || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话" :span="2">
            {{ profile.phone || '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="学籍信息" :column="2" border class="drawer-section">
          <el-descriptions-item label="学院">
            {{ profile.college || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="专业">
            {{ profile.major || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="年级">
            {{ profile.grade || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="班级">
            {{ profile.className || '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="身体指标" :column="2" border class="drawer-section">
          <el-descriptions-item label="身高">
            {{ formatValue(profile.height, 'cm') }}
          </el-descriptions-item>
          <el-descriptions-item label="体重">
            {{ formatValue(profile.weight, 'kg') }}
          </el-descriptions-item>
          <el-descriptions-item label="BMI">
            {{ formatNumber(profile.bmi) }}
          </el-descriptions-item>
          <el-descriptions-item label="BMI 等级">
            {{ profile.bmiLevel || '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="健康风险" :column="1" border class="drawer-section">
          <el-descriptions-item label="慢性病史">
            {{ profile.chronicDisease || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="过敏史">
            {{ profile.allergyHistory || '未填写' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="紧急联系人" :column="2" border class="drawer-section">
          <el-descriptions-item label="联系人">
            {{ profile.emergencyContact || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ profile.emergencyPhone || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            {{ profile.remark || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间" :span="2">
            {{ formatDateTime(profile.updateTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <el-empty v-else description="暂无档案数据" :image-size="90" />
    </div>
  </el-drawer>
</template>

<style scoped>
.profile-drawer-body {
  min-height: 240px;
}

.drawer-hero {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  margin-bottom: 18px;
  background:
    radial-gradient(circle at 100% 0%, rgba(61, 220, 151, 0.15), transparent 38%),
    #f4fbf8;
  border: 1px solid #dcefe7;
  border-radius: 14px;
}

.drawer-avatar {
  flex-shrink: 0;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #1f9d7a, #3ddc97);
}

.drawer-identity {
  flex: 1;
  min-width: 0;
}

.drawer-identity h3 {
  margin-bottom: 3px;
  font-size: 17px;
  font-weight: 700;
  color: var(--hp-text);
}

.drawer-identity p {
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--hp-text-muted);
}

.identity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.drawer-bmi {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
  color: var(--hp-text-muted);
  font-size: 11px;
}

.drawer-bmi strong {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--hp-primary-dark);
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
    flex-wrap: wrap;
  }

  .drawer-bmi {
    align-items: flex-start;
    width: 100%;
    padding-top: 8px;
    border-top: 1px dashed #dcefe7;
  }
}
</style>
