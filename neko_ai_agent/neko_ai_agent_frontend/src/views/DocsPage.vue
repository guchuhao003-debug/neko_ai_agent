<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import hljs from 'highlight.js/lib/core'
import java from 'highlight.js/lib/languages/java'
import bash from 'highlight.js/lib/languages/bash'
import javascript from 'highlight.js/lib/languages/javascript'
import 'highlight.js/styles/github-dark.css'

hljs.registerLanguage('java', java)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('javascript', javascript)

const router = useRouter()
const activeSection = ref('intro')

const sections = [
  { id: 'intro', label: '项目简介' },
  { id: 'tech-stack', label: '技术栈' },
  { id: 'quick-start', label: '快速开始' },
  { id: 'agents', label: '智能体使用指南' },
  { id: 'agent-love', label: 'AI 恋爱大师', indent: true },
  { id: 'agent-manus', label: 'NekoMenus 超级智能体', indent: true },
  { id: 'agent-pet', label: 'AI 宠物专家', indent: true },
  { id: 'architecture', label: '核心架构' },
]

const scrollToSection = (id) => {
  activeSection.value = id
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

// Track active section on scroll
let observer = null

onMounted(() => {
  const options = {
    root: null,
    rootMargin: '-80px 0px -60% 0px',
    threshold: 0
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

  // Apply syntax highlighting to all code blocks
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
    <!-- Sidebar -->
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
        <h3 class="docs-nav-title">目录</h3>
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

    <!-- Main Content -->
    <div class="docs-main">
      <!-- 项目简介 -->
      <section id="intro" class="docs-section">
        <h1 class="docs-page-title">Neko AI Agent 文档</h1>
        <p class="docs-page-subtitle">全面了解 Neko AI Agent 智能体平台的功能与使用方法</p>

        <h2 class="docs-section-title">项目简介</h2>
        <div class="docs-divider"></div>
        <p class="docs-text">
          <strong>Neko AI Agent</strong> 是一个多智能体 AI 对话平台，基于 Spring AI + 阿里云百炼大模型构建。
          平台提供多种 AI 智能体，支持 SSE 实时流式对话、多轮会话记忆、RAG 知识增强、MCP 工具调用等能力。
        </p>
        <div class="docs-feature-grid">
          <div class="docs-feature-card">
            <div class="docs-feature-icon">🤖</div>
            <h4>多智能体架构</h4>
            <p>支持 ReAct Agent、ChatClient Agent 等多种智能体模式</p>
          </div>
          <div class="docs-feature-card">
            <div class="docs-feature-icon">⚡</div>
            <h4>SSE 实时流式</h4>
            <p>三种 SSE 流式模式，字符级实时输出</p>
          </div>
          <div class="docs-feature-card">
            <div class="docs-feature-icon">🧠</div>
            <h4>多轮记忆</h4>
            <p>基于 chatId 的会话记忆，支持上下文连续对话</p>
          </div>
          <div class="docs-feature-card">
            <div class="docs-feature-icon">🔧</div>
            <h4>工具编排</h4>
            <p>8 种内置工具 + MCP 协议外部工具扩展</p>
          </div>
        </div>
      </section>

      <!-- 技术栈 -->
      <section id="tech-stack" class="docs-section">
        <h2 class="docs-section-title">技术栈</h2>
        <div class="docs-divider"></div>

        <div class="docs-stack-group">
          <h3 class="docs-subtitle">后端</h3>
          <ul class="docs-list">
            <li><strong>Spring Boot 3.4.4</strong> — Java 21，Web 应用框架</li>
            <li><strong>Spring AI + spring-ai-alibaba-starter</strong> — AI 应用开发框架</li>
            <li><strong>DashScope (通义千问 Qwen)</strong> — 阿里云百炼大模型服务</li>
            <li><strong>MyBatis-Plus</strong> — ORM 数据库操作</li>
            <li><strong>MCP (Model Context Protocol)</strong> — 外部工具协议集成</li>
            <li><strong>腾讯云 COS</strong> — 对象存储（头像等文件上传）</li>
          </ul>
        </div>

        <div class="docs-stack-group">
          <h3 class="docs-subtitle">前端</h3>
          <ul class="docs-list">
            <li><strong>Vue 3</strong> — 渐进式前端框架（Composition API）</li>
            <li><strong>Vite</strong> — 新一代前端构建工具</li>
            <li><strong>Vue Router</strong> — 路由管理</li>
            <li><strong>Axios</strong> — HTTP 请求（含 json-bigint 大数处理）</li>
            <li><strong>EventSource</strong> — SSE 流式连接</li>
          </ul>
        </div>

        <div class="docs-stack-group">
          <h3 class="docs-subtitle">AI 模型</h3>
          <ul class="docs-list">
            <li><strong>Qwen-Max</strong> — 通义千问旗舰模型（用于超级智能体复杂推理）</li>
            <li><strong>Qwen-Plus</strong> — 通义千问增强模型（用于恋爱专家、宠物专家）</li>
          </ul>
        </div>
      </section>

      <!-- 快速开始 -->
      <section id="quick-start" class="docs-section">
        <h2 class="docs-section-title">快速开始</h2>
        <div class="docs-divider"></div>

        <h3 class="docs-subtitle">环境要求</h3>
        <ul class="docs-list">
          <li>JDK 21+</li>
          <li>Maven 3.8+</li>
          <li>Node.js 18+</li>
        </ul>

        <h3 class="docs-subtitle">启动后端</h3>
        <div class="docs-code-block">
          <div class="docs-code-header">
            <span>Shell</span>
          </div>
          <pre><code class="language-bash"># 克隆项目
git clone https://github.com/guchuhao003-debug/neko_ai_agent.git
cd neko_ai_agent

# 配置 API Key（编辑 application-local.yml）
# dashscope.api-key: your-api-key

# 启动后端服务（端口 8123，context-path /api）
./mvnw spring-boot:run</code></pre>
        </div>

        <h3 class="docs-subtitle">启动前端</h3>
        <div class="docs-code-block">
          <div class="docs-code-header">
            <span>Shell</span>
          </div>
          <pre><code class="language-bash"># 进入前端目录
cd neko_ai_agent_frontend

# 安装依赖
npm install

# 启动开发服务器（端口 5173）
npm run dev</code></pre>
        </div>

        <p class="docs-text">启动后访问 <code class="docs-inline-code">http://localhost:5173</code> 即可使用平台。</p>
      </section>

      <!-- 智能体使用指南 -->
      <section id="agents" class="docs-section">
        <h2 class="docs-section-title">智能体使用指南</h2>
        <div class="docs-divider"></div>
        <p class="docs-text">Neko AI Agent 平台提供三种不同类型的 AI 智能体，各具特色。以下分别介绍其功能、使用方法和实现原理。</p>
      </section>

      <!-- AI 恋爱大师 -->
      <section id="agent-love" class="docs-section docs-section-sub">
        <h3 class="docs-subtitle docs-agent-title">
          <span class="docs-agent-badge love">♥</span>
          AI 恋爱大师
        </h3>
        <p class="docs-text">
          AI 恋爱大师是一位专业的恋爱咨询顾问，能够分析感情状况、提供沟通建议、解答恋爱困惑。
          支持多轮对话记忆和 RAG 知识增强，提供更加专业和个性化的建议。
        </p>

        <h4 class="docs-small-title">示例 Prompt</h4>
        <div class="docs-prompt-box">
          <p>"我和女朋友因为异地恋经常吵架，她觉得我不够关心她，我该怎么改善？"</p>
        </div>
        <div class="docs-prompt-box">
          <p>"第一次约会应该聊些什么话题？怎么避免冷场？"</p>
        </div>
        <div class="docs-prompt-box">
          <p>"对方回复消息越来越慢，是不是对我没兴趣了？我应该怎么做？"</p>
        </div>

        <h4 class="docs-small-title">实现流程</h4>
        <div class="docs-code-block">
          <div class="docs-code-header">
            <span>Java — LoveApp.java</span>
          </div>
          <pre><code class="language-java">@Component
public class LoveApp {
    private final ChatClient chatClient;

    public LoveApp(ChatModel chatModel, VectorStore vectorStore,
                   FileBasedChatMemory chatMemory) {
        this.chatClient = ChatClient.builder(chatModel)
            .defaultSystem("你是一位专业的恋爱咨询专家...")
            .defaultAdvisors(
                new MessageChatMemoryAdvisor(chatMemory),  // 多轮记忆
                new QuestionAnswerAdvisor(vectorStore)      // RAG 知识增强
            )
            .build();
    }

    public Flux&lt;String&gt; doChatByStream(String message, String chatId) {
        return chatClient.prompt()
            .user(message)
            .advisors(a -> a.param("chat_memory_conversation_id", chatId))
            .stream().content();
    }
}</code></pre>
        </div>
        <ul class="docs-list">
          <li><strong>ChatClient</strong> — 封装 AI 对话客户端</li>
          <li><strong>MessageChatMemoryAdvisor</strong> — 基于 chatId 的多轮对话记忆</li>
          <li><strong>QuestionAnswerAdvisor</strong> — RAG 向量检索增强回答质量</li>
          <li><strong>FileBasedChatMemory</strong> — Kryo 序列化本地持久化会话</li>
        </ul>
      </section>

      <!-- NekoMenus 超级智能体 -->
      <section id="agent-manus" class="docs-section docs-section-sub">
        <h3 class="docs-subtitle docs-agent-title">
          <span class="docs-agent-badge manus">N</span>
          NekoMenus 超级智能体
        </h3>
        <p class="docs-text">
          NekoMenus 是基于 ReAct（Reasoning + Acting）模式的超级智能体，具备多工具编排能力。
          能自主拆解复杂任务为多个步骤，逐步思考并执行，支持文件操作、网页搜索、终端命令、PDF 生成等。
        </p>

        <h4 class="docs-small-title">示例 Prompt</h4>
        <div class="docs-prompt-box">
          <p>"帮我搜索关于 Vue 3 Composition API 的最新资料，总结核心用法并生成一份 PDF 文档。"</p>
        </div>
        <div class="docs-prompt-box">
          <p>"查看当前目录下的文件列表，找到所有 .java 文件并统计代码行数。"</p>
        </div>
        <div class="docs-prompt-box">
          <p>"帮我发一封邮件给同事，内容是本周项目进展汇报。"</p>
        </div>

        <h4 class="docs-small-title">工具列表</h4>
        <div class="docs-table-wrap">
          <table class="docs-table">
            <thead>
              <tr>
                <th>工具名称</th>
                <th>功能描述</th>
              </tr>
            </thead>
            <tbody>
              <tr><td>FileOperationTool</td><td>文件读写、创建、删除操作</td></tr>
              <tr><td>WebSearchTool</td><td>联网搜索获取实时信息</td></tr>
              <tr><td>WebScrapingTool</td><td>网页内容抓取与解析</td></tr>
              <tr><td>ResourceDownloadTool</td><td>资源文件下载</td></tr>
              <tr><td>TerminalOperationTool</td><td>终端命令执行</td></tr>
              <tr><td>PDFGenerationTool</td><td>PDF 文档生成</td></tr>
              <tr><td>QQEmailSenderTool</td><td>QQ 邮箱邮件发送</td></tr>
              <tr><td>TerminateTool</td><td>任务终止信号</td></tr>
            </tbody>
          </table>
        </div>

        <h4 class="docs-small-title">实现流程</h4>
        <div class="docs-code-block">
          <div class="docs-code-header">
            <span>Java — Agent 架构层次</span>
          </div>
          <pre><code class="language-plaintext">BaseAgent                    // 状态机: IDLE → RUNNING → FINISHED/ERROR
  └─ ReActAgent              // Think → Act 循环模式
       └─ ToolCallAgent      // 手动工具调用（兼容 DashScope）
            └─ NekoManus     // 具体实现: 最多 20 步，多工具编排

// NekoManus 执行流程:
// 1. think() — 调用 LLM 分析任务，决定下一步行动
// 2. act()   — 执行 LLM 选择的工具
// 3. 检查 TerminateTool 是否被调用 → 结束
// 4. 循环直到任务完成或达到最大步数</code></pre>
        </div>
      </section>

      <!-- AI 宠物专家 -->
      <section id="agent-pet" class="docs-section docs-section-sub">
        <h3 class="docs-subtitle docs-agent-title">
          <span class="docs-agent-badge pet">🐾</span>
          AI 宠物专家
        </h3>
        <p class="docs-text">
          AI 宠物专家提供科学的宠物养护建议，涵盖饮食、健康、训练、日常护理等方面。
          通过 MCP 协议集成外部工具（如图片搜索），并支持多轮对话记忆。
        </p>

        <h4 class="docs-small-title">示例 Prompt</h4>
        <div class="docs-prompt-box">
          <p>"我家金毛最近食欲不振，精神也不太好，可能是什么原因？需要去医院吗？"</p>
        </div>
        <div class="docs-prompt-box">
          <p>"3个月大的小猫应该怎么喂食？一天喂几次合适？"</p>
        </div>
        <div class="docs-prompt-box">
          <p>"如何训练狗狗定点上厕所？有什么有效的方法？"</p>
        </div>

        <h4 class="docs-small-title">实现流程</h4>
        <div class="docs-code-block">
          <div class="docs-code-header">
            <span>Java — PetApp.java</span>
          </div>
          <pre><code class="language-java">@Component
public class PetApp {
    private final ChatClient chatClient;

    public PetApp(ChatModel chatModel, FileBasedChatMemory chatMemory,
                  ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = ChatClient.builder(chatModel)
            .defaultSystem("你是一位专业的宠物健康顾问...")
            .defaultAdvisors(
                new MessageChatMemoryAdvisor(chatMemory)
            )
            .defaultTools(toolCallbackProvider)  // MCP 工具
            .build();
    }

    public Flux&lt;String&gt; doChatWithMcp(String message, String chatId) {
        return chatClient.prompt()
            .user(message)
            .advisors(a -> a.param("chat_memory_conversation_id", chatId))
            .stream().content();
    }
}</code></pre>
        </div>
        <ul class="docs-list">
          <li><strong>MCP Tools</strong> — 通过 stdio 传输连接外部 MCP 服务（高德地图、图片搜索等）</li>
          <li><strong>ToolCallbackProvider</strong> — 自动注入所有已配置的 MCP 工具</li>
          <li><strong>FileBasedChatMemory</strong> — 多轮对话上下文记忆</li>
        </ul>
      </section>

      <!-- 核心架构 -->
      <section id="architecture" class="docs-section">
        <h2 class="docs-section-title">核心架构</h2>
        <div class="docs-divider"></div>

        <h3 class="docs-subtitle">SSE 流式通信</h3>
        <p class="docs-text">平台支持三种 SSE 流式输出模式，适配不同场景：</p>
        <div class="docs-table-wrap">
          <table class="docs-table">
            <thead>
              <tr>
                <th>模式</th>
                <th>端点后缀</th>
                <th>机制</th>
                <th>适用场景</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Flux&lt;String&gt;</td>
                <td>/sse</td>
                <td>Spring WebFlux 响应式流</td>
                <td>恋爱大师、宠物专家</td>
              </tr>
              <tr>
                <td>Flux&lt;ServerSentEvent&gt;</td>
                <td>/server_sent_event</td>
                <td>结构化 SSE（含元数据）</td>
                <td>需要事件类型区分的场景</td>
              </tr>
              <tr>
                <td>SseEmitter</td>
                <td>/sse_emitter</td>
                <td>手动 SSE + 超时回调</td>
                <td>超级智能体分步输出</td>
              </tr>
            </tbody>
          </table>
        </div>

        <h3 class="docs-subtitle">前端打字机效果</h3>
        <div class="docs-code-block">
          <div class="docs-code-header">
            <span>JavaScript — ChatRoom.vue (Typewriter Queue)</span>
          </div>
          <pre><code class="language-javascript">// SSE 接收数据 → 推入队列
eventSource.onmessage = (event) => {
  typingQueue.push(event.data)
}

// 定时器每 20ms 从队列取字符渲染
setInterval(() => {
  if (typingQueue.length > 0) {
    const chars = typingQueue.splice(0, 3) // 每次渲染 3 字符
    currentMessage.value += chars.join('')
  }
}, 20)</code></pre>
        </div>

        <h3 class="docs-subtitle">项目结构</h3>
        <div class="docs-code-block">
          <div class="docs-code-header">
            <span>目录结构</span>
          </div>
          <pre><code class="language-plaintext">neko_ai_agent/
├── src/main/java/com/wenxi/neko_ai_agent/
│   ├── agent/          # Agent 智能体（BaseAgent → ReActAgent → NekoManus）
│   ├── app/            # AI 应用（LoveApp、PetApp）
│   ├── config/         # 配置类（COS、AI 模型）
│   ├── controller/     # API 控制器（AiController、UserController）
│   ├── manager/        # 管理器（CosManager）
│   ├── service/        # 业务服务层
│   └── tools/          # 工具类（8 种 Tool）
├── neko_ai_agent_frontend/
│   ├── src/views/      # 页面组件
│   ├── src/components/ # 通用组件（ChatRoom、NavBar）
│   └── src/api/        # API 接口封装
└── neko-image-search-mcp-server/  # MCP 图片搜索服务</code></pre>
        </div>
      </section>
    </div>
  </main>
</template>
