<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUser } from '../composables/useUser'
import { GlobalUpdateUser } from '../api/user'

const router = useRouter()
const { currentUser, fetchUser, logout } = useUser()

const showDropdown = ref(false)
const showProfileModal = ref(false)
const isEditing = ref(false)
const isSaving = ref(false)
const editError = ref('')
const editSuccess = ref('')

const editForm = ref({
  userName: '',
  userEmail: '',
  userProfile: ''
})

onMounted(() => {
  fetchUser()
})

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
}

const closeDropdown = () => {
  showDropdown.value = false
}

const openProfile = () => {
  showProfileModal.value = true
  isEditing.value = false
  editError.value = ''
  editSuccess.value = ''
  closeDropdown()
}

const closeProfile = () => {
  showProfileModal.value = false
  isEditing.value = false
}

const startEdit = () => {
  editForm.value.userName = currentUser.value?.userName || ''
  editForm.value.userEmail = currentUser.value?.userEmail || ''
  editForm.value.userProfile = currentUser.value?.userProfile || ''
  editError.value = ''
  editSuccess.value = ''
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
  editError.value = ''
  editSuccess.value = ''
}

const saveEdit = async () => {
  editError.value = ''
  editSuccess.value = ''

  if (!editForm.value.userName.trim()) {
    editError.value = '用户名不能为空'
    return
  }

  isSaving.value = true

  try {
    const response = await GlobalUpdateUser({
      id: currentUser.value.id,
      userName: editForm.value.userName.trim(),
      userEmail: editForm.value.userEmail.trim() || null,
      userProfile: editForm.value.userProfile.trim() || null
    })
    if (response.data.code === 0) {
      editSuccess.value = '修改成功'
      await fetchUser()
      setTimeout(() => {
        isEditing.value = false
        editSuccess.value = ''
      }, 1000)
    } else {
      editError.value = response.data.message || '修改失败，请重试'
    }
  } catch (error) {
    console.error('修改失败:', error)
    editError.value = '修改失败，请检查网络连接'
  } finally {
    isSaving.value = false
  }
}

const handleLogout = async () => {
  closeDropdown()
  const success = await logout()
  if (success) {
    router.push('/')
  }
}

const goToLogin = () => {
  router.push('/login')
}

const getAvatarText = () => {
  if (!currentUser.value) return ''
  const name = currentUser.value.userName || currentUser.value.userAccount || ''
  return name.charAt(0).toUpperCase()
}
</script>

<template>
  <nav class="navbar" @mouseleave="closeDropdown">
    <div class="navbar-inner">
      <!-- Logo -->
      <router-link to="/" class="navbar-logo">
        <img src="/neko.png" alt="Neko AI" class="logo-icon" />
        <span class="logo-text">Neko AI</span>
      </router-link>

      <!-- Right Section -->
      <div class="navbar-right">
        <!-- Not Logged In -->
        <button v-if="!currentUser" class="navbar-login-btn" @click="goToLogin">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
            <polyline points="10 17 15 12 10 7" />
            <line x1="15" y1="12" x2="3" y2="12" />
          </svg>
          <span>登录</span>
        </button>

        <!-- Logged In -->
        <div v-else class="user-menu">
          <button class="avatar-btn" @click="toggleDropdown">
            <div v-if="currentUser.userAvatar" class="user-avatar-img">
              <img :src="currentUser.userAvatar" :alt="currentUser.userName" />
            </div>
            <div v-else class="user-avatar-text">
              {{ getAvatarText() }}
            </div>
          </button>

          <!-- Dropdown -->
          <Transition name="dropdown">
            <div v-if="showDropdown" class="dropdown-menu">
              <div class="dropdown-header">
                <span class="dropdown-username">{{ currentUser.userName || '用户' }}</span>
                <span class="dropdown-account">{{ currentUser.userAccount }}</span>
              </div>
              <div class="dropdown-divider" />
              <button class="dropdown-item" @click="openProfile">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                <span>我的中心</span>
              </button>
              <button class="dropdown-item dropdown-item-danger" @click="handleLogout">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                  <polyline points="16 17 21 12 16 7" />
                  <line x1="21" y1="12" x2="9" y2="12" />
                </svg>
                <span>退出登录</span>
              </button>
            </div>
          </Transition>
        </div>
      </div>
    </div>
  </nav>

  <!-- Profile Modal -->
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="showProfileModal" class="modal-overlay" @click="closeProfile">
        <div class="modal-container profile-modal" @click.stop>
          <button class="modal-close" @click="closeProfile">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>

          <div class="profile-header">
            <!-- Avatar -->
            <div class="profile-avatar-wrap">
              <div v-if="currentUser?.userAvatar" class="profile-avatar-img">
                <img :src="currentUser.userAvatar" :alt="currentUser.userName" />
              </div>
              <div v-else class="profile-avatar-text">
                {{ getAvatarText() }}
              </div>
            </div>
            <h2 class="profile-name">{{ currentUser?.userName || '用户' }}</h2>
            <p class="profile-role">{{ currentUser?.userRole === 'admin' ? '管理员' : '普通用户' }}</p>
          </div>

          <!-- Display Mode -->
          <div v-if="!isEditing" class="profile-body">
            <div class="profile-item">
              <div class="profile-item-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="4" width="20" height="16" rx="2" />
                  <path d="M12 12h.01" />
                  <path d="M17 12h.01" />
                  <path d="M7 12h.01" />
                </svg>
              </div>
              <div class="profile-item-info">
                <span class="profile-item-label">账号</span>
                <span class="profile-item-value">{{ currentUser?.userAccount || '-' }}</span>
              </div>
            </div>

            <div class="profile-item">
              <div class="profile-item-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
              </div>
              <div class="profile-item-info">
                <span class="profile-item-label">用户名</span>
                <span class="profile-item-value">{{ currentUser?.userName || '-' }}</span>
              </div>
            </div>

            <div class="profile-item">
              <div class="profile-item-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                  <polyline points="22,6 12,13 2,6" />
                </svg>
              </div>
              <div class="profile-item-info">
                <span class="profile-item-label">邮箱</span>
                <span class="profile-item-value">{{ currentUser?.userEmail || '未绑定' }}</span>
              </div>
            </div>

            <div class="profile-item">
              <div class="profile-item-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="4" y1="9" x2="20" y2="9" />
                  <line x1="4" y1="15" x2="20" y2="15" />
                  <line x1="10" y1="3" x2="8" y2="21" />
                  <line x1="16" y1="3" x2="14" y2="21" />
                </svg>
              </div>
              <div class="profile-item-info">
                <span class="profile-item-label">简介</span>
                <span class="profile-item-value">{{ currentUser?.userProfile || '这个人很懒，什么都没写~' }}</span>
              </div>
            </div>

            <button class="profile-edit-btn" @click="startEdit">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
              </svg>
              <span>编辑信息</span>
            </button>
          </div>

          <!-- Edit Mode -->
          <div v-else class="profile-body profile-edit-form">
            <div class="profile-edit-group">
              <label class="profile-edit-label">账号</label>
              <input
                type="text"
                class="profile-edit-input profile-edit-input-disabled"
                :value="currentUser?.userAccount"
                disabled
              />
            </div>

            <div class="profile-edit-group">
              <label class="profile-edit-label">用户名</label>
              <input
                v-model="editForm.userName"
                type="text"
                class="profile-edit-input"
                placeholder="请输入用户名"
              />
            </div>

            <div class="profile-edit-group">
              <label class="profile-edit-label">邮箱</label>
              <input
                v-model="editForm.userEmail"
                type="email"
                class="profile-edit-input"
                placeholder="请输入邮箱地址"
              />
            </div>

            <div class="profile-edit-group">
              <label class="profile-edit-label">简介</label>
              <textarea
                v-model="editForm.userProfile"
                class="profile-edit-input profile-edit-textarea"
                placeholder="请输入个人简介"
                rows="3"
              />
            </div>

            <!-- Error/Success Message -->
            <div v-if="editError" class="profile-edit-msg profile-edit-msg-error">{{ editError }}</div>
            <div v-if="editSuccess" class="profile-edit-msg profile-edit-msg-success">{{ editSuccess }}</div>

            <div class="profile-edit-actions">
              <button class="profile-edit-cancel" @click="cancelEdit">取消</button>
              <button class="profile-edit-save" :disabled="isSaving" @click="saveEdit">
                <span v-if="!isSaving">保存</span>
                <span v-else>保存中...</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
