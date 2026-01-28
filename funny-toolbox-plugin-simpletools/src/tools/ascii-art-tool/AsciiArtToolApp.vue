<script setup lang="ts">
import { ref, computed } from 'vue'
import figlet from 'figlet'
// 导入本地字体
import standardFont from 'figlet/importable-fonts/Standard.js'
import bannerFont from 'figlet/importable-fonts/Banner.js'
import bigFont from 'figlet/importable-fonts/Big.js'
import blockFont from 'figlet/importable-fonts/Block.js'
import bubbleFont from 'figlet/importable-fonts/Bubble.js'
import digitalFont from 'figlet/importable-fonts/Digital.js'
import ivritFont from 'figlet/importable-fonts/Ivrit.js'
import leanFont from 'figlet/importable-fonts/Lean.js'
import miniFont from 'figlet/importable-fonts/Mini.js'
import scriptFont from 'figlet/importable-fonts/Script.js'
import shadowFont from 'figlet/importable-fonts/Shadow.js'
import slantFont from 'figlet/importable-fonts/Slant.js'
import smallFont from 'figlet/importable-fonts/Small.js'
import speedFont from 'figlet/importable-fonts/Speed.js'
import staceyFont from 'figlet/importable-fonts/Stacey.js'
import starWarsFont from 'figlet/importable-fonts/Star Wars.js'
import stopFont from 'figlet/importable-fonts/Stop.js'

import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import { Toaster } from '@/components/ui/sonner'
import { toast } from 'vue-sonner'
import { Checkbox } from '@/components/ui/checkbox'
import { Badge } from '@/components/ui/badge'

// 模式选择：文本或图片
type Mode = 'text' | 'image'
const mode = ref<Mode>('text')

// 文本模式状态
const inputText = ref('')
const outputText = ref('')
const selectedFont = ref('Standard')

// 预定义字符集
const charsetPresets = [
  { name: '标准', value: '@%#*+=-:. ', description: '经典 ASCII 字符' },
  { name: '密集', value: '$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,"^`\'. ', description: '丰富细节' },
  { name: '简约', value: '@#*+. ', description: '简洁风格' },
  { name: '方块', value: '█▓▒░ ', description: '块状字符' },
  { name: '数字', value: '9876543210 ', description: '数字渐变' },
  { name: '自定义', value: '', description: '手动输入字符集' }
]

const selectedCharset = ref('标准')
const customCharset = ref('@%#*+=-:. ')

// 获取当前使用的字符集
const currentCharset = computed(() => {
  if (selectedCharset.value === '自定义') {
    return customCharset.value || '@%#*+=-:. '
  }
  return charsetPresets.find(p => p.name === selectedCharset.value)?.value || '@%#*+=-:. '
})

// 图片模式状态
const imageFile = ref<File | null>(null)
const imagePreview = ref('')
const imageWidth = ref(80)
const invertImage = ref(false)

// Figlet 字体列表
const figletFonts = [
  'Standard', 'Banner', 'Big', 'Block', 'Bubble', 'Digital', 'Ivrit', 'Lean', 'Mini',
  'Script', 'Shadow', 'Slant', 'Small', 'Speed', 'Stacey', 'Starwars', 'Stop'
]

// 预加载字体
const loadFonts = () => {
  figlet.parseFont('Standard', standardFont)
  figlet.parseFont('Banner', bannerFont)
  figlet.parseFont('Big', bigFont)
  figlet.parseFont('Block', blockFont)
  figlet.parseFont('Bubble', bubbleFont)
  figlet.parseFont('Digital', digitalFont)
  figlet.parseFont('Ivrit', ivritFont)
  figlet.parseFont('Lean', leanFont)
  figlet.parseFont('Mini', miniFont)
  figlet.parseFont('Script', scriptFont)
  figlet.parseFont('Shadow', shadowFont)
  figlet.parseFont('Slant', slantFont)
  figlet.parseFont('Small', smallFont)
  figlet.parseFont('Speed', speedFont)
  figlet.parseFont('Stacey', staceyFont)
  figlet.parseFont('Starwars', starWarsFont)
  figlet.parseFont('Stop', stopFont)
}

// 组件加载时预加载字体
loadFonts()

// 选择字体并生成
const selectFontAndGenerate = (font: string) => {
  selectedFont.value = font
  if (inputText.value.trim()) {
    generateTextArt()
  }
}

// 文本转 ASCII 艺术字
const generateTextArt = () => {
  try {
    if (!inputText.value.trim()) {
      toast.error('请输入要转换的文本')
      return
    }

    figlet.text(inputText.value, { font: selectedFont.value as any }, (err, result) => {
      if (err) {
        toast.error(`生成失败: ${err.message}`)
        return
      }
      outputText.value = result || ''
      toast.success('字符画生成成功！')
    })
  } catch (error) {
    toast.error(`生成失败: ${error instanceof Error ? error.message : '未知错误'}`)
  }
}

// 图片转字符画
const generateImageArt = async () => {
  if (!imageFile.value) {
    toast.error('请先选择图片')
    return
  }

  try {
    const img = new Image()
    img.src = imagePreview.value

    img.onload = () => {
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        toast.error('无法创建画布')
        return
      }

      // 计算缩放比例
      const targetWidth = imageWidth.value
      const aspectRatio = img.height / img.width
      const targetHeight = Math.floor(targetWidth * aspectRatio * 0.5) // 字符高宽比约为 2:1

      canvas.width = targetWidth
      canvas.height = targetHeight

      // 绘制图片
      ctx.drawImage(img, 0, 0, targetWidth, targetHeight)

      // 获取图片数据
      const imageData = ctx.getImageData(0, 0, targetWidth, targetHeight)
      const pixels = imageData.data

      const charset = currentCharset.value
      const charsetLength = charset.length

      let asciiArt = ''

        for (let y = 0; y < targetHeight; y++) {
        for (let x = 0; x < targetWidth; x++) {
          const offset = (y * targetWidth + x) * 4
          const r = pixels[offset] || 0
          const g = pixels[offset + 1] || 0
          const b = pixels[offset + 2] || 0

          // 计算亮度
          const brightness = (r + g + b) / 3          // 根据亮度选择字符
          let charIndex = Math.floor(((invertImage.value ? 255 - brightness : brightness) / 255) * (charsetLength - 1))
          charIndex = Math.max(0, Math.min(charsetLength - 1, charIndex))

          asciiArt += charset[charIndex]
        }
        asciiArt += '\n'
      }

      outputText.value = asciiArt
      toast.success('字符画生成成功！')
    }

    img.onerror = () => {
      toast.error('图片加载失败')
    }
  } catch (error) {
    toast.error(`生成失败: ${error instanceof Error ? error.message : '未知错误'}`)
  }
}

// 处理图片文件选择
const handleImageSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) return

  if (!file.type.startsWith('image/')) {
    toast.error('请选择图片文件')
    return
  }

  imageFile.value = file

  // 创建预览
  const reader = new FileReader()
  reader.onload = (e) => {
    imagePreview.value = e.target?.result as string
  }
  reader.readAsDataURL(file)

  toast.info('图片已加载')
}

// 复制结果
const copyOutput = async () => {
  if (!outputText.value) {
    toast.error('没有可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(outputText.value)
    toast.success('已复制到剪贴板')
  } catch (error) {
    toast.error('复制失败')
  }
}

// 粘贴到输入
const pasteInput = async () => {
  try {
    const text = await navigator.clipboard.readText()
    inputText.value = text
    toast.success('已粘贴内容')
  } catch {
    toast.error('粘贴失败', { description: '请检查浏览器权限' })
  }
}

// 清空输入
const clearInput = () => {
  inputText.value = ''
}

// 清空
const clear = () => {
  inputText.value = ''
  outputText.value = ''
  imageFile.value = null
  imagePreview.value = ''
  toast.info('已清空')
}

// 下载为文本文件
const downloadArt = () => {
  if (!outputText.value) {
    toast.error('没有可下载的内容')
    return
  }

  const blob = new Blob([outputText.value], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `ascii-art-${Date.now()}.txt`
  a.click()
  URL.revokeObjectURL(url)

  toast.success('已下载字符画')
}
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <Toaster />
    
    <main class="mx-auto flex max-w-4xl flex-col gap-6 px-4 py-8">
      <!-- 标题区域 -->
      <header>
        <h1 class="text-2xl font-semibold tracking-tight">字符画生成器</h1>
        <p class="mt-1 text-sm text-muted-foreground">
          将文本或图片转换为精美的 ASCII 字符画，支持多种字体样式和自定义字符集
        </p>
      </header>

      <Separator />

      <!-- 模式选择 -->
      <Card class="p-4">
        <div class="flex flex-col gap-3">
          <Label class="text-sm font-medium">生成模式</Label>
          <div class="flex gap-2">
            <Button 
              @click="mode = 'text'" 
              :variant="mode === 'text' ? 'default' : 'outline'"
              size="sm"
              class="flex-1"
            >
              文本艺术字
            </Button>
            <Button 
              @click="mode = 'image'" 
              :variant="mode === 'image' ? 'default' : 'outline'"
              size="sm"
              class="flex-1"
            >
              图片字符画
            </Button>
          </div>
        </div>
      </Card>

      <!-- 文本模式 -->
      <div v-if="mode === 'text'" class="flex flex-col gap-6">
        <!-- 字体选择区域 -->
        <Card class="p-4">
          <div class="flex flex-col gap-3">
            <Label class="text-sm font-medium">选择字体样式（点击直接生成）</Label>
            <div class="flex flex-wrap gap-2">
              <Badge
                v-for="font in figletFonts"
                :key="font"
                :variant="selectedFont === font ? 'default' : 'outline'"
                @click="selectFontAndGenerate(font)"
                :class="[
                  'cursor-pointer transition-colors px-3 py-1.5 text-sm',
                  selectedFont === font 
                    ? 'hover:bg-primary/80' 
                    : 'hover:bg-accent'
                ]"
              >
                {{ font }}
              </Badge>
            </div>
            <p class="text-xs text-muted-foreground">
              已选择：<strong>{{ selectedFont }}</strong>
            </p>
          </div>
        </Card>

        <!-- 输入输出区域 -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- 输入区域 -->
          <Card class="p-4">
            <div class="flex flex-col gap-3">
              <div class="flex items-center justify-between">
                <Label class="text-sm font-medium">输入文本</Label>
                <span class="text-xs text-muted-foreground">{{ inputText.length }}/50</span>
              </div>
              <Textarea
                v-model="inputText"
                placeholder="输入文本（支持中英文、数字）..."
                class="min-h-[120px] font-mono resize-none"
                maxlength="50"
                @keydown.ctrl.enter="generateTextArt"
              />
              <div class="flex gap-2">
                <Button @click="pasteInput" variant="outline" size="sm" class="flex-1">
                  粘贴
                </Button>
                <Button @click="clearInput" variant="outline" size="sm" class="flex-1">
                  清空
                </Button>
              </div>
            </div>
          </Card>

          <!-- 输出区域 -->
          <Card class="p-4">
            <div class="flex flex-col gap-3">
              <div class="flex items-center justify-between">
                <Label class="text-sm font-medium">输出结果</Label>
                <Badge variant="secondary" class="text-xs">{{ outputText.split('\n').length }} 行</Badge>
              </div>
              <Textarea
                v-model="outputText"
                placeholder="生成的字符画将显示在这里..."
                class="min-h-[120px] font-mono text-xs resize-none bg-muted/30 leading-tight"
                readonly
              />
              <div class="flex gap-2">
                <Button @click="copyOutput" variant="outline" size="sm" class="flex-1">
                  复制
                </Button>
                <Button @click="downloadArt" variant="outline" size="sm" class="flex-1">
                  下载
                </Button>
              </div>
            </div>
          </Card>
        </div>

        <!-- 预览区域 -->
        <Card class="p-4" v-if="outputText">
          <div class="flex flex-col gap-3">
            <Label class="text-sm font-medium">预览</Label>
            <div class="bg-black text-green-400 p-4 rounded-md overflow-x-auto">
              <pre class="font-mono text-xs leading-tight">{{ outputText }}</pre>
            </div>
          </div>
        </Card>
      </div>

      <!-- 图片模式 -->
      <div v-if="mode === 'image'" class="flex flex-col gap-6">
        <!-- 图片上传和配置 -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- 左侧：图片上传和预览 -->
          <Card class="p-4">
            <div class="flex flex-col gap-3">
              <Label class="text-sm font-medium">选择图片</Label>
              <Input 
                type="file" 
                accept="image/*" 
                @change="handleImageSelect"
                class="cursor-pointer"
              />
              
              <div v-if="imagePreview" class="mt-2">
                <Label class="text-xs mb-2 block text-muted-foreground">预览</Label>
                <div class="border rounded-md p-2 bg-muted/30 flex items-center justify-center">
                  <img :src="imagePreview" alt="预览" class="max-w-full h-auto max-h-48 rounded" />
                </div>
              </div>
            </div>
          </Card>

          <!-- 右侧：参数配置 -->
          <Card class="p-4">
            <div class="flex flex-col gap-4">
              <div>
                <div class="flex items-center justify-between mb-2">
                  <Label class="text-sm font-medium">字符宽度</Label>
                  <Badge variant="secondary" class="text-xs">{{ imageWidth }}</Badge>
                </div>
                <Input 
                  type="range" 
                  v-model.number="imageWidth" 
                  min="40" 
                  max="200" 
                  step="10"
                  class="w-full"
                />
                <p class="text-xs text-muted-foreground mt-1">控制生成字符画的宽度（40-200）</p>
              </div>

              <div>
                <Label class="text-sm font-medium mb-2 block">字符集样式</Label>
                <div class="flex flex-wrap gap-2">
                  <Badge
                    v-for="preset in charsetPresets"
                    :key="preset.name"
                    :variant="selectedCharset === preset.name ? 'default' : 'outline'"
                    @click="selectedCharset = preset.name"
                    :class="[
                      'cursor-pointer transition-colors px-3 py-1.5 text-sm',
                      selectedCharset === preset.name 
                        ? 'hover:bg-primary/80' 
                        : 'hover:bg-accent'
                    ]"
                  >
                    {{ preset.name }}
                  </Badge>
                </div>
                <p class="text-xs text-muted-foreground mt-2">{{ charsetPresets.find(p => p.name === selectedCharset)?.description }}</p>
              </div>

              <div v-if="selectedCharset === '自定义'">
                <Label class="text-xs mb-2 block">自定义字符集（从暗到亮）</Label>
                <Input 
                  v-model="customCharset" 
                  placeholder="例如：@%#*+=-:. " 
                  class="font-mono text-sm"
                />
              </div>

              <div class="flex items-center gap-2 pt-2">
                <Checkbox id="invert" v-model:checked="invertImage" />
                <Label for="invert" class="text-sm cursor-pointer">反转亮度</Label>
              </div>
            </div>
          </Card>
        </div>

        <!-- 生成按钮 -->
        <Card class="p-4">
          <div class="flex gap-2">
            <Button @click="generateImageArt" size="default" class="flex-1">
              生成字符画
            </Button>
            <Button @click="clear" variant="outline" size="default">
              清空
            </Button>
          </div>
        </Card>

        <!-- 输出区域 -->
        <Card class="p-4" v-if="outputText">
          <div class="flex flex-col gap-3">
            <div class="flex items-center justify-between">
              <Label class="text-sm font-medium">输出结果</Label>
              <Badge variant="secondary" class="text-xs">{{ outputText.split('\n').length }} 行</Badge>
            </div>
            <Textarea
              v-model="outputText"
              placeholder="生成的字符画将显示在这里..."
              class="min-h-[300px] font-mono text-xs resize-none bg-muted/30 leading-tight"
              readonly
            />
            <div class="flex gap-2">
              <Button @click="copyOutput" variant="outline" size="sm" class="flex-1">
                复制
              </Button>
              <Button @click="downloadArt" variant="outline" size="sm" class="flex-1">
                下载
              </Button>
            </div>
          </div>
        </Card>

        <!-- 预览区域 -->
        <Card class="p-4" v-if="outputText">
          <div class="flex flex-col gap-3">
            <Label class="text-sm font-medium">黑底预览</Label>
            <div class="bg-black text-green-400 p-4 rounded-md overflow-x-auto">
              <pre class="font-mono text-xs leading-tight">{{ outputText }}</pre>
            </div>
          </div>
        </Card>
      </div>

      <!-- 使用说明 -->
      <Card class="p-4 bg-muted/30">
        <h3 class="text-sm font-semibold mb-2">使用提示</h3>
        <ul class="space-y-1 text-xs text-muted-foreground">
          <li><strong>文本模式：</strong>点击字体徽章即可立即生成对应样式的字符画，支持中英文、数字</li>
          <li><strong>图片模式：</strong>上传图片后调整参数，点击生成按钮转换为字符画</li>
          <li><strong>自定义字符集：</strong>可以手动输入喜欢的字符，从暗到亮排列（如：@%#*+=-:. ）</li>
          <li><strong>快捷键：</strong>文本模式下按 Ctrl+Enter 快速生成</li>
        </ul>
      </Card>
    </main>
  </div>
</template>
