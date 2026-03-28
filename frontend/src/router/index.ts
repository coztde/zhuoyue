import { createRouter, createWebHistory } from 'vue-router'
import { useAdminLoginModal } from '@/composables/useAdminLoginModal'
import { useGlobalNotice } from '@/composables/useGlobalNotice'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('@/pages/HomePage.vue') },
    { path: '/leaderboard', name: 'leaderboard', component: () => import('@/pages/LeaderboardPage.vue') },
    { path: '/market', name: 'market', component: () => import('@/pages/TalentMarketPage.vue') },
    { path: '/missions', name: 'missions', component: () => import('@/pages/MissionPage.vue') },
    { path: '/wiki-help', name: 'wiki-help', component: () => import('@/pages/WikiHelpPage.vue') },
    { path: '/ai-qa', name: 'ai-qa', component: () => import('@/pages/AiQaPage.vue') },
    { path: '/admin/courses', redirect: '/admin/courses/create' },
    { path: '/admin/courses/create', name: 'admin-courses-create', component: () => import('@/pages/AdminCoursesPage.vue') },
    { path: '/admin/courses/edit', name: 'admin-courses-edit', component: () => import('@/pages/AdminCourseEditPage.vue') },
    { path: '/admin/courses/delete', name: 'admin-courses-delete', component: () => import('@/pages/AdminCourseDeletePage.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  if (!to.path.startsWith('/admin')) {
    return true
  }

  const token = localStorage.getItem('admin-token')
  if (token) {
    return true
  }

  const { openAdminLoginModal } = useAdminLoginModal()
  const { showNotice } = useGlobalNotice()
  showNotice('请先登录管理员账号后再进入后台', 'info')
  window.setTimeout(() => {
    openAdminLoginModal()
  }, 0)
  return true
})

export default router
