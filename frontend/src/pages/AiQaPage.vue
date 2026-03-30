<template>
  <div class="space-y-4">
    <!-- 顶部信息栏 -->
    <div class="flex items-center justify-between">
      <div>
        <p class="font-code text-[10px] uppercase tracking-[0.4em] text-cyan-400/40">// AI_KNOWLEDGE_BASE</p>
        <h1 class="mt-1 font-display text-lg uppercase tracking-[0.3em] text-cyan-100/90">智能知识库问答</h1>
      </div>
      <div class="flex items-center gap-3">
        <span class="font-code text-[10px] text-slate-600">{{ sessions.length }} 个会话</span>
        <div class="flex items-center gap-2">
          <span class="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-400"></span>
          <span class="font-code text-[10px] uppercase tracking-widest text-emerald-400/70">RAG · ONLINE</span>
        </div>
      </div>
    </div>

    <!-- 主体 -->
    <div class="grid gap-4 xl:grid-cols-[200px_1fr_220px]">

      <!-- 左侧：会话列表 -->
      <aside class="flex flex-col gap-2">
        <button
          class="flex items-center gap-2 border border-cyan-300/20 bg-cyan-400/8 px-3 py-2.5 text-xs text-cyan-300 transition hover:border-cyan-300/40 hover:bg-cyan-400/15"
          @click="newSession"
        >
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 4v16m8-8H4"/></svg>
          <span class="font-code tracking-wider">NEW SESSION</span>
        </button>
        <div class="flex flex-col gap-1 overflow-y-auto" style="max-height: calc(100vh - 280px)">
          <div
            v-for="s in sessions"
            :key="s.id"
            :class="s.id === activeSessionId
              ? 'border-cyan-300/25 bg-cyan-400/8 text-white'
              : 'border-transparent text-slate-500 hover:border-cyan-300/10 hover:bg-slate-900/50 hover:text-slate-300'"
            class="group relative cursor-pointer border px-3 py-2.5 transition"
            @click="switchSession(s.id)"
          >
            <p class="truncate text-xs font-medium">{{ s.title }}</p>
            <p class="font-code mt-0.5 text-[10px] text-slate-600">{{ s.messages.length }} 条 · {{ formatSessionTime(s.updatedAt) }}</p>
            <button
              class="absolute right-2 top-1/2 -translate-y-1/2 text-slate-700 opacity-0 transition hover:text-rose-400 group-hover:opacity-100"
              @click.stop="deleteSession(s.id)"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12"/></svg>
            </button>
          </div>
          <div v-if="!sessions.length" class="px-3 py-4 text-center">
            <p class="font-code text-[10px] text-slate-700">// NO SESSIONS</p>
          </div>
        </div>
      </aside>

      <!-- 中间：对话区域 -->
      <div class="panel relative flex flex-col overflow-hidden" style="height: calc(100vh - 220px); min-height: 500px;">
        <div class="h-px w-full bg-gradient-to-r from-transparent via-cyan-400/30 to-transparent"></div>
        <div v-if="activeSession" class="flex items-center justify-between border-b border-cyan-300/8 px-5 py-2.5">
          <span class="font-code text-[10px] text-slate-500">{{ activeSession.title }}</span>
          <span class="font-code text-[10px] text-slate-700">{{ currentMessages.length }} msgs</span>
        </div>
        <div ref="msgListRef" class="flex-1 overflow-y-auto px-6 py-5 space-y-6 scroll-smooth">
          <!-- 空状态 -->
          <div v-if="!currentMessages.length" class="flex h-full flex-col items-center justify-center gap-6 py-16 text-center">
            <div class="relative">
              <div class="flex h-16 w-16 items-center justify-center border border-cyan-300/20 bg-gradient-to-br from-cyan-400/15 to-blue-500/10">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="rgba(88,216,255,0.8)" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M12 8v4m0 4h.01"/></svg>
              </div>
              <span class="absolute -right-1 -top-1 h-3 w-3 animate-ping rounded-full bg-cyan-400/40"></span>
              <span class="absolute -right-1 -top-1 h-3 w-3 rounded-full bg-cyan-400/60"></span>
            </div>
            <div class="space-y-2">
              <p class="text-base font-semibold text-white">知识库已就绪</p>
              <p class="max-w-xs text-sm text-slate-500 leading-6">基于课程全量文档构建，支持 RAG 检索增强回答</p>
            </div>
            <div class="grid grid-cols-2 gap-2 w-full max-w-md">
              <button v-for="q in suggested.slice(0,4)" :key="q"
                class="group border border-cyan-300/10 bg-slate-950/50 px-3 py-2.5 text-left text-xs text-slate-400 transition hover:border-cyan-300/25 hover:bg-cyan-400/5 hover:text-slate-200"
                @click="askSuggested(q)"
              >
                <span class="text-cyan-400/40 group-hover:text-cyan-400/70 mr-1">›</span>{{ q }}
              </button>
            </div>
          </div>
          <!-- 消息列表 -->
          <template v-else>
            <div v-for="(msg, idx) in currentMessages" :key="idx"
              :class="msg.role === 'user' ? 'flex-row-reverse' : 'flex-row'"
              class="flex items-start gap-3"
            >
              <div class="shrink-0 mt-0.5">
                <div v-if="msg.role === 'user'" class="flex h-7 w-7 items-center justify-center border border-slate-600/50 bg-slate-800 font-code text-[10px] text-slate-400">YOU</div>
                <div v-else class="flex h-7 w-7 items-center justify-center border border-cyan-300/25 bg-gradient-to-br from-cyan-400/20 to-blue-500/10">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="rgba(88,216,255,0.8)" stroke-width="2"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
                </div>
              </div>
              <div :class="msg.role === 'user' ? 'items-end' : 'items-start'" class="flex max-w-[80%] flex-col gap-1">
                <span class="font-code text-[10px] uppercase tracking-widest" :class="msg.role === 'user' ? 'text-slate-500' : 'text-cyan-400/50'">{{ msg.role === 'user' ? 'You' : 'AI · KB' }}</span>
                <div :class="msg.role === 'user' ? 'border-slate-700/60 bg-slate-800/80 text-slate-100' : 'border-cyan-300/10 bg-slate-950/70 text-slate-200'" class="border px-4 py-3 text-sm leading-7 whitespace-pre-wrap">
                  {{ msg.content }}<span v-if="msg.streaming" class="ml-0.5 inline-block h-[1em] w-0.5 animate-pulse bg-cyan-400 align-middle"></span>
                </div>
              </div>
            </div>
          </template>
        </div>
        <!-- 输入区 -->
        <div class="border-t border-cyan-300/8 bg-slate-950/80 p-4">
          <div class="flex items-end gap-3">
            <div class="flex-1 border border-cyan-300/15 bg-slate-900/60 transition focus-within:border-cyan-300/35">
              <textarea v-model="inputText" :disabled="loading" placeholder="向知识库提问... (Enter 发送，Shift+Enter 换行)" rows="3"
                class="w-full resize-none bg-transparent px-4 py-3 text-sm text-slate-100 placeholder-slate-600 outline-none disabled:opacity-40"
                @keydown.enter.exact.prevent="sendMessage"
              />
              <div class="flex items-center justify-between border-t border-cyan-300/8 px-4 py-2">
                <span class="font-code text-[10px] text-slate-600">Enter ↵ 发送 · Shift+Enter 换行</span>
                <span class="font-code text-[10px]" :class="inputText.length > 400 ? 'text-rose-400' : 'text-slate-700'">{{ inputText.length }}/500</span>
              </div>
            </div>
            <!-- 停止按钮（流式输出中显示） -->
            <button v-if="loading && currentRequestId"
              class="flex h-full min-h-[76px] w-12 flex-col items-center justify-center gap-1.5 border border-rose-400/30 bg-rose-400/10 text-rose-300 transition hover:border-rose-400/50 hover:bg-rose-400/18 active:scale-95"
              @click="abortChat"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="1"/></svg>
              <span class="font-code text-[9px] uppercase tracking-wider">Stop</span>
            </button>
            <!-- 发送按钮 -->
            <button v-else :disabled="loading || !inputText.trim()"
              class="flex h-full min-h-[76px] w-12 flex-col items-center justify-center gap-1.5 border border-cyan-300/20 bg-cyan-400/10 text-cyan-300 transition hover:border-cyan-300/40 hover:bg-cyan-400/18 disabled:cursor-not-allowed disabled:opacity-30 active:scale-95"
              @click="sendMessage"
            >
              <span v-if="loading" class="h-4 w-4 animate-spin rounded-full border-2 border-cyan-300/20 border-t-cyan-300"></span>
              <template v-else>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 19V5M5 12l7-7 7 7"/></svg>
                <span class="font-code text-[9px] uppercase tracking-wider">Send</span>
              </template>
            </button>
          </div>
        </div>
      </div>

      <!-- 右侧面板 -->
      <aside class="space-y-3">
        <div class="panel p-4 space-y-3">
          <p class="font-code text-[10px] uppercase tracking-[0.3em] text-cyan-400/40">// SESSION</p>
          <div class="flex items-center justify-between text-xs"><span class="text-slate-500">消息数</span><span class="font-code text-cyan-300/70">{{ currentMessages.length }}</span></div>
          <div class="flex items-center justify-between text-xs">
            <span class="text-slate-500">状态</span>
            <span class="font-code flex items-center gap-1.5" :class="loading ? 'text-yellow-400/70' : 'text-emerald-400/70'">
              <span class="h-1 w-1 rounded-full" :class="loading ? 'bg-yellow-400 animate-pulse' : 'bg-emerald-400'"></span>
              {{ loading ? 'THINKING' : 'READY' }}
            </span>
          </div>
        </div>
        <div class="panel p-4 space-y-2">
          <p class="font-code text-[10px] uppercase tracking-[0.3em] text-cyan-400/40">// QUICK_ASK</p>
          <div class="space-y-1 pt-1">
            <button v-for="q in suggested" :key="q" :disabled="loading"
              class="group w-full border border-transparent px-3 py-2 text-left transition hover:border-cyan-300/15 hover:bg-cyan-400/5 disabled:opacity-30"
              @click="askSuggested(q)">
              <span class="font-code text-[10px] text-cyan-400/30 group-hover:text-cyan-400/60">›› </span>
              <span class="text-xs text-slate-500 group-hover:text-slate-300">{{ q }}</span>
            </button>
          </div>
        </div>
        <div class="panel p-4 space-y-2">
          <p class="font-code text-[10px] uppercase tracking-[0.3em] text-cyan-400/40">// MODEL_INFO</p>
          <div class="space-y-1.5 pt-1">
            <div v-for="info in modelInfo" :key="info.k" class="flex items-center justify-between">
              <span class="font-code text-[10px] text-slate-600">{{ info.k }}</span>
              <span class="font-code text-[10px] text-slate-400">{{ info.v }}</span>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>
<script setup lang="ts">
import { api } from '@/services/api'
import { computed, nextTick, ref, watch } from 'vue'

type Msg = { role: 'user' | 'ai'; content: string; streaming?: boolean }
type Session = { id: string; title: string; messages: Msg[]; updatedAt: number }

const STORAGE_KEY = 'ai-qa-sessions'

// 从 localStorage 恢复会话列表
function loadSessions(): Session[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch { return [] }
}

function saveSessions() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions.value))
}

const sessions = ref<Session[]>(loadSessions())
const activeSessionId = ref<string>(sessions.value[0]?.id ?? '')
const inputText = ref('')
const loading = ref(false)
const msgListRef = ref<HTMLElement | null>(null)

const activeSession = computed(() => sessions.value.find(s => s.id === activeSessionId.value))
const currentMessages = computed(() => activeSession.value?.messages ?? [])

/** 当前流式请求的 ID，用于发起中断 */
const currentRequestId = ref<string | null>(null)

const suggested = [
  '什么是 RAG？',
  'Prompt 工程最佳实践？',
  'Agent vs Workflow？',
  'Function Call 如何用？',
  'MCP 协议解决什么？',
  '如何搭建知识库系统？',
]

const modelInfo = [
  { k: 'PROVIDER', v: 'Coze' },
  { k: 'TYPE', v: 'RAG Bot' },
  { k: 'STREAM', v: 'SSE' },
  { k: 'LATENCY', v: '~5-15s' },
]

// 自动保存
watch(sessions, saveSessions, { deep: true })

function newSession() {
  const id = Date.now().toString()
  sessions.value.unshift({ id, title: '新会话', messages: [], updatedAt: Date.now() })
  activeSessionId.value = id
}

function switchSession(id: string) {
  activeSessionId.value = id
  scrollToBottom()
}

function deleteSession(id: string) {
  sessions.value = sessions.value.filter(s => s.id !== id)
  if (activeSessionId.value === id) {
    activeSessionId.value = sessions.value[0]?.id ?? ''
  }
}

// 用第一条用户消息作为会话标题
function updateSessionTitle(session: Session, question: string) {
  if (session.title === '新会话') {
    session.title = question.slice(0, 20) + (question.length > 20 ? '...' : '')
  }
}

async function scrollToBottom() {
  await nextTick()
  if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight
}

async function sendMessage() {
  const q = inputText.value.trim()
  if (!q || loading.value) return

  // 没有会话时自动新建
  if (!activeSession.value) newSession()
  const session = activeSession.value!

  inputText.value = ''
  loading.value = true
  session.messages.push({ role: 'user', content: q })
  session.updatedAt = Date.now()
  updateSessionTitle(session, q)

  const idx = session.messages.length
  session.messages.push({ role: 'ai', content: '', streaming: true })
  await scrollToBottom()

  try {
    await api.cozeChatStream(
      q,
      (chunk) => {
        session.messages[idx] = { ...session.messages[idx], content: session.messages[idx].content + chunk }
        scrollToBottom()
      },
      () => {
        session.messages[idx] = { ...session.messages[idx], streaming: false }
        session.updatedAt = Date.now()
        loading.value = false
        currentRequestId.value = null
        scrollToBottom()
      },
      (err) => {
        session.messages[idx] = { ...session.messages[idx], content: err || '请求失败，请稍后重试', streaming: false }
        loading.value = false
        currentRequestId.value = null
      },
      () => {
        // 被中断
        session.messages[idx] = { ...session.messages[idx], content: session.messages[idx].content + ' _(已停止)_', streaming: false }
        loading.value = false
        currentRequestId.value = null
      },
      // onRequestId：响应头一到立即记录，此时流还未开始，可用于中断
      (id) => { currentRequestId.value = id },
    )
  } catch (e) {
    session.messages[idx] = { ...session.messages[idx], content: e instanceof Error ? e.message : '请求失败', streaming: false }
    loading.value = false
    currentRequestId.value = null
  }
}

async function abortChat() {
  if (!currentRequestId.value) return
  await api.cozeAbort(currentRequestId.value)
  currentRequestId.value = null
  loading.value = false
}

function askSuggested(q: string) { inputText.value = q; sendMessage() }

function formatSessionTime(ts: number): string {
  const d = new Date(ts)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

// 初始化：没有会话时创建一个默认会话
if (!sessions.value.length) newSession()
</script>
