<template>
  <Teleport to="body">
    <Transition name="confirm-dialog-fade">
      <div
        v-if="visible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 px-4 backdrop-blur-md"
        @click.self="$emit('cancel')"
      >
        <div class="w-full max-w-lg border border-cyan-300/15 bg-slate-950/92 p-6 shadow-[0_30px_120px_rgba(2,8,23,0.72)]">
          <p class="text-xs uppercase tracking-[0.36em] text-cyan-100/55">{{ eyebrow }}</p>
          <h3 class="mt-3 text-2xl font-semibold text-white">{{ title }}</h3>
          <p class="mt-4 text-base leading-7 text-slate-300">{{ message }}</p>

          <div class="mt-8 flex justify-end gap-3">
            <button
              class="border border-cyan-300/12 bg-slate-950/40 px-5 py-2.5 text-sm font-semibold text-slate-300 transition hover:bg-slate-900/70 hover:text-white"
              type="button"
              @click="$emit('cancel')"
            >
              {{ cancelText }}
            </button>
            <button
              class="border border-rose-300/20 bg-rose-400/12 px-5 py-2.5 text-sm font-semibold text-rose-50 transition hover:bg-rose-400/20"
              type="button"
              @click="$emit('confirm')"
            >
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    visible: boolean
    title: string
    message: string
    eyebrow?: string
    confirmText?: string
    cancelText?: string
  }>(),
  {
    eyebrow: '操作确认',
    confirmText: '确认删除',
    cancelText: '取消',
  },
)

defineEmits<{
  confirm: []
  cancel: []
}>()
</script>

<style scoped>

</style>
