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

    <!-- Content -->
    <div class="home-content">
      <h1 class="home-title">Neko AI Agent</h1>
      <p class="home-subtitle">超 越 想 象 ， 智 能 无 界</p>
      <div class="subtitle-line" />

      <div class="card-grid">
        <router-link
          v-for="app in appList"
          :key="app.path"
          :to="app.path"
          class="app-card"
        >
          <div class="card-glass">
            <div class="card-icon-wrap" aria-hidden="true">
              <!-- Heart with cat silhouette -->
              <svg v-if="app.icon === 'love'" viewBox="0 0 64 64" class="card-icon-svg">
                <path
                  d="M32 56 C30 54 10 42 10 28 C10 18 16 12 24 12 C29 12 32 15 32 18 C32 15 35 12 40 12 C48 12 54 18 54 28 C54 42 34 54 32 56Z"
                  fill="none"
                  stroke="white"
                  stroke-width="2"
                />
                <path
                  d="M28 22 L26 14 L30 20 L32 18 L34 20 L38 14 L36 22 C38 26 38 32 32 34 C26 32 26 26 28 22Z"
                  fill="rgba(255,255,255,0.6)"
                />
              </svg>
              <!-- Pet paw print -->
              <svg v-else-if="app.icon === 'pet'" viewBox="0 0 64 64" class="card-icon-svg">
                <ellipse cx="32" cy="42" rx="10" ry="12" fill="rgba(255,255,255,0.7)" />
                <circle cx="22" cy="28" r="5" fill="rgba(255,255,255,0.6)" />
                <circle cx="32" cy="24" r="5" fill="rgba(255,255,255,0.6)" />
                <circle cx="42" cy="28" r="5" fill="rgba(255,255,255,0.6)" />
                <circle cx="26" cy="18" r="4" fill="rgba(255,255,255,0.5)" />
              </svg>
              <!-- N with planetary ring -->
              <svg v-else viewBox="0 0 64 64" class="card-icon-svg">
                <ellipse
                  cx="32" cy="32" rx="22" ry="8"
                  fill="none" stroke="rgba(255,255,255,0.4)" stroke-width="1.5"
                  transform="rotate(-20, 32, 32)"
                />
                <text
                  x="32" y="40"
                  text-anchor="middle"
                  font-family="Orbitron, sans-serif"
                  font-weight="900"
                  font-size="30"
                  fill="white"
                >N</text>
              </svg>
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
    </div>
  </main>
</template>
