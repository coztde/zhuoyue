<template>
  <AppShell>
    <RouterView />
  </AppShell>
  <GlobalNotice />
</template>

<script setup lang="ts">
import AppShell from '@/components/AppShell.vue'
import GlobalNotice from '@/components/GlobalNotice.vue'
</script>
