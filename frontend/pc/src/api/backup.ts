import { del, get, post } from '@/utils/request'
import { downloadFile } from '@/utils/download'
import type {
  BackupQuery,
  BackupRecord,
  PageResult,
} from '@/types/system'

const BACKUP_TIMEOUT = 180000

/** 创建数据库逻辑备份（全表 CSV + ZIP） */
export function createBackup(): Promise<BackupRecord> {
  return post<BackupRecord>('/system/backup', undefined, {
    timeout: BACKUP_TIMEOUT,
  })
}

export function pageBackups(
  params: BackupQuery,
): Promise<PageResult<BackupRecord>> {
  return get<PageResult<BackupRecord>>('/system/backup/page', params)
}

export function getBackup(id: number): Promise<BackupRecord> {
  return get<BackupRecord>(`/system/backup/${id}`)
}

/** 下载备份 ZIP */
export function downloadBackup(
  id: number,
  fileName: string,
): Promise<boolean> {
  return downloadFile(
    `/system/backup/${id}/download`,
    {},
    fileName || `health_backup_${id}.zip`,
    BACKUP_TIMEOUT,
  )
}

/** 删除备份记录及服务器文件 */
export function deleteBackups(ids: number[]): Promise<boolean> {
  return del<boolean>(`/system/backup/${ids.join(',')}`)
}
