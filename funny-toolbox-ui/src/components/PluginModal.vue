<template>
  <Dialog v-model:open="isOpen">
    <DialogContent class="sm:max-w-[90vw] lg:max-w-[1400px] max-h-[90vh] p-0 overflow-hidden gap-0">
      <DialogHeader class="px-6 pt-4 pb-3 pr-14 border-b flex-shrink-0">
        <div class="flex justify-between items-start">
          <div class="flex-1 pr-4">
            <DialogTitle>{{ plugin?.name }}</DialogTitle>
            <DialogDescription>{{ plugin?.description }}</DialogDescription>
          </div>
          <div class="flex gap-2 flex-shrink-0">
            <Button
              variant="outline"
              size="sm"
              @click="openInNewWindow"
              title="在新窗口中打开"
            >
              <ExternalLink class="h-4 w-4" />
            </Button>
          </div>
        </div>
      </DialogHeader>
      
      <div class="flex-1 overflow-hidden">
        <!-- 加载状态 -->
        <div 
          v-if="isLoading" 
          class="flex items-center justify-center h-[70vh] text-gray-500"
        >
          <div class="text-center">
            <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 mb-2"></div>
            <p>加载中...</p>
          </div>
        </div>
        
        <!-- iframe容器 -->
        <div v-show="!isLoading" class="h-full">
          <iframe
            v-if="iframeUrl"
            ref="iframeRef"
            :src="iframeUrl"
            class="w-full h-[70vh] border-0 block"
            :title="plugin?.name"
            @load="handleIframeLoad"
          />
          <div v-else class="flex items-center justify-center h-[70vh] text-gray-500">
            此插件暂无前端页面
          </div>
        </div>
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { Plugin } from '@/api';
import { getBackendBaseURL } from '@/api/request';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { ExternalLink } from 'lucide-vue-next';

interface Props {
  plugin: Plugin | null;
  open: boolean;
}

interface Emits {
  (e: 'update:open', value: boolean): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const isLoading = ref(false);
const iframeRef = ref<HTMLIFrameElement | null>(null);

const isOpen = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
});

// 监听弹窗打开，重置加载状态
watch(() => props.open, (newVal) => {
  if (newVal && iframeUrl.value) {
    isLoading.value = true;
  }
});

// iframe加载完成处理
const handleIframeLoad = () => {
  // 延迟一小段时间确保内容完全渲染
  setTimeout(() => {
    isLoading.value = false;
  }, 100);
};

const iframeUrl = computed(() => {
  if (!props.plugin?.frontendEntry) {
    return null;
  }
  
  const backendBaseURL = getBackendBaseURL();
  
  // 如果是完整 URL，直接使用
  if (props.plugin.frontendEntry.startsWith('http://') || 
      props.plugin.frontendEntry.startsWith('https://')) {
    return props.plugin.frontendEntry;
  }
  
  // frontendEntry 通常是 /plugins/xxx/index.html 格式
  // 需要拼接后端地址
  if (props.plugin.frontendEntry.startsWith('/')) {
    return backendBaseURL + props.plugin.frontendEntry;
  }
  
  // 其他情况，使用后端地址 + frontendEntry
  return `${backendBaseURL}/${props.plugin.frontendEntry}`;
});

const openInNewWindow = () => {
  if (iframeUrl.value) {
    window.open(iframeUrl.value, '_blank');
    isOpen.value = false;
  }
};
</script>

<style scoped>
/* 自定义滚动条样式 - 仅在滚动时显示 */
.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
  transition: scrollbar-color 0.3s ease;
}

.custom-scrollbar:hover {
  scrollbar-color: rgba(155, 155, 155, 0.5) transparent;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: transparent;
  border-radius: 4px;
  transition: background-color 0.3s ease;
}

.custom-scrollbar:hover::-webkit-scrollbar-thumb {
  background-color: rgba(155, 155, 155, 0.5);
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(155, 155, 155, 0.7);
}
</style>
