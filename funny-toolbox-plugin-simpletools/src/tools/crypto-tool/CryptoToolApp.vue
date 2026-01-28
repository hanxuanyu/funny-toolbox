<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Input } from '@/components/ui/input'
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
import CryptoJS from 'crypto-js'

type CryptoAlgorithm = 
  | 'AES'
  | 'DES'
  | 'TripleDES'
  | 'RC4'
  | 'Rabbit'
  | 'MD5'
  | 'SHA1'
  | 'SHA256'
  | 'SHA512'
  | 'SHA3'
  | 'RIPEMD160'
  | 'HMAC-MD5'
  | 'HMAC-SHA1'
  | 'HMAC-SHA256'
  | 'HMAC-SHA512'

interface AlgorithmOption {
  value: CryptoAlgorithm
  label: string
  description: string
  requiresKey: boolean
  isReversible: boolean
  category: 'symmetric' | 'hash' | 'hmac'
}

const algorithms: AlgorithmOption[] = [
  // 对称加密算法（可逆）
  { value: 'AES', label: 'AES', description: '高级加密标准，最常用的对称加密算法，安全性高', requiresKey: true, isReversible: true, category: 'symmetric' },
  { value: 'DES', label: 'DES', description: '数据加密标准，较旧的加密算法，密钥较短', requiresKey: true, isReversible: true, category: 'symmetric' },
  { value: 'TripleDES', label: 'Triple DES', description: '三重 DES 加密，比 DES 更安全但速度较慢', requiresKey: true, isReversible: true, category: 'symmetric' },
  { value: 'RC4', label: 'RC4', description: '流加密算法，速度快但安全性较低', requiresKey: true, isReversible: true, category: 'symmetric' },
  { value: 'Rabbit', label: 'Rabbit', description: '高速流加密算法，性能优异', requiresKey: true, isReversible: true, category: 'symmetric' },
  
  // 哈希算法（不可逆）
  { value: 'MD5', label: 'MD5', description: '消息摘要算法，128 位哈希值，已不推荐用于安全场景', requiresKey: false, isReversible: false, category: 'hash' },
  { value: 'SHA1', label: 'SHA-1', description: '安全哈希算法 1，160 位，已被认为不安全', requiresKey: false, isReversible: false, category: 'hash' },
  { value: 'SHA256', label: 'SHA-256', description: '256 位安全哈希算法，广泛使用', requiresKey: false, isReversible: false, category: 'hash' },
  { value: 'SHA512', label: 'SHA-512', description: '512 位安全哈希算法，更高安全性', requiresKey: false, isReversible: false, category: 'hash' },
  { value: 'SHA3', label: 'SHA-3', description: '最新的安全哈希算法标准，基于 Keccak', requiresKey: false, isReversible: false, category: 'hash' },
  { value: 'RIPEMD160', label: 'RIPEMD-160', description: '160 位哈希算法，用于比特币等场景', requiresKey: false, isReversible: false, category: 'hash' },
  
  // HMAC 算法（带密钥的哈希）
  { value: 'HMAC-MD5', label: 'HMAC-MD5', description: '基于 MD5 的消息认证码', requiresKey: true, isReversible: false, category: 'hmac' },
  { value: 'HMAC-SHA1', label: 'HMAC-SHA1', description: '基于 SHA-1 的消息认证码', requiresKey: true, isReversible: false, category: 'hmac' },
  { value: 'HMAC-SHA256', label: 'HMAC-SHA256', description: '基于 SHA-256 的消息认证码', requiresKey: true, isReversible: false, category: 'hmac' },
  { value: 'HMAC-SHA512', label: 'HMAC-SHA512', description: '基于 SHA-512 的消息认证码', requiresKey: true, isReversible: false, category: 'hmac' },
]

const inputText = ref('')
const outputText = ref('')
const secretKey = ref('')
const selectedAlgorithm = ref<CryptoAlgorithm>('AES')

const currentAlgorithm = computed(() => 
  algorithms.find(a => a.value === selectedAlgorithm.value)
)

const showKeyInput = computed(() => 
  currentAlgorithm.value?.requiresKey ?? false
)

const canDecrypt = computed(() => 
  currentAlgorithm.value?.isReversible ?? false
)

// 加密函数（左侧明文 -> 右侧密文）
const encrypt = () => {
  try {
    if (!inputText.value) {
      toast.warning('请在左侧输入要加密的内容')
      return
    }

    const algorithm = selectedAlgorithm.value
    const text = inputText.value
    const key = secretKey.value

    if (currentAlgorithm.value?.requiresKey && !key) {
      toast.error('请输入密钥')
      return
    }

    let result = ''

    switch (algorithm) {
      // 对称加密
      case 'AES':
        result = CryptoJS.AES.encrypt(text, key).toString()
        break
      case 'DES':
        result = CryptoJS.DES.encrypt(text, key).toString()
        break
      case 'TripleDES':
        result = CryptoJS.TripleDES.encrypt(text, key).toString()
        break
      case 'RC4':
        result = CryptoJS.RC4.encrypt(text, key).toString()
        break
      case 'Rabbit':
        result = CryptoJS.Rabbit.encrypt(text, key).toString()
        break
      
      // 哈希算法
      case 'MD5':
        result = CryptoJS.MD5(text).toString()
        break
      case 'SHA1':
        result = CryptoJS.SHA1(text).toString()
        break
      case 'SHA256':
        result = CryptoJS.SHA256(text).toString()
        break
      case 'SHA512':
        result = CryptoJS.SHA512(text).toString()
        break
      case 'SHA3':
        result = CryptoJS.SHA3(text).toString()
        break
      case 'RIPEMD160':
        result = CryptoJS.RIPEMD160(text).toString()
        break
      
      // HMAC 算法
      case 'HMAC-MD5':
        result = CryptoJS.HmacMD5(text, key).toString()
        break
      case 'HMAC-SHA1':
        result = CryptoJS.HmacSHA1(text, key).toString()
        break
      case 'HMAC-SHA256':
        result = CryptoJS.HmacSHA256(text, key).toString()
        break
      case 'HMAC-SHA512':
        result = CryptoJS.HmacSHA512(text, key).toString()
        break
      
      default:
        throw new Error('不支持的算法')
    }

    outputText.value = result
    toast.success('加密成功', { description: '结果已显示在右侧' })
  } catch (error) {
    console.error('加密失败:', error)
    toast.error(`加密失败: ${error instanceof Error ? error.message : '未知错误'}`)
  }
}

// 解密函数（右侧密文 -> 左侧明文）
const decrypt = () => {
  try {
    if (!outputText.value) {
      toast.warning('请在右侧输入要解密的密文')
      return
    }

    if (!canDecrypt.value) {
      toast.error('该算法不支持解密（单向哈希）')
      return
    }

    const algorithm = selectedAlgorithm.value
    const text = outputText.value
    const key = secretKey.value

    if (!key) {
      toast.error('请输入密钥')
      return
    }

    let result = ''

    switch (algorithm) {
      case 'AES':
        result = CryptoJS.AES.decrypt(text, key).toString(CryptoJS.enc.Utf8)
        break
      case 'DES':
        result = CryptoJS.DES.decrypt(text, key).toString(CryptoJS.enc.Utf8)
        break
      case 'TripleDES':
        result = CryptoJS.TripleDES.decrypt(text, key).toString(CryptoJS.enc.Utf8)
        break
      case 'RC4':
        result = CryptoJS.RC4.decrypt(text, key).toString(CryptoJS.enc.Utf8)
        break
      case 'Rabbit':
        result = CryptoJS.Rabbit.decrypt(text, key).toString(CryptoJS.enc.Utf8)
        break
      
      default:
        throw new Error('不支持的解密算法')
    }

    if (!result) {
      throw new Error('解密失败，可能是密钥错误或数据损坏')
    }

    inputText.value = result
    toast.success('解密成功', { description: '结果已显示在左侧' })
  } catch (error) {
    console.error('解密失败:', error)
    toast.error(`解密失败: ${error instanceof Error ? error.message : '未知错误'}`)
  }
}

// 复制结果
const copyOutput = async () => {
  if (!outputText.value) {
    toast.warning('没有可复制的内容')
    return
  }

  try {
    await navigator.clipboard.writeText(outputText.value)
    toast.success('已复制到剪贴板')
  } catch (error) {
    toast.error('复制失败')
  }
}

// 复制输入
const copyInput = async () => {
  if (!inputText.value) {
    toast.warning('没有可复制的内容')
    return
  }

  try {
    await navigator.clipboard.writeText(inputText.value)
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

// 粘贴到输出
const pasteOutput = async () => {
  try {
    const text = await navigator.clipboard.readText()
    outputText.value = text
    toast.success('已粘贴内容')
  } catch {
    toast.error('粘贴失败', { description: '请检查浏览器权限' })
  }
}

// 清空输入
const clearInput = () => {
  inputText.value = ''
}

// 清空输出
const clearOutput = () => {
  outputText.value = ''
}

// 清空全部
const clear = () => {
  inputText.value = ''
  outputText.value = ''
  secretKey.value = ''
  toast.info('已清空')
}

// 交换输入输出
const swap = () => {
  if (!outputText.value) {
    toast.warning('没有可交换的内容')
    return
  }
  
  const temp = inputText.value
  inputText.value = outputText.value
  outputText.value = temp
  toast.info('已交换左右内容')
}

// 算法分类
const algorithmsByCategory = computed(() => {
  const grouped = {
    symmetric: algorithms.filter(a => a.category === 'symmetric'),
    hash: algorithms.filter(a => a.category === 'hash'),
    hmac: algorithms.filter(a => a.category === 'hmac'),
  }
  return grouped
})

// 生成随机密钥
const generateRandomKey = () => {
  const length = 32
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*'
  let key = ''
  for (let i = 0; i < length; i++) {
    key += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  secretKey.value = key
  toast.success('已生成随机密钥')
}
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <Toaster />
    <main class="mx-auto flex max-w-4xl flex-col gap-6 px-4 py-8">
      <header>
        <h1 class="text-2xl font-semibold tracking-tight">加解密工具</h1>
        <p class="mt-1 text-sm text-muted-foreground">
          支持对称加密（AES、DES、3DES、RC4、Rabbit）、哈希算法（MD5、SHA系列）及消息认证码（HMAC）。左侧为明文，右侧为密文。
        </p>
      </header>

      <Separator />

      <!-- 算法选择和密钥输入 -->
      <Card class="p-4">
        <div class="flex flex-col gap-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- 算法选择 -->
            <div class="flex flex-col gap-2">
              <Label>加密算法</Label>
              <Select v-model="selectedAlgorithm">
                <SelectTrigger class="w-full">
                  <SelectValue placeholder="选择算法" />
                </SelectTrigger>
                <SelectContent>
                  <!-- 对称加密 -->
                  <SelectGroup>
                    <div class="px-2 py-1.5 text-sm font-semibold text-primary">对称加密（可逆）</div>
                    <SelectItem
                      v-for="algo in algorithmsByCategory.symmetric"
                      :key="algo.value"
                      :value="algo.value"
                    >
                      <div class="flex flex-col gap-0.5">
                        <span class="font-medium">{{ algo.label }}</span>
                        <span class="text-xs text-muted-foreground">{{ algo.description }}</span>
                      </div>
                    </SelectItem>
                  </SelectGroup>
                  
                  <!-- 哈希算法 -->
                  <SelectGroup>
                    <div class="px-2 py-1.5 text-sm font-semibold text-primary mt-2">哈希算法（不可逆）</div>
                    <SelectItem
                      v-for="algo in algorithmsByCategory.hash"
                      :key="algo.value"
                      :value="algo.value"
                    >
                      <div class="flex flex-col gap-0.5">
                        <span class="font-medium">{{ algo.label }}</span>
                        <span class="text-xs text-muted-foreground">{{ algo.description }}</span>
                      </div>
                    </SelectItem>
                  </SelectGroup>
                  
                  <!-- HMAC -->
                  <SelectGroup>
                    <div class="px-2 py-1.5 text-sm font-semibold text-primary mt-2">消息认证码（HMAC）</div>
                    <SelectItem
                      v-for="algo in algorithmsByCategory.hmac"
                      :key="algo.value"
                      :value="algo.value"
                    >
                      <div class="flex flex-col gap-0.5">
                        <span class="font-medium">{{ algo.label }}</span>
                        <span class="text-xs text-muted-foreground">{{ algo.description }}</span>
                      </div>
                    </SelectItem>
                  </SelectGroup>
                </SelectContent>
              </Select>
            </div>

            <!-- 密钥输入 -->
            <div class="flex flex-col gap-2" v-if="showKeyInput">
              <Label>
                密钥
                <span class="text-xs font-normal text-muted-foreground ml-1">（必填）</span>
              </Label>
              <div class="flex gap-2">
                <Input
                  v-model="secretKey"
                  type="text"
                  placeholder="请输入密钥"
                  class="flex-1"
                />
                <Button
                  @click="generateRandomKey"
                  variant="outline"
                  size="default"
                  class="shrink-0"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/></svg>
                  生成
                </Button>
              </div>
            </div>
          </div>

          <!-- 算法说明 -->
          <div v-if="currentAlgorithm" class="mt-3 rounded-md bg-muted/50 p-3 text-xs text-muted-foreground">
            <p class="font-medium mb-1">💡 {{ currentAlgorithm.label }} 说明：</p>
            <p class="mb-2">{{ currentAlgorithm.description }}</p>
            <div class="flex gap-3">
              <span class="inline-flex items-center gap-1">
                {{ currentAlgorithm.isReversible ? '✓' : '✗' }}
                {{ currentAlgorithm.isReversible ? '可解密' : '不可逆' }}
              </span>
              <span class="inline-flex items-center gap-1">
                {{ currentAlgorithm.requiresKey ? '🔑' : '📝' }}
                {{ currentAlgorithm.requiresKey ? '需要密钥' : '无需密钥' }}
              </span>
            </div>
            <p class="mt-2 text-xs">
              <strong>操作：</strong>
              {{ currentAlgorithm.isReversible 
                ? '在左侧输入明文点击"加密"，或在右侧输入密文点击"解密"' 
                : '在左侧输入文本点击"加密"，生成哈希值显示在右侧（不可逆）' }}
            </p>
          </div>
        </div>
      </Card>

      <!-- 主要加解密区域 -->
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <!-- 左侧输入 -->
        <Card class="flex flex-col gap-3 p-4">
          <div class="flex items-center justify-between">
            <Label for="input-text">明文（原始文本）</Label>
            <div class="flex gap-1">
              <Button size="icon-sm" variant="ghost" @click="pasteInput" title="粘贴">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="8" height="4" x="8" y="2" rx="1" ry="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
              </Button>
              <Button size="icon-sm" variant="ghost" @click="copyInput" title="复制">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>
              </Button>
              <Button size="icon-sm" variant="ghost" @click="clearInput" title="清空">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
              </Button>
            </div>
          </div>
          <Textarea
            id="input-text"
            v-model="inputText"
            rows="10"
            placeholder="输入明文进行加密，或查看解密后的明文结果..."
            class="min-h-[240px] font-mono text-sm"
          />
          <div class="flex gap-2">
            <Button size="sm" class="flex-1" @click="encrypt">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              加密
            </Button>
          </div>
        </Card>

        <!-- 右侧输出 -->
        <Card class="flex flex-col gap-3 p-4">
          <div class="flex items-center justify-between">
            <Label for="output-text">密文（加密结果）</Label>
            <div class="flex gap-1">
              <Button size="icon-sm" variant="ghost" @click="pasteOutput" title="粘贴">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="8" height="4" x="8" y="2" rx="1" ry="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
              </Button>
              <Button size="icon-sm" variant="ghost" @click="copyOutput" title="复制">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>
              </Button>
              <Button size="icon-sm" variant="ghost" @click="clearOutput" title="清空">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
              </Button>
            </div>
          </div>
          <Textarea
            id="output-text"
            v-model="outputText"
            rows="10"
            placeholder="输入密文进行解密，或查看加密后的密文结果..."
            class="min-h-[240px] font-mono text-sm"
          />
          <div class="flex gap-2">
            <Button 
              size="sm" 
              class="flex-1" 
              variant="outline" 
              @click="decrypt"
              :disabled="!canDecrypt"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 9.9-1"/></svg>
              解密
            </Button>
          </div>
        </Card>
      </div>

      <!-- 使用说明 -->
      <Card class="p-4">
        <div class="rounded-md bg-muted/50 p-3 text-xs text-muted-foreground">
          <p class="font-medium mb-1">💡 使用说明：</p>
          <ul class="space-y-0.5 list-disc list-inside">
            <li><strong>加密操作：</strong>在左侧输入明文，点击"加密"按钮，密文显示在右侧</li>
            <li><strong>解密操作：</strong>在右侧输入密文，点击"解密"按钮，明文显示在左侧（仅对称加密支持）</li>
            <li><strong>哈希算法：</strong>MD5、SHA 系列为单向加密，仅支持加密，不可解密</li>
            <li><strong>HMAC：</strong>带密钥的哈希算法，用于消息认证和完整性校验，不可逆</li>
            <li><strong>安全提示：</strong>生产环境建议使用 AES-256 或 SHA-256 以上算法，妥善保管密钥</li>
          </ul>
        </div>
      </Card>
    </main>
  </div>
</template>
