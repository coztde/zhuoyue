import { ref } from 'vue'

/**
 * 管理员登录弹窗全局状态。
 * 顶部导航和后台受限页面共用同一个登录框，
 * 这里统一收口状态，避免多个页面各自维护一套弹窗逻辑。
 */
const adminLoginModalVisible = ref(false)

export function useAdminLoginModal() {
  const openAdminLoginModal = () => {
    adminLoginModalVisible.value = true
  }

  const closeAdminLoginModal = () => {
    adminLoginModalVisible.value = false
  }

  return {
    adminLoginModalVisible,
    openAdminLoginModal,
    closeAdminLoginModal,
  }
}
