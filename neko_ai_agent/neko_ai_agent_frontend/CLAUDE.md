# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Vue 3 + Vite frontend for **Neko AI Agent**, a dual-AI-agent platform with SSE streaming chat.

- **Home** (`/`): App switching page with three AI agent cards
- **AI 恋爱大师** (`/love`): Chat with `chatId` session support, single AI bubble per turn
- **AI 宠物专家** (`/pet`): Chat with `chatId` session support, single AI bubble per turn
- **AI 超级智能体** (`/manus`): Step-based chat, each step gets its own AI bubble

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
- [src/router/index.js](src/router/index.js) — Four routes with meta (title, description); `router.afterEach` updates `document.title` and `<meta name="description">`

### Page Structure

- [src/App.vue](src/App.vue) — Root shell with `<router-view>` + site footer
- [src/views/HomePage.vue](src/views/HomePage.vue) — Home page with three app card entries
- [src/views/LoveChatPage.vue](src/views/LoveChatPage.vue) — Thin wrapper around ChatRoom with `chatId` session
  - Session ID: `crypto.randomUUID()` (standard UUID format)
- [src/views/PetChatPage.vue](src/views/PetChatPage.vue) — Thin wrapper around ChatRoom with `chatId` session
  - Session ID: `crypto.randomUUID()` (standard UUID format)
- [src/views/ManusChatPage.vue](src/views/ManusChatPage.vue) — Thin wrapper around ChatRoom with `stepBubbleMode: true`
  - Session ID: `MANUS-${Date.now()}` (timestamp-based format)
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

#### Step Bubble Mode (Manus)

- Each SSE message creates a new AI bubble
- Duplicate detection: compares `chunk.trim()` with `lastStepText` to prevent repeated bubbles
- Auto-adds extra newline after each step for visual separation

### API

- [src/api/http.js](src/api/http.js) — Axios instance with `baseURL` + `getApiUrl` helper for constructing SSE URLs with query params

### Styles

- [src/style.css](src/style.css) — All CSS in one file. Dark tech theme with:
  - Aurora gradient animations, star-dot textures, scan-line grid overlays
  - Glassmorphism panels with backdrop-filter blur
  - Message entrance animations, card sweep hover effects
  - Responsive breakpoints at 1024px and 768px
  - `prefers-reduced-motion` support

### Key Props on ChatRoom

| Prop | Type | Default | Purpose |
|------|------|---------|---------|
| `title` | String | required | Page title display |
| `ssePath` | String | required | Backend SSE endpoint path |
| `useChatId` | Boolean | false | Whether to send `chatId` param |
| `chatId` | String | '' | Session ID for the backend |
| `stepBubbleMode` | Boolean | false | Each step = separate AI bubble |
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
