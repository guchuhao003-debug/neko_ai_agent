# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Vue 3 + Vite frontend for **Neko AI Agent**, a dual-AI-agent platform with SSE streaming chat.

- **Home** (`/`): App switching page with three AI agent cards
- **AI 恋爱大师** (`/love`): Chat with `chatId` session support, single AI bubble per turn
- **AI 宠物专家** (`/pet`): Chat with `chatId` session support, single AI bubble per turn
- **AI 超级智能体** (`/manus`): Step-based chat, thinking panel + final result

## Commands

```bash
npm install          # Install dependencies
npm run dev          # Start Vite dev server (default port 5173)
npm run build        # Production build → dist/
npm run preview      # Preview production build
```

Backend runs at `http://localhost:8123`, API base: `http://localhost:8123/api`.

### Backend API Endpoints

- **AI 恋爱大师 SSE**: `GET /api/ai/love_app/chat/sse?message=xxx&chatId=xxx`
  - Requires `chatId` parameter for session tracking
  - Returns SSE stream with AI responses
- **AI 宠物专家 SSE**: `GET /api/ai/pet_app/chat/sse?message=xxx&chatId=xxx`
  - Requires `chatId` parameter for session tracking
  - Returns SSE stream with AI responses
- **AI 超级智能体 SSE**: `GET /api/ai/manus/chat?message=xxx`
  - No `chatId` required
  - Returns SSE stream with step-by-step AI responses

## Architecture

### Build Configuration

- [vite.config.js](vite.config.js) — Minimal Vite setup with Vue plugin only; no path aliases, no dev server proxy
- No test framework configured; no linting setup

### State Management

No global state library (Vuex/Pinia). All state is component-local:
- ChatRoom manages its own messages, loading state, SSE connections
- Session IDs (`chatId`) generated in page components and passed as props
- No shared stores or composables for cross-component state

### Entry & Routing

- [src/main.js](src/main.js) — Creates Vue app, registers router
- [src/router/index.js](src/router/index.js) — Routes with meta (title, description); `router.afterEach` updates `document.title` and `<meta name="description">`

### Page Structure

- [src/App.vue](src/App.vue) — Root shell with `<router-view>` + site footer
- [src/views/HomePage.vue](src/views/HomePage.vue) — Home page with three app card entries
- [src/views/LoveChatPage.vue](src/views/LoveChatPage.vue) — Thin wrapper around ChatRoom with `chatId` session
  - Session ID: `crypto.randomUUID()` (standard UUID format)
- [src/views/PetChatPage.vue](src/views/PetChatPage.vue) — Thin wrapper around ChatRoom with `chatId` session
  - Session ID: `crypto.randomUUID()` (standard UUID format)
- [src/views/ManusChatPage.vue](src/views/ManusChatPage.vue) — Thin wrapper around ChatRoom with `stepBubbleMode: true`
  - Session ID: `MANUS-${Date.now()}` (timestamp-based format)
- [src/views/DocsPage.vue](src/views/DocsPage.vue) — 操作文档页面（纯前端静态）
  - 路由: `/docs`，从 NavBar "操作文档"按钮进入
  - 左侧 sticky 目录导航 + 右侧文档内容（项目简介、技术栈、快速开始、智能体使用指南、核心架构）
  - 包含示例代码块和示例 Prompt 展示
  - IntersectionObserver 自动高亮当前章节
- [src/views/UserManagePage.vue](src/views/UserManagePage.vue) — 管理员用户管理页面（CRUD）
  - 路由: `/admin/users`，仅管理员可从 NavBar 下拉菜单进入
  - 功能: 用户列表分页、按 ID 搜索、添加用户、编辑用户、删除用户
  - 调用后端接口: `/user/list/page/vo`、`/user/get`、`/user/add`、`/user/delete`、`/user/update`
- [src/components/ChatRoom.vue](src/components/ChatRoom.vue) — Core chat component handling all SSE logic

### SSE Handling

All SSE streaming logic lives in [ChatRoom.vue](src/components/ChatRoom.vue):

- Uses native `EventSource` for SSE connections
- Custom typewriter queue (`setInterval` at 20ms) for character-by-character rendering
- Supports multiple SSE event names: `message`, `token`, `delta`, `chunk`, `content`, `answer`
- Supports `done` / `complete` events for explicit stream termination
- Compatible with JSON payloads (extracts `delta` / `content` / `text` fields)
- `stepBubbleMode`: each step creates a new AI bubble with duplicate detection
- Handles connection-close-as-completion when AI content exists
- **Markdown rendering**: AI bubbles use `marked` library to render structured Markdown (headings, lists, code blocks, tables, etc.)

#### Typewriter Queue Implementation

The typewriter effect uses a queue-based system to ensure smooth character-by-character rendering:

1. **Queue Management**: Incoming SSE chunks are pushed to `typingQueue[]`
2. **Interval Timer**: `setInterval` at 20ms processes the queue
3. **Batch Processing**: Renders 3 characters per tick for smooth animation
4. **Task Tracking**: `activeTypingTask` holds current rendering task
5. **Stream Completion**: Only finalizes when queue is empty AND stream has ended

#### Error Handling

- **Connection Errors**: If `EventSource.CLOSED` and AI content exists, treats as normal completion
- **No Content Errors**: Shows "连接中断，请稍后重试。" if connection fails before any AI response
- **Manual Stop**: `stopGenerating()` immediately clears queue and closes connection
- **Cleanup**: `onBeforeUnmount` ensures all connections and timers are properly closed

#### Step Bubble Mode (Manus) — Thinking Panel

- `stepBubbleMode` 不再为每步创建单独气泡，改为收集所有步骤到一个可折叠的"深度思考"面板
- 面板结构：`isThinking` 消息对象包含 `steps[]` 数组、`thinkingDone`、`thinkingCollapsed` 状态
- 执行中：显示旋转 loading + "深度思考中..."
- 完成后：自动折叠，显示勾选 + "深度思考已完成" + 步骤数，点击可展开/收起
- 最终结果：从步骤中反向查找有意义内容（跳过 doTerminate/任务结束等标记），提取为独立的 `isFinalResult` 气泡
- CSS 类：`.thinking-bubble`、`.thinking-header`、`.thinking-steps`、`.thinking-step-item`、`.final-result-bubble`

### API

- [src/api/http.js](src/api/http.js) — Axios instance with `baseURL` + `getApiUrl` helper for constructing SSE URLs with query params
  - Uses `json-bigint` (`storeAsString: true`) in `transformResponse` to handle Long ID precision loss (snowflake IDs exceed JS `Number.MAX_SAFE_INTEGER`)

### Markdown Rendering

AI message bubbles use the `marked` library for Markdown → HTML rendering:

- **Import**: `import { marked } from 'marked'` in ChatRoom.vue
- **Config**: `breaks: true`, `gfm: true` (GitHub Flavored Markdown)
- **Preprocessing**: Literal `\n` sequences normalized to real newlines before parsing
- **Template**: AI bubbles use `v-html="renderMarkdown(item.content)"`, user bubbles remain plain text `{{ item.content }}`
- **Styles**: `.markdown-body` class provides full dark-theme styling for headings, lists, code blocks, tables, blockquotes, links, etc.
- [src/api/user.js](src/api/user.js) — User API functions:
  - `userRegister` — POST `/user/register`
  - `userLogin` — POST `/user/login`
  - `getCurrentUser` — GET `/user/get/current`
  - `userLogout` — POST `/user/logout`
  - `sendCode` — POST `/user/send_code`
  - `userLoginByEmail` — POST `/user/login/email`
  - `GlobalUpdateUser` — POST `/user/global/update` (用户自主修改个人信息，通用接口)
  - `uploadAvatar` — POST `/user/upload/avatar` (multipart/form-data，用户上传头像到腾讯云 COS)
  - `addUser` — POST `/user/add` (管理员创建用户，默认密码 12345678)
  - `getUserById` — GET `/user/get?id=xxx` (管理员根据 ID 查询用户)
  - `deleteUser` — POST `/user/delete` (管理员删除用户)
  - `updateUser` — POST `/user/update` (管理员修改用户信息)
  - `listUserByPage` — POST `/user/list/page/vo` (管理员分页查询用户列表)

### User Profile (NavBar)

- [src/components/NavBar.vue](src/components/NavBar.vue) — Navigation bar with user dropdown and profile modal
  - "操作文档"按钮：始终显示，跳转 `/docs`，玻璃态胶囊样式
  - Profile modal supports display mode and edit mode
  - Edit calls `GlobalUpdateUser` with `{ id, userName, userEmail, userProfile }`
  - Avatar upload: hover 头像显示相机遮罩层，点击触发文件选择，上传到 COS 后刷新用户信息
  - Admin dropdown: 当 `currentUser.userRole === 'admin'` 时显示"用户管理"菜单项，跳转 `/admin/users`
  - Uses `useUser()` composable from [src/composables/useUser.js](src/composables/useUser.js)

### Important: Backend API Path Mapping

Backend `UserController` has class-level `@RequestMapping("/user")`, combined with `context-path: /api`:

| 接口 | 方法注解 | 完整路径 | 权限 |
|------|----------|----------|------|
| 管理员修改用户 | `@PostMapping("/update")` | `/api/user/update` | 管理员 |
| 用户修改个人信息 | `@PostMapping("/global/update")` | `/api/user/global/update` | 通用 |

前端编辑个人信息必须使用 `/user/global/update`（通用接口），不要使用 `/user/update`（管理员接口）。

### Known Issue Fixed: Long ID Precision

后端 User ID 使用雪花算法（`IdType.ASSIGN_ID`，19位 Long），JavaScript `Number` 最大安全整数为 `2^53-1`（16位）。通过 `json-bigint` 的 `storeAsString: true` 配置，axios 响应中超出安全范围的大整数自动转为字符串，确保 ID 精确传递。

### Known Issue Fixed: 注册邮箱选填空字符串

前端注册表单邮箱字段初始值为 `""`（空字符串），用户不填时发送给后端的是 `""`。后端原本只判断 `userEmail != null`，空字符串会进入邮箱格式校验导致报错。修复：改用 `StrUtil.isNotBlank(userEmail)` 判断，空字符串和 null 均跳过邮箱校验；保存数据库时空字符串转为 null。

**修改文件**: `service/impl/UserServiceImpl.java#userRegister`

### Avatar Upload (腾讯云 COS)

用户头像上传完整链路：

- **前端**: NavBar profile modal 中 hover 头像 → 显示相机遮罩 → 点击触发 `<input type="file">` → 前端校验（类型/大小 ≤ 2MB）→ 调用 `uploadAvatar` API（multipart/form-data）
- **后端**: `POST /api/user/upload/avatar` → 校验登录态 + 文件校验 → 上传到腾讯云 COS（`avatar/{userId}/{uuid}.{ext}`）→ 更新 DB `userAvatar` 字段 → 返回 URL
- **后端关键文件**:
  - `config/CosClientConfig.java` — COS 客户端配置（读取 `cos.client.*`）
  - `manager/CosManager.java` — COS 上传/删除管理器（`uploadFile`、`deleteFile`、`getHost`）
  - `controller/UserController.java#uploadAvatar` — 头像上传接口（先上传新头像，再删除旧头像 COS 资源）
- **COS 配置位于**: `application-local.yml`（`cos.client.*`，含敏感密钥，勿暴露）

### Styles

- [src/style.css](src/style.css) — All CSS in one file. Dark tech theme with:
  - Aurora gradient animations, star-dot textures, scan-line grid overlays
  - Glassmorphism panels with backdrop-filter blur
  - Message entrance animations, card sweep hover effects
  - Sparkle stars background animation (40 twinkling dots on homepage)
  - Home page tag badges with shimmer + float animation
  - Thinking panel styles (collapsible step container for Manus agent)
  - Markdown body styles (`.markdown-body` — headings, lists, code blocks, tables, blockquotes, links in dark theme)
  - Docs page layout (sidebar + main content, code blocks, tables)
  - Responsive breakpoints at 1024px and 768px
  - `prefers-reduced-motion` support

### Key Props on ChatRoom

| Prop | Type | Default | Purpose |
|------|------|---------|---------|
| `title` | String | required | Page title display |
| `ssePath` | String | required | Backend SSE endpoint path |
| `useChatId` | Boolean | false | Whether to send `chatId` param |
| `chatId` | String | '' | Session ID for the backend |
| `stepBubbleMode` | Boolean | false | Collect steps into thinking panel |
| `sessionTitle` | String | '默认会话' | Session title in header |
| `sessionId` | String | '' | Display session ID in header |
| `aiName` | String | 'Neko AI' | AI sender name |
| `aiAvatar` | String | 'NA' | AI avatar initials — selects SVG icon |

#### Avatar Code → Icon Mapping

`aiAvatar` drives which SVG icon renders in the AI message bubble:

| Code | Icon | Used by |
|------|------|---------|
| `NL` | Heart | LoveChatPage |
| `NM` | Robot | ManusChatPage |
| `NP` | Cat | PetChatPage |
| other | Smiley face | Default fallback |
