import { createRouter, createWebHistory } from 'vue-router'
import LandingPage from '../views/LandingPage.vue'
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
import QuotaPage from '../views/QuotaPage.vue'
import QuotaCodeManagePage from '../views/QuotaCodeManagePage.vue'
import { useUser } from '../composables/useUser'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: LandingPage,
      meta: {
        title: 'Neko AI Agent',
        description:
          'Neko AI Agent 1.0.0 是多智能体 AI 平台，支持专家咨询、自定义智能体、NekoMenus、历史会话和积分配额。',
      },
    },
    {
      path: '/ai-agents',
      name: 'aiAgents',
      component: HomePage,
      meta: {
        requiresAuth: true,
        title: 'AI 智能体 - Neko AI Agent',
        description:
          'Neko AI Agent AI 智能体入口，提供心屿树洞、宠爱智问、NekoMenus 和自定义智能体工作台。',
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
        requiresAuth: true,
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
        requiresAuth: true,
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
        requiresAuth: true,
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
        requiresAuth: true,
        title: '自定义智能体 - Neko AI Agent',
        description: '创建、管理并使用自定义 AI 智能体。',
      },
    },
    {
      path: '/agent/:id/chat',
      name: 'agentChat',
      component: AgentChatPage,
      meta: {
        requiresAuth: true,
        title: '自定义智能体对话 - Neko AI Agent',
        description: '和自定义 AI 智能体进行实时流式对话。',
      },
    },
    {
      path: '/quota',
      name: 'quota',
      component: QuotaPage,
      meta: {
        requiresAuth: true,
        title: '积分配额 - Neko AI Agent',
        description: '查看当前积分配额并兑换积分兑换码。',
      },
    },
    {
      path: '/admin/users',
      name: 'userManage',
      component: UserManagePage,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
        title: '用户管理 - Neko AI Agent',
        description: '管理员用户管理页面，支持增删改查。',
      },
    },
    {
      path: '/admin/agents',
      name: 'agentManage',
      component: AgentManagePage,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
        title: '智能体管理 - Neko AI Agent',
        description: '管理员智能体管理页面，管理所有自定义智能体。',
      },
    },
    {
      path: '/admin/quota-codes',
      name: 'quotaCodeManage',
      component: QuotaCodeManagePage,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
        title: '积分兑换码管理 - Neko AI Agent',
        description: '管理员批量生成、查询和删除积分兑换码。',
      },
    },
    {
      path: '/docs',
      name: 'docs',
      component: DocsPage,
      meta: {
        title: '操作指南 - Neko AI Agent',
        description: 'Neko AI Agent 平台操作文档，包含账号、积分、智能体和后台管理指南。',
      },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      redirect: '/',
      meta: {
        title: 'Neko AI Agent',
        description: 'Neko AI Agent - 多智能体 AI 平台',
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

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth && !to.meta.requiresAdmin) {
    return true
  }

  const { currentUser, fetchUser } = useUser()
  let user = currentUser.value
  if (!user) {
    try {
      user = await fetchUser()
    } catch (error) {
      console.error('导航鉴权失败:', error)
      return false
    }
  }

  if (!user) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }
  if (to.meta.requiresAdmin && user.userRole !== 'admin') {
    return { name: 'home' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title || 'Neko AI Agent'
  const description = ensureMeta('description')
  description.setAttribute(
    'content',
    to.meta.description || 'Neko AI Agent - 多智能体 AI 平台'
  )
})

export default router
