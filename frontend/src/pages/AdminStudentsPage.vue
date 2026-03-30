<template>
  <div class="space-y-6">
    <!-- 导航标签栏 -->
    <div class="flex flex-wrap gap-3 border border-cyan-300/10 bg-slate-950/35 p-3">
      <RouterLink v-for="item in tabs" :key="item.to" :to="item.to"
        class="border border-cyan-300/12 px-4 py-2 text-sm text-slate-300 transition hover:border-cyan-300/25 hover:bg-cyan-400/10 hover:text-white"
        active-class="border-cyan-300/30 bg-cyan-400/12 text-white">
        {{ item.label }}
      </RouterLink>
    </div>

    <!-- 新增/编辑表单 -->
    <GlassPanel eyebrow="后台模块" :title="editingId ? '编辑学生' : '新增学生'" subtitle="录入学生的 GitHub / Gitee 账号">
      <form class="space-y-4 border border-cyan-300/10 bg-slate-950/35 p-5" @submit.prevent="submitForm">
        <div class="grid gap-4 md:grid-cols-2">
          <label class="space-y-2 text-sm text-slate-300">
            <span>真实姓名</span>
            <input v-model="form.realName" maxlength="50" class="w-full border border-cyan-300/10 bg-slate-950/70 px-3 py-2 text-white outline-none" />
          </label>
          <label class="space-y-2 text-sm text-slate-300">
            <span>平台</span>
            <select v-model="form.platform" class="w-full border border-cyan-300/10 bg-slate-950/70 px-3 py-2 text-white outline-none">
              <option value="GITHUB">GitHub</option>
              <option value="GITEE">Gitee</option>
            </select>
          </label>
          <label class="space-y-2 text-sm text-slate-300">
            <span>平台用户名</span>
            <input v-model="form.platformUsername" class="w-full border border-cyan-300/10 bg-slate-950/70 px-3 py-2 text-white outline-none" />
          </label>
        </div>
        <div class="flex gap-3">
          <button type="submit" :disabled="submitting"
            class="border border-cyan-300/20 bg-cyan-400/12 px-5 py-3 text-sm font-semibold text-white disabled:opacity-60">
            {{ submitting ? '保存中...' : (editingId ? '保存修改' : '新增学生') }}
          </button>
          <button v-if="editingId" type="button"
            class="border border-slate-600/40 px-5 py-3 text-sm text-slate-400 hover:text-white"
            @click="cancelEdit">取消</button>
        </div>
        <p v-if="formMessage" class="text-sm" :class="formMessageType === 'error' ? 'text-rose-200' : 'text-cyan-100'">{{ formMessage }}</p>
      </form>
    </GlassPanel>

    <!-- 学生列表 -->
    <GlassPanel eyebrow="学生档案" title="已录入学生" :subtitle="`共 ${students.length} 名学生`">
      <div class="space-y-3 border border-cyan-300/10 bg-slate-950/35 p-5">
        <div class="flex items-center justify-between">
          <p class="text-xs text-slate-500">点击「管理仓库」配置仓库，点击「AI分析」查看评分</p>
          <div class="flex gap-2">
            <button :disabled="analyzingAll"
              class="border border-violet-400/20 bg-violet-400/10 px-4 py-2 text-sm font-semibold text-violet-300 disabled:opacity-60"
              @click="doAnalyzeAll">{{ analyzingAll ? 'AI分析中...' : 'AI全量分析' }}</button>
            <button :disabled="syncingAll"
              class="border border-cyan-300/20 bg-cyan-400/12 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
              @click="doSyncAll">{{ syncingAll ? '同步中...' : '全量同步' }}</button>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-cyan-300/10 text-left text-xs text-slate-500">
                <th class="pb-2 pr-4">#</th>
                <th class="pb-2 pr-4">姓名</th>
                <th class="pb-2 pr-4">平台</th>
                <th class="pb-2 pr-4">用户名</th>
                <th class="pb-2 pr-4">完成阶段</th>
                <th class="pb-2 pr-4">最后同步</th>
                <th class="pb-2">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(s, idx) in students" :key="s.id" class="border-b border-cyan-300/5 text-slate-300 last:border-0">
                <td class="py-3 pr-4 text-slate-500">{{ idx + 1 }}</td>
                <td class="py-3 pr-4 font-medium text-white">{{ s.realName }}</td>
                <td class="py-3 pr-4">
                  <span class="border px-2 py-0.5 text-xs"
                    :class="s.platform === 'GITHUB' ? 'border-violet-400/30 text-violet-300' : 'border-orange-400/30 text-orange-300'">
                    {{ s.platform }}</span>
                </td>
                <td class="py-3 pr-4">{{ s.platformUsername }}</td>
                <td class="py-3 pr-4">{{ s.completedStages ?? 0 }}</td>
                <td class="py-3 pr-4 text-slate-500">{{ formatTime(s.lastSyncedAt) }}</td>
                <td class="py-3">
                  <div class="flex flex-wrap gap-2 text-xs">
                    <button class="text-cyan-400 hover:text-cyan-200" @click="startEdit(s)">编辑</button>
                    <button class="text-emerald-400 hover:text-emerald-200" @click="openRepoPanel(s)">管理仓库</button>
                    <button class="text-violet-400 hover:text-violet-200" @click="openProgressPanel(s)">AI分析</button>
                    <button :disabled="syncingId === s.id" class="text-sky-400 hover:text-sky-200 disabled:opacity-50" @click="doSyncOne(s.id)">{{ syncingId === s.id ? '同步中' : '立即同步' }}</button>
                    <button class="text-rose-400 hover:text-rose-200" @click="doDelete(s.id, s.realName)">删除</button>
                  </div>
                </td>
              </tr>
              <tr v-if="students.length === 0">
                <td colspan="7" class="py-8 text-center text-slate-500">暂无学生，请先新增</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </GlassPanel>

    <!-- 仓库管理面板 -->
    <GlassPanel v-if="repoStudentId" :eyebrow="repoStudentName" title="仓库管理" subtitle="配置学生关联的 Git 仓库">
      <div class="space-y-4 border border-cyan-300/10 bg-slate-950/35 p-5">
        <div class="flex flex-wrap gap-2">
          <button :disabled="repoLoading" class="border border-emerald-400/20 bg-emerald-400/10 px-3 py-1.5 text-xs text-emerald-300 disabled:opacity-50" @click="doScanRepos">一键扫描所有公开仓库</button>
          <button :disabled="repoLoading" class="border border-cyan-400/20 bg-cyan-400/10 px-3 py-1.5 text-xs text-cyan-300 disabled:opacity-50" @click="doLinkPageRepo">关联主页仓库(.github.io)</button>
          <button class="border border-slate-600/40 px-3 py-1.5 text-xs text-slate-400 hover:text-white" @click="closeRepoPanel">关闭</button>
        </div>
        <div class="flex gap-2">
          <input v-model="newRepoName" placeholder="owner/repo 格式，例如：octocat/Hello-World"
            class="flex-1 border border-cyan-300/10 bg-slate-950/70 px-3 py-2 text-sm text-white outline-none" />
          <button :disabled="repoLoading" class="border border-cyan-300/20 bg-cyan-400/12 px-4 py-2 text-sm text-white disabled:opacity-50" @click="doAddRepo">手动添加</button>
        </div>
        <div class="space-y-1">
          <div v-for="repo in repos" :key="repo.id" class="flex items-center justify-between border border-cyan-300/5 px-3 py-2">
            <div class="flex items-center gap-3">
              <span class="border border-slate-600/30 px-1.5 py-0.5 text-xs text-slate-500">{{ repoTypeLabel(repo.repoType) }}</span>
              <a :href="(repo.platform === 'GITHUB' ? 'https://github.com/' : 'https://gitee.com/') + repo.repoName"
                target="_blank" rel="noopener noreferrer"
                class="text-sm text-cyan-400 hover:text-cyan-200 hover:underline">{{ repo.repoName }}</a>
            </div>
            <div class="flex gap-3 text-xs">
              <span v-if="repo.isPrimary" class="text-amber-400">★ 主仓库</span>
              <button v-else class="text-slate-400 hover:text-amber-300" @click="doSetPrimary(repo.id)">设为主仓库</button>
              <button class="text-rose-400 hover:text-rose-200" @click="doDeleteRepo(repo.id)">删除</button>
            </div>
          </div>
          <p v-if="repos.length === 0" class="py-4 text-center text-xs text-slate-500">暂无仓库，请手动添加或一键扫描</p>
        </div>
      </div>
    </GlassPanel>

    <!-- AI 分析面板 -->
    <GlassPanel v-if="progressStudentId" :eyebrow="progressStudentName" title="AI 分析结果" subtitle="各阶段代码完成度评估">
      <div class="space-y-3 border border-cyan-300/10 bg-slate-950/35 p-5">
        <!-- 顶部工具栏：仓库选择 + 批量分析 + 关闭 -->
        <div class="flex flex-wrap items-center gap-3">
          <div class="flex items-center gap-2 flex-1">
            <span class="text-xs text-slate-400">分析仓库：</span>
            <select v-model="selectedRepoId" class="border border-cyan-300/10 bg-slate-950/70 px-2 py-1 text-xs text-white outline-none">
              <option value="">自动（主仓库）</option>
              <option v-for="r in progressRepos" :key="r.id" :value="r.id">{{ r.repoName }}{{ r.isPrimary ? ' ★' : '' }}</option>
            </select>
          </div>
          <button :disabled="analyzingAll" class="border border-violet-400/20 bg-violet-400/10 px-3 py-1.5 text-xs text-violet-300 disabled:opacity-50" @click="doAnalyzeStudent">批量分析全部阶段</button>
          <button class="border border-slate-600/40 px-3 py-1.5 text-xs text-slate-400 hover:text-white" @click="closeProgressPanel">关闭</button>
        </div>
        <div v-for="p in progressList" :key="p.id" class="border border-cyan-300/8 p-4 space-y-3">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <span class="text-sm font-medium text-white">{{ p.stageTitle }}</span>
              <span class="border px-2 py-0.5 text-xs" :class="p.isCompleted ? 'border-emerald-400/30 text-emerald-300' : 'border-slate-600/30 text-slate-500'">{{ p.isCompleted ? '已完成' : '未完成' }}</span>
              <span v-if="p.manualEditedAt" class="border border-amber-400/30 px-2 py-0.5 text-xs text-amber-300">已人工修改</span>
            </div>
            <div class="flex items-center gap-3">
              <span v-if="p.manualScore != null || p.aiScore != null" class="text-2xl font-bold"
                :class="(p.manualScore ?? p.aiScore ?? 0) >= 80 ? 'text-emerald-400' : (p.manualScore ?? p.aiScore ?? 0) >= 60 ? 'text-amber-400' : 'text-rose-400'">
                {{ p.manualScore ?? p.aiScore }}
              </span>
              <button :disabled="analyzingId === p.id"
                class="border border-violet-400/20 bg-violet-400/10 px-3 py-1 text-xs text-violet-300 disabled:opacity-50"
                @click="doAnalyzeOne(p)">{{ analyzingId === p.id ? 'AI分析中...' : 'AI分析' }}</button>
              <button class="border border-cyan-400/20 px-3 py-1 text-xs text-cyan-300" @click="startEditProgress(p)">人工修改</button>
            </div>
          </div>
          <div v-if="p.manualFeedback || p.aiFeedback" class="rounded border border-slate-700/40 bg-slate-900/50 p-3">
            <p class="text-xs text-slate-500 mb-1">{{ p.manualFeedback ? '人工评语' : 'AI评语' }}</p>
            <p class="text-sm text-slate-300 whitespace-pre-wrap">{{ p.manualFeedback ?? p.aiFeedback }}</p>
          </div>
          <div v-if="editingProgressId === p.id" class="space-y-2 border border-amber-400/20 bg-amber-400/5 p-3">
            <div class="flex items-center gap-3">
              <label class="text-xs text-slate-400">人工评分 (0-100)</label>
              <input v-model.number="manualScoreInput" type="number" min="0" max="100"
                class="w-24 border border-cyan-300/10 bg-slate-950/70 px-2 py-1 text-sm text-white outline-none" />
            </div>
            <textarea v-model="manualFeedbackInput" rows="4" placeholder="输入人工评语..."
              class="w-full border border-cyan-300/10 bg-slate-950/70 px-3 py-2 text-sm text-white outline-none resize-none" />
            <div class="flex gap-2">
              <button class="border border-cyan-300/20 bg-cyan-400/12 px-4 py-1.5 text-xs text-white" @click="saveManual(p.id)">保存</button>
              <button v-if="p.manualEditedAt" class="border border-rose-400/20 px-4 py-1.5 text-xs text-rose-300" @click="clearManual(p.id)">清除人工覆盖</button>
              <button class="border border-slate-600/40 px-4 py-1.5 text-xs text-slate-400" @click="editingProgressId = null">取消</button>
            </div>
          </div>
        </div>
        <p v-if="progressList.length === 0" class="py-4 text-center text-xs text-slate-500">暂无分析记录，请点击「AI分析」按钮</p>
      </div>
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import GlassPanel from '@/components/GlassPanel.vue'
import { useAdminLoginModal } from '@/composables/useAdminLoginModal'
import { useGlobalNotice } from '@/composables/useGlobalNotice'
import { api } from '@/services/api'
import type { StudentProfile, StudentRepo, StageProgressVO } from '@/types/models'
import { onMounted, reactive, ref } from 'vue'

const { openAdminLoginModal } = useAdminLoginModal()
const { showNotice } = useGlobalNotice()

const tabs = [
  { label: '新增阶段', to: '/admin/courses/create' },
  { label: '修改阶段', to: '/admin/courses/edit' },
  { label: '删除阶段', to: '/admin/courses/delete' },
  { label: '学生管理', to: '/admin/students' },
  { label: 'Wiki举报', to: '/admin/wiki/reports' },
]

const students = ref<StudentProfile[]>([])
const editingId = ref<string | null>(null)
const submitting = ref(false)
const syncingAll = ref(false)
const syncingId = ref<string | null>(null)
const analyzingAll = ref(false)
const formMessage = ref('')
const formMessageType = ref<'success' | 'error'>('success')

const form = reactive({
  realName: '',
  platform: 'GITHUB' as 'GITHUB' | 'GITEE',
  platformUsername: '',
})

onMounted(async () => {
  if (!api.isAdminLoggedIn()) { openAdminLoginModal(); return }
  await loadStudents()
})

async function loadStudents() {
  students.value = await api.getAdminStudents()
}

async function submitForm() {
  if (!form.realName.trim() || !form.platformUsername.trim()) {
    formMessageType.value = 'error'
    formMessage.value = '姓名和用户名均为必填'
    return
  }
  submitting.value = true
  formMessage.value = ''
  try {
    if (editingId.value) {
      await api.updateStudent(editingId.value, form)
      showNotice('学生信息已更新', 'success')
    } else {
      await api.createStudent(form)
      showNotice('学生已新增', 'success')
    }
    cancelEdit()
    await loadStudents()
  } catch (error) {
    formMessageType.value = 'error'
    formMessage.value = error instanceof Error ? error.message : '操作失败'
  } finally {
    submitting.value = false
  }
}

function startEdit(s: StudentProfile) {
  editingId.value = s.id
  form.realName = s.realName
  form.platform = s.platform
  form.platformUsername = s.platformUsername
  formMessage.value = ''
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function cancelEdit() {
  editingId.value = null
  Object.assign(form, { realName: '', platform: 'GITHUB', platformUsername: '' })
  formMessage.value = ''
}

async function doDelete(id: string, name: string) {
  if (!window.confirm(`确认删除学生「${name}」？删除后不可恢复。`)) return
  try {
    await api.deleteStudent(id)
    showNotice(`已删除学生：${name}`, 'success')
    await loadStudents()
  } catch (error) {
    showNotice(error instanceof Error ? error.message : '删除失败', 'error')
  }
}

async function doSyncAll() {
  syncingAll.value = true
  try {
    await api.syncAll()
    showNotice('全量同步完成', 'success')
    await loadStudents()
  } catch (error) {
    showNotice(error instanceof Error ? error.message : '同步失败', 'error')
  } finally {
    syncingAll.value = false
  }
}

async function doSyncOne(id: string) {
  syncingId.value = id
  try {
    await api.syncStudent(id)
    showNotice('同步完成', 'success')
    await loadStudents()
  } catch (error) {
    showNotice(error instanceof Error ? error.message : '同步失败', 'error')
  } finally {
    syncingId.value = null
  }
}

async function doAnalyzeAll() {
  analyzingAll.value = true
  try {
    await api.analyzeAll()
    showNotice('AI全量分析已在后台启动，稍后刷新查看结果', 'success')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'AI分析启动失败', 'error')
  } finally {
    analyzingAll.value = false
  }
}

// ── 仓库管理 ──────────────────────────────────────────
const repoStudentId = ref<string | null>(null)
const repoStudentName = ref('')
const repos = ref<StudentRepo[]>([])
const repoLoading = ref(false)
const newRepoName = ref('')

function repoTypeLabel(type: string) {
  const map: Record<string, string> = { MANUAL: '手动', AUTO_SCAN: '扫描', GITHUB_IO: '主页' }
  return map[type] ?? type
}

async function openRepoPanel(s: StudentProfile) {
  repoStudentId.value = s.id
  repoStudentName.value = s.realName
  progressStudentId.value = null
  repos.value = await api.getStudentRepos(s.id)
}

function closeRepoPanel() {
  repoStudentId.value = null
  repos.value = []
  newRepoName.value = ''
}

async function doAddRepo() {
  if (!newRepoName.value.trim() || !newRepoName.value.includes('/')) {
    showNotice('仓库名格式应为 owner/repo', 'error')
    return
  }
  repoLoading.value = true
  try {
    const student = students.value.find(s => s.id === repoStudentId.value)!
    await api.addStudentRepo(repoStudentId.value!, newRepoName.value.trim(), student.platform)
    newRepoName.value = ''
    repos.value = await api.getStudentRepos(repoStudentId.value!)
    showNotice('仓库已添加', 'success')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : '添加失败', 'error')
  } finally {
    repoLoading.value = false
  }
}

async function doScanRepos() {
  repoLoading.value = true
  try {
    const result = await api.scanStudentRepos(repoStudentId.value!)
    repos.value = await api.getStudentRepos(repoStudentId.value!)
    showNotice(`扫描完成，新增 ${result.added} 个仓库`, 'success')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : '扫描失败', 'error')
  } finally {
    repoLoading.value = false
  }
}

async function doLinkPageRepo() {
  repoLoading.value = true
  try {
    await api.linkPageRepo(repoStudentId.value!)
    repos.value = await api.getStudentRepos(repoStudentId.value!)
    showNotice('主页仓库已关联', 'success')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : '关联失败', 'error')
  } finally {
    repoLoading.value = false
  }
}

async function doDeleteRepo(repoId: string) {
  if (!window.confirm('确认删除该仓库？')) return
  try {
    await api.deleteStudentRepo(repoStudentId.value!, repoId)
    repos.value = await api.getStudentRepos(repoStudentId.value!)
    showNotice('已删除', 'success')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : '删除失败', 'error')
  }
}

// ── AI 分析 ──────────────────────────────────────────
const progressStudentId = ref<string | null>(null)
const progressStudentName = ref('')
const progressList = ref<StageProgressVO[]>([])
const progressRepos = ref<StudentRepo[]>([])  // AI面板中的仓库列表（用于选择）
const selectedRepoId = ref<string>('')         // 选中的仓库 ID，空字符串表示自动（主仓库）
const analyzingId = ref<string | null>(null)
const editingProgressId = ref<string | null>(null)
const manualScoreInput = ref<number | null>(null)
const manualFeedbackInput = ref('')

async function openProgressPanel(s: StudentProfile) {
  progressStudentId.value = s.id
  progressStudentName.value = s.realName
  repoStudentId.value = null
  selectedRepoId.value = ''
  ;[progressList.value, progressRepos.value] = await Promise.all([
    api.getStudentProgress(s.id),
    api.getStudentRepos(s.id),
  ])
}

function closeProgressPanel() {
  progressStudentId.value = null
  progressList.value = []
  progressRepos.value = []
  selectedRepoId.value = ''
  editingProgressId.value = null
}

// 批量分析该学生所有阶段（使用选中仓库或主仓库）
async function doAnalyzeStudent() {
  if (!progressStudentId.value) return
  analyzingAll.value = true
  try {
    const repoId = selectedRepoId.value || undefined
    await api.analyzeStudent(progressStudentId.value, repoId)
    progressList.value = await api.getStudentProgress(progressStudentId.value)
    showNotice('批量分析完成', 'success')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'AI分析失败', 'error')
  } finally {
    analyzingAll.value = false
  }
}

async function doAnalyzeOne(p: StageProgressVO) {
  analyzingId.value = p.id
  try {
    const repoId = selectedRepoId.value || undefined
    const updated = await api.analyzeStage(progressStudentId.value!, p.stageId, repoId)
    const idx = progressList.value.findIndex(x => x.id === p.id)
    if (idx >= 0) progressList.value[idx] = updated
    showNotice('AI 分析完成', 'success')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : 'AI 分析失败', 'error')
  } finally {
    analyzingId.value = null
  }
}

async function doSetPrimary(repoId: string) {
  if (!repoStudentId.value) return
  try {
    await api.setPrimaryRepo(repoStudentId.value, repoId)
    repos.value = await api.getStudentRepos(repoStudentId.value)
    showNotice('已设为主仓库', 'success')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : '设置失败', 'error')
  }
}

function startEditProgress(p: StageProgressVO) {
  editingProgressId.value = p.id
  manualScoreInput.value = p.manualScore ?? p.aiScore ?? null
  manualFeedbackInput.value = p.manualFeedback ?? p.aiFeedback ?? ''
}

async function saveManual(progressId: string) {
  try {
    const updated = await api.updateManualScore(progressId, manualScoreInput.value, manualFeedbackInput.value || null)
    const idx = progressList.value.findIndex(x => x.id === progressId)
    if (idx >= 0) progressList.value[idx] = updated
    editingProgressId.value = null
    showNotice('人工评语已保存', 'success')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : '保存失败', 'error')
  }
}

async function clearManual(progressId: string) {
  try {
    const updated = await api.updateManualScore(progressId, null, null)
    const idx = progressList.value.findIndex(x => x.id === progressId)
    if (idx >= 0) progressList.value[idx] = updated
    editingProgressId.value = null
    showNotice('已清除人工覆盖', 'success')
  } catch (error) {
    showNotice(error instanceof Error ? error.message : '操作失败', 'error')
  }
}

function formatTime(iso?: string): string {
  if (!iso) return '未同步'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}
</script>
