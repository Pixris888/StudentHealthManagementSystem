import { del, get, post, put } from '@/utils/request'
import type { PageResult } from '@/types/system'
import type {
  CheckinPhoto,
  CheckinQuery,
  CheckinSave,
  CheckinSession,
  CheckinSessionEnd,
  CheckinSessionQuery,
  CheckinSessionStart,
  CheckinSummary,
  HealthCheckin,
} from '@/types/health'

/** 今日打卡状态、四板块完成情况与照片 */
export function getTodayCheckin(): Promise<HealthCheckin> {
  return get<HealthCheckin>('/health/checkin/today')
}

/** 手动填写/补录打卡（按日期 upsert） */
export function saveCheckin(data: CheckinSave): Promise<HealthCheckin> {
  return post<HealthCheckin>('/health/checkin', data)
}

/** 修改本人打卡记录 */
export function updateCheckin(
  id: number,
  data: CheckinSave,
): Promise<HealthCheckin> {
  return put<HealthCheckin>(`/health/checkin/${id}`, data)
}

/** 本人历史打卡分页 */
export function pageMyCheckins(
  params: CheckinQuery,
): Promise<PageResult<HealthCheckin>> {
  return get<PageResult<HealthCheckin>>('/health/checkin/mine/page', params)
}

/** 本人月度打卡概览 */
export function getCheckinSummary(month?: string): Promise<CheckinSummary> {
  return get<CheckinSummary>(
    '/health/checkin/summary',
    month ? { month } : undefined,
  )
}

/** 校医/超级管理员分页查询打卡记录 */
export function pageAllCheckins(
  params: CheckinQuery,
): Promise<PageResult<HealthCheckin>> {
  return get<PageResult<HealthCheckin>>('/health/checkin/page', params)
}

/** 打卡详情 */
export function getCheckinDetail(id: number): Promise<HealthCheckin> {
  return get<HealthCheckin>(`/health/checkin/${id}`)
}

/** 开始睡眠/运动计时 */
export function startCheckinSession(
  data: CheckinSessionStart,
): Promise<CheckinSession> {
  return post<CheckinSession>('/health/checkin/session/start', data)
}

/** 结束计时，返回会话与同步后的每日打卡 */
export function endCheckinSession(id: number): Promise<CheckinSessionEnd> {
  return post<CheckinSessionEnd>(`/health/checkin/session/${id}/end`)
}

/** 查询进行中的会话，没有时返回 null */
export function getActiveCheckinSession(
  sessionType: number,
): Promise<CheckinSession | null> {
  return get<CheckinSession | null>('/health/checkin/session/active', {
    sessionType,
  })
}

/** 本人计时会话历史分页 */
export function pageMyCheckinSessions(
  params: CheckinSessionQuery,
): Promise<PageResult<CheckinSession>> {
  return get<PageResult<CheckinSession>>(
    '/health/checkin/session/mine/page',
    params,
  )
}

/** 上传打卡照片（服务端加水印） */
export function uploadCheckinPhoto(
  checkinId: number,
  file: File,
  photoType: number,
): Promise<CheckinPhoto> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('photoType', String(photoType))
  return post<CheckinPhoto>(
    `/health/checkin/${checkinId}/photos`,
    formData,
    { timeout: 60000 },
  )
}

/** 查询某条打卡的照片 */
export function getCheckinPhotos(
  checkinId: number,
): Promise<CheckinPhoto[]> {
  return get<CheckinPhoto[]>(`/health/checkin/${checkinId}/photos`)
}

/** 删除本人照片 */
export function deleteCheckinPhoto(photoId: number): Promise<boolean> {
  return del<boolean>(`/health/checkin/photos/${photoId}`)
}
