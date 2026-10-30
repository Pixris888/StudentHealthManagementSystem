/** 后端统一响应结构：{ code, msg, data } */
export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

/** MyBatis-Plus 分页结构 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface LoginForm {
  username: string
  password: string
}

export interface RegisterForm {
  username: string
  password: string
  confirmPassword: string
  realName: string
  studentNo: string
  gender?: number
  phone?: string
  email?: string
  deptName?: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  refreshExpiresIn: number
  userId: number
  username: string
  realName: string
  userType: number
  roles: string[]
  permissions: string[]
}

export interface MenuTree {
  id: number
  parentId: number
  menuName: string
  path: string | null
  component: string | null
  perms: string | null
  icon: string | null
  menuType: number
  orderNum: number
  status: number
  children?: MenuTree[]
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  nickName?: string
  userType: number
  userTypeName: string
  avatar?: string
  deptName?: string
  roles: string[]
  permissions: string[]
  menus: MenuTree[]
}

export interface Role {
  id: number
  roleCode: string
  roleName: string
  userType: number
  roleSort: number
  dataScope: number
  status: number
  remark?: string
}

export interface RoleDetail extends Role {
  menuIds: number[]
}

export interface RoleQuery {
  page: number
  size: number
  roleCode?: string
  roleName?: string
  status?: number
}

export interface RoleForm {
  id?: number
  roleCode: string
  roleName: string
  userType: number
  roleSort: number
  dataScope: number
  status: number
  remark?: string
  menuIds?: number[]
}

export interface MenuDetail {
  id: number
  menuName: string
  parentId: number
  menuType: number
  orderNum: number
  path: string | null
  component: string | null
  perms: string | null
  icon: string | null
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface MenuForm {
  id?: number
  menuName: string
  parentId?: number
  menuType: number
  orderNum: number
  path?: string
  component?: string
  perms?: string
  icon?: string
  status: number
  remark?: string
}

export interface ChangePasswordForm {
  oldPassword: string
  newPassword: string
}

/** 用户管理列表/详情 */
export interface User {
  id: number
  username: string
  realName: string
  nickName?: string
  userType: number
  userTypeName?: string
  studentNo?: string
  gender?: number
  avatar?: string
  phone?: string
  email?: string
  deptName?: string
  status: number
  loginIp?: string
  loginTime?: string
  createTime?: string
  remark?: string
  roleIds?: number[]
  roles?: Role[]
}

export interface UserQuery {
  page: number
  size: number
  username?: string
  realName?: string
  userType?: number
  status?: number
  deptName?: string
}

export interface UserCreate {
  username: string
  password: string
  realName: string
  nickName?: string
  userType?: number
  studentNo?: string
  gender?: number
  avatar?: string
  phone?: string
  email?: string
  deptName?: string
  roleIds: number[]
  remark?: string
}

export interface UserUpdate {
  id: number
  realName: string
  nickName?: string
  gender?: number
  avatar?: string
  phone?: string
  email?: string
  deptName?: string
  studentNo?: string
  remark?: string
}

/* ---------------- 系统参数 ---------------- */

/** 参数类型 0-系统内置 1-自定义 */
export type ConfigType = 0 | 1

export interface SysConfig {
  id: number
  configName: string
  configKey: string
  configValue?: string
  configType: ConfigType
  remark?: string
  createBy?: number
  createTime?: string
  updateBy?: number
  updateTime?: string
}

export interface SysConfigQuery {
  page: number
  size: number
  keyword?: string
  configType?: ConfigType
}

export interface SysConfigForm {
  id?: number
  configName: string
  configKey: string
  configValue?: string
  configType: ConfigType
  remark?: string
}

/* ---------------- 字典管理 ---------------- */

/** 字典类型 / 字典数据启用状态 0-停用 1-正常 */
export type SystemEnableStatus = 0 | 1

export interface SysDictType {
  id: number
  dictName: string
  dictType: string
  status: SystemEnableStatus
  remark?: string
  createBy?: number
  createTime?: string
  updateBy?: number
  updateTime?: string
}

export interface DictTypeQuery {
  page: number
  size: number
  keyword?: string
  status?: SystemEnableStatus
}

export interface DictTypeForm {
  id?: number
  dictName: string
  dictType: string
  status: SystemEnableStatus
  remark?: string
}

export interface SysDictData {
  id: number
  dictType: string
  dictLabel: string
  dictValue: string
  dictSort: number
  status: SystemEnableStatus
  remark?: string
  createBy?: number
  createTime?: string
  updateBy?: number
  updateTime?: string
}

export interface DictDataQuery {
  page: number
  size: number
  dictType?: string
  keyword?: string
  status?: SystemEnableStatus
}

export interface DictDataForm {
  id?: number
  dictType: string
  dictLabel: string
  dictValue: string
  dictSort: number
  status: SystemEnableStatus
  remark?: string
}

/* ---------------- 操作日志 ---------------- */

/** 0-成功 1-失败（与其它模块的状态值相反，注意区分） */
export type OperLogStatus = 0 | 1

export interface OperLog {
  id: number
  title: string
  businessType?: number
  method?: string
  requestMethod?: string
  operatorType?: number
  operUserId?: number
  operUserName?: string
  operIp?: string
  operLocation?: string
  operParam?: string
  jsonResult?: string
  status: OperLogStatus
  errorMsg?: string
  operTime?: string
  createTime?: string
}

export interface OperLogQuery {
  page: number
  size: number
  keyword?: string
  businessType?: number
  status?: OperLogStatus
  startDate?: string
  endDate?: string
  operUserId?: number
}

/* ---------------- 数据备份 ---------------- */

export type BackupStatus = 0 | 1

export interface BackupRecord {
  id: number
  fileName: string
  fileSize?: number
  tableCount?: number
  rowCount?: number
  status: BackupStatus
  statusName?: string
  errorMsg?: string
  createBy?: number
  createTime?: string
}

export interface BackupQuery {
  page: number
  size: number
  keyword?: string
  status?: BackupStatus
  startDate?: string
  endDate?: string
}
