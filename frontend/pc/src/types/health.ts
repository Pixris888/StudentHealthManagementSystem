/** 健康档案展示对象 */
export interface HealthProfile {
  id?: number | null
  userId?: number
  studentNo?: string
  realName?: string
  gender?: number | null
  genderName?: string
  age?: number | null
  birthDate?: string | null
  bloodType?: string | null
  height?: number | null
  weight?: number | null
  bmi?: number | null
  bmiLevel?: string | null
  college?: string | null
  major?: string | null
  grade?: string | null
  className?: string | null
  chronicDisease?: string | null
  allergyHistory?: string | null
  emergencyContact?: string | null
  emergencyPhone?: string | null
  phone?: string | null
  remark?: string | null
  createTime?: string
  updateTime?: string
}

/** 学生新增/更新自己的健康档案请求 */
export interface HealthProfileSave {
  gender?: number | null
  birthDate?: string | null
  bloodType?: string | null
  height?: number | null
  weight?: number | null
  college?: string | null
  major?: string | null
  grade?: string | null
  className?: string | null
  chronicDisease?: string | null
  allergyHistory?: string | null
  emergencyContact?: string | null
  emergencyPhone?: string | null
  remark?: string | null
}

/** 校医/管理员健康档案分页查询条件 */
export interface HealthProfileQuery {
  page: number
  size: number
  studentNo?: string
  realName?: string
  college?: string
  major?: string
  grade?: string
  className?: string
  gender?: number
}

/** 体测成绩展示对象 */
export interface PhysicalTest {
  id: number
  userId?: number
  studentNo?: string
  testYear: number
  testTerm: string
  height?: number | null
  weight?: number | null
  bmi?: number | null
  vitalCapacity?: number | null
  run50m?: number | null
  sitAndReach?: number | null
  standingJump?: number | null
  enduranceRun?: number | null
  strengthItem?: number | null
  totalScore?: number | null
  gradeLevel?: string | null
  testDate?: string | null
  createTime?: string
}

/** 学生新增/修改体测记录请求 */
export interface PhysicalTestSave {
  testYear: number
  testTerm: string
  height?: number | null
  weight?: number | null
  vitalCapacity?: number | null
  run50m?: number | null
  sitAndReach?: number | null
  standingJump?: number | null
  enduranceRun?: number | null
  strengthItem?: number | null
  totalScore?: number | null
  gradeLevel?: string | null
  testDate?: string | null
  remark?: string | null
}

/** 体测成绩分页查询条件（个人与管理员共用） */
export interface PhysicalTestQuery {
  page: number
  size: number
  testYear?: number
  testTerm?: string
  studentNo?: string
}

export interface PhysicalTrendQuery {
  startYear?: number
  endYear?: number
}

/** Excel 导入结果 */
export interface PhysicalImportResult {
  total: number
  inserted: number
  updated: number
  failed: number
  errors: string[]
}

/** 打卡照片 */
export interface CheckinPhoto {
  id: number
  checkinId: number
  photoUrl: string
  photoType: number
  photoTypeName?: string
  createTime?: string
}

/** 每日健康打卡 */
export interface HealthCheckin {
  id?: number
  userId?: number
  studentNo?: string
  realName?: string
  checkinDate?: string
  checkedIn?: boolean
  sleepTime?: string | null
  wakeTime?: string | null
  sleepDuration?: number | null
  stayUpLate?: number | null
  stayUpLateName?: string
  sleepSource?: number | null
  sleepSourceName?: string
  exerciseType?: string | null
  exerciseDuration?: number | null
  exerciseIntensity?: number | null
  exerciseIntensityName?: string
  exerciseSufficient?: number | null
  exerciseSufficientName?: string
  exerciseSource?: number | null
  exerciseSourceName?: string
  dietBreakfast?: number | null
  dietBalance?: number | null
  dietBalanceName?: string
  waterIntake?: number | null
  emotionType?: number | null
  emotionTypeName?: string
  moodScore?: number | null
  content?: string | null
  dataQuality?: number
  dataQualityName?: string
  abnormalReason?: string | null
  backfilled?: boolean
  sleepCompleted?: boolean
  exerciseCompleted?: boolean
  dietCompleted?: boolean
  emotionCompleted?: boolean
  photoCount?: number
  photos?: CheckinPhoto[]
  createTime?: string
  updateTime?: string
}

/** 月度打卡概览 */
export interface CheckinSummary {
  month?: string
  checkinDays?: number
  monthDays?: number
  checkinRate?: number
  consecutiveDays?: number
  lateCount?: number
  exerciseDays?: number
  totalExerciseMinutes?: number
  avgSleepDuration?: number | null
  avgWaterIntake?: number | null
  avgMoodScore?: number | null
  breakfastDays?: number
  abnormalCount?: number
}

/** 打卡分页查询 */
export interface CheckinQuery {
  page: number
  size: number
  startDate?: string
  endDate?: string
  stayUpLate?: number
  exerciseSufficient?: number
  dataQuality?: number
  studentNo?: string
}

/** 手动填写/补录打卡 */
export interface CheckinSave {
  checkinDate?: string
  sleepTime?: string | null
  wakeTime?: string | null
  exerciseType?: string | null
  exerciseDuration?: number | null
  exerciseIntensity?: number | null
  dietBreakfast?: number | null
  dietBalance?: number | null
  waterIntake?: number | null
  emotionType?: number | null
  moodScore?: number | null
  content?: string | null
}

/** 睡眠/运动计时会话 */
export interface CheckinSession {
  id: number
  userId?: number
  studentNo?: string
  checkinDate?: string | null
  sessionType: number
  sessionTypeName?: string
  startTime: string
  endTime?: string | null
  durationMinutes?: number | null
  status: number
  statusName?: string
  exerciseType?: string | null
  exerciseIntensity?: number | null
  exerciseIntensityName?: string
  dataQuality?: number
  dataQualityName?: string
  abnormalReason?: string | null
  createTime?: string
}

export interface CheckinSessionQuery {
  page: number
  size: number
  sessionType?: number
  status?: number
  startDate?: string
  endDate?: string
}

export interface CheckinSessionStart {
  sessionType: number
  exerciseType?: string
  exerciseIntensity?: number
}

export interface CheckinSessionEnd {
  session: CheckinSession
  checkin?: HealthCheckin | null
}
