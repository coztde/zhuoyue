<template>
  <div class="py-2">
    <div class="flex items-start gap-2">
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-white">{{ comment.authorName }}</span>
          <span class="text-xs text-slate-500">{{ formatTime(comment.createdAt) }}</span>
        </div>
        <p class="mt-1 text-sm text-slate-300 whitespace-pre-wrap break-words">{{ comment.content }}</p>
        <div class="mt-1.5 flex items-center gap-3">
          <button
            class="flex items-center gap-1 text-xs transition"
            :class="comment.liked ? 'text-cyan-300' : 'text-slate-500 hover:text-slate-300'"
            @click="$emit('like', comment.id)">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24">
              <path d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5"/>
            </svg>
            {{ comment.likeCount }}
          </button>
          <button class="text-xs text-slate-500 hover:text-slate-300 transition" @click="$emit('reply', comment)">
            回复
          </button>
          <button class="text-xs text-rose-400/50 hover:text-rose-400 transition" @click="$emit('report', comment.id)">
            举报
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { WikiComment } from '@/types/models'

defineProps<{ comment: WikiComment; postId: string }>()
defineEmits<{
  reply: [comment: WikiComment]
  like: [commentId: string]
  report: [commentId: string]
}>()

function formatTime(iso: string): string {
  const d = new Date(iso)
  const now = new Date()
  const diff = (now.getTime() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  if (diff < 86400 * 7) return `${Math.floor(diff / 86400)} 天前`
  return d.toLocaleDateString('zh-CN')
}
</script>
