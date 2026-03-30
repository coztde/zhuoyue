<template>
  <div class="space-y-5">
    <!-- Hero 区域 -->
    <div class="relative overflow-hidden border border-cyan-300/10 bg-slate-950/55 px-6 py-8 backdrop-blur-xl">
      <div class="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-cyan-300/30 to-transparent"></div>
      <div class="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-cyan-400/8 blur-3xl"></div>
      <div class="relative flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p class="mb-2 font-display text-xs uppercase tracking-[0.45em] text-cyan-200/55">社区</p>
          <h1 class="font-display text-2xl uppercase tracking-[0.3em] text-cyan-100/90">Wiki 求助</h1>
          <p class="mt-1.5 text-sm text-slate-400">任何人都可以发布文章或提问，共同维护知识库</p>
        </div>
        <button class="inline-flex shrink-0 items-center gap-2 border border-cyan-300/25 bg-cyan-400/12 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-cyan-400/20 active:scale-95" @click="openEditor()">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M12 4v16m8-8H4"/></svg>
          发布文章
        </button>
      </div>
      <!-- 标签筛选 -->
      <div class="mt-5 flex flex-wrap gap-2">
        <button v-for="t in allTags" :key="t"
          class="inline-flex items-center gap-1.5 rounded-sm border px-3 py-1 text-xs font-medium transition"
          :class="activeTag === t ? 'border-cyan-300/40 bg-cyan-400/15 text-white' : 'border-cyan-300/10 text-slate-400 hover:border-cyan-300/25 hover:text-white'"
          @click="setTag(t)">
          <span v-if="t !== '全部'" class="h-1.5 w-1.5 rounded-full" :class="tagDot(t)"></span>
          {{ t }}
        </button>
      </div>
      <!-- 搜索+排序 -->
      <div class="mt-3 flex flex-wrap gap-2">
        <div class="relative flex-1 min-w-48">
          <svg class="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
          <input v-model="keyword" placeholder="搜索标题或内容..." class="w-full border border-cyan-300/10 bg-slate-950/60 py-2 pl-9 pr-3 text-sm text-white outline-none placeholder:text-slate-500 focus:border-cyan-300/25" @keyup.enter="doSearch" />
        </div>
        <div class="flex overflow-hidden border border-cyan-300/10">
          <button class="px-4 py-2 text-xs font-medium transition" :class="sort === 'heat' ? 'bg-cyan-400/15 text-white' : 'text-slate-400 hover:text-white'" @click="setSort('heat')">🔥 最热</button>
          <button class="border-l border-cyan-300/10 px-4 py-2 text-xs font-medium transition" :class="sort === 'new' ? 'bg-cyan-400/15 text-white' : 'text-slate-400 hover:text-white'" @click="setSort('new')">🕐 最新</button>
        </div>
      </div>
    </div>

    <!-- 加载骨架 -->
    <div v-if="loading" class="grid gap-2">
      <div v-for="i in 5" :key="i" class="h-20 animate-pulse border border-cyan-300/5 bg-slate-950/40"></div>
    </div>

    <!-- 空态 -->
    <div v-else-if="posts.length === 0" class="flex flex-col items-center gap-3 border border-dashed border-cyan-300/15 bg-slate-950/35 py-16 text-center">
      <svg class="h-10 w-10 text-slate-600" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"/></svg>
      <p class="text-slate-400">暂无文章</p>
      <button class="border border-cyan-300/20 bg-cyan-400/10 px-4 py-2 text-sm text-cyan-300 hover:bg-cyan-400/15" @click="openEditor()">来发第一篇</button>
    </div>

    <!-- 文章列表 -->
    <div v-else class="grid gap-2">
      <div v-for="post in posts" :key="post.id"
        class="group relative cursor-pointer border border-cyan-300/8 bg-slate-950/40 px-5 py-4 transition hover:border-cyan-300/20 hover:bg-slate-950/60"
        @click="router.push({ name: 'wiki-post', params: { id: post.id } })">
        <div class="absolute inset-y-0 left-0 w-0.5 opacity-0 transition group-hover:opacity-100" :class="tagAccent(post.tag)"></div>
        <div class="flex items-start gap-4">
          <div class="flex-1 min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <span class="inline-flex items-center gap-1 rounded-sm border px-2 py-0.5 text-xs" :class="tagClass(post.tag)">
                <span class="h-1 w-1 rounded-full" :class="tagDot(post.tag)"></span>
                {{ post.tag }}
              </span>
              <h3 class="text-sm font-semibold text-white">{{ post.title }}</h3>
            </div>
            <p class="mt-1.5 text-xs leading-5 text-slate-500 line-clamp-2">{{ stripMarkdown(post.content) }}</p>
            <div class="mt-2.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-600">
              <span class="flex items-center gap-1 text-slate-500">
                <svg class="h-3 w-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                {{ post.authorName }}
              </span>
              <span>{{ formatTime(post.createdAt) }}</span>
              <span class="flex items-center gap-1" :class="post.liked ? 'text-cyan-400' : ''">
                <svg class="h-3 w-3" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24"><path d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5"/></svg>
                {{ post.likeCount }}
              </span>
              <span class="flex items-center gap-1">
                <svg class="h-3 w-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
                {{ post.commentCount }}
              </span>
              <span v-if="sort === 'heat'" class="text-cyan-400/50">热度 {{ post.heatScore.toFixed(1) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-1">
      <button v-for="p in totalPages" :key="p"
        class="h-7 min-w-7 px-2 border text-xs transition"
        :class="p === currentPage ? 'border-cyan-300/40 bg-cyan-400/12 text-white' : 'border-cyan-300/8 text-slate-500 hover:text-white'"
        @click="goPage(p)">{{ p }}</button>
    </div>

    <!-- 编辑器弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="editorVisible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm px-4" @click.self="closeEditor()">
          <div class="w-full max-w-3xl border border-cyan-300/12 bg-slate-900 shadow-2xl">
            <div class="flex items-center justify-between border-b border-cyan-300/10 px-5 py-4">
              <div class="flex items-center gap-2">
                <div class="h-2 w-2 rounded-full bg-cyan-400/70"></div>
                <h2 class="text-xs font-semibold uppercase tracking-widest text-slate-300">{{ editingPost ? '编辑文章' : '发布文章' }}</h2>
              </div>
              <button class="flex h-7 w-7 items-center justify-center text-slate-500 hover:text-white" @click="closeEditor()">
                <svg class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="p-5 space-y-3">
              <input v-model="editorTitle" placeholder="文章标题..."
                class="w-full border-0 border-b border-cyan-300/10 bg-transparent pb-2.5 text-xl font-semibold text-white outline-none placeholder:text-slate-600 focus:border-cyan-300/30" />
              <div class="flex gap-2">
                <div class="relative flex-1">
                  <svg class="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  <input v-model="editorAuthor" placeholder="你的昵称（可选）" class="w-full border border-cyan-300/10 bg-slate-950/60 py-1.5 pl-8 pr-3 text-sm text-white outline-none placeholder:text-slate-500 focus:border-cyan-300/25" />
                </div>
                <select v-model="editorTag" class="border border-cyan-300/10 bg-slate-950/60 px-3 py-1.5 text-sm text-white outline-none focus:border-cyan-300/25">
                  <option v-for="t in postTags" :key="t" :value="t">{{ t }}</option>
                </select>
              </div>
              <MarkdownEditor v-model="editorContent" />
              <input v-if="editingPost" v-model="editorReason" placeholder="修改理由（选填，让其他人了解你的改动）"
                class="w-full border border-amber-400/15 bg-amber-400/5 px-3 py-2 text-sm text-slate-300 outline-none placeholder:text-slate-600 focus:border-amber-400/30" />
              <p v-if="editorError" class="text-xs text-rose-300">{{ editorError }}</p>
              <div class="flex gap-3 pt-1">
                <button :disabled="submittingPost" class="border border-cyan-300/25 bg-cyan-400/12 px-6 py-2 text-sm font-semibold text-white disabled:opacity-60 hover:bg-cyan-400/20 active:scale-95" @click="submitPost">{{ submittingPost ? '提交中...' : (editingPost ? '保存修改' : '发布') }}</button>
                <button class="border border-slate-700/50 px-5 py-2 text-sm text-slate-400 hover:text-white" @click="closeEditor()">取消</button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { api } from '@/services/api'
import { useGlobalNotice } from '@/composables/useGlobalNotice'
import type { WikiPost, WikiTag } from '@/types/models'

const router = useRouter()

const { showNotice } = useGlobalNotice()

function getOrCreateToken(): string {
  let token = localStorage.getItem('wiki-author-token')
  if (!token) { token = crypto.randomUUID(); localStorage.setItem('wiki-author-token', token) }
  return token
}
getOrCreateToken()

// ── 标签 ────────────────────────────────────────────────────
const allTags = ['全部', '文章', '问题', '分享', '公告'] as const
const postTags = ['文章', '问题', '分享', '公告'] as const
const activeTag = ref('全部')

function setTag(t: string) { activeTag.value = t; loadPosts(1) }

const TAG_CLASSES: Record<string, string> = {
  '文章': 'border-cyan-400/30 text-cyan-300',
  '问题': 'border-amber-400/30 text-amber-300',
  '分享': 'border-emerald-400/30 text-emerald-300',
  '公告': 'border-rose-400/30 text-rose-300',
}
const TAG_DOTS: Record<string, string> = {
  '文章': 'bg-cyan-400',
  '问题': 'bg-amber-400',
  '分享': 'bg-emerald-400',
  '公告': 'bg-rose-400',
}
const TAG_ACCENTS: Record<string, string> = {
  '文章': 'bg-cyan-400/60',
  '问题': 'bg-amber-400/60',
  '分享': 'bg-emerald-400/60',
  '公告': 'bg-rose-400/60',
}
function tagClass(tag: string) { return TAG_CLASSES[tag] ?? 'border-slate-400/30 text-slate-300' }
function tagDot(tag: string) { return TAG_DOTS[tag] ?? 'bg-slate-400' }
function tagAccent(tag: string) { return TAG_ACCENTS[tag] ?? 'bg-slate-400/60' }

// ── 列表 ────────────────────────────────────────────────────
const posts = ref<WikiPost[]>([])
const loading = ref(false)
const sort = ref('heat')
const keyword = ref('')
const currentPage = ref(1)
const totalPages = ref(1)

async function loadPosts(page = 1) {
  loading.value = true
  try {
    const tagParam = activeTag.value === '全部' ? undefined : activeTag.value
    const result = await api.getWikiPosts(page, 20, sort.value, keyword.value || undefined, tagParam)
    posts.value = result.records
    totalPages.value = result.pages
    currentPage.value = result.current
  } catch (e: any) {
    showNotice(e.message || '加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function setSort(s: string) { sort.value = s; loadPosts(1) }
function doSearch() { loadPosts(1) }
function goPage(p: number) { loadPosts(p) }
onMounted(() => loadPosts())

// ── 编辑器 ──────────────────────────────────────────────────
const editorVisible = ref(false)
const editorTitle = ref('')
const editorContent = ref('')
const editorAuthor = ref(localStorage.getItem('wiki-author-name') ?? '')
const editorTag = ref<WikiTag>('文章')
const editorReason = ref('')
const editorError = ref('')
const submittingPost = ref(false)
const editingPost = ref<WikiPost | null>(null)

function openEditor(post?: WikiPost) {
  editingPost.value = post ?? null
  editorTitle.value = post?.title ?? ''
  editorContent.value = post?.content ?? ''
  editorAuthor.value = post?.authorName ?? localStorage.getItem('wiki-author-name') ?? ''
  editorTag.value = (post?.tag ?? '文章') as WikiTag
  editorReason.value = ''
  editorError.value = ''
  editorVisible.value = true
}

async function closeEditor() {
  if (editingPost.value) {
    try { await api.unlockWikiPost(editingPost.value.id) } catch {}
  }
  editorVisible.value = false
}

async function submitPost() {
  editorError.value = ''
  if (!editorTitle.value.trim()) { editorError.value = '标题不能为空'; return }
  if (!editorContent.value.trim()) { editorError.value = '内容不能为空'; return }
  submittingPost.value = true
  try {
    const name = editorAuthor.value.trim()
    if (name) localStorage.setItem('wiki-author-name', name)
    if (editingPost.value) {
      const updated = await api.updateWikiPost(editingPost.value.id, editorTitle.value, editorContent.value, name, editingPost.value.version, editorTag.value, editorReason.value)
      showNotice('文章已更新', 'success')
      editorVisible.value = false
      router.push({ name: 'wiki-post', params: { id: updated.id } })
      return
    } else {
      await api.createWikiPost(editorTitle.value, editorContent.value, name, editorTag.value)
      showNotice('发布成功', 'success')
      await loadPosts(1)
    }
    editorVisible.value = false
  } catch (e: any) { editorError.value = e.message || '提交失败' }
  finally { submittingPost.value = false }
}


// ── 工具函数 ────────────────────────────────────────────────

function stripMarkdown(text: string): string {
  return text.replace(/[#*`>[\]_~]/g, '').replace(/\n+/g, ' ').substring(0, 120)
}

function formatTime(iso: string): string {
  const d = new Date(iso)
  const now = new Date()
  const diff = (now.getTime() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  if (diff < 86400 * 7) return `${Math.floor(diff / 86400)} 天前`
  return d.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.modal-fade-enter-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.modal-fade-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.97); }
</style>