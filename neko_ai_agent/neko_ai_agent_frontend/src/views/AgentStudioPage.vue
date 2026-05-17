<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  createAgent,
  deleteAgent,
  listMyAgents,
  listPublicAgents,
  updateAgent,
  uploadAgentAvatar,
} from '../api/agent'
import { useUser } from '../composables/useUser'

const router = useRouter()
const { currentUser, fetchUser } = useUser()

const myAgents = ref([])
const publicAgents = ref([])
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const avatarInput = ref(null)
const activeTab = ref('mine')
const editingAgent = ref(null)
const notice = ref('')

const emptyForm = {
  name: '',
  avatar: '',
  systemPrompt: '',
  modelId: 'deepseek-chat',
  temperature: 0.7,
  maxTokens: 2048,
  isPublic: false,
  status: true,
}

const form = reactive({ ...emptyForm })

const isEditing = computed(() => Boolean(editingAgent.value?.id))
const visibleAgents = computed(() => activeTab.value === 'mine' ? myAgents.value : publicAgents.value)

const resetForm = () => {
  Object.assign(form, emptyForm)
  editingAgent.value = null
}

const showNotice = (message) => {
  notice.value = message
  setTimeout(() => {
    if (notice.value === message) {
      notice.value = ''
    }
  }, 2600)
}

const normalizeRecords = (response) => {
  const page = response?.data?.data
  return page?.records || []
}

const handleAvatarUpload = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  if (!allowed.includes(file.type)) {
    showNotice('仅支持 jpg/png/webp/gif 格式')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    showNotice('图片大小不能超过 2MB')
    return
  }
  uploading.value = true
  try {
    const res = await uploadAgentAvatar(file)
    if (res.data.code === 0 && res.data.data) {
      form.avatar = res.data.data
      showNotice('头像上传成功')
    } else {
      showNotice(res.data.message || '上传失败')
    }
  } catch {
    showNotice('头像上传失败')
  } finally {
    uploading.value = false
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

const loadAgents = async () => {
  loading.value = true
  try {
    const tasks = [listPublicAgents({ current: 1, pageSize: 24 })]
    if (currentUser.value) {
      tasks.push(listMyAgents({ current: 1, pageSize: 24 }))
    }
    const results = await Promise.all(tasks)
    publicAgents.value = normalizeRecords(results[0])
    myAgents.value = currentUser.value ? normalizeRecords(results[1]) : []
  } finally {
    loading.value = false
  }
}

const submitAgent = async () => {
  if (!currentUser.value) {
    showNotice('请先登录后再创建智能体')
    router.push('/login')
    return
  }
  if (!form.name.trim() || !form.systemPrompt.trim() || !form.modelId) {
    showNotice('请填写名称、提示词和模型')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      name: form.name.trim(),
      avatar: form.avatar.trim(),
      systemPrompt: form.systemPrompt.trim(),
      temperature: Number(form.temperature),
      maxTokens: Number(form.maxTokens),
    }
    if (isEditing.value) {
      await updateAgent({ ...payload, id: editingAgent.value.id })
      showNotice('智能体已更新')
    } else {
      await createAgent(payload)
      showNotice('智能体已创建')
    }
    resetForm()
    await loadAgents()
  } finally {
    saving.value = false
  }
}

const editAgent = (agent) => {
  editingAgent.value = agent
  Object.assign(form, {
    name: agent.name || '',
    avatar: agent.avatar || '',
    systemPrompt: agent.systemPrompt || '',
    modelId: agent.modelId || 'deepseek-chat',
    temperature: Number(agent.temperature ?? 0.7),
    maxTokens: Number(agent.maxTokens ?? 2048),
    isPublic: Boolean(agent.isPublic),
    status: agent.status !== false,
  })
  activeTab.value = 'mine'
}

const removeAgent = async (agent) => {
  if (!confirm(`确认删除智能体「${agent.name}」吗？`)) return
  await deleteAgent(agent.id)
  showNotice('智能体已删除')
  if (editingAgent.value?.id === agent.id) {
    resetForm()
  }
  await loadAgents()
}

const openChat = (agent) => {
  router.push(`/agent/${agent.id}/chat`)
}

onMounted(async () => {
  if (!currentUser.value) {
    await fetchUser()
  }
  if (!currentUser.value) {
    router.push('/login')
    return
  }
  resetForm()
  await loadAgents()
})
</script>

<template>
  <main class="page agent-studio-page">
    <!-- Sparkle Stars (复用全局样式) -->
    <div class="sparkle-container">
      <div class="sparkle-star" v-for="n in 20" :key="'star-' + n" />
    </div>

    <section class="agent-studio-shell">
      <header class="agent-studio-header">
        <div>
          <router-link to="/" class="agent-back-link">
            <span class="back-arrow">‹</span>
            <span>返回首页</span>
          </router-link>
          <p class="agent-eyebrow">Custom Agent Studio</p>
          <h1 class="agent-studio-title">自定义智能体</h1>
          <p class="agent-studio-subtitle">创建提示词、选择模型，并把你的专属助手投入对话。</p>
          <div class="agent-studio-divider"></div>
        </div>
      </header>

      <Transition name="toast-fade">
        <div v-if="notice" class="agent-notice">{{ notice }}</div>
      </Transition>

      <div class="agent-studio-layout">
        <section class="agent-form-panel">
          <div class="agent-panel-heading">
            <h2>{{ isEditing ? '编辑智能体' : '创建智能体' }}</h2>
            <span>{{ isEditing ? `ID ${editingAgent.id}` : 'New' }}</span>
          </div>

          <label class="agent-field">
            <span>名称</span>
            <input v-model="form.name" maxlength="50" placeholder="例如：代码评审助手" />
          </label>

          <div class="agent-field">
            <span>头像</span>
            <div class="agent-avatar-upload" @click="avatarInput?.click()">
              <img v-if="form.avatar" :src="form.avatar" alt="头像预览" class="agent-avatar-preview" />
              <div v-else class="agent-avatar-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 5v14M5 12h14" />
                </svg>
                <span>上传头像</span>
              </div>
              <div v-if="uploading" class="agent-avatar-loading">
                <div class="agent-avatar-spinner"></div>
              </div>
              <div v-else-if="form.avatar" class="agent-avatar-overlay">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/>
                  <circle cx="12" cy="13" r="4"/>
                </svg>
              </div>
            </div>
            <input ref="avatarInput" type="file" accept="image/jpeg,image/png,image/webp,image/gif" style="display:none" @change="handleAvatarUpload" />
          </div>

          <label class="agent-field">
            <span>系统提示词</span>
            <textarea
              v-model="form.systemPrompt"
              maxlength="4000"
              placeholder="定义角色、边界、回答风格和任务目标"
            />
          </label>

          <div class="agent-form-grid">
            <label class="agent-field">
              <span>温度 {{ form.temperature }}</span>
              <input v-model.number="form.temperature" type="range" min="0" max="2" step="0.1" />
            </label>
            <label class="agent-field">
              <span>最大 Token</span>
              <input v-model.number="form.maxTokens" type="number" min="256" max="8192" step="256" />
            </label>
          </div>

          <div class="agent-switch-row">
            <label>
              <input v-model="form.isPublic" type="checkbox" />
              <span>公开到广场</span>
            </label>
            <label>
              <input v-model="form.status" type="checkbox" />
              <span>启用</span>
            </label>
          </div>

          <div class="agent-form-actions">
            <button class="agent-secondary-btn" @click="resetForm">重置</button>
            <button class="agent-primary-btn" :disabled="saving" @click="submitAgent">
              {{ saving ? '保存中...' : (isEditing ? '保存修改' : '创建智能体') }}
            </button>
          </div>
        </section>

        <section class="agent-list-panel">
          <div class="agent-tabs">
            <button :class="{ active: activeTab === 'mine' }" @click="activeTab = 'mine'">
              我的智能体
            </button>
            <button :class="{ active: activeTab === 'public' }" @click="activeTab = 'public'">
              公开广场
            </button>
          </div>

          <div v-if="loading" class="agent-empty">加载中...</div>
          <div v-else-if="visibleAgents.length === 0" class="agent-empty">
            {{ activeTab === 'mine' ? '还没有创建智能体' : '暂无公开智能体' }}
          </div>
          <div v-else class="agent-list-grid">
            <article v-for="agent in visibleAgents" :key="agent.id" class="agent-item-card">
              <div class="agent-item-top">
                <img v-if="agent.avatar" :src="agent.avatar" alt="" class="agent-item-avatar" />
                <div v-else class="agent-item-avatar agent-item-avatar-fallback">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="5" y="8" width="14" height="12" rx="2" fill="rgba(100,150,255,0.15)" />
                    <path d="M12 2v4M8.5 8v-1a1 1 0 0 1 1-1h5a1 1 0 0 1 1 1v1" />
                    <circle cx="9" cy="13" r="1" fill="currentColor" />
                    <circle cx="15" cy="13" r="1" fill="currentColor" />
                    <path d="M9 17h6" />
                  </svg>
                </div>
                <div>
                  <h3>{{ agent.name }}</h3>
                </div>
              </div>
              <div class="agent-item-meta">
                <span>{{ agent.isPublic ? '公开' : '私有' }}</span>
                <span>{{ agent.status ? '启用' : '停用' }}</span>
                <span>{{ agent.useCount || 0 }} 次使用</span>
              </div>
              <div class="agent-item-actions">
                <button class="agent-primary-btn" @click="openChat(agent)">开始对话</button>
                <template v-if="activeTab === 'mine'">
                  <button class="agent-secondary-btn" @click="editAgent(agent)">编辑</button>
                  <button class="agent-danger-btn" @click="removeAgent(agent)">删除</button>
                </template>
              </div>
            </article>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>
