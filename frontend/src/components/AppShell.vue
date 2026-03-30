<template>
  <div class="relative flex min-h-screen flex-col overflow-hidden">
    <!-- 背景光晕 -->
    <div class="pointer-events-none absolute inset-0">
      <div class="absolute left-0 top-0 h-72 w-72 bg-cyan-400/10 blur-3xl"></div>
      <div class="absolute right-0 top-0 h-64 w-64 bg-emerald-400/6 blur-3xl"></div>
      <div class="absolute bottom-0 left-1/3 h-64 w-64 bg-blue-500/6 blur-3xl"></div>
    </div>

    <!-- Header -->
    <header class="relative z-10 border-b border-cyan-300/10 bg-slate-950/92 backdrop-blur-xl">
      <!-- 顶部极细发光线 -->
      <div class="h-px w-full bg-gradient-to-r from-transparent via-cyan-400/40 to-transparent"></div>

      <div class="flex min-h-[80px] w-full items-center justify-between gap-6 px-6 py-3 md:px-8 xl:px-10">
        <!-- Logo -->
        <div class="flex min-w-0 items-center gap-4">
          <div class="relative flex h-14 w-14 items-center justify-center border border-cyan-300/20 bg-gradient-to-br from-cyan-400/15 via-blue-500/8 to-transparent">
            <span class="font-display text-xl font-bold text-white">ZY</span>
            <!-- logo 角标 -->
            <span class="absolute -right-px -top-px h-2 w-2 border-r border-t border-cyan-400/50"></span>
            <span class="absolute -bottom-px -left-px h-2 w-2 border-b border-l border-cyan-400/50"></span>
          </div>
          <div class="min-w-0">
            <p class="font-code text-[10px] uppercase tracking-[0.3em] text-cyan-400/40">// SYSTEM ONLINE</p>
            <h1 class="mt-1 truncate text-xl font-semibold tracking-[0.1em] text-white md:text-2xl">卓越班 · 数字化成长看板</h1>
          </div>
        </div>

        <!-- Nav -->
        <nav class="flex flex-wrap items-center justify-end gap-px text-sm">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="group relative border border-transparent px-4 py-2.5 text-slate-400 transition-all duration-150 hover:border-cyan-300/15 hover:bg-cyan-400/8 hover:text-white"
            active-class="!border-cyan-300/20 !bg-cyan-400/10 !text-cyan-200"
          >
            <span class="font-code text-[10px] text-cyan-400/0 transition group-hover:text-cyan-400/40" style="margin-right:2px">&gt;</span>
            {{ item.label }}
          </RouterLink>

          <template v-if="adminLoggedIn">
            <RouterLink
              to="/admin/courses/create"
              class="ml-1 border border-cyan-300/25 bg-cyan-400/10 px-4 py-2.5 text-sm font-semibold text-cyan-200 transition hover:border-cyan-300/40 hover:bg-cyan-400/18"
            >
              管理后台
            </RouterLink>
            <button
              class="border border-slate-700/60 bg-slate-900/50 px-4 py-2.5 text-sm text-slate-400 transition hover:border-slate-600 hover:text-white"
              type="button"
              @click="handleLogoutAdmin"
            >
              退出
            </button>
          </template>
          <button
            v-else
            class="ml-1 border border-cyan-300/25 bg-cyan-400/10 px-4 py-2.5 text-sm font-semibold text-cyan-200 transition hover:border-cyan-300/40 hover:bg-cyan-400/18"
            type="button"
            @click="handleOpenAdminLogin"
          >
            后台登录
          </button>
        </nav>
      </div>
    </header>

    <!-- Main -->
    <main class="relative z-10 flex-1 px-6 py-6 md:px-8 md:py-8 xl:px-10">
      <slot />
    </main>

    <!-- Footer -->
    <footer class="relative z-10 border-t border-cyan-300/8 bg-slate-950/80 px-6 py-4 md:px-8 xl:px-10">
      <div class="flex items-center justify-between">
        <span class="font-code text-[10px] text-slate-600">// ZHUOYUE PLATFORM · GIT-DRIVEN GROWTH</span>
        <span class="font-code text-[10px] text-slate-700">BUILD 2025</span>
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

const syncAdminStatus = () => { adminLoggedIn.value = api.isAdminLoggedIn() }
const handleOpenAdminLogin = () => { openAdminLoginModal() }
const handleLogoutAdmin = () => {
  api.logoutAdmin()
  if (route.path.startsWith('/admin')) router.push('/')
}

onMounted(() => window.addEventListener('admin-auth-changed', syncAdminStatus))
onBeforeUnmount(() => window.removeEventListener('admin-auth-changed', syncAdminStatus))
</script>
