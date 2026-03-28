<template>
  <Teleport to="body">
    <Transition name="admin-login-fade">
      <div
        v-if="visible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 px-4 backdrop-blur-md"
        @click.self="handleClose"
      >
        <div class="relative w-full max-w-md border border-cyan-300/15 bg-slate-950/92 p-6 shadow-[0_30px_120px_rgba(2,8,23,0.72)]">
          <button
            class="absolute right-4 top-4 flex h-11 w-11 items-center justify-center border border-cyan-300/15 bg-slate-900/80 text-2xl leading-none text-slate-300 transition hover:border-cyan-300/35 hover:text-white"
            type="button"
            aria-label="关闭登录弹窗"
            @click="handleClose"
          >
            ×
          </button>

          <div class="mb-6">
            <p class="font-display text-xs uppercase tracking-[0.42em] text-cyan-100/60">Admin Access</p>
            <h2 class="mt-3 text-3xl font-semibold text-white">管理员登录</h2>
            <p class="muted-copy mt-3">用于任务发布、仓库导入、批量分析与结果修正。</p>
          </div>

          <form class="space-y-4" @submit.prevent="handleLogin">
            <label class="block space-y-2">
              <span class="text-sm text-slate-300">用户名</span>
              <input
                v-model="form.username"
                class="w-full border border-cyan-300/10 bg-slate-950/70 px-4 py-3 text-white outline-none transition focus:border-cyan-300/35"
                autocomplete="username"
              />
            </label>

            <label class="block space-y-2">
              <span class="text-sm text-slate-300">密码</span>
              <input
                v-model="form.password"
                type="password"
                class="w-full border border-cyan-300/10 bg-slate-950/70 px-4 py-3 text-white outline-none transition focus:border-cyan-300/35"
                autocomplete="current-password"
              />
            </label>

            <div v-if="message" class="border border-cyan-300/10 bg-slate-900/60 px-4 py-3 text-sm text-cyan-100">
              {{ message }}
            </div>

            <button
              class="w-full bg-cyan-400/90 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-300 disabled:cursor-not-allowed disabled:opacity-60"
              type="submit"
              :disabled="submitting"
            >
              {{ submitting ? '登录中...' : '登录后台' }}
            </button>
          </form>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { api } from '@/services/api'
import { useAdminLoginModal } from '@/composables/useAdminLoginModal'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const { adminLoginModalVisible, closeAdminLoginModal } = useAdminLoginModal()

const visible = computed(() => adminLoginModalVisible.value)
const submitting = ref(false)
const message = ref('')
const form = reactive({
  username: 'admin',
  password: 'wzlg123456',
})

/**
 * 弹窗打开时清空提示信息。
 * 这样用户多次打开登录框时，不会看到上一次失败或成功的残留提示。
 */
watch(visible, (value) => {
  if (value) {
    message.value = ''
  }
})

const handleClose = () => {
  if (!submitting.value) {
    closeAdminLoginModal()
  }
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    handleClose()
  }
}

async function handleLogin() {
  submitting.value = true
  try {
    const result = await api.adminLogin(form.username, form.password)
    message.value = `登录成功：${result.displayName}`
    closeAdminLoginModal()
    router.push('/admin/courses/create')
  } catch (error) {
    message.value = error instanceof Error ? error.message : '登录失败'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>

</style>
