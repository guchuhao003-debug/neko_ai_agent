# Neko AI Agent

<p align="center">
  <strong>基于 Spring AI Alibaba 的多智能体 AI 平台</strong>
</p>

<p align="center">
  融合 <strong>ReAct 自主规划智能体</strong>、<strong>RAG 知识库问答</strong>、<strong>MCP 工具生态</strong> 与 <strong>SSE 流式对话</strong>
</p>

---

## 项目简介

Neko AI Agent 是一个基于 **Spring Boot 3.4.4 + Java 21 + Vue 3** 的 AI 智能体平台，使用 **阿里云 DashScope (Qwen)** 作为大模型底座，集成了 **Spring AI Alibaba**、**LangChain4j** 等主流 AI 框架。

平台提供两个开箱即用的 AI 应用（恋爱大师 + 养宠大师），以及一个具备自主规划能力的 **NekoManus 超级智能体**，支持工具调用、RAG 知识库增强、文件会话记忆持久化、MCP 多服务器扩展。

---

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Neko AI Agent Platform                    │
├───────────────┬──────────────────────┬──────────────────────┤
│   Frontend    │      Backend         │    MCP Server         │
│  Vue 3 + Vite │  Spring Boot 3.4.4   │  Spring AI MCP        │
│   Port 5173   │  Port 8123 (/api)    │  stdio transport      │
├───────────────┴──────────────────────┴──────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                    AI Applications                     │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │   │
│  │  │   LoveApp    │  │   PetApp     │  │ NekoManus  │  │   │
│  │  │  恋爱大师     │  │  养宠大师     │  │ 超级智能体   │  │   │
│  │  └──────┬───────┘  └──────┬───────┘  └─────┬──────┘  │   │
│  │         │                 │                │          │   │
│  │         └─────────────────┴────────────────┘          │   │
│  │                           │                            │   │
│  │  ┌────────────────────────┼────────────────────────┐  │   │
│  │  │              ChatClient (Spring AI)              │  │   │
│  │  └────────────────────────┼────────────────────────┘  │   │
│  │                           │                            │   │
│  └───────────────────────────┼────────────────────────────┘   │
│                              │                                │
│  ┌───────────┬───────────────┼───────────────┬──────────┐    │
│  │  Advisors │     RAG       │    Tools      │  Memory  │    │
│  │ ┌───────┐ │ ┌───────────┐ │ ┌───────────┐ │ ┌──────┐ │    │
│  │ │Logger │ │ │DocLoader  │ │ │FileOps    │ │ │File- │ │    │
│  │ │ReRead │ │ │TextSplit  │ │ │WebSearch  │ │ │Based │ │    │
│  │ │Memory │ │ │KeywordEnr │ │ │WebScrape  │ │ │Kryo  │ │    │
│  │ │RAG    │ │ │QueryRwrt  │ │ │PdfGen     │ │ │      │ │    │
│  │ └───────┘ │ │VectorStore│ │ │EmailSend  │ │ └──────┘ │    │
│  │           │ └───────────┘ │ │Terminal   │ │           │    │
│  └───────────┴───────────────┴─│Terminate  │─┴───────────┘    │
│                                └───────────┘                  │
│                                                               │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │                     AI Provider                           │ │
│  │  DashScope (Qwen) · Spring AI · LangChain4j · HTTP SDK   │ │
│  └──────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

### 模块划分

| 模块 | 说明 | 技术栈 |
|------|------|--------|
| `neko_ai_agent` (root) | 后端主服务 | Spring Boot 3.4.4, Java 21, Maven |
| `neko_ai_agent_frontend` | 前端 UI | Vue 3, Vite, Axios |
| `neko-image-search-mcp-server` | MCP 图片搜索服务 | Spring Boot 3.4.5, Spring AI MCP Server |

---

## 核心功能

### 1. AI 恋爱大师 (LoveApp)

基于 RAG 知识库的恋爱心理专家对话系统：

- **知识库问答**：已加载 3 篇恋爱知识文档（单身篇 / 恋爱篇 / 已婚篇），基于 DashScope Embedding 向量化
- **多轮对话记忆**：`FileBasedChatMemory` 将对话历史使用 Kryo 序列化到 `tmp/chat_memory/` 目录，按 `chatId` 隔离会话
- **查询重写**：`RewriteQueryTransformer` 在检索前对用户输入进行语义改写，提升召回率
- **文档处理管线**：加载 → Token 切分 (`MyTokenTextSplitter`) → 关键词元信息增强 (`KeywordMetadataEnricher`) → 向量入库 (`SimpleVectorStore`)
- **结构化输出**：支持生成结构化恋爱报告 (`LoveReport`)
- **多 RAG 源切换**：代码内置了本地向量库 / 阿里云知识库 (`DashScopeDocumentRetriever`) / PgVector 三种方案
- **工具调用**：可搭配全部 8 个工具（搜索、文件、邮件等）
- **MCP 扩展**：支持通过 `ToolCallbackProvider` 动态加载外部 MCP 工具

### 2. AI 养宠大师 (PetApp)

10 年资深宠物养护专家 AI 角色扮演：

- 精心设计的 System Prompt（角色定义 / 对话风格 / 核心能力 / 引导策略 / 回答框架 / 驱虫知识）
- 支持基础对话 + 流式 SSE 输出 + 结构化报告 (`PetReport`) + 工具调用 + MCP 扩展

### 3. NekoManus 超级智能体

具备自主规划与工具调用能力的 ReAct Agent：

- **Think → Act 循环**：每步先思考是否需要调用工具，再执行工具调用
- **手动工具调用管理**：因 DashScope API 与 Spring AI 内置工具调用机制不兼容，自行维护 `ChatOptions` 和消息上下文（`proxyToolCalls=true`）
- **终止机制**：`TerminateTool` 允许 Agent 判断任务完成后主动终止，在 `ToolCallAgent.act()` 中检测并设置 `AgentState.FINISHED`
- **最大 20 步执行限制**，防止无限循环
- **SSE 流式输出**：每一步的思考与执行结果实时推送到前端

### 4. 工具生态 (8 个内置工具)

| 工具 | 功能 | 实现方式 |
|------|------|----------|
| `FileOperationTool` | 读写本地文件 | Hutool `FileUtil` |
| `WebSearchTool` | 百度搜索引擎检索 | SearchAPI.io |
| `WebScrapingTool` | 网页内容抓取 | Jsoup |
| `ResourceDownloadTool` | 从 URL 下载资源 | Hutool `HttpUtil.downloadFile` |
| `TerminalOperationTool` | 执行终端命令 | `ProcessBuilder("cmd.exe")` |
| `PDFGenerationTool` | 生成 PDF 文件 | iText Core 9.1 |
| `QQEmailSenderTool` | QQ 邮箱 SMTP 发送 | JavaMail (STARTTLS) |
| `TerminateTool` | 终止 Agent 任务 | 标志位切换 AgentState |

### 5. MCP 多服务器集成

通过 `mcp-servers.json` 配置 stdio 传输方式的 MCP 服务器：

- **高德地图 MCP**：npx 启动 `@amap/amap-maps-mcp-server`
- **图片搜索 MCP**：本地 JAR 包 `neko-image-search-mcp-server`（Pexels API）

`LoveApp` 和 `PetApp` 均可通过 `ToolCallbackProvider` 注入 MCP 工具，与内置工具混合调用。

### 6. RAG 知识库管线

```
文档加载               文本分割              关键词增强           向量存储
┌──────────┐        ┌──────────┐         ┌──────────┐        ┌──────────┐
│ Markdown │   →    │  Token   │    →    │ Keyword  │   →    │ Simple   │
│ Loader   │        │ Splitter │         │ Enricher │        │ Vector   │
│          │        │(200/100) │         │  (Top5)  │        │ Store    │
└──────────┘        └──────────┘         └──────────┘        └────┬─────┘
                                                                  │
用户查询 → QueryRewriter(Rewrite) → QuestionAnswerAdvisor → ChatClient
```

- `GitHubDocumentReader`：实现了 `DocumentReader` 接口，支持在 GitHub 仓库递归加载文档（文件扩展名过滤、文件大小限制、自定义元数据）
- `LoveAppRagCustomAdvisorFactory`：自定义 RAG Advisor（文档过滤 + 相似度阈值 + TopK + 上下文增强）
- `LoveAppRagCloudAdvisorConfig`：基于阿里云 DashScope 云端知识库

### 7. 对话记忆持久化

`FileBasedChatMemory` 实现了 Spring AI 的 `ChatMemory` 接口：
- 每个会话 (`chatId`) 独立存储为一个 `.kryo` 文件
- 使用 Kryo 序列化 `List<Message>`，比 Java 原生序列化更快更小
- 支持按 `lastN` 参数检索最近 N 条历史消息

### 8. SSE 流式输出（3 种方式）

| 方式 | 说明 |
|------|------|
| `Flux<String>` | WebFlux 响应式流，`produces = TEXT_EVENT_STREAM_VALUE` |
| `Flux<ServerSentEvent<String>>` | 结构化 SSE 事件，可携带 event id / comment / retry |
| `SseEmitter` | 手动控制 SSE 生命周期（超时回调、完成回调、错误处理） |

前端使用 `EventSource` 接收流 + 20ms 间隔打字机队列逐字渲染。

---

## 项目结构

```
neko_ai_agent/
├── src/main/java/com/wenxi/neko_ai_agent/
│   ├── NekoAiAgentApplication.java      # Spring Boot 启动类
│   ├── agent/                            # 智能体框架
│   │   ├── BaseAgent.java                #   抽象基类（状态机 + 步进循环 + SSE）
│   │   ├── ReActAgent.java               #   Think → Act 模式
│   │   ├── ToolCallAgent.java            #   手动工具调用管理
│   │   ├── NekoManus.java                #   超级智能体（@Component）
│   │   └── model/AgentState.java         #   IDLE/RUNNING/FINISHED/ERROR
│   ├── app/                              # AI 应用
│   │   ├── LoveApp.java                  #   恋爱大师（RAG + 工具 + MCP）
│   │   └── PetApp.java                   #   养宠大师（工具 + MCP）
│   ├── controller/                       # HTTP 接口
│   │   ├── AiController.java             #   /ai/love_app/*, /ai/pet_app/*, /ai/manus/*
│   │   └── HealthController.java         #   /health
│   ├── tools/                            # 内置工具集
│   │   ├── ToolRegistration.java         #   工具注册中心（@Configuration @Bean）
│   │   ├── FileOperationTool.java
│   │   ├── WebSearchTool.java
│   │   ├── WebScrapingTool.java
│   │   ├── ResourceDownloadTool.java
│   │   ├── TerminalOperationTool.java
│   │   ├── PDFGenerationTool.java
│   │   ├── QQEmailSenderTool.java
│   │   ├── TerminateTool.java
│   │   └── UserQueryTool.java
│   ├── rag/                              # RAG 检索增强生成
│   │   ├── LoveAppDocumentLoader.java    #   文档加载
│   │   ├── MyTokenTextSplitter.java      #   Token 分割
│   │   ├── MyKeywordEnricher.java        #   关键词增强
│   │   ├── LoveAppVectorStoreConfig.java #   向量库配置（@Primary Bean）
│   │   ├── QueryRewriter.java            #   查询重写
│   │   ├── GitHubDocumentReader.java     #   GitHub 文档读取器
│   │   ├── LoveAppRagCustomAdvisorFactory.java
│   │   └── LoveAppContextualQueryAugmenterFactory.java
│   ├── advisor/                          # 横切关注点
│   │   ├── MyLoggerAdvisor.java          #   请求/响应日志
│   │   └── ReReadingAdvisor.java         #   Re2 推理增强
│   ├── chatmemory/
│   │   └── FileBasedChatMemory.java      # Kryo 文件持久化记忆
│   ├── config/
│   │   ├── CorsConfig.java               #   全局跨域
│   │   └── LoveAppRagCloudAdvisorConfig.java
│   ├── common/                           # 通用工具
│   │   ├── GlobalExceptionHandler.java   #   全局异常处理
│   │   └── ResultUtils.java              #   统一响应工具
│   ├── exception/                        # 异常体系
│   │   ├── BaseResponse.java             #   统一响应包装
│   │   ├── BusinessException.java        #   业务异常
│   │   └── ErrorCode.java                #   错误码枚举
│   ├── constant/                         # 常量定义
│   │   ├── FileConstant.java
│   │   ├── GlobalConstant.java
│   │   └── PromptConstant.java           #   养宠大师 System Prompt
│   └── demo/                             # 演示代码
│       ├── invoke/                       #   4 种 AI 调用方式演示
│       └── rag/MultiQueryExpanderDemo.java
├── src/main/resources/
│   ├── application.yml                   #   通用配置
│   ├── application-local.yml             #   本地环境（API Key 等）
│   ├── application-prod.yml              #   生产环境
│   ├── mcp-servers.json                  #   MCP 服务器配置
│   └── document/                         #   RAG 知识文档
├── neko_ai_agent_frontend/               # Vue 3 前端（独立 CLAUDE.md）
├── neko-image-search-mcp-server/         # MCP 图片搜索服务
└── tmp/                                  # 运行时文件（内存、下载、PDF）
```

---

## 快速开始

### 环境要求

- **JDK 21**
- **Maven**（或使用 `mvnw` wrapper）
- **Node.js 18+**（前端）

### 1. 配置 API Key

编辑 `src/main/resources/application-local.yml`，填入你的 DashScope API Key：

```yaml
spring:
  ai:
    dashscope:
      api-key: sk-your-api-key-here
```

### 2. 构建 MCP 服务器（可选）

```bash
cd neko-image-search-mcp-server
../mvnw package -DskipTests
```

### 3. 启动后端

```bash
./mvnw spring-boot:run
```

后端运行在 `http://localhost:8123/api`，Swagger 文档在 `http://localhost:8123/api/swagger-ui.html`。

### 4. 启动前端

```bash
cd neko_ai_agent_frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`。

---

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/health` | 健康检查 |
| `GET` | `/api/ai/love_app/chat/sync` | 恋爱大师 - 同步对话 |
| `GET` | `/api/ai/love_app/chat/sse` | 恋爱大师 - SSE 流式对话 |
| `GET` | `/api/ai/love_app/chat/sse_emitter` | 恋爱大师 - SseEmitter 流式 |
| `GET` | `/api/ai/pet_app/chat/sync` | 养宠大师 - 同步对话（结构化报告） |
| `GET` | `/api/ai/pet_app/chat/sse` | 养宠大师 - SSE 流式对话 |
| `GET` | `/api/ai/manus/chat` | NekoManus 超级智能体 - SseEmitter 流式 |

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `message` | String | 是 | 用户输入消息 |
| `chatId` | String | LoveApp/PetApp 必填 | 会话 ID，用于多轮对话记忆 |

---

## 设计模式与亮点

- **Agent 继承体系**：`BaseAgent` → `ReActAgent` → `ToolCallAgent` → `NekoManus` 四层抽象，职责清晰
- **工厂 + 注册 + 适配器**：`ToolRegistration.allTools()` 统一管理工具 Bean，`ToolCallbacks.from()` 适配不同工具类
- **Advisor 链模式**：`MessageChatMemoryAdvisor` → `MyLoggerAdvisor` → `QuestionAnswerAdvisor`，可插拔组合
- **SSE 多态输出**：同一份业务逻辑（`LoveApp.doChatByStream`）可通过 `Flux` / `ServerSentEvent` / `SseEmitter` 三种方式暴露
- **Builder 模式**：`GitHubDocumentReader`、`DashScopeChatOptions` 等均采用 Builder 构造
- **策略模式**：RAG 可在本地向量库 / 阿里云知识库 / PgVector 之间切换

---

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Spring Boot 3.4.4, Spring AI 1.0.0-M6, Spring AI Alibaba 1.0.0-M6.1 |
| AI 模型 | 阿里云 DashScope (Qwen-Plus), DashScope Embedding |
| AI 框架 | Spring AI, LangChain4j (DashScope Community), dashscope-sdk-java 2.19.2 |
| 工具库 | Hutool 5.8.37, Jsoup 1.19.1, iText Core 9.1, Kryo 5.6.2 |
| API 文档 | Knife4j 4.4.0 (Swagger) |
| 前端 | Vue 3.5, Vite 8, Vue Router 5, Axios |
| MCP | Spring AI MCP Client/Server 1.0.0-M6 |
| 序列化 | Kryo 5.6.2, Jackson |
| 构建 | Maven Wrapper, JDK 21 |

---

## AI 调用方式对比

项目在 `demo/invoke/` 中演示了 4 种调用 DashScope 的方式：

| 方式 | 类 | 特点 |
|------|-----|------|
| HTTP 原生 | `HttpAiInvoke` | 直接 HTTP 调用 DashScope API |
| 官方 SDK | `SdkAiInvoke` | 阿里云 dashscope-sdk-java |
| LangChain4j | `LangChainAiInvoke` | langchain4j-community-dashscope |
| Spring AI | `SpringAIInvoke` | Spring AI Alibaba Starter（推荐） |
