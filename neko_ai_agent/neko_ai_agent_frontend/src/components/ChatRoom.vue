<script setup>
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { marked } from 'marked'
import { getApiUrl } from '../api/http'
import { useUser } from '../composables/useUser'

// Configure marked for safe rendering
marked.setOptions({
  breaks: true,
  gfm: true,
})

const renderMarkdown = (text) => {
  if (!text) return ''
  // Normalize literal \n sequences to real newlines for proper markdown parsing
  const normalized = text.replace(/\\n/g, '\n')
  return marked.parse(normalized)
}

const props = defineProps({
  title: { type: String, required: true },
  ssePath: { type: String, required: true },
  useChatId: { type: Boolean, default: false },
  chatId: { type: String, default: '' },
  stepBubbleMode: { type: Boolean, default: false },
  sessionTitle: { type: String, default: '默认会话' },
  sessionId: { type: String, default: '' },
  aiName: { type: String, default: 'Neko AI' },
  aiAvatar: { type: String, default: 'NA' },
})

const { currentUser } = useUser()
const userAvatarUrl = computed(() => currentUser.value?.userAvatar || '')

const messages = ref([])
const inputText = ref('')
const isLoading = ref(false)
const listRef = ref(null)
let source = null
let typingTimer = null
let typingQueue = []
let activeTypingTask = null
let streamEnded = false
let hasReceivedAiContent = false
let lastStepText = ''

// Step thinking mode state
let stepThinkingIndex = -1
let stepCount = 0

const canSend = computed(() => inputText.value.trim().length > 0 && !isLoading.value)
const sessionIdText = computed(() => props.sessionId || 'AUTO')
const sessionIdDisplay = computed(() => {
  const id = props.sessionId || ''
  return id.length > 20 ? id.slice(0, 8) + '...' + id.slice(-4) : id
})

const formatTime = () => {
  const now = new Date()
  return now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const scrollToBottom = async () => {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

const closeCurrentStream = () => {
  if (source) {
    source.close()
    source = null
  }
}

const stopTypewriter = () => {
  if (typingTimer) {
    clearInterval(typingTimer)
    typingTimer = null
  }
  typingQueue = []
  activeTypingTask = null
}

const extractChunkText = (rawData) => {
  if (rawData == null) return ''
  const text = String(rawData).trim()
  if (!text) return ''

  if (text === '[DONE]' || text === 'DONE' || text === '[done]' || text === 'done') {
    return null
  }

  try {
    const parsed = JSON.parse(text)
    if (typeof parsed === 'string') return parsed
    if (parsed?.done === true || parsed?.finished === true) return null
    const delta = parsed?.delta ?? parsed?.content ?? parsed?.text ?? ''
    return typeof delta === 'string' ? delta : String(delta)
  } catch {
    return String(rawData)
  }
}

const finalizeStream = () => {
  // For step mode: mark thinking as complete and extract final result
  if (props.stepBubbleMode && stepThinkingIndex >= 0) {
    const thinkingMsg = messages.value[stepThinkingIndex]
    if (thinkingMsg && thinkingMsg.steps && thinkingMsg.steps.length > 0) {
      thinkingMsg.thinkingDone = true
      thinkingMsg.thinkingCollapsed = true

      // Find a meaningful final result (skip terminate/end markers)
      const skipPatterns = ['doTerminate', '任务结束', '任务完成', '执行结束', '达到最大步骤', '思考完成 - 无需行动']
      let finalContent = ''

      // Look from the end backward to find a step with real content
      for (let i = thinkingMsg.steps.length - 1; i >= 0; i--) {
        const step = (thinkingMsg.steps[i] || '').trim()
        const isSkippable = skipPatterns.some(p => step.includes(p))
        if (!isSkippable && step.length > 20) {
          // Found a meaningful step — extract the content after the colon if present
          const colonIdx = step.indexOf('的结果:')
          if (colonIdx > -1) {
            finalContent = step.slice(colonIdx + 4).trim().replace(/^["']|["']$/g, '')
          } else {
            finalContent = step
          }
          break
        }
      }

      if (finalContent) {
        messages.value.push({
          role: 'ai',
          content: finalContent,
          time: formatTime(),
          isFinalResult: true,
        })
      }
    }
  }

  isLoading.value = false
  stopTypewriter()
  streamEnded = false
  hasReceivedAiContent = false
  lastStepText = ''
  stepThinkingIndex = -1
  stepCount = 0
  closeCurrentStream()
  scrollToBottom()
}

const stopGenerating = () => {
  // 标记 thinking 消息为已停止
  if (props.stepBubbleMode && stepThinkingIndex >= 0) {
    const thinkingMsg = messages.value[stepThinkingIndex]
    if (thinkingMsg) {
      thinkingMsg.thinkingDone = true
      thinkingMsg.thinkingStopped = true
      thinkingMsg.thinkingCollapsed = true
    }
  } else {
    // 非 stepBubbleMode 时才显示停止提示
    messages.value.push({
      role: 'ai',
      content: '已停止生成回复。',
      time: formatTime(),
      isStopNotice: true,
    })
  }
  finalizeStream()
}

const flushTypewriter = () => {
  if (!activeTypingTask && typingQueue.length > 0) {
    activeTypingTask = typingQueue.shift()
  }

  if (!activeTypingTask) {
    if (streamEnded) finalizeStream()
    return
  }

  const step = Math.min(3, activeTypingTask.text.length)

  if (activeTypingTask.isStep) {
    // Append to the current step in thinking message
    const thinkingMsg = messages.value[activeTypingTask.aiIndex]
    if (thinkingMsg && thinkingMsg.steps) {
      const stepIdx = activeTypingTask.stepIdx
      thinkingMsg.steps[stepIdx] = (thinkingMsg.steps[stepIdx] || '') + activeTypingTask.text.slice(0, step)
    }
  } else {
    messages.value[activeTypingTask.aiIndex].content += activeTypingTask.text.slice(0, step)
  }
  activeTypingTask.text = activeTypingTask.text.slice(step)
  scrollToBottom()

  if (!activeTypingTask.text) activeTypingTask = null

  if (!activeTypingTask && typingQueue.length === 0 && streamEnded) {
    finalizeStream()
  }
}

const enqueueChunk = (chunk, aiIndex, isStep = false, stepIdx = 0) => {
  typingQueue.push({ aiIndex, text: chunk, isStep, stepIdx })
  hasReceivedAiContent = true
  if (!typingTimer) {
    typingTimer = setInterval(() => { flushTypewriter() }, 20)
  }
}

const markStreamEnded = () => {
  streamEnded = true
  flushTypewriter()
}

const copySessionId = () => {
  navigator.clipboard?.writeText(props.sessionId || '')
}

const toggleThinking = (idx) => {
  const msg = messages.value[idx]
  if (msg) {
    msg.thinkingCollapsed = !msg.thinkingCollapsed
  }
}

const sendMessage = async () => {
  const message = inputText.value.trim()
  if (!message) return

  closeCurrentStream()
  stopTypewriter()
  streamEnded = false
  hasReceivedAiContent = false
  lastStepText = ''
  stepThinkingIndex = -1
  stepCount = 0
  isLoading.value = true

  const now = formatTime()
  messages.value.push({
    role: 'user',
    content: message,
    time: now,
  })

  let aiIndex = -1
  if (!props.stepBubbleMode) {
    aiIndex = messages.value.push({
      role: 'ai',
      content: '',
      time: now,
    }) - 1
  } else {
    // Create a thinking container message
    stepThinkingIndex = messages.value.push({
      role: 'ai',
      content: '',
      time: now,
      isThinking: true,
      thinkingDone: false,
      thinkingCollapsed: false,
      steps: [],
    }) - 1
  }

  inputText.value = ''
  scrollToBottom()

  const params = { message }
  if (props.useChatId && props.chatId) {
    params.chatId = props.chatId
  }

  const sseUrl = getApiUrl(props.ssePath, params)
  source = new EventSource(sseUrl)

  const handleIncomingEvent = (event) => {
    const chunk = extractChunkText(event.data)
    if (chunk === null) {
      markStreamEnded()
      return
    }
    if (chunk) {
      if (props.stepBubbleMode) {
        const normalizedChunk = chunk.trim()
        if (normalizedChunk && normalizedChunk === lastStepText) return
        lastStepText = normalizedChunk

        // Add as a new step in the thinking container
        const thinkingMsg = messages.value[stepThinkingIndex]
        if (thinkingMsg) {
          const newStepIdx = thinkingMsg.steps.length
          thinkingMsg.steps.push('')
          stepCount++
          const stepChunk = chunk.endsWith('\n') ? chunk : chunk + '\n'
          enqueueChunk(stepChunk, stepThinkingIndex, true, newStepIdx)
        }
      } else {
        enqueueChunk(chunk, aiIndex)
      }
    }
  }

  source.addEventListener('message', handleIncomingEvent)
  source.addEventListener('token', handleIncomingEvent)
  source.addEventListener('delta', handleIncomingEvent)
  source.addEventListener('chunk', handleIncomingEvent)
  source.addEventListener('content', handleIncomingEvent)
  source.addEventListener('answer', handleIncomingEvent)

  source.addEventListener('done', () => { markStreamEnded() })
  source.addEventListener('complete', () => { markStreamEnded() })

  source.onerror = () => {
    const isConnectionClosed = source?.readyState === EventSource.CLOSED
    if (isConnectionClosed && hasReceivedAiContent) {
      markStreamEnded()
      return
    }
    if (!hasReceivedAiContent) {
      messages.value.push({
        role: 'ai',
        content: '连接中断，请稍后重试。',
        time: formatTime(),
      })
    }
    markStreamEnded()
  }
}

const handleKeyDown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

onBeforeUnmount(() => {
  closeCurrentStream()
  stopTypewriter()
})
</script>

<template>
  <main class="page chat-page">
    <!-- Floating Particles -->
    <div class="chat-particles">
      <div class="chat-particle" />
      <div class="chat-particle" />
      <div class="chat-particle" />
      <div class="chat-particle" />
      <div class="chat-particle" />
      <div class="chat-particle" />
    </div>

    <!-- Top Bar -->
    <header class="chat-topbar">
      <router-link to="/" class="back-btn">
        <span class="back-arrow">‹</span>
        <span>返回主页</span>
      </router-link>

      <div class="session-title-wrap">
        <span>会话名称：{{ sessionTitle }}</span>
        <span class="session-dropdown">▾</span>
      </div>

      <div class="session-id-wrap">
        <span>会话 ID：{{ sessionIdDisplay }}</span>
        <button class="copy-btn" @click="copySessionId" title="复制会话ID">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.2">
            <rect x="4" y="4" width="8" height="8" rx="1.5" />
            <path d="M10 4V3a1 1 0 00-1-1H3a1 1 0 00-1 1v6a1 1 0 001 1h1" />
          </svg>
        </button>
      </div>
    </header>

    <!-- Messages -->
    <section ref="listRef" class="chat-messages">
      <div v-if="messages.length === 0" class="empty-tip">
        发送第一条消息，开始本次智能对话
      </div>

      <div
        v-for="(item, idx) in messages"
        :key="idx"
        class="message-row"
        :class="item.role === 'user' ? 'is-user' : 'is-ai'"
      >
        <!-- AI Avatar -->
        <div v-if="item.role === 'ai'" class="avatar ai-avatar">
          <!-- Love Expert - Heart Icon -->
          <svg v-if="aiAvatar === 'NL'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" fill="rgba(255,100,150,0.2)" />
          </svg>
          <!-- Super Agent - Robot Icon -->
          <svg v-else-if="aiAvatar === 'NM'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <rect x="5" y="8" width="14" height="12" rx="2" fill="rgba(100,150,255,0.2)" />
            <path d="M12 2v4M8.5 8v-1a1 1 0 0 1 1-1h5a1 1 0 0 1 1 1v1" />
            <circle cx="9" cy="13" r="1" fill="currentColor" />
            <circle cx="15" cy="13" r="1" fill="currentColor" />
            <path d="M9 17h6" />
            <path d="M5 12h-2M21 12h-2" />
          </svg>
          <!-- Pet Expert - Cat Icon -->
          <svg v-else-if="aiAvatar === 'NP'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 5c-1.5 0-2.5 1-3 2l-2-3c-.5 1-1 3-1 5 0 3 2 5 4 6h4c2-1 4-3 4-6 0-2-.5-4-1-5l-2 3c-.5-1-1.5-2-3-2z" fill="rgba(255,180,100,0.2)" />
            <circle cx="9.5" cy="10" r="0.8" fill="currentColor" />
            <circle cx="14.5" cy="10" r="0.8" fill="currentColor" />
            <path d="M12 12v2M10 13.5l2 1 2-1" />
            <path d="M8 16c0 2 1.5 4 4 4s4-2 4-4" />
          </svg>
          <!-- Default - Smile Icon -->
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2z" />
            <path d="M8 14s1.5 2 4 2 4-2 4-2" />
            <circle cx="9" cy="9" r="1" fill="currentColor" />
            <circle cx="15" cy="9" r="1" fill="currentColor" />
          </svg>
        </div>

        <!-- User Avatar -->
        <div v-else class="avatar user-avatar">
          <img v-if="userAvatarUrl" :src="userAvatarUrl" alt="用户头像" class="user-avatar-img" />
          <svg v-else viewBox="0 0 24 24" fill="currentColor" opacity="0.6">
            <path d="M12 12c2.7 0 5-2.3 5-5s-2.3-5-5-5-5 2.3-5 5 2.3 5 5 5zm0 2c-3.3 0-10 1.7-10 5v2h20v-2c0-3.3-6.7-5-10-5z" />
          </svg>
        </div>

        <!-- Message Body -->
        <div class="message-body">
          <span class="sender-label">{{ item.role === 'ai' ? aiName : '我' }}</span>

          <!-- Thinking Steps Container (Manus) -->
          <div v-if="item.isThinking" class="bubble thinking-bubble">
            <div class="thinking-header" @click="toggleThinking(idx)">
              <svg class="thinking-icon" :class="{ spinning: !item.thinkingDone }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle v-if="!item.thinkingDone" cx="12" cy="12" r="10" stroke-dasharray="30 70" />
                <path v-else-if="item.thinkingStopped" d="M8 8l8 8M16 8l-8 8" />
                <path v-else d="M9 12l2 2 4-4" />
                <circle v-if="item.thinkingDone" cx="12" cy="12" r="10" />
              </svg>
              <span class="thinking-title">{{ item.thinkingDone ? (item.thinkingStopped ? '已停止思考' : '深度思考已完成') : '深度思考中...' }}</span>
              <span class="thinking-step-count" v-if="item.steps.length">{{ item.steps.length }} 步</span>
              <svg class="thinking-chevron" :class="{ collapsed: item.thinkingCollapsed }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </div>
            <div class="thinking-steps" v-show="!item.thinkingCollapsed">
              <div v-for="(step, sIdx) in item.steps" :key="sIdx" class="thinking-step-item">
                <span class="thinking-step-label">Step {{ sIdx + 1 }}</span>
                <span class="thinking-step-text">{{ step }}</span>
              </div>
            </div>
          </div>

          <!-- Final Result Bubble (Markdown) -->
          <div v-else-if="item.isFinalResult" class="bubble final-result-bubble markdown-body" v-html="renderMarkdown(item.content)"></div>

          <!-- Stop Notice Bubble -->
          <div v-else-if="item.isStopNotice" class="bubble stop-notice-bubble">{{ item.content }}</div>

          <!-- Normal Bubble -->
          <div v-else-if="item.role === 'ai'" class="bubble markdown-body" v-html="renderMarkdown(item.content) || '思考中...'"></div>
          <div v-else class="bubble">{{ item.content }}</div>

          <div class="message-meta">
            <span>{{ item.time }}</span>
            <span v-if="item.role === 'user'" class="read-status">✓✓</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Input Bar -->
    <footer class="chat-input-bar">
      <div class="input-wrapper">
        <button class="attach-btn" aria-label="附件">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
            <path d="M13.5 6.5L6 14a2.5 2.5 0 11-3.5-3.5L10 3a4 4 0 015.6 5.6L8 16a6 6 0 11-8.4-8.4L7 0" />
          </svg>
        </button>
        <input
          v-model="inputText"
          class="chat-input"
          type="text"
          placeholder="输入消息..."
          @keydown="handleKeyDown"
        />
        <button
          v-if="isLoading"
          class="stop-btn"
          type="button"
          @click="stopGenerating"
          title="停止生成"
        >
          <svg width="14" height="14" viewBox="0 0 14 14" fill="currentColor">
            <rect x="1" y="1" width="12" height="12" rx="2" />
          </svg>
        </button>
        <button
          v-else
          class="send-btn"
          :disabled="!canSend"
          @click="sendMessage"
          title="发送"
        >
          <svg viewBox="0 0 18 18" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M16 2L8 10" />
            <path d="M16 2L11 16L8 10L2 7L16 2Z" />
          </svg>
        </button>
      </div>
      <p class="input-helper">Enter 发送，Shift + Enter 换行</p>
    </footer>
  </main>
</template>
