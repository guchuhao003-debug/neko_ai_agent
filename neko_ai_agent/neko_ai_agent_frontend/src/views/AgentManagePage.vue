<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listAllAgents, getAgent, updateAgent, deleteAgent } from '../api/agent'

const router = useRouter()

const agentList = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const isLoading = ref(false)

const searchId = ref('')

const showEditModal = ref(false)
const editForm = ref({
  id: '',
  name: '',
  isPublic: false,
  status: true,
})
const editError = ref('')
const editLoading = ref(false)

const toastMsg = ref('')
const toastType = ref('')

const showToast = (msg, type = 'success') => {
  toastMsg.value = msg
  toastType.value = type
  setTimeout(() => { toastMsg.value = '' }, 2500)
}

const fetchAgents = async () => {
  isLoading.value = true
  try {
    const response = await listAllAgents({
      current: current.value,
      pageSize: pageSize.value,
    })
    if (response.data.code === 0) {
      agentList.value = response.data.data.records || []
      total.value = response.data.data.total || 0
    } else {
      showToast(response.data.message || '获取智能体列表失败', 'error')
    }
  } catch {
    showToast('获取智能体列表失败', 'error')
  } finally {
    isLoading.value = false
  }
}

const handleSearch = async () => {
  if (!searchId.value.trim()) {
    fetchAgents()
    return
  }
  isLoading.value = true
  try {
    const response = await getAgent(searchId.value.trim())
    if (response.data.code === 0 && response.data.data) {
      agentList.value = [response.data.data]
      total.value = 1
    } else {
      agentList.value = []
      total.value = 0
      showToast('未找到该智能体', 'error')
    }
  } catch {
    agentList.value = []
    total.value = 0
    showToast('查询失败', 'error')
  } finally {
    isLoading.value = false
  }
}

const clearSearch = () => {
  searchId.value = ''
  current.value = 1
  fetchAgents()
}

const handleDelete = async (agent) => {
  if (!confirm(`确认删除智能体「${agent.name}」？此操作不可逆。`)) return
  try {
    const response = await deleteAgent(agent.id)
    if (response.data.code === 0) {
      showToast('删除成功')
      fetchAgents()
    } else {
      showToast(response.data.message || '删除失败', 'error')
    }
  } catch {
    showToast('删除失败', 'error')
  }
}

const openEditModal = (agent) => {
  editForm.value = {
    id: agent.id,
    name: agent.name || '',
    isPublic: Boolean(agent.isPublic),
    status: agent.status !== false,
  }
  editError.value = ''
  showEditModal.value = true
}

const handleEdit = async () => {
  editError.value = ''
  if (!editForm.value.name.trim()) {
    editError.value = '名称不能为空'
    return
  }
  editLoading.value = true
  try {
    const response = await updateAgent(editForm.value)
    if (response.data.code === 0) {
      showToast('修改成功')
      showEditModal.value = false
      fetchAgents()
    } else {
      editError.value = response.data.message || '修改失败'
    }
  } catch {
    editError.value = '修改失败，请检查网络'
  } finally {
    editLoading.value = false
  }
}

const totalPages = () => Math.ceil(total.value / pageSize.value) || 1

const goPage = (page) => {
  if (page < 1 || page > totalPages()) return
  current.value = page
  fetchAgents()
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchAgents()
})
</script>

<template>
  <main class="page user-manage-page">
    <div class="um-container">
      <div class="um-header">
        <button class="um-back-btn" @click="router.push('/')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          <span>返回主页</span>
        </button>
        <h1 class="um-title">智能体管理</h1>
        <p class="um-subtitle">管理平台所有自定义智能体</p>
      </div>

      <div class="um-toolbar">
        <div class="um-search">
          <input
            v-model="searchId"
            type="text"
            class="um-search-input"
            placeholder="输入智能体 ID 搜索"
            @keydown.enter="handleSearch"
          />
          <button class="um-btn um-btn-search" @click="handleSearch">搜索</button>
          <button v-if="searchId" class="um-btn um-btn-clear" @click="clearSearch">清除</button>
        </div>
      </div>

      <Transition name="fade">
        <div v-if="toastMsg" class="um-toast" :class="toastType === 'error' ? 'um-toast-error' : 'um-toast-success'">
          {{ toastMsg }}
        </div>
      </Transition>

      <div class="um-table-wrap">
        <div v-if="isLoading" class="um-loading">加载中...</div>
        <table v-else class="um-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>头像</th>
              <th>名称</th>
              <th>创建者 ID</th>
              <th>可见性</th>
              <th>状态</th>
              <th>使用次数</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="agentList.length === 0">
              <td colspan="9" class="um-empty">暂无数据</td>
            </tr>
            <tr v-for="agent in agentList" :key="agent.id">
              <td class="um-td-id">{{ agent.id }}</td>
              <td>
                <div class="um-avatar">
                  <img v-if="agent.avatar" :src="agent.avatar" :alt="agent.name" />
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="5" y="8" width="14" height="12" rx="2" fill="rgba(100,150,255,0.15)" />
                    <path d="M12 2v4M8.5 8v-1a1 1 0 0 1 1-1h5a1 1 0 0 1 1 1v1" />
                    <circle cx="9" cy="13" r="1" fill="currentColor" />
                    <circle cx="15" cy="13" r="1" fill="currentColor" />
                    <path d="M9 17h6" />
                  </svg>
                </div>
              </td>
              <td>{{ agent.name || '-' }}</td>
              <td class="um-td-id">{{ agent.userId || '-' }}</td>
              <td>
                <span class="um-role-tag" :class="agent.isPublic ? 'um-role-admin' : ''">
                  {{ agent.isPublic ? '公开' : '私有' }}
                </span>
              </td>
              <td>
                <span class="um-role-tag" :class="agent.status ? 'um-role-admin' : 'um-role-disabled'">
                  {{ agent.status ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ agent.useCount || 0 }}</td>
              <td>{{ formatDate(agent.createTime) }}</td>
              <td class="um-td-actions">
                <button class="um-action-btn um-action-edit" @click="openEditModal(agent)">编辑</button>
                <button class="um-action-btn um-action-delete" @click="handleDelete(agent)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="total > pageSize" class="um-pagination">
        <button class="um-page-btn" :disabled="current <= 1" @click="goPage(current - 1)">上一页</button>
        <span class="um-page-info">{{ current }} / {{ totalPages() }}</span>
        <button class="um-page-btn" :disabled="current >= totalPages()" @click="goPage(current + 1)">下一页</button>
      </div>
    </div>

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
            <h2 class="um-modal-title">编辑智能体</h2>
            <div class="um-modal-form">
              <div class="um-form-group">
                <label>名称</label>
                <input v-model="editForm.name" type="text" placeholder="请输入名称" maxlength="50" />
              </div>
              <div class="um-form-group">
                <label>可见性</label>
                <select v-model="editForm.isPublic">
                  <option :value="false">私有</option>
                  <option :value="true">公开</option>
                </select>
              </div>
              <div class="um-form-group">
                <label>状态</label>
                <select v-model="editForm.status">
                  <option :value="true">启用</option>
                  <option :value="false">停用</option>
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
