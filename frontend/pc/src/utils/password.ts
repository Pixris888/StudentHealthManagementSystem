/**
 * 与后端一致的密码强度校验。
 * 返回 null 表示通过，否则返回错误提示。
 *
 * @param forbidden 密码中不允许包含的值，例如用户名、学号
 */
export function validatePassword(
  password: string,
  forbidden: Array<string | null | undefined> = [],
): string | null {
  if (!password) return '请输入密码'
  if (password.length < 8 || password.length > 32) {
    return '密码长度必须为 8-32 位'
  }
  if (/\s/.test(password)) {
    return '密码不能包含空白字符'
  }
  const categories = [/[a-z]/, /[A-Z]/, /\d/, /[^A-Za-z0-9]/].filter(
    (pattern) => pattern.test(password),
  ).length
  if (categories < 3) {
    return '密码必须包含大写字母、小写字母、数字、特殊字符中的至少三类'
  }
  const lowerPassword = password.toLowerCase()
  for (const value of forbidden) {
    if (value && lowerPassword.includes(value.toLowerCase())) {
      return '密码不能包含用户名或学号'
    }
  }
  return null
}
