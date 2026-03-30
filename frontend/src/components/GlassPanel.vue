<template>
  <section class="panel relative overflow-hidden p-5 md:p-6">
    <!-- 顶部发光线 -->
    <div class="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-cyan-300/30 to-transparent"></div>
    <!-- 右上角光晕 -->
    <div class="pointer-events-none absolute right-0 top-0 h-32 w-32 bg-cyan-400/8 blur-3xl"></div>
    <!-- 四角装饰符 -->
    <span class="pointer-events-none absolute left-1.5 top-1.5 font-code text-[10px] text-cyan-400/20">┌</span>
    <span class="pointer-events-none absolute right-1.5 top-1.5 font-code text-[10px] text-cyan-400/20">┐</span>
    <span class="pointer-events-none absolute bottom-1.5 left-1.5 font-code text-[10px] text-cyan-400/20">└</span>
    <span class="pointer-events-none absolute bottom-1.5 right-1.5 font-code text-[10px] text-cyan-400/20">┘</span>

    <!-- 标题区 -->
    <div v-if="title || subtitle" class="mb-5 flex items-start justify-between gap-4">
      <div>
        <p v-if="eyebrow" class="mb-2 font-code text-[10px] uppercase tracking-[0.45em] text-cyan-400/50">// {{ eyebrow }}</p>
        <h2 v-if="title" class="section-title">{{ title }}</h2>
        <p v-if="subtitle" class="muted-copy mt-2 max-w-xl">{{ subtitle }}</p>
      </div>
      <slot name="action" />
    </div>

    <slot />
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  subtitle?: string
  eyebrow?: string
}>()
</script>
