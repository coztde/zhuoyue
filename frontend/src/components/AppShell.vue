<template>
  <div class="relative flex min-h-screen flex-col overflow-hidden">
    <div class="pointer-events-none absolute inset-0 opacity-60">
      <div class="absolute left-0 top-0 h-60 w-60 bg-cyan-400/12 blur-3xl"></div>
      <div class="absolute right-0 top-0 h-72 w-72 bg-emerald-400/8 blur-3xl"></div>
      <div class="absolute bottom-0 left-1/3 h-72 w-72 bg-blue-500/8 blur-3xl"></div>
    </div>

    <header class="relative z-10 border-b border-cyan-300/10 bg-slate-950/92 backdrop-blur-xl">
      <div class="flex min-h-[86px] w-full items-center justify-between gap-6 px-6 py-4 md:px-8 xl:px-10">
        <div class="flex min-w-0 items-center gap-4">
          <div class="flex h-16 w-16 items-center justify-center border border-cyan-300/15 bg-gradient-to-br from-cyan-400/20 via-blue-500/10 to-transparent">
            <span class="font-display text-2xl text-white">ZY</span>
          </div>
          <div class="min-w-0">
            <p class="truncate text-xs uppercase tracking-[0.32em] text-cyan-100/55">卓越班数字化管理平台</p>
            <h1 class="mt-2 truncate text-2xl font-semibold tracking-[0.12em] text-white md:text-3xl">Git 驱动成长看板</h1>
          </div>
        </div>

        <nav class="flex flex-wrap items-center justify-end gap-0.5 text-sm text-slate-300">
            <RouterLink
              v-for="item in navItems"
              :key="item.to"
              :to="item.to"
              class="border border-cyan-300/12 bg-slate-950/40 px-5 py-3 text-center transition hover:bg-cyan-400/10 hover:text-white"
              active-class="bg-cyan-400/12 text-white"
            >
              {{ item.label }}
            </RouterLink>
            <template v-if="adminLoggedIn">
              <RouterLink
                to="/admin/courses/create"
                class="border border-cyan-300/20 bg-cyan-400/12 px-5 py-3 text-sm font-semibold text-white transition hover:bg-cyan-400/20"
              >
                管理后台
              </RouterLink>
              <button
                class="border border-cyan-300/12 bg-slate-950/40 px-5 py-3 text-sm font-semibold text-slate-300 transition hover:bg-slate-900/70 hover:text-white"
                type="button"
                @click="handleLogoutAdmin"
              >
                退出后台
              </button>
            </template>
            <button
              v-else
              class="border border-cyan-300/20 bg-cyan-400/12 px-5 py-3 text-sm font-semibold text-white transition hover:bg-cyan-400/20"
              type="button"
              @click="handleOpenAdminLogin"
            >
              后台登录
            </button>
        </nav>
      </div>
    </header>

    <main class="relative z-10 flex-1 px-6 py-6 md:px-8 md:py-8 xl:px-10">
      <slot />
    </main>

    <footer class="relative z-10 border-t border-cyan-300/10 bg-slate-950/82 px-6 py-5 md:px-8 xl:px-10">
      <div class="text-center text-sm text-slate-400">
        卓越班数字化管理平台 | Git 驱动学习反馈 · 任务分析 · 人才展示
      </div>
    </footer>

    <AdminLoginModal />
  </div>
</template>

<script setup lang="ts">
import AdminLoginModal from '@/components/AdminLoginModal.vue'
import { useAdminLoginModal } from '@/composables/useAdminLoginModal'
import { api } from '@/services/api'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const { openAdminLoginModal } = useAdminLoginModal()
const adminLoggedIn = ref(api.isAdminLoggedIn())

const navItems = [
  { label: '大屏首页', to: '/' },
  { label: '卷王榜', to: '/leaderboard' },
  { label: '人才市场', to: '/market' },
  { label: '课程展览', to: '/missions' },
  { label: 'Wiki求助', to: '/wiki-help' },
  { label: 'AI问答', to: '/ai-qa' },
]

const syncAdminStatus = () => {
  adminLoggedIn.value = api.isAdminLoggedIn()
}

const handleOpenAdminLogin = () => {
  openAdminLoginModal()
}

const handleLogoutAdmin = () => {
  api.logoutAdmin()
  if (route.path.startsWith('/admin')) {
    router.push('/')
  }
}

onMounted(() => {
  window.addEventListener('admin-auth-changed', syncAdminStatus)
})

onBeforeUnmount(() => {
  window.removeEventListener('admin-auth-changed', syncAdminStatus)
})
</script>
