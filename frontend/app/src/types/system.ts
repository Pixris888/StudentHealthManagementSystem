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
}

export interface ChangePasswordForm {
  oldPassword: string
  newPassword: string
}
