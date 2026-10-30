const ACCESS_TOKEN_KEY = 'health_access_token'
const REFRESH_TOKEN_KEY = 'health_refresh_token'
const REMEMBER_USERNAME_KEY = 'health_remember_username'

function readStorage(key: string): string {
  try {
    const value = uni.getStorageSync(key)
    return typeof value === 'string' ? value : ''
  } catch {
    return ''
  }
}

function writeStorage(key: string, value: string): void {
  try {
    if (value) {
      uni.setStorageSync(key, value)
    } else {
      uni.removeStorageSync(key)
    }
  } catch {
    // 本地存储失败时不影响主流程
  }
}

export function getAccessToken(): string {
  return readStorage(ACCESS_TOKEN_KEY)
}

export function getRefreshToken(): string {
  return readStorage(REFRESH_TOKEN_KEY)
}

export function setTokens(accessToken: string, refreshToken: string): void {
  writeStorage(ACCESS_TOKEN_KEY, accessToken)
  writeStorage(REFRESH_TOKEN_KEY, refreshToken)
}

export function clearTokens(): void {
  try {
    uni.removeStorageSync(ACCESS_TOKEN_KEY)
    uni.removeStorageSync(REFRESH_TOKEN_KEY)
  } catch {
    // ignore
  }
}

export function getRememberedUsername(): string {
  return readStorage(REMEMBER_USERNAME_KEY)
}

export function rememberUsername(username: string): void {
  writeStorage(REMEMBER_USERNAME_KEY, username)
}
