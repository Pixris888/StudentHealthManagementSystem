import { get, post, put } from '@/utils/request'
import type { PageResult } from '@/types/system'
import type {
  PhysicalImportResult,
  PhysicalTest,
  PhysicalTestQuery,
  PhysicalTestSave,
  PhysicalTrendQuery,
} from '@/types/health'

/** 学生分页查询自己的体测记录 */
export function pageMyPhysicalTests(
  params: PhysicalTestQuery,
): Promise<PageResult<PhysicalTest>> {
  return get<PageResult<PhysicalTest>>('/health/physical_test/mine/page', params)
}

/** 学生新增体测记录 */
export function addMyPhysicalTest(
  data: PhysicalTestSave,
): Promise<PhysicalTest> {
  return post<PhysicalTest>('/health/physical_test/mine', data)
}

/** 学生修改自己的体测记录 */
export function updateMyPhysicalTest(
  id: number,
  data: PhysicalTestSave,
): Promise<PhysicalTest> {
  return put<PhysicalTest>(`/health/physical_test/mine/${id}`, data)
}

/** 学生 Excel 导入体测成绩 */
export function importPhysicalTests(file: File): Promise<PhysicalImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  return post<PhysicalImportResult>('/health/physical_test/import', formData, {
    timeout: 60000,
  })
}

/** 学生健康指标趋势 */
export function getPhysicalTrend(
  params?: PhysicalTrendQuery,
): Promise<PhysicalTest[]> {
  return get<PhysicalTest[]>('/health/physical_test/trend', params)
}

/** 校医/超级管理员分页查询体测成绩 */
export function pageAllPhysicalTests(
  params: PhysicalTestQuery,
): Promise<PageResult<PhysicalTest>> {
  return get<PageResult<PhysicalTest>>('/health/physical_test/page', params)
}

/** 校医/超级管理员查看体测记录详情 */
export function getPhysicalTestDetail(id: number): Promise<PhysicalTest> {
  return get<PhysicalTest>(`/health/physical_test/${id}`)
}
