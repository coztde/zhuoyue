export interface ApiResult<T> {
  success: boolean
  code: string
  message: string
  data: T
}

export interface AdminLoginResponse {
  token: string
  username: string
  displayName: string
  expireSeconds: number
}

export interface AdminCourse {
  id: string
  title: string
  taskTitle: string
  stageOrder?: number
}

export interface MissionStage {
  id: string
  title: string
  taskTitle: string
  stageOrder?: number
}

/** 学生档案（管理员视角）。 */
export interface StudentProfile {
  id: string
  realName: string
  platform: 'GITHUB' | 'GITEE'
  platformUsername: string
  avatarUrl?: string
  displayName?: string
  activeStatus: 'ACTIVE' | 'DISABLED'
  completedStages?: number
  lastSyncedAt?: string
}

/** 学生仓库。 */
export interface StudentRepo {
  id: string
  studentId: string
  platform: 'GITHUB' | 'GITEE'
  repoName: string
  repoType: 'MANUAL' | 'AUTO_SCAN' | 'GITHUB_IO'
  isActive: boolean
  isPrimary: boolean
  createdAt?: string
}

/** 阶段进度（含 AI 分析结果）。 */
export interface StageProgressVO {
  id: string
  studentId: string
  stageId: string
  stageTitle?: string
  repoName?: string
  isCompleted: boolean
  commitCount: number
  lastCommitTime?: string
  aiScore?: number
  aiFeedback?: string
  aiAnalyzedAt?: string
  manualScore?: number
  manualFeedback?: string
  manualEditedAt?: string
  effectiveScore?: number
  effectiveFeedback?: string
  manualEdited?: boolean
}

export type WikiTag = '文章' | '问题' | '分享' | '公告'

/** Wiki 文章。 */
export interface WikiPost {
  id: string
  title: string
  content: string
  authorName: string
  tag: WikiTag
  version: number
  likeCount: number
  commentCount: number
  heatScore: number
  createdAt: string
  updatedAt: string
  liked: boolean
  isAuthor: boolean
  isEditing: boolean
  editReason?: string
}

/** Wiki 评论（支持嵌套）。 */
export interface WikiComment {
  id: string
  postId: string
  parentId?: string
  authorName: string
  content: string
  likeCount: number
  createdAt: string
  liked: boolean
  isAuthor: boolean
  children?: WikiComment[]
}

export interface WikiReport {
  id: string
  targetType: 'POST' | 'COMMENT'
  targetId: string
  postId?: string  // COMMENT类型时关联的文章ID
  reason?: string
  reporterToken: string
  isHandled: boolean
  createdAt: string
}

export interface WikiPageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/** 排行榜条目（公开接口）。 */
export interface LeaderboardItem {
  id: string
  realName: string
  displayName?: string
  avatarUrl?: string
  completedStages: number
  totalStages: number
  latestCommit?: string
  lastSyncedAt?: string
}
