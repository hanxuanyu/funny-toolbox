<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Badge } from '@/components/ui/badge'
import { 
  Code2, 
  Braces, 
  Lock, 
  Image, 
  Dices,
  Clock,
  Palette,
  ExternalLink 
} from 'lucide-vue-next'

// 检测是否为开发环境
const isDev = import.meta.env.DEV

// 根据环境生成工具路径
const getToolHref = (toolId: string) => {
  if (isDev) {
    // 开发模式：使用 public 目录下的 HTML 文件
    return `/tool-${toolId}.html`
  } else {
    // 生产模式：使用打包后的相对路径
    return `./tools/${toolId}/index.html`
  }
}

const tools = [
  {
    id: 'encoding-tool',
    name: '编码工具',
    description: '支持 Base64、URL、HTML、Unicode、十六进制、二进制及哈希加密等多种编码方式。',
    href: getToolHref('encoding-tool'),
    icon: Code2,
    color: 'from-blue-500 to-cyan-500',
    tags: ['编码', '解码', '哈希'],
  },
  {
    id: 'json-formatter',
    name: 'JSON 工具箱',
    description: '格式化、压缩、验证、JSONPath 查询、结构分析等多功能 JSON 处理工具。',
    href: getToolHref('json-formatter'),
    icon: Braces,
    color: 'from-green-500 to-emerald-500',
    tags: ['JSON', '格式化', '验证'],
  },
  {
    id: 'crypto-tool',
    name: '加解密工具',
    description: '支持 AES、DES、3DES、RC4、Rabbit 等对称加密，以及 MD5、SHA、HMAC 等哈希算法。',
    href: getToolHref('crypto-tool'),
    icon: Lock,
    color: 'from-red-500 to-pink-500',
    tags: ['加密', '解密', '安全'],
  },
  {
    id: 'datetime-tool',
    name: '时间日期工具',
    description: '时间戳转换、时区转换、日期计算、格式化等多功能时间处理工具。',
    href: getToolHref('datetime-tool'),
    icon: Clock,
    color: 'from-blue-500 to-indigo-500',
    tags: ['时间', '日期', '转换'],
    isNew: true,
  },
  {
    id: 'ascii-art-tool',
    name: '字符画生成器',
    description: '将文本或图片转换为精美的 ASCII 字符画，支持多种字体和自定义字符集。',
    href: getToolHref('ascii-art-tool'),
    icon: Image,
    color: 'from-purple-500 to-violet-500',
    tags: ['ASCII', '字符画', '艺术'],
  },
  {
    id: 'decision-simulator',
    name: '决策模拟器',
    description: '抛硬币、掷骰子、转盘、随机选择、神谕等多种随机决策工具,帮助你做出选择。',
    href: getToolHref('decision-simulator'),
    icon: Dices,
    color: 'from-amber-500 to-orange-500',
    tags: ['随机', '决策', '娱乐'],
  },
  {
    id: 'color-tool',
    name: '颜色转换工具',
    description: '支持 HEX、RGB、HSL、HSV 等多种颜色格式的互相转换，包含颜色拾取器。',
    href: getToolHref('color-tool'),
    icon: Palette,
    color: 'from-pink-500 to-rose-500',
    tags: ['颜色', '转换', '拾色'],
    isNew: true,
  },
]
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50 dark:from-slate-950 dark:via-blue-950 dark:to-indigo-950">
    <main class="mx-auto flex max-w-6xl flex-col gap-8 px-4 py-12">
      <!-- Header Section -->
      <section class="flex flex-col gap-3 text-center">
        <div class="flex items-center justify-center gap-3 mb-2">
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-600 to-purple-600 flex items-center justify-center shadow-lg">
            <svg class="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
            </svg>
          </div>
          <h1 class="text-4xl font-bold bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 bg-clip-text text-transparent">
            Funny Toolbox
          </h1>
        </div>
        <p class="text-lg text-slate-600 dark:text-slate-400 max-w-2xl mx-auto">
          精心打造的在线工具合集，简单易用，功能强大
        </p>
        <div class="flex items-center justify-center gap-2 mt-2">
          <Badge variant="secondary" class="px-3 py-1">
            <span class="mr-1">⚡</span> 纯前端
          </Badge>
          <Badge variant="secondary" class="px-3 py-1">
            <span class="mr-1">🎨</span> 现代化UI
          </Badge>
          <Badge variant="secondary" class="px-3 py-1">
            <span class="mr-1">🚀</span> 单页应用
          </Badge>
        </div>
      </section>

      <Separator class="my-4" />

      <!-- Tools Grid -->
      <section class="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        <Card
          v-for="tool in tools"
          :key="tool.id"
          class="group relative overflow-hidden transition-all duration-300 hover:shadow-xl hover:scale-[1.02] hover:-translate-y-1 border-2 hover:border-blue-200 dark:hover:border-blue-800"
        >
          <!-- Gradient Background -->
          <div 
            :class="['absolute inset-0 opacity-0 group-hover:opacity-10 transition-opacity duration-300 bg-gradient-to-br', tool.color]"
          ></div>
          
          <!-- New Badge -->
          <Badge 
            v-if="tool.isNew" 
            variant="destructive" 
            class="absolute top-3 right-3 z-10 animate-pulse"
          >
            NEW
          </Badge>

          <div class="relative flex flex-col gap-4 p-6">
            <!-- Icon and Title -->
            <div class="flex items-start gap-4">
              <div 
                :class="['w-14 h-14 rounded-xl flex items-center justify-center text-white shadow-lg bg-gradient-to-br', tool.color]"
              >
                <component :is="tool.icon" :size="28" />
              </div>
              <div class="flex-1">
                <h2 class="text-xl font-semibold leading-tight mb-1">{{ tool.name }}</h2>
                <div class="flex flex-wrap gap-1.5">
                  <Badge 
                    v-for="tag in tool.tags" 
                    :key="tag" 
                    variant="outline" 
                    class="text-xs px-2 py-0"
                  >
                    {{ tag }}
                  </Badge>
                </div>
              </div>
            </div>

            <!-- Description -->
            <p class="text-sm text-muted-foreground leading-relaxed min-h-[2.5rem]">
              {{ tool.description }}
            </p>

            <!-- Action Button -->
            <div class="flex justify-end pt-2">
              <Button 
                as-child 
                size="sm"
                class="group/btn"
              >
                <a 
                  :href="tool.href" 
                  target="_blank" 
                  rel="noreferrer noopener"
                  class="flex items-center gap-2"
                >
                  <span>打开工具</span>
                  <ExternalLink :size="14" class="transition-transform group-hover/btn:translate-x-0.5 group-hover/btn:-translate-y-0.5" />
                </a>
              </Button>
            </div>
          </div>
        </Card>
      </section>

      <!-- Footer -->
      <footer class="mt-8 text-center text-sm text-muted-foreground">
        <p>基于 Vite + Vue 3 + TypeScript + shadcn-vue 构建</p>
        <p class="mt-1 text-xs">所有工具均可独立部署，无需后端支持</p>
      </footer>
    </main>
  </div>
</template>
