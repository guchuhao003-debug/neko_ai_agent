<script setup>
import { ref } from 'vue'
import ChatRoom from '../components/ChatRoom.vue'

const chatId = ref(`MANUS-${Date.now()}`)
</script>

<template>
  <ChatRoom
    title="AI 超级智能体"
    session-title="超级智能体任务会话"
    :session-id="chatId"
    sse-path="/ai/manus/chat"
    :step-bubble-mode="true"
    v-model:chat-id="chatId"
    ai-name="NekoMenus"
    ai-avatar="NM"
    app-type="manus"
    :show-model-selector="true"
  />
</template>
