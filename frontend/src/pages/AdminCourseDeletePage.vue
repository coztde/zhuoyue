<template>
  <div class="space-y-6">
    <AdminCourseManagerTabs />
    <GlassPanel eyebrow="后台模块" title="删除课程阶段">
      <div class="space-y-4">
        <div v-if="message" class="text-sm" :class="messageType === 'error' ? 'text-rose-200' : 'text-cyan-100'">
          {{ message }}
        </div>

        <div v-if="loading" class="border border-cyan-300/10 bg-slate-950/35 p-5 text-sm text-slate-300">
          正在加载课程阶段...
        </div>

        <div v-else-if="!courses.length" class="border border-cyan-300/10 bg-slate-950/35 p-5 text-sm text-slate-300">
          当前还没有已发布的课程阶段。
        </div>

        <div v-else class="grid gap-4">
          <article v-for="course in courses" :key="course.id" class="border border-cyan-300/10 bg-slate-950/35 p-5">
            <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
              <div class="space-y-3">
                <div>
                  <p class="text-sm tracking-[0.18em] text-cyan-100/70">第 {{ course.stageOrder ?? 0 }} 阶段</p>
                  <h3 class="mt-2 text-xl font-semibold text-white">{{ course.title }}</h3>
                </div>
                <div class="border border-cyan-300/10 bg-slate-950/55 p-3">
                  <p class="text-xs uppercase tracking-[0.24em] text-cyan-100/55">课程任务</p>
                  <p class="mt-3 text-sm text-white">{{ course.taskTitle }}</p>
                </div>
              </div>

              <button
                class="border border-rose-300/20 bg-rose-400/10 px-4 py-2 text-sm font-semibold text-rose-100 transition hover:bg-rose-400/20 disabled:cursor-not-allowed disabled:opacity-60"
                type="button"
                :disabled="deletingId === course.id"
                @click="openDeleteDialog(course.id)"
              >
                {{ deletingId === course.id ? '删除中...' : '删除阶段' }}
              </button>
            </div>
          </article>
        </div>
      </div>
    </GlassPanel>

    <ConfirmDialog
      :visible="deleteDialogVisible"
      title="确认删除课程阶段"
      message="删除后，这个课程阶段会被直接清理，而且不能直接恢复。"
      confirm-text="确认删除"
      cancel-text="先不删"
      @confirm="confirmDelete"
      @cancel="closeDeleteDialog"
    />
  </div>
</template>

<script setup lang="ts">
import AdminCourseManagerTabs from '@/components/AdminCourseManagerTabs.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import GlassPanel from '@/components/GlassPanel.vue'
import { useAdminLoginModal } from '@/composables/useAdminLoginModal'
import { api } from '@/services/api'
import type { AdminCourse } from '@/types/models'
import { onMounted, ref } from 'vue'

const { openAdminLoginModal } = useAdminLoginModal()
const courses = ref<AdminCourse[]>([])
const loading = ref(false)
const deletingId = ref<string | null>(null)
const pendingDeleteId = ref<string | null>(null)
const deleteDialogVisible = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error' | 'info'>('success')

onMounted(async () => {
  if (!api.isAdminLoggedIn()) {
    openAdminLoginModal()
    return
  }
  await loadCourses()
})

async function loadCourses() {
  loading.value = true
  message.value = ''
  try {
    courses.value = await api.getAdminCourses()
  } catch (error) {
    messageType.value = 'error'
    message.value = error instanceof Error ? error.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function openDeleteDialog(courseId: string) {
  pendingDeleteId.value = courseId
  deleteDialogVisible.value = true
}

function closeDeleteDialog() {
  deleteDialogVisible.value = false
  pendingDeleteId.value = null
}

async function confirmDelete() {
  if (pendingDeleteId.value == null) {
    return
  }

  const courseId = pendingDeleteId.value
  deletingId.value = courseId
  deleteDialogVisible.value = false
  message.value = ''
  try {
    await api.deleteCourse(courseId)
    courses.value = courses.value.filter((course) => course.id !== courseId)
    messageType.value = 'success'
    message.value = '课程阶段已删除'
  } catch (error) {
    const nextMessage = error instanceof Error ? error.message : '删除失败，请稍后重试'
    if (nextMessage.includes('不存在')) {
      await loadCourses()
      messageType.value = 'info'
      message.value = '课程阶段已不存在，列表已自动刷新'
    } else {
      messageType.value = 'error'
      message.value = nextMessage
    }
  } finally {
    deletingId.value = null
    pendingDeleteId.value = null
  }
}
</script>
