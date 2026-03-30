<template>
  <div v-if="loading" class="py-24 text-center text-slate-500 text-sm">加载中...</div>
  <div v-else-if="!post" class="py-24 text-center text-slate-500 text-sm">文章不存在或已被删除</div>
  <div v-else class="mx-auto max-w-3xl space-y-6">
    <div>
      <RouterLink to="/wiki-help" class="inline-flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-300 transition">
        <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M19 12H5m7-7-7 7 7 7"/></svg>
        返回 Wiki
      </RouterLink>
    </div>
    <div class="border border-cyan-300/10 bg-slate-950/55 px-8 py-7">
      <div class="flex items-center gap-2 mb-3">
        <span class="inline-flex items-center gap-1 rounded-sm border px-2 py-0.5 text-xs" :class="tagClass(post.tag)">
          <span class="h-1 w-1 rounded-full" :class="tagDot(post.tag)"></span>
          {{ post.tag }}
        </span>
      </div>
      <h1 class="text-2xl font-bold text-white leading-snug">{{ post.title }}</h1>
      <div class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
        <span class="flex items-center gap-1"><svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg><span class="text-slate-300">{{ post.authorName }}</span></span>
        <span>{{ formatTime(post.createdAt) }}</span>
        <span v-if="post.updatedAt !== post.createdAt" class="text-slate-600">· 编辑于 {{ formatTime(post.updatedAt) }}<template v-if="post.editReason">：{{ post.editReason }}</template></span>
      </div>
    </div>
    <div class="border border-cyan-300/8 bg-slate-950/40 px-8 py-7">
      <MarkdownEditor :model-value="post.content" :readonly="true" />
      <div class="mt-6 flex flex-wrap items-center gap-2 border-t border-cyan-300/8 pt-4">
        <button class="inline-flex items-center gap-1.5 border px-3 py-1.5 text-xs font-medium transition" :class="post.liked ? 'border-cyan-300/40 bg-cyan-400/12 text-cyan-300' : 'border-cyan-300/10 text-slate-400 hover:text-white'" @click="toggleLike">
          <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24"><path d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5"/></svg>
          {{ post.likeCount }} 点赞
        </button>
        <button class="inline-flex items-center gap-1.5 border px-3 py-1.5 text-xs transition" :class="post.isEditing ? 'border-amber-400/20 text-amber-400/70 cursor-not-allowed' : 'border-cyan-300/10 text-slate-400 hover:text-white'" @click="handleEditClick">
          <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          {{ post.isEditing ? '编辑中...' : '编辑' }}
        </button>
        <button class="ml-auto inline-flex items-center gap-1.5 border border-rose-400/15 px-3 py-1.5 text-xs text-rose-400/60 hover:text-rose-400" @click="reportVisible = true">
          <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>
          举报
        </button>
        <button v-if="isAdmin" class="inline-flex items-center gap-1.5 border border-rose-500/30 bg-rose-500/10 px-3 py-1.5 text-xs text-rose-400 hover:bg-rose-500/20" @click="adminDelete">
          <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"/></svg>
          删除文章
        </button>
      </div>
    </div>
    <div class="border border-cyan-300/10 bg-slate-950/55">
      <div class="border-b border-cyan-300/8 px-6 py-4"><span class="text-sm font-semibold text-slate-300">{{ post.commentCount }} 条评论</span></div>
      <div class="border-b border-cyan-300/8 px-6 py-4 space-y-2">
        <div class="flex gap-2">
          <input v-model="commentAuthorName" placeholder="昵称（可选）" class="w-28 shrink-0 border border-cyan-300/10 bg-slate-950/60 px-2 py-1.5 text-xs text-white outline-none placeholder:text-slate-600 focus:border-cyan-300/20" />
          <textarea v-model="commentContent" placeholder="写下你的评论..." rows="2" class="flex-1 border border-cyan-300/10 bg-slate-950/60 px-3 py-1.5 text-xs text-white outline-none placeholder:text-slate-600 resize-none focus:border-cyan-300/20"></textarea>
          <button :disabled="submittingComment" class="shrink-0 self-end border border-cyan-300/20 bg-cyan-400/12 px-3 py-1.5 text-xs text-white disabled:opacity-60 hover:bg-cyan-400/20" @click="submitComment()">{{ submittingComment ? '...' : '发表' }}</button>
        </div>
      </div>
      <div v-if="replyTo" class="border-b border-cyan-300/8 bg-slate-950/40 px-6 py-3 space-y-2">
        <div class="flex items-center justify-between text-xs"><span class="text-slate-400">回复 <span class="text-white">{{ replyTo.authorName }}</span></span><button class="text-slate-500 hover:text-white" @click="replyTo = null">取消</button></div>
        <div class="flex gap-2">
          <textarea v-model="replyContent" placeholder="写下回复..." rows="2" class="flex-1 border border-cyan-300/10 bg-slate-950/60 px-3 py-1.5 text-xs text-white outline-none placeholder:text-slate-600 resize-none"></textarea>
          <button :disabled="submittingReply" class="shrink-0 self-end border border-cyan-300/20 bg-cyan-400/12 px-3 py-1.5 text-xs text-white disabled:opacity-60" @click="submitReply()">{{ submittingReply ? '...' : '回复' }}</button>
        </div>
      </div>
      <div class="px-6 py-3 space-y-1">
        <div v-if="commentsLoading" class="py-6 text-center text-xs text-slate-500">加载中...</div>
        <template v-else>
          <div v-if="comments.length === 0" class="py-6 text-center text-xs text-slate-600">暂无评论，来发第一条吧</div>
          <div v-for="comment in comments" :key="comment.id">
            <WikiCommentItem :comment="comment" :post-id="post.id" @reply="setReply" @like="toggleCommentLike" @report="openCommentReport" />
            <div v-if="comment.children?.length" class="ml-6 border-l border-cyan-300/8 pl-3">
              <WikiCommentItem v-for="child in comment.children" :key="child.id" :comment="child" :post-id="post.id" @reply="setReply" @like="toggleCommentLike" @report="openCommentReport" />
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>

  <!-- 编辑器弹窗 -->
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="editorVisible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm px-4" @click.self="closeEditor">
        <div class="w-full max-w-3xl border border-cyan-300/12 bg-slate-900 shadow-2xl">
          <div class="flex items-center justify-between border-b border-cyan-300/10 px-5 py-4">
            <div class="flex items-center gap-2"><div class="h-2 w-2 rounded-full bg-cyan-400/70"></div><h2 class="text-xs font-semibold uppercase tracking-widest text-slate-300">编辑文章</h2></div>
            <button class="flex h-7 w-7 items-center justify-center text-slate-500 hover:text-white" @click="closeEditor"><svg class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M6 18L18 6M6 6l12 12"/></svg></button>
          </div>
          <div class="p-5 space-y-3">
            <input v-model="editorTitle" placeholder="文章标题..." class="w-full border-0 border-b border-cyan-300/10 bg-transparent pb-2.5 text-xl font-semibold text-white outline-none placeholder:text-slate-600 focus:border-cyan-300/30" />
            <div class="flex gap-2">
              <div class="relative flex-1"><svg class="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg><input v-model="editorAuthor" placeholder="你的昵称（可选）" class="w-full border border-cyan-300/10 bg-slate-950/60 py-1.5 pl-8 pr-3 text-sm text-white outline-none placeholder:text-slate-500" /></div>
              <select v-model="editorTag" class="border border-cyan-300/10 bg-slate-950/60 px-3 py-1.5 text-sm text-white outline-none"><option v-for="t in postTags" :key="t" :value="t">{{ t }}</option></select>
            </div>
            <MarkdownEditor v-model="editorContent" />
            <input v-model="editorReason" placeholder="修改理由（选填）" class="w-full border border-amber-400/15 bg-amber-400/5 px-3 py-2 text-sm text-slate-300 outline-none placeholder:text-slate-600 focus:border-amber-400/30" />
            <p v-if="editorError" class="text-xs text-rose-300">{{ editorError }}</p>
            <div class="flex gap-3 pt-1">
              <button :disabled="submittingPost" class="border border-cyan-300/25 bg-cyan-400/12 px-6 py-2 text-sm font-semibold text-white disabled:opacity-60 hover:bg-cyan-400/20 active:scale-95" @click="submitPost">{{ submittingPost ? '提交中...' : '保存修改' }}</button>
              <button class="border border-slate-700/50 px-5 py-2 text-sm text-slate-400 hover:text-white" @click="closeEditor">取消</button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- 举报弹窗 -->
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="reportVisible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm px-4" @click.self="reportVisible = false">
        <div class="w-full max-w-sm border border-rose-400/15 bg-slate-900 shadow-2xl">
          <div class="flex items-center gap-2 border-b border-cyan-300/10 px-5 py-4"><svg class="h-4 w-4 text-rose-400" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M3 6a3 3 0 013-3h10a1 1 0 01.8 1.6L14.25 8l2.55 3.4A1 1 0 0116 13H6a1 1 0 00-1 1v3a1 1 0 11-2 0V6z" clip-rule="evenodd"/></svg><h3 class="text-sm font-semibold text-white">举报内容</h3></div>
          <div class="p-5 space-y-3">
            <textarea v-model="reportReason" placeholder="请描述举报原因（可选）..." rows="3" class="w-full border border-cyan-300/10 bg-slate-950/60 px-3 py-2 text-sm text-white outline-none placeholder:text-slate-500 resize-none"></textarea>
            <div class="flex gap-2">
              <button :disabled="submittingReport" class="border border-rose-400/30 bg-rose-400/10 px-4 py-2 text-sm text-rose-300 disabled:opacity-60 hover:bg-rose-400/15" @click="submitReport">{{ submittingReport ? '提交中...' : '确认举报' }}</button>
              <button class="border border-slate-700/50 px-4 py-2 text-sm text-slate-400 hover:text-white" @click="reportVisible = false">取消</button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import WikiCommentItem from '@/components/WikiCommentItem.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { api } from '@/services/api'
import { useGlobalNotice } from '@/composables/useGlobalNotice'
import type { WikiPost, WikiComment, WikiTag } from '@/types/models'

const route = useRoute()
const { showNotice } = useGlobalNotice()

const isAdmin = ref(api.isAdminLoggedIn())
window.addEventListener('admin-auth-changed', () => { isAdmin.value = api.isAdminLoggedIn() })

function getOrCreateToken(): string {
  let token = localStorage.getItem('wiki-author-token')
  if (!token) { token = crypto.randomUUID(); localStorage.setItem('wiki-author-token', token) }
  return token
}
getOrCreateToken()

const postTags = ['文章', '问题', '分享', '公告'] as const
const TAG_CLASSES: Record<string, string> = { '文章': 'border-cyan-400/30 text-cyan-300', '问题': 'border-amber-400/30 text-amber-300', '分享': 'border-emerald-400/30 text-emerald-300', '公告': 'border-rose-400/30 text-rose-300' }
const TAG_DOTS: Record<string, string> = { '文章': 'bg-cyan-400', '问题': 'bg-amber-400', '分享': 'bg-emerald-400', '公告': 'bg-rose-400' }
function tagClass(tag: string) { return TAG_CLASSES[tag] ?? 'border-slate-400/30 text-slate-300' }
function tagDot(tag: string) { return TAG_DOTS[tag] ?? 'bg-slate-400' }

function formatTime(iso: string): string {
  const d = new Date(iso), now = new Date(), diff = (now.getTime() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  if (diff < 86400 * 7) return `${Math.floor(diff / 86400)} 天前`
  return d.toLocaleDateString('zh-CN')
}

const post = ref<WikiPost | null>(null)
const loading = ref(true)

async function loadPost() {
  loading.value = true
  try { post.value = await api.getWikiPost(route.params.id as string) }
  catch { post.value = null }
  finally { loading.value = false }
}

// 管理员删除
async function adminDelete() {
  if (!post.value) return
  if (!confirm('确定删除这篇文章？此操作不可恢复。')) return
  try {
    await api.adminDeleteWikiPost(post.value.id)
    showNotice('文章已删除', 'success')
    window.location.href = '/wiki-help'
  } catch (e: any) { showNotice(e.message || '删除失败', 'error') }
}

// 点赞
async function toggleLike() {
  if (!post.value) return
  try { const updated = await api.toggleWikiLike('POST', post.value.id); if (updated) post.value = updated }
  catch (e: any) { showNotice(e.message || '操作失败', 'error') }
}

// 评论
const comments = ref<WikiComment[]>([])
const commentsLoading = ref(false)
const commentContent = ref('')
const commentAuthorName = ref(localStorage.getItem('wiki-author-name') ?? '')
const submittingComment = ref(false)
const replyTo = ref<WikiComment | null>(null)
const replyContent = ref('')
const submittingReply = ref(false)

async function loadComments() {
  if (!post.value) return
  commentsLoading.value = true
  try { comments.value = await api.getWikiComments(post.value.id) }
  finally { commentsLoading.value = false }
}

async function toggleCommentLike(commentId: string) {
  try {
    await api.toggleWikiLike('COMMENT', commentId)
    if (post.value) comments.value = await api.getWikiComments(post.value.id)
  } catch (e: any) { showNotice(e.message || '操作失败', 'error') }
}

function setReply(comment: WikiComment) { replyTo.value = comment; replyContent.value = '' }

async function submitComment() {
  if (!commentContent.value.trim() || !post.value) return
  submittingComment.value = true
  try {
    const name = commentAuthorName.value.trim()
    if (name) localStorage.setItem('wiki-author-name', name)
    await api.addWikiComment(post.value.id, commentContent.value, name)
    commentContent.value = ''
    await loadComments()
    post.value = await api.getWikiPost(post.value.id)
  } catch (e: any) { showNotice(e.message || '评论失败', 'error') }
  finally { submittingComment.value = false }
}

async function submitReply() {
  if (!replyContent.value.trim() || !replyTo.value || !post.value) return
  submittingReply.value = true
  try {
    const name = commentAuthorName.value.trim()
    await api.addWikiComment(post.value.id, replyContent.value, name, replyTo.value.id)
    replyContent.value = ''; replyTo.value = null
    await loadComments()
    post.value = await api.getWikiPost(post.value.id)
  } catch (e: any) { showNotice(e.message || '回复失败', 'error') }
  finally { submittingReply.value = false }
}

// 编辑器
const editorVisible = ref(false)
const editorTitle = ref('')
const editorContent = ref('')
const editorAuthor = ref(localStorage.getItem('wiki-author-name') ?? '')
const editorTag = ref<WikiTag>('文章')
const editorReason = ref('')
const editorError = ref('')
const submittingPost = ref(false)

async function handleEditClick() {
  if (!post.value) return
  if (post.value.isEditing) { showNotice('当前有人正在编辑此文章，请稍后再试', 'error'); return }
  try {
    const locked = await api.lockWikiPost(post.value.id)
    editorTitle.value = locked.title
    editorContent.value = locked.content
    editorAuthor.value = locked.authorName
    editorTag.value = locked.tag
    editorReason.value = ''
    editorError.value = ''
    editorVisible.value = true
  } catch (e: any) {
    showNotice(e.message || '获取编辑权限失败', 'error')
    post.value = await api.getWikiPost(post.value.id)
  }
}

async function closeEditor() {
  if (post.value) { try { await api.unlockWikiPost(post.value.id) } catch {} }
  editorVisible.value = false
}

async function submitPost() {
  if (!post.value) return
  editorError.value = ''
  if (!editorTitle.value.trim()) { editorError.value = '标题不能为空'; return }
  if (!editorContent.value.trim()) { editorError.value = '内容不能为空'; return }
  submittingPost.value = true
  try {
    const name = editorAuthor.value.trim()
    if (name) localStorage.setItem('wiki-author-name', name)
    const updated = await api.updateWikiPost(post.value.id, editorTitle.value, editorContent.value, name, post.value.version, editorTag.value, editorReason.value)
    post.value = updated
    editorVisible.value = false
    showNotice('文章已更新', 'success')
  } catch (e: any) { editorError.value = e.message || '提交失败' }
  finally { submittingPost.value = false }
}

// 举报
const reportVisible = ref(false)
const reportReason = ref('')
const reportCommentId = ref<string | null>(null)
const submittingReport = ref(false)

function openCommentReport(commentId: string) { reportCommentId.value = commentId; reportReason.value = ''; reportVisible.value = true }

async function submitReport() {
  submittingReport.value = true
  try {
    if (reportCommentId.value) await api.reportWiki('COMMENT', reportCommentId.value, reportReason.value)
    else if (post.value) await api.reportWiki('POST', post.value.id, reportReason.value)
    reportVisible.value = false
    showNotice('举报已提交，感谢反馈', 'success')
  } catch (e: any) { showNotice(e.message || '举报失败', 'error') }
  finally { submittingReport.value = false }
}

onMounted(async () => { await loadPost(); await loadComments() })
onUnmounted(async () => { if (post.value && editorVisible.value) { try { await api.unlockWikiPost(post.value.id) } catch {} } })
</script>

<style scoped>
.modal-fade-enter-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.modal-fade-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.97); }
</style>
