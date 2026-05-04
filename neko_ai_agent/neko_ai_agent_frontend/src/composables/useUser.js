import { ref } from 'vue'
import { getCurrentUser as fetchCurrentUser, userLogout as logoutApi } from '../api/user'

const currentUser = ref(null)
const isLoading = ref(false)

/**
 * 用户状态管理 composable
 */
export const useUser = () => {
  /**
   * 获取当前登录用户
   */
  const fetchUser = async () => {
    if (isLoading.value) return

    isLoading.value = true
    try {
      const response = await fetchCurrentUser()
      if (response.data.code === 0) {
        currentUser.value = response.data.data
      } else {
        currentUser.value = null
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      currentUser.value = null
    } finally {
      isLoading.value = false
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
