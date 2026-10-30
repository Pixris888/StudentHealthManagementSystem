const ACCESS_TOKEN_KEY = 'health_access_token'
const REFRESH_TOKEN_KEY = 'health_refresh_token'
const REMEMBER_USERNAME_KEY = 'health_remember_username'

export function getAccessToken(): string {
  return localStorage.getItem(ACCESS_TOKEN_KEY) ?? ''
}

export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_TOKEN_KEY) ?? ''
}

export function setTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function getRememberedUsername(): string {
  return localStorage.getItem(REMEMBER_USERNAME_KEY) ?? ''
}

export function rememberUsername(username: string): void {
  if (username) {
    localStorage.setItem(REMEMBER_USERNAME_KEY, username)
  } else {
    localStorage.removeItem(REMEMBER_USERNAME_KEY)
  }
}
