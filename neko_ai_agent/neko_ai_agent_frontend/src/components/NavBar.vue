<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUser } from '../composables/useUser'
import { GlobalUpdateUser, uploadAvatar } from '../api/user'

const router = useRouter()
const { currentUser, fetchUser, logout } = useUser()

const showDropdown = ref(false)
const showProfileModal = ref(false)
const showAnnouncementModal = ref(false)
const isEditing = ref(false)
const isSaving = ref(false)
const editError = ref('')
const editSuccess = ref('')
const isUploadingAvatar = ref(false)
const avatarFileInput = ref(null)
const avatarMsg = ref('')
const avatarMsgType = ref('')

const editForm = ref({
  userName: '',
  userEmail: '',
  userProfile: ''
})

onMounted(async () => {
  await fetchUser()
  // 登录后自动弹出系统公告（延迟 3 秒）
  if (sessionStorage.getItem('just_logged_in')) {
    sessionStorage.removeItem('just_logged_in')
    setTimeout(() => {
      showAnnouncementModal.value = true
    }, 1000)
  }
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

const openAnnouncement = () => {
  showAnnouncementModal.value = true
}

const closeAnnouncement = () => {
  showAnnouncementModal.value = false
}

const goToUserManage = () => {
  closeDropdown()
  router.push('/admin/users')
}

const goToAgentManage = () => {
  closeDropdown()
  router.push('/admin/agents')
}

const getAvatarText = () => {
  if (!currentUser.value) return ''
  const name = currentUser.value.userName || currentUser.value.userAccount || ''
  return name.charAt(0).toUpperCase()
}

const triggerAvatarUpload = () => {
  avatarFileInput.value?.click()
}

const handleAvatarChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  // 前端校验文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  if (!allowedTypes.includes(file.type)) {
    avatarMsg.value = '不支持的图片格式，仅支持 jpg/png/webp/gif'
    avatarMsgType.value = 'error'
    return
  }
  // 前端校验文件大小（2MB）
  if (file.size > 2 * 1024 * 1024) {
    avatarMsg.value = '图片大小不能超过 2MB'
    avatarMsgType.value = 'error'
    return
  }

  isUploadingAvatar.value = true
  avatarMsg.value = ''
  try {
    const response = await uploadAvatar(file)
    if (response.data.code === 0) {
      await fetchUser()
      avatarMsg.value = '头像更新成功'
      avatarMsgType.value = 'success'
      setTimeout(() => { avatarMsg.value = '' }, 2500)
    } else {
      avatarMsg.value = response.data.message || '头像上传失败'
      avatarMsgType.value = 'error'
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    avatarMsg.value = '头像上传失败，请检查网络连接'
    avatarMsgType.value = 'error'
  } finally {
    isUploadingAvatar.value = false
    // 清空 input 值，允许重复选择同一文件
    if (avatarFileInput.value) {
      avatarFileInput.value.value = ''
    }
  }
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
        <!-- Custom Agent Button -->
        <router-link to="/agents" class="navbar-agent-btn" title="自定义智能体">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M12 3l7 4v6c0 4.2-2.8 7.2-7 8-4.2-.8-7-3.8-7-8V7l7-4z" />
            <path d="M9 12h6M12 9v6" />
          </svg>
          <span>自定义智能体</span>
        </router-link>

        <!-- Announcement Button -->
        <button class="navbar-notify-btn" title="系统公告" @click="openAnnouncement">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 0 1-3.46 0" />
          </svg>
          <span class="notify-dot"></span>
        </button>

        <!-- Docs Button -->
        <router-link to="/docs" class="navbar-docs-btn" title="操作文档">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
            <line x1="8" y1="7" x2="16" y2="7" />
            <line x1="8" y1="11" x2="14" y2="11" />
          </svg>
          <span>操作文档</span>
        </router-link>

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
              <button v-if="currentUser.userRole === 'admin'" class="dropdown-item" @click="goToUserManage">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                  <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
                <span>用户管理</span>
              </button>
              <button v-if="currentUser.userRole === 'admin'" class="dropdown-item" @click="goToAgentManage">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="5" y="8" width="14" height="12" rx="2" />
                  <path d="M12 2v4M8.5 8v-1a1 1 0 0 1 1-1h5a1 1 0 0 1 1 1v1" />
                  <circle cx="9" cy="13" r="1" fill="currentColor" />
                  <circle cx="15" cy="13" r="1" fill="currentColor" />
                  <path d="M9 17h6" />
                </svg>
                <span>智能体管理</span>
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
            <!-- Avatar with upload -->
            <div class="profile-avatar-wrap profile-avatar-clickable" @click="triggerAvatarUpload">
              <div v-if="currentUser?.userAvatar" class="profile-avatar-img">
                <img :src="currentUser.userAvatar" :alt="currentUser.userName" />
              </div>
              <div v-else class="profile-avatar-text">
                {{ getAvatarText() }}
              </div>
              <!-- Upload overlay -->
              <div class="profile-avatar-overlay" :class="{ 'is-uploading': isUploadingAvatar }">
                <svg v-if="!isUploadingAvatar" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
                  <circle cx="12" cy="13" r="4" />
                </svg>
                <div v-else class="avatar-upload-spinner"></div>
              </div>
            </div>
            <input
              ref="avatarFileInput"
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              style="display: none"
              @change="handleAvatarChange"
            />
            <h2 class="profile-name">{{ currentUser?.userName || '用户' }}</h2>
            <p class="profile-role">{{ currentUser?.userRole === 'admin' ? '管理员' : '普通用户' }}</p>
            <!-- Avatar upload feedback -->
            <Transition name="fade">
              <div v-if="avatarMsg" class="profile-avatar-msg" :class="avatarMsgType === 'success' ? 'profile-edit-msg-success' : 'profile-edit-msg-error'">
                {{ avatarMsg }}
              </div>
            </Transition>
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

  <!-- Announcement Modal -->
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="showAnnouncementModal" class="modal-overlay" @click="closeAnnouncement">
        <div class="modal-container announcement-modal" @click.stop>
          <button class="modal-close" @click="closeAnnouncement">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>

          <div class="announcement-header">
            <svg class="announcement-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
            </svg>
            <h2 class="announcement-title">Neko AI Agent 系统公告</h2>
            <span class="announcement-badge">v1.0.0</span>
          </div>

          <div class="announcement-body">
            <div class="announcement-section">
              <h3>Neko AI Agent v1.0.0 正式发布</h3>
              <p class="announcement-date">发布日期：2026 年 5 月</p>
              <p>我们很高兴地宣布，<strong>Neko AI Agent 智能体平台</strong> 1.0.0 版本正式上线！这是一个基于 Spring AI + Vue 3 构建的多智能体协作平台，为用户提供多场景 AI 对话体验。</p>
            </div>

            <div class="announcement-section">
              <h3>核心功能</h3>
              <ul>
                <li><strong>AI 恋爱大师</strong> — 基于 RAG 增强检索，融合情感分析与多轮对话记忆，提供专业恋爱咨询建议</li>
                <li><strong>NekoMenus 超级智能体</strong> — 采用 ReAct 推理-行动模式，支持联网搜索、文件操作、PDF 生成、邮件发送等 10+ 工具的自主规划与执行</li>
                <li><strong>AI 宠物专家</strong> — 集成 MCP 协议连接高德地图等外部服务，结合文件记忆系统，提供科学养宠建议</li>
              </ul>
            </div>

            <div class="announcement-section">
              <h3>技术亮点</h3>
              <ul>
                <li>SSE 实时流式输出，打字机效果逐字呈现</li>
                <li>深度思考面板：可视化 Agent 推理过程，支持折叠展开</li>
                <li>Markdown 渲染：AI 回复支持结构化格式输出</li>
                <li>MCP 协议集成：标准化连接第三方工具与服务</li>
                <li>会话记忆：基于文件持久化的多轮对话上下文</li>
              </ul>
            </div>

            <p class="announcement-footer-text">感谢你使用 Neko AI Agent，期待你的反馈与建议！</p>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
