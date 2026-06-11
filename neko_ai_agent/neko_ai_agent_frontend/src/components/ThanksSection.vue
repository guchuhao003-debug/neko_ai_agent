<script setup>
// 致谢模块数据，供首页与 AI 智能体页共同复用。
const thanksList = [
  {
    name: 'Spring AI',
    desc: '感谢 Spring AI 团队提供的优秀框架，为 AI 应用开发提供了坚实的基础',
    img: 'https://spring.io/img/projects/spring-ai.svg',
    alt: 'Spring AI',
    link: 'https://github.com/spring-projects/spring-ai',
    action: '访问 GitHub',
  },
  {
    name: '阿里云百炼大模型服务平台',
    desc: '感谢阿里云百炼平台提供强大的大模型能力和完善的服务支持',
    img: '/aliyun.png',
    alt: '阿里云百炼',
    link: 'https://bailian.console.aliyun.com/',
    action: '了解更多',
  },
]
</script>

<template>
  <section class="thanks-section">
    <h2 class="thanks-title">致谢</h2>
    <div class="thanks-divider"></div>
    <p class="thanks-desc">Neko AI Agent 离不开开源社区和服务平台的支持</p>

    <div class="thanks-cards">
      <div v-for="item in thanksList" :key="item.name" class="thanks-card">
        <div class="thanks-card-icon">
          <img :src="item.img" :alt="item.alt" />
        </div>
        <h3 class="thanks-card-name">{{ item.name }}</h3>
        <p class="thanks-card-desc">{{ item.desc }}</p>
        <a
          :href="item.link"
          target="_blank"
          rel="noopener noreferrer"
          class="thanks-card-btn"
        >
          <span>{{ item.action }}</span>
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            width="14"
            height="14"
          >
            <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8" />
            <path d="M3 8a2 2 0 0 1 2-2h6" />
            <polyline points="15 3 21 3 21 9" />
            <line x1="10" y1="14" x2="21" y2="3" />
          </svg>
        </a>
      </div>
    </div>
  </section>
</template>
