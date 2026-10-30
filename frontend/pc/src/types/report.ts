/** 统计报表模块类型定义（字段与后端 VO 严格对齐） */

export interface DailyHealthPointVO {
  date: string
  sleepTime?: string
  sleepDuration?: number
  stayUpLate?: 0 | 1
  exerciseDuration?: number
  exerciseSufficient?: 0 | 1
  dietBreakfast?: 0 | 1
  waterIntake?: number
  emotionType?: 1 | 2 | 3 | 4 | 5
  moodScore?: number
  dataQuality?: 0 | 1
}

export interface MonthlyReportVO {
  month: string
  userId: number
  studentNo: string
  realName: string
  college?: string
  major?: string
  grade?: string
  className?: string
  checkinDays: number
  validDays: number
  abnormalDays: number
  avgSleepDuration?: number
  stayUpLateCount: number
  lateNightRate?: number
  avgExerciseDuration?: number
  exerciseInsufficientCount: number
  exerciseSufficientRate?: number
  avgMoodScore?: number
  breakfastRate?: number
  avgWaterIntake?: number
  bmi?: number
  bmiLevel?: string
  latestPhysicalScore?: number
  latestPhysicalGradeLevel?: string
  daily: DailyHealthPointVO[]
  suggestions: string[]
  summary: string
  generateTime?: string
}

export interface HealthReportHistoryVO {
  id: number
  month: string
  reportTitle: string
  checkinDays: number
  avgSleepDuration?: number
  stayUpLateCount: number
  avgExerciseDuration?: number
  exerciseInsufficientCount: number
  avgMoodScore?: number
  generateTime?: string
}

export interface DashboardOverviewVO {
  studentTotal: number
  checkinStudents: number
  checkinRate?: number
  checkinRecords: number
  avgSleepDuration?: number
  stayUpLateCount: number
  lateNightRate?: number
  avgExerciseDuration?: number
  exerciseSufficientRate?: number
  avgMoodScore?: number
  breakfastRate?: number
  abnormalRecords: number
}

export interface TrendPointVO {
  date: string
  checkinStudents: number
  checkinRecords: number
  avgSleepDuration?: number
  stayUpLateCount: number
  avgExerciseDuration?: number
  avgMoodScore?: number
}

export interface CollegeDistributionVO {
  college: string
  studentCount: number
  checkinStudents: number
  checkinRate?: number
  avgSleepDuration?: number
  lateNightRate?: number
  exerciseSufficientRate?: number
}

export interface EmotionDistributionVO {
  emotionType: number
  emotionName: string
  count: number
  rate?: number
}

export interface RiskStudentVO {
  userId: number
  studentNo: string
  realName: string
  college?: string
  grade?: string
  className?: string
  checkinDays: number
  stayUpLateCount: number
  exerciseInsufficientCount: number
  abnormalDays: number
  riskLevel: '较高' | '需关注' | string
}

export interface DashboardVO {
  month: string
  college?: string
  grade?: string
  overview: DashboardOverviewVO
  trend: TrendPointVO[]
  collegeDistribution: CollegeDistributionVO[]
  emotionDistribution: EmotionDistributionVO[]
  riskStudents: RiskStudentVO[]
}

export interface LedgerRowVO {
  userId: number
  studentNo: string
  realName: string
  college?: string
  major?: string
  grade?: string
  className?: string
  checkinDays: number
  validDays: number
  abnormalDays: number
  avgSleepDuration?: number
  stayUpLateCount: number
  lateNightRate?: number
  avgExerciseDuration?: number
  exerciseInsufficientCount: number
  exerciseSufficientRate?: number
  avgMoodScore?: number
  breakfastRate?: number
  avgWaterIntake?: number
  bmi?: number
  bmiLevel?: string
  riskLevel?: string
}

export interface FilterOptionsVO {
  colleges: string[]
  grades: string[]
  classes: string[]
}

export interface ReportQuery {
  month?: string
  college?: string
  grade?: string
}

export interface LedgerQuery extends ReportQuery {
  page?: number
  size?: number
  className?: string
  keyword?: string
}

export interface ReportHistoryQuery {
  page?: number
  size?: number
}
