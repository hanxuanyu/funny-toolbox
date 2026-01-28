<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef, watch } from "vue"
import QRCodeStyling, { type FileExtension, type Options as QRCodeOptions, type TypeNumber } from "qr-code-styling"
import jsQR from "jsqr"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
    // CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { Switch } from "@/components/ui/switch"
import { Textarea } from "@/components/ui/textarea"
import GradientControls from "@/components/GradientControls.vue"
import type { GradientForm, QrFormState, QrPreset } from "@/types/qr"

const defaultQrData = typeof window !== "undefined" ? window.location.href : "https://funny-toolbox.io/"

const previewRef = ref<HTMLElement | null>(null)
const floatingPreviewRef = ref<HTMLElement | null>(null)
const previewContainerRef = ref<HTMLElement | null>(null)
const previewAreaRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const existingQrInputRef = ref<HTMLInputElement | null>(null)
const qrInstance = shallowRef<QRCodeStyling | null>(null)
const isDownloading = ref(false)
const activePreset = ref("自定义")
const PREVIEW_SIZE = 300
const previewSize = ref(PREVIEW_SIZE)
const windowHeight = ref(typeof window !== "undefined" ? window.innerHeight : 900)
const windowWidth = ref(typeof window !== "undefined" ? window.innerWidth : 1200)
const isPreviewInViewport = ref(true)
const floatingPosition = reactive({ x: 16, y: 16 })
const floatingDragOffset = reactive({ x: 0, y: 0 })
const isDraggingFloatingPreview = ref(false)
const downloadSizeChoice = ref("512")
const downloadSizeOptions = ["256", "320", "384", "448", "512", "640", "800", "1024"]
const resolvedDownloadSize = computed(() => Number(downloadSizeChoice.value) || previewSize.value)
const previewBoxStyle = computed(() => ({
  width: `${previewSize.value}px`,
  height: `${previewSize.value}px`,
  maxWidth: "100%",
  maxHeight: "100%",
}))
const isMobileViewport = computed(() => windowWidth.value < 1024)
const shouldShowFloatingPreview = computed(() => isMobileViewport.value && !isPreviewInViewport.value)
const floatingVisibilityReady = ref(false)
const shouldRenderFloatingPreview = computed(() => floatingVisibilityReady.value && shouldShowFloatingPreview.value)

const randomId = () => Math.random().toString(36).slice(2, 9)

const encodeUtf8Binary = (text: string) => {
  if (!text)
    return ""
  if (typeof TextEncoder !== "undefined") {
    const encoder = new TextEncoder()
    const bytes = encoder.encode(text)
    let binary = ""
    bytes.forEach((byte) => {
      binary += String.fromCharCode(byte)
    })
    return binary
  }
  try {
    return unescape(encodeURIComponent(text))
  }
  catch {
    return text
  }
}

const createGradient = (
  options?: Partial<Omit<GradientForm, "colorStops">> & { colorStops?: Array<{ offset: number; color: string }> },
): GradientForm => ({
  enabled: options?.enabled ?? false,
  type: options?.type ?? "linear",
  rotation: options?.rotation ?? 0,
  colorStops:
    options?.colorStops?.map((stop) => ({
      ...stop,
      id: randomId(),
    }))
    ?? [
      { id: randomId(), offset: 0, color: "#000000" },
      { id: randomId(), offset: 100, color: "#ffffff" },
    ],
})

const createDefaultState = (): QrFormState => ({
  width: PREVIEW_SIZE,
  height: PREVIEW_SIZE,
  type: "svg",
  shape: "square",
  margin: 12,
  data: defaultQrData,
  image: "",
  qrOptions: {
    typeNumber: 0,
    mode: "Byte",
    errorCorrectionLevel: "Q",
  },
  imageOptions: {
    hideBackgroundDots: true,
    imageSize: 0.4,
    margin: 0,
    crossOrigin: "none",
    saveAsBlob: true,
  },
  dotsOptions: {
    type: "rounded",
    color: "#111827",
    roundSize: true,
    gradient: createGradient({
      enabled: false,
      colorStops: [
        { offset: 0, color: "#0f172a" },
        { offset: 100, color: "#2563eb" },
      ],
    }),
  },
  backgroundOptions: {
    color: "#ffffff",
    gradient: createGradient(),
  },
  cornersSquareOptions: {
    type: "extra-rounded",
    color: "#111827",
    gradient: createGradient({ enabled: false }),
  },
  cornersDotOptions: {
    type: "dot",
    color: "#2563eb",
    gradient: createGradient({ enabled: false }),
  },
})

const form = reactive<QrFormState>(createDefaultState())
const lastRoundSizeBeforeCircle = ref(form.dotsOptions.roundSize)
const isCircleShape = computed(() => form.shape === "circle")
const isDecodingExistingQr = ref(false)
const existingQrMessage = reactive({
  type: "idle" as "idle" | "success" | "error",
  text: "",
})

let previewResizeObserver: ResizeObserver | null = null
let previewAreaResizeObserver: ResizeObserver | null = null
let previewVisibilityObserver: IntersectionObserver | null = null
let floatingPositionInitialized = false

const getPreviewAreaHeight = () => {
  if (!previewAreaRef.value)
    return Infinity
  if (typeof window === "undefined")
    return previewAreaRef.value.clientHeight
  const style = window.getComputedStyle(previewAreaRef.value)
  const padding = parseFloat(style.paddingTop || "0") + parseFloat(style.paddingBottom || "0")
  return Math.max(0, previewAreaRef.value.clientHeight - padding)
}

const clampPreviewSize = (width: number) => {
  const widthBound = Math.min(480, Math.max(180, Math.round(width || PREVIEW_SIZE)))
  const heightAllowance = (() => {
    const areaHeight = getPreviewAreaHeight()
    if (!Number.isFinite(areaHeight) || areaHeight <= 0)
      return Math.max(180, Math.min(420, windowHeight.value - 220))
    return Math.max(160, Math.min(420, areaHeight))
  })()
  return Math.max(160, Math.min(widthBound, heightAllowance))
}

const syncPreviewDimensions = (size: number) => {
  const bounded = clampPreviewSize(size)
  if (previewSize.value === bounded)
    return
  previewSize.value = bounded
  form.width = bounded
  form.height = bounded
  if (qrInstance.value)
    qrInstance.value.update({ width: bounded, height: bounded })
}

const observePreviewContainer = (el: HTMLElement | null) => {
  previewResizeObserver?.disconnect()
  if (!el) {
    previewResizeObserver = null
    return
  }
  syncPreviewDimensions(el.clientWidth)
  if (typeof ResizeObserver === "undefined") {
    previewResizeObserver = null
    return
  }
  previewResizeObserver = new ResizeObserver((entries) => {
    const entry = entries[0]
    if (!entry)
      return
    syncPreviewDimensions(entry.contentRect.width)
  })
  previewResizeObserver.observe(el)
}

const observePreviewArea = (el: HTMLElement | null) => {
  previewAreaResizeObserver?.disconnect()
  if (!el || typeof ResizeObserver === "undefined") {
    previewAreaResizeObserver = null
    if (previewContainerRef.value)
      syncPreviewDimensions(previewContainerRef.value.clientWidth)
    return
  }
  previewAreaResizeObserver = new ResizeObserver(() => {
    if (previewContainerRef.value)
      syncPreviewDimensions(previewContainerRef.value.clientWidth)
  })
  previewAreaResizeObserver.observe(el)
  if (previewContainerRef.value)
    syncPreviewDimensions(previewContainerRef.value.clientWidth)
}

const observePreviewVisibility = (el: HTMLElement | null) => {
  previewVisibilityObserver?.disconnect()
  if (!el || typeof IntersectionObserver === "undefined") {
    previewVisibilityObserver = null
    isPreviewInViewport.value = true
    floatingVisibilityReady.value = !!el || typeof IntersectionObserver === "undefined"
    return
  }
  floatingVisibilityReady.value = false
  previewVisibilityObserver = new IntersectionObserver((entries) => {
    const entry = entries[0]
    isPreviewInViewport.value = !!entry?.isIntersecting
    floatingVisibilityReady.value = true
  }, {
    threshold: 0.2,
    rootMargin: "-32px 0px -32px 0px",
  })
  previewVisibilityObserver.observe(el)
}

const ensureCircleRoundSize = () => {
  if (form.shape === "circle" && !form.dotsOptions.roundSize)
    form.dotsOptions.roundSize = true
}

const clampImageSize = (value: number) => {
  const clamped = clampNumber(value, 0.1, 0.6)
  return Math.round(clamped * 10) / 10
}

const setExistingQrMessage = (type: "idle" | "success" | "error", text: string) => {
  existingQrMessage.type = type
  existingQrMessage.text = text
}

const openExistingQrPicker = () => existingQrInputRef.value?.click()

const readFileAsDataUrl = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      if (typeof reader.result === "string")
        resolve(reader.result)
      else
        reject(new Error("无法读取图片内容"))
    }
    reader.onerror = () => reject(new Error("读取文件失败"))
    reader.readAsDataURL(file)
  })

const loadImageData = (dataUrl: string) =>
  new Promise<ImageData>((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement("canvas")
      canvas.width = img.width
      canvas.height = img.height
      const ctx = canvas.getContext("2d")
      if (!ctx) {
        reject(new Error("浏览器暂不支持 Canvas 操作"))
        return
      }
      ctx.drawImage(img, 0, 0)
      resolve(ctx.getImageData(0, 0, canvas.width, canvas.height))
    }
    img.onerror = () => reject(new Error("图片加载失败，请重试"))
    img.src = dataUrl
  })

const decodeQrFromFile = async (file: File) => {
  const dataUrl = await readFileAsDataUrl(file)
  const imageData = await loadImageData(dataUrl)
  const result = jsQR(imageData.data, imageData.width, imageData.height, {
    inversionAttempts: "attemptBoth",
  })
  if (result?.data)
    return result.data.trim()
  throw new Error("未检测到二维码或内容不可识别")
}

const summarizeDecodedText = (text: string) => (text.length > 60 ? `${text.slice(0, 60)}…` : text)

const handleExistingQrUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement | null
  const file = input?.files?.[0]
  if (!file)
    return
  isDecodingExistingQr.value = true
  setExistingQrMessage("idle", "解析中…")
  try {
    const decoded = await decodeQrFromFile(file)
    form.data = decoded
    activePreset.value = "自定义"
    setExistingQrMessage("success", `解析成功：${summarizeDecodedText(decoded)}`)
  }
  catch (error) {
    const message = error instanceof Error ? error.message : "解析失败，请更换图片"
    setExistingQrMessage("error", message)
  }
  finally {
    isDecodingExistingQr.value = false
    if (input)
      input.value = ""
  }
}

const presets: QrPreset[] = [
  {
    name: "商务蓝调",
    description: "品牌官网常用的冷静配色",
    config: () => ({
      dotsOptions: {
        type: "classy",
        color: "#0f172a",
        roundSize: true,
        gradient: createGradient({
          enabled: true,
          rotation: 25,
          colorStops: [
            { offset: 0, color: "#0f172a" },
            { offset: 100, color: "#1d4ed8" },
          ],
        }),
      },
      backgroundOptions: {
        color: "#e0e7ff",
        gradient: createGradient({
          enabled: true,
          type: "radial",
          colorStops: [
            { offset: 0, color: "#f8fafc" },
            { offset: 100, color: "#e0e7ff" },
          ],
        }),
      },
      cornersSquareOptions: {
        type: "extra-rounded",
        color: "#0f172a",
        gradient: createGradient({ enabled: false }),
      },
      cornersDotOptions: {
        type: "dot",
        color: "#2563eb",
        gradient: createGradient({ enabled: false }),
      },
      imageOptions: {
        imageSize: 0.32,
        margin: 10,
        hideBackgroundDots: true,
        crossOrigin: "anonymous",
        saveAsBlob: true,
      },
      shape: "square",
    }),
  },
  {
    name: "霓虹夜灯",
    description: "暗黑背景下的高饱和渐变",
    config: () => ({
      backgroundOptions: {
        color: "#020617",
        gradient: createGradient({
          enabled: true,
          rotation: 90,
          colorStops: [
            { offset: 0, color: "#020617" },
            { offset: 100, color: "#0f172a" },
          ],
        }),
      },
      dotsOptions: {
        type: "dots",
        color: "#38bdf8",
        roundSize: false,
        gradient: createGradient({
          enabled: true,
          rotation: 60,
          colorStops: [
            { offset: 0, color: "#14b8a6" },
            { offset: 100, color: "#a855f7" },
          ],
        }),
      },
      cornersSquareOptions: {
        type: "classy-rounded",
        color: "#f472b6",
        gradient: createGradient({ enabled: false }),
      },
      cornersDotOptions: {
        type: "extra-rounded",
        color: "#38bdf8",
        gradient: createGradient({ enabled: false }),
      },
      qrOptions: {
        typeNumber: 0,
        mode: "Byte",
        errorCorrectionLevel: "H",
      },
      shape: "circle",
    }),
  },
  {
    name: "极简留白",
    description: "简洁电商或个人名片",
    config: () => ({
      backgroundOptions: {
        color: "#ffffff",
        gradient: createGradient({ enabled: false }),
      },
      dotsOptions: {
        type: "square",
        color: "#111827",
        roundSize: true,
        gradient: createGradient({ enabled: false }),
      },
      cornersSquareOptions: {
        type: "rounded",
        color: "#111827",
        gradient: createGradient({ enabled: false }),
      },
      cornersDotOptions: {
        type: "dot",
        color: "#111827",
        gradient: createGradient({ enabled: false }),
      },
      imageOptions: {
        imageSize: 0.26,
        margin: 16,
        hideBackgroundDots: true,
        crossOrigin: "none",
        saveAsBlob: true,
      },
      shape: "square",
    }),
  },
  {
    name: "暖色渐层",
    description: "活动海报或票券",
    config: () => ({
      backgroundOptions: {
        color: "#fff7ed",
        gradient: createGradient({
          enabled: true,
          type: "radial",
          colorStops: [
            { offset: 0, color: "#fff7ed" },
            { offset: 100, color: "#fed7aa" },
          ],
        }),
      },
      dotsOptions: {
        type: "classy-rounded",
        color: "#f97316",
        roundSize: true,
        gradient: createGradient({
          enabled: true,
          rotation: 120,
          colorStops: [
            { offset: 0, color: "#ea580c" },
            { offset: 100, color: "#facc15" },
          ],
        }),
      },
      cornersSquareOptions: {
        type: "extra-rounded",
        color: "#ea580c",
        gradient: createGradient({ enabled: false }),
      },
      cornersDotOptions: {
        type: "classy",
        color: "#b45309",
        gradient: createGradient({ enabled: false }),
      },
      qrOptions: {
        typeNumber: 0,
        mode: "Byte",
        errorCorrectionLevel: "M",
      },
      shape: "square",
    }),
  },
  {
    name: "柠檬科技",
    description: "明亮的浅色科技风，适合 App 下载页",
    config: () => ({
      backgroundOptions: {
        color: "#ecfccb",
        gradient: createGradient({
          enabled: true,
          rotation: 45,
          colorStops: [
            { offset: 0, color: "#f0fdf4" },
            { offset: 100, color: "#ecfccb" },
          ],
        }),
      },
      dotsOptions: {
        type: "rounded",
        color: "#15803d",
        roundSize: true,
        gradient: createGradient({
          enabled: true,
          rotation: 10,
          colorStops: [
            { offset: 0, color: "#22c55e" },
            { offset: 100, color: "#65a30d" },
          ],
        }),
      },
      cornersSquareOptions: {
        type: "rounded",
        color: "#166534",
        gradient: createGradient({ enabled: false }),
      },
      cornersDotOptions: {
        type: "classy-rounded",
        color: "#65a30d",
        gradient: createGradient({ enabled: false }),
      },
      imageOptions: {
        imageSize: 0.3,
        margin: 14,
        hideBackgroundDots: true,
        crossOrigin: "anonymous",
        saveAsBlob: true,
      },
      qrOptions: {
        typeNumber: 0,
        mode: "Byte",
        errorCorrectionLevel: "Q",
      },
      shape: "square",
    }),
  },
  {
    name: "海盐薄荷",
    description: "低饱和绿色，自然清新",
    config: () => ({
      backgroundOptions: {
        color: "#ecfeff",
        gradient: createGradient({
          enabled: true,
          rotation: 45,
          colorStops: [
            { offset: 0, color: "#e0f2fe" },
            { offset: 100, color: "#ccfbf1" },
          ],
        }),
      },
      dotsOptions: {
        type: "rounded",
        color: "#0f766e",
        roundSize: true,
        gradient: createGradient({
          enabled: true,
          rotation: 60,
          colorStops: [
            { offset: 0, color: "#14b8a6" },
            { offset: 100, color: "#0ea5e9" },
          ],
        }),
      },
      cornersSquareOptions: {
        type: "rounded",
        color: "#0f766e",
        gradient: createGradient({ enabled: false }),
      },
      cornersDotOptions: {
        type: "dot",
        color: "#0ea5e9",
        gradient: createGradient({ enabled: false }),
      },
      shape: "square",
    }),
  },
  {
    name: "紫罗兰流光",
    description: "粉紫渐层，适合活动推广",
    config: () => ({
      backgroundOptions: {
        color: "#fdf4ff",
        gradient: createGradient({
          enabled: true,
          type: "radial",
          colorStops: [
            { offset: 0, color: "#faf5ff" },
            { offset: 100, color: "#f0abfc" },
          ],
        }),
      },
      dotsOptions: {
        type: "classy-rounded",
        color: "#a855f7",
        roundSize: true,
        gradient: createGradient({
          enabled: true,
          rotation: 120,
          colorStops: [
            { offset: 0, color: "#d946ef" },
            { offset: 100, color: "#7c3aed" },
          ],
        }),
      },
      cornersSquareOptions: {
        type: "extra-rounded",
        color: "#701a75",
        gradient: createGradient({ enabled: false }),
      },
      cornersDotOptions: {
        type: "classy",
        color: "#a855f7",
        gradient: createGradient({ enabled: false }),
      },
      shape: "square",
    }),
  },
  {
    name: "灰度工业",
    description: "科技展板常见灰调",
    config: () => ({
      backgroundOptions: {
        color: "#f5f5f5",
        gradient: createGradient({ enabled: false }),
      },
      dotsOptions: {
        type: "square",
        color: "#1f2937",
        roundSize: false,
        gradient: createGradient({ enabled: false }),
      },
      cornersSquareOptions: {
        type: "classy-rounded",
        color: "#111827",
        gradient: createGradient({ enabled: false }),
      },
      cornersDotOptions: {
        type: "square",
        color: "#4b5563",
        gradient: createGradient({ enabled: false }),
      },
      qrOptions: {
        typeNumber: 0,
        mode: "Byte",
        errorCorrectionLevel: "Q",
      },
    }),
  },
  {
    name: "樱桃布丁",
    description: "红橙对比，节日气氛",
    config: () => ({
      backgroundOptions: {
        color: "#fff1f2",
        gradient: createGradient({
          enabled: true,
          rotation: 90,
          colorStops: [
            { offset: 0, color: "#ffe4e6" },
            { offset: 100, color: "#fdf2f8" },
          ],
        }),
      },
      dotsOptions: {
        type: "dots",
        color: "#dc2626",
        roundSize: false,
        gradient: createGradient({
          enabled: true,
          rotation: 45,
          colorStops: [
            { offset: 0, color: "#fb7185" },
            { offset: 100, color: "#f97316" },
          ],
        }),
      },
      cornersSquareOptions: {
        type: "extra-rounded",
        color: "#be123c",
        gradient: createGradient({ enabled: false }),
      },
      cornersDotOptions: {
        type: "dot",
        color: "#f97316",
        gradient: createGradient({ enabled: false }),
      },
      shape: "square",
    }),
  },
]

const dotTypeOptions = [
  { value: "square", label: "直角 square" },
  { value: "rounded", label: "圆角 rounded" },
  { value: "dots", label: "圆点 dots" },
  { value: "classy", label: "Classy" },
  { value: "classy-rounded", label: "Classy rounded" },
  { value: "extra-rounded", label: "Extra rounded" },
]

const cornerTypeOptions = [
  { value: "square", label: "Square" },
  { value: "dot", label: "Dot" },
  { value: "rounded", label: "Rounded" },
  { value: "extra-rounded", label: "Extra rounded" },
  { value: "dots", label: "Dots" },
  { value: "classy", label: "Classy" },
  { value: "classy-rounded", label: "Classy rounded" },
]

const modeOptions = ["Numeric", "Alphanumeric", "Byte", "Kanji"]
const errorCorrectionOptions = ["L", "M", "Q", "H"]

const clampNumber = (value: number, min: number, max: number) => {
  const safe = Number.isFinite(value) ? value : min
  return Math.min(max, Math.max(min, safe))
}

const floatingPreviewInnerSize = computed(() => clampNumber(Math.round(previewSize.value * 0.45), 120, 180))
const floatingPreviewOuterWidth = computed(() => floatingPreviewInnerSize.value + 28)
const floatingPreviewOuterHeight = computed(() => floatingPreviewInnerSize.value + 60)
const floatingPreviewViewportStyle = computed(() => ({
  width: `${floatingPreviewInnerSize.value}px`,
  height: `${floatingPreviewInnerSize.value}px`,
}))
const floatingPreviewScale = computed(() => {
  if (!previewSize.value)
    return 1
  return Math.min(1, Number((floatingPreviewInnerSize.value / previewSize.value).toFixed(4)))
})
const floatingPreviewStyle = computed(() => ({
  width: `${floatingPreviewOuterWidth.value}px`,
  height: `${floatingPreviewOuterHeight.value}px`,
  left: `${floatingPosition.x}px`,
  top: `${floatingPosition.y}px`,
  touchAction: "none",
}))

const clampFloatingPosition = (x: number, y: number) => {
  const padding = 12
  const maxX = Math.max(padding, windowWidth.value - floatingPreviewOuterWidth.value - padding)
  const maxY = Math.max(padding, windowHeight.value - floatingPreviewOuterHeight.value - padding)
  floatingPosition.x = clampNumber(x, padding, maxX)
  floatingPosition.y = clampNumber(y, padding, maxY)
}

const resetFloatingPreviewPosition = () => {
  clampFloatingPosition(
    windowWidth.value - floatingPreviewOuterWidth.value - 16,
    windowHeight.value - floatingPreviewOuterHeight.value - 24,
  )
  floatingPositionInitialized = true
}

const handleFloatingPointerMove = (event: PointerEvent) => {
  if (!isDraggingFloatingPreview.value)
    return
  clampFloatingPosition(event.clientX - floatingDragOffset.x, event.clientY - floatingDragOffset.y)
}

const stopFloatingDrag = () => {
  if (!isDraggingFloatingPreview.value)
    return
  isDraggingFloatingPreview.value = false
  window.removeEventListener("pointermove", handleFloatingPointerMove)
  window.removeEventListener("pointerup", stopFloatingDrag)
}

const handleFloatingPointerDown = (event: PointerEvent) => {
  if (!shouldRenderFloatingPreview.value)
    return
  isDraggingFloatingPreview.value = true
  floatingDragOffset.x = event.clientX - floatingPosition.x
  floatingDragOffset.y = event.clientY - floatingPosition.y
  window.addEventListener("pointermove", handleFloatingPointerMove)
  window.addEventListener("pointerup", stopFloatingDrag)
  event.preventDefault()
}

const openFilePicker = () => fileInputRef.value?.click()

const handleImageUpload = (event: Event) => {
  const file = (event.target as HTMLInputElement)?.files?.[0]
  if (!file)
    return
  const reader = new FileReader()
  reader.onload = () => {
    form.image = reader.result?.toString() ?? ""
  }
  reader.readAsDataURL(file)
}

const clearImage = () => {
  form.image = ""
  if (fileInputRef.value)
    fileInputRef.value.value = ""
}

const applyPartial = (target: any, patch: any) => {
  Object.entries(patch).forEach(([key, value]) => {
    if (value === undefined)
      return
    if (isPlainObject(value) && isPlainObject(target[key])) {
      applyPartial(target[key], value as Record<string, any>)
    }
    else if (Array.isArray(value)) {
      target[key] = deepClone(value)
    }
    else {
      target[key] = value
    }
  })
}

const deepClone = <T,>(value: T): T => {
  if (typeof structuredClone === "function")
    return structuredClone(value)
  return JSON.parse(JSON.stringify(value)) as T
}

const isPlainObject = (value: unknown): value is Record<string, any> =>
  !!value && typeof value === "object" && !Array.isArray(value)

const applyPreset = (preset: QrPreset) => {
  applyPartial(form, preset.config() as Record<string, any>)
  form.width = previewSize.value
  form.height = previewSize.value
  ensureCircleRoundSize()
  activePreset.value = preset.name
}

const resetConfig = () => {
  Object.assign(form, createDefaultState())
  form.width = previewSize.value
  form.height = previewSize.value
  activePreset.value = "自定义"
}

const gradientToOptions = (gradient: GradientForm) => {
  if (!gradient.enabled || !gradient.colorStops || gradient.colorStops.length < 2)
    return undefined
  return {
    type: gradient.type,
    rotation: (gradient.rotation * Math.PI) / 180,
    colorStops: [...gradient.colorStops]
      .sort((a, b) => a.offset - b.offset)
      .map((stop) => ({
        offset: stop.offset / 100,
        color: stop.color,
      })),
  }
}

const buildQrOptions = (state: QrFormState, sizeOverride?: number): QRCodeOptions => ({
  width: sizeOverride ?? state.width,
  height: sizeOverride ?? state.height,
  type: state.type,
  shape: state.shape,
  margin: state.margin,
  data: encodeUtf8Binary(state.data || "https://example.com"),
  image: state.image || undefined,
  qrOptions: {
    ...state.qrOptions,
    typeNumber: state.qrOptions.typeNumber as TypeNumber,
  },
  imageOptions: {
    ...state.imageOptions,
    crossOrigin: state.imageOptions.crossOrigin === "none" ? undefined : state.imageOptions.crossOrigin,
  },
  dotsOptions: {
    type: state.dotsOptions.type,
    color: state.dotsOptions.color,
    roundSize: state.dotsOptions.roundSize,
    gradient: gradientToOptions(state.dotsOptions.gradient),
  },
  cornersSquareOptions: {
    type: state.cornersSquareOptions.type,
    color: state.cornersSquareOptions.color,
    gradient: gradientToOptions(state.cornersSquareOptions.gradient),
  },
  cornersDotOptions: {
    type: state.cornersDotOptions.type,
    color: state.cornersDotOptions.color,
    gradient: gradientToOptions(state.cornersDotOptions.gradient),
  },
  backgroundOptions: {
    color: state.backgroundOptions.color,
    gradient: gradientToOptions(state.backgroundOptions.gradient),
  },
})

const clonePreviewNode = (node: Element): HTMLElement | SVGElement | null => {
  if (typeof document === "undefined")
    return null
  if (node instanceof HTMLCanvasElement) {
    const clonedCanvas = document.createElement("canvas")
    clonedCanvas.width = node.width
    clonedCanvas.height = node.height
    const ctx = clonedCanvas.getContext("2d")
    if (ctx)
      ctx.drawImage(node, 0, 0)
    return clonedCanvas
  }
  if (node instanceof SVGElement)
    return node.cloneNode(true) as SVGElement
  if (node instanceof HTMLElement)
    return node.cloneNode(true) as HTMLElement
  return null
}

const applyFloatingStyles = (el: HTMLElement | SVGElement) => {
  el.style.width = `${previewSize.value}px`
  el.style.height = `${previewSize.value}px`
  el.style.transformOrigin = "top left"
  el.style.transform = `scale(${floatingPreviewScale.value})`
  el.style.display = "block"
}

const syncFloatingPreviewContainer = () => {
  if (!shouldRenderFloatingPreview.value)
    return
  const target = floatingPreviewRef.value
  const sourceContainer = previewRef.value
  if (!target || !sourceContainer)
    return
  const source = sourceContainer.firstElementChild
  if (!source)
    return
  const clone = clonePreviewNode(source)
  if (!clone)
    return
  applyFloatingStyles(clone)
  target.innerHTML = ""
  target.appendChild(clone)
}

const scheduleFloatingSync = async () => {
  if (!shouldRenderFloatingPreview.value)
    return
  await nextTick()
  syncFloatingPreviewContainer()
}

const handleDownload = async (extension: FileExtension) => {
  isDownloading.value = true
  try {
    const targetSize = resolvedDownloadSize.value
    const exportQr = new QRCodeStyling(buildQrOptions(form, targetSize))
    await exportQr.download({
      extension,
      name: `qr-${targetSize}-${extension}-${Date.now()}`,
    })
  }
  finally {
    isDownloading.value = false
    scheduleFloatingSync()
  }
}

const dataLength = computed(() => new TextEncoder().encode(form.data).length)

onMounted(() => {
  qrInstance.value = new QRCodeStyling(buildQrOptions(form))
  if (previewRef.value)
    qrInstance.value.append(previewRef.value)
  scheduleFloatingSync()
})

watch(
  form,
  () => {
    const nextOptions = buildQrOptions(form)
    if (qrInstance.value)
      qrInstance.value.update(nextOptions)
    scheduleFloatingSync()
  },
  { deep: true },
)

watch(
  () => form.imageOptions.imageSize,
  (value) => {
    const normalized = clampImageSize(value)
    if (normalized !== value)
      form.imageOptions.imageSize = normalized
  },
)

watch(
  () => form.imageOptions.margin,
  (value) => {
    const normalized = clampNumber(value, 0, 17)
    if (normalized !== value)
      form.imageOptions.margin = normalized
  },
)

watch(
  () => form.shape,
  (shape, previousShape) => {
    if (shape === "circle" && previousShape !== "circle") {
      lastRoundSizeBeforeCircle.value = form.dotsOptions.roundSize
      ensureCircleRoundSize()
    }
    else if (shape !== "circle" && previousShape === "circle") {
      form.dotsOptions.roundSize = lastRoundSizeBeforeCircle.value
    }
  },
)

watch(previewContainerRef, (el) => {
  observePreviewContainer(el)
})

watch(previewAreaRef, (el) => {
  observePreviewArea(el)
  observePreviewVisibility(el)
})

watch(previewRef, (el) => {
  if (el && qrInstance.value) {
    el.innerHTML = ""
    qrInstance.value.append(el)
    scheduleFloatingSync()
  }
})

watch(floatingPreviewRef, (el) => {
  if (el)
    scheduleFloatingSync()
})

watch(shouldRenderFloatingPreview, async (visible) => {
  if (visible) {
    await nextTick()
    if (!floatingPositionInitialized) {
      resetFloatingPreviewPosition()
      floatingPositionInitialized = true
    }
    else {
      clampFloatingPosition(floatingPosition.x, floatingPosition.y)
    }
    syncFloatingPreviewContainer()
  }
  else {
    stopFloatingDrag()
  }
})

watch([floatingPreviewOuterWidth, floatingPreviewOuterHeight], () => {
  if (shouldRenderFloatingPreview.value)
    clampFloatingPosition(floatingPosition.x, floatingPosition.y)
})

watch([floatingPreviewInnerSize, previewSize], () => {
  scheduleFloatingSync()
})

const fileExtensions: FileExtension[] = ["png", "jpeg", "webp", "svg"]

const handleWindowResize = () => {
  windowHeight.value = window.innerHeight
  windowWidth.value = window.innerWidth
  if (previewContainerRef.value)
    syncPreviewDimensions(previewContainerRef.value.clientWidth)
  if (shouldRenderFloatingPreview.value)
    clampFloatingPosition(floatingPosition.x, floatingPosition.y)
}

onMounted(() => {
  window.addEventListener("resize", handleWindowResize)
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleWindowResize)
  previewResizeObserver?.disconnect()
  previewAreaResizeObserver?.disconnect()
  previewVisibilityObserver?.disconnect()
  stopFloatingDrag()
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-b from-muted/40 via-background to-background">
    <main class="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-4 lg:gap-6 lg:py-8">
      <header class="space-y-3 text-center lg:text-left">
        <div class="space-y-2">
          <h1 class="text-3xl font-semibold tracking-tight md:text-4xl">Funny-QRCode</h1>
        </div>
      </header>

      <div class="grid gap-6 lg:grid-cols-[minmax(0,1.4fr)_minmax(320px,0.8fr)]">
          <Card class="order-2 flex max-h-none flex-col overflow-hidden border-border/70 lg:order-1 lg:h-[calc(100vh-160px)]">
          <CardHeader class="gap-2 pb-0">
            <CardTitle>参数配置</CardTitle>
            <!--<CardDescription>所有参数与 qr-code-styling 官方一致，移动端也可轻松操作。</CardDescription>-->
          </CardHeader>
          <CardContent class="flex-1 overflow-hidden p-0">
            <div
              class="h-full space-y-6 overflow-y-auto px-4 pb-6 pt-6 md:px-6 lg:pr-4"
            >
                <section class="space-y-3">
                  <div class="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <p class="text-sm font-semibold">快速预设</p>
                    <!--<p class="text-xs text-muted-foreground">常用风格一键加载，稍后再微调细节</p>-->
                    </div>
                    <span class="text-xs text-muted-foreground">当前：{{ activePreset }}</span>
                  </div>
                  <div class="rounded-xl border bg-muted/40 p-3">
                    <div class="max-h-48 space-y-2 overflow-y-auto pr-1">
                      <div class="grid grid-cols-2 gap-2 sm:grid-cols-3">
                        <Button
                          v-for="preset in presets"
                          :key="preset.name"
                          :title="preset.description"
                          size="sm"
                          type="button"
                          :variant="activePreset === preset.name ? 'default' : 'outline'"
                          class="w-full rounded-full border-dashed px-3 py-1 text-xs font-medium transition"
                          :aria-pressed="activePreset === preset.name"
                          @click="applyPreset(preset)"
                        >
                          {{ preset.name }}
                        </Button>
                      </div>
                    </div>
                  </div>
                </section>

                <Separator />

                <section class="space-y-4">
                  <div class="space-y-1">
                    <p class="text-sm font-semibold">二维码内容</p>
                    <p class="text-xs text-muted-foreground">将链接、文本或 vCard 粘贴于此，实时预览更新</p>
                  </div>
                  <div class="flex flex-wrap items-center gap-3">
                    <Button
                      variant="outline"
                      size="sm"
                      type="button"
                      :disabled="isDecodingExistingQr"
                      @click="openExistingQrPicker"
                    >
                      现有二维码美化
                    </Button>
                    <span v-if="isDecodingExistingQr" class="text-xs text-muted-foreground">解析中…</span>
                    <span
                      v-else-if="existingQrMessage.text"
                      class="text-xs"
                      :class="existingQrMessage.type === 'error' ? 'text-destructive' : 'text-emerald-600'"
                    >
                      {{ existingQrMessage.text }}
                    </span>
                    <input
                      ref="existingQrInputRef"
                      type="file"
                      accept="image/*"
                      class="hidden"
                      @change="handleExistingQrUpload"
                    >
                  </div>
                  <div class="space-y-2">
                    <div class="flex items-center justify-between text-xs text-muted-foreground">
                      <Label for="qr-data">文本数据</Label>
                      <span>字节数：{{ dataLength }}</span>
                    </div>
                    <Textarea
                      id="qr-data"
                      v-model="form.data"
                      rows="4"
                      placeholder="https://your-link.com 或任何文本"
                    />
                  </div>
                </section>

                <Separator />

                <section class="space-y-4">
                  <div class="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <p class="text-sm font-semibold">基础参数</p>
                      <p class="text-xs text-muted-foreground">输出类型、整体形状与外边距</p>
                    </div>
                    <Button variant="ghost" size="sm" @click="resetConfig" type="button">恢复默认</Button>
                  </div>
                  <div class="grid gap-4 md:grid-cols-3">
                    <div class="space-y-2">
                      <Label for="qr-type">输出类型</Label>
                      <Select id="qr-type" v-model="form.type">
                        <SelectTrigger class="w-full">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="canvas">Canvas</SelectItem>
                          <SelectItem value="svg">SVG</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div class="space-y-2">
                      <Label for="qr-shape">整体形状</Label>
                      <Select id="qr-shape" v-model="form.shape">
                        <SelectTrigger class="w-full">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="square">Square</SelectItem>
                          <SelectItem value="circle">Circle</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div class="space-y-2">
                      <Label for="qr-margin">外边距 (px)</Label>
                      <div class="flex items-center gap-3">
                        <input
                          id="qr-margin-slider"
                          type="range"
                          min="0"
                          max="80"
                          step="1"
                          v-model.number="form.margin"
                          class="h-2 flex-1 cursor-pointer accent-primary"
                          aria-label="外边距滑动条"
                        >
                        <Input
                          id="qr-margin"
                          type="number"
                          min="0"
                          max="80"
                          class="w-20 text-right"
                          v-model.number="form.margin"
                          @change="form.margin = clampNumber(form.margin, 0, 80)"
                        />
                      </div>
                    </div>
                  </div>
                </section>

                <Separator />

                <section class="space-y-4">
                  <div>
                    <p class="text-sm font-semibold">QR 编码参数</p>
                    <p class="text-xs text-muted-foreground">选择编码模式、容错等级与版本</p>
                  </div>
                  <div class="grid gap-4 md:grid-cols-3">
                    <div class="space-y-2">
                      <Label for="qr-mode">编码模式</Label>
                      <Select id="qr-mode" v-model="form.qrOptions.mode">
                        <SelectTrigger class="w-full">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem v-for="mode in modeOptions" :key="mode" :value="mode">{{ mode }}</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div class="space-y-2">
                      <Label for="qr-ecc">容错等级</Label>
                      <Select id="qr-ecc" v-model="form.qrOptions.errorCorrectionLevel">
                        <SelectTrigger class="w-full">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem v-for="level in errorCorrectionOptions" :key="level" :value="level">
                            {{ level }}
                          </SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div class="space-y-2">
                      <Label for="qr-version">版本 (0 自动)</Label>
                      <div class="flex items-center gap-3">
                        <input
                          id="qr-version-slider"
                          type="range"
                          min="0"
                          max="40"
                          step="1"
                          v-model.number="form.qrOptions.typeNumber"
                          class="h-2 flex-1 cursor-pointer accent-primary"
                          aria-label="二维码版本滑动条"
                        >
                        <Input
                          id="qr-version"
                          type="number"
                          min="0"
                          max="40"
                          class="w-20 text-right"
                          v-model.number="form.qrOptions.typeNumber"
                          @change="form.qrOptions.typeNumber = clampNumber(form.qrOptions.typeNumber, 0, 40)"
                        />
                      </div>
                    </div>
                  </div>
                </section>

                <Separator />

                <section class="space-y-4">
                  <div>
                    <p class="text-sm font-semibold">中心图像</p>
                    <p class="text-xs text-muted-foreground">上传或引用品牌图标，立即展示</p>
                  </div>
                  <div class="grid gap-4 md:grid-cols-2">
                    <div class="space-y-2">
                      <Label for="qr-image-url">图像 URL</Label>
                      <Input
                        id="qr-image-url"
                        v-model="form.image"
                        placeholder="https://cdn...svg"
                      />
                      <p class="text-[11px] text-muted-foreground">可直接粘贴外链，或下方上传本地图片。</p>
                    </div>
                    <div class="space-y-2">
                      <Label>本地上传</Label>
                      <div class="flex flex-wrap items-center gap-2">
                        <input
                          ref="fileInputRef"
                          type="file"
                          accept="image/*"
                          class="hidden"
                          @change="handleImageUpload"
                        >
                        <Button variant="outline" size="sm" type="button" @click="openFilePicker">选择文件</Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          type="button"
                          @click="clearImage"
                          :disabled="!form.image"
                        >清空</Button>
                      </div>
                      <p class="text-[11px] text-muted-foreground">支持 PNG/JPEG/SVG，推荐大小小于 100KB。</p>
                    </div>
                  </div>
                  <div v-if="form.image" class="rounded-md border bg-muted/30 p-3 text-xs text-muted-foreground">
                    <p class="font-medium text-foreground">已载入图像</p>
                    <p class="truncate">{{ form.image.slice(0, 120) }}{{ form.image.length > 120 ? '…' : '' }}</p>
                  </div>
                  <div class="grid gap-4 md:grid-cols-2">
                    <div class="space-y-1.5">
                      <Label for="img-size">图像系数 (0-0.6)</Label>
                      <div class="flex items-center gap-3">
                        <input
                          id="img-size-slider"
                          type="range"
                          min="0.1"
                          max="0.6"
                          step="0.1"
                          v-model.number="form.imageOptions.imageSize"
                          class="h-2 flex-1 cursor-pointer accent-primary"
                          aria-label="图像系数滑动条"
                        >
                        <Input
                          id="img-size"
                          type="number"
                          min="0.1"
                          max="0.6"
                          step="0.1"
                          class="w-24 text-right"
                          v-model.number="form.imageOptions.imageSize"
                          @change="form.imageOptions.imageSize = clampImageSize(form.imageOptions.imageSize)"
                        />
                      </div>
                    </div>
                    <div class="space-y-1.5">
                      <Label for="img-margin">图像边距 (0-17px)</Label>
                      <div class="flex items-center gap-3">
                        <input
                          id="img-margin-slider"
                          type="range"
                          min="0"
                          max="17"
                          step="1"
                          v-model.number="form.imageOptions.margin"
                          class="h-2 flex-1 cursor-pointer accent-primary"
                          aria-label="图像边距滑动条"
                        >
                        <Input
                          id="img-margin"
                          type="number"
                          min="0"
                          max="17"
                          class="w-24 text-right"
                          v-model.number="form.imageOptions.margin"
                          @change="form.imageOptions.margin = clampNumber(form.imageOptions.margin, 0, 17)"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="grid gap-4 md:grid-cols-3">
                    <div class="space-y-1">
                      <Label for="img-origin">跨域策略</Label>
                      <Select id="img-origin" v-model="form.imageOptions.crossOrigin">
                        <SelectTrigger class="w-full">
                          <SelectValue placeholder="默认 (同源)" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="none">默认 (同源)</SelectItem>
                          <SelectItem value="anonymous">anonymous</SelectItem>
                          <SelectItem value="use-credentials">use-credentials</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div class="flex items-center justify-between rounded-md border px-3 py-2">
                      <div class="text-xs">
                        <p class="font-medium text-foreground">隐藏背景像素</p>
                        <p class="text-muted-foreground">Hide background dots</p>
                      </div>
                      <Switch v-model="form.imageOptions.hideBackgroundDots" />
                    </div>
                    <div class="flex items-center justify-between rounded-md border px-3 py-2">
                      <div class="text-xs">
                        <p class="font-medium text-foreground">SVG 内嵌图像</p>
                        <p class="text-muted-foreground">saveAsBlob</p>
                      </div>
                      <Switch v-model="form.imageOptions.saveAsBlob" />
                    </div>
                  </div>
                </section>

                <Separator />

                <section class="space-y-6">
                  <div>
                    <p class="text-sm font-semibold">点阵样式</p>
                    <p class="text-xs text-muted-foreground">Dots、定位角等细节调整</p>
                  </div>
                  <div class="grid gap-4 md:grid-cols-2">
                    <div class="space-y-2">
                      <Label for="dot-type">点阵类型</Label>
                      <Select id="dot-type" v-model="form.dotsOptions.type">
                        <SelectTrigger class="w-full">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem v-for="option in dotTypeOptions" :key="option.value" :value="option.value">
                            {{ option.label }}
                          </SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div class="space-y-2">
                      <Label>点阵颜色</Label>
                      <div class="flex items-center gap-3 rounded-md border bg-background px-3 py-2">
                        <input
                          type="color"
                          v-model="form.dotsOptions.color"
                          class="h-10 w-10 cursor-pointer rounded border"
                        >
                        <span class="text-sm font-medium">{{ form.dotsOptions.color }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="flex items-center justify-between rounded-md border px-3 py-2">
                    <div class="text-xs">
                      <p class="font-medium text-foreground">正方像素</p>
                      <p class="text-muted-foreground">roundSize = {{ form.dotsOptions.roundSize ? 'true' : 'false' }}</p>
                    </div>
                    <Switch v-model="form.dotsOptions.roundSize" :disabled="isCircleShape" :aria-disabled="isCircleShape" />
                  </div>
                  <p v-if="isCircleShape" class="text-[11px] text-amber-600">
                    圆形外轮廓需要固定像素栅格，系统已自动开启正方像素。
                  </p>
                  <GradientControls
                    v-model="form.dotsOptions.gradient"
                    title="点阵渐变"
                    description="最多添加 4 个颜色节点"
                  />

                  <Separator />

                  <div class="grid gap-8 lg:grid-cols-2">
                    <div class="space-y-4">
                      <div class="flex items-center justify-between">
                        <p class="text-sm font-semibold">角标 (方块)</p>
                        <Select v-model="form.cornersSquareOptions.type">
                          <SelectTrigger class="w-40">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem v-for="option in cornerTypeOptions" :key="option.value" :value="option.value">
                              {{ option.label }}
                            </SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div class="space-y-2">
                        <Label>颜色</Label>
                        <div class="flex items-center gap-3 rounded-md border bg-background px-3 py-2">
                          <input
                            type="color"
                            v-model="form.cornersSquareOptions.color"
                            class="h-10 w-10 cursor-pointer rounded border"
                          >
                          <span class="text-sm font-medium">{{ form.cornersSquareOptions.color }}</span>
                        </div>
                      </div>
                      <GradientControls
                        v-model="form.cornersSquareOptions.gradient"
                        title="角标渐变"
                      />
                    </div>
                    <div class="space-y-4">
                      <div class="flex items-center justify-between">
                        <p class="text-sm font-semibold">角点 (中点)</p>
                        <Select v-model="form.cornersDotOptions.type">
                          <SelectTrigger class="w-40">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem v-for="option in cornerTypeOptions" :key="option.value" :value="option.value">
                              {{ option.label }}
                            </SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div class="space-y-2">
                        <Label>颜色</Label>
                        <div class="flex items-center gap-3 rounded-md border bg-background px-3 py-2">
                          <input
                            type="color"
                            v-model="form.cornersDotOptions.color"
                            class="h-10 w-10 cursor-pointer rounded border"
                          >
                          <span class="text-sm font-medium">{{ form.cornersDotOptions.color }}</span>
                        </div>
                      </div>
                      <GradientControls
                        v-model="form.cornersDotOptions.gradient"
                        title="角点渐变"
                      />
                    </div>
                  </div>
                </section>

                <Separator />

                <section class="space-y-4">
                  <div>
                    <p class="text-sm font-semibold">背景样式</p>
                    <p class="text-xs text-muted-foreground">透明背景或渐变背景</p>
                  </div>
                  <div class="space-y-2">
                    <Label>背景色</Label>
                    <div class="flex items-center gap-3 rounded-md border bg-background px-3 py-2">
                      <input
                        type="color"
                        v-model="form.backgroundOptions.color"
                        class="h-10 w-10 cursor-pointer rounded border"
                      >
                      <span class="text-sm font-medium">{{ form.backgroundOptions.color }}</span>
                    </div>
                  </div>
                  <GradientControls
                    v-model="form.backgroundOptions.gradient"
                    title="背景渐变"
                  />
                </section>
              </div>
          </CardContent>
        </Card>

        <div class="order-1 flex flex-col gap-4 lg:order-2 lg:sticky lg:top-4">
          <Card class="flex flex-col border-border/70 lg:h-[calc(100vh-160px)] lg:overflow-hidden">
            <CardHeader class="flex flex-wrap items-center justify-between gap-3 pb-1">
              <div>
                <CardTitle class="text-lg leading-tight">实时预览</CardTitle>
              </div>
              <div class="flex items-center gap-2 text-xs text-muted-foreground">
                <span>导出尺寸</span>
                <Select v-model="downloadSizeChoice">
                  <SelectTrigger class="w-28 text-sm">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem v-for="size in downloadSizeOptions" :key="size" :value="size">
                      {{ size }} px
                    </SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </CardHeader>
            <CardContent class="flex flex-1 min-h-0 flex-col gap-3 px-4 pb-3 pt-0">
              <div
                ref="previewAreaRef"
                class="flex flex-1 min-h-0 items-center justify-center overflow-hidden rounded-2xl border bg-card/80 p-3"
              >
                <div ref="previewContainerRef" class="w-full max-w-[420px]">
                  <div class="mx-auto" :style="previewBoxStyle">
                    <div ref="previewRef" class="h-full w-full" aria-live="polite" />
                  </div>
                </div>
              </div>
              <div class="shrink-0 space-y-2">
                <div class="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                  <span>数据 {{ dataLength }} B</span>
                  <span>·</span>
                  <span>容错 {{ form.qrOptions.errorCorrectionLevel }}</span>
                  <span>·</span>
                  <span>预览 {{ previewSize }} px</span>
                  <span v-if="isDownloading">· 导出准备中…</span>
                </div>
                <div class="flex flex-wrap items-center gap-2">
                  <span class="text-xs text-muted-foreground">导出格式</span>
                  <div class="flex flex-wrap gap-2">
                    <Button
                      v-for="ext in fileExtensions"
                      :key="ext"
                      size="sm"
                      variant="outline"
                      :disabled="isDownloading"
                      type="button"
                      @click="handleDownload(ext)"
                    >{{ ext.toUpperCase() }}</Button>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </main>
    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0 scale-95 translate-y-2"
        enter-to-class="opacity-100 scale-100 translate-y-0"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100 scale-100 translate-y-0"
        leave-to-class="opacity-0 scale-95 translate-y-2"
      >
        <div
          v-if="shouldRenderFloatingPreview"
          class="pointer-events-auto fixed z-50 rounded-2xl border border-border/60 bg-card/95 p-2.5 shadow-xl backdrop-blur"
          :style="floatingPreviewStyle"
          @pointerdown="handleFloatingPointerDown"
        >
          <div class="flex items-center justify-between pb-1 text-[10px] uppercase tracking-[0.08em] text-muted-foreground/80">
            <span>实时预览</span>
            <button
              type="button"
              class="font-semibold text-primary/90 hover:text-primary"
              @click.stop.prevent="resetFloatingPreviewPosition"
              @pointerdown.stop
            >
              复位
            </button>
          </div>
          <div class="rounded-xl border border-border/70 bg-background/90 p-1.5 shadow-sm">
            <div class="flex items-center justify-center">
              <div class="overflow-hidden rounded-lg" :style="floatingPreviewViewportStyle">
                <div
                  ref="floatingPreviewRef"
                  class="h-full w-full"
                  aria-hidden="true"
                />
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
