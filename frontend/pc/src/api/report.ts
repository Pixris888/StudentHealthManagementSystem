import { get } from '@/utils/request'
import { downloadExcel } from '@/utils/download'
import type { PageResult } from '@/types/system'
import type {
  DashboardVO,
  FilterOptionsVO,
  HealthReportHistoryVO,
  LedgerQuery,
  LedgerRowVO,
  MonthlyReportVO,
  ReportHistoryQuery,
  ReportQuery,
} from '@/types/report'

/* ---------------- 学生个人报告 ---------------- */

/** 生成/查看个人月度健康报告 */
export function getMonthlyReport(month?: string): Promise<MonthlyReportVO> {
  return get<MonthlyReportVO>(
    '/health/report/mine/monthly',
    month ? { month } : undefined,
  )
}

/** 历史报告分页 */
export function getReportHistory(
  params: ReportHistoryQuery,
): Promise<PageResult<HealthReportHistoryVO>> {
  return get<PageResult<HealthReportHistoryVO>>(
    '/health/report/mine/history/page',
    params,
  )
}

/** 下载个人月度报告 Excel */
export function downloadMonthlyReport(month: string): Promise<boolean> {
  return downloadExcel(
    '/health/report/mine/monthly/download',
    { month },
    `个人健康报告_${month}.xlsx`,
  )
}

/* ---------------- 全校看板 ---------------- */

export function getDashboard(params: ReportQuery): Promise<DashboardVO> {
  return get<DashboardVO>('/health/report/dashboard', params)
}

/* ---------------- 健康台账 ---------------- */

export function getLedgerPage(
  params: LedgerQuery,
): Promise<PageResult<LedgerRowVO>> {
  return get<PageResult<LedgerRowVO>>('/health/report/ledger/page', params)
}

export function getLedgerFilters(): Promise<FilterOptionsVO> {
  return get<FilterOptionsVO>('/health/report/ledger/filters')
}

/** 导出台账 Excel（校医脱敏，超管完整） */
export function downloadLedgerReport(
  params: LedgerQuery,
): Promise<boolean> {
  return downloadExcel(
    '/health/report/ledger/export',
    {
      month: params.month,
      college: params.college,
      grade: params.grade,
      className: params.className,
      keyword: params.keyword,
    },
    `校园健康台账_${params.month ?? ''}.xlsx`,
  )
}

/** 超管全平台多 Sheet 报表导出 */
export function downloadPlatformReport(
  params: ReportQuery,
): Promise<boolean> {
  return downloadExcel(
    '/health/report/platform/export',
    {
      month: params.month,
      college: params.college,
      grade: params.grade,
    },
    `全平台健康报表_${params.month ?? ''}.xlsx`,
  )
}
