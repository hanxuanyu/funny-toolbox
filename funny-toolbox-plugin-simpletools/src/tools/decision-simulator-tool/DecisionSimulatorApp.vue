<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { 
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { Toaster } from '@/components/ui/sonner'
import { 
  Coins, 
  Dices, 
  CircleDot, 
  ListOrdered, 
  Sparkles, 
  X, 
  RotateCcw,
  Copy,
  Plus
} from 'lucide-vue-next'
import { toast } from 'vue-sonner'

type DecisionMode = 'coin' | 'dice' | 'wheel' | 'picker' | 'oracle'

const currentMode = ref<DecisionMode>('coin')
const isAnimating = ref(false)
const result = ref<string>('')

// Coin Flip
const coinSide1 = ref('正面')
const coinSide2 = ref('反面')
const coinResult = ref<'heads' | 'tails' | null>(null)
const coinFlipCount = ref(0)

// Dice Roll
const diceType = ref<number>(6)
const diceCount = ref<number>(1)
const diceResults = ref<number[]>([])
const diceRolling = ref(false)

// Spinner Wheel
const wheelOptions = ref<string[]>(['选项1', '选项2', '选项3', '选项4', '选项5', '选项6'])
const wheelNewOption = ref('')
const wheelRotation = ref(0)
const wheelSelectedIndex = ref<number | null>(null)
const wheelPasteInput = ref('')

// Random Picker
const pickerItems = ref<string>('苹果\n香蕉\n橙子\n葡萄')
const pickerResult = ref<string>('')
const pickerAnimIndex = ref(0)

// Oracle (Magic 8-Ball)
const oracleQuestion = ref('')
const oracleAnswer = ref('')
const oracleAnswers = [
  // 肯定回答 (40个)
  { text: '是的，毫无疑问', type: 'positive' },
  { text: '当然可以', type: 'positive' },
  { text: '前景很好', type: 'positive' },
  { text: '非常有可能', type: 'positive' },
  { text: '一切迹象都指向是', type: 'positive' },
  { text: '你可以相信它', type: 'positive' },
  { text: '绝对是的', type: 'positive' },
  { text: '确定无疑', type: 'positive' },
  { text: '命运站在你这边', type: 'positive' },
  { text: '星象显示吉兆', type: 'positive' },
  { text: '就是现在，去做吧', type: 'positive' },
  { text: '天时地利人和', type: 'positive' },
  { text: '这是最佳时机', type: 'positive' },
  { text: '你的直觉是对的', type: 'positive' },
  { text: '勇敢前进', type: 'positive' },
  { text: '好运即将降临', type: 'positive' },
  { text: '完全赞成', type: 'positive' },
  { text: '强烈建议', type: 'positive' },
  { text: '没有理由不这样做', type: 'positive' },
  { text: '宇宙在为你加油', type: 'positive' },
  { text: '这将是明智的选择', type: 'positive' },
  { text: '成功在望', type: 'positive' },
  { text: '结果会超出你的期待', type: 'positive' },
  { text: '顺其自然，水到渠成', type: 'positive' },
  { text: '所有条件都已具备', type: 'positive' },
  { text: '这是你应得的', type: 'positive' },
  { text: '尽管去做', type: 'positive' },
  { text: '时机已经成熟', type: 'positive' },
  { text: '跟随你的心', type: 'positive' },
  { text: '这是正确的道路', type: 'positive' },
  { text: '好事即将发生', type: 'positive' },
  { text: '你值得拥有', type: 'positive' },
  { text: '机会难得', type: 'positive' },
  { text: '福星高照', type: 'positive' },
  { text: '大吉大利', type: 'positive' },
  { text: '诸事皆宜', type: 'positive' },
  { text: '顺风顺水', type: 'positive' },
  { text: '心想事成', type: 'positive' },
  { text: '万事俱备', type: 'positive' },
  { text: '好运连连', type: 'positive' },

  // 中立回答 (40个)
  { text: '答案不太明确，再试一次', type: 'neutral' },
  { text: '稍后再问', type: 'neutral' },
  { text: '现在还不能告诉you', type: 'neutral' },
  { text: '现在无法预测', type: 'neutral' },
  { text: '仔细思考后再问', type: 'neutral' },
  { text: '答案藏在你心中', type: 'neutral' },
  { text: '时机未到', type: 'neutral' },
  { text: '静观其变', type: 'neutral' },
  { text: '等待更多信息', type: 'neutral' },
  { text: '雾里看花，未见真章', type: 'neutral' },
  { text: '需要更多时间考虑', type: 'neutral' },
  { text: '听从内心的声音', type: 'neutral' },
  { text: '暂时不明朗', type: 'neutral' },
  { text: '一切皆有可能', type: 'neutral' },
  { text: '答案在变化中', type: 'neutral' },
  { text: '依赖于你的选择', type: 'neutral' },
  { text: '未来尚未确定', type: 'neutral' },
  { text: '保持耐心', type: 'neutral' },
  { text: '先问问自己', type: 'neutral' },
  { text: '需要三思而后行', type: 'neutral' },
  { text: '时间会给你答案', type: 'neutral' },
  { text: '顺其自然即可', type: 'neutral' },
  { text: '答案因人而异', type: 'neutral' },
  { text: '视情况而定', type: 'neutral' },
  { text: '还需观察', type: 'neutral' },
  { text: '半信半疑', type: 'neutral' },
  { text: '说不准', type: 'neutral' },
  { text: '难以预料', type: 'neutral' },
  { text: '需要权衡利弊', type: 'neutral' },
  { text: '再等等看', type: 'neutral' },
  { text: '保持开放的心态', type: 'neutral' },
  { text: '命运尚未书写', type: 'neutral' },
  { text: '事在人为', type: 'neutral' },
  { text: '取决于你的努力', type: 'neutral' },
  { text: '前路未明', type: 'neutral' },
  { text: '暂时无法判断', type: 'neutral' },
  { text: '这需要智慧', type: 'neutral' },
  { text: '多方考量', type: 'neutral' },
  { text: '结果难料', type: 'neutral' },
  { text: '一半一半', type: 'neutral' },

  // 否定回答 (40个)
  { text: '别指望了', type: 'negative' },
  { text: '我的回答是不', type: 'negative' },
  { text: '我的消息来源说不', type: 'negative' },
  { text: '前景不太好', type: 'negative' },
  { text: '非常值得怀疑', type: 'negative' },
  { text: '不太可能', type: 'negative' },
  { text: '现在不是时候', type: 'negative' },
  { text: '建议重新考虑', type: 'negative' },
  { text: '可能会失望', type: 'negative' },
  { text: '星象不利', type: 'negative' },
  { text: '三思而行', type: 'negative' },
  { text: '暂时搁置', type: 'negative' },
  { text: '风险太大', type: 'negative' },
  { text: '谨慎为好', type: 'negative' },
  { text: '时机不对', type: 'negative' },
  { text: '不建议', type: 'negative' },
  { text: '可能会后悔', type: 'negative' },
  { text: '还是算了吧', type: 'negative' },
  { text: '困难重重', type: 'negative' },
  { text: '阻力很大', type: 'negative' },
  { text: '不看好', type: 'negative' },
  { text: '成功率很低', type: 'negative' },
  { text: '条件不成熟', type: 'negative' },
  { text: '不合时宜', type: 'negative' },
  { text: '缺少契机', type: 'negative' },
  { text: '前途未卜', type: 'negative' },
  { text: '可能碰壁', type: 'negative' },
  { text: '有更好的选择', type: 'negative' },
  { text: '再想想吧', type: 'negative' },
  { text: '暂缓执行', type: 'negative' },
  { text: '不是明智之举', type: 'negative' },
  { text: '需要重新规划', type: 'negative' },
  { text: '换个方向试试', type: 'negative' },
  { text: '时运不济', type: 'negative' },
  { text: '不太明智', type: 'negative' },
  { text: '还需准备', type: 'negative' },
  { text: '条件不足', type: 'negative' },
  { text: '不如暂停', type: 'negative' },
  { text: '凶多吉少', type: 'negative' },
  { text: '逆水行舟', type: 'negative' },
]

const modes = [
  { id: 'coin', name: '抛硬币', icon: Coins, color: 'bg-amber-500', description: '经典二选一决策' },
  { id: 'dice', name: '掷骰子', icon: Dices, color: 'bg-red-500', description: '随机数字生成' },
  { id: 'wheel', name: '转盘', icon: CircleDot, color: 'bg-blue-500', description: '多选项转盘抽选' },
  { id: 'picker', name: '随机选择', icon: ListOrdered, color: 'bg-green-500', description: '列表随机抽取' },
  { id: 'oracle', name: '神谕', icon: Sparkles, color: 'bg-purple-500', description: '是非问答占卜' },
]

// Clear result when mode changes
const changeMode = (mode: DecisionMode) => {
  currentMode.value = mode
  result.value = ''
  // Reset mode-specific states
  if (mode === 'coin') {
    coinResult.value = null
  } else if (mode === 'dice') {
    diceResults.value = []
  } else if (mode === 'wheel') {
    wheelSelectedIndex.value = null
  } else if (mode === 'picker') {
    pickerResult.value = ''
  } else if (mode === 'oracle') {
    oracleAnswer.value = ''
  }
}

// Coin Flip Logic
const flipCoin = async () => {
  if (isAnimating.value) return
  
  if (!coinSide1.value.trim() || !coinSide2.value.trim()) {
    toast.error('请输入正反面内容')
    return
  }
  
  isAnimating.value = true
  coinFlipCount.value++
  
  const flipResult = Math.random() < 0.5 ? 'heads' : 'tails'
  
  // Animate coin flip with more rotations (5-8 full spins)
  const coinElement = document.getElementById('coin')
  if (coinElement) {
    // Get current rotation (normalized to prevent overflow)
    const currentTransform = coinElement.style.transform
    const currentRotation = currentTransform ? parseFloat(currentTransform.match(/rotateY\(([\d.-]+)deg\)/)?.[1] || '0') : 0
    const normalizedRotation = currentRotation % 360
    
    // More dramatic spins: 5-8 full rotations
    const baseSpins = 5 + Math.floor(Math.random() * 4)
    const targetFace = flipResult === 'heads' ? 0 : 180
    
    // Calculate rotation needed to reach target face
    let rotationNeeded = targetFace - normalizedRotation
    if (rotationNeeded < 0) {
      rotationNeeded += 360
    }
    
    // Total rotation = normalized current + base spins + final adjustment
    const totalRotation = normalizedRotation + baseSpins * 360 + rotationNeeded
    coinElement.style.transform = `rotateY(${totalRotation}deg)`
  }
  
  setTimeout(() => {
    coinResult.value = flipResult
    result.value = flipResult === 'heads' ? coinSide1.value : coinSide2.value
    isAnimating.value = false
    toast.success(`结果：${result.value}`, { duration: 2000 })
  }, 1000)
}

// Reset coin
const resetCoin = () => {
  coinSide1.value = '正面'
  coinSide2.value = '反面'
  coinResult.value = null
  result.value = ''
  const coinElement = document.getElementById('coin')
  if (coinElement) {
    coinElement.style.transform = 'rotateY(0deg)'
  }
  toast.info('已重置')
}

// Copy result
const copyResult = async () => {
  if (!result.value) {
    toast.error('没有可复制的结果')
    return
  }
  try {
    await navigator.clipboard.writeText(result.value)
    toast.success('已复制到剪贴板')
  } catch (error) {
    toast.error('复制失败')
  }
}

// Dice Roll Logic
const rollDice = async () => {
  if (isAnimating.value) return
  isAnimating.value = true
  diceRolling.value = true
  diceResults.value = []
  
  let interval = setInterval(() => {
    diceResults.value = Array.from({ length: diceCount.value }, () => 
      Math.floor(Math.random() * diceType.value) + 1
    )
  }, 50)
  
  setTimeout(() => {
    clearInterval(interval)
    diceResults.value = Array.from({ length: diceCount.value }, () => 
      Math.floor(Math.random() * diceType.value) + 1
    )
    const total = diceResults.value.reduce((a, b) => a + b, 0)
    result.value = `点数：${diceResults.value.join(', ')} | 总和：${total}`
    diceRolling.value = false
    isAnimating.value = false
    toast.success(result.value, { duration: 2000 })
  }, 800)
}

// Reset dice
const resetDice = () => {
  diceType.value = 6
  diceCount.value = 1
  diceResults.value = []
  result.value = ''
  toast.info('已重置')
}

// Spinner Wheel Logic
const spinWheel = async () => {
  if (isAnimating.value || wheelOptions.value.length === 0) return
  isAnimating.value = true
  
  // Randomly select which option will be chosen (this is the final result)
  const selectedIndex = Math.floor(Math.random() * wheelOptions.value.length)
  const selectedOption = wheelOptions.value[selectedIndex]
  
  // Calculate degrees per segment
  const degreesPerSegment = 360 / wheelOptions.value.length
  
  // Calculate the angle at the CENTER of the selected segment
  // SVG segments start from -90° (right side) in the path generation
  // But we want segments to align with pointer at top (0°)
  // So: segment 0 center is at degreesPerSegment/2 from top
  const segmentCenterAngle = selectedIndex * degreesPerSegment + degreesPerSegment / 2
  
  // More dramatic spins (10-15 full rotations)
  const baseSpins = 10 + Math.floor(Math.random() * 6)
  
  // Get current rotation value (this may be a large number from previous spins)
  const currentRotation = wheelRotation.value
  
  // Calculate what the current visual angle is (0-360)
  const currentNormalizedRotation = currentRotation % 360
  
  // Calculate final position: rotate to align segment center with pointer at top (0°)
  // Final angle should be: 360 - segmentCenterAngle (to bring segment center to top)
  const finalAngle = 360 - segmentCenterAngle
  
  // Calculate rotation needed to reach finalAngle from current normalized position
  let rotationNeeded = finalAngle - currentNormalizedRotation
  
  // Ensure we always rotate forward (positive direction)
  if (rotationNeeded < 0) {
    rotationNeeded += 360
  }
  
  // Total rotation = CURRENT (not normalized) + base spins + rotation needed
  // This ensures smooth continuation from wherever the wheel currently is
  const targetRotation = currentRotation + baseSpins * 360 + rotationNeeded
  
  // Apply rotation
  wheelRotation.value = targetRotation
  
  // Show result after animation completes (5 seconds)
  setTimeout(() => {
    wheelSelectedIndex.value = selectedIndex
    result.value = selectedOption || ''
    toast.success(`🎯 选中：${selectedOption}`, { duration: 3000 })
    isAnimating.value = false
  }, 5000)
}

const addWheelOption = () => {
  if (!wheelNewOption.value.trim()) {
    toast.error('请输入选项内容')
    return
  }
  if (wheelOptions.value.length >= 20) {
    toast.error('最多支持20个选项')
    return
  }
  wheelOptions.value.push(wheelNewOption.value.trim())
  wheelNewOption.value = ''
  toast.success('选项已添加')
}

const removeWheelOption = (index: number) => {
  if (wheelOptions.value.length <= 2) {
    toast.error('至少需要保留2个选项')
    return
  }
  wheelOptions.value.splice(index, 1)
  toast.success('选项已删除')
}

// Parse pasted content and add as options
const parseWheelPaste = () => {
  if (!wheelPasteInput.value.trim()) {
    toast.error('请输入要粘贴的内容')
    return
  }
  
  // Try to detect delimiter: newline, comma, semicolon, tab, pipe
  const content = wheelPasteInput.value.trim()
  let items: string[] = []
  
  // Check for newlines first
  if (content.includes('\n')) {
    items = content.split('\n')
  } 
  // Then check for common delimiters
  else if (content.includes(',')) {
    items = content.split(',')
  } else if (content.includes(';')) {
    items = content.split(';')
  } else if (content.includes('|')) {
    items = content.split('|')
  } else if (content.includes('\t')) {
    items = content.split('\t')
  } else if (content.includes(' ')) {
    items = content.split(' ')
  } else {
    items = [content]
  }
  
  // Clean and filter items
  items = items
    .map(item => item.trim())
    .filter(item => item.length > 0)
    .slice(0, 20) // Max 20 items
  
  if (items.length === 0) {
    toast.error('未能识别有效选项')
    return
  }
  
  if (items.length > 20) {
    toast.warning(`选项过多，仅保留前20个`)
  }
  
  wheelOptions.value = items
  wheelPasteInput.value = ''
  toast.success(`已添加 ${items.length} 个选项`)
}

// Reset wheel
const resetWheel = () => {
  wheelOptions.value = ['选项1', '选项2', '选项3', '选项4', '选项5', '选项6']
  wheelNewOption.value = ''
  wheelPasteInput.value = ''
  wheelRotation.value = 0
  wheelSelectedIndex.value = null
  result.value = ''
  toast.info('已重置')
}

// Random Picker Logic
const pickRandom = async () => {
  if (isAnimating.value) return
  
  const items = pickerItems.value.split('\n').filter(item => item.trim())
  if (items.length === 0) {
    toast.error('请输入至少一个选项')
    return
  }
  
  isAnimating.value = true
  let counter = 0
  const maxCount = 15
  
  const interval = setInterval(() => {
    pickerAnimIndex.value = Math.floor(Math.random() * items.length)
    counter++
    
    if (counter >= maxCount) {
      clearInterval(interval)
      const finalIndex = Math.floor(Math.random() * items.length)
      const selectedItem = items[finalIndex]
      if (selectedItem) {
        pickerResult.value = selectedItem
        result.value = pickerResult.value
        toast.success(`选中：${result.value}`, { duration: 2000 })
      }
      isAnimating.value = false
    }
  }, 100)
}

// Reset picker
const resetPicker = () => {
  pickerItems.value = '苹果\n香蕉\n橙子\n葡萄'
  pickerResult.value = ''
  result.value = ''
  toast.info('已重置')
}

// Get picker item count
const pickerItemCount = computed(() => {
  return pickerItems.value.split('\n').filter(item => item.trim()).length
})

// Oracle Question Validation & Caching
const ORACLE_CACHE_KEY = 'oracle-questions-cache'

// Get today's date string (YYYY-MM-DD) for cache validation
const getTodayDateString = (): string => {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}

// Validate question format (must be a yes/no question)
const validateOracleQuestion = (question: string): { valid: boolean; message?: string } => {
  const trimmed = question.trim()
  
  // Check minimum length
  if (trimmed.length < 3) {
    return { valid: false, message: '问题太短，请输入完整的问题' }
  }
  
  // Check maximum length
  if (trimmed.length > 80) {
    return { valid: false, message: '问题太长，请简化你的问题' }
  }
  
  // Must end with question mark
  if (!trimmed.endsWith('?') && !trimmed.endsWith('？') && !trimmed.endsWith('吗') && !trimmed.endsWith('呢')) {
    return { valid: false, message: '请输入一个疑问句（以？、吗、呢等结尾）' }
  }
  
  // Should contain yes/no question keywords
  const yesNoKeywords = ['吗', '呢', '是否', '能否', '可否', '会不会', '要不要', '该不该', '好不好', '行不行', '应该', '会', '能', '可以', '可不可以']
  const hasKeyword = yesNoKeywords.some(keyword => trimmed.includes(keyword))
  
  if (!hasKeyword && !trimmed.endsWith('?') && !trimmed.endsWith('？')) {
    return { valid: false, message: '请输入是非问题（例如：我应该...吗？）' }
  }
  
  return { valid: true }
}

// Normalize question for caching (remove punctuation, lowercase, trim)
const normalizeQuestion = (question: string): string => {
  return question
    .trim()
    .toLowerCase()
    .replace(/[？?!！。,.，、\s]+/g, '')
}

// Get cached answer type for a question
const getCachedAnswerType = (question: string): 'positive' | 'neutral' | 'negative' | null => {
  try {
    const cacheStr = localStorage.getItem(ORACLE_CACHE_KEY)
    if (!cacheStr) return null
    
    const cache = JSON.parse(cacheStr)
    const normalized = normalizeQuestion(question)
    const cached = cache[normalized]
    
    if (!cached) return null
    
    // Check if cache is from a different day (compare date strings)
    const today = getTodayDateString()
    if (cached.date !== today) {
      // Remove expired entry from a previous day
      delete cache[normalized]
      localStorage.setItem(ORACLE_CACHE_KEY, JSON.stringify(cache))
      return null
    }
    
    return cached.type
  } catch (error) {
    console.error('Error reading oracle cache:', error)
    return null
  }
}

// Save answer type to cache
const setCachedAnswerType = (question: string, type: 'positive' | 'neutral' | 'negative') => {
  try {
    const cacheStr = localStorage.getItem(ORACLE_CACHE_KEY)
    const cache = cacheStr ? JSON.parse(cacheStr) : {}
    const normalized = normalizeQuestion(question)
    
    cache[normalized] = {
      type,
      date: getTodayDateString(), // Store date string instead of timestamp
      original: question
    }
    
    localStorage.setItem(ORACLE_CACHE_KEY, JSON.stringify(cache))
  } catch (error) {
    console.error('Error saving oracle cache:', error)
  }
}

// Oracle Logic with Validation and Caching
const askOracle = async () => {
  if (isAnimating.value) return
  
  if (!oracleQuestion.value.trim()) {
    toast.error('请输入你的问题')
    return
  }
  
  // Validate question format
  const validation = validateOracleQuestion(oracleQuestion.value)
  if (!validation.valid) {
    toast.error(validation.message || '问题格式不正确')
    return
  }
  
  isAnimating.value = true
  oracleAnswer.value = ''
  
  // Shake animation
  const oracleElement = document.getElementById('oracle-ball')
  if (oracleElement) {
    oracleElement.style.animation = 'shake 0.5s ease-in-out'
  }
  
  setTimeout(() => {
    // Check if this question has been asked today
    let answerType = getCachedAnswerType(oracleQuestion.value)
    
    // If no cached type, randomly select one and cache it
    if (!answerType) {
      const types: Array<'positive' | 'neutral' | 'negative'> = ['positive', 'neutral', 'negative']
      answerType = types[Math.floor(Math.random() * types.length)]!
      setCachedAnswerType(oracleQuestion.value, answerType)
    }
    
    // Filter answers by type and select randomly from that category
    const filteredAnswers = oracleAnswers.filter(a => a.type === answerType)
    const randomAnswer = filteredAnswers[Math.floor(Math.random() * filteredAnswers.length)]
    
    if (randomAnswer) {
      oracleAnswer.value = randomAnswer.text
      result.value = randomAnswer.text
      toast.success('神谕已降临', { duration: 2000 })
    }
    
    isAnimating.value = false
    
    if (oracleElement) {
      oracleElement.style.animation = ''
    }
  }, 1500)
}

// Reset oracle
const resetOracle = () => {
  oracleQuestion.value = ''
  oracleAnswer.value = ''
  result.value = ''
  toast.info('已重置')
}

// Wheel gradient colors for SVG
const wheelGradientColors = [
  { start: '#ef4444', end: '#dc2626' }, // red
  { start: '#3b82f6', end: '#2563eb' }, // blue
  { start: '#10b981', end: '#059669' }, // green
  { start: '#f59e0b', end: '#d97706' }, // amber
  { start: '#8b5cf6', end: '#7c3aed' }, // purple
  { start: '#ec4899', end: '#db2777' }, // pink
  { start: '#f97316', end: '#ea580c' }, // orange
  { start: '#14b8a6', end: '#0d9488' }, // teal
  { start: '#6366f1', end: '#4f46e5' }, // indigo
  { start: '#06b6d4', end: '#0891b2' }, // cyan
]

// Generate SVG path for wheel segment
// Segments are drawn starting from top (0°) and going clockwise
const getWheelSegmentPath = (index: number, total: number) => {
  const anglePerSegment = 360 / total
  
  // Start from top (0°) and go clockwise
  // Convert to radians: 0° is at top, so we use -90° offset for SVG coordinate system
  const startAngleDeg = index * anglePerSegment
  const endAngleDeg = (index + 1) * anglePerSegment
  
  // Convert to radians with -90° offset (SVG coords: 0° is at 3 o'clock)
  const startAngle = (startAngleDeg - 90) * (Math.PI / 180)
  const endAngle = (endAngleDeg - 90) * (Math.PI / 180)
  
  const x1 = 50 + 50 * Math.cos(startAngle)
  const y1 = 50 + 50 * Math.sin(startAngle)
  const x2 = 50 + 50 * Math.cos(endAngle)
  const y2 = 50 + 50 * Math.sin(endAngle)
  
  const largeArc = anglePerSegment > 180 ? 1 : 0
  
  return `M 50 50 L ${x1} ${y1} A 50 50 0 ${largeArc} 1 ${x2} ${y2} Z`
}

// Get text position for wheel segment (at the center of each segment)
const getTextPosition = (index: number, total: number) => {
  const anglePerSegment = 360 / total
  
  // Position text at the center of the segment
  // Segments start from top (0°), so center of segment 0 is at anglePerSegment/2
  const centerAngleDeg = index * anglePerSegment + anglePerSegment / 2
  
  // Convert to radians with -90° offset for SVG coordinate system
  const angle = (centerAngleDeg - 90) * (Math.PI / 180)
  const radius = 32 // Distance from center
  
  return {
    x: 50 + radius * Math.cos(angle),
    y: 50 + radius * Math.sin(angle)
  }
}
</script>

<template>
  <div class="min-h-screen w-full bg-gradient-to-br from-background via-muted/20 to-background">
    <Toaster position="top-center" richColors />
    
    <div class="container mx-auto p-3 md:p-6 max-w-6xl">
      <!-- Header -->
      <div class="text-center mb-4">
        <h1 class="text-2xl md:text-3xl font-bold bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
          决策模拟器
        </h1>
      </div>

      <!-- Mode Selection - Compact Dropdown -->
      <div class="mb-4">
        <div class="flex items-center gap-2">
          <Label class="text-sm font-medium whitespace-nowrap">决策方式：</Label>
          <Select v-model="currentMode">
            <SelectTrigger class="w-full max-w-xs">
              <SelectValue>
                <div class="flex items-center gap-2">
                  <component 
                    :is="modes.find(m => m.id === currentMode)?.icon" 
                    :size="18" 
                    :class="modes.find(m => m.id === currentMode)?.color.replace('bg-', 'text-')"
                  />
                  <span>{{ modes.find(m => m.id === currentMode)?.name }}</span>
                  <span class="text-xs text-muted-foreground">
                    · {{ modes.find(m => m.id === currentMode)?.description }}
                  </span>
                </div>
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem 
                v-for="mode in modes" 
                :key="mode.id" 
                :value="mode.id"
                @click="changeMode(mode.id as DecisionMode)"
              >
                <div class="flex items-center gap-2 py-1">
                  <component :is="mode.icon" :size="18" :class="mode.color.replace('bg-', 'text-')" />
                  <div class="flex flex-col">
                    <span class="font-medium">{{ mode.name }}</span>
                    <span class="text-xs text-muted-foreground">{{ mode.description }}</span>
                  </div>
                </div>
              </SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <!-- Coin Flip Mode -->
      <div v-if="currentMode === 'coin'" class="space-y-4">
        <Card class="p-4 md:p-6 shadow-lg">
          <!-- Compact Controls -->
          <div class="flex items-center justify-between gap-2 mb-4">
            <div class="flex-1 grid grid-cols-2 gap-2">
              <Input v-model="coinSide1" placeholder="正面" maxlength="8" class="h-9 text-sm" />
              <Input v-model="coinSide2" placeholder="反面" maxlength="8" class="h-9 text-sm" />
            </div>
            <div class="flex gap-1">
              <Button @click="resetCoin" variant="ghost" size="icon" class="h-9 w-9">
                <RotateCcw :size="16" />
              </Button>
              <Button @click="copyResult" variant="ghost" size="icon" class="h-9 w-9" :disabled="!result">
                <Copy :size="16" />
              </Button>
            </div>
          </div>

          <!-- Main Animation Area - Focus -->
          <div class="flex flex-col items-center justify-center py-4 md:py-6">
            <div 
              id="coin"
              class="relative w-36 h-36 md:w-44 md:h-44 rounded-full shadow-2xl transition-transform duration-1000 preserve-3d cursor-pointer hover:scale-105"
              style="transform-style: preserve-3d"
              @click="flipCoin"
            >
              <div 
                class="absolute inset-0 rounded-full bg-gradient-to-br from-amber-300 via-amber-400 to-amber-500 flex items-center justify-center text-white text-xl md:text-2xl font-bold shadow-lg backface-hidden p-4"
                style="backface-visibility: hidden"
              >
                <span class="text-center break-words drop-shadow-md">{{ coinSide1 }}</span>
              </div>
              <div 
                class="absolute inset-0 rounded-full bg-gradient-to-br from-amber-500 via-amber-600 to-amber-700 flex items-center justify-center text-white text-xl md:text-2xl font-bold shadow-lg p-4"
                style="backface-visibility: hidden; transform: rotateY(180deg)"
              >
                <span class="text-center break-words drop-shadow-md">{{ coinSide2 }}</span>
              </div>
              <!-- Coin edge effect -->
              <div class="absolute inset-0 rounded-full ring-4 ring-amber-600/20 pointer-events-none"></div>
            </div>
            
            <Button 
              @click="flipCoin" 
              :disabled="isAnimating || !coinSide1.trim() || !coinSide2.trim()"
              class="mt-4"
            >
              {{ isAnimating ? '抛掷中...' : '🎲 抛硬币' }}
            </Button>
          </div>
        </Card>
      </div>

      <!-- Dice Roll Mode -->
      <div v-if="currentMode === 'dice'" class="space-y-4">
        <Card class="p-4 md:p-6 shadow-lg">
          <!-- Compact Controls -->
          <div class="flex items-center justify-between gap-2 mb-4">
            <div class="flex-1 grid grid-cols-2 gap-2">
              <Select v-model="diceType">
                <SelectTrigger class="h-9 text-sm">
                  <SelectValue placeholder="类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem :value="4">D4</SelectItem>
                  <SelectItem :value="6">D6</SelectItem>
                  <SelectItem :value="8">D8</SelectItem>
                  <SelectItem :value="10">D10</SelectItem>
                  <SelectItem :value="12">D12</SelectItem>
                  <SelectItem :value="20">D20</SelectItem>
                  <SelectItem :value="100">D100</SelectItem>
                </SelectContent>
              </Select>
              <Select v-model="diceCount">
                <SelectTrigger class="h-9 text-sm">
                  <SelectValue placeholder="数量" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="n in 6" :key="n" :value="n">{{ n }}个</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="flex gap-1">
              <Button @click="resetDice" variant="ghost" size="icon" class="h-9 w-9">
                <RotateCcw :size="16" />
              </Button>
              <Button @click="copyResult" variant="ghost" size="icon" class="h-9 w-9" :disabled="!result">
                <Copy :size="16" />
              </Button>
            </div>
          </div>

          <!-- Main Animation Area - Focus -->
          <div class="flex flex-col items-center justify-center py-4 md:py-6">
            <div class="flex flex-wrap gap-2 md:gap-3 justify-center mb-4 min-h-[100px] items-center">
              <div
                v-for="(die, index) in diceResults"
                :key="index"
                :class="[
                  'w-16 h-16 md:w-20 md:h-20 rounded-xl shadow-2xl flex items-center justify-center text-white text-xl md:text-2xl font-bold transition-all duration-300 relative overflow-hidden',
                  diceRolling ? 'animate-bounce' : 'hover:scale-110',
                ]"
              >
                <div class="absolute inset-0 bg-gradient-to-br from-red-400 via-red-500 to-red-600"></div>
                <div class="absolute inset-0 bg-[radial-gradient(circle_at_30%_30%,rgba(255,255,255,0.3),transparent_50%)]"></div>
                <span class="relative z-10 drop-shadow-lg">{{ die }}</span>
              </div>
              <div v-if="diceResults.length === 0" class="text-center py-4">
                <Dices :size="40" class="mx-auto mb-2 opacity-20 text-muted-foreground" />
                <p class="text-sm text-muted-foreground">准备掷骰</p>
              </div>
            </div>
            
            <Button 
              @click="rollDice" 
              :disabled="isAnimating"
            >
              {{ isAnimating ? '掷骰中...' : '🎲 掷骰子' }}
            </Button>
          </div>
        </Card>
      </div>

      <!-- Spinner Wheel Mode -->
      <div v-if="currentMode === 'wheel'" class="space-y-4">
        <Card class="p-4 md:p-6 shadow-lg">
          <!-- Compact Controls -->
          <div class="space-y-3 mb-4">
            <div class="flex items-center gap-2">
              <Input 
                v-model="wheelNewOption" 
                placeholder="添加单个选项"
                maxlength="15"
                class="h-9 text-sm flex-1"
                @keyup.enter="addWheelOption"
              />
              <Button @click="addWheelOption" size="icon" class="h-9 w-9">
                <Plus :size="18" />
              </Button>
              <Button @click="resetWheel" variant="ghost" size="icon" class="h-9 w-9">
                <RotateCcw :size="16" />
              </Button>
              <Button @click="copyResult" variant="ghost" size="icon" class="h-9 w-9" :disabled="!result">
                <Copy :size="16" />
              </Button>
            </div>
            
            <!-- Paste Multiple Options -->
            <details class="group">
              <summary class="text-xs text-muted-foreground cursor-pointer hover:text-foreground flex items-center gap-1">
                <span>批量导入选项（支持换行、逗号等分隔）</span>
                <span class="text-xs">{{ wheelOptions.length }}/20</span>
              </summary>
              <div class="mt-2 flex gap-2">
                <Textarea 
                  v-model="wheelPasteInput" 
                  placeholder="粘贴多个选项，支持换行、逗号、分号等分隔符&#10;例如：选项1,选项2,选项3 或每行一个"
                  rows="3"
                  class="text-xs font-mono resize-none"
                />
                <Button @click="parseWheelPaste" size="sm" class="self-end">
                  导入
                </Button>
              </div>
            </details>
            
            <!-- Option Tags -->
            <div class="flex flex-wrap gap-1.5 min-h-[32px] p-2 bg-muted/20 rounded-md max-h-24 overflow-y-auto">
              <Badge 
                v-for="(option, index) in wheelOptions" 
                :key="index"
                variant="secondary"
                class="px-2 py-1 text-xs cursor-pointer hover:bg-destructive/20 hover:text-destructive group/badge transition-colors"
                @click="removeWheelOption(index)"
              >
                {{ option }}
                <X :size="12" class="ml-1 opacity-0 group-hover/badge:opacity-100 transition-opacity" />
              </Badge>
            </div>
          </div>

          <!-- Main Animation Area - Focus -->
          <div class="flex flex-col items-center justify-center py-4 md:py-6">
            <div class="relative w-64 h-64 md:w-80 md:h-80 mb-4">
              <!-- Wheel with smooth gradient transitions -->
              <div 
                class="absolute inset-0 rounded-full shadow-2xl overflow-hidden transition-transform duration-[5000ms] ease-out"
                :style="{ transform: `rotate(${wheelRotation}deg)` }"
              >
                <svg viewBox="0 0 100 100" class="w-full h-full">
                  <g v-for="(option, index) in wheelOptions" :key="index">
                    <path
                      :d="getWheelSegmentPath(index, wheelOptions.length)"
                      :fill="`url(#gradient-${index})`"
                      class="transition-all duration-200"
                    />
                    <defs>
                      <linearGradient :id="`gradient-${index}`" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" :stop-color="wheelGradientColors[index % wheelGradientColors.length]?.start || '#ef4444'" />
                        <stop offset="100%" :stop-color="wheelGradientColors[index % wheelGradientColors.length]?.end || '#dc2626'" />
                      </linearGradient>
                    </defs>
                    <text
                      :x="getTextPosition(index, wheelOptions.length).x"
                      :y="getTextPosition(index, wheelOptions.length).y"
                      :transform="`rotate(${(360 / wheelOptions.length) * index + (360 / wheelOptions.length) / 2}, ${getTextPosition(index, wheelOptions.length).x}, ${getTextPosition(index, wheelOptions.length).y})`"
                      text-anchor="middle"
                      class="fill-white font-bold text-[0.25rem] md:text-[0.3rem] drop-shadow-lg pointer-events-none"
                      style="paint-order: stroke; stroke: rgba(0,0,0,0.3); stroke-width: 0.3px;"
                    >
                      {{ option.length > 8 ? option.slice(0, 8) + '...' : option }}
                    </text>
                  </g>
                </svg>
              </div>
              
              <!-- Pointer -->
              <div class="absolute -top-4 left-1/2 -translate-x-1/2 w-0 h-0 border-l-[20px] border-r-[20px] border-t-[30px] border-l-transparent border-r-transparent border-t-yellow-400 shadow-lg z-10 drop-shadow-2xl"></div>
              
              <!-- Center Circle with glow -->
              <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-16 h-16 rounded-full bg-gradient-to-br from-white to-gray-100 dark:from-slate-700 dark:to-slate-900 shadow-2xl border-4 border-yellow-400 z-10 flex items-center justify-center">
                <div class="absolute inset-0 rounded-full bg-yellow-400/20 blur-md"></div>
                <CircleDot :size="24" class="text-yellow-500 relative z-10" />
              </div>
            </div>
            
            <Button 
              @click="spinWheel" 
              :disabled="isAnimating || wheelOptions.length === 0"
            >
              {{ isAnimating ? '转动中...' : '🎯 开始转动' }}
            </Button>
          </div>
        </Card>
      </div>

      <!-- Random Picker Mode -->
      <div v-if="currentMode === 'picker'" class="space-y-4">
        <Card class="p-4 md:p-6 shadow-lg">
          <!-- Compact Controls -->
          <div class="flex items-start gap-2 mb-4">
            <div class="flex-1 space-y-1">
              <Textarea 
                v-model="pickerItems" 
                placeholder="输入选项（每行一个）&#10;苹果&#10;香蕉&#10;橙子&#10;葡萄"
                rows="4"
                class="font-mono text-xs resize-none"
              />
              <p class="text-xs text-muted-foreground">{{ pickerItemCount }} 个选项</p>
            </div>
            <div class="flex flex-col gap-1">
              <Button @click="resetPicker" variant="ghost" size="icon" class="h-9 w-9">
                <RotateCcw :size="16" />
              </Button>
              <Button @click="copyResult" variant="ghost" size="icon" class="h-9 w-9" :disabled="!result">
                <Copy :size="16" />
              </Button>
            </div>
          </div>

          <!-- Main Animation Area - Focus -->
          <div class="flex flex-col items-center justify-center py-4 md:py-6">
            <div class="min-h-[100px] flex items-center justify-center w-full px-4">
              <div 
                v-if="isAnimating"
                class="text-2xl md:text-4xl font-bold text-primary animate-pulse text-center break-words max-w-full"
              >
                {{ pickerItems.split('\n').filter(item => item.trim())[pickerAnimIndex] || '' }}
              </div>
              <div 
                v-else-if="pickerResult"
                class="text-2xl md:text-4xl font-bold bg-gradient-to-r from-green-600 via-blue-600 to-purple-600 bg-clip-text text-transparent text-center break-words max-w-full animate-in zoom-in duration-500"
              >
                {{ pickerResult }}
              </div>
              <div v-else class="text-center text-muted-foreground">
                <ListOrdered :size="40" class="mx-auto mb-2 opacity-20" />
                <p class="text-sm">准备开始</p>
              </div>
            </div>
            
            <Button 
              @click="pickRandom" 
              :disabled="isAnimating || pickerItemCount === 0"
              class="mt-4"
            >
              {{ isAnimating ? '选择中...' : '🎲 随机选择' }}
            </Button>
          </div>
        </Card>
      </div>

      <!-- Oracle Mode -->
      <div v-if="currentMode === 'oracle'" class="space-y-4">
        <Card class="p-4 md:p-6 shadow-lg">
          <!-- Compact Controls -->
          <div class="flex items-center gap-2 mb-4">
            <Input 
              v-model="oracleQuestion" 
              placeholder="问一个是非问题，例如：今天我应该去跑步吗？"
              maxlength="80"
              class="h-9 text-sm"
              @keyup.enter="askOracle"
            />
            <div class="flex gap-1">
              <Button @click="resetOracle" variant="ghost" size="icon" class="h-9 w-9">
                <RotateCcw :size="16" />
              </Button>
              <Button @click="copyResult" variant="ghost" size="icon" class="h-9 w-9" :disabled="!result">
                <Copy :size="16" />
              </Button>
            </div>
          </div>

          <!-- Main Animation Area - Focus -->
          <div class="flex flex-col items-center justify-center py-4 md:py-6">
            <div 
              id="oracle-ball"
              class="relative w-44 h-44 md:w-52 md:h-52 rounded-full bg-gradient-to-br from-purple-900 via-indigo-900 to-purple-800 shadow-2xl flex items-center justify-center mb-4 cursor-pointer transition-all hover:scale-105"
              @click="askOracle"
            >
              <!-- Outer glow -->
              <div class="absolute inset-0 rounded-full bg-purple-500/30 blur-xl animate-pulse"></div>
              
              <!-- Inner sphere -->
              <div class="relative w-24 h-24 md:w-28 md:h-28 rounded-full bg-gradient-to-br from-indigo-400 via-purple-500 to-indigo-600 flex items-center justify-center shadow-inner">
                <div class="w-16 h-16 md:w-20 md:h-20 rounded-full bg-gradient-to-br from-indigo-950 to-purple-950 flex items-center justify-center relative overflow-hidden">
                  <!-- Shine effect -->
                  <div class="absolute inset-0 bg-gradient-to-br from-white/20 via-transparent to-transparent"></div>
                  <Sparkles :size="32" class="text-yellow-300 animate-pulse relative z-10" />
                </div>
              </div>
              
              <!-- Answer display -->
              <div 
                v-if="oracleAnswer"
                class="absolute inset-0 flex items-center justify-center p-6 md:p-8 animate-in fade-in zoom-in duration-500"
              >
                <div class="text-center text-white font-bold text-sm md:text-lg bg-black/50 backdrop-blur-md rounded-xl p-4 shadow-2xl border border-white/20">
                  {{ oracleAnswer }}
                </div>
              </div>
            </div>
            
            <Button 
              @click="askOracle" 
              :disabled="isAnimating || !oracleQuestion.trim()"
            >
              {{ isAnimating ? '思考中...' : '✨ 询问神谕' }}
            </Button>
          </div>
        </Card>
      </div>

    </div>
  </div>
</template>

<style scoped>
.preserve-3d {
  transform-style: preserve-3d;
}

.backface-hidden {
  backface-visibility: hidden;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-10px); }
  20%, 40%, 60%, 80% { transform: translateX(10px); }
}
</style>
