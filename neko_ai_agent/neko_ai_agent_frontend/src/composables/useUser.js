import { ref } from 'vue'
import { getCurrentUser as fetchCurrentUser, userLogout as logoutApi } from '../api/user'

const currentUser = ref(null)
const isLoading = ref(false)
const NOT_LOGIN_CODE = 40100
let fetchPromise = null

/**
 * 用户状态管理 composable
 */
export const useUser = () => {
  /**
   * 获取当前登录用户
   */
  const fetchUser = async () => {
    // 复用进行中的请求，避免多个入口同时鉴权时误判登录状态。
    if (isLoading.value && fetchPromise) return fetchPromise

    isLoading.value = true
    fetchPromise = (async () => {
      const response = await fetchCurrentUser()
      if (response.data.code === 0) {
        currentUser.value = response.data.data
        return currentUser.value
      }
      if (response.data.code === NOT_LOGIN_CODE) {
        currentUser.value = null
        return null
      }
      throw new Error(response.data.message || '获取用户信息失败')
    })()

    try {
      return await fetchPromise
    } catch (error) {
      console.error('获取用户信息失败:', error)
      if (error?.response?.data?.code === NOT_LOGIN_CODE || error?.response?.status === 401) {
        currentUser.value = null
        return null
      }
      currentUser.value = null
      throw error
    } finally {
      isLoading.value = false
      fetchPromise = null
    }
  }

  /**
   * 设置当前用户
   */
  const setUser = (user) => {
    currentUser.value = user
  }

  /**
   * 清除当前用户
   */
  const clearUser = () => {
    currentUser.value = null
  }

  /**
   * 用户注销
   */
  const logout = async () => {
    try {
      const response = await logoutApi()
      if (response.data.code === 0) {
        clearUser()
        return true
      }
      return false
    } catch (error) {
      console.error('注销失败:', error)
      return false
    }
  }

  return {
    currentUser,
    isLoading,
    fetchUser,
    setUser,
    clearUser,
    logout
  }
}
