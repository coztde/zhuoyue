<template>
  <div class="space-y-6">
    <AdminCourseManagerTabs />
    <GlassPanel eyebrow="后台模块" title="修改课程阶段" subtitle="先选择要修改的阶段，再更新顺序、标题和课程任务">
      <div class="space-y-4 border border-cyan-300/10 bg-slate-950/35 p-5">
        <label class="block space-y-2 text-sm text-slate-300">
          <span>选择阶段</span>
          <select
            v-model="selectedCourseId"
            class="w-full border border-cyan-300/10 bg-slate-950/70 px-3 py-2 text-white outline-none"
          >
            <option value="">请选择要修改的阶段</option>
            <option v-for="course in sortedCourses" :key="course.id" :value="course.id">
              第 {{ course.stageOrder ?? '-' }} 阶段 - {{ course.title }}
            </option>
          </select>
          <p class="text-xs text-slate-400">
            {{ sortedCourses.length ? '选择后会自动带出原有内容' : '当前还没有可修改的课程阶段' }}
          </p>
        </label>

        <form v-if="selectedCourse" class="space-y-4" @submit.prevent="submitUpdate">
          <div class="grid gap-4 md:grid-cols-[0.75fr_1.25fr]">
            <label class="space-y-2 text-sm text-slate-300">
              <span>阶段顺序</span>
              <input
                v-model.number="form.stageOrder"
                type="number"
                min="1"
                class="w-full border border-cyan-300/10 bg-slate-950/70 px-3 py-2 text-white outline-none"
              />
              <p class="text-xs text-slate-400">
                已占用顺序：{{ otherOccupiedStageOrders.length ? otherOccupiedStageOrders.join('、') : '暂无' }}
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
            {{ submitting ? '更新中...' : '保存修改' }}
          </button>
        </form>

        <div v-else-if="loading" class="border border-cyan-300/10 bg-slate-950/35 p-5 text-sm text-slate-300">
          正在加载课程阶段...
        </div>

        <div v-else class="border border-cyan-300/10 bg-slate-950/35 p-5 text-sm text-slate-300">
          请选择一个课程阶段后再修改。
        </div>

        <p v-if="message" class="text-sm" :class="messageType === 'error' ? 'text-rose-200' : 'text-cyan-100'">
          {{ message }}
        </p>
      </div>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'

const { openAdminLoginModal } = useAdminLoginModal()
const { showNotice } = useGlobalNotice()
const courses = ref<AdminCourse[]>([])
const selectedCourseId = ref('')
const message = ref('')
const messageType = ref<'success' | 'error'>('success')
const loading = ref(false)
const submitting = ref(false)
const form = reactive({
  title: '',
  stageOrder: 1,
  taskTitle: '',
})

const sortedCourses = computed(() => [...courses.value].sort((left, right) => (left.stageOrder ?? 0) - (right.stageOrder ?? 0)))

const selectedCourse = computed(() => courses.value.find((course) => course.id === selectedCourseId.value) ?? null)

const otherOccupiedStageOrders = computed(() => courses.value
  .filter((course) => course.id !== selectedCourseId.value)
  .map((course) => course.stageOrder)
  .filter((stageOrder): stageOrder is number => typeof stageOrder === 'number')
  .sort((left, right) => left - right))

const stageOrderTaken = computed(() => otherOccupiedStageOrders.value.includes(form.stageOrder))

watch(selectedCourse, (course) => {
  if (!course) {
    Object.assign(form, {
      title: '',
      stageOrder: 1,
      taskTitle: '',
    })
    return
  }

  Object.assign(form, {
    title: course.title,
    stageOrder: course.stageOrder ?? 1,
    taskTitle: course.taskTitle,
  })
  message.value = ''
})

onMounted(async () => {
  if (!api.isAdminLoggedIn()) {
    openAdminLoginModal()
    return
  }
  await loadCourses()
})

async function loadCourses() {
  loading.value = true
  try {
    courses.value = await api.getAdminCourses()
    if (!selectedCourseId.value && courses.value.length) {
      selectedCourseId.value = courses.value[0].id
    }
  } catch (error) {
    messageType.value = 'error'
    message.value = error instanceof Error ? error.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function submitUpdate() {
  if (!selectedCourse.value) {
    messageType.value = 'error'
    message.value = '请先选择要修改的课程阶段'
    return
  }

  if (stageOrderTaken.value) {
    messageType.value = 'error'
    message.value = '这个阶段顺序已经存在，请换一个阶段顺序'
    return
  }

  submitting.value = true
  message.value = ''
  try {
    const updatedCourse = await api.updateCourse(selectedCourse.value.id, {
      title: form.title,
      stageOrder: form.stageOrder,
      taskTitle: form.taskTitle,
    })

    courses.value = courses.value.map((course) => (course.id === updatedCourse.id ? updatedCourse : course))
    selectedCourseId.value = updatedCourse.id
    messageType.value = 'success'
    message.value = '课程阶段已更新'
    showNotice('修改课程阶段成功', 'success')
  } catch (error) {
    messageType.value = 'error'
    message.value = error instanceof Error ? error.message : '更新失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>
