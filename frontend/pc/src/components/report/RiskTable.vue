<script setup lang="ts">
import { riskTagType } from '@/utils/report'
import type { RiskStudentVO } from '@/types/report'

withDefaults(
  defineProps<{
    students: RiskStudentVO[]
    loading?: boolean
  }>(),
  { loading: false },
)
</script>

<template>
  <el-table
    v-loading="loading"
    :data="students"
    row-key="userId"
    size="small"
    max-height="360"
    style="width: 100%"
  >
    <el-table-column
      prop="studentNo"
      label="学号"
      width="100"
      show-overflow-tooltip
    />
    <el-table-column
      prop="realName"
      label="姓名"
      width="64"
      show-overflow-tooltip
    />
    <el-table-column
      prop="college"
      label="学院"
      min-width="100"
      show-overflow-tooltip
    >
      <template #default="{ row }">{{ row.college || '—' }}</template>
    </el-table-column>
    <el-table-column prop="grade" label="年级" width="56">
      <template #default="{ row }">{{ row.grade || '—' }}</template>
    </el-table-column>
    <el-table-column
      prop="className"
      label="班级"
      width="86"
      show-overflow-tooltip
    >
      <template #default="{ row }">{{ row.className || '—' }}</template>
    </el-table-column>
    <el-table-column
      prop="checkinDays"
      label="打卡"
      width="52"
      align="center"
    />
    <el-table-column
      prop="stayUpLateCount"
      label="熬夜"
      width="52"
      align="center"
    />
    <el-table-column
      prop="exerciseInsufficientCount"
      label="运动不足"
      width="66"
      align="center"
    />
    <el-table-column
      prop="abnormalDays"
      label="异常"
      width="52"
      align="center"
    />
    <el-table-column label="风险等级" width="76" align="center">
      <template #default="{ row }">
        <el-tag :type="riskTagType(row.riskLevel)" effect="light">
          {{ row.riskLevel || '—' }}
        </el-tag>
      </template>
    </el-table-column>

    <template #empty>
      <el-empty description="暂无需要关注的学生" :image-size="76" />
    </template>
  </el-table>
</template>
