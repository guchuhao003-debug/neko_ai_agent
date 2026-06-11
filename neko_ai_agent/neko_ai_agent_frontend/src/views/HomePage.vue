<script setup>
import ThanksSection from '../components/ThanksSection.vue'

const appList = [
  {
    title: '心屿树洞',
    desc: '梳理关系问题，准备沟通表达',
    subDesc: '面向恋爱与亲密关系的咨询助手',
    path: '/love',
    icon: 'love',
  },
  {
    title: 'NekoMenus 超级智能体',
    desc: '拆解复杂任务，调用工具执行',
    subDesc: '支持搜索、文件、PDF 与邮件处理',
    path: '/manus',
    icon: 'manus',
  },
  {
    title: '宠爱智问',
    desc: '覆盖喂养、护理、训练与健康观察',
    subDesc: '面向日常养宠场景的知识助手',
    path: '/pet',
    icon: 'pet',
  },
  {
    title: '自定义智能体工作台',
    desc: '创建角色，配置模型，沉淀专属能力',
    subDesc: '面向每个任务的私人 Agent',
    path: '/agents',
    icon: 'custom',
  },
]
</script>

<template>
  <main class="page home-page">
    <!-- Animated Grid Overlay -->
    <div class="grid-overlay" />

    <!-- Floating Particles -->
    <div class="particles">
      <div class="particle" />
      <div class="particle" />
      <div class="particle" />
      <div class="particle" />
      <div class="particle" />
      <div class="particle" />
      <div class="particle" />
      <div class="particle" />
      <div class="particle" />
      <div class="particle" />
      <div class="particle" />
      <div class="particle" />
    </div>

    <!-- Sparkle Stars -->
    <div class="sparkle-container">
      <div class="sparkle-star" v-for="n in 40" :key="'star-' + n" />
    </div>

    <!-- Content -->
    <div class="home-content">
      <h1 class="home-title">Neko AI Agent</h1>
      <p class="home-subtitle">多 智 能 体 AI 平 台 · 1.0.0 正 式 版</p>
      <div class="subtitle-line" />

      <!-- Tag Badges -->
      <div class="home-tags">
        <span class="home-tag">内置专家智能体</span>
        <span class="home-tag">自定义 Agent</span>
        <span class="home-tag">SSE 流式对话</span>
        <span class="home-tag">积分配额管理</span>
      </div>

      <div class="card-grid">
        <router-link
          v-for="app in appList.filter(item => item.path !== '/agents')"
          :key="app.path"
          :to="app.path"
          class="app-card"
        >
          <div class="card-glass">
            <div class="card-icon-wrap" aria-hidden="true">
              <!-- Love Expert Logo -->
              <img v-if="app.icon === 'love'" src="/loveapp.png" alt="AI 恋爱大师" class="card-icon-img card-icon-love" />
              <!-- Pet Expert Logo -->
              <img v-else-if="app.icon === 'pet'" src="/petapp.png" alt="AI 宠物专家" class="card-icon-img card-icon-pet" />
              <!-- Custom Agent Logo -->
              <img v-else-if="app.icon === 'custom'" src="/neko.png" alt="自定义智能体" class="card-icon-img" />
              <!-- Neko Manus Logo -->
              <img v-else src="/neko.png" alt="NekoMenus" class="card-icon-img" />
            </div>

            <h2 class="card-title">{{ app.title }}</h2>
            <p class="card-desc">{{ app.desc }}</p>
            <p class="card-sub-desc">{{ app.subDesc }}</p>

            <span class="card-btn">
              进入智能体
              <svg class="card-btn-arrow" width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
                <path d="M1 7h12M8 2l5 5-5 5" />
              </svg>
            </span>
          </div>
        </router-link>
      </div>

      <ThanksSection />
    </div>
  </main>
</template>
