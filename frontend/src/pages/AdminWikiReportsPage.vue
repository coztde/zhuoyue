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

    <GlassPanel eyebrow="后台模块" title="Wiki 举报审核" :subtitle="`共 ${total} 条未处理举报`">
      <div class="space-y-3 border border-cyan-300/10 bg-slate-950/35 p-5">
        <div v-if="loading" class="py-8 text-center text-slate-500 text-sm">加载中...</div>
        <div v-else-if="reports.length === 0" class="py-8 text-center text-slate-500 text-sm">暂无未处理举报</div>
        <table v-else class="w-full text-sm">
          <thead>
            <tr class="border-b border-cyan-300/10 text-left text-xs text-slate-500">
              <th class="pb-2 pr-4">#</th>
              <th class="pb-2 pr-4">类型</th>
              <th class="pb-2 pr-4">目标ID</th>
              <th class="pb-2 pr-4">举报原因</th>
              <th class="pb-2 pr-4">时间</th>
              <th class="pb-2">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(r, idx) in reports" :key="r.id" class="border-b border-cyan-300/5 text-slate-300 last:border-0">
              <td class="py-3 pr-4 text-slate-500">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3 pr-4">
                <span class="border px-2 py-0.5 text-xs"
                  :class="r.targetType === 'POST' ? 'border-violet-400/30 text-violet-300' : 'border-orange-400/30 text-orange-300'">
                  {{ r.targetType === 'POST' ? '文章' : '评论' }}
                </span>
              </td>
              <td class="py-3 pr-4 font-mono text-xs">
                <RouterLink :to="`/wiki/${r.targetType === 'POST' ? r.targetId : r.postId}`" class="text-cyan-400 hover:text-cyan-200 underline underline-offset-2">
                  {{ r.targetType === 'POST' ? `文章#${r.targetId}` : `评论#${r.targetId}` }}
                </RouterLink>
              </td>
              <td class="py-3 pr-4 max-w-xs truncate">{{ r.reason || '（未填写）' }}</td>
              <td class="py-3 pr-4 text-slate-500 text-xs">{{ formatTime(r.createdAt) }}</td>
              <td class="py-3">
                <div class="flex flex-wrap gap-2 text-xs">
                  <button class="text-rose-400 hover:text-rose-200"
                    @click="doDelete(r)">删除内容</button>
                  <button class="text-emerald-400 hover:text-emerald-200"
                    @click="doHandle(r.id)">忽略</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- 分页 -->
        <div v-if="totalPages > 1" class="flex justify-center gap-2 pt-2">
          <button v-for="p in totalPages" :key="p"
            class="w-8 h-8 border text-sm transition"
            :class="p === currentPage ? 'border-cyan-300/40 text-white bg-cyan-400/12' : 'border-cyan-300/10 text-slate-400 hover:text-white'"
            @click="loadReports(p)">{{ p }}</button>
        </div>
      </div>
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import GlassPanel from '@/components/GlassPanel.vue'
import { api } from '@/services/api'
import { useGlobalNotice } from '@/composables/useGlobalNotice'
import type { WikiReport } from '@/types/models'

const { showNotice } = useGlobalNotice()

const tabs = [
  { to: '/admin/courses/create', label: '课程管理' },
  { to: '/admin/students', label: '学生管理' },
  { to: '/admin/wiki/reports', label: 'Wiki举报' },
]

const reports = ref<WikiReport[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const totalPages = ref(1)
const pageSize = 20

async function loadReports(page = 1) {
  loading.value = true
  try {
    const result = await api.adminGetWikiReports(page, pageSize)
    reports.value = result.records as WikiReport[]
    total.value = result.total
    totalPages.value = result.pages
    currentPage.value = result.current
  } catch (e: any) {
    showNotice(e.message || '加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function doHandle(id: string) {
  try {
    await api.adminHandleWikiReport(id)
    showNotice('已标记为已处理', 'success')
    await loadReports(currentPage.value)
  } catch (e: any) {
    showNotice(e.message || '操作失败', 'error')
  }
}

async function doDelete(r: WikiReport) {
  try {
    if (r.targetType === 'POST') {
      await api.adminDeleteWikiPost(r.targetId)
    } else {
      await api.adminDeleteWikiComment(r.targetId)
    }
    await api.adminHandleWikiReport(r.id)
    showNotice('内容已删除', 'success')
    await loadReports(currentPage.value)
  } catch (e: any) {
    showNotice(e.message || '操作失败', 'error')
  }
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('zh-CN')
}

onMounted(() => loadReports())
</script>
