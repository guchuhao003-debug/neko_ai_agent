import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../views/HomePage.vue'
import LoveChatPage from '../views/LoveChatPage.vue'
import ManusChatPage from '../views/ManusChatPage.vue'
import PetChatPage from '../views/PetChatPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
      meta: {
        title: 'Neko AI Agent - 科技感 AI 智能体平台',
        description:
          'Neko AI Agent 提供恋爱专家智能体与超级智能体，支持流式实时对话与沉浸式科技风交互体验。',
      },
    },
    {
      path: '/love',
      name: 'love',
      component: LoveChatPage,
      meta: {
        title: 'AI 恋爱大师 - Neko AI Agent',
        description:
          'AI 恋爱大师支持基于 chatId 的多会话流式聊天，为你提供恋爱咨询和沟通建议。',
      },
    },
    {
      path: '/manus',
      name: 'manus',
      component: ManusChatPage,
      meta: {
        title: 'AI 超级智能体 - Neko AI Agent',
        description:
          'NekoMenus 超级智能体支持分步骤 SSE 实时输出，适用于复杂任务拆解与执行。',
      },
    },
    {
      path: '/pet',
      name: 'pet',
      component: PetChatPage,
      meta: {
        title: 'AI 宠物专家 - Neko AI Agent',
        description:
          'AI 宠物专家提供专业的宠物养护建议，支持基于 chatId 的多会话流式聊天。',
      },
    },
  ],
})

const ensureMeta = (name) => {
  let el = document.querySelector(`meta[name="${name}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute('name', name)
    document.head.appendChild(el)
  }
  return el
}

router.afterEach((to) => {
  document.title = to.meta.title || 'Neko AI Agent'
  const description = ensureMeta('description')
  description.setAttribute(
    'content',
    to.meta.description || 'Neko AI Agent - 智能体实时对话平台'
  )
})

export default router
