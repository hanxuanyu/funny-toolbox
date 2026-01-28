<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import { Toaster } from '@/components/ui/sonner'
import { toast } from 'vue-sonner'
import { Copy, Upload } from 'lucide-vue-next'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

interface RGB {
  r: number
  g: number
  b: number
}

interface HSL {
  h: number
  s: number
  l: number
}

// 颜色转换工具函数
const hexToRgb = (hex: string): RGB | null => {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex.trim())
  if (!result || !result[1] || !result[2] || !result[3]) return null
  return {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16),
  }
}

const rgbToHex = (r: number, g: number, b: number): string => {
  const toHex = (n: number) => {
    const hex = n.toString(16)
    return hex.length === 1 ? '0' + hex : hex
  }
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`.toUpperCase()
}

const rgbToHsl = (r: number, g: number, b: number): HSL => {
  r /= 255
  g /= 255
  b /= 255
  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  let h = 0
  let s = 0
  const l = (max + min) / 2

  if (max !== min) {
    const d = max - min
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
    switch (max) {
      case r:
        h = ((g - b) / d + (g < b ? 6 : 0)) / 6
        break
      case g:
        h = ((b - r) / d + 2) / 6
        break
      case b:
        h = ((r - g) / d + 4) / 6
        break
    }
  }

  return {
    h: Math.round(h * 360),
    s: Math.round(s * 100),
    l: Math.round(l * 100),
  }
}

const hslToRgb = (h: number, s: number, l: number): RGB => {
  h /= 360
  s /= 100
  l /= 100

  let r = 0
  let g = 0
  let b = 0

  if (s === 0) {
    r = g = b = l
  } else {
    const hue2rgb = (p: number, q: number, t: number) => {
      if (t < 0) t += 1
      if (t > 1) t -= 1
      if (t < 1 / 6) return p + (q - p) * 6 * t
      if (t < 1 / 2) return q
      if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6
      return p
    }

    const q = l < 0.5 ? l * (1 + s) : l + s - l * s
    const p = 2 * l - q
    r = hue2rgb(p, q, h + 1 / 3)
    g = hue2rgb(p, q, h)
    b = hue2rgb(p, q, h - 1 / 3)
  }

  return {
    r: Math.round(r * 255),
    g: Math.round(g * 255),
    b: Math.round(b * 255),
  }
}

const rgbToHsv = (r: number, g: number, b: number): { h: number; s: number; v: number } => {
  r /= 255
  g /= 255
  b /= 255
  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  const d = max - min
  let h = 0
  const s = max === 0 ? 0 : d / max
  const v = max

  if (max !== min) {
    switch (max) {
      case r:
        h = ((g - b) / d + (g < b ? 6 : 0)) / 6
        break
      case g:
        h = ((b - r) / d + 2) / 6
        break
      case b:
        h = ((r - g) / d + 4) / 6
        break
    }
  }

  return {
    h: Math.round(h * 360),
    s: Math.round(s * 100),
    v: Math.round(v * 100),
  }
}

const hsvToRgb = (h: number, s: number, v: number): RGB => {
  h = h / 360
  s = s / 100
  v = v / 100

  const i = Math.floor(h * 6)
  const f = h * 6 - i
  const p = v * (1 - s)
  const q = v * (1 - f * s)
  const t = v * (1 - (1 - f) * s)

  let r = 0
  let g = 0
  let b = 0

  switch (i % 6) {
    case 0:
      r = v
      g = t
      b = p
      break
    case 1:
      r = q
      g = v
      b = p
      break
    case 2:
      r = p
      g = v
      b = t
      break
    case 3:
      r = p
      g = q
      b = v
      break
    case 4:
      r = t
      g = p
      b = v
      break
    case 5:
      r = v
      g = p
      b = q
      break
  }

  return {
    r: Math.round(r * 255),
    g: Math.round(g * 255),
    b: Math.round(b * 255),
  }
}

const parseColorInput = (input: string): RGB | null => {
  const trimmed = input.trim()

  // 尝试解析十六进制
  if (/^#?[0-9a-fA-F]{6}$/.test(trimmed)) {
    return hexToRgb(trimmed)
  }

  // 尝试解析 RGB 格式：rgb(255, 255, 255)
  const rgbMatch = trimmed.match(/rgb\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)/i)
  if (rgbMatch && rgbMatch[1] && rgbMatch[2] && rgbMatch[3]) {
    return {
      r: Math.min(255, Math.max(0, parseInt(rgbMatch[1]))),
      g: Math.min(255, Math.max(0, parseInt(rgbMatch[2]))),
      b: Math.min(255, Math.max(0, parseInt(rgbMatch[3]))),
    }
  }

  // 尝试解析 HSL 格式：hsl(0, 0%, 0%)
  const hslMatch = trimmed.match(/hsl\s*\(\s*(\d+)\s*,\s*(\d+)%?\s*,\s*(\d+)%?\s*\)/i)
  if (hslMatch && hslMatch[1] && hslMatch[2] && hslMatch[3]) {
    return hslToRgb(
      parseInt(hslMatch[1]),
      parseInt(hslMatch[2]),
      parseInt(hslMatch[3]),
    )
  }

  // 尝试解析 HSV 格式：hsv(0, 0%, 0%)
  const hsvMatch = trimmed.match(/hsv\s*\(\s*(\d+)\s*,\s*(\d+)%?\s*,\s*(\d+)%?\s*\)/i)
  if (hsvMatch && hsvMatch[1] && hsvMatch[2] && hsvMatch[3]) {
    return hsvToRgb(
      parseInt(hsvMatch[1]),
      parseInt(hsvMatch[2]),
      parseInt(hsvMatch[3]),
    )
  }

  return null
}

// 响应式数据
const inputColorCode = ref('')
const currentRGB = ref<RGB | null>(null)
const pickedColor = ref('#FFC2C2')
const tempPickedColor = ref<{ r: number; g: number; b: number } | null>(null)  // 临时取色，等待确认
const isTouching = ref(false)  // 是否正在触摸

const displayColor = computed(() => {
  if (currentRGB.value) {
    return rgbToHex(currentRGB.value.r, currentRGB.value.g, currentRGB.value.b)
  }
  return pickedColor.value
})

const formattedColors = computed(() => {
  const rgb = currentRGB.value || hexToRgb(pickedColor.value) || { r: 255, g: 194, b: 194 }
  const hex = rgbToHex(rgb.r, rgb.g, rgb.b)
  const hsl = rgbToHsl(rgb.r, rgb.g, rgb.b)
  const hsv = rgbToHsv(rgb.r, rgb.g, rgb.b)

  return {
    hex: hex,
    hexLower: hex.toLowerCase(),
    rgb: `rgb(${rgb.r}, ${rgb.g}, ${rgb.b})`,
    rgbShort: `${rgb.r}, ${rgb.g}, ${rgb.b}`,
    hsl: `hsl(${hsl.h}, ${hsl.s}%, ${hsl.l}%)`,
    hslShort: `${hsl.h}, ${hsl.s}%, ${hsl.l}%`,
    hsv: `hsv(${hsv.h}, ${hsv.s}%, ${hsv.v}%)`,
    hsvShort: `${hsv.h}, ${hsv.s}%, ${hsv.v}%`,
  }
})

// 处理输入颜色代码
const handleColorInput = () => {
  const rgb = parseColorInput(inputColorCode.value)
  if (rgb) {
    currentRGB.value = rgb
    pickedColor.value = rgbToHex(rgb.r, rgb.g, rgb.b)
    toast.success('颜色解析成功')
  } else {
    toast.error('无法识别颜色格式，请输入有效的 HEX、RGB 或 HSL 格式')
  }
}

// 处理拾色器变化
const handlePickerChange = () => {
  const rgb = hexToRgb(pickedColor.value)
  if (rgb) {
    currentRGB.value = rgb
  }
}

// 复制到剪贴板
const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    toast.success('已复制到剪贴板')
  })
}

// 清空输入
const clearInput = () => {
  inputColorCode.value = ''
  currentRGB.value = null
}

// 图片取色相关
const imagePickerOpen = ref(false)
const uploadedImage = ref<string | null>(null)
const canvas = ref<HTMLCanvasElement | null>(null)
const previewCanvas = ref<HTMLCanvasElement | null>(null)
const hoverColor = ref<{ r: number; g: number; b: number } | null>(null)
const mouseX = ref(0)
const mouseY = ref(0)

// 处理图片上传
const handleImageUpload = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    toast.error('请选择一个图片文件')
    return
  }

  // 检查文件大小（限制为5MB）
  if (file.size > 5 * 1024 * 1024) {
    toast.error('图片大小不超过5MB')
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    uploadedImage.value = e.target?.result as string
    // 使用 nextTick 确保 DOM 更新后再绘制
    setTimeout(() => drawImageToCanvas(), 0)
  }
  reader.onerror = () => {
    toast.error('图片加载失败')
  }
  reader.readAsDataURL(file)
}

// 获取光标位置的颜色
const getColorAtPosition = (clientX: number, clientY: number) => {
  if (!canvas.value) return

  const rect = canvas.value.getBoundingClientRect()
  
  // 鼠标相对于canvas显示区域的位置
  let relativeX = clientX - rect.left
  let relativeY = clientY - rect.top
  
  // 检查鼠标是否在canvas的实际显示范围内（考虑canvas可能小于容器）
  // 这很重要，因为当图片被缩放后，canvas可能不会填满整个容器
  if (relativeX < 0 || relativeY < 0 || relativeX >= rect.width || relativeY >= rect.height) return

  // canvas内部像素坐标 = 鼠标相对位置 * (内部尺寸 / 显示尺寸)
  const scaleX = canvas.value.width / rect.width
  const scaleY = canvas.value.height / rect.height

  const x = Math.floor(relativeX * scaleX)
  const y = Math.floor(relativeY * scaleY)
  
  // 确保坐标在canvas的实际内容范围内
  if (x < 0 || y < 0 || x >= canvas.value.width || y >= canvas.value.height) return

  const ctx = canvas.value.getContext('2d')
  if (!ctx) return

  const imageData = ctx.getImageData(x, y, 1, 1)
  return {
    r: imageData.data[0] || 0,
    g: imageData.data[1] || 0,
    b: imageData.data[2] || 0,
    x,
    y,
  }
}

// 鼠标移动事件处理
const handleCanvasMouseMove = (event: MouseEvent) => {
  const color = getColorAtPosition(event.clientX, event.clientY)
  if (color) {
    mouseX.value = event.clientX
    mouseY.value = event.clientY
    hoverColor.value = { r: color.r, g: color.g, b: color.b }
    updatePreviewCanvas(color.x, color.y)
  }
}

// 绘制放大预览
const updatePreviewCanvas = (x: number, y: number) => {
  if (!previewCanvas.value || !canvas.value) return

  const ctx = previewCanvas.value.getContext('2d')
  if (!ctx) return

  const canvasCtx = canvas.value.getContext('2d')
  if (!canvasCtx) return

  const size = 50
  // 获取预览canvas的实际内部尺寸
  const previewSize = previewCanvas.value.width
  const halfSize = Math.floor(size / 2)

  // 清除预览画布
  ctx.fillStyle = '#f1f5f9'
  ctx.fillRect(0, 0, previewSize, previewSize)

  // 计算裁剪区域的起始位置（以被取色点为中心）
  let cropX = Math.max(0, x - halfSize)
  let cropY = Math.max(0, y - halfSize)
  
  // 边界检查：如果超出右边或下边，向左或向上调整
  if (cropX + size > canvas.value.width) {
    cropX = Math.max(0, canvas.value.width - size)
  }
  if (cropY + size > canvas.value.height) {
    cropY = Math.max(0, canvas.value.height - size)
  }

  // 计算实际能获取的大小（处理边界情况）
  let cropWidth = Math.min(size, canvas.value.width - cropX)
  let cropHeight = Math.min(size, canvas.value.height - cropY)

  // 获取源图像数据
  try {
    const imageData = canvasCtx.getImageData(cropX, cropY, cropWidth, cropHeight)

    // 创建缩放后的图像
    const tempCanvas = document.createElement('canvas')
    tempCanvas.width = cropWidth
    tempCanvas.height = cropHeight
    const tempCtx = tempCanvas.getContext('2d')
    if (tempCtx) {
      tempCtx.putImageData(imageData, 0, 0)
      // 绘制到预览canvas中，使用实际的裁剪大小
      ctx.drawImage(tempCanvas, 0, 0, cropWidth, cropHeight, 0, 0, previewSize, previewSize)
    }
  } catch (e) {
    // 忽略越界错误
  }

  // 计算目标像素在预览canvas中的位置
  const targetPixelX = ((x - cropX) / cropWidth) * previewSize
  const targetPixelY = ((y - cropY) / cropHeight) * previewSize

  // 绘制十字线标记中心像素
  ctx.strokeStyle = '#ef4444'
  ctx.lineWidth = 2
  
  // 十字线中心坐标
  const crossX = targetPixelX
  const crossY = targetPixelY
  
  // 绘制水平线
  ctx.beginPath()
  ctx.moveTo(Math.max(0, crossX - 10), crossY)
  ctx.lineTo(Math.min(previewSize, crossX + 10), crossY)
  ctx.stroke()
  
  // 绘制竖直线
  ctx.beginPath()
  ctx.moveTo(crossX, Math.max(0, crossY - 10))
  ctx.lineTo(crossX, Math.min(previewSize, crossY + 10))
  ctx.stroke()

  // 绘制中心点（更小的圆形）
  ctx.fillStyle = '#ef4444'
  ctx.beginPath()
  ctx.arc(crossX, crossY, 2, 0, Math.PI * 2)
  ctx.fill()

  // 绘制边框
  ctx.strokeStyle = '#64748b'
  ctx.lineWidth = 1
  ctx.strokeRect(0, 0, previewSize, previewSize)
}

// 从图片中取色
// 获取点击/触摸位置的颜色，用于预览
const getColorAndPreview = (clientX: number, clientY: number) => {
  const color = getColorAtPosition(clientX, clientY)
  if (!color) return
  
  tempPickedColor.value = { r: color.r, g: color.g, b: color.b }
  hoverColor.value = { r: color.r, g: color.g, b: color.b }
  updatePreviewCanvas(color.x, color.y)
}

// 点击取色（桌面端直接确认）
const pickColorFromImage = (event: MouseEvent) => {
  const color = getColorAtPosition(event.clientX, event.clientY)
  if (!color) return

  const hex = rgbToHex(color.r, color.g, color.b)
  pickedColor.value = hex
  currentRGB.value = { r: color.r, g: color.g, b: color.b }
  tempPickedColor.value = null

  toast.success(`已取色：${hex}`)
  imagePickerOpen.value = false
  hoverColor.value = null
}

// 触摸开始
const handleCanvasTouchStart = (event: TouchEvent) => {
  isTouching.value = true
  const touch = event.touches[0]
  if (touch) {
    getColorAndPreview(touch.clientX, touch.clientY)
  }
}

// 触摸移动
const handleCanvasTouchMove = (event: TouchEvent) => {
  if (!isTouching.value) return
  event.preventDefault()
  const touch = event.touches[0]
  if (touch) {
    getColorAndPreview(touch.clientX, touch.clientY)
  }
}

// 触摸结束
const handleCanvasTouchEnd = () => {
  isTouching.value = false
}

// 确认取色（用于移动端）
const confirmPickedColor = () => {
  if (!tempPickedColor.value) return

  const hex = rgbToHex(tempPickedColor.value.r, tempPickedColor.value.g, tempPickedColor.value.b)
  pickedColor.value = hex
  currentRGB.value = tempPickedColor.value
  tempPickedColor.value = null

  toast.success(`已取色：${hex}`)
  imagePickerOpen.value = false
  hoverColor.value = null
}

// 清除鼠标悬停状态
const handleCanvasMouseLeave = () => {
  hoverColor.value = null
}

// 绘制图片到canvas
const drawImageToCanvas = () => {
  if (!uploadedImage.value || !canvas.value) return

  const img = new Image()
  img.onload = () => {
    const ctx = canvas.value!.getContext('2d')
    if (!ctx) return

    // 获取容器的实际可用尺寸
    const container = canvas.value!.parentElement
    if (!container) return
    
    // 使用getComputedStyle获取实际的padding值
    const computedStyle = window.getComputedStyle(container)
    const paddingLeft = parseFloat(computedStyle.paddingLeft) || 0
    const paddingRight = parseFloat(computedStyle.paddingRight) || 0
    const paddingTop = parseFloat(computedStyle.paddingTop) || 0
    const paddingBottom = parseFloat(computedStyle.paddingBottom) || 0
    
    const rect = container.getBoundingClientRect()
    const availableWidth = Math.floor(rect.width - paddingLeft - paddingRight)
    const availableHeight = Math.floor(rect.height - paddingTop - paddingBottom)
    
    // 原始图片尺寸
    let width = img.width
    let height = img.height

    // 按比例缩放以适应可用空间，保持原始宽高比
    const widthRatio = availableWidth / width
    const heightRatio = availableHeight / height
    const scale = Math.min(widthRatio, heightRatio, 1) // 不放大
    
    width = Math.round(width * scale)
    height = Math.round(height * scale)

    // 设置canvas的内部像素尺寸（这决定了坐标系）
    canvas.value!.width = width
    canvas.value!.height = height
    
    // 绘制图片
    ctx.drawImage(img, 0, 0, width, height)
  }
  img.src = uploadedImage.value
}

// 关闭图片选择器
const closeImagePicker = () => {
  imagePickerOpen.value = false
}

// 清除已上传的图片
const clearUploadedImage = () => {
  uploadedImage.value = null
}

// 监听模态框打开状态，打开时重新绘制图片
watch(imagePickerOpen, (isOpen) => {
  if (isOpen && uploadedImage.value) {
    // 延迟绘制以确保 DOM 已更新
    setTimeout(() => drawImageToCanvas(), 50)
  }
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 p-4 sm:p-6 md:p-8">
    <Toaster />

    <div class="max-w-6xl mx-auto">
      <!-- 标题区域 -->
      <div class="mb-8 text-center">
        <h1 class="text-3xl sm:text-4xl font-bold text-slate-900 mb-2">🎨 颜色转换工具</h1>
        <p class="text-slate-600 text-sm sm:text-base">输入或选择颜色，轻松转换各种格式：HEX、RGB、HSL、HSV</p>
      </div>

      <!-- 主容器：两列布局 -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- 左列：输入和选择区域 -->
        <div class="lg:col-span-1 space-y-4">
          <Card class="p-4 sm:p-6 shadow-md hover:shadow-lg transition-shadow h-full">
            <div class="space-y-4">
              <!-- 输入颜色代码 -->
              <div>
                <Label for="color-input" class="text-sm font-semibold text-slate-700 mb-2 block">
                  输入颜色代码
                </Label>
                <p class="text-xs text-slate-500 mb-3">支持：#FF0000、rgb(255, 0, 0)、hsl(0, 100%, 50%)</p>
                <div class="flex gap-2 flex-col">
                  <Input
                    id="color-input"
                    v-model="inputColorCode"
                    placeholder="例：#FF0000"
                    class="w-full"
                    @keyup.enter="handleColorInput"
                  />
                  <div class="flex gap-2">
                    <Button @click="handleColorInput" class="flex-1 bg-blue-600 hover:bg-blue-700 text-sm">
                      转换
                    </Button>
                    <Button @click="clearInput" variant="outline" class="flex-1 text-sm">
                      清空
                    </Button>
                  </div>
                </div>
              </div>

              <Separator />

              <!-- 拾色器 -->
              <div>
                <Label for="color-picker" class="text-sm font-semibold text-slate-700 mb-3 block">
                  颜色选择器
                </Label>
                <div class="flex justify-center gap-3">
                  <input
                    id="color-picker"
                    v-model="pickedColor"
                    type="color"
                    class="w-20 h-20 rounded-lg cursor-pointer border-4 border-slate-200 shadow-md hover:shadow-lg transition-shadow"
                    @input="handlePickerChange"
                  />
                  <Button
                    @click="imagePickerOpen = true"
                    :variant="uploadedImage ? 'default' : 'outline'"
                    class="h-20 w-20 p-0 flex items-center justify-center rounded-lg overflow-hidden relative transition-all hover:shadow-md"
                    :title="uploadedImage ? '已上传图片，点击重新选择' : '从图片中取色'"
                    :style="uploadedImage ? { backgroundImage: `url(${uploadedImage})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}"
                  >
                    <Upload v-if="!uploadedImage" class="w-5 h-5" />
                    <div v-else class="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity">
                      <Upload class="w-5 h-5 text-white" />
                    </div>
                  </Button>
                </div>
              </div>

              <Separator />

              <!-- 颜色预览 -->
              <div>
                <Label class="text-sm font-semibold text-slate-700 mb-3 block">颜色预览</Label>
                <div
                  class="w-full h-20 rounded-lg border-4 border-slate-200 shadow-sm transition-all"
                  :style="{ backgroundColor: displayColor }"
                />
              </div>
            </div>
          </Card>
        </div>

        <!-- 右列：颜色格式输出 -->
        <div class="lg:col-span-2">
          <Card class="p-4 sm:p-6 shadow-md hover:shadow-lg transition-shadow">
            <div>
              <Label class="text-sm font-semibold text-slate-700 mb-4 block">颜色格式</Label>

              <!-- 网格布局显示各种格式 -->
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <!-- HEX 大写 -->
                <div class="bg-slate-50 rounded-lg p-3 border border-slate-200">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-medium text-slate-600">HEX</span>
                    <Button
                      @click="copyToClipboard(formattedColors.hex)"
                      size="sm"
                      variant="ghost"
                      class="h-6 w-6 p-0 hover:bg-slate-200"
                    >
                      <Copy class="w-3 h-3" />
                    </Button>
                  </div>
                  <div class="font-mono text-sm text-slate-900 break-all">{{ formattedColors.hex }}</div>
                </div>

                <!-- HEX 小写 -->
                <div class="bg-slate-50 rounded-lg p-3 border border-slate-200">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-medium text-slate-600">HEX (小写)</span>
                    <Button
                      @click="copyToClipboard(formattedColors.hexLower)"
                      size="sm"
                      variant="ghost"
                      class="h-6 w-6 p-0 hover:bg-slate-200"
                    >
                      <Copy class="w-3 h-3" />
                    </Button>
                  </div>
                  <div class="font-mono text-sm text-slate-900 break-all">{{ formattedColors.hexLower }}</div>
                </div>

                <!-- RGB -->
                <div class="bg-slate-50 rounded-lg p-3 border border-slate-200">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-medium text-slate-600">RGB</span>
                    <Button
                      @click="copyToClipboard(formattedColors.rgb)"
                      size="sm"
                      variant="ghost"
                      class="h-6 w-6 p-0 hover:bg-slate-200"
                    >
                      <Copy class="w-3 h-3" />
                    </Button>
                  </div>
                  <div class="font-mono text-sm text-slate-900 break-all">{{ formattedColors.rgb }}</div>
                </div>

                <!-- RGB 数值 -->
                <div class="bg-slate-50 rounded-lg p-3 border border-slate-200">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-medium text-slate-600">RGB 数值</span>
                    <Button
                      @click="copyToClipboard(formattedColors.rgbShort)"
                      size="sm"
                      variant="ghost"
                      class="h-6 w-6 p-0 hover:bg-slate-200"
                    >
                      <Copy class="w-3 h-3" />
                    </Button>
                  </div>
                  <div class="font-mono text-sm text-slate-900 break-all">{{ formattedColors.rgbShort }}</div>
                </div>

                <!-- HSL -->
                <div class="bg-slate-50 rounded-lg p-3 border border-slate-200">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-medium text-slate-600">HSL</span>
                    <Button
                      @click="copyToClipboard(formattedColors.hsl)"
                      size="sm"
                      variant="ghost"
                      class="h-6 w-6 p-0 hover:bg-slate-200"
                    >
                      <Copy class="w-3 h-3" />
                    </Button>
                  </div>
                  <div class="font-mono text-sm text-slate-900 break-all">{{ formattedColors.hsl }}</div>
                </div>

                <!-- HSL 数值 -->
                <div class="bg-slate-50 rounded-lg p-3 border border-slate-200">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-medium text-slate-600">HSL 数值</span>
                    <Button
                      @click="copyToClipboard(formattedColors.hslShort)"
                      size="sm"
                      variant="ghost"
                      class="h-6 w-6 p-0 hover:bg-slate-200"
                    >
                      <Copy class="w-3 h-3" />
                    </Button>
                  </div>
                  <div class="font-mono text-sm text-slate-900 break-all">{{ formattedColors.hslShort }}</div>
                </div>

                <!-- HSV -->
                <div class="bg-slate-50 rounded-lg p-3 border border-slate-200">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-medium text-slate-600">HSV</span>
                    <Button
                      @click="copyToClipboard(formattedColors.hsv)"
                      size="sm"
                      variant="ghost"
                      class="h-6 w-6 p-0 hover:bg-slate-200"
                    >
                      <Copy class="w-3 h-3" />
                    </Button>
                  </div>
                  <div class="font-mono text-sm text-slate-900 break-all">{{ formattedColors.hsv }}</div>
                </div>

                <!-- HSV 数值 -->
                <div class="bg-slate-50 rounded-lg p-3 border border-slate-200">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-medium text-slate-600">HSV 数值</span>
                    <Button
                      @click="copyToClipboard(formattedColors.hsvShort)"
                      size="sm"
                      variant="ghost"
                      class="h-6 w-6 p-0 hover:bg-slate-200"
                    >
                      <Copy class="w-3 h-3" />
                    </Button>
                  </div>
                  <div class="font-mono text-sm text-slate-900 break-all">{{ formattedColors.hsvShort }}</div>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>

      <!-- 信息提示 -->
      <div class="mt-8 bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-blue-800">
        <p class="font-semibold mb-2">💡 使用提示：</p>
        <ul class="space-y-1 ml-4 list-disc">
          <li>输入颜色代码：支持 HEX、RGB、HSL、HSV 等多种格式</li>
          <li>使用颜色选择器快速选择颜色</li>
          <li>点击上传按钮从图片中取色</li>
          <li>点击复制按钮将颜色代码复制到剪贴板</li>
        </ul>
      </div>

      <!-- 图片取色模态框 -->
      <Dialog v-model:open="imagePickerOpen">
        <DialogContent class="h-[85vh] max-h-[85vh] flex flex-col" style="max-width: 1152px; width: 90vw;">
          <DialogHeader>
            <DialogTitle>从图片中取色</DialogTitle>
            <DialogDescription>
              上传一张图片，然后点击图片中的任意位置来取色
            </DialogDescription>
          </DialogHeader>

          <div class="space-y-4 flex-1 overflow-hidden flex flex-col">
            <!-- 文件上传 -->
            <div v-if="!uploadedImage" class="flex items-center justify-center flex-1">
              <label class="flex flex-col items-center justify-center w-full h-full border-2 border-dashed border-slate-300 rounded-lg cursor-pointer bg-slate-50 hover:bg-slate-100 transition-colors">
                <div class="flex flex-col items-center justify-center">
                  <Upload class="w-8 h-8 text-slate-400 mb-2" />
                  <p class="text-sm text-slate-600">点击上传或拖放一张图片</p>
                  <p class="text-xs text-slate-500 mt-1">支持 JPG、PNG、GIF 等格式（最大 5MB）</p>
                </div>
                <input
                  type="file"
                  class="hidden"
                  accept="image/*"
                  @change="handleImageUpload"
                />
              </label>
            </div>

            <!-- 图片显示和取色 -->
            <div v-else class="space-y-4 flex-1 overflow-hidden flex flex-col">
              <p class="text-sm text-slate-600">将鼠标移到图片上预览，点击取色</p>
              
              <!-- 预览和图片容器 -->
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4 flex-1 overflow-hidden">
                <!-- 主图片 -->
                <div class="md:col-span-2 flex items-center justify-center bg-slate-100 rounded-lg p-4 overflow-hidden">
                  <canvas
                    ref="canvas"
                    class="cursor-crosshair border-2 border-slate-300 rounded"
                    style="display: block; max-width: 100%; max-height: 100%; object-fit: contain;"
                    @click="pickColorFromImage"
                    @mousemove="handleCanvasMouseMove"
                    @mouseleave="handleCanvasMouseLeave"
                    @touchstart="handleCanvasTouchStart"
                    @touchmove="handleCanvasTouchMove"
                    @touchend="handleCanvasTouchEnd"
                  />
                </div>

                <!-- 放大预览区域 -->
                <div class="space-y-2 md:flex md:flex-col">
                  <div class="bg-white rounded-lg border-2 border-slate-300 p-3 flex flex-col items-center justify-center h-full">
                    <canvas
                      ref="previewCanvas"
                      class="w-full max-w-[120px] md:max-w-[200px] border border-slate-200 rounded"
                      width="120"
                      height="120"
                      style="image-rendering: pixelated"
                    />
                  </div>
                  
                  <!-- 颜色预览块和值 -->
                  <div v-if="hoverColor" class="space-y-2">
                    <div
                      class="w-full h-12 rounded-lg border-2 border-slate-300 shadow-sm"
                      :style="{ backgroundColor: `rgb(${hoverColor.r}, ${hoverColor.g}, ${hoverColor.b})` }"
                    />
                    <div class="text-xs text-slate-600 text-center font-mono">
                      RGB: {{ hoverColor.r }}, {{ hoverColor.g }}, {{ hoverColor.b }}
                    </div>
                  </div>
                  <div v-else class="text-xs text-slate-400 text-center py-6">
                    将鼠标移到图片上查看预览
                  </div>
                </div>
              </div>
              
              <!-- <p class="text-xs text-slate-500 text-center">左侧显示原始图片，右侧显示光标位置的50x50像素放大预览</p> -->
            </div>
          </div>

          <!-- 按钮 -->
          <div class="flex justify-end gap-2 mt-6">
            <Button
              v-if="uploadedImage"
              @click="clearUploadedImage"
              variant="outline"
            >
              重新上传
            </Button>
            <Button
              v-if="tempPickedColor"
              @click="confirmPickedColor"
              class="bg-green-600 hover:bg-green-700"
            >
              确认取色
            </Button>
            <Button
              @click="closeImagePicker"
              variant="outline"
            >
              关闭
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  </div>
</template>

<style scoped>
/* 响应式优化 */
@media (max-width: 640px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
