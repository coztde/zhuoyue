<template>
  <Transition name="global-notice-fade">
    <div
      v-if="visible"
      class="fixed right-6 top-6 z-[70] max-w-md border px-4 py-3 text-sm shadow-[0_18px_60px_rgba(2,8,23,0.45)] backdrop-blur-md"
      :class="noticeClass"
    >
      <div class="flex items-start justify-between gap-4">
        <p class="leading-6">{{ message }}</p>
        <button class="text-inherit/80 transition hover:text-white" type="button" @click="hideNotice">×</button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useGlobalNotice } from '@/composables/useGlobalNotice'

const { visible, message, type, hideNotice } = useGlobalNotice()

const noticeClass = computed(() => {
  if (type.value === 'success') {
    return 'border-emerald-300/30 bg-emerald-400/12 text-emerald-50'
  }
  if (type.value === 'error') {
    return 'border-rose-300/30 bg-rose-400/12 text-rose-50'
  }
  return 'border-cyan-300/30 bg-slate-950/88 text-cyan-50'
})
</script>

<style scoped>

</style>
