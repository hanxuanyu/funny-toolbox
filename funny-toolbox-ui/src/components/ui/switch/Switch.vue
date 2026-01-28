<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  id: {
    type: String,
    default: undefined,
  },
  name: {
    type: String,
    default: undefined,
  },
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
}>();

const isChecked = computed(() => props.modelValue);

const toggle = () => {
  if (props.disabled) return;
  emit('update:modelValue', !props.modelValue);
};
</script>

<template>
  <button
    type="button"
    :id="id"
    :name="name"
    role="switch"
    :aria-checked="isChecked"
    :disabled="disabled"
    @click="toggle"
    @keydown.enter.prevent="toggle"
    @keydown.space.prevent="toggle"
    class="relative inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border border-transparent transition-colors duration-200 ease-in-out focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
    :class="isChecked ? 'bg-blue-600' : 'bg-gray-300'"
  >
    <span
      aria-hidden="true"
      class="inline-block h-4 w-4 transform rounded-full bg-white shadow transition duration-200"
      :class="isChecked ? 'translate-x-4' : 'translate-x-0'"
    ></span>
  </button>
</template>
