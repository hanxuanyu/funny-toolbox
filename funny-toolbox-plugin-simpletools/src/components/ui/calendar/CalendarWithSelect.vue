<script lang="ts" setup>
import type { CalendarRootEmits, CalendarRootProps, DateValue } from "reka-ui"
import type { HTMLAttributes, Ref } from "vue"
import type { LayoutTypes } from "."
import { getLocalTimeZone, today } from "@internationalized/date"
import { createReusableTemplate, reactiveOmit, useVModel } from "@vueuse/core"
import { CalendarRoot, useDateFormatter, useForwardPropsEmits } from "reka-ui"
import { createYear, createYearRange, toDate } from "reka-ui/date"
import { computed, toRaw, ref, watch } from "vue"
import { cn } from "@/lib/utils"
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { CalendarCell, CalendarCellTrigger, CalendarGrid, CalendarGridBody, CalendarGridHead, CalendarGridRow, CalendarHeadCell, CalendarHeader, CalendarHeading, CalendarNextButton, CalendarPrevButton } from "."

const props = withDefaults(defineProps<CalendarRootProps & { class?: HTMLAttributes["class"], layout?: LayoutTypes, yearRange?: DateValue[] }>(), {
  modelValue: undefined,
  layout: undefined,
})
const emits = defineEmits<CalendarRootEmits>()

const delegatedProps = reactiveOmit(props, "class", "layout", "placeholder")

const placeholder = useVModel(props, "placeholder", emits, {
  passive: true,
  defaultValue: props.defaultPlaceholder ?? today(getLocalTimeZone()),
}) as Ref<DateValue>

const formatter = useDateFormatter(props.locale ?? "en")

const yearRange = computed(() => {
  return props.yearRange ?? createYearRange({
    start: props?.minValue ?? (toRaw(props.placeholder) ?? props.defaultPlaceholder ?? today(getLocalTimeZone()))
      .cycle("year", -100),

    end: props?.maxValue ?? (toRaw(props.placeholder) ?? props.defaultPlaceholder ?? today(getLocalTimeZone()))
      .cycle("year", 10),
  })
})

// 用于 Select 的本地状态
const selectedMonth = ref<string>('')
const selectedYear = ref<string>('')

// 监听 placeholder 变化，更新 Select 的值
watch(() => placeholder.value, (newVal) => {
  if (newVal) {
    selectedMonth.value = String(newVal.month)
    selectedYear.value = String(newVal.year)
  }
}, { immediate: true })

const [DefineMonthTemplate, ReuseMonthTemplate] = createReusableTemplate<{ date: DateValue }>()
const [DefineYearTemplate, ReuseYearTemplate] = createReusableTemplate<{ date: DateValue }>()

const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<template>
  <DefineMonthTemplate v-slot="{ date }">
    <Select
      v-model="selectedMonth"
      @update:model-value="(value) => {
        placeholder = placeholder.set({
          month: Number(value),
        })
      }"
    >
      <SelectTrigger class="h-6 text-xs min-w-[90px] px-2">
        <SelectValue placeholder="月份" />
      </SelectTrigger>
      <SelectContent class="z-[100]" :side-offset="5">
        <SelectGroup>
          <SelectItem 
            v-for="(month) in createYear({ dateObj: date })" 
            :key="month.toString()" 
            :value="String(month.month)"
          >
            {{ formatter.custom(toDate(month), { month: 'short' }) }}
          </SelectItem>
        </SelectGroup>
      </SelectContent>
    </Select>
  </DefineMonthTemplate>

  <DefineYearTemplate v-slot="{ date }">
    <Select
      v-model="selectedYear"
      @update:model-value="(value) => {
        placeholder = placeholder.set({
          year: Number(value),
        })
      }"
    >
      <SelectTrigger class="h-6 text-xs min-w-[80px] px-2">
        <SelectValue placeholder="年份" />
      </SelectTrigger>
      <SelectContent class="z-[100]" :side-offset="5">
        <SelectGroup>
          <SelectItem 
            v-for="(year) in yearRange" 
            :key="year.toString()" 
            :value="String(year.year)"
          >
            {{ formatter.custom(toDate(year), { year: 'numeric' }) }}
          </SelectItem>
        </SelectGroup>
      </SelectContent>
    </Select>
  </DefineYearTemplate>

  <CalendarRoot
    v-slot="{ grid, weekDays, date }"
    v-bind="forwarded"
    v-model:placeholder="placeholder"
    data-slot="calendar"
    :class="cn('p-3 flex flex-col items-center', props.class)"
  >
    <CalendarHeader class="pt-0">
      <slot name="calendar-heading" :date="date" :month="ReuseMonthTemplate" :year="ReuseYearTemplate">
        <template v-if="layout === 'month-and-year'">
          <div class="flex items-center justify-between gap-2">
            <CalendarPrevButton>
              <slot name="calendar-prev-icon" />
            </CalendarPrevButton>
            <div class="flex items-center gap-1">
              <ReuseMonthTemplate :date="date" />
              <ReuseYearTemplate :date="date" />
            </div>
            <CalendarNextButton>
              <slot name="calendar-next-icon" />
            </CalendarNextButton>
          </div>
        </template>
        <template v-else-if="layout === 'month-only'">
          <div class="flex items-center justify-between gap-2">
            <CalendarPrevButton>
              <slot name="calendar-prev-icon" />
            </CalendarPrevButton>
            <div class="flex items-center gap-1">
              <ReuseMonthTemplate :date="date" />
              {{ formatter.custom(toDate(date), { year: 'numeric' }) }}
            </div>
            <CalendarNextButton>
              <slot name="calendar-next-icon" />
            </CalendarNextButton>
          </div>
        </template>
        <template v-else-if="layout === 'year-only'">
          <div class="flex items-center justify-between gap-2">
            <CalendarPrevButton>
              <slot name="calendar-prev-icon" />
            </CalendarPrevButton>
            <div class="flex items-center gap-1">
              {{ formatter.custom(toDate(date), { month: 'short' }) }}
              <ReuseYearTemplate :date="date" />
            </div>
            <CalendarNextButton>
              <slot name="calendar-next-icon" />
            </CalendarNextButton>
          </div>
        </template>
        <template v-else>
          <div class="flex items-center justify-between gap-2">
            <CalendarPrevButton>
              <slot name="calendar-prev-icon" />
            </CalendarPrevButton>
            <CalendarHeading />
            <CalendarNextButton>
              <slot name="calendar-next-icon" />
            </CalendarNextButton>
          </div>
        </template>
      </slot>
    </CalendarHeader>

    <div class="flex flex-col gap-y-4 mt-4 sm:flex-row sm:gap-x-4 sm:gap-y-0">
      <CalendarGrid v-for="month in grid" :key="month.value.toString()">
        <CalendarGridHead>
          <CalendarGridRow>
            <CalendarHeadCell
              v-for="day in weekDays" :key="day"
            >
              {{ day }}
            </CalendarHeadCell>
          </CalendarGridRow>
        </CalendarGridHead>
        <CalendarGridBody>
          <CalendarGridRow v-for="(weekDates, index) in month.rows" :key="`weekDate-${index}`" class="mt-2">
            <CalendarCell
              v-for="weekDate in weekDates"
              :key="weekDate.toString()"
              :date="weekDate"
            >
              <CalendarCellTrigger
                :day="weekDate"
                :month="month.value"
              />
            </CalendarCell>
          </CalendarGridRow>
        </CalendarGridBody>
      </CalendarGrid>
    </div>
  </CalendarRoot>
</template>
