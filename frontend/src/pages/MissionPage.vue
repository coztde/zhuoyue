<template>
  <div class="space-y-6">
    <GlassPanel eyebrow="课程展览" title="课程任务地图" subtitle="按阶段查看当前课程任务与成长路径">
      <div class="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <section class="space-y-4">
          <div class="grid gap-4 md:grid-cols-3">
            <div class="border border-cyan-300/10 bg-slate-950/45 p-4">
              <p class="text-xs uppercase tracking-[0.3em] text-cyan-100/60">阶段数</p>
              <p class="mt-3 text-3xl font-semibold text-white">{{ stages.length }}</p>
            </div>
            <div class="border border-cyan-300/10 bg-slate-950/45 p-4">
              <p class="text-xs uppercase tracking-[0.3em] text-cyan-100/60">任务数</p>
              <p class="mt-3 text-3xl font-semibold text-white">{{ totalTasks }}</p>
            </div>
            <div class="border border-cyan-300/10 bg-slate-950/45 p-4">
              <p class="text-xs uppercase tracking-[0.3em] text-cyan-100/60">当前状态</p>
              <p class="mt-3 text-lg font-semibold text-emerald-200">{{ stages.length ? '已发布' : '待配置' }}</p>
            </div>
          </div>

          <div class="border border-cyan-300/10 bg-slate-950/35 p-5">
            <div class="flex items-center justify-between gap-4">
              <div>
                <p class="text-xs uppercase tracking-[0.32em] text-cyan-100/60">任务速览</p>
                <h3 class="mt-2 text-2xl font-semibold text-white">从阶段到任务一眼看清</h3>
              </div>
            <div class="hidden md:flex items-center gap-2">
                <span class="h-2.5 w-2.5 rounded-full bg-cyan-300/80"></span>
                <span class="text-sm text-slate-400">阶段节点</span>
              </div>
            </div>

            <div v-if="loading" class="mt-6 border border-cyan-300/10 bg-slate-950/50 px-5 py-10 text-center text-sm text-slate-300">
              正在加载课程任务...
            </div>

            <div v-else-if="errorMessage" class="mt-6 border border-rose-300/15 bg-rose-400/10 px-5 py-4 text-sm text-rose-100">
              {{ errorMessage }}
            </div>

            <div v-else-if="!stages.length" class="mt-6 border border-cyan-300/10 bg-slate-950/50 px-5 py-10 text-center text-sm text-slate-300">
              暂时还没有课程阶段，等后台发布后这里会自动展示。
            </div>

            <div v-else class="mt-6 space-y-5">
              <article
                v-for="(stage, index) in stages"
                :key="stage.id"
                class="relative overflow-hidden border border-cyan-300/10 bg-slate-950/55 p-5"
              >
                <div class="absolute left-0 top-0 h-full w-1 bg-gradient-to-b from-cyan-300/80 via-cyan-400/30 to-transparent"></div>
                <div class="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
                  <div class="space-y-3">
                    <div class="flex items-center gap-3">
                      <span class="flex h-10 w-10 items-center justify-center border border-cyan-300/20 bg-cyan-400/10 text-sm font-semibold text-cyan-50">
                        {{ stage.stageOrder ?? index + 1 }}
                      </span>
                      <div>
                        <p class="text-xs uppercase tracking-[0.28em] text-cyan-100/55">Stage {{ stage.stageOrder ?? index + 1 }}</p>
                        <h3 class="mt-1 text-2xl font-semibold text-white">{{ stage.title }}</h3>
                      </div>
                    </div>
                  </div>

                  <div class="flex items-center gap-2 text-xs text-slate-400">
                    <span class="border border-cyan-300/12 px-3 py-1.5">1 Task</span>
                  </div>
                </div>

                <div class="mt-5 border border-cyan-300/10 bg-slate-900/70 p-4 transition hover:border-cyan-300/25 hover:bg-slate-900/85">
                  <p class="text-xs uppercase tracking-[0.26em] text-cyan-100/45">Core Task</p>
                  <p class="mt-3 text-lg font-semibold text-white">{{ stage.taskTitle }}</p>
                </div>
              </article>
            </div>
          </div>
        </section>

        <aside class="space-y-4">
          <div class="border border-cyan-300/10 bg-gradient-to-br from-cyan-400/10 via-slate-950/65 to-slate-950/85 p-5">
            <p class="text-xs uppercase tracking-[0.32em] text-cyan-100/60">学习建议</p>
            <h3 class="mt-2 text-2xl font-semibold text-white">先看阶段，再拆任务</h3>
            <p class="mt-4 text-sm leading-7 text-slate-300">
              这一页适合做课程导航总览。先理解每个阶段的目标，再把当前阶段的核心任务吃透，学习路径会更清晰。
            </p>
          </div>

          <div class="border border-cyan-300/10 bg-slate-950/45 p-5">
            <p class="text-xs uppercase tracking-[0.32em] text-cyan-100/60">阶段目录</p>
            <div class="mt-4 space-y-3">
              <div
                v-for="(stage, index) in stages"
                :key="stage.id"
                class="flex items-center justify-between border border-cyan-300/10 bg-slate-950/60 px-4 py-3"
              >
                <div class="min-w-0">
                  <p class="text-sm font-semibold text-white">第 {{ stage.stageOrder ?? index + 1 }} 阶段</p>
                  <p class="mt-1 truncate text-xs text-slate-400">{{ stage.title }}</p>
                </div>
                <span class="text-xs text-cyan-100/65">1 项</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import GlassPanel from '@/components/GlassPanel.vue'
import { api } from '@/services/api'
import type { MissionStage } from '@/types/models'
import { computed, onMounted, ref } from 'vue'

const stages = ref<MissionStage[]>([])
const loading = ref(false)
const errorMessage = ref('')

const totalTasks = computed(() => stages.value.length)

onMounted(async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const missionStages = await api.getMissions()
    stages.value = [...missionStages].sort((left, right) => (left.stageOrder ?? 0) - (right.stageOrder ?? 0))
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '课程任务加载失败'
  } finally {
    loading.value = false
  }
})
</script>
