<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { DateFormatter, getLocalTimeZone, today, CalendarDate } from '@internationalized/date'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Input } from '@/components/ui/input'
import { Toaster } from '@/components/ui/sonner'
import { toast } from 'vue-sonner'
import { Badge } from '@/components/ui/badge'
import CalendarWithSelect from '@/components/ui/calendar/CalendarWithSelect.vue'
import { Separator } from '@/components/ui/separator'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { CalendarIcon, Clock, Copy, RefreshCw } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

// ==================== 核心状态 ====================
const inputText = ref('')
const selectedDateValue = ref(today(getLocalTimeZone()))
const selectedTime = ref({ 
  hours: new Date().getHours(), 
  minutes: new Date().getMinutes(),
  seconds: new Date().getSeconds(),
  milliseconds: new Date().getMilliseconds()
})
const inputTimezone = ref('Asia/Shanghai')
const isLiveMode = ref(true) // 是否实时更新模式
const isUpdatingFromLive = ref(false) // 标记是否正在从实时模式更新
let liveUpdateTimer: number | null = null

// 日期格式化器（用于显示）
const df = new DateFormatter('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

const dateFormatter = new DateFormatter('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
})

// 将 DateValue 转换为 JavaScript Date
function dateValueToDate(dateValue: any): Date {
  if (!dateValue) return new Date()
  return new Date(dateValue.year, dateValue.month - 1, dateValue.day)
}

// 将 JavaScript Date 转换为 DateValue
function dateToDateValue(date: Date): CalendarDate {
  return new CalendarDate(date.getFullYear(), date.getMonth() + 1, date.getDate())
}

// ==================== 时区列表 ====================
const timezones = [
  { value: 'Asia/Shanghai', label: '中国 (UTC+8)', offset: '+08:00' },
  { value: 'Asia/Tokyo', label: '日本 (UTC+9)', offset: '+09:00' },
  { value: 'Asia/Seoul', label: '韩国 (UTC+9)', offset: '+09:00' },
  { value: 'Asia/Hong_Kong', label: '香港 (UTC+8)', offset: '+08:00' },
  { value: 'Asia/Singapore', label: '新加坡 (UTC+8)', offset: '+08:00' },
  { value: 'Asia/Dubai', label: '迪拜 (UTC+4)', offset: '+04:00' },
  { value: 'Europe/London', label: '伦敦 (UTC+0/+1)', offset: '+00:00' },
  { value: 'Europe/Paris', label: '巴黎 (UTC+1/+2)', offset: '+01:00' },
  { value: 'Europe/Berlin', label: '柏林 (UTC+1/+2)', offset: '+01:00' },
  { value: 'Europe/Moscow', label: '莫斯科 (UTC+3)', offset: '+03:00' },
  { value: 'America/New_York', label: '纽约 (UTC-5/-4)', offset: '-05:00' },
  { value: 'America/Chicago', label: '芝加哥 (UTC-6/-5)', offset: '-06:00' },
  { value: 'America/Los_Angeles', label: '洛杉矶 (UTC-8/-7)', offset: '-08:00' },
  { value: 'America/Toronto', label: '多伦多 (UTC-5/-4)', offset: '-05:00' },
  { value: 'Australia/Sydney', label: '悉尼 (UTC+10/+11)', offset: '+10:00' },
  { value: 'Pacific/Auckland', label: '奥克兰 (UTC+12/+13)', offset: '+12:00' },
  { value: 'UTC', label: 'UTC (世界协调时)', offset: '+00:00' },
]

// ==================== 智能输入解析 ====================
interface ParsedInput {
  type: 'timestamp' | 'datetime' | 'unknown'
  date?: Date
  timestampMs?: number
  timestampSec?: number
  isValid: boolean
}

const parsedInput = computed<ParsedInput>(() => {
  const input = inputText.value.trim()
  if (!input) {
    return { type: 'unknown', isValid: false }
  }

  // 尝试解析为时间戳（纯数字）
  if (/^\d+$/.test(input)) {
    const num = parseInt(input)
    
    // 10位数字 - 秒级时间戳
    if (input.length === 10) {
      const date = new Date(num * 1000)
      if (!isNaN(date.getTime())) {
        return {
          type: 'timestamp',
          date,
          timestampMs: num * 1000,
          timestampSec: num,
          isValid: true
        }
      }
    }
    
    // 13位数字 - 毫秒级时间戳
    if (input.length === 13) {
      const date = new Date(num)
      if (!isNaN(date.getTime())) {
        return {
          type: 'timestamp',
          date,
          timestampMs: num,
          timestampSec: Math.floor(num / 1000),
          isValid: true
        }
      }
    }
  }

  // 尝试解析为日期时间字符串
  const date = new Date(input)
  if (!isNaN(date.getTime())) {
    return {
      type: 'datetime',
      date,
      timestampMs: date.getTime(),
      timestampSec: Math.floor(date.getTime() / 1000),
      isValid: true
    }
  }

  return { type: 'unknown', isValid: false }
})

// 监听输入解析结果，自动更新日期选择器
watch(() => parsedInput.value, (parsed) => {
  if (parsed.isValid && parsed.date) {
    selectedDateValue.value = dateToDateValue(parsed.date)
    selectedTime.value = {
      hours: parsed.date.getHours(),
      minutes: parsed.date.getMinutes(),
      seconds: parsed.date.getSeconds(),
      milliseconds: parsed.date.getMilliseconds()
    }
  }
})

// ==================== 当前有效日期时间 ====================
const currentDateTime = computed(() => {
  const baseDate = dateValueToDate(selectedDateValue.value)
  const date = new Date(baseDate)
  date.setHours(selectedTime.value.hours)
  date.setMinutes(selectedTime.value.minutes)
  date.setSeconds(selectedTime.value.seconds)
  date.setMilliseconds(selectedTime.value.milliseconds)
  return date
})

// ==================== 格式化函数 ====================
function formatDateTime(date: Date, format: string): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  const milliseconds = String(date.getMilliseconds()).padStart(3, '0')

  const formats: Record<string, string> = {
    'YYYY-MM-DD HH:mm:ss.SSS': `${year}-${month}-${day} ${hours}:${minutes}:${seconds}.${milliseconds}`,
    'YYYY-MM-DD HH:mm:ss': `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`,
    'YYYY/MM/DD HH:mm:ss': `${year}/${month}/${day} ${hours}:${minutes}:${seconds}`,
    'DD/MM/YYYY HH:mm:ss': `${day}/${month}/${year} ${hours}:${minutes}:${seconds}`,
    'MM/DD/YYYY HH:mm:ss': `${month}/${day}/${year} ${hours}:${minutes}:${seconds}`,
    'YYYY-MM-DD': `${year}-${month}-${day}`,
    'YYYY/MM/DD': `${year}/${month}/${day}`,
    'DD/MM/YYYY': `${day}/${month}/${year}`,
    'MM/DD/YYYY': `${month}/${day}/${year}`,
    'HH:mm:ss.SSS': `${hours}:${minutes}:${seconds}.${milliseconds}`,
    'HH:mm:ss': `${hours}:${minutes}:${seconds}`,
    'HH:mm': `${hours}:${minutes}`,
  }

  return formats[format] ?? formats['YYYY-MM-DD HH:mm:ss']!
}

function formatTimezone(date: Date, timezone: string): string {
  try {
    return new Intl.DateTimeFormat('zh-CN', {
      timeZone: timezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    }).format(date).replace(/\//g, '-')
  } catch (error) {
    return '无效时区'
  }
}

// ==================== 时间戳显示 ====================
const timestamps = computed(() => ({
  milliseconds: currentDateTime.value.getTime(),
  seconds: Math.floor(currentDateTime.value.getTime() / 1000),
  microSeconds: currentDateTime.value.getTime() * 1000,
}))

// ==================== 常用格式显示 ====================
const commonFormats = computed(() => [
  { label: 'ISO 8601 (含毫秒)', value: currentDateTime.value.toISOString() },
  { label: 'UTC', value: currentDateTime.value.toUTCString() },
  { label: 'YYYY-MM-DD HH:mm:ss.SSS', value: formatDateTime(currentDateTime.value, 'YYYY-MM-DD HH:mm:ss.SSS') },
  { label: 'YYYY-MM-DD HH:mm:ss', value: formatDateTime(currentDateTime.value, 'YYYY-MM-DD HH:mm:ss') },
  { label: 'YYYY/MM/DD HH:mm:ss', value: formatDateTime(currentDateTime.value, 'YYYY/MM/DD HH:mm:ss') },
  { label: 'DD/MM/YYYY HH:mm:ss', value: formatDateTime(currentDateTime.value, 'DD/MM/YYYY HH:mm:ss') },
  { label: 'MM/DD/YYYY HH:mm:ss', value: formatDateTime(currentDateTime.value, 'MM/DD/YYYY HH:mm:ss') },
  { label: '仅日期 YYYY-MM-DD', value: formatDateTime(currentDateTime.value, 'YYYY-MM-DD') },
  { label: '仅时间 HH:mm:ss.SSS', value: formatDateTime(currentDateTime.value, 'HH:mm:ss.SSS') },
  { label: '仅时间 HH:mm:ss', value: formatDateTime(currentDateTime.value, 'HH:mm:ss') },
])

// ==================== 时区转换 ====================
const timezoneConversions = computed(() => {
  return timezones
    .filter(tz => tz.value !== inputTimezone.value)
    .slice(0, 6)
    .map(tz => ({
      ...tz,
      time: formatTimezone(currentDateTime.value, tz.value)
    }))
})

// ==================== 日期计算 ====================
const targetDateValue = ref(
  new CalendarDate(
    new Date().getFullYear(),
    new Date().getMonth() + 1,
    new Date().getDate() + 7
  )
)

const targetTime = ref({
  hours: 0,
  minutes: 0,
  seconds: 0
})

const targetDate = computed(() => {
  const baseDate = dateValueToDate(targetDateValue.value)
  const date = new Date(baseDate)
  date.setHours(targetTime.value.hours)
  date.setMinutes(targetTime.value.minutes)
  date.setSeconds(targetTime.value.seconds)
  date.setMilliseconds(0)
  return date
})

const dateCalculation = computed(() => {
  const diff = targetDate.value.getTime() - currentDateTime.value.getTime()
  const absDiff = Math.abs(diff)
  
  const days = Math.floor(absDiff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((absDiff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((absDiff % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((absDiff % (1000 * 60)) / 1000)
  
  const totalHours = Math.floor(absDiff / (1000 * 60 * 60))
  const totalMinutes = Math.floor(absDiff / (1000 * 60))
  const totalSeconds = Math.floor(absDiff / 1000)

  return {
    isPast: diff < 0,
    days,
    hours,
    minutes,
    seconds,
    totalHours,
    totalMinutes,
    totalSeconds,
    totalDays: Math.floor(absDiff / (1000 * 60 * 60 * 24))
  }
})

// ==================== 相对时间 ====================
const relativeTime = computed(() => {
  const now = new Date()
  const diff = currentDateTime.value.getTime() - now.getTime()
  const absDiff = Math.abs(diff)
  
  const minutes = Math.floor(absDiff / (1000 * 60))
  const hours = Math.floor(absDiff / (1000 * 60 * 60))
  const days = Math.floor(absDiff / (1000 * 60 * 60 * 24))
  
  if (absDiff < 60000) return diff < 0 ? '刚刚' : '即将'
  if (minutes < 60) return diff < 0 ? `${minutes}分钟前` : `${minutes}分钟后`
  if (hours < 24) return diff < 0 ? `${hours}小时前` : `${hours}小时后`
  if (days < 30) return diff < 0 ? `${days}天前` : `${days}天后`
  
  const months = Math.floor(days / 30)
  if (months < 12) return diff < 0 ? `${months}个月前` : `${months}个月后`
  
  const years = Math.floor(days / 365)
  return diff < 0 ? `${years}年前` : `${years}年后`
})

// ==================== 星期和节气 ====================
const weekInfo = computed(() => {
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const weekDaysEn = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
  const day = currentDateTime.value.getDay()
  
  return {
    cn: weekDays[day],
    en: weekDaysEn[day],
    isWeekend: day === 0 || day === 6
  }
})

// ==================== 实时更新功能 ====================
const updateToNow = () => {
  isUpdatingFromLive.value = true
  const now = new Date()
  selectedDateValue.value = dateToDateValue(now)
  selectedTime.value = {
    hours: now.getHours(),
    minutes: now.getMinutes(),
    seconds: now.getSeconds(),
    milliseconds: now.getMilliseconds()
  }
  setTimeout(() => {
    isUpdatingFromLive.value = false
  }, 50)
}

const startLiveUpdate = () => {
  if (liveUpdateTimer) return
  liveUpdateTimer = window.setInterval(() => {
    if (isLiveMode.value) {
      updateToNow()
    }
  }, 1000)
}

const stopLiveUpdate = () => {
  if (liveUpdateTimer) {
    clearInterval(liveUpdateTimer)
    liveUpdateTimer = null
  }
}

// 生命周期
onMounted(() => {
  updateToNow()
  startLiveUpdate()
})

onUnmounted(() => {
  stopLiveUpdate()
})

// ==================== 工具函数 ====================
const copyText = async (text: string, label: string = '内容') => {
  try {
    await navigator.clipboard.writeText(text)
    toast.success(`已复制${label}`)
  } catch {
    toast.error('复制失败')
  }
}

const setNow = () => {
  const now = new Date()
  isUpdatingFromLive.value = true
  selectedDateValue.value = dateToDateValue(now)
  selectedTime.value = {
    hours: now.getHours(),
    minutes: now.getMinutes(),
    seconds: now.getSeconds(),
    milliseconds: now.getMilliseconds()
  }
  inputText.value = ''
  setTimeout(() => {
    isUpdatingFromLive.value = false
  }, 50)
}

const toggleLiveMode = () => {
  isLiveMode.value = !isLiveMode.value
  if (isLiveMode.value) {
    updateToNow()
  }
}

const pasteFromClipboard = async () => {
  try {
    const text = await navigator.clipboard.readText()
    inputText.value = text
    toast.success('已粘贴内容')
  } catch {
    toast.error('粘贴失败')
  }
}

const addDays = (days: number) => {
  isLiveMode.value = false
  const newDate = new Date(currentDateTime.value)
  newDate.setDate(newDate.getDate() + days)
  selectedDateValue.value = dateToDateValue(newDate)
  selectedTime.value = {
    hours: newDate.getHours(),
    minutes: newDate.getMinutes(),
    seconds: newDate.getSeconds(),
    milliseconds: newDate.getMilliseconds()
  }
  toast.info(`已${days > 0 ? '增加' : '减少'} ${Math.abs(days)} 天`)
}

const addHours = (hours: number) => {
  isLiveMode.value = false
  const newDate = new Date(currentDateTime.value)
  newDate.setHours(newDate.getHours() + hours)
  selectedDateValue.value = dateToDateValue(newDate)
  selectedTime.value = {
    hours: newDate.getHours(),
    minutes: newDate.getMinutes(),
    seconds: newDate.getSeconds(),
    milliseconds: newDate.getMilliseconds()
  }
  toast.info(`已${hours > 0 ? '增加' : '减少'} ${Math.abs(hours)} 小时`)
}

// ==================== 日期选择器辅助 ====================
const formattedDate = computed(() => {
  return df.format(dateValueToDate(selectedDateValue.value))
})
</script>

<template>
  <div class="min-h-screen w-full bg-gradient-to-br from-background via-muted/20 to-background">
    <Toaster position="top-center" richColors />
    
    <div class="container mx-auto p-4 md:p-8 max-w-7xl">
      <!-- 标题区域 -->
      <div class="mb-8 text-center">
        <h1 class="text-4xl md:text-5xl font-bold mb-3 bg-gradient-to-r from-blue-600 to-cyan-600 bg-clip-text text-transparent">
          时间日期工具
        </h1>
        <p class="text-muted-foreground text-lg">
          智能识别、实时转换、多维度时间处理工具
        </p>
      </div>

      <div class="grid gap-6 lg:grid-cols-3">
        <!-- 左侧：输入区域 -->
        <div class="lg:col-span-1 space-y-4">
          <!-- 智能输入框 -->
          <Card class="p-6 shadow-lg">
            <div class="space-y-4">
              <div>
                <Label class="text-lg font-semibold mb-2 flex items-center gap-2">
                  <Clock class="w-5 h-5" />
                  智能输入
                </Label>
                <p class="text-xs text-muted-foreground mb-3">
                  支持时间戳或日期时间字符串，自动识别格式
                </p>
              </div>

              <div class="space-y-2">
                <div class="flex gap-2">
                  <Input
                    v-model="inputText"
                    placeholder="例如: 1733644800000 或 2024-12-08 15:30:00"
                    class="font-mono text-sm"
                  />
                  <Button size="icon" variant="outline" @click="pasteFromClipboard">
                    📋
                  </Button>
                </div>
                <div v-if="parsedInput.isValid" class="flex items-center gap-2 text-xs">
                  <Badge :variant="parsedInput.type === 'timestamp' ? 'default' : 'secondary'">
                    {{ parsedInput.type === 'timestamp' ? '时间戳' : '日期时间' }}
                  </Badge>
                  <span class="text-muted-foreground">✓ 识别成功</span>
                </div>
                <div v-else-if="inputText.trim()" class="text-xs text-red-500">
                  ✗ 无法识别格式
                </div>
              </div>

              <Separator />

              <!-- 日期选择器 -->
              <div class="space-y-2">
                <Label>选择日期</Label>
                <Popover>
                  <PopoverTrigger as-child>
                    <Button
                      variant="outline"
                      :class="cn('w-full justify-start text-left font-normal')"
                    >
                      <CalendarIcon class="mr-2 h-4 w-4" />
                      {{ formattedDate }}
                    </Button>
                  </PopoverTrigger>
                  <PopoverContent class="w-auto p-0" align="start">
                    <CalendarWithSelect
                      v-model="selectedDateValue as any"
                      initial-focus
                      layout="month-and-year"
                      locale="zh-CN"
                    />
                  </PopoverContent>
                </Popover>
              </div>

              <!-- 时间选择器 -->
              <div class="grid grid-cols-2 gap-3">
                <div class="space-y-2">
                  <Label class="text-sm">小时</Label>
                  <Select v-model="selectedTime.hours">
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent class="max-h-60">
                      <SelectGroup>
                        <SelectItem v-for="h in 24" :key="h - 1" :value="h - 1">
                          {{ String(h - 1).padStart(2, '0') }}
                        </SelectItem>
                      </SelectGroup>
                    </SelectContent>
                  </Select>
                </div>
                <div class="space-y-2">
                  <Label class="text-sm">分钟</Label>
                  <Select v-model="selectedTime.minutes">
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent class="max-h-60">
                      <SelectGroup>
                        <SelectItem v-for="m in 60" :key="m - 1" :value="m - 1">
                          {{ String(m - 1).padStart(2, '0') }}
                        </SelectItem>
                      </SelectGroup>
                    </SelectContent>
                  </Select>
                </div>
                <div class="space-y-2">
                  <Label class="text-sm">秒</Label>
                  <Select v-model="selectedTime.seconds">
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent class="max-h-60">
                      <SelectGroup>
                        <SelectItem v-for="s in 60" :key="s - 1" :value="s - 1">
                          {{ String(s - 1).padStart(2, '0') }}
                        </SelectItem>
                      </SelectGroup>
                    </SelectContent>
                  </Select>
                </div>
                <div class="space-y-2">
                  <Label class="text-sm">毫秒</Label>
                  <Input
                    type="number"
                    v-model.number="selectedTime.milliseconds"
                    min="0"
                    max="999"
                    placeholder="000"
                    class="text-sm"
                  />
                </div>
              </div>

              <!-- 快捷按钮 -->
              <div class="space-y-2">
                <div class="grid grid-cols-2 gap-2">
                  <Button 
                    size="sm" 
                    variant="secondary" 
                    @click="setNow" 
                    class="w-full"
                  >
                    <RefreshCw class="w-3 h-3 mr-1" />
                    当前时间
                  </Button>
                  <Button 
                    size="sm" 
                    :variant="isLiveMode ? 'default' : 'outline'" 
                    @click="toggleLiveMode" 
                    class="w-full"
                  >
                    <RefreshCw :class="['w-3 h-3 mr-1', isLiveMode && 'animate-spin']" />
                    {{ isLiveMode ? '实时' : '固定' }}
                  </Button>
                </div>
                <Select v-model="inputTimezone" class="w-full">
                  <SelectTrigger class="h-9">
                    <SelectValue placeholder="时区" />
                  </SelectTrigger>
                  <SelectContent class="max-h-60">
                    <SelectGroup>
                      <SelectItem v-for="tz in timezones" :key="tz.value" :value="tz.value">
                        {{ tz.label }}
                      </SelectItem>
                    </SelectGroup>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </Card>

          <!--日期快捷调整-->
          <Card class="p-4 shadow-lg">
            <Label class="text-sm font-semibold mb-3 block">快捷调整</Label>
            <div class="grid grid-cols-2 gap-2">
              <Button size="sm" variant="outline" @click="addDays(-7)">-7天</Button>
              <Button size="sm" variant="outline" @click="addDays(-1)">-1天</Button>
              <Button size="sm" variant="outline" @click="addDays(1)">+1天</Button>
              <Button size="sm" variant="outline" @click="addDays(7)">+7天</Button>
              <Button size="sm" variant="outline" @click="addHours(-1)">-1时</Button>
              <Button size="sm" variant="outline" @click="addHours(1)">+1时</Button>
            </div>
          </Card>
        </div>

        <!-- 右侧：显示区域 -->
        <div class="lg:col-span-2 space-y-4">
          <!-- 当前选择的时间显示 -->
          <Card class="p-6 shadow-lg bg-gradient-to-br from-blue-50 to-cyan-50 dark:from-blue-950/20 dark:to-cyan-950/20">
            <div class="text-center space-y-2">
              <div class="flex items-center justify-center gap-2 text-sm text-muted-foreground">
                <span>当前时间</span>
                <Badge :variant="isLiveMode ? 'default' : 'outline'" class="text-xs">
                  {{ isLiveMode ? '实时' : '固定' }}
                </Badge>
              </div>
              <div class="text-2xl md:text-3xl font-bold bg-gradient-to-r from-blue-600 to-cyan-600 bg-clip-text text-transparent">
                {{ formatDateTime(currentDateTime, 'YYYY-MM-DD HH:mm:ss.SSS') }}
              </div>
              <div class="flex items-center justify-center gap-3 flex-wrap">
                <Badge variant="outline">{{ weekInfo.cn }}</Badge>
                <Badge variant="outline">{{ weekInfo.en }}</Badge>
                <Badge :variant="weekInfo.isWeekend ? 'default' : 'secondary'">
                  {{ weekInfo.isWeekend ? '周末' : '工作日' }}
                </Badge>
                <Badge variant="outline">{{ relativeTime }}</Badge>
              </div>
            </div>
          </Card>

          <!-- 时间戳显示 -->
          <Card class="p-6 shadow-lg">
            <div class="flex items-center justify-between mb-4">
              <Label class="text-lg font-semibold">时间戳</Label>
              <Badge variant="secondary">多格式</Badge>
            </div>
            <div class="grid gap-3">
              <div class="flex items-center justify-between p-3 bg-muted/50 rounded-lg">
                <div>
                  <div class="text-xs text-muted-foreground">毫秒 (13位)</div>
                  <div class="font-mono text-sm font-medium">{{ timestamps.milliseconds }}</div>
                </div>
                <Button size="icon-sm" variant="ghost" @click="copyText(String(timestamps.milliseconds), '毫秒时间戳')">
                  <Copy class="w-4 h-4" />
                </Button>
              </div>
              <div class="flex items-center justify-between p-3 bg-muted/50 rounded-lg">
                <div>
                  <div class="text-xs text-muted-foreground">秒 (10位)</div>
                  <div class="font-mono text-sm font-medium">{{ timestamps.seconds }}</div>
                </div>
                <Button size="icon-sm" variant="ghost" @click="copyText(String(timestamps.seconds), '秒时间戳')">
                  <Copy class="w-4 h-4" />
                </Button>
              </div>
              <div class="flex items-center justify-between p-3 bg-muted/50 rounded-lg">
                <div>
                  <div class="text-xs text-muted-foreground">微秒 (16位)</div>
                  <div class="font-mono text-sm font-medium">{{ timestamps.microSeconds }}</div>
                </div>
                <Button size="icon-sm" variant="ghost" @click="copyText(String(timestamps.microSeconds), '微秒时间戳')">
                  <Copy class="w-4 h-4" />
                </Button>
              </div>
            </div>
          </Card>

          <!-- 常用格式 -->
          <Card class="p-6 shadow-lg">
            <div class="flex items-center justify-between mb-4">
              <Label class="text-lg font-semibold">常用格式</Label>
              <Badge variant="secondary">10种格式</Badge>
            </div>
            <div class="grid gap-2">
              <div
                v-for="format in commonFormats"
                :key="format.label"
                class="flex items-center justify-between p-3 bg-muted/50 rounded-lg hover:bg-muted transition-colors"
              >
                <div class="flex-1">
                  <div class="text-xs text-muted-foreground mb-1">{{ format.label }}</div>
                  <div class="font-mono text-sm">{{ format.value }}</div>
                </div>
                <Button size="icon-sm" variant="ghost" @click="copyText(format.value, format.label)">
                  <Copy class="w-4 h-4" />
                </Button>
              </div>
            </div>
          </Card>

          <!-- 时区转换 -->
          <Card class="p-6 shadow-lg">
            <div class="flex items-center justify-between mb-4">
              <Label class="text-lg font-semibold">时区转换</Label>
              <Badge variant="secondary">当前: {{ timezones.find(tz => tz.value === inputTimezone)?.label }}</Badge>
            </div>
            <div class="grid md:grid-cols-2 gap-3">
              <div
                v-for="tz in timezoneConversions"
                :key="tz.value"
                class="p-3 bg-muted/50 rounded-lg"
              >
                <div class="flex items-center justify-between mb-1">
                  <span class="text-sm font-medium">{{ tz.label }}</span>
                  <Button size="icon-sm" variant="ghost" @click="copyText(tz.time, tz.label)">
                    <Copy class="w-3 h-3" />
                  </Button>
                </div>
                <div class="font-mono text-xs text-muted-foreground">{{ tz.time }}</div>
              </div>
            </div>
          </Card>

          <!-- 日期间隔计算 -->
          <Card class="p-6 shadow-lg">
            <div class="mb-4">
              <Label class="text-lg font-semibold mb-3 block">日期间隔计算</Label>
              <div class="space-y-3">
                <div class="flex gap-2 items-center">
                  <Popover>
                    <PopoverTrigger as-child>
                      <Button
                        variant="outline"
                        :class="cn('w-full justify-start text-left font-normal', !targetDateValue && 'text-muted-foreground')"
                      >
                        <CalendarIcon class="mr-2 h-4 w-4" />
                        {{ targetDateValue ? dateFormatter.format(dateValueToDate(targetDateValue)) : "选择目标日期" }}
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent class="w-auto p-0">
                      <CalendarWithSelect v-model="targetDateValue as any" initial-focus layout="month-and-year" locale="zh-CN" />
                    </PopoverContent>
                  </Popover>
                </div>
                
                <!-- 目标时间选择器 -->
                <div class="grid grid-cols-3 gap-2">
                  <div class="space-y-1">
                    <Label class="text-xs text-muted-foreground">小时</Label>
                    <Select v-model="targetTime.hours">
                      <SelectTrigger class="h-9">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent class="max-h-60">
                        <SelectGroup>
                          <SelectItem v-for="h in 24" :key="h - 1" :value="h - 1">
                            {{ String(h - 1).padStart(2, '0') }}
                          </SelectItem>
                        </SelectGroup>
                      </SelectContent>
                    </Select>
                  </div>
                  <div class="space-y-1">
                    <Label class="text-xs text-muted-foreground">分钟</Label>
                    <Select v-model="targetTime.minutes">
                      <SelectTrigger class="h-9">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent class="max-h-60">
                        <SelectGroup>
                          <SelectItem v-for="m in 60" :key="m - 1" :value="m - 1">
                            {{ String(m - 1).padStart(2, '0') }}
                          </SelectItem>
                        </SelectGroup>
                      </SelectContent>
                    </Select>
                  </div>
                  <div class="space-y-1">
                    <Label class="text-xs text-muted-foreground">秒</Label>
                    <Select v-model="targetTime.seconds">
                      <SelectTrigger class="h-9">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent class="max-h-60">
                        <SelectGroup>
                          <SelectItem v-for="s in 60" :key="s - 1" :value="s - 1">
                            {{ String(s - 1).padStart(2, '0') }}
                          </SelectItem>
                        </SelectGroup>
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                
                <div class="text-xs text-center text-muted-foreground">
                  目标时间: {{ formatDateTime(targetDate, 'YYYY-MM-DD HH:mm:ss') }}
                </div>
              </div>
            </div>

            <div class="space-y-3">
              <div class="text-center p-4 bg-gradient-to-r from-blue-50 to-purple-50 dark:from-blue-950/20 dark:to-purple-950/20 rounded-lg">
                <div class="text-2xl font-bold">
                  {{ dateCalculation.isPast ? '已过去' : '还有' }}
                </div>
                <div class="text-3xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent mt-1">
                  {{ dateCalculation.days }} 天 {{ dateCalculation.hours }} 时 {{ dateCalculation.minutes }} 分
                </div>
              </div>

              <div class="grid grid-cols-2 md:grid-cols-4 gap-2">
                <div class="text-center p-3 bg-blue-50 dark:bg-blue-950/20 rounded-lg">
                  <div class="text-xl font-bold text-blue-600">{{ dateCalculation.totalDays }}</div>
                  <div class="text-xs text-muted-foreground">总天数</div>
                </div>
                <div class="text-center p-3 bg-green-50 dark:bg-green-950/20 rounded-lg">
                  <div class="text-xl font-bold text-green-600">{{ dateCalculation.totalHours }}</div>
                  <div class="text-xs text-muted-foreground">总小时</div>
                </div>
                <div class="text-center p-3 bg-yellow-50 dark:bg-yellow-950/20 rounded-lg">
                  <div class="text-xl font-bold text-yellow-600">{{ dateCalculation.totalMinutes }}</div>
                  <div class="text-xs text-muted-foreground">总分钟</div>
                </div>
                <div class="text-center p-3 bg-purple-50 dark:bg-purple-950/20 rounded-lg">
                  <div class="text-xl font-bold text-purple-600">{{ dateCalculation.totalSeconds }}</div>
                  <div class="text-xs text-muted-foreground">总秒数</div>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  </div>
</template>
