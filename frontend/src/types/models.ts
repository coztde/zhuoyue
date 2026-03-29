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
