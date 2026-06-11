<script setup>
import ThanksSection from '../components/ThanksSection.vue'
import { useRouter } from 'vue-router'
import { useUser } from '../composables/useUser'

const router = useRouter()
const { currentUser, fetchUser } = useUser()

const workflowSteps = [
  {
    icon: 'check',
    title: '目标识别',
    desc: '理解用户意图，提取任务目标、限制条件与期望交付物',
    active: false,
  },
  {
    icon: 'loader',
    title: '自主规划',
    desc: '拆解复杂任务，规划搜索、文件处理、PDF 生成等执行步骤',
    active: true,
    status: 'Planning',
  },
  {
    icon: 'file',
    title: '工具执行',
    desc: '按计划调用工具完成检索、读取、生成、下载和邮件发送',
    active: false,
  },
  {
    icon: 'grid',
    title: '结果校验',
    desc: '整合执行结果，检查遗漏风险，并输出清晰 Markdown 答案',
    active: false,
  },
]

/**
 * 登录后跳转到 AI 智能体入口页，未登录时跳转登录页。
 */
const goToAgents = async () => {
  let user = currentUser.value

  if (!user) {
    try {
      user = await fetchUser()
    } catch (error) {
      console.error('首页入口登录校验失败:', error)
    }
  }

  if (user) {
    router.push('/ai-agents')
    return
  }

  router.push({
    name: 'login',
    query: { redirect: '/ai-agents' },
  })
}

/**
 * 跳转到平台操作指南。
 */
const goToDemo = () => {
  router.push('/docs')
}
</script>

<template>
  <main class="landing-page">
    <section class="landing-hero">
      <div class="landing-stars" aria-hidden="true">
        <span v-for="index in 20" :key="index" />
      </div>

      <div class="landing-copy">
        <div class="landing-badge">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 2l1.8 6.2L20 10l-6.2 1.8L12 18l-1.8-6.2L4 10l6.2-1.8L12 2z" />
          </svg>
          <span>NekoMenus 自主规划核心</span>
        </div>

        <div class="landing-headline">
          <h1 class="landing-title">Neko AI Agent</h1>
          <p class="landing-subtitle">自 主 规 划 · 智 能 对 话</p>
          <div class="landing-subtitle-line" />
        </div>

        <p class="landing-description">
          以 AI 自主规划的超级智能体为核心，智能理解你的需求并自主规划处理流程。
          同时提供丰富的 AI 智能体创造功能，从 API 接入、Prompt 工程到 AI Agent
          智能体对话，满足您的各种对话以及创造需求。
        </p>

        <div class="landing-actions">
          <button class="landing-primary-btn" @click="goToAgents">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M5 12h14M13 6l6 6-6 6" />
            </svg>
            <span>立即体验</span>
          </button>
          <button class="landing-secondary-btn" @click="goToDemo">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="12" cy="12" r="9" />
              <path d="M10 8l6 4-6 4V8z" />
            </svg>
            <span>观看演示</span>
          </button>
        </div>
      </div>

      <aside class="landing-core-panel" aria-label="NekoMenus 自主规划流程">
        <div class="landing-window-top">
          <div class="landing-window-dots" aria-hidden="true">
            <span class="red" />
            <span class="yellow" />
            <span class="green" />
          </div>
          <span class="landing-window-name">NEKOMENUS-PLANNER-CORE</span>
        </div>

        <div class="landing-user-card">
          <div class="landing-user-avatar">U</div>
          <div>
            <h2>用户目标</h2>
            <p>“帮我搜索 Spring AI 资料，整理核心能力，并生成一份 PDF 摘要。”</p>
          </div>
        </div>

        <div class="landing-flow-list">
          <div
            v-for="step in workflowSteps"
            :key="step.title"
            class="landing-flow-item"
            :class="{ active: step.active }"
          >
            <div class="landing-flow-icon" :class="step.icon">
              <svg v-if="step.icon === 'check'" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M20 6L9 17l-5-5" />
              </svg>
              <svg v-else-if="step.icon === 'loader'" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M21 12a9 9 0 1 1-6.2-8.56" />
              </svg>
              <svg v-else-if="step.icon === 'file'" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M7 3h7l5 5v13H7V3z" />
                <path d="M14 3v5h5M9 14h6M9 17h4" />
              </svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 4h6v6H4V4zM14 4h6v6h-6V4zM4 14h6v6H4v-6zM14 14h6v6h-6v-6z" />
              </svg>
            </div>
            <div class="landing-flow-copy">
              <div class="landing-flow-title">
                <h3>{{ step.title }}</h3>
                <span v-if="step.status">{{ step.status }}</span>
              </div>
              <p>{{ step.desc }}</p>
            </div>
          </div>
        </div>
      </aside>
    </section>

    <ThanksSection />
  </main>
</template>
