<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Separator } from '@/components/ui/separator'
import { Toaster } from '@/components/ui/sonner'
import { toast } from 'vue-sonner'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

// JSON 示例模板
const examples = {
  user: {
    name: '用户信息',
    data: {
      id: 1001,
      username: 'john_doe',
      email: 'john@example.com',
      profile: {
        firstName: 'John',
        lastName: 'Doe',
        age: 28,
        address: {
          street: '123 Main St',
          city: 'San Francisco',
          country: 'USA',
        },
      },
      roles: ['user', 'admin'],
      isActive: true,
      createdAt: '2024-01-15T10:30:00Z',
    },
  },
  api: {
    name: 'API 响应',
    data: {
      status: 'success',
      code: 200,
      message: 'Request processed successfully',
      data: {
        items: [
          { id: 1, name: 'Product A', price: 29.99, inStock: true },
          { id: 2, name: 'Product B', price: 49.99, inStock: false },
          { id: 3, name: 'Product C', price: 19.99, inStock: true },
        ],
        pagination: {
          page: 1,
          pageSize: 10,
          total: 3,
          hasMore: false,
        },
      },
      timestamp: Date.now(),
    },
  },
  config: {
    name: '配置文件',
    data: {
      app: {
        name: 'MyApp',
        version: '1.0.0',
        environment: 'production',
      },
      database: {
        host: 'localhost',
        port: 5432,
        name: 'mydb',
        pool: {
          min: 2,
          max: 10,
        },
      },
      features: {
        analytics: true,
        notifications: false,
        darkMode: true,
      },
      limits: {
        maxUploadSize: 10485760,
        rateLimit: 100,
      },
    },
  },
  array: {
    name: '数组数据',
    data: [
      { id: 1, name: 'Alice', score: 95 },
      { id: 2, name: 'Bob', score: 87 },
      { id: 3, name: 'Charlie', score: 92 },
      { id: 4, name: 'David', score: 78 },
    ],
  },
}

const input = ref('')
const output = ref('')
const lastValid = ref('')
const selectedExample = ref<string>('')
const jsonPathQuery = ref('$')
const jsonPathResult = ref('')

// 当前模式
type Mode = 'format' | 'jsonpath' | 'stats' | 'escape'
const currentMode = ref<Mode>('format')

// JSON 统计信息
interface JsonStats {
  totalKeys: number
  totalValues: number
  depth: number
  objectCount: number
  arrayCount: number
  stringCount: number
  numberCount: number
  booleanCount: number
  nullCount: number
}

const jsonStats = ref<JsonStats | null>(null)

// 格式化 JSON
function formatJson(space = 2) {
  try {
    const parsed = JSON.parse(input.value)
    const formatted = JSON.stringify(parsed, null, space)
    output.value = formatted
    lastValid.value = formatted
    toast.success('格式化成功', { description: 'JSON 语法校验通过' })
  } catch (error: any) {
    toast.error('JSON 解析失败', { 
      description: error.message || '请检查输入是否为合法 JSON' 
    })
  }
}

// 压缩 JSON
function minifyJson() {
  try {
    const parsed = JSON.parse(input.value)
    const formatted = JSON.stringify(parsed)
    output.value = formatted
    lastValid.value = formatted
    toast.success('压缩成功', { description: '已生成压缩后的 JSON 字符串' })
  } catch (error: any) {
    toast.error('JSON 解析失败', { 
      description: error.message || '请检查输入是否为合法 JSON' 
    })
  }
}

// 转义 JSON（用于嵌入字符串）
function escapeJson() {
  try {
    const parsed = JSON.parse(input.value)
    const minified = JSON.stringify(parsed)
    const escaped = JSON.stringify(minified)
    output.value = escaped
    toast.success('转义成功', { description: '已生成可嵌入的字符串' })
  } catch (error: any) {
    toast.error('JSON 解析失败', { 
      description: error.message || '请检查输入是否为合法 JSON' 
    })
  }
}

// 反转义 JSON
function unescapeJson() {
  try {
    // 尝试解析转义的字符串
    const unescaped = JSON.parse(input.value)
    // 再解析内部的 JSON
    const parsed = JSON.parse(unescaped)
    const formatted = JSON.stringify(parsed, null, 2)
    output.value = formatted
    toast.success('反转义成功', { description: '已还原 JSON 格式' })
  } catch (error: any) {
    toast.error('反转义失败', { 
      description: error.message || '请检查输入格式' 
    })
  }
}

// 排序 JSON 键
function sortJsonKeys() {
  try {
    const parsed = JSON.parse(input.value)
    const sorted = sortObjectKeys(parsed)
    const formatted = JSON.stringify(sorted, null, 2)
    output.value = formatted
    lastValid.value = formatted
    toast.success('排序成功', { description: 'JSON 键已按字母顺序排序' })
  } catch (error: any) {
    toast.error('JSON 解析失败', { 
      description: error.message || '请检查输入是否为合法 JSON' 
    })
  }
}

// 递归排序对象键
function sortObjectKeys(obj: any): any {
  if (Array.isArray(obj)) {
    return obj.map(item => sortObjectKeys(item))
  }
  if (obj !== null && typeof obj === 'object') {
    return Object.keys(obj)
      .sort()
      .reduce((result: any, key) => {
        result[key] = sortObjectKeys(obj[key])
        return result
      }, {})
  }
  return obj
}

// 简单的 JSONPath 查询实现
function queryJsonPath() {
  if (!jsonPathQuery.value.trim()) {
    toast.warning('请输入 JSONPath 表达式')
    return
  }

  try {
    const parsed = JSON.parse(input.value)
    const result = evaluateJsonPath(parsed, jsonPathQuery.value)
    jsonPathResult.value = JSON.stringify(result, null, 2)
    toast.success('查询成功', { description: '已提取匹配的数据' })
  } catch (error: any) {
    toast.error('查询失败', { 
      description: error.message || '请检查 JSON 和表达式' 
    })
  }
}

// 简化的 JSONPath 求值（支持基本语法）
function evaluateJsonPath(data: any, path: string): any {
  // 移除开头的 $
  let query = path.trim()
  if (query.startsWith('$')) {
    query = query.slice(1)
  }
  
  // 如果是根路径，返回整个对象
  if (!query || query === '.') {
    return data
  }

  // 处理点号分隔的路径
  const parts = query.split('.').filter(p => p)
  let current = data

  for (const part of parts) {
    // 处理数组索引 [n]
    const arrayMatch = part.match(/^(\w+)?\[(\d+|\*)\]$/)
    if (arrayMatch) {
      const [, key, index] = arrayMatch
      
      if (key) {
        current = current[key]
      }
      
      if (!Array.isArray(current)) {
        throw new Error(`路径 ${part} 不是数组`)
      }
      
      if (index === '*') {
        return current // 返回整个数组
      } else {
        current = current[parseInt(index)]
      }
    } else {
      // 普通属性访问
      if (current === null || current === undefined) {
        throw new Error(`无法访问路径: ${part}`)
      }
      current = current[part]
    }
    
    if (current === undefined) {
      throw new Error(`路径不存在: ${part}`)
    }
  }

  return current
}

// 统计 JSON 信息
function analyzeJson() {
  try {
    const parsed = JSON.parse(input.value)
    const stats: JsonStats = {
      totalKeys: 0,
      totalValues: 0,
      depth: 0,
      objectCount: 0,
      arrayCount: 0,
      stringCount: 0,
      numberCount: 0,
      booleanCount: 0,
      nullCount: 0,
    }

    function analyze(obj: any, depth: number) {
      stats.depth = Math.max(stats.depth, depth)
      
      if (obj === null) {
        stats.nullCount++
        stats.totalValues++
      } else if (Array.isArray(obj)) {
        stats.arrayCount++
        stats.totalValues++
        obj.forEach(item => analyze(item, depth + 1))
      } else if (typeof obj === 'object') {
        stats.objectCount++
        stats.totalValues++
        Object.keys(obj).forEach(key => {
          stats.totalKeys++
          analyze(obj[key], depth + 1)
        })
      } else if (typeof obj === 'string') {
        stats.stringCount++
        stats.totalValues++
      } else if (typeof obj === 'number') {
        stats.numberCount++
        stats.totalValues++
      } else if (typeof obj === 'boolean') {
        stats.booleanCount++
        stats.totalValues++
      }
    }

    analyze(parsed, 0)
    jsonStats.value = stats
    currentMode.value = 'stats'
    toast.success('分析完成', { description: '已生成 JSON 结构统计' })
  } catch (error: any) {
    toast.error('分析失败', { 
      description: error.message || '请检查输入是否为合法 JSON' 
    })
  }
}

// 复制输出
function copyOutput() {
  if (!output.value) {
    toast.warning('没有可复制的内容')
    return
  }
  navigator.clipboard.writeText(output.value)
  toast.success('已复制到剪贴板')
}

// 复制 JSONPath 结果
function copyJsonPathResult() {
  if (!jsonPathResult.value) {
    toast.warning('没有可复制的内容')
    return
  }
  navigator.clipboard.writeText(jsonPathResult.value)
  toast.success('已复制到剪贴板')
}

// 使用最近合法 JSON
function useLastValid() {
  if (!lastValid.value) {
    toast.warning('没有可用的历史记录')
    return
  }
  input.value = lastValid.value
  toast.info('已恢复到最近一次合法 JSON')
}

// 加载示例
function loadExample() {
  if (!selectedExample.value) return
  
  const example = examples[selectedExample.value as keyof typeof examples]
  if (example) {
    input.value = JSON.stringify(example.data, null, 2)
    toast.success('示例已加载', { description: example.name })
  }
}

// 清空输入
function clearInput() {
  input.value = ''
  output.value = ''
  jsonPathResult.value = ''
  jsonStats.value = null
}

// 粘贴
async function pasteInput() {
  try {
    const text = await navigator.clipboard.readText()
    input.value = text
    toast.success('已粘贴内容')
  } catch {
    toast.error('粘贴失败', { description: '请检查浏览器权限' })
  }
}

// 验证 JSON
function validateJson() {
  try {
    JSON.parse(input.value)
    toast.success('JSON 格式正确', { description: '语法验证通过' })
  } catch (error: any) {
    const message = error.message || '未知错误'
    // 尝试提取错误位置
    const posMatch = message.match(/position (\d+)/)
    const description = posMatch 
      ? `错误位置: 第 ${posMatch[1]} 个字符` 
      : message
    toast.error('JSON 格式错误', { description })
  }
}

// 切换到格式化模式
function switchToFormatMode() {
  currentMode.value = 'format'
  jsonStats.value = null
}

// 切换到 JSONPath 模式
function switchToJsonPathMode() {
  currentMode.value = 'jsonpath'
  jsonStats.value = null
}

// 切换到转义模式
function switchToEscapeMode() {
  currentMode.value = 'escape'
  jsonStats.value = null
}
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <Toaster />
    <main class="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8">
      <header>
        <h1 class="text-2xl font-semibold tracking-tight">JSON 工具箱</h1>
        <p class="mt-1 text-sm text-muted-foreground">
          格式化、压缩、验证、查询、分析 JSON 数据的多功能工具
        </p>
      </header>

      <Separator />

      <!-- 快捷操作栏 -->
      <Card class="p-4">
        <div class="flex flex-col gap-4">
          <div class="flex flex-wrap items-center gap-2">
            <Label class="text-sm font-medium">快捷操作：</Label>
            <div class="flex flex-wrap gap-2">
              <Button size="sm" variant="secondary" @click="pasteInput">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mr-1"><rect width="8" height="4" x="8" y="2" rx="1" ry="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
                粘贴
              </Button>
              <Button size="sm" variant="secondary" @click="clearInput">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mr-1"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
                清空
              </Button>
              <Button size="sm" variant="secondary" @click="validateJson">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mr-1"><path d="M20 6 9 17l-5-5"/></svg>
                验证格式
              </Button>
              <Button size="sm" variant="secondary" @click="useLastValid">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mr-1"><path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M8 16H3v5"/></svg>
                恢复历史
              </Button>
            </div>
          </div>
          
          <div class="flex flex-wrap items-center gap-2">
            <Label class="text-sm font-medium">加载示例：</Label>
            <div class="flex flex-wrap items-center gap-2">
              <Select v-model="selectedExample">
                <SelectTrigger class="w-[180px] h-8">
                  <SelectValue placeholder="选择示例模板" />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    <SelectItem value="user">{{ examples.user.name }}</SelectItem>
                    <SelectItem value="api">{{ examples.api.name }}</SelectItem>
                    <SelectItem value="config">{{ examples.config.name }}</SelectItem>
                    <SelectItem value="array">{{ examples.array.name }}</SelectItem>
                  </SelectGroup>
                </SelectContent>
              </Select>
              <Button size="sm" variant="outline" @click="loadExample" :disabled="!selectedExample">
                载入
              </Button>
            </div>
          </div>
        </div>
      </Card>

      <!-- 主要操作区域 -->
      <div class="grid gap-4 md:grid-cols-2">
        <!-- 左侧输入 -->
        <Card class="flex flex-col gap-3 p-4">
          <Label for="json-input">输入 JSON</Label>
          <Textarea
            id="json-input"
            v-model="input"
            rows="18"
            placeholder='粘贴或输入 JSON，例如: {"name": "value", "list": [1, 2, 3]}'
            class="min-h-[400px] font-mono text-sm"
          />
        </Card>

        <!-- 右侧输出/功能区 -->
        <Card class="flex flex-col gap-3 p-4">
          <!-- 功能选项卡 -->
          <div class="flex gap-1 border-b pb-2">
            <Button 
              size="sm" 
              :variant="currentMode === 'format' ? 'default' : 'ghost'"
              @click="switchToFormatMode"
            >
              格式化
            </Button>
            <Button 
              size="sm" 
              :variant="currentMode === 'jsonpath' ? 'default' : 'ghost'"
              @click="switchToJsonPathMode"
            >
              JSONPath 查询
            </Button>
            <Button 
              size="sm" 
              :variant="currentMode === 'escape' ? 'default' : 'ghost'"
              @click="switchToEscapeMode"
            >
              转义工具
            </Button>
            <Button 
              size="sm" 
              :variant="currentMode === 'stats' ? 'default' : 'ghost'"
              @click="analyzeJson"
            >
              结构分析
            </Button>
          </div>

          <!-- 格式化模式 -->
          <div v-if="currentMode === 'format'" class="flex flex-col gap-3">
            <div class="flex flex-wrap gap-2">
              <Button size="sm" @click="formatJson(2)">
                格式化（2 空格）
              </Button>
              <Button size="sm" variant="outline" @click="formatJson(4)">
                格式化（4 空格）
              </Button>
              <Button size="sm" variant="outline" @click="minifyJson">
                压缩
              </Button>
              <Button size="sm" variant="outline" @click="sortJsonKeys">
                排序键
              </Button>
            </div>

            <Separator />

            <div class="flex items-center justify-between">
              <Label for="json-output">输出结果</Label>
              <Button size="icon-sm" variant="ghost" @click="copyOutput" title="复制">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>
              </Button>
            </div>
            <Textarea
              id="json-output"
              v-model="output"
              rows="12"
              placeholder="格式化结果将显示在这里..."
              class="min-h-[280px] font-mono text-sm"
              readonly
            />
          </div>

          <!-- JSONPath 查询模式 -->
          <div v-else-if="currentMode === 'jsonpath'" class="flex flex-col gap-3">
            <div class="space-y-2">
              <Label for="jsonpath-query">JSONPath 表达式</Label>
              <div class="flex gap-2">
                <Input
                  id="jsonpath-query"
                  v-model="jsonPathQuery"
                  placeholder="例如: $.data.items[0].name"
                  class="font-mono text-sm"
                />
                <Button size="sm" @click="queryJsonPath">
                  查询
                </Button>
              </div>
              <p class="text-xs text-muted-foreground">
                支持语法：<code class="bg-muted px-1 py-0.5 rounded">$.key</code>、
                <code class="bg-muted px-1 py-0.5 rounded">$.key.subkey</code>、
                <code class="bg-muted px-1 py-0.5 rounded">$.array[0]</code>、
                <code class="bg-muted px-1 py-0.5 rounded">$.array[*]</code>
              </p>
            </div>

            <Separator />

            <div class="flex items-center justify-between">
              <Label for="jsonpath-result">查询结果</Label>
              <Button size="icon-sm" variant="ghost" @click="copyJsonPathResult" title="复制">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>
              </Button>
            </div>
            <Textarea
              id="jsonpath-result"
              v-model="jsonPathResult"
              rows="12"
              placeholder="查询结果将显示在这里..."
              class="min-h-[280px] font-mono text-sm"
              readonly
            />
          </div>

          <!-- 转义工具模式 -->
          <div v-else-if="currentMode === 'escape'" class="flex flex-col gap-3">
            <div class="flex flex-wrap gap-2">
              <Button size="sm" @click="escapeJson">
                转义（JSON → 字符串）
              </Button>
              <Button size="sm" variant="outline" @click="unescapeJson">
                反转义（字符串 → JSON）
              </Button>
            </div>
            
            <p class="text-xs text-muted-foreground">
              转义后的 JSON 可以安全地嵌入到代码字符串中，例如：<code class="bg-muted px-1 py-0.5 rounded">const json = "{\"key\":\"value\"}"</code>
            </p>

            <Separator />

            <div class="flex items-center justify-between">
              <Label for="escape-output">输出结果</Label>
              <Button size="icon-sm" variant="ghost" @click="copyOutput" title="复制">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>
              </Button>
            </div>
            <Textarea
              id="escape-output"
              v-model="output"
              rows="12"
              placeholder="转义结果将显示在这里..."
              class="min-h-[280px] font-mono text-sm"
              readonly
            />
          </div>

          <!-- 统计分析模式 -->
          <div v-else-if="currentMode === 'stats' && jsonStats" class="flex flex-col gap-3">
            <Label>JSON 结构统计</Label>
            <div class="grid grid-cols-2 gap-3 text-sm">
              <div class="rounded-md border p-3">
                <div class="text-muted-foreground">总键数</div>
                <div class="text-2xl font-semibold">{{ jsonStats.totalKeys }}</div>
              </div>
              <div class="rounded-md border p-3">
                <div class="text-muted-foreground">总值数</div>
                <div class="text-2xl font-semibold">{{ jsonStats.totalValues }}</div>
              </div>
              <div class="rounded-md border p-3">
                <div class="text-muted-foreground">嵌套深度</div>
                <div class="text-2xl font-semibold">{{ jsonStats.depth }}</div>
              </div>
              <div class="rounded-md border p-3">
                <div class="text-muted-foreground">对象数</div>
                <div class="text-2xl font-semibold">{{ jsonStats.objectCount }}</div>
              </div>
              <div class="rounded-md border p-3">
                <div class="text-muted-foreground">数组数</div>
                <div class="text-2xl font-semibold">{{ jsonStats.arrayCount }}</div>
              </div>
              <div class="rounded-md border p-3">
                <div class="text-muted-foreground">字符串数</div>
                <div class="text-2xl font-semibold">{{ jsonStats.stringCount }}</div>
              </div>
              <div class="rounded-md border p-3">
                <div class="text-muted-foreground">数字数</div>
                <div class="text-2xl font-semibold">{{ jsonStats.numberCount }}</div>
              </div>
              <div class="rounded-md border p-3">
                <div class="text-muted-foreground">布尔值数</div>
                <div class="text-2xl font-semibold">{{ jsonStats.booleanCount }}</div>
              </div>
            </div>
          </div>
        </Card>
      </div>

      <!-- 使用说明 -->
      <Card class="p-4 bg-muted/30">
        <details class="cursor-pointer">
          <summary class="text-sm font-medium">💡 使用说明与技巧</summary>
          <div class="mt-3 space-y-2 text-xs text-muted-foreground">
            <div><strong>格式化：</strong>美化 JSON 结构，支持 2/4 空格缩进；压缩可去除所有空白字符</div>
            <div><strong>排序键：</strong>按字母顺序重排所有对象的键，便于对比和版本控制</div>
            <div><strong>JSONPath 查询：</strong>使用类似 <code class="bg-muted px-1">$.users[0].name</code> 的表达式提取数据</div>
            <div><strong>转义工具：</strong>将 JSON 转换为可嵌入代码的转义字符串，或反向操作</div>
            <div><strong>结构分析：</strong>统计 JSON 的键值数量、嵌套深度、数据类型分布等信息</div>
            <div><strong>示例模板：</strong>提供常见的 JSON 结构示例，快速上手测试</div>
          </div>
        </details>
      </Card>
    </main>
  </div>
</template>
