<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { userLogin, sendCode, userLoginByEmail } from '../api/user'
import { useUser } from '../composables/useUser'

const router = useRouter()
const { setUser } = useUser()

// Tab 切换: 'account' | 'email'
const activeTab = ref('account')

// 账号登录表单
const accountForm = ref({
  userAccount: '',
  userPassword: ''
})

// 邮箱登录表单
const emailForm = ref({
  userEmail: '',
  inputCode: ''
})

const isLoading = ref(false)
const isSending = ref(false)
const countdown = ref(0)
const errorMessage = ref('')

let countdownTimer = null

const startCountdown = () => {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

const handleSendCode = async () => {
  errorMessage.value = ''

  if (!emailForm.value.userEmail.trim()) {
    errorMessage.value = '请输入邮箱地址'
    return
  }

  isSending.value = true

  try {
    const response = await sendCode(emailForm.value.userEmail)
    if (response.data.code === 0) {
      startCountdown()
    } else {
      errorMessage.value = response.data.message || '验证码发送失败，请重试'
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
    errorMessage.value = '发送验证码失败，请检查网络连接'
  } finally {
    isSending.value = false
  }
}

const handleAccountLogin = async () => {
  errorMessage.value = ''

  if (!accountForm.value.userAccount.trim()) {
    errorMessage.value = '请输入账号'
    return
  }

  if (!accountForm.value.userPassword.trim()) {
    errorMessage.value = '请输入密码'
    return
  }

  isLoading.value = true

  try {
    const response = await userLogin(accountForm.value)
    if (response.data.code === 0) {
      setUser(response.data.data)
      sessionStorage.setItem('just_logged_in', '1')
      router.push('/')
    } else {
      errorMessage.value = response.data.message || '登录失败，请重试'
    }
  } catch (error) {
    console.error('登录失败:', error)
    errorMessage.value = '登录失败，请检查网络连接'
  } finally {
    isLoading.value = false
  }
}

const handleEmailLogin = async () => {
  errorMessage.value = ''

  if (!emailForm.value.userEmail.trim()) {
    errorMessage.value = '请输入邮箱地址'
    return
  }

  if (!emailForm.value.inputCode.trim()) {
    errorMessage.value = '请输入验证码'
    return
  }

  isLoading.value = true

  try {
    const response = await userLoginByEmail(emailForm.value.userEmail, emailForm.value.inputCode)
    if (response.data.code === 0) {
      setUser(response.data.data)
      sessionStorage.setItem('just_logged_in', '1')
      router.push('/')
    } else {
      errorMessage.value = response.data.message || '登录失败，请重试'
    }
  } catch (error) {
    console.error('登录失败:', error)
    errorMessage.value = '登录失败，请检查网络连接'
  } finally {
    isLoading.value = false
  }
}

const handleSubmit = () => {
  if (activeTab.value === 'account') {
    handleAccountLogin()
  } else {
    handleEmailLogin()
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
  errorMessage.value = ''
}

const handleKeyDown = (e) => {
  if (e.key === 'Enter' && !isLoading.value) {
    handleSubmit()
  }
}
</script>

<template>
  <main class="page auth-page">
    <!-- Background Effects -->
    <div class="auth-particles">
      <div class="auth-particle" />
      <div class="auth-particle" />
      <div class="auth-particle" />
      <div class="auth-particle" />
      <div class="auth-particle" />
      <div class="auth-particle" />
    </div>

    <div class="auth-container">
      <div class="auth-card">
        <div class="auth-card-glass">
          <!-- Header -->
          <div class="auth-header">
            <h1 class="auth-title">欢迎回来</h1>
            <p class="auth-subtitle">登录 Neko AI Agent</p>
          </div>

          <!-- Tab Switcher -->
          <div class="auth-tabs">
            <button
              class="auth-tab"
              :class="{ active: activeTab === 'account' }"
              @click="switchTab('account')"
            >
              账号登录
            </button>
            <button
              class="auth-tab"
              :class="{ active: activeTab === 'email' }"
              @click="switchTab('email')"
            >
              邮箱登录
            </button>
          </div>

          <!-- Account Login Form -->
          <form v-if="activeTab === 'account'" class="auth-form" @submit.prevent="handleAccountLogin">
            <div class="form-group">
              <label class="form-label">账号</label>
              <div class="input-wrapper">
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                <input
                  v-model="accountForm.userAccount"
                  type="text"
                  class="form-input"
                  placeholder="请输入账号"
                  @keydown="handleKeyDown"
                />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">密码</label>
              <div class="input-wrapper">
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
                <input
                  v-model="accountForm.userPassword"
                  type="password"
                  class="form-input"
                  placeholder="请输入密码"
                  @keydown="handleKeyDown"
                />
              </div>
            </div>

            <!-- Error Message -->
            <div v-if="errorMessage" class="error-message">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <span>{{ errorMessage }}</span>
            </div>

            <button type="submit" class="auth-btn" :disabled="isLoading">
              <span v-if="!isLoading">登录</span>
              <span v-else class="loading-text">登录中...</span>
            </button>
          </form>

          <!-- Email Login Form -->
          <form v-else class="auth-form" @submit.prevent="handleEmailLogin">
            <div class="form-group">
              <label class="form-label">邮箱</label>
              <div class="input-wrapper">
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                  <polyline points="22,6 12,13 2,6" />
                </svg>
                <input
                  v-model="emailForm.userEmail"
                  type="email"
                  class="form-input"
                  placeholder="请输入邮箱地址"
                  @keydown="handleKeyDown"
                />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">验证码</label>
              <div class="code-input-row">
                <div class="input-wrapper code-input-wrapper">
                  <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                  <input
                    v-model="emailForm.inputCode"
                    type="text"
                    class="form-input"
                    placeholder="请输入验证码"
                    @keydown="handleKeyDown"
                  />
                </div>
                <button
                  type="button"
                  class="send-code-btn"
                  :disabled="isSending || countdown > 0"
                  @click="handleSendCode"
                >
                  <span v-if="countdown > 0">{{ countdown }}s</span>
                  <span v-else-if="isSending">发送中...</span>
                  <span v-else>获取验证码</span>
                </button>
              </div>
            </div>

            <!-- Error Message -->
            <div v-if="errorMessage" class="error-message">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <span>{{ errorMessage }}</span>
            </div>

            <button type="submit" class="auth-btn" :disabled="isLoading">
              <span v-if="!isLoading">登录</span>
              <span v-else class="loading-text">登录中...</span>
            </button>
          </form>

          <!-- Footer Link -->
          <div class="auth-footer">
            <span class="footer-text">没有账号？</span>
            <router-link to="/register" class="footer-link">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
