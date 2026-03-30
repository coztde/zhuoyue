<template>
  <div class="space-y-6">
    <GlassPanel eyebrow="MISSION_MAP" title="课程任务地图" subtitle="按阶段查看当前课程任务与成长路径">
      <div class="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <!-- 左侧：阶段列表 -->
        <section class="space-y-4">
          <div class="grid gap-4 md:grid-cols-3">
            <div class="border border-cyan-300/10 bg-slate-950/45 p-4">
              <p class="font-code text-[10px] uppercase tracking-[0.3em] text-cyan-400/40">// STAGES</p>
              <p class="font-code mt-3 text-3xl font-medium text-white">{{ stages.length }}</p>
            </div>
            <div class="border border-cyan-300/10 bg-slate-950/45 p-4">
              <p class="font-code text-[10px] uppercase tracking-[0.3em] text-cyan-400/40">// TASKS</p>
              <p class="font-code mt-3 text-3xl font-medium text-white">{{ totalTasks }}</p>
            </div>
            <div class="border border-cyan-300/10 bg-slate-950/45 p-4">
              <p class="font-code text-[10px] uppercase tracking-[0.3em] text-cyan-400/40">// STATUS</p>
              <p class="font-code mt-3 text-lg font-medium" :class="stages.length ? 'text-emerald-400' : 'text-slate-500'">{{ stages.length ? '■ ACTIVE' : '○ PENDING' }}</p>
            </div>
          </div>

          <div class="border border-cyan-300/10 bg-slate-950/35 p-5">
            <!-- 区域标题 -->
            <div class="flex items-center gap-3 border-b border-cyan-300/10 pb-4">
              <div class="flex h-8 w-8 items-center justify-center bg-cyan-400/15">
                <span class="text-xs font-bold text-cyan-300">▶</span>
              </div>
              <div>
                <p class="text-xs uppercase tracking-[0.32em] text-cyan-100/50">Task Roadmap</p>
                <h3 class="text-base font-semibold text-white">阶段任务路线图</h3>
              </div>
              <div class="ml-auto flex items-center gap-1.5 rounded border border-cyan-300/15 bg-cyan-400/5 px-2.5 py-1">
                <span class="h-1.5 w-1.5 rounded-full bg-emerald-400"></span>
                <span class="text-xs text-slate-400">{{ stages.length }} 阶段</span>
              </div>
            </div>

            <div v-if="loading" class="mt-6 flex items-center justify-center gap-3 py-10">
              <span class="h-4 w-4 animate-spin rounded-full border-2 border-cyan-300/20 border-t-cyan-300"></span>
              <span class="font-code text-xs text-slate-500">LOADING MISSIONS...</span>
            </div>
            <div v-else-if="errorMessage" class="mt-6 border border-rose-300/15 bg-rose-400/10 px-5 py-4 text-sm text-rose-100">
              {{ errorMessage }}
            </div>
            <div v-else-if="!stages.length" class="mt-6 py-10 text-center text-sm text-slate-500">
              暂时还没有课程阶段，等后台发布后这里会自动展示。
            </div>

            <!-- 时间线布局 -->
            <div v-else class="relative mt-5 pl-6">
              <!-- 纵向连接线 -->
              <div class="absolute left-[11px] top-3 bottom-3 w-px bg-gradient-to-b from-cyan-400/60 via-cyan-400/20 to-transparent"></div>

              <div class="space-y-4">
                <article
                  v-for="(stage, index) in stages"
                  :key="stage.id"
                  class="relative"
                >
                  <!-- 时间线节点圆点 -->
                  <div class="absolute -left-6 top-4 flex h-5 w-5 items-center justify-center rounded-full border border-cyan-300/40 bg-slate-950 text-[10px] font-bold text-cyan-300">
                    {{ stage.stageOrder ?? index + 1 }}
                  </div>

                  <!-- 卡片内容 -->
                  <div class="border border-cyan-300/10 bg-slate-950/60 transition hover:border-cyan-300/25 hover:bg-slate-950/80">
                    <!-- 卡片头部 -->
                    <div class="flex items-center justify-between gap-3 border-b border-cyan-300/8 px-4 py-3">
                      <div>
                        <span class="text-[10px] uppercase tracking-widest text-cyan-100/40">Stage {{ stage.stageOrder ?? index + 1 }}</span>
                        <h3 class="mt-0.5 text-sm font-semibold text-white">{{ stage.title }}</h3>
                      </div>
                      <span class="font-code shrink-0 border border-cyan-300/12 bg-cyan-400/5 px-2 py-0.5 text-[10px] text-cyan-300/50">[ 1 TASK ]</span>
                    </div>
                    <!-- 任务描述 -->
                    <div class="px-4 py-3">
                      <p class="text-[10px] uppercase tracking-widest text-slate-500">核心任务</p>
                      <p class="mt-1.5 text-sm text-slate-200">{{ stage.taskTitle }}</p>
                    </div>
                  </div>
                </article>
              </div>
            </div>
          </div>
        </section>

        <!-- 右侧：学习内容知识导航 -->
        <aside class="space-y-4">
          <!-- 标题卡片 -->
          <div class="border border-cyan-300/10 bg-gradient-to-br from-cyan-400/8 via-slate-950/65 to-slate-950/85 p-5">
            <p class="font-code text-[10px] uppercase tracking-[0.32em] text-cyan-400/40">// KNOWLEDGE_NAV</p>
            <h3 class="section-title mt-2">大模型开发知识导航</h3>
            <p class="muted-copy mt-2">先理解概念边界，再掌握核心术语，最后理清各模块关系。</p>
          </div>

          <!-- 模块一：大模型开发概述 -->
          <div class="border border-cyan-300/10 bg-slate-950/45">
            <button class="flex w-full items-center justify-between px-5 py-4 text-left transition hover:bg-slate-900/50" @click="toggleSection(0)">
              <div class="flex items-center gap-3">
                <span class="flex h-7 w-7 items-center justify-center border border-cyan-300/20 bg-cyan-400/10 text-xs font-semibold text-cyan-300">01</span>
                <span class="text-sm font-semibold text-white">大模型开发概述</span>
              </div>
              <span class="text-xs text-slate-500" :class="openSections[0] ? 'rotate-180' : ''"
                style="display:inline-block;transition:transform 0.2s">▼</span>
            </button>
            <div v-show="openSections[0]" class="border-t border-cyan-300/10 px-5 pb-4">
              <ul class="mt-3 space-y-2">
                <li v-for="item in section0" :key="item" class="flex items-start gap-2 text-sm text-slate-300">
                  <span class="mt-1.5 h-1 w-1 shrink-0 rounded-full bg-cyan-300/50"></span>
                  {{ item }}
                </li>
              </ul>
            </div>
          </div>

          <!-- 模块二：核心专有名词 -->
          <div class="border border-cyan-300/10 bg-slate-950/45">
            <button class="flex w-full items-center justify-between px-5 py-4 text-left transition hover:bg-slate-900/50" @click="toggleSection(1)">
              <div class="flex items-center gap-3">
                <span class="flex h-7 w-7 items-center justify-center border border-violet-300/20 bg-violet-400/10 text-xs font-semibold text-violet-300">02</span>
                <span class="text-sm font-semibold text-white">核心专有名词</span>
              </div>
              <span class="text-xs text-slate-500" :class="openSections[1] ? 'rotate-180' : ''"
                style="display:inline-block;transition:transform 0.2s">▼</span>
            </button>
            <div v-show="openSections[1]" class="border-t border-cyan-300/10 px-5 pb-4">
              <div class="mt-3 grid grid-cols-2 gap-2">
                <div v-for="term in section1" :key="term.name" class="border border-violet-300/10 bg-violet-400/5 px-3 py-2">
                  <p class="text-xs font-semibold text-violet-200">{{ term.name }}</p>
                  <p class="mt-0.5 text-xs text-slate-400">{{ term.desc }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 模块三：核心概念关系 -->
          <div class="border border-cyan-300/10 bg-slate-950/45">
            <button class="flex w-full items-center justify-between px-5 py-4 text-left transition hover:bg-slate-900/50" @click="toggleSection(2)">
              <div class="flex items-center gap-3">
                <span class="flex h-7 w-7 items-center justify-center border border-emerald-300/20 bg-emerald-400/10 text-xs font-semibold text-emerald-300">03</span>
                <span class="text-sm font-semibold text-white">核心概念关系</span>
              </div>
              <span class="text-xs text-slate-500" :class="openSections[2] ? 'rotate-180' : ''"
                style="display:inline-block;transition:transform 0.2s">▼</span>
            </button>
            <div v-show="openSections[2]" class="border-t border-cyan-300/10 px-5 pb-4">
              <ul class="mt-3 space-y-2">
                <li v-for="item in section2" :key="item.key" class="border border-emerald-300/10 bg-emerald-400/5 px-3 py-2.5">
                  <p class="text-xs font-semibold text-emerald-200">{{ item.key }}</p>
                  <p class="mt-0.5 text-xs leading-5 text-slate-400">{{ item.desc }}</p>
                </li>
              </ul>
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
const openSections = ref([true, false, false])

const totalTasks = computed(() => stages.value.length)

function toggleSection(index: number) {
  openSections.value[index] = !openSections.value[index]
}

// 大模型开发概述内容
const section0 = [
  '什么是大语言模型（LLM）',
  '大模型能做什么，不能做什么',
  '大模型开发和传统开发的区别',
  'Prompt 驱动开发',
  'Tool 驱动开发',
  'Workflow 驱动开发',
]

// 核心专有名词
const section1 = [
  { name: 'Prompt', desc: '提示词工程' },
  { name: 'Function Call', desc: 'Tool Calling' },
  { name: 'MCP', desc: '标准化工具连接' },
  { name: 'RAG', desc: '检索增强生成' },
  { name: 'Agent', desc: '自主决策循环' },
  { name: 'Workflow', desc: '固定流程编排' },
  { name: 'Context', desc: '上下文工程' },
  { name: 'Memory', desc: '长期记忆系统' },
  { name: 'Skills', desc: '技能系统' },
]

// 核心概念关系
const section2 = [
  { key: 'Prompt', desc: '让模型按要求思考' },
  { key: 'Function Call', desc: '让模型调用外部能力' },
  { key: 'RAG', desc: '给模型补充外部知识' },
  { key: 'Agent', desc: '模型 + 工具 + 决策循环' },
  { key: 'Workflow', desc: '固定流程编排，可预测执行' },
  { key: 'MCP', desc: '让模型与外部工具/资源标准化连接' },
]

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
