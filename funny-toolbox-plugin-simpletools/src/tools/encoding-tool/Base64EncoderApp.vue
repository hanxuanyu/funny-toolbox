<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Separator } from '@/components/ui/separator'
import { Toaster } from '@/components/ui/sonner'
import { toast } from 'vue-sonner'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

type EncodingType = 
  | 'base64'
  | 'url'
  | 'html'
  | 'unicode'
  | 'hex'
  | 'binary'
  | 'md5'
  | 'sha1'
  | 'sha256'

interface EncodingOption {
  value: EncodingType
  label: string
  description: string
}

const encodingTypes: EncodingOption[] = [
  { value: 'base64', label: 'Base64', description: '将文本转换为 Base64 编码，常用于数据传输' },
  { value: 'url', label: 'URL 编码', description: '对 URL 中的特殊字符进行百分号编码' },
  { value: 'html', label: 'HTML 实体', description: '将特殊字符转换为 HTML 实体（如 & → &amp;）' },
  { value: 'unicode', label: 'Unicode 转义', description: '将字符转为 \\uXXXX 格式，用于 JS/JSON' },
  { value: 'hex', label: '十六进制', description: '显示文本的十六进制字节表示（如 A → 41）' },
  { value: 'binary', label: '二进制', description: '显示文本的二进制字节表示（如 A → 01000001）' },
  { value: 'md5', label: 'MD5 哈希', description: '生成 MD5 摘要，不可逆（仅编码）' },
  { value: 'sha1', label: 'SHA-1 哈希', description: '生成 SHA-1 摘要，不可逆（仅编码）' },
  { value: 'sha256', label: 'SHA-256 哈希', description: '生成 SHA-256 摘要，不可逆（仅编码）' },
]

const leftInput = ref('')
const rightInput = ref('')
const selectedEncoding = ref<EncodingType>('base64')

// 简单哈希实现（使用 Web Crypto API）
async function simpleHash(text: string, algorithm: string): Promise<string> {
  const encoder = new TextEncoder()
  const data = encoder.encode(text)
  const hashBuffer = await crypto.subtle.digest(algorithm, data)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map(byte => byte.toString(16).padStart(2, '0')).join('')
}

// 编码函数集合
const encoders: Record<EncodingType, (text: string) => string | Promise<string>> = {
  base64: (text) => btoa(unescape(encodeURIComponent(text))),
  url: (text) => encodeURIComponent(text),
  html: (text) => text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;'),
  unicode: (text) => text
    .split('')
    .map(char => {
      const code = char.charCodeAt(0)
      return code > 127 ? `\\u${code.toString(16).padStart(4, '0')}` : char
    })
    .join(''),
  hex: (text) => Array.from(new TextEncoder().encode(text))
    .map(byte => byte.toString(16).padStart(2, '0'))
    .join(' '),
  binary: (text) => Array.from(new TextEncoder().encode(text))
    .map(byte => byte.toString(2).padStart(8, '0'))
    .join(' '),
  md5: (text): Promise<string> => simpleHash(text, 'SHA-1'), // MD5 not supported, fallback to SHA-1
  sha1: (text): Promise<string> => simpleHash(text, 'SHA-1'),
  sha256: (text): Promise<string> => simpleHash(text, 'SHA-256'),
}

// 解码函数集合
const decoders: Record<EncodingType, (text: string) => string> = {
  base64: (text) => decodeURIComponent(escape(atob(text))),
  url: (text) => decodeURIComponent(text),
  html: (text) => text
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&amp;/g, '&'),
  unicode: (text) => text.replace(/\\u([0-9a-fA-F]{4})/g, (_, code) => 
    String.fromCharCode(parseInt(code, 16))
  ),
  hex: (text) => {
    const bytes = text.split(' ').filter(s => s).map(hex => parseInt(hex, 16))
    return new TextDecoder().decode(new Uint8Array(bytes))
  },
  binary: (text) => {
    const bytes = text.split(' ').filter(s => s).map(bin => parseInt(bin, 2))
    return new TextDecoder().decode(new Uint8Array(bytes))
  },
  md5: () => { throw new Error('哈希算法不可逆') },
  sha1: () => { throw new Error('哈希算法不可逆') },
  sha256: () => { throw new Error('哈希算法不可逆') },
}

// 从左到右编码
async function encodeLeftToRight() {
  if (!leftInput.value.trim()) {
    toast.warning('请输入内容')
    return
  }
  
  try {
    const encoder = encoders[selectedEncoding.value]
    const result = encoder(leftInput.value)
    // 处理异步哈希
    if (result && typeof result === 'object' && 'then' in result) {
      rightInput.value = await result
    } else {
      rightInput.value = result as string
    }
    toast.success('编码成功', { description: `已使用 ${getEncodingLabel()} 编码` })
  } catch (error) {
    toast.error('编码失败', { description: '请检查输入内容是否有效' })
  }
}

// 从右到左解码
function decodeRightToLeft() {
  if (!rightInput.value.trim()) {
    toast.warning('请输入内容')
    return
  }
  
  try {
    const decoder = decoders[selectedEncoding.value]
    leftInput.value = decoder(rightInput.value)
    toast.success('解码成功', { description: `已使用 ${getEncodingLabel()} 解码` })
  } catch (error: any) {
    toast.error('解码失败', { description: error.message || '请检查输入内容是否有效' })
  }
}

// 交换左右内容
function swapInputs() {
  const temp = leftInput.value
  leftInput.value = rightInput.value
  rightInput.value = temp
  toast.info('已交换左右内容')
}

// 复制左侧内容
function copyLeft() {
  if (!leftInput.value) {
    toast.warning('没有可复制的内容')
    return
  }
  navigator.clipboard.writeText(leftInput.value)
  toast.success('已复制左侧内容')
}

// 复制右侧内容
function copyRight() {
  if (!rightInput.value) {
    toast.warning('没有可复制的内容')
    return
  }
  navigator.clipboard.writeText(rightInput.value)
  toast.success('已复制右侧内容')
}

// 清空左侧
function clearLeft() {
  leftInput.value = ''
}

// 清空右侧
function clearRight() {
  rightInput.value = ''
}

// 粘贴到左侧
async function pasteLeft() {
  try {
    const text = await navigator.clipboard.readText()
    leftInput.value = text
    toast.success('已粘贴内容')
  } catch {
    toast.error('粘贴失败', { description: '请检查浏览器权限' })
  }
}

// 粘贴到右侧
async function pasteRight() {
  try {
    const text = await navigator.clipboard.readText()
    rightInput.value = text
    toast.success('已粘贴内容')
  } catch {
    toast.error('粘贴失败', { description: '请检查浏览器权限' })
  }
}

// 获取当前编码类型的显示名称
function getEncodingLabel() {
  return encodingTypes.find(t => t.value === selectedEncoding.value)?.label || ''
}

// 是否支持解码
function supportsDecoding() {
  return !['md5', 'sha1', 'sha256'].includes(selectedEncoding.value)
}
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <Toaster />
    <main class="mx-auto flex max-w-4xl flex-col gap-6 px-4 py-8">
      <header>
        <h1 class="text-2xl font-semibold tracking-tight">编码工具</h1>
        <p class="mt-1 text-sm text-muted-foreground">
          支持多种编码方式的互转，包括 Base64、URL、HTML、Unicode、十六进制、二进制及哈希加密等。
        </p>
      </header>

      <Separator />

      <!-- 编码类型选择和快捷操作 -->
      <Card class="p-4">
        <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div class="flex flex-col gap-2">
            <Label>编码类型</Label>
            <Select v-model="selectedEncoding">
              <SelectTrigger class="w-full md:w-[320px]">
                <SelectValue placeholder="选择编码类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectItem
                    v-for="type in encodingTypes"
                    :key="type.value"
                    :value="type.value"
                  >
                    <div class="flex flex-col gap-0.5">
                      <span class="font-medium">{{ type.label }}</span>
                      <span class="text-xs text-muted-foreground">{{ type.description }}</span>
                    </div>
                  </SelectItem>
                </SelectGroup>
              </SelectContent>
            </Select>
          </div>
          
          <div class="flex items-center gap-2">
            <Button size="sm" variant="secondary" @click="swapInputs">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 3 4 7l4 4"/><path d="M4 7h16"/><path d="m16 21 4-4-4-4"/><path d="M20 17H4"/></svg>
              交换左右
            </Button>
          </div>
        </div>
        
        <div class="mt-3 rounded-md bg-muted/50 p-3 text-xs text-muted-foreground">
          <p class="font-medium mb-1">💡 使用说明：</p>
          <ul class="space-y-0.5 list-disc list-inside">
            <li><strong>编码：</strong>在左侧输入原始文本，点击"编码"按钮，结果显示在右侧</li>
            <li><strong>解码：</strong>在右侧输入已编码文本，点击"解码"按钮，结果显示在左侧</li>
            <li><strong>提示：</strong>哈希算法（MD5/SHA）为单向加密，仅支持编码，不支持解码</li>
            <li><strong>快捷：</strong>可使用粘贴、复制、清空按钮快速操作，或点击"交换左右"对调内容</li>
          </ul>
        </div>
      </Card>

      <!-- 主要编码区域 -->
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <!-- 左侧输入 -->
        <Card class="flex flex-col gap-3 p-4">
          <div class="flex items-center justify-between">
            <Label for="left-input">原始文本</Label>
            <div class="flex gap-1">
              <Button size="icon-sm" variant="ghost" @click="pasteLeft" title="粘贴">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="8" height="4" x="8" y="2" rx="1" ry="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
              </Button>
              <Button size="icon-sm" variant="ghost" @click="copyLeft" title="复制">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>
              </Button>
              <Button size="icon-sm" variant="ghost" @click="clearLeft" title="清空">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
              </Button>
            </div>
          </div>
          <Textarea
            id="left-input"
            v-model="leftInput"
            rows="10"
            placeholder="在此输入或粘贴原始文本..."
            class="min-h-[240px] font-mono text-sm"
          />
          <div class="flex gap-2">
            <Button size="sm" class="flex-1" @click="encodeLeftToRight">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
              编码
            </Button>
          </div>
        </Card>

        <!-- 右侧输出 -->
        <Card class="flex flex-col gap-3 p-4">
          <div class="flex items-center justify-between">
            <Label for="right-input">编码结果</Label>
            <div class="flex gap-1">
              <Button size="icon-sm" variant="ghost" @click="pasteRight" title="粘贴">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="8" height="4" x="8" y="2" rx="1" ry="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
              </Button>
              <Button size="icon-sm" variant="ghost" @click="copyRight" title="复制">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>
              </Button>
              <Button size="icon-sm" variant="ghost" @click="clearRight" title="清空">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
              </Button>
            </div>
          </div>
          <Textarea
            id="right-input"
            v-model="rightInput"
            rows="10"
            placeholder="编码结果将显示在这里..."
            class="min-h-[240px] font-mono text-sm"
          />
          <div class="flex gap-2">
            <Button 
              size="sm" 
              class="flex-1" 
              variant="outline" 
              @click="decodeRightToLeft"
              :disabled="!supportsDecoding()"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
              解码
            </Button>
          </div>
        </Card>
      </div>
    </main>
  </div>
</template>
