<template>
  <div class="space-y-6">
    <AdminCourseManagerTabs />
    <GlassPanel eyebrow="后台模块" title="新增课程阶段" subtitle="阶段顺序不能重复，只填写顺序、标题和课程任务">
      <form class="space-y-4 border border-cyan-300/10 bg-slate-950/35 p-5" @submit.prevent="submitCourse">
        <div class="grid gap-4 md:grid-cols-[0.75fr_1.25fr]">
          <label class="space-y-2 text-sm text-slate-300">
            <span>阶段顺序</span>
            <input v-model.number="form.stageOrder" type="number" min="1" class="w-full border border-cyan-300/10 bg-slate-950/70 px-3 py-2 text-white outline-none" />
            <p class="text-xs text-slate-400">
              已占用顺序：{{ occupiedStageOrders.length ? occupiedStageOrders.join('、') : '暂无' }}
            </p>
          </label>
          <label class="space-y-2 text-sm text-slate-300">
            <span>标题</span>
            <input
              v-model="form.title"
              maxlength="128"
              class="w-full border border-cyan-300/10 bg-slate-950/70 px-3 py-2 text-white outline-none"
            />
            <p class="text-xs text-slate-400">最多 128 个字符</p>
          </label>
        </div>

        <label class="block space-y-2 border border-cyan-300/10 bg-slate-950/40 p-4 text-sm text-slate-300">
          <span class="text-sm font-semibold text-white">课程任务</span>
          <textarea
            v-model="form.taskTitle"
            rows="6"
            class="w-full resize-y border border-cyan-300/10 bg-slate-950/70 px-3 py-2 text-white outline-none"
            placeholder="这里填写课程任务正文，支持较长文本内容"
          ></textarea>
          <p class="text-xs text-slate-400">这里是正文文本，不再限制 255 个字符</p>
        </label>

        <button
          class="border border-cyan-300/20 bg-cyan-400/12 px-5 py-3 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
          type="submit"
          :disabled="submitting || stageOrderTaken"
        >
          {{ submitting ? '发布中...' : '发布课程阶段' }}
        </button>

        <p v-if="message" class="text-sm" :class="messageType === 'error' ? 'text-rose-200' : 'text-cyan-100'">
          {{ message }}
        </p>
      </form>
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import AdminCourseManagerTabs from '@/components/AdminCourseManagerTabs.vue'
import GlassPanel from '@/components/GlassPanel.vue'
import { useAdminLoginModal } from '@/composables/useAdminLoginModal'
import { useGlobalNotice } from '@/composables/useGlobalNotice'
import { api } from '@/services/api'
import type { AdminCourse } from '@/types/models'
import { computed, onMounted, reactive, ref } from 'vue'

const { openAdminLoginModal } = useAdminLoginModal()
const { showNotice } = useGlobalNotice()
const courses = ref<AdminCourse[]>([])
const message = ref('')
const messageType = ref<'success' | 'error'>('success')
const submitting = ref(false)
const form = reactive({
  title: '',
  stageOrder: 1,
  taskTitle: '',
})

const occupiedStageOrders = computed(() => courses.value
  .map((course) => course.stageOrder)
  .filter((stageOrder): stageOrder is number => typeof stageOrder === 'number')
  .sort((left, right) => left - right))

const stageOrderTaken = computed(() => occupiedStageOrders.value.includes(form.stageOrder))

onMounted(async () => {
  if (!api.isAdminLoggedIn()) {
    openAdminLoginModal()
    return
  }
  await loadCourses()
})

async function submitCourse() {
  if (stageOrderTaken.value) {
    messageType.value = 'error'
    message.value = '这个阶段顺序已经存在，请换一个阶段顺序'
    return
  }

  submitting.value = true
  message.value = ''
  try {
    await api.createCourse({
      title: form.title,
      stageOrder: form.stageOrder,
      taskTitle: form.taskTitle,
    })
    messageType.value = 'success'
    message.value = '课程阶段已发布'
    showNotice('新增课程阶段成功', 'success')
    Object.assign(form, {
      title: '',
      stageOrder: 1,
      taskTitle: '',
    })
    await loadCourses()
  } catch (error) {
    messageType.value = 'error'
    message.value = error instanceof Error ? error.message : '发布失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

async function loadCourses() {
  courses.value = await api.getAdminCourses()
}
</script>
