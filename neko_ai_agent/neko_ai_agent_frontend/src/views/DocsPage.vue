<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import 'highlight.js/styles/github-dark.css'

hljs.registerLanguage('bash', bash)

const router = useRouter()
const activeSection = ref('intro')

const sections = [
  { id: 'intro', label: '平台概览' },
  { id: 'account', label: '账号与登录' },
  { id: 'quota', label: '积分配额' },
  { id: 'builtin-agents', label: '内置智能体' },
  { id: 'agent-love', label: '心屿树洞', indent: true },
  { id: 'agent-pet', label: '宠爱智问', indent: true },
  { id: 'agent-manus', label: 'NekoMenus', indent: true },
  { id: 'custom-agent', label: '自定义智能体' },
  { id: 'chat-history', label: '对话与文件' },
  { id: 'admin', label: '后台管理' },
  { id: 'notes', label: '使用注意' },
]

const scrollToSection = (id) => {
  activeSection.value = id
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

// 监听页面滚动，保持左侧目录高亮。
let observer = null

onMounted(() => {
  const options = {
    root: null,
    rootMargin: '-80px 0px -60% 0px',
    threshold: 0,
  }
  observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        activeSection.value = entry.target.id
      }
    })
  }, options)

  sections.forEach((s) => {
    const el = document.getElementById(s.id)
    if (el) observer.observe(el)
  })

  // 高亮文档中的命令示例。
  nextTick(() => {
    document.querySelectorAll('.docs-code-block pre code').forEach((block) => {
      hljs.highlightElement(block)
    })
  })
})

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
})
</script>

<template>
  <main class="page docs-page">
    <!-- 左侧目录 -->
    <aside class="docs-sidebar">
      <div class="docs-sidebar-header">
        <button class="docs-back-btn" @click="router.push('/')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          <span>返回主页</span>
        </button>
      </div>
      <nav class="docs-nav">
        <h3 class="docs-nav-title">操作目录</h3>
        <button
          v-for="s in sections"
          :key="s.id"
          class="docs-nav-item"
          :class="{ active: activeSection === s.id, indent: s.indent }"
          @click="scrollToSection(s.id)"
        >
          {{ s.label }}
        </button>
      </nav>
    </aside>

    <!-- 主内容 -->
    <div class="docs-main">
      <section id="intro" class="docs-section">
        <h1 class="docs-page-title">Neko AI Agent 操作指南</h1>
        <p class="docs-page-subtitle">
          面向平台用户的功能使用指南，覆盖账号、积分、智能体、历史对话和后台管理。
        </p>

        <h2 class="docs-section-title">平台概览</h2>
        <div class="docs-divider"></div>
        <p class="docs-text">
          <strong>Neko AI Agent</strong> 是一个多智能体 AI 对话平台。你可以使用
          内置专家智能体完成咨询类对话，也可以使用 NekoMenus 处理更复杂的任务，
          还可以在工作台中创建自己的专属智能体。
        </p>
        <div class="docs-feature-grid">
          <div class="docs-feature-card">
            <div class="docs-feature-icon">AI</div>
            <h4>多智能体入口</h4>
            <p>恋爱、宠物、超级智能体和自定义智能体统一接入。</p>
          </div>
          <div class="docs-feature-card">
            <div class="docs-feature-icon">SSE</div>
            <h4>实时流式回复</h4>
            <p>回答会边生成边展示，并支持手动停止。</p>
          </div>
          <div class="docs-feature-card">
            <div class="docs-feature-icon">ID</div>
            <h4>历史会话</h4>
            <p>按会话 ID 保存对话，可继续查看、切换和删除历史记录。</p>
          </div>
          <div class="docs-feature-card">
            <div class="docs-feature-icon">10</div>
            <h4>积分控制</h4>
            <p>每次智能体对话扣减积分，便于控制模型使用成本。</p>
          </div>
        </div>
      </section>

      <section id="account" class="docs-section">
        <h2 class="docs-section-title">账号与登录</h2>
        <div class="docs-divider"></div>
        <p class="docs-text">
          平台的聊天、积分、自定义智能体和后台管理功能都需要登录。未登录用户访问受保护
          页面时，会被引导到登录页。
        </p>
        <div class="docs-table-wrap">
          <table class="docs-table">
            <thead>
              <tr>
                <th>操作</th>
                <th>入口</th>
                <th>说明</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>注册账号</td>
                <td>/register</td>
                <td>填写用户名、邮箱、账号和密码，创建普通用户账号。</td>
              </tr>
              <tr>
                <td>账号密码登录</td>
                <td>/login</td>
                <td>使用账号和密码登录平台。</td>
              </tr>
              <tr>
                <td>邮箱验证码登录</td>
                <td>/login</td>
                <td>发送邮箱验证码后登录，验证码错误或过期会提示失败。</td>
              </tr>
              <tr>
                <td>退出登录</td>
                <td>导航栏用户区域</td>
                <td>退出后将无法继续访问聊天、积分和后台页面。</td>
              </tr>
            </tbody>
          </table>
        </div>
        <ul class="docs-list">
          <li><strong>普通用户</strong> 可以使用智能体、创建自定义智能体和兑换积分。</li>
          <li><strong>管理员</strong> 额外拥有用户管理、智能体管理和兑换码管理权限。</li>
          <li><strong>资料安全</strong> 用户详情返回脱敏信息，Session 不保存密码哈希。</li>
        </ul>
      </section>

      <section id="quota" class="docs-section">
        <h2 class="docs-section-title">积分配额</h2>
        <div class="docs-divider"></div>
        <p class="docs-text">
          平台通过积分控制 AI 调用次数。进入 <code class="docs-inline-code">/quota</code>
          可查看每日积分、额外积分、总积分和今日消耗情况。
        </p>
        <div class="docs-table-wrap">
          <table class="docs-table">
            <thead>
              <tr>
                <th>项目</th>
                <th>规则</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>每日免费积分</td>
                <td>每天 00:00 自动刷新为 100。</td>
              </tr>
              <tr>
                <td>对话扣减</td>
                <td>每次智能体对话扣减 10 积分。</td>
              </tr>
              <tr>
                <td>额外积分</td>
                <td>通过管理员发放的兑换码获得，不随每日刷新清零。</td>
              </tr>
              <tr>
                <td>兑换码有效期</td>
                <td>管理员生成后 24 小时内有效，使用后立即失效。</td>
              </tr>
            </tbody>
          </table>
        </div>
        <h3 class="docs-subtitle">兑换步骤</h3>
        <ul class="docs-list">
          <li>进入“我的积分配额”页面。</li>
          <li>在兑换输入框中填写兑换码。</li>
          <li>点击“立即兑换”，成功后额外积分会立刻到账。</li>
          <li>若提示积分不足，需要等待每日刷新或兑换额外积分后再发起对话。</li>
        </ul>
      </section>

      <section id="builtin-agents" class="docs-section">
        <h2 class="docs-section-title">内置智能体</h2>
        <div class="docs-divider"></div>
        <p class="docs-text">
          首页提供三个内置智能体入口。进入任一聊天页后，在底部输入问题，按
          Enter 或点击发送按钮即可开始流式对话。Shift + Enter 可在输入框换行。
        </p>
      </section>

      <section id="agent-love" class="docs-section docs-section-sub">
        <h3 class="docs-subtitle docs-agent-title">
          <span class="docs-agent-badge love">L</span>
          心屿树洞
        </h3>
        <p class="docs-text">
          心屿树洞面向恋爱、沟通、关系修复和情感困惑场景。它适合用来梳理关系问题、
          准备沟通话术、分析约会体验或获得更温和的情绪支持。
        </p>
        <h4 class="docs-small-title">推荐问法</h4>
        <div class="docs-prompt-box">
          <p>我和伴侣最近经常因为回复消息吵架，应该怎样沟通更合适？</p>
        </div>
        <div class="docs-prompt-box">
          <p>第一次约会想准备几个自然的话题，你能帮我列一个轻松的聊天路线吗？</p>
        </div>
        <ul class="docs-list">
          <li>尽量描述关系背景、冲突原因和你希望达成的目标。</li>
          <li>涉及现实安全、法律、医疗等问题时，请优先寻求专业人士帮助。</li>
        </ul>
      </section>

      <section id="agent-pet" class="docs-section docs-section-sub">
        <h3 class="docs-subtitle docs-agent-title">
          <span class="docs-agent-badge pet">P</span>
          宠爱智问
        </h3>
        <p class="docs-text">
          宠爱智问面向宠物养护咨询，覆盖喂养、训练、日常护理和常见健康问题。
          它可以给出基础判断和护理建议，但不能替代线下兽医诊断。
        </p>
        <h4 class="docs-small-title">推荐问法</h4>
        <div class="docs-prompt-box">
          <p>3 个月大的小猫一天应该喂几次？干粮和湿粮怎么搭配更合适？</p>
        </div>
        <div class="docs-prompt-box">
          <p>我家金毛最近食欲下降、精神不好，需要观察哪些危险信号？</p>
        </div>
        <ul class="docs-list">
          <li>请说明宠物品种、年龄、体重、症状持续时间和已采取的处理方式。</li>
          <li>若出现持续呕吐、呼吸困难、抽搐、便血等症状，应立即就医。</li>
        </ul>
      </section>

      <section id="agent-manus" class="docs-section docs-section-sub">
        <h3 class="docs-subtitle docs-agent-title">
          <span class="docs-agent-badge manus">N</span>
          NekoMenus 超级智能体
        </h3>
        <p class="docs-text">
          NekoMenus 适合处理需要拆解步骤、调用工具或生成文件的复杂任务。它会在
          “深度思考”区域展示执行过程，并在最终答复中输出 Markdown 结果。
        </p>
        <h4 class="docs-small-title">适合任务</h4>
        <ul class="docs-list">
          <li>搜索资料并整理摘要。</li>
          <li>读取或写入文件，生成可访问的结果文件。</li>
          <li>下载资源、生成 PDF、执行安全范围内的终端命令。</li>
          <li>在任务完成后通过最终答案总结执行结果。</li>
        </ul>
        <h4 class="docs-small-title">推荐问法</h4>
        <div class="docs-prompt-box">
          <p>帮我搜索 Spring AI 的资料，整理核心能力，并生成一份 PDF 摘要。</p>
        </div>
        <div class="docs-prompt-box">
          <p>查看当前项目的 Java 文件结构，按模块总结每个包的作用。</p>
        </div>
      </section>

      <section id="custom-agent" class="docs-section">
        <h2 class="docs-section-title">自定义智能体</h2>
        <div class="docs-divider"></div>
        <p class="docs-text">
          自定义智能体工作台位于 <code class="docs-inline-code">/agents</code>。
          你可以创建专属助手，也可以在公开广场中使用其他用户公开的智能体。
        </p>
        <h3 class="docs-subtitle">创建智能体</h3>
        <ul class="docs-list">
          <li>填写智能体名称，建议让名称直接表达用途。</li>
          <li>上传头像，图片大小不能超过 2MB。</li>
          <li>编写系统提示词，说明角色、任务范围、回答风格和禁止事项。</li>
          <li>选择模型，设置温度和最大 Token。</li>
          <li>按需要开启“公开到广场”和“启用”。</li>
          <li>点击“创建智能体”，创建成功后可在“我的智能体”中开始对话。</li>
        </ul>
        <h3 class="docs-subtitle">提示词建议</h3>
        <div class="docs-prompt-box">
          <p>
            你是一名代码评审助手。请重点检查空指针、权限边界、异常处理和测试缺口。
            回答时先给风险，再给修改建议。
          </p>
        </div>
        <ul class="docs-list">
          <li>把“角色、能力、边界、输出格式”写清楚，效果通常更稳定。</li>
          <li>公开智能体前，避免在提示词中写入密钥、内部地址或个人隐私。</li>
          <li>停用后的智能体不会继续作为可用智能体提供对话。</li>
        </ul>
      </section>

      <section id="chat-history" class="docs-section">
        <h2 class="docs-section-title">对话与文件</h2>
        <div class="docs-divider"></div>
        <h3 class="docs-subtitle">历史对话</h3>
        <ul class="docs-list">
          <li>点击聊天页左上角的历史按钮，可打开历史对话侧边栏。</li>
          <li>点击“新建对话”会生成新的会话 ID，当前页面消息会清空。</li>
          <li>点击某条历史记录可加载该会话的完整消息。</li>
          <li>点击删除按钮可删除对应历史记录，删除前页面会要求确认。</li>
          <li>复制会话 ID 可用于排查问题或和管理员沟通。</li>
        </ul>

        <h3 class="docs-subtitle">模型切换</h3>
        <ul class="docs-list">
          <li>支持模型选择的聊天页会在输入框左侧显示模型切换按钮。</li>
          <li>切换模型只影响之后发送的新消息，不会重写历史回答。</li>
          <li>自定义智能体默认使用创建时配置的模型，聊天页不显示临时模型选择器。</li>
        </ul>

        <h3 class="docs-subtitle">生成文件</h3>
        <p class="docs-text">
          NekoMenus 生成 PDF、下载资源或写入文件后，前端会把工具返回的本地路径转换为
          可点击文件链接。点击链接时，后端会通过受控文件接口打开对应结果。
        </p>
      </section>

      <section id="admin" class="docs-section">
        <h2 class="docs-section-title">后台管理</h2>
        <div class="docs-divider"></div>
        <p class="docs-text">
          后台页面仅管理员可访问。普通用户即使知道路径，也会被路由守卫拦截。
        </p>
        <div class="docs-table-wrap">
          <table class="docs-table">
            <thead>
              <tr>
                <th>页面</th>
                <th>路径</th>
                <th>主要功能</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>用户管理</td>
                <td>/admin/users</td>
                <td>查询、添加、编辑、删除用户，并调整用户角色。</td>
              </tr>
              <tr>
                <td>智能体管理</td>
                <td>/admin/agents</td>
                <td>查看全站智能体，编辑公开状态和启用状态，删除异常智能体。</td>
              </tr>
              <tr>
                <td>兑换码管理</td>
                <td>/admin/quota-codes</td>
                <td>批量生成兑换码，按状态或关键字查询，删除兑换码。</td>
              </tr>
            </tbody>
          </table>
        </div>
        <ul class="docs-list">
          <li>管理员添加用户时，默认密码为页面提示中的初始密码。</li>
          <li>删除用户、智能体、兑换码等操作会要求确认，请确认对象后再执行。</li>
          <li>兑换码生成后请及时分发，超过有效期后不能继续兑换。</li>
        </ul>
      </section>

      <section id="notes" class="docs-section">
        <h2 class="docs-section-title">使用注意</h2>
        <div class="docs-divider"></div>
        <ul class="docs-list">
          <li>AI 回答可能存在不准确内容，重要结论请自行核验。</li>
          <li>不要在对话中输入密码、密钥、身份证号等敏感信息。</li>
          <li>涉及医疗、法律、财务、安全风险的问题，应咨询专业人士。</li>
          <li>使用 NekoMenus 执行终端或文件任务时，请明确说明允许操作的范围。</li>
          <li>如果页面长时间无响应，可停止生成后重新发送更清晰、更小范围的问题。</li>
          <li>遇到模型调用异常、积分不足或登录失效时，请先检查积分和登录状态。</li>
        </ul>

        <h3 class="docs-subtitle">本地开发入口</h3>
        <div class="docs-code-block">
          <div class="docs-code-header">
            <span>PowerShell</span>
          </div>
          <pre><code class="language-bash"># 后端
.\mvnw spring-boot:run

# 前端
cd neko_ai_agent_frontend
npm install
npm run dev</code></pre>
        </div>
        <p class="docs-text">
          默认前端地址为 <code class="docs-inline-code">http://localhost:5173</code>，
          默认后端接口前缀为 <code class="docs-inline-code">http://localhost:8123/api</code>。
        </p>
      </section>
    </div>
  </main>
</template>
