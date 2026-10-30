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

export interface ReportHistoryQuery {
  page?: number
  size?: number
}
