import { get, post, put } from '@/utils/request'
import type {
  ChangePasswordForm,
  LoginForm,
  LoginResult,
  RegisterForm,
  UserInfo,
} from '@/types/system'

export function loginApi(data: LoginForm): Promise<LoginResult> {
  return post<LoginResult>('/auth/login', data)
}

export function registerApi(data: RegisterForm): Promise<LoginResult> {
  return post<LoginResult>('/auth/register', data)
}

export function getUserInfoApi(): Promise<UserInfo> {
  return get<UserInfo>('/auth/info')
}

export function logoutApi(): Promise<boolean> {
  return post<boolean>('/auth/logout', undefined, { silent: true })
}

export function changePasswordApi(data: ChangePasswordForm): Promise<boolean> {
  return put<boolean>('/auth/password', data)
}
