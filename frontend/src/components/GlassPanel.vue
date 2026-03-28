<template>
  <section class="panel relative overflow-hidden p-5 md:p-6">
    <div class="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-cyan-300/25 to-transparent"></div>
    <div class="pointer-events-none absolute right-0 top-0 h-28 w-28 rounded-full bg-cyan-400/10 blur-3xl"></div>
    <div v-if="title || subtitle" class="mb-5 flex items-start justify-between gap-4">
      <div>
        <p v-if="eyebrow" class="mb-2 font-display text-xs uppercase tracking-[0.45em] text-cyan-200/55">{{ eyebrow }}</p>
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
