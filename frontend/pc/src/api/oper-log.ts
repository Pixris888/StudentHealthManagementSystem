import { get } from '@/utils/request'
import type {
  OperLog,
  OperLogQuery,
  PageResult,
} from '@/types/system'

/** 操作日志分页查询（只读） */
export function pageOperLogs(
  params: OperLogQuery,
): Promise<PageResult<OperLog>> {
  return get<PageResult<OperLog>>('/system/oper_log/page', params)
}

/** 操作日志详情 */
export function getOperLog(id: number): Promise<OperLog> {
  return get<OperLog>(`/system/oper_log/${id}`)
}
