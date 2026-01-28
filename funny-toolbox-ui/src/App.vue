<script setup lang="ts">
import { RouterView } from 'vue-router';
import { Toaster } from '@/components/ui/sonner';
</script>

<template>
  <RouterView />
  <Toaster position="bottom-right" richColors />
</template>

<style>
/* 全局样式已在 style.css 中定义 */
</style>
