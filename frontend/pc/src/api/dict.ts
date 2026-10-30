import { del, get, post, put } from '@/utils/request'
import type {
  DictDataForm,
  DictDataQuery,
  DictTypeForm,
  DictTypeQuery,
  PageResult,
  SysDictData,
  SysDictType,
} from '@/types/system'

/* ---------------- 字典类型 ---------------- */

export function pageDictTypes(
  params: DictTypeQuery,
): Promise<PageResult<SysDictType>> {
  return get<PageResult<SysDictType>>('/system/dict_type/page', params)
}

/** 启用字典类型下拉（登录用户可用） */
export function listEnabledDictTypes(): Promise<SysDictType[]> {
  return get<SysDictType[]>('/system/dict_type/enabled')
}

export function getDictType(id: number): Promise<SysDictType> {
  return get<SysDictType>(`/system/dict_type/${id}`)
}

export function createDictType(data: DictTypeForm): Promise<boolean> {
  return post<boolean>('/system/dict_type', data)
}

export function updateDictType(data: DictTypeForm): Promise<boolean> {
  return put<boolean>('/system/dict_type', data)
}

export function deleteDictTypes(ids: number[]): Promise<boolean> {
  return del<boolean>(`/system/dict_type/${ids.join(',')}`)
}

/* ---------------- 字典数据 ---------------- */

export function pageDictData(
  params: DictDataQuery,
): Promise<PageResult<SysDictData>> {
  return get<PageResult<SysDictData>>('/system/dict_data/page', params)
}

/** 按字典类型查询启用数据（登录用户可用） */
export function listDictDataByType(
  dictType: string,
): Promise<SysDictData[]> {
  return get<SysDictData[]>(
    `/system/dict_data/type/${encodeURIComponent(dictType)}`,
  )
}

export function getDictData(id: number): Promise<SysDictData> {
  return get<SysDictData>(`/system/dict_data/${id}`)
}

export function createDictData(data: DictDataForm): Promise<boolean> {
  return post<boolean>('/system/dict_data', data)
}

export function updateDictData(data: DictDataForm): Promise<boolean> {
  return put<boolean>('/system/dict_data', data)
}

export function deleteDictData(ids: number[]): Promise<boolean> {
  return del<boolean>(`/system/dict_data/${ids.join(',')}`)
}
