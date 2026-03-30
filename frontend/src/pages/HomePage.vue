<template>
  <div class="space-y-6">
    <!-- 顶部 KPI 卡片 -->
    <div class="grid grid-cols-2 gap-4 sm:grid-cols-4">
      <div v-for="kpi in kpis" :key="kpi.label" class="panel relative overflow-hidden p-5">
        <div class="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-cyan-300/25 to-transparent"></div>
        <p class="text-xs uppercase tracking-widest text-cyan-200/50">{{ kpi.label }}</p>
        <p class="mt-2 text-3xl font-bold text-white">{{ kpi.value }}</p>
        <p class="mt-1 text-xs" :class="kpi.up ? 'text-emerald-400' : 'text-slate-500'">{{ kpi.desc }}</p>
      </div>
    </div>

    <!-- 折线图 + 整体进度 -->
    <div class="grid gap-6 lg:grid-cols-3">
      <GlassPanel eyebrow="提交趋势" title="总提交数量" class="lg:col-span-2">
        <template #action>
          <div class="flex items-center gap-2 text-xs text-slate-400">
            <span>统计天数</span>
            <select v-model="chartDays" class="border border-cyan-300/20 bg-slate-900 px-2 py-1 text-cyan-200 outline-none">
              <option :value="7">7 天</option>
              <option :value="14">14 天</option>
              <option :value="30">30 天</option>
              <option :value="60">60 天</option>
            </select>
          </div>
        </template>
        <div ref="lineChartEl" class="h-56 w-full" />
      </GlassPanel>

      <GlassPanel eyebrow="任务进度" title="整体完成情况">
        <div class="space-y-4">
          <div class="flex items-end justify-between">
            <span class="text-4xl font-bold text-cyan-300">{{ overallPct }}%</span>
            <span class="text-sm text-slate-400">{{ completedTasks }} / {{ totalTasks }} 人次完成</span>
          </div>
          <div class="h-2 w-full overflow-hidden rounded-full bg-slate-800">
            <div class="h-full rounded-full bg-gradient-to-r from-cyan-500 to-cyan-300 transition-all duration-700" :style="{ width: overallPct + '%' }" />
          </div>
          <div class="space-y-3 pt-2">
            <div v-for="stage in stageProgress" :key="stage.name" class="space-y-1">
              <div class="flex justify-between text-xs">
                <span class="text-slate-300">{{ stage.name }}</span>
                <span class="text-slate-500">{{ stage.done }}/{{ stage.total }}</span>
              </div>
              <div class="h-1.5 w-full overflow-hidden rounded-full bg-slate-800">
                <div class="h-full rounded-full bg-cyan-400/70 transition-all" :style="{ width: stage.total > 0 ? (stage.done / stage.total * 100) + '%' : '0%' }" />
              </div>
            </div>
          </div>
        </div>
      </GlassPanel>
    </div>

    <!-- 活动流 + 推荐栏 -->
    <div class="grid gap-6 lg:grid-cols-3">
      <GlassPanel eyebrow="实时动态" title="Git 活动流" class="lg:col-span-2">
        <div class="max-h-80 space-y-0 overflow-y-auto pr-1">
          <div
            v-for="(ev, i) in activityFeed"
            :key="i"
            class="flex items-start gap-3 border-b border-cyan-300/5 py-3 last:border-0"
          >
            <span class="mt-0.5 text-base">{{ eventIcon(ev.type) }}</span>
            <div class="min-w-0 flex-1">
              <p class="text-sm text-white">
                <span class="font-semibold text-cyan-300">{{ ev.actor }}</span>
                {{ eventVerb(ev.type) }}
                <span class="font-medium text-slate-200">{{ ev.target }}</span>
              </p>
              <p class="text-xs text-slate-500">{{ ev.time }}</p>
            </div>
            <span class="shrink-0 rounded px-1.5 py-0.5 text-xs font-medium" :class="eventBadge(ev.type)">{{ ev.type }}</span>
          </div>
        </div>
      </GlassPanel>

      <GlassPanel eyebrow="精选内容" title="推荐栏">
        <div class="space-y-3">
          <div
            v-for="(rec, i) in sortedRecommendations"
            :key="rec.id"
            class="group relative flex items-start gap-3 border border-cyan-300/8 bg-slate-950/40 p-3 transition hover:border-cyan-300/20"
          >
            <button
              class="absolute right-2 top-2 opacity-0 transition group-hover:opacity-100"
              :class="rec.pinned ? 'opacity-100 text-amber-400' : 'text-slate-600 hover:text-amber-400'"
              @click="togglePin(i)"
              title="置顶"
            >★</button>
            <span class="mt-0.5 text-lg">{{ rec.type === 'repo' ? '📦' : '📄' }}</span>
            <div class="min-w-0 flex-1 pr-4">
              <p class="truncate text-sm font-semibold text-white">{{ rec.title }}</p>
              <p class="mt-0.5 truncate text-xs text-slate-500">{{ rec.desc }}</p>
              <div class="mt-1.5 flex items-center gap-2">
                <span v-if="rec.pinned" class="rounded bg-amber-500/15 px-1.5 py-0.5 text-xs text-amber-400">置顶</span>
                <span class="text-xs text-slate-600">{{ rec.meta }}</span>
              </div>
            </div>
          </div>
        </div>
      </GlassPanel>
    </div>
  </div>
</template>
<script setup lang="ts">
import GlassPanel from '@/components/GlassPanel.vue'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

// ── KPI 卡片 ────────────────────────────────────────────
const kpis = [
  { label: '总提交数', value: '1,284', desc: '↑ 较昨日 +23', up: true },
  { label: '活跃学生', value: '38', desc: '共 42 人', up: true },
  { label: '任务完成率', value: '67%', desc: '↑ 较上周 +5%', up: true },
  { label: 'Wiki 文章', value: '56', desc: '本周新增 4 篇', up: false },
]

// ── 折线图 ────────────────────────────────────────────
const chartDays = ref<number>(14)
const lineChartEl = ref<HTMLElement | null>(null)
let lineChart: echarts.ECharts | null = null

function generateCommitData(days: number) {
  const labels: string[] = []
  const data: number[] = []
  const now = new Date()
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    labels.push(`${d.getMonth() + 1}/${d.getDate()}`)
    data.push(Math.floor(Math.random() * 40 + 10))
  }
  return { labels, data }
}

function renderLineChart() {
  if (!lineChartEl.value) return
  if (!lineChart) lineChart = echarts.init(lineChartEl.value, 'dark')
  const { labels, data } = generateCommitData(chartDays.value)
  lineChart.setOption({
    backgroundColor: 'transparent',
    grid: { top: 20, right: 16, bottom: 28, left: 40 },
    tooltip: { trigger: 'axis', backgroundColor: '#0f172a', borderColor: '#22d3ee33' },
    xAxis: { type: 'category', data: labels, axisLine: { lineStyle: { color: '#334155' } }, axisLabel: { color: '#64748b', fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#1e293b' } }, axisLabel: { color: '#64748b', fontSize: 11 } },
    series: [{
      type: 'line',
      data,
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { color: '#22d3ee', width: 2 },
      itemStyle: { color: '#22d3ee' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(34,211,238,0.25)' }, { offset: 1, color: 'rgba(34,211,238,0)' }]) },
    }],
  })
}

watch(chartDays, async () => { await nextTick(); renderLineChart() })

// ── 整体进度 ────────────────────────────────────────────
const stageProgress = [
  { name: '阶段一：环境搭建', done: 40, total: 42 },
  { name: '阶段二：基础开发', done: 35, total: 42 },
  { name: '阶段三：功能联调', done: 22, total: 42 },
  { name: '阶段四：项目答辩', done: 8, total: 42 },
]
const totalTasks = computed(() => stageProgress.reduce((s, x) => s + x.total, 0))
const completedTasks = computed(() => stageProgress.reduce((s, x) => s + x.done, 0))
const overallPct = computed(() => Math.round(completedTasks.value / totalTasks.value * 100))

// ── 活动流 ────────────────────────────────────────────
type EventType = 'commit' | 'wiki' | 'complete'
interface FeedEvent { type: EventType; actor: string; target: string; time: string }
const activityFeed = ref<FeedEvent[]>([
  { type: 'commit', actor: '张三', target: 'feature/user-auth', time: '3 分钟前' },
  { type: 'wiki', actor: '李四', target: 'Vue3 组合式 API 实践', time: '12 分钟前' },
  { type: 'complete', actor: '王五', target: '阶段二：基础开发', time: '25 分钟前' },
  { type: 'commit', actor: '赵六', target: 'fix/login-redirect', time: '41 分钟前' },
  { type: 'commit', actor: '孙七', target: 'refactor/api-layer', time: '1 小时前' },
  { type: 'wiki', actor: '周八', target: 'Spring Boot 踩坑记录', time: '1 小时前' },
  { type: 'complete', actor: '吴九', target: '阶段一：环境搭建', time: '2 小时前' },
  { type: 'commit', actor: '郑十', target: 'docs/readme-update', time: '2 小时前' },
  { type: 'commit', actor: '张三', target: 'chore/ci-config', time: '3 小时前' },
  { type: 'wiki', actor: '李四', target: 'Git 工作流最佳实践', time: '4 小时前' },
])

function eventIcon(t: EventType) { return t === 'commit' ? '⬡' : t === 'wiki' ? '📝' : '✅' }
function eventVerb(t: EventType) { return t === 'commit' ? '提交了' : t === 'wiki' ? '发布了 Wiki' : '完成了' }
function eventBadge(t: EventType) {
  return t === 'commit' ? 'bg-cyan-400/10 text-cyan-300'
       : t === 'wiki' ? 'bg-violet-400/10 text-violet-300'
       : 'bg-emerald-400/10 text-emerald-300'
}

// ── 推荐栏 ────────────────────────────────────────────
interface Recommendation { id: number; type: 'repo' | 'wiki'; title: string; desc: string; meta: string; pinned: boolean }
const recommendations = ref<Recommendation[]>([
  { id: 1, type: 'repo', title: 'zhuoyue-platform', desc: '卓越班数字化管理平台主仓库', meta: '42 commits', pinned: true },
  { id: 2, type: 'wiki', title: 'Vue3 组合式 API 实践', desc: '详解 setup、ref、computed 使用场景', meta: '李四 · 3 天前', pinned: false },
  { id: 3, type: 'repo', title: 'spring-boot-demo', desc: '后端开发参考示例项目', meta: '18 commits', pinned: false },
  { id: 4, type: 'wiki', title: 'Git 工作流最佳实践', desc: '团队协作 branch 策略与 PR 规范', meta: '周八 · 昨天', pinned: false },
  { id: 5, type: 'wiki', title: 'Spring Boot 踩坑记录', desc: 'Bean 注入、事务、跨域常见问题', meta: '吴九 · 5 天前', pinned: false },
])

const sortedRecommendations = computed(() =>
  [...recommendations.value].sort((a, b) => (b.pinned ? 1 : 0) - (a.pinned ? 1 : 0))
)

function togglePin(sortedIdx: number) {
  const item = sortedRecommendations.value[sortedIdx]
  const orig = recommendations.value.find(r => r.id === item.id)
  if (orig) orig.pinned = !orig.pinned
}

// ── 定时刷新 ────────────────────────────────────────────
let timer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await nextTick()
  renderLineChart()
  window.addEventListener('resize', () => lineChart?.resize())
  timer = setInterval(() => { renderLineChart() }, 30_000)
})

onUnmounted(() => {
  lineChart?.dispose()
  window.removeEventListener('resize', () => lineChart?.resize())
  if (timer) clearInterval(timer)
})
</script>
