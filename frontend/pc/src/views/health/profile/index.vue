<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getHealthProfileDetail,
  getMyHealthProfile,
  pageHealthProfiles,
} from '@/api/health-profile'
import {
  bmiLevelTagType,
  formatDate,
  formatDateTime,
  formatNumber,
  formatValue,
  genderLabel,
  genderTagType,
  GENDER_OPTIONS,
} from '@/utils/health'
import type {
  HealthProfile,
  HealthProfileQuery,
} from '@/types/health'
import HealthProfileForm from './components/HealthProfileForm.vue'
import HealthProfileDrawer from './components/HealthProfileDrawer.vue'

const userStore = useUserStore()
const roles = computed(() => userStore.roles)
const isManageMode = computed(
  () =>
    roles.value.includes('DOCTOR_ADMIN') ||
    roles.value.includes('SUPER_ADMIN'),
)
const isDoctor = computed(
  () =>
    roles.value.includes('DOCTOR_ADMIN') &&
    !roles.value.includes('SUPER_ADMIN'),
)
const isStudentMode = computed(
  () => !isManageMode.value && userStore.userInfo?.userType === 1,
)

/* ---------------- 学生：我的健康档案 ---------------- */
const profile = ref<HealthProfile | null>(null)
const profileLoading = ref(false)
const loadError = ref('')
const editing = ref(false)

const hasProfile = computed(() => profile.value?.id != null)
const displayName = computed(
  () => profile.value?.realName || userStore.displayName,
)
const avatarText = computed(() => displayName.value.charAt(0))
const completeness = computed(() => {
  const item = profile.value
  if (!item) return 0
  const fields = [
    item.gender != null,
    item.birthDate,
    item.bloodType,
    item.height,
    item.weight,
    item.college,
    item.major,
    item.grade,
    item.className,
    item.emergencyContact,
    item.emergencyPhone,
  ]
  const filled = fields.filter(
    (value) => value !== null && value !== undefined && value !== '',
  ).length
  return Math.round((filled / fields.length) * 100)
})

async function loadMyProfile(): Promise<void> {
  profileLoading.value = true
  loadError.value = ''
  try {
    profile.value = await getMyHealthProfile()
  } catch (error) {
    loadError.value =
      error instanceof Error ? error.message : '健康档案加载失败，请稍后重试'
  } finally {
    profileLoading.value = false
  }
}

function startEdit(): void {
  editing.value = true
}

function handleSaved(updated: HealthProfile): void {
  profile.value = updated
  editing.value = false
}

/* ---------------- 校医/管理员：学生档案列表 ---------------- */
const list = ref<HealthProfile[]>([])
const total = ref(0)
const listLoading = ref(false)
const query = reactive<HealthProfileQuery>({
  page: 1,
  size: 10,
  studentNo: '',
  realName: '',
  college: '',
  major: '',
  grade: '',
  className: '',
  gender: undefined,
})

const drawerVisible = ref(false)
const detail = ref<HealthProfile | null>(null)
const detailLoading = ref(false)

async function loadProfiles(): Promise<void> {
  listLoading.value = true
  try {
    const page = await pageHealthProfiles({
      page: query.page,
      size: query.size,
      studentNo: query.studentNo?.trim() || undefined,
      realName: query.realName?.trim() || undefined,
      college: query.college?.trim() || undefined,
      major: query.major?.trim() || undefined,
      grade: query.grade?.trim() || undefined,
      className: query.className?.trim() || undefined,
      gender: query.gender,
    })
    list.value = page.records
    total.value = page.total
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    listLoading.value = false
  }
}

function handleSearch(): void {
  query.page = 1
  loadProfiles()
}

function resetQuery(): void {
  query.page = 1
  query.size = 10
  query.studentNo = ''
  query.realName = ''
  query.college = ''
  query.major = ''
  query.grade = ''
  query.className = ''
  query.gender = undefined
  loadProfiles()
}

function handleSizeChange(): void {
  query.page = 1
  loadProfiles()
}

async function openDetail(row: HealthProfile): Promise<void> {
  if (!row.userId) return
  drawerVisible.value = true
  detail.value = null
  detailLoading.value = true
  try {
    detail.value = await getHealthProfileDetail(row.userId)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  if (isManageMode.value) {
    loadProfiles()
  } else if (isStudentMode.value) {
    loadMyProfile()
  }
})
</script>

<template>
  <div class="page-container health-profile-page">
    <!-- 校医 / 超级管理员 -->
    <template v-if="isManageMode">
      <div class="page-heading">
        <div>
          <h2>学生健康档案</h2>
          <p>查看学生基础健康信息、身体指标、健康风险与紧急联系人</p>
        </div>
        <el-tag type="info" effect="plain">共 {{ total }} 份档案</el-tag>
      </div>

      <el-alert
        v-if="isDoctor"
        title="校医视角下学号、姓名、联系电话等身份信息已脱敏，体检指标保持完整"
        type="info"
        :closable="false"
        show-icon
      />

      <section class="panel-card search-card">
        <el-form :model="query" inline class="search-form" @submit.prevent>
          <el-form-item label="学号">
            <el-input
              v-model="query.studentNo"
              placeholder="请输入学号"
              clearable
              style="width: 160px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input
              v-model="query.realName"
              placeholder="请输入姓名"
              clearable
              style="width: 140px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="学院">
            <el-input
              v-model="query.college"
              placeholder="请输入学院"
              clearable
              style="width: 160px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="专业">
            <el-input
              v-model="query.major"
              placeholder="请输入专业"
              clearable
              style="width: 150px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="年级">
            <el-input
              v-model="query.grade"
              placeholder="如 2023 级"
              clearable
              style="width: 130px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="班级">
            <el-input
              v-model="query.className"
              placeholder="请输入班级"
              clearable
              style="width: 140px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="性别">
            <el-select
              v-model="query.gender"
              placeholder="全部"
              clearable
              style="width: 110px"
            >
              <el-option
                v-for="item in GENDER_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">
              查询
            </el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="panel-card table-card">
        <div class="toolbar">
          <div class="toolbar-left">
            <span class="table-title">档案列表</span>
            <el-tag type="info" effect="plain">仅展示已建档学生</el-tag>
          </div>
        </div>

        <el-table
          v-loading="listLoading"
          :data="list"
          row-key="id"
          style="width: 100%"
        >
          <el-table-column label="学号" min-width="140">
            <template #default="{ row }">
              {{ row.studentNo || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="姓名" width="110">
            <template #default="{ row }">
              {{ row.realName || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="性别" width="80" align="center">
            <template #default="{ row }">
              <el-tag
                :type="genderTagType(row.gender)"
                size="small"
                effect="light"
              >
                {{ row.genderName || genderLabel(row.gender) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="年龄" width="70" align="center">
            <template #default="{ row }">
              {{ formatValue(row.age, '岁') }}
            </template>
          </el-table-column>
          <el-table-column
            prop="college"
            label="学院"
            min-width="150"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.college || '—' }}
            </template>
          </el-table-column>
          <el-table-column
            prop="major"
            label="专业"
            min-width="140"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.major || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="BMI" width="130" align="center">
            <template #default="{ row }">
              <div class="bmi-cell">
                <strong>{{ formatNumber(row.bmi) }}</strong>
                <el-tag
                  v-if="row.bmiLevel"
                  :type="bmiLevelTagType(row.bmiLevel)"
                  size="small"
                  effect="plain"
                >
                  {{ row.bmiLevel }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">
              <span class="muted-text">{{ formatDateTime(row.updateTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <span class="selection-tip">
            数据由学生本人维护，校医和超级管理员仅做查看与统计
          </span>
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="loadProfiles"
            @size-change="handleSizeChange"
          />
        </div>
      </section>

      <HealthProfileDrawer
        v-model="drawerVisible"
        :profile="detail"
        :loading="detailLoading"
        :desensitized="isDoctor"
      />
    </template>

    <!-- 学生：我的健康档案 -->
    <template v-else-if="isStudentMode">
      <div class="page-heading">
        <div>
          <h2>我的健康档案</h2>
          <p>维护个人基础健康信息，系统将自动计算年龄与 BMI</p>
        </div>
        <el-button
          v-if="!editing && !loadError"
          :type="hasProfile ? 'default' : 'primary'"
          :icon="hasProfile ? Edit : Plus"
          @click="startEdit"
        >
          {{ hasProfile ? '编辑档案' : '完善档案' }}
        </el-button>
      </div>

      <el-result
        v-if="loadError"
        icon="warning"
        title="档案加载失败"
        :sub-title="loadError"
      >
        <template #extra>
          <el-button type="primary" :icon="Refresh" @click="loadMyProfile">
            重新加载
          </el-button>
        </template>
      </el-result>

      <template v-else>
        <div v-loading="profileLoading" class="student-profile-content">
          <HealthProfileForm
            v-if="editing && profile"
            :profile="profile"
            @cancel="editing = false"
            @saved="handleSaved"
          />

          <template v-else-if="profile">
            <section class="student-hero">
              <div class="hero-identity-wrap">
                <el-avatar :size="60" class="hero-avatar">
                  {{ avatarText }}
                </el-avatar>
                <div class="hero-identity">
                  <div class="hero-name-row">
                    <h3>{{ displayName }}</h3>
                    <el-tag
                      :type="genderTagType(profile.gender)"
                      size="small"
                      effect="light"
                    >
                      {{ profile.genderName || genderLabel(profile.gender) }}
                    </el-tag>
                    <el-tag
                      v-if="profile.age != null"
                      size="small"
                      type="info"
                      effect="plain"
                    >
                      {{ profile.age }} 岁
                    </el-tag>
                  </div>
                  <p class="hero-sub">
                    {{ profile.studentNo || '学号未绑定' }}
                    <span>·</span>
                    {{ profile.college || '学院未填写' }}
                    <span>·</span>
                    {{ profile.major || '专业未填写' }}
                  </p>
                  <div class="hero-tags">
                    <span v-if="profile.grade">{{ profile.grade }}</span>
                    <span v-if="profile.className">{{ profile.className }}</span>
                    <span v-if="profile.bloodType">{{ profile.bloodType }}</span>
                  </div>
                </div>
              </div>

              <div class="hero-metrics">
                <div class="hero-metric">
                  <span>身高</span>
                  <div><strong>{{ formatNumber(profile.height) }}</strong><em>cm</em></div>
                </div>
                <div class="hero-metric">
                  <span>体重</span>
                  <div><strong>{{ formatNumber(profile.weight) }}</strong><em>kg</em></div>
                </div>
                <div class="hero-metric bmi">
                  <span>BMI</span>
                  <div><strong>{{ formatNumber(profile.bmi) }}</strong></div>
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

              <div class="hero-completeness">
                <el-progress
                  type="dashboard"
                  :percentage="completeness"
                  :width="90"
                  :stroke-width="8"
                  color="#1f9d7a"
                >
                  <template #default="{ percentage }">
                    <span class="completeness-value">{{ percentage }}%</span>
                  </template>
                </el-progress>
                <span>档案完整度</span>
              </div>
            </section>

            <div v-if="!hasProfile" class="empty-profile-banner">
              <div>
                <strong>尚未建立完整健康档案</strong>
                <p>完善身高、体重、健康风险和紧急联系人后，校医可更好地了解你的健康情况。</p>
              </div>
              <el-button type="primary" :icon="Plus" @click="startEdit">
                立即建档
              </el-button>
            </div>

            <div class="profile-info-grid">
              <section class="panel-card info-card">
                <div class="info-card-head">
                  <h4>学籍信息</h4>
                  <span>ACADEMIC</span>
                </div>
                <div class="info-rows">
                  <div class="info-row">
                    <span>学院</span>
                    <strong>{{ profile.college || '未填写' }}</strong>
                  </div>
                  <div class="info-row">
                    <span>专业</span>
                    <strong>{{ profile.major || '未填写' }}</strong>
                  </div>
                  <div class="info-row">
                    <span>年级</span>
                    <strong>{{ profile.grade || '未填写' }}</strong>
                  </div>
                  <div class="info-row">
                    <span>班级</span>
                    <strong>{{ profile.className || '未填写' }}</strong>
                  </div>
                </div>
              </section>

              <section class="panel-card info-card">
                <div class="info-card-head">
                  <h4>身体指标</h4>
                  <span>PHYSICAL</span>
                </div>
                <div class="info-rows">
                  <div class="info-row">
                    <span>身高 / 体重</span>
                    <strong>
                      {{ formatValue(profile.height, 'cm') }} /
                      {{ formatValue(profile.weight, 'kg') }}
                    </strong>
                  </div>
                  <div class="info-row">
                    <span>血型</span>
                    <strong>{{ profile.bloodType || '未填写' }}</strong>
                  </div>
                  <div class="info-row">
                    <span>出生日期</span>
                    <strong>{{ formatDate(profile.birthDate) }}</strong>
                  </div>
                  <div class="info-row">
                    <span>BMI 等级</span>
                    <strong>{{ profile.bmiLevel || '未计算' }}</strong>
                  </div>
                </div>
              </section>

              <section class="panel-card info-card">
                <div class="info-card-head">
                  <h4>健康风险</h4>
                  <span>RISK</span>
                </div>
                <div class="info-rows">
                  <div class="info-row block">
                    <span>慢性病史</span>
                    <p>{{ profile.chronicDisease || '未填写' }}</p>
                  </div>
                  <div class="info-row block">
                    <span>过敏史</span>
                    <p>{{ profile.allergyHistory || '未填写' }}</p>
                  </div>
                </div>
              </section>

              <section class="panel-card info-card">
                <div class="info-card-head">
                  <h4>紧急联系人</h4>
                  <span>EMERGENCY</span>
                </div>
                <div class="info-rows">
                  <div class="info-row">
                    <span>联系人</span>
                    <strong>{{ profile.emergencyContact || '未填写' }}</strong>
                  </div>
                  <div class="info-row">
                    <span>联系电话</span>
                    <strong>{{ profile.emergencyPhone || '未填写' }}</strong>
                  </div>
                  <div class="info-row">
                    <span>账号电话</span>
                    <strong>{{ profile.phone || '未填写' }}</strong>
                  </div>
                  <div class="info-row">
                    <span>最近更新</span>
                    <strong>{{ formatDateTime(profile.updateTime) }}</strong>
                  </div>
                </div>
              </section>
            </div>

            <section v-if="profile.remark" class="panel-card remark-card">
              <div class="info-card-head">
                <h4>备注</h4>
                <span>REMARK</span>
              </div>
              <p>{{ profile.remark }}</p>
            </section>
          </template>

          <el-empty
            v-else
            description="暂无健康档案数据"
            :image-size="100"
          />
        </div>
      </template>
    </template>

    <el-result
      v-else
      icon="warning"
      title="暂无访问权限"
      sub-title="当前账号角色无法访问健康档案模块"
    />
  </div>
</template>

<style scoped>
.health-profile-page {
  gap: 18px;
}

.search-card {
  padding: 18px 20px 4px;
}

.table-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.bmi-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.bmi-cell strong {
  font-size: 14px;
  font-weight: 700;
  color: var(--hp-text);
}

.table-footer :deep(.el-pagination) {
  margin-left: auto;
}

/* ---------------- 学生档案 ---------------- */
.student-profile-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 200px;
}

.student-hero {
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 26px 30px;
  overflow: hidden;
  background:
    radial-gradient(circle at 88% 0%, rgba(61, 220, 151, 0.18), transparent 36%),
    linear-gradient(120deg, #0d4439 0%, #12604f 58%, #17866b 100%);
  border-radius: 18px;
  box-shadow: 0 16px 40px rgba(15, 74, 64, 0.16);
}

.hero-identity-wrap {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.hero-avatar {
  flex-shrink: 0;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.25);
}

.hero-identity {
  min-width: 0;
}

.hero-name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 7px;
}

.hero-name-row h3 {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
}

.hero-sub {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  font-size: 12px;
  color: rgba(234, 255, 247, 0.62);
}

.hero-sub span {
  color: rgba(234, 255, 247, 0.3);
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.hero-tags span {
  padding: 3px 8px;
  font-size: 10.5px;
  color: rgba(234, 255, 247, 0.7);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 5px;
}

.hero-metrics {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 26px;
  padding-left: 28px;
  border-left: 1px solid rgba(255, 255, 255, 0.12);
}

.hero-metric {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.hero-metric > span {
  font-size: 10.5px;
  letter-spacing: 0.5px;
  color: rgba(234, 255, 247, 0.5);
}

.hero-metric > div {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.hero-metric strong {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.hero-metric em {
  font-size: 10.5px;
  font-style: normal;
  color: rgba(234, 255, 247, 0.5);
}

.hero-metric.bmi {
  align-items: center;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
}

/* 深色横幅中的等级标签需要显式高对比色 */
.hero-metric.bmi :deep(.el-tag) {
  color: #0b5c46;
  font-weight: 600;
  background: #c9f5e2;
  border-color: transparent;
}

.hero-metric.bmi :deep(.el-tag__content) {
  color: #0b5c46;
}

.hero-completeness {
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding-left: 24px;
  font-size: 10.5px;
  color: rgba(234, 255, 247, 0.55);
  border-left: 1px solid rgba(255, 255, 255, 0.12);
}

.hero-completeness :deep(.el-progress__text) {
  font-size: 14px !important;
}

.completeness-value {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}

.empty-profile-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 22px;
  background: #fff9e9;
  border: 1px solid #f2e2b8;
  border-radius: 14px;
}

.empty-profile-banner strong {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
  color: #8a681d;
}

.empty-profile-banner p {
  font-size: 12px;
  color: #a68a4b;
}

.profile-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.info-card {
  padding: 20px 22px;
}

.info-card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  margin-bottom: 14px;
  border-bottom: 1px solid #edf3f0;
}

.info-card-head h4 {
  font-size: 14px;
  font-weight: 700;
  color: var(--hp-text);
}

.info-card-head span {
  font-size: 8.5px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: #b2c6be;
}

.info-rows {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  font-size: 12.5px;
}

.info-row > span {
  flex-shrink: 0;
  color: var(--hp-text-muted);
}

.info-row strong {
  font-weight: 500;
  color: var(--hp-text);
  text-align: right;
  word-break: break-word;
}

.info-row.block {
  flex-direction: column;
  gap: 5px;
}

.info-row.block p {
  font-size: 12px;
  line-height: 1.7;
  color: var(--hp-text-secondary);
}

.remark-card {
  padding: 20px 22px;
}

.remark-card p {
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--hp-text-secondary);
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 1200px) {
  .student-hero {
    flex-wrap: wrap;
    gap: 20px;
  }

  .hero-metrics {
    padding-left: 0;
    border-left: none;
  }

  .hero-completeness {
    padding-left: 0;
    border-left: none;
  }
}

@media (max-width: 960px) {
  .profile-info-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .search-card {
    padding: 14px 12px 0;
  }

  .student-hero {
    padding: 22px 18px;
  }

  .hero-identity-wrap {
    align-items: flex-start;
  }

  .hero-name-row h3 {
    font-size: 19px;
  }

  .hero-metrics {
    width: 100%;
    gap: 14px;
    justify-content: space-between;
  }

  .hero-completeness {
    width: 100%;
    flex-direction: row;
    justify-content: flex-start;
    gap: 10px;
  }

  .empty-profile-banner {
    align-items: flex-start;
    flex-direction: column;
  }

  .table-footer :deep(.el-pagination) {
    margin-left: 0;
  }
}
</style>
