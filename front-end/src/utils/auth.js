// Token 统一保存在 sessionStorage，与 api-contract.md 的登录鉴权约定一致。
const TOKEN_KEY = 'elm_lite_access_token'

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  sessionStorage.removeItem(TOKEN_KEY)
}
