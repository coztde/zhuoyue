<template>
  <div class="space-y-8">
    <!-- 顶部标题栏 -->
    <GlassPanel eyebrow="LEADERBOARD" title="卷王榜 · 课程阶段完成排行" :subtitle="lastSyncText">
      <div class="space-y-8 p-2">

        <!-- Top 3 大卡片 -->
        <div v-if="top3.length > 0" class="grid gap-4 sm:grid-cols-3">
          <div
            v-for="(item, idx) in top3"
            :key="item.id"
            class="flex flex-col items-center gap-3 border bg-slate-950/40 p-6 text-center transition hover:bg-slate-950/60"
            :class="idx === 0 ? 'border-yellow-400/30' : idx === 1 ? 'border-slate-400/25' : 'border-orange-400/20'"
          >
            <span class="font-code text-xs tracking-widest" :class="idx === 0 ? 'text-yellow-400/80' : idx === 1 ? 'text-slate-400/80' : 'text-orange-400/70'">#{{ idx + 1 }} {{ rankLabel[idx] }}</span>
            <img
              v-if="item.avatarUrl"
              :src="item.avatarUrl"
              :alt="item.displayName || item.realName"
              class="h-16 w-16 rounded-full border border-cyan-300/20 object-cover"
              referrerpolicy="no-referrer"
            />
            <div v-else class="flex h-16 w-16 items-center justify-center rounded-full border border-cyan-300/20 bg-slate-800 text-2xl font-bold text-cyan-300">
              {{ (item.displayName || item.realName).charAt(0) }}
            </div>
            <p class="font-semibold text-white">{{ item.displayName || item.realName }}</p>
            <p v-if="item.displayName" class="text-xs text-slate-500">{{ item.realName }}</p>
            <p class="font-code text-3xl font-medium text-cyan-300">{{ item.completedStages }}<span class="text-sm text-slate-500">/{{ item.totalStages }}</span></p>
            <p class="font-code text-[10px] uppercase tracking-widest text-slate-500">STAGES DONE</p>
            <p v-if="item.latestCommit" class="text-xs text-slate-500">{{ formatTime(item.latestCommit) }}</p>
          </div>
        </div>

        <!-- 4~10 名列表 -->
        <div v-if="rest.length > 0" class="border border-cyan-300/8 bg-slate-950/35">
          <div
            v-for="(item, idx) in rest"
            :key="item.id"
            class="flex items-center gap-4 border-b border-cyan-300/5 px-4 py-3 last:border-0"
          >
            <span class="font-code w-8 text-center text-xs text-slate-600">#{{ idx + 4 }}</span>
            <img
              v-if="item.avatarUrl"
              :src="item.avatarUrl"
              :alt="item.displayName || item.realName"
              class="h-9 w-9 flex-shrink-0 rounded-full border border-cyan-300/20 object-cover"
              referrerpolicy="no-referrer"
            />
            <div v-else class="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full border border-cyan-300/20 bg-slate-800 text-sm font-bold text-cyan-300">
              {{ (item.displayName || item.realName).charAt(0) }}
            </div>
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium text-white">{{ item.displayName || item.realName }}</p>
              <div class="mt-1 flex items-center gap-2">
                <div class="h-px flex-1 overflow-hidden bg-slate-800">
                  <div
                    class="h-full bg-cyan-400/60 transition-all"
                    :style="{ width: item.totalStages > 0 ? `${(item.completedStages / item.totalStages) * 100}%` : '0%' }"
                  />
                </div>
                <span class="text-xs text-slate-400">{{ item.completedStages }}/{{ item.totalStages }}</span>
              </div>
            </div>
            <p class="flex-shrink-0 text-xs text-slate-500">{{ formatTime(item.latestCommit) }}</p>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && top10.length === 0" class="py-16 text-center">
          <p class="font-code text-xs text-cyan-400/30 mb-2">// NO_DATA</p>
          <p class="text-sm text-slate-500">榜单数据加载中，请管理员先录入学生并触发同步</p>
        </div>
        <div v-if="loading" class="flex items-center justify-center gap-3 py-16">
          <span class="h-4 w-4 animate-spin rounded-full border-2 border-cyan-300/20 border-t-cyan-300"></span>
          <span class="font-code text-xs text-slate-500">LOADING...</span>
        </div>
      </div>
    </GlassPanel>

    <!-- ECharts 各阶段完成人数分布 -->
    <GlassPanel v-if="fullList.length > 0" eyebrow="ANALYTICS" title="各阶段完成人数分布" subtitle="统计全员在每个课程阶段的完成情况">
      <div ref="chartEl" class="h-64 w-full px-2 py-4" />
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import GlassPanel from '@/components/GlassPanel.vue'
import { api } from '@/services/api'
import type { LeaderboardItem } from '@/types/models'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const medals = ['🥇', '🥈', '🥉']
const rankLabel = ['CHAMPION', 'RUNNER-UP', 'THIRD']

const loading = ref(true)
const top10 = ref<LeaderboardItem[]>([])
const fullList = ref<LeaderboardItem[]>([])
const chartEl = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const top3 = computed(() => top10.value.slice(0, 3))
const rest = computed(() => top10.value.slice(3))

const lastSyncText = computed(() => {
  const t = top10.value[0]?.lastSyncedAt
  return t ? `最后同步：${formatTime(t)}` : '数据来自 GitHub / Gitee 实时同步'
})

onMounted(async () => {
  await loadData()
})

onUnmounted(() => {
  chartInstance?.dispose()
})

async function loadData() {
  loading.value = true
  try {
    const [board, full] = await Promise.all([
      api.getLeaderboard(),
      api.getLeaderboardFull(),
    ])
    top10.value = board
    fullList.value = full
  } catch {
    // 接口不可用时静默失败，页面展示空状态
  } finally {
    loading.value = false
  }
}

watch(fullList, async (list) => {
  if (list.length === 0) return
  await nextTick()
  renderChart(list)
})

/**
 * 各阶段完成人数 = 完成阶段数 >= n 的学生数量（近似，后续可扩展精确阶段字段）
 */
function renderChart(list: LeaderboardItem[]) {
  if (!chartEl.value) return
  const totalStages = list[0]?.totalStages ?? 0
  if (totalStages === 0) return

  const stageCounts = Array.from({ length: totalStages }, (_, i) =>
    list.filter(s => s.completedStages > i).length
  )
  const stageLabels = Array.from({ length: totalStages }, (_, i) => `阶段 ${i + 1}`)

  if (!chartInstance) {
    chartInstance = echarts.init(chartEl.value, 'dark')
  }
  chartInstance.setOption({
    backgroundColor: 'transparent',
    grid: { left: 60, right: 20, top: 10, bottom: 20 },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { lineStyle: { color: '#334155' } },
      splitLine: { lineStyle: { color: '#1e293b' } },
      axisLabel: { color: '#64748b', fontSize: 11 },
    },
    yAxis: {
      type: 'category',
      data: stageLabels,
      axisLabel: { color: '#94a3b8', fontSize: 11 },
      axisLine: { lineStyle: { color: '#334155' } },
    },
    series: [{
      type: 'bar',
      data: stageCounts,
      itemStyle: { color: 'rgba(34,211,238,0.55)', borderRadius: 2 },
      label: { show: true, position: 'right', color: '#94a3b8', fontSize: 11, formatter: '{c} 人' },
    }],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  })
}

function formatTime(iso?: string): string {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}
</script>
