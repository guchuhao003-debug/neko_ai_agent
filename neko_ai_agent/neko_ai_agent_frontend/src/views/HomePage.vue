<script setup>
const appList = [
  {
    title: '恋爱专家智能体',
    desc: '懂你所想，解你所惑',
    subDesc: '专属你的恋爱咨询专家',
    path: '/love',
    icon: 'love',
  },
  {
    title: 'NekoMenus 超级智能体',
    desc: '多模态理解，深度规划执行',
    subDesc: '你的全能超级智能体',
    path: '/manus',
    icon: 'manus',
  },
  {
    title: '宠物专家智能体',
    desc: '科学养护，陪伴成长',
    subDesc: '专业的宠物健康顾问',
    path: '/pet',
    icon: 'pet',
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
      <p class="home-subtitle">超 越 想 象 ， 智 能 无 界</p>
      <div class="subtitle-line" />

      <!-- Tag Badges -->
      <div class="home-tags">
        <span class="home-tag">Custom-agent</span>
        <span class="home-tag">Multi-agent</span>
        <span class="home-tag">Manus-agent</span>
      </div>

      <div class="card-grid">
        <router-link
          v-for="app in appList"
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

      <!-- 致谢 -->
      <section class="thanks-section">
        <h2 class="thanks-title">致谢</h2>
        <div class="thanks-divider"></div>
        <p class="thanks-desc">Neko AI Agent 离不开开源社区和服务平台的支持</p>

        <div class="thanks-cards">
          <div class="thanks-card">
            <div class="thanks-card-icon">
              <img src="https://spring.io/img/projects/spring-ai.svg" alt="Spring AI" />
            </div>
            <h3 class="thanks-card-name">Spring AI</h3>
            <p class="thanks-card-desc">感谢 Spring AI 团队提供的优秀框架，为 AI 应用开发提供了坚实的基础</p>
            <a href="https://github.com/spring-projects/spring-ai" target="_blank" rel="noopener noreferrer" class="thanks-card-btn">
              <span>访问 GitHub</span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                <polyline points="15 3 21 3 21 9" />
                <line x1="10" y1="14" x2="21" y2="3" />
              </svg>
            </a>
          </div>

          <div class="thanks-card">
            <div class="thanks-card-icon">
              <img src="/aliyun.png" alt="阿里云百炼" />
            </div>
            <h3 class="thanks-card-name">阿里云百炼大模型服务平台</h3>
            <p class="thanks-card-desc">感谢阿里云百炼平台提供强大的大模型能力和完善的服务支持</p>
            <a href="https://bailian.console.aliyun.com/" target="_blank" rel="noopener noreferrer" class="thanks-card-btn">
              <span>了解更多</span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                <polyline points="15 3 21 3 21 9" />
                <line x1="10" y1="14" x2="21" y2="3" />
              </svg>
            </a>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>
