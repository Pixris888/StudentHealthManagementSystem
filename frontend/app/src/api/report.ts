import { download, get } from '@/utils/request'
import type { PageResult } from '@/types/system'
import type {
  HealthReportHistoryVO,
  MonthlyReportVO,
  ReportHistoryQuery,
} from '@/types/report'

/** 生成/查看个人月度健康报告 */
export function getMonthlyReport(month?: string): Promise<MonthlyReportVO> {
  return get<MonthlyReportVO>(
    '/health/report/mine/monthly',
    month ? { month } : undefined,
    { timeout: 60000 },
  )
}

/** 历史报告分页 */
export function getReportHistory(
  params: ReportHistoryQuery,
): Promise<PageResult<HealthReportHistoryVO>> {
  return get<PageResult<HealthReportHistoryVO>>(
    '/health/report/mine/history/page',
    { ...params },
    { silent: true },
  )
}

/** 下载个人月度报告 Excel */
export function downloadMonthlyReport(month: string): Promise<string> {
  return download(
    `/health/report/mine/monthly/download?month=${encodeURIComponent(month)}`,
    `个人健康报告_${month}.xlsx`,
  )
}
