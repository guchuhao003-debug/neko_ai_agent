<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChatRoom from '../components/ChatRoom.vue'
import { getAgent } from '../api/agent'
import { useUser } from '../composables/useUser'

const route = useRoute()
const router = useRouter()
const { currentUser, fetchUser } = useUser()
const agent = ref(null)
const chatId = ref(`AGENT-${route.params.id}-${Date.now()}`)
const loading = ref(true)
const errorText = ref('')

const agentId = computed(() => route.params.id)
const sessionTitle = computed(() => agent.value?.name || '自定义智能体')
const agentAvatar = computed(() => agent.value?.avatar || 'NM')

onMounted(async () => {
  if (!currentUser.value) {
    await fetchUser()
  }
  if (!currentUser.value) {
    router.push('/login')
    return
  }
  try {
    const res = await getAgent(agentId.value)
    if (res.data.code === 0) {
      agent.value = res.data.data
      return
    }
    errorText.value = res.data.message || '智能体不存在'
  } catch (error) {
    errorText.value = error?.response?.data?.message || '加载智能体失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-if="loading" class="page agent-chat-loading">加载智能体中...</div>
  <main v-else-if="errorText" class="page agent-chat-error">
    <div class="agent-chat-error-box">
      <h1>{{ errorText }}</h1>
      <button class="agent-primary-btn" @click="router.push('/agents')">返回工作台</button>
    </div>
  </main>
  <ChatRoom
    v-else
    v-model:chat-id="chatId"
    :title="agent.name"
    sse-path="/agent/chat/sse"
    :use-chat-id="true"
    :chat-id="chatId"
    :session-title="sessionTitle"
    :session-id="chatId"
    :ai-name="agent.name"
    :ai-avatar="agentAvatar"
    :show-model-selector="false"
    :extra-params="{ agentId: agent.id }"
  />
</template>
