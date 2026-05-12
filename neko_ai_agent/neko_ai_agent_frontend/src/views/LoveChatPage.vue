<script setup>
import { ref } from 'vue'
import ChatRoom from '../components/ChatRoom.vue'

const chatId = ref(crypto.randomUUID())
</script>

<template>
  <ChatRoom
    title="AI 恋爱大师"
    session-title="恋爱咨询 · 初次见面建议"
    :session-id="chatId"
    sse-path="/ai/love_app/chat/sse"
    :use-chat-id="true"
    v-model:chat-id="chatId"
    ai-name="Neko Love"
    ai-avatar="NL"
    app-type="love"
    :show-model-selector="true"
  />
</template>
