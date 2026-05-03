# AI Agent Frontend (Vue3)

基于 Vue3 + Vite + Axios 实现的前端项目，包含首页应用切换与两个 SSE 聊天页面。

## 1. 项目目标

- 首页用于切换不同 AI 应用
- 页面 1：AI 恋爱大师（带 `chatId` 的 SSE 实时聊天）
- 页面 2：AI 超级智能体（SSE 实时聊天）

## 2. 技术栈

- Vue 3
- Vite
- Vue Router
- Axios

## 3. 后端接口

- 接口前缀：`http://localhost:8123/api`
- 恋爱大师（SSE）：`GET /ai/love_app/chat/sse?message=xxx&chatId=xxx`
- 超级智能体（SSE）：`GET /ai/manus/chat?message=xxx`

## 4. 页面说明

### 首页 `/`
- 展示两个应用入口
- 点击进入对应聊天页面
- 首页升级为极客科技风：暗色舞台背景 + 光环 + 玻璃质感卡片 + 扫光动效
- 使用 SVG 图标（恋爱心形 / N 字标），视觉更统一专业
- 支持 PC / 平板 / 手机响应式布局
- 背景升级为多层动态极光 + 星点纹理 + 纵横网格，视觉层次更丰富

### AI 恋爱大师 `/love`
- 聊天室布局：AI 左侧，用户右侧
- 页面初始化自动生成 `chatId`（`crypto.randomUUID()`）
- 发送消息后通过 SSE 流式渲染回复
- 在同一个 AI 气泡内持续拼接消息（打字机效果）
- 默认 AI 头像：`NL`
- AI 消息和用户消息的气泡内文字都为左对齐
- 支持手动“停止生成”
- 聊天区背景增加低对比扫描纹理与径向光晕，增强沉浸感

### AI 超级智能体 `/manus`
- 聊天室布局同上
- 发送消息后通过 SSE 流式渲染回复
- 每个 Step 单独使用一个 AI 气泡输出（Step 之间不合并）
- 每个 Step 在气泡内按打字机方式渲染
- 每次 SSE Step 消息会自动追加额外换行，增强步骤分隔感
- 默认 AI 头像：`NM`
- AI 消息和用户消息的气泡内文字都为左对齐
- 支持手动“停止生成”

## 4.1 SEO 与版权

- `index.html` 已补充基础 SEO：`description` / `keywords` / `robots` / Open Graph
- 路由级 SEO：不同页面会动态设置 `document.title` 与 `meta description`
- 全站底部已增加版权信息（仿照内容平台常见页脚样式）

## 5.1 SSE 处理细节

- 前端通过 `EventSource` 持续接收后端流式消息并实时渲染
- 自动识别常见结束标记：`[DONE]` / `DONE` / `done`
- 兼容 JSON 数据片段（自动提取 `delta` / `content` / `text`）
- 支持多种事件名：`message` / `token` / `delta` / `chunk` / `content` / `answer`
- 支持 `done` / `complete` 事件结束流
- 若连接关闭且已有内容，按正常结束处理，避免误报错误
- 前端使用打字机队列逐字渲染，确保单次大段返回也能呈现逐步输出效果
- 在超级智能体 Step 气泡模式下，会对连续重复 Step 文本进行防重，避免出现重复“思考完成”等气泡

## 5. 目录结构

```text
src/
  api/
    http.js               # Axios 实例与 URL 生成
  components/
    ChatRoom.vue          # 通用聊天组件（SSE 处理）
  router/
    index.js              # 路由配置
  views/
    HomePage.vue          # 首页
    LoveChatPage.vue      # AI 恋爱大师页面
    ManusChatPage.vue     # AI 超级智能体页面
  App.vue
  main.js
  style.css
```

## 6. 本地运行（Windows）

在 PowerShell 或 CMD 中执行：

```powershell
npm install
npm run dev
```

打开 Vite 输出地址（默认一般为 `http://localhost:5173`）。

## 7. 生产构建

```powershell
npm run build
```

## 8. 注意事项

- 需确保后端服务已启动并可访问：`http://localhost:8123`
- 浏览器需支持 `EventSource`（现代浏览器均支持）
- 若跨域受限，请在后端配置 CORS

## 9. 可扩展建议

- 按 `chatId` 持久化会话记录（LocalStorage / IndexedDB）
- 增加“重新生成”按钮
- 增加 SSE 断线重连与错误状态提示
