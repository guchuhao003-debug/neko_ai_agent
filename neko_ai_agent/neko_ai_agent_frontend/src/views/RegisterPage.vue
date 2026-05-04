<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { userRegister } from '../api/user'

const router = useRouter()

const formData = ref({
  userName: '',
  userEmail: '',
  userAccount: '',
  userPassword: '',
  checkPassword: ''
})

const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const handleRegister = async () => {
  errorMessage.value = ''
  successMessage.value = ''

  if (!formData.value.userName.trim()) {
    errorMessage.value = '请输入用户名'
    return
  }

  if (!formData.value.userAccount.trim()) {
    errorMessage.value = '请输入账号'
    return
  }

  if (!formData.value.userPassword.trim()) {
    errorMessage.value = '请输入密码'
    return
  }

  if (formData.value.userPassword !== formData.value.checkPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  isLoading.value = true

  try {
    const response = await userRegister(formData.value)
    if (response.data.code === 0) {
      successMessage.value = '注册成功，即将跳转到登录页面...'
      setTimeout(() => {
        router.push('/login')
      }, 1500)
    } else {
      errorMessage.value = response.data.message || '注册失败，请重试'
    }
  } catch (error) {
    console.error('注册失败:', error)
    errorMessage.value = '注册失败，请检查网络连接'
  } finally {
    isLoading.value = false
  }
}

const handleKeyDown = (e) => {
  if (e.key === 'Enter' && !isLoading.value) {
    handleRegister()
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
            <h1 class="auth-title">创建账号</h1>
            <p class="auth-subtitle">加入 Neko AI Agent</p>
          </div>

          <!-- Form -->
          <form class="auth-form" @submit.prevent="handleRegister">
            <!-- Username Input -->
            <div class="form-group">
              <label class="form-label">用户名</label>
              <div class="input-wrapper">
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                <input
                  v-model="formData.userName"
                  type="text"
                  class="form-input"
                  placeholder="请输入用户名"
                  @keydown="handleKeyDown"
                />
              </div>
            </div>

            <!-- Email Input (Optional) -->
            <div class="form-group">
              <label class="form-label">邮箱 <span class="form-label-optional">(选填)</span></label>
              <div class="input-wrapper">
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                  <polyline points="22,6 12,13 2,6" />
                </svg>
                <input
                  v-model="formData.userEmail"
                  type="email"
                  class="form-input"
                  placeholder="请输入邮箱地址（选填）"
                  @keydown="handleKeyDown"
                />
              </div>
            </div>

            <!-- Account Input -->
            <div class="form-group">
              <label class="form-label">账号</label>
              <div class="input-wrapper">
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="4" width="20" height="16" rx="2" />
                  <path d="M12 12h.01" />
                  <path d="M17 12h.01" />
                  <path d="M7 12h.01" />
                </svg>
                <input
                  v-model="formData.userAccount"
                  type="text"
                  class="form-input"
                  placeholder="请输入账号"
                  @keydown="handleKeyDown"
                />
              </div>
            </div>

            <!-- Password Input -->
            <div class="form-group">
              <label class="form-label">密码</label>
              <div class="input-wrapper">
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
                <input
                  v-model="formData.userPassword"
                  type="password"
                  class="form-input"
                  placeholder="请输入密码"
                  @keydown="handleKeyDown"
                />
              </div>
            </div>

            <!-- Confirm Password Input -->
            <div class="form-group">
              <label class="form-label">确认密码</label>
              <div class="input-wrapper">
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                </svg>
                <input
                  v-model="formData.checkPassword"
                  type="password"
                  class="form-input"
                  placeholder="请再次输入密码"
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

            <!-- Success Message -->
            <div v-if="successMessage" class="success-message">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
              <span>{{ successMessage }}</span>
            </div>

            <!-- Submit Button -->
            <button type="submit" class="auth-btn" :disabled="isLoading">
              <span v-if="!isLoading">注册</span>
              <span v-else class="loading-text">注册中...</span>
            </button>
          </form>

          <!-- Footer Link -->
          <div class="auth-footer">
            <span class="footer-text">已有账号？</span>
            <router-link to="/login" class="footer-link">去登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
