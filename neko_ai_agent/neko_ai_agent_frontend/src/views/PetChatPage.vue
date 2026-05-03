<script setup>
import { ref } from 'vue'
import ChatRoom from '../components/ChatRoom.vue'

const chatId = ref(crypto.randomUUID())
</script>

<template>
  <ChatRoom
    title="AI 宠物专家"
    session-title="宠物咨询 · 专业养护建议"
    :session-id="chatId"
    sse-path="/ai/pet_app/chat/sse"
    :use-chat-id="true"
    :chat-id="chatId"
    ai-name="Neko Pet"
    ai-avatar="NP"
  />
</template>
