import axios from 'axios'
import type { AxiosError } from 'axios'
import router from '@/router'
import { useAdminLoginModal } from '@/composables/useAdminLoginModal'
import { useGlobalNotice } from '@/composables/useGlobalNotice'
import type {
  AdminLoginResponse,
  AdminCourse,
  ApiResult,
  MissionStage,
  StudentProfile,
  StudentRepo,
  StageProgressVO,
  LeaderboardItem,
  WikiPost,
  WikiComment,
  WikiReport,
  WikiPageResult,
  WikiTag,
} from '@/types/models'

/**
 * 统一 Axios 实例。
 * 前端统一连接真实后端，
 * 这样联调阶段看到的问题就是实际链路问题，便于尽快定位。
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10000,
})

const emitAdminAuthChanged = () => {
  window.dispatchEvent(new Event('admin-auth-changed'))
}

const { openAdminLoginModal } = useAdminLoginModal()
const { showNotice } = useGlobalNotice()
let adminSessionRedirecting = false

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin-token')
  if (token && config.url?.startsWith('/api/admin')) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (shouldHandleAdminUnauthorized(error)) {
      handleAdminSessionExpired()
    }
    return Promise.reject(error)
  },
)

async function unwrap<T>(promise: Promise<{ data: ApiResult<T> }>): Promise<T> {
  try {
    const { data } = await promise
    if (!data.success) {
      throw new Error(data.message || '请求失败')
    }
    return data.data
  } catch (error) {
    throw new Error(resolveRequestError(error))
  }
}

function resolveRequestError(error: unknown) {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiResult<unknown>>
    const responseMessage = axiosError.response?.data?.message
    if (responseMessage) {
      return responseMessage
    }
    if (axiosError.code === 'ECONNABORTED') {
      return '请求超时，请检查后端服务是否正常'
    }
    if (!axiosError.response) {
      return '后端服务不可用，请确认接口服务已经启动'
    }
    if (axiosError.response.status === 401 || axiosError.response.status === 403) {
      return '后台接口需要先登录'
    }
    if (axiosError.response.status >= 500) {
      return '后端服务异常，请稍后重试'
    }
  }
  if (error instanceof Error) {
    return error.message
  }
  return '请求失败，请稍后重试'
}

function shouldHandleAdminUnauthorized(error: unknown) {
  if (!axios.isAxiosError(error)) {
    return false
  }
  const requestUrl = error.config?.url ?? ''
  if (!requestUrl.startsWith('/api/admin')) {
    return false
  }
  if (requestUrl.startsWith('/api/admin/auth/login')) {
    return false
  }

  const status = error.response?.status
  const message = error.response?.data?.message ?? ''
  return status === 401 || status === 403 || message.includes('先登录') || message.includes('登录已过期') || message.includes('token')
}

function handleAdminSessionExpired() {
  if (adminSessionRedirecting) {
    return
  }
  adminSessionRedirecting = true
  localStorage.removeItem('admin-token')
  emitAdminAuthChanged()
  showNotice('登录状态已失效，请重新登录后继续操作', 'error')

  const finish = () => {
    openAdminLoginModal()
    adminSessionRedirecting = false
  }

  if (router.currentRoute.value.path !== '/') {
    router.push('/').finally(finish)
    return
  }
  finish()
}

export const api = {
  getMissions(): Promise<MissionStage[]> {
    return unwrap(request.get<ApiResult<MissionStage[]>>('/api/public/missions'))
  },
  async adminLogin(username: string, password: string): Promise<AdminLoginResponse> {
    const result = await unwrap(request.post<ApiResult<AdminLoginResponse>>('/api/admin/auth/login', { username, password }))
    localStorage.setItem('admin-token', result.token)
    emitAdminAuthChanged()
    return result
  },
  logoutAdmin() {
    localStorage.removeItem('admin-token')
    emitAdminAuthChanged()
  },
  isAdminLoggedIn(): boolean {
    return !!localStorage.getItem('admin-token')
  },
  getAdminCourses(): Promise<AdminCourse[]> {
    return unwrap(request.get<ApiResult<AdminCourse[]>>('/api/admin/courses'))
  },
  createCourse(payload: Partial<AdminCourse>): Promise<AdminCourse> {
    return unwrap(request.post<ApiResult<AdminCourse>>('/api/admin/courses', payload))
  },
  updateCourse(courseId: string | number, payload: Partial<AdminCourse>): Promise<AdminCourse> {
    return unwrap(request.put<ApiResult<AdminCourse>>(`/api/admin/courses/${courseId}`, payload))
  },
  deleteCourse(courseId: string | number): Promise<void> {
    return unwrap(request.delete<ApiResult<void>>(`/api/admin/courses/${courseId}`))
  },

  // ── 排行榜（公开） ──────────────────────────────────────
  getLeaderboard(): Promise<LeaderboardItem[]> {
    return unwrap(request.get<ApiResult<LeaderboardItem[]>>('/api/public/leaderboard'))
  },
  getLeaderboardFull(): Promise<LeaderboardItem[]> {
    return unwrap(request.get<ApiResult<LeaderboardItem[]>>('/api/public/leaderboard/full'))
  },

  // ── 学生管理（管理员） ──────────────────────────────────
  getAdminStudents(): Promise<StudentProfile[]> {
    return unwrap(request.get<ApiResult<StudentProfile[]>>('/api/admin/students'))
  },
  createStudent(payload: Pick<StudentProfile, 'realName' | 'platform' | 'platformUsername'>): Promise<StudentProfile> {
    return unwrap(request.post<ApiResult<StudentProfile>>('/api/admin/students', payload))
  },
  updateStudent(id: string, payload: Pick<StudentProfile, 'realName' | 'platform' | 'platformUsername'>): Promise<StudentProfile> {
    return unwrap(request.put<ApiResult<StudentProfile>>(`/api/admin/students/${id}`, payload))
  },
  deleteStudent(id: string): Promise<void> {
    return unwrap(request.delete<ApiResult<void>>(`/api/admin/students/${id}`))
  },

  // ── 仓库管理（管理员） ──────────────────────────────────
  getStudentRepos(studentId: string): Promise<StudentRepo[]> {
    return unwrap(request.get<ApiResult<StudentRepo[]>>(`/api/admin/students/${studentId}/repos`))
  },
  addStudentRepo(studentId: string, repoName: string, platform: string): Promise<StudentRepo> {
    return unwrap(request.post<ApiResult<StudentRepo>>(`/api/admin/students/${studentId}/repos`, { repoName, platform }))
  },
  scanStudentRepos(studentId: string): Promise<{ added: number }> {
    return unwrap(request.post<ApiResult<{ added: number }>>(`/api/admin/students/${studentId}/repos/scan`))
  },
  linkPageRepo(studentId: string): Promise<StudentRepo> {
    return unwrap(request.post<ApiResult<StudentRepo>>(`/api/admin/students/${studentId}/repos/link-page`))
  },
  deleteStudentRepo(studentId: string, repoId: string): Promise<void> {
    return unwrap(request.delete<ApiResult<void>>(`/api/admin/students/${studentId}/repos/${repoId}`))
  },
  setPrimaryRepo(studentId: string, repoId: string): Promise<StudentRepo> {
    return unwrap(request.put<ApiResult<StudentRepo>>(`/api/admin/students/${studentId}/repos/${repoId}/primary`))
  },

  // ── AI 分析（管理员） ──────────────────────────────────
  // repoId 可选，不传则使用主仓库
  analyzeStage(studentId: string, stageId: string, repoId?: string): Promise<StageProgressVO> {
    const params = repoId ? { repoId } : {}
    return unwrap(request.post<ApiResult<StageProgressVO>>(`/api/admin/ai/analyze/${studentId}/${stageId}`, params))
  },
  // 批量分析单个学生所有阶段，repoId 可选
  analyzeStudent(studentId: string, repoId?: string): Promise<string> {
    const params = repoId ? { repoId } : {}
    return unwrap(request.post<ApiResult<string>>(`/api/admin/ai/analyze/${studentId}`, params))
  },
  analyzeAll(): Promise<string> {
    return unwrap(request.post<ApiResult<string>>('/api/admin/ai/analyze/all'))
  },
  getStudentProgress(studentId: string): Promise<StageProgressVO[]> {
    return unwrap(request.get<ApiResult<StageProgressVO[]>>(`/api/admin/ai/progress/${studentId}`))
  },
  updateManualScore(progressId: string, manualScore: number | null, manualFeedback: string | null): Promise<StageProgressVO> {
    return unwrap(request.put<ApiResult<StageProgressVO>>(`/api/admin/ai/progress/${progressId}/manual`, { manualScore, manualFeedback }))
  },

  // ── Wiki（公开） ──────────────────────────────────────
  getWikiPosts(page = 1, size = 20, sort = 'heat', keyword?: string, tag?: string): Promise<WikiPageResult<WikiPost>> {
    const token = localStorage.getItem('wiki-author-token') ?? ''
    return unwrap(request.get<ApiResult<WikiPageResult<WikiPost>>>('/api/public/wiki/posts', {
      params: { page, size, sort, keyword, tag },
      headers: { 'X-Wiki-Token': token },
    }))
  },
  getWikiPost(id: string): Promise<WikiPost> {
    const token = localStorage.getItem('wiki-author-token') ?? ''
    return unwrap(request.get<ApiResult<WikiPost>>(`/api/public/wiki/posts/${id}`, {
      headers: { 'X-Wiki-Token': token },
    }))
  },
  createWikiPost(title: string, content: string, authorName: string, tag: WikiTag): Promise<WikiPost> {
    const token = localStorage.getItem('wiki-author-token') ?? ''
    return unwrap(request.post<ApiResult<WikiPost>>('/api/public/wiki/posts', {
      title, content, authorName, tag, authorToken: token,
    }))
  },
  updateWikiPost(id: string, title: string, content: string, authorName: string, version: number, tag: WikiTag, editReason: string): Promise<WikiPost> {
    const token = localStorage.getItem('wiki-author-token') ?? ''
    return unwrap(request.put<ApiResult<WikiPost>>(`/api/public/wiki/posts/${id}`, {
      title, content, authorName, tag, authorToken: token, version, editReason,
    }))
  },
  lockWikiPost(id: string): Promise<WikiPost> {
    const token = localStorage.getItem('wiki-author-token') ?? ''
    return unwrap(request.post<ApiResult<WikiPost>>(`/api/public/wiki/posts/${id}/lock`, {}, {
      headers: { 'X-Wiki-Token': token },
    }))
  },
  unlockWikiPost(id: string): Promise<void> {
    const token = localStorage.getItem('wiki-author-token') ?? ''
    return unwrap(request.post<ApiResult<void>>(`/api/public/wiki/posts/${id}/unlock`, {}, {
      headers: { 'X-Wiki-Token': token },
    }))
  },
  getWikiComments(postId: string): Promise<WikiComment[]> {
    const token = localStorage.getItem('wiki-author-token') ?? ''
    return unwrap(request.get<ApiResult<WikiComment[]>>(`/api/public/wiki/posts/${postId}/comments`, {
      headers: { 'X-Wiki-Token': token },
    }))
  },
  addWikiComment(postId: string, content: string, authorName: string, parentId?: string): Promise<WikiComment> {
    const token = localStorage.getItem('wiki-author-token') ?? ''
    return unwrap(request.post<ApiResult<WikiComment>>(`/api/public/wiki/posts/${postId}/comments`, {
      content, authorName, authorToken: token, parentId,
    }))
  },
  toggleWikiLike(targetType: 'POST' | 'COMMENT', targetId: string): Promise<WikiPost | null> {
    const token = localStorage.getItem('wiki-author-token') ?? ''
    return unwrap(request.post<ApiResult<WikiPost | null>>('/api/public/wiki/like', {
      targetType, targetId, voterToken: token,
    }, { headers: { 'X-Wiki-Token': token } }))
  },
  reportWiki(targetType: 'POST' | 'COMMENT', targetId: string, reason: string): Promise<void> {
    const token = localStorage.getItem('wiki-author-token') ?? ''
    return unwrap(request.post<ApiResult<void>>('/api/public/wiki/report', {
      targetType, targetId, reason, reporterToken: token,
    }))
  },

  // ── Wiki 管理员 ────────────────────────────────────────
  adminGetWikiReports(page = 1, size = 20): Promise<WikiPageResult<WikiReport>> {
    return unwrap(request.get<ApiResult<WikiPageResult<WikiReport>>>('/api/admin/wiki/reports', {
      params: { page, size },
    }))
  },
  adminHandleWikiReport(id: string): Promise<void> {
    return unwrap(request.put<ApiResult<void>>(`/api/admin/wiki/reports/${id}/handle`))
  },
  adminDeleteWikiPost(id: string): Promise<void> {
    return unwrap(request.delete<ApiResult<void>>(`/api/admin/wiki/posts/${id}`))
  },
  adminDeleteWikiComment(id: string): Promise<void> {
    return unwrap(request.delete<ApiResult<void>>(`/api/admin/wiki/comments/${id}`))
  },

  // ── Coze 知识库问答（公开） ──────────────────────────────
  /**
   * 向 Coze 知识库提问，返回 SSE 流。
   * 使用 fetch + ReadableStream 实现（EventSource 不支持 POST）。
   * @param question 提问内容
   * @param onChunk  收到每个文字块时的回调
   * @param onDone   回答完成时的回调
   * @param onError  发生错误时的回调
   */
  async cozeChatStream(
    question: string,
    onChunk: (text: string) => void,
    onDone: () => void,
    onError: (msg: string) => void,
    onAborted?: () => void,
    onRequestId?: (id: string) => void,
  ): Promise<void> {
    const baseURL = import.meta.env.VITE_API_BASE_URL ?? ''
    const response = await fetch(`${baseURL}/api/coze/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question }),
    })
    // 响应头一到立即回调 requestId，此时流还未开始，前端可马上记录用于中断
    const requestId = response.headers.get('X-Request-Id')
    if (requestId) onRequestId?.(requestId)
    if (!response.ok || !response.body) {
      onError('请求失败，请稍后重试')
      return
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const { done, value } = await reader.read()
      if (done) { onDone(); break }
      buffer += decoder.decode(value, { stream: true })
      // SSE 格式：event 和 data 成对出现，以空行分隔
      // Spring SseEmitter 格式示例："event:message\ndata:你好\n\n"
      const blocks = buffer.split('\n\n')
      buffer = blocks.pop() ?? ''
      for (const block of blocks) {
        const lines = block.split('\n')
        let eventName = ''
        let dataVal = ''
        for (const line of lines) {
          if (line.startsWith('event:')) eventName = line.slice(6).trim()
          else if (line.startsWith('data:')) dataVal = line.slice(5).trim()
        }
        if (eventName === 'message' && dataVal) onChunk(dataVal)
        else if (eventName === 'done') { onDone(); return }
        else if (eventName === 'aborted') { onAborted?.(); return }
        else if (eventName === 'error') onError(dataVal)
      }
    }
  },

  /** 中断指定请求的流式输出，后端真正关闭 Coze 连接 */
  async cozeAbort(requestId: string): Promise<void> {
    const baseURL = import.meta.env.VITE_API_BASE_URL ?? ''
    await fetch(`${baseURL}/api/coze/abort/${requestId}`, { method: 'POST' })
  },

  // ── Git 同步（管理员） ──────────────────────────────────
  syncAll(): Promise<void> {
    return unwrap(request.post<ApiResult<void>>('/api/admin/sync/all'))
  },
  syncStudent(id: string): Promise<void> {
    return unwrap(request.post<ApiResult<void>>(`/api/admin/sync/${id}`))
  },
}
