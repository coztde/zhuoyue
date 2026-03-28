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
}
