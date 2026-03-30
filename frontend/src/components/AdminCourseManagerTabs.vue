<template>
  <div class="flex flex-wrap gap-3 border border-cyan-300/10 bg-slate-950/35 p-3">
    <RouterLink
      v-for="item in tabs"
      :key="item.to"
      :to="item.to"
      class="border border-cyan-300/12 px-4 py-2 text-sm text-slate-300 transition hover:border-cyan-300/25 hover:bg-cyan-400/10 hover:text-white"
      active-class="border-cyan-300/30 bg-cyan-400/12 text-white"
    >
      {{ item.label }}
    </RouterLink>
  </div>
</template>

<script setup lang="ts">
const tabs = [
  { label: '新增阶段', to: '/admin/courses/create' },
  { label: '修改阶段', to: '/admin/courses/edit' },
  { label: '删除阶段', to: '/admin/courses/delete' },
  { label: '学生管理', to: '/admin/students' },
  { label: 'Wiki举报', to: '/admin/wiki/reports' },
]
</script>
