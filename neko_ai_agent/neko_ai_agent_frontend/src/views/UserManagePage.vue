<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listUserByPage, addUser, deleteUser, updateUser, getUserById } from '../api/user'

const router = useRouter()

// 用户列表数据
const userList = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const isLoading = ref(false)

// 搜索
const searchId = ref('')

// 添加用户弹窗
const showAddModal = ref(false)
const addForm = ref({
  userName: '',
  userAccount: '',
  userRole: 'user'
})
const addError = ref('')
const addLoading = ref(false)

// 编辑用户弹窗
const showEditModal = ref(false)
const editForm = ref({
  id: '',
  userName: '',
  userEmail: '',
  userProfile: '',
  userRole: 'user'
})
const editError = ref('')
const editLoading = ref(false)

// 消息提示
const toastMsg = ref('')
const toastType = ref('')

const showToast = (msg, type = 'success') => {
  toastMsg.value = msg
  toastType.value = type
  setTimeout(() => { toastMsg.value = '' }, 2500)
}

// 加载用户列表
const fetchUsers = async () => {
  isLoading.value = true
  try {
    const response = await listUserByPage({
      current: current.value,
      pageSize: pageSize.value,
      sortField: 'createTime',
      sortOrder: 'descend'
    })
    if (response.data.code === 0) {
      userList.value = response.data.data.records || []
      total.value = response.data.data.total || 0
    } else {
      showToast(response.data.message || '获取用户列表失败', 'error')
    }
  } catch (error) {
    showToast('获取用户列表失败', 'error')
  } finally {
    isLoading.value = false
  }
}

// 搜索用户
const handleSearch = async () => {
  if (!searchId.value.trim()) {
    fetchUsers()
    return
  }
  isLoading.value = true
  try {
    const response = await getUserById(searchId.value.trim())
    if (response.data.code === 0 && response.data.data) {
      userList.value = [response.data.data]
      total.value = 1
    } else {
      userList.value = []
      total.value = 0
      showToast('未找到该用户', 'error')
    }
  } catch (error) {
    userList.value = []
    total.value = 0
    showToast('查询失败', 'error')
  } finally {
    isLoading.value = false
  }
}

const clearSearch = () => {
  searchId.value = ''
  current.value = 1
  fetchUsers()
}

// 添加用户
const openAddModal = () => {
  addForm.value = { userName: '', userAccount: '', userRole: 'user' }
  addError.value = ''
  showAddModal.value = true
}

const handleAdd = async () => {
  addError.value = ''
  if (!addForm.value.userName.trim()) {
    addError.value = '请输入用户名'
    return
  }
  if (!addForm.value.userAccount.trim()) {
    addError.value = '请输入账号'
    return
  }
  addLoading.value = true
  try {
    const response = await addUser(addForm.value)
    if (response.data.code === 0) {
      showToast('用户创建成功，默认密码: 12345678')
      showAddModal.value = false
      fetchUsers()
    } else {
      addError.value = response.data.message || '创建失败'
    }
  } catch (error) {
    addError.value = '创建失败，请检查网络'
  } finally {
    addLoading.value = false
  }
}

// 删除用户
const handleDelete = async (user) => {
  if (!confirm(`确认删除用户「${user.userName || user.userAccount}」？此操作不可逆。`)) return
  try {
    const response = await deleteUser(user.id)
    if (response.data.code === 0) {
      showToast('删除成功')
      fetchUsers()
    } else {
      showToast(response.data.message || '删除失败', 'error')
    }
  } catch (error) {
    showToast('删除失败', 'error')
  }
}

// 编辑用户
const openEditModal = (user) => {
  editForm.value = {
    id: user.id,
    userName: user.userName || '',
    userEmail: user.userEmail || '',
    userProfile: user.userProfile || '',
    userRole: user.userRole || 'user'
  }
  editError.value = ''
  showEditModal.value = true
}

const handleEdit = async () => {
  editError.value = ''
  if (!editForm.value.userName.trim()) {
    editError.value = '用户名不能为空'
    return
  }
  editLoading.value = true
  try {
    const response = await updateUser(editForm.value)
    if (response.data.code === 0) {
      showToast('修改成功')
      showEditModal.value = false
      fetchUsers()
    } else {
      editError.value = response.data.message || '修改失败'
    }
  } catch (error) {
    editError.value = '修改失败，请检查网络'
  } finally {
    editLoading.value = false
  }
}

// 分页
const totalPages = () => Math.ceil(total.value / pageSize.value) || 1

const goPage = (page) => {
  if (page < 1 || page > totalPages()) return
  current.value = page
  fetchUsers()
}

// 格式化时间
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <main class="page user-manage-page">
    <div class="um-container">
      <!-- Header -->
      <div class="um-header">
        <button class="um-back-btn" @click="router.push('/')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          <span>返回主页</span>
        </button>
        <h1 class="um-title">用户管理</h1>
        <p class="um-subtitle">管理平台用户信息</p>
      </div>

      <!-- Toolbar -->
      <div class="um-toolbar">
        <div class="um-search">
          <input
            v-model="searchId"
            type="text"
            class="um-search-input"
            placeholder="输入用户 ID 搜索"
            @keydown.enter="handleSearch"
          />
          <button class="um-btn um-btn-search" @click="handleSearch">搜索</button>
          <button v-if="searchId" class="um-btn um-btn-clear" @click="clearSearch">清除</button>
        </div>
        <button class="um-btn um-btn-add" @click="openAddModal">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          <span>添加用户</span>
        </button>
      </div>

      <!-- Toast -->
      <Transition name="fade">
        <div v-if="toastMsg" class="um-toast" :class="toastType === 'error' ? 'um-toast-error' : 'um-toast-success'">
          {{ toastMsg }}
        </div>
      </Transition>

      <!-- Table -->
      <div class="um-table-wrap">
        <div v-if="isLoading" class="um-loading">加载中...</div>
        <table v-else class="um-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>头像</th>
              <th>用户名</th>
              <th>账号</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="userList.length === 0">
              <td colspan="8" class="um-empty">暂无数据</td>
            </tr>
            <tr v-for="user in userList" :key="user.id">
              <td class="um-td-id">{{ user.id }}</td>
              <td>
                <div class="um-avatar">
                  <img v-if="user.userAvatar" :src="user.userAvatar" :alt="user.userName" />
                  <span v-else>{{ (user.userName || user.userAccount || '?').charAt(0).toUpperCase() }}</span>
                </div>
              </td>
              <td>{{ user.userName || '-' }}</td>
              <td>{{ user.userAccount || '-' }}</td>
              <td>{{ user.userEmail || '-' }}</td>
              <td>
                <span class="um-role-tag" :class="user.userRole === 'admin' ? 'um-role-admin' : ''">
                  {{ user.userRole === 'admin' ? '管理员' : '用户' }}
                </span>
              </td>
              <td>{{ formatDate(user.createTime) }}</td>
              <td class="um-td-actions">
                <button class="um-action-btn um-action-edit" @click="openEditModal(user)">修改</button>
                <button class="um-action-btn um-action-delete" @click="handleDelete(user)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="total > pageSize" class="um-pagination">
        <button class="um-page-btn" :disabled="current <= 1" @click="goPage(current - 1)">上一页</button>
        <span class="um-page-info">{{ current }} / {{ totalPages() }}</span>
        <button class="um-page-btn" :disabled="current >= totalPages()" @click="goPage(current + 1)">下一页</button>
      </div>
    </div>

    <!-- Add User Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showAddModal" class="modal-overlay" @click="showAddModal = false">
          <div class="modal-container um-modal" @click.stop>
            <button class="modal-close" @click="showAddModal = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
            <h2 class="um-modal-title">添加用户</h2>
            <div class="um-modal-form">
              <div class="um-form-group">
                <label>用户名</label>
                <input v-model="addForm.userName" type="text" placeholder="请输入用户名" />
              </div>
              <div class="um-form-group">
                <label>账号</label>
                <input v-model="addForm.userAccount" type="text" placeholder="请输入账号" />
              </div>
              <div class="um-form-group">
                <label>角色</label>
                <select v-model="addForm.userRole">
                  <option value="user">普通用户</option>
                  <option value="admin">管理员</option>
                </select>
              </div>
              <div v-if="addError" class="um-form-error">{{ addError }}</div>
              <div class="um-modal-actions">
                <button class="um-btn um-btn-cancel" @click="showAddModal = false">取消</button>
                <button class="um-btn um-btn-confirm" :disabled="addLoading" @click="handleAdd">
                  {{ addLoading ? '创建中...' : '创建' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Edit User Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showEditModal" class="modal-overlay" @click="showEditModal = false">
          <div class="modal-container um-modal" @click.stop>
            <button class="modal-close" @click="showEditModal = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
            <h2 class="um-modal-title">编辑用户</h2>
            <div class="um-modal-form">
              <div class="um-form-group">
                <label>用户名</label>
                <input v-model="editForm.userName" type="text" placeholder="请输入用户名" />
              </div>
              <div class="um-form-group">
                <label>邮箱</label>
                <input v-model="editForm.userEmail" type="email" placeholder="请输入邮箱" />
              </div>
              <div class="um-form-group">
                <label>简介</label>
                <textarea v-model="editForm.userProfile" placeholder="请输入简介" rows="3" />
              </div>
              <div class="um-form-group">
                <label>角色</label>
                <select v-model="editForm.userRole">
                  <option value="user">普通用户</option>
                  <option value="admin">管理员</option>
                </select>
              </div>
              <div v-if="editError" class="um-form-error">{{ editError }}</div>
              <div class="um-modal-actions">
                <button class="um-btn um-btn-cancel" @click="showEditModal = false">取消</button>
                <button class="um-btn um-btn-confirm" :disabled="editLoading" @click="handleEdit">
                  {{ editLoading ? '保存中...' : '保存' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </main>
</template>
