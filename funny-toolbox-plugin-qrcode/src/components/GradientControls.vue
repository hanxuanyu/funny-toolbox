<script setup lang="ts">
import { computed } from "vue"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { Switch } from "@/components/ui/switch"
import type { GradientForm, GradientStop } from "@/types/qr"

const gradient = defineModel<GradientForm>({ required: true })

const props = withDefaults(defineProps<{ title: string; description?: string }>(), {
  description: "",
})

const canRemove = computed(() => gradient.value.colorStops.length > 2)

const addStop = () => {
  const last = gradient.value.colorStops[gradient.value.colorStops.length - 1]
  const nextOffset = Math.min(100, Math.max(0, last?.offset ?? 100))
  gradient.value.colorStops.push({
    id: cryptoId(),
    offset: nextOffset,
    color: "#ffffff",
  })
}

const removeStop = (stop: GradientStop) => {
  if (!canRemove.value)
    return
  const index = gradient.value.colorStops.findIndex((item) => item.id === stop.id)
  if (index >= 0)
    gradient.value.colorStops.splice(index, 1)
}

const clampRotation = () => {
  gradient.value.rotation = clamp(gradient.value.rotation, 0, 360)
}

const clampStop = (stop: GradientStop) => {
  stop.offset = clamp(stop.offset, 0, 100)
}

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

const cryptoId = () => Math.random().toString(36).slice(2, 10)
</script>

<template>
  <div class="space-y-3">
    <div class="flex items-center justify-between gap-4">
      <div>
        <p class="text-sm font-medium">{{ props.title }}</p>
        <p class="text-xs text-muted-foreground" v-if="props.description">
          {{ props.description }}
        </p>
      </div>
      <div class="flex items-center gap-2 text-xs text-muted-foreground">
        <span>关闭</span>
        <Switch v-model="gradient.enabled" aria-label="切换渐变" />
        <span>开启</span>
      </div>
    </div>

    <div v-if="gradient.enabled" class="rounded-lg border bg-muted/40 p-3 space-y-4">
      <div class="grid gap-3 sm:grid-cols-2">
        <Label class="text-xs uppercase tracking-wide text-muted-foreground">渐变类型</Label>
        <Select v-model="gradient.type">
          <SelectTrigger class="w-full">
            <SelectValue placeholder="选择类型" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="linear">线性 (linear)</SelectItem>
            <SelectItem value="radial">径向 (radial)</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <div class="grid gap-3 sm:grid-cols-2">
        <Label class="text-xs uppercase tracking-wide text-muted-foreground">旋转角度 (°)</Label>
        <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:gap-3 min-w-0">
          <input
            type="range"
            min="0"
            max="360"
            step="5"
            v-model.number="gradient.rotation"
            class="h-2 w-full flex-1 cursor-pointer accent-primary"
            aria-label="渐变角度滑动条"
          >
          <Input
            v-model.number="gradient.rotation"
            type="number"
            min="0"
            max="360"
            step="5"
            class="w-full min-w-0 text-right sm:w-20"
            @change="clampRotation"
          />
        </div>
      </div>
      <Separator />
      <div class="space-y-3">
        <div class="flex items-center justify-between text-xs text-muted-foreground">
          <span>颜色节点</span>
          <Button
            size="sm"
            variant="outline"
            @click="addStop"
            type="button"
          >添加节点</Button>
        </div>
        <div class="flex flex-col gap-3">
          <div
            v-for="(stop, index) in gradient.colorStops"
            :key="stop.id"
            class="grid gap-2 rounded-md border bg-background/60 p-3 sm:grid-cols-[auto_1fr_auto] sm:items-center"
          >
            <div class="flex items-center gap-3">
              <input
                type="color"
                v-model="stop.color"
                class="h-10 w-10 cursor-pointer rounded-md border bg-transparent p-0"
                aria-label="选择颜色"
              >
              <div>
                <p class="text-xs font-medium text-muted-foreground">节点 {{ index + 1 }}</p>
                <p class="text-[11px] text-muted-foreground/80">偏移 {{ stop.offset }}%</p>
              </div>
            </div>
            <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:gap-3 min-w-0">
              <input
                type="range"
                min="0"
                max="100"
                step="1"
                v-model.number="stop.offset"
                class="h-2 w-full flex-1 cursor-pointer accent-primary"
                aria-label="颜色节点偏移滑动条"
              >
              <Input
                v-model.number="stop.offset"
                type="number"
                min="0"
                max="100"
                step="1"
                class="w-full min-w-0 text-right sm:w-20"
                @change="clampStop(stop)"
              />
            </div>
            <Button
              v-if="canRemove"
              size="sm"
              variant="ghost"
              class="justify-self-end text-xs text-destructive"
              type="button"
              @click="removeStop(stop)"
            >删除</Button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
