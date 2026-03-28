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
