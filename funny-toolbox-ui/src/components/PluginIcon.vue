<template>
  <span v-if="iconType === 'emoji'" class="inline-block text-inherit">
    {{ iconValue }}
  </span>
  <i v-else-if="iconType === 'fa'" :class="iconValue" />
  <i v-else-if="iconType === 'md'" class="material-icons">{{ iconValue }}</i>
  <span v-else-if="iconType === 'svg'" class="inline-flex items-center justify-center w-full h-full" v-html="iconValue" />
  <img
    v-else-if="iconType === 'url'"
    :src="iconValue"
    :alt="alt"
    class="inline-block w-full h-full object-contain"
  />
  <span v-else class="inline-block">
    {{ defaultIcon }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  icon?: string;
  alt?: string;
  defaultIcon?: string;
}

const props = withDefaults(defineProps<Props>(), {
  icon: '',
  alt: 'icon',
  defaultIcon: '🧰',
});

// 解析图标类型和值
const iconType = computed(() => {
  if (!props.icon) return 'default';
  
  // Font Awesome: fa:fa-solid fa-hand
  if (props.icon.startsWith('fa:')) {
    return 'fa';
  }
  
  // Material Icons: md:home
  if (props.icon.startsWith('md:')) {
    return 'md';
  }
  
  // SVG: svg:<svg>...</svg> or data:image/svg+xml
  if (props.icon.startsWith('svg:') || props.icon.startsWith('data:image/svg')) {
    return 'svg';
  }
  
  // URL: http://, https://, /
  if (props.icon.startsWith('http://') || 
      props.icon.startsWith('https://') || 
      props.icon.startsWith('/')) {
    return 'url';
  }
  
  // Emoji: 单个字符或常见 emoji 模式
  if (props.icon.length <= 4 && /[\u{1F300}-\u{1F9FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/u.test(props.icon)) {
    return 'emoji';
  }
  
  // 默认当作 emoji
  return 'emoji';
});

const iconValue = computed(() => {
  if (!props.icon) return props.defaultIcon;
  
  // 移除前缀
  if (props.icon.startsWith('fa:')) {
    return props.icon.substring(3);
  }
  
  if (props.icon.startsWith('md:')) {
    return props.icon.substring(3);
  }
  
  if (props.icon.startsWith('svg:')) {
    return props.icon.substring(4);
  }
  
  return props.icon;
});
</script>

<style scoped>
/* Material Icons 支持 */
@import url('https://fonts.googleapis.com/icon?family=Material+Icons');

/* Font Awesome 支持 - 如果项目中已安装 Font Awesome，可以移除此行 */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css');

/* Font Awesome 图标尺寸 */
.fa, .fas, .far, .fal, .fab {
  font-size: 2rem;
  width: 100%;
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.material-icons {
  font-family: 'Material Icons';
  font-weight: normal;
  font-style: normal;
  font-size: 2rem;
  line-height: 1;
  letter-spacing: normal;
  text-transform: none;
  display: inline-block;
  white-space: nowrap;
  word-wrap: normal;
  direction: ltr;
  -webkit-font-smoothing: antialiased;
  vertical-align: middle;
}

/* SVG 图标尺寸约束 - 确保不会溢出父容器 */
:deep(svg) {
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
</style>
