import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../views/HomePage.vue'
import LoveChatPage from '../views/LoveChatPage.vue'
import ManusChatPage from '../views/ManusChatPage.vue'
import PetChatPage from '../views/PetChatPage.vue'
import LoginPage from '../views/LoginPage.vue'
import RegisterPage from '../views/RegisterPage.vue'
import UserManagePage from '../views/UserManagePage.vue'
import DocsPage from '../views/DocsPage.vue'
import AgentStudioPage from '../views/AgentStudioPage.vue'
import AgentChatPage from '../views/AgentChatPage.vue'
import AgentManagePage from '../views/AgentManagePage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
      meta: {
        title: 'Neko AI Agent',
        description:
          'Neko AI Agent 提供恋爱专家智能体与超级智能体，支持流式实时对话与沉浸式科技风交互体验。',
      },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginPage,
      meta: {
        title: '登录 - Neko AI Agent',
        description: '登录 Neko AI Agent 智能体平台。',
      },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterPage,
      meta: {
        title: '注册 - Neko AI Agent',
        description: '注册 Neko AI Agent 智能体平台账号。',
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
    {
      path: '/agents',
      name: 'agents',
      component: AgentStudioPage,
      meta: {
        title: '自定义智能体 - Neko AI Agent',
        description: '创建、管理并使用自定义 AI 智能体。',
      },
    },
    {
      path: '/agent/:id/chat',
      name: 'agentChat',
      component: AgentChatPage,
      meta: {
        title: '自定义智能体对话 - Neko AI Agent',
        description: '和自定义 AI 智能体进行实时流式对话。',
      },
    },
    {
      path: '/admin/users',
      name: 'userManage',
      component: UserManagePage,
      meta: {
        title: '用户管理 - Neko AI Agent',
        description: '管理员用户管理页面，支持增删改查。',
      },
    },
    {
      path: '/admin/agents',
      name: 'agentManage',
      component: AgentManagePage,
      meta: {
        title: '智能体管理 - Neko AI Agent',
        description: '管理员智能体管理页面，管理所有自定义智能体。',
      },
    },
    {
      path: '/docs',
      name: 'docs',
      component: DocsPage,
      meta: {
        title: '快速开始 - Neko AI Agent',
        description: 'Neko AI Agent 平台使用文档，包含项目简介、技术栈和各智能体使用指南。',
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
