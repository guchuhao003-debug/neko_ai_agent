<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const year = new Date().getFullYear()
const isChatPage = computed(() => route.name === 'love' || route.name === 'manus')

const showContactModal = ref(false)

const openContactModal = () => {
  showContactModal.value = true
}

const closeContactModal = () => {
  showContactModal.value = false
}
</script>

<template>
  <div class="app-shell">
    <!-- Global Particle Canvas -->
    <div class="particle-canvas">
      <div v-for="i in 60" :key="i" class="sparkle-particle" />
    </div>

    <router-view />
    <footer v-if="!isChatPage" class="site-footer">
      <div class="site-footer-inner">
        <div class="footer-content">
          <div class="footer-left">
            <p class="footer-copyright">© {{ year }} Neko AI Agent. All Rights Reserved.</p>
            <p class="footer-slogan">Powered by Neko AI · 智能驱动未来</p>
          </div>

          <div class="footer-links">
            <button @click="openContactModal" class="footer-link footer-link-btn">
              <svg class="footer-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
              <span>联系我们</span>
            </button>

            <a href="https://github.com/guchuhao003-debug/neko_ai_agent.git" class="footer-link" target="_blank" rel="noopener noreferrer">
              <svg class="footer-icon" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
              </svg>
              <span>GitHub</span>
            </a>
          </div>
        </div>
      </div>
    </footer>

    <!-- Contact Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showContactModal" class="modal-overlay" @click="closeContactModal">
          <div class="modal-container" @click.stop>
            <button class="modal-close" @click="closeContactModal">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>

            <div class="modal-header">
              <h2 class="modal-title">Neko AI Agent</h2>
              <p class="modal-subtitle">联系我们</p>
            </div>

            <div class="modal-body">
              <div class="contact-item">
                <div class="contact-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                    <polyline points="22,6 12,13 2,6"/>
                  </svg>
                </div>
                <div class="contact-info">
                  <div class="contact-label">联系 QQ 邮箱</div>
                  <a href="mailto:935070021@qq.com" class="contact-value">935070021@qq.com</a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
