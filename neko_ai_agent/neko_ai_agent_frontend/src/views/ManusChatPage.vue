<script setup>
import { ref } from 'vue'
import ChatRoom from '../components/ChatRoom.vue'

const sessionId = ref(`MANUS-${Date.now()}`)
</script>

<template>
  <ChatRoom
    title="AI 超级智能体"
    session-title="超级智能体任务会话"
    :session-id="sessionId"
    sse-path="/ai/manus/chat"
    :step-bubble-mode="true"
    ai-name="NekoMenus"
    ai-avatar="NM"
  />
</template>
