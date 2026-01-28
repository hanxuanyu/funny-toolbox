<script setup lang="ts">
// 使用 vue-sonner 提供的 Vue Toaster 组件，配合 toast API
import { Toaster } from 'vue-sonner'
</script>

<template>
  <!-- 右下角弹出，右下角位置 -->
  <Toaster position="bottom-right" rich-colors />
</template>
