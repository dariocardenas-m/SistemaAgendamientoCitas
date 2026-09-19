<template>
  <div class="rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-400">{{ label }}</p>
    <p class="mt-2 text-3xl font-bold" :class="valueClass">{{ value }}</p>
    <p class="mt-1 text-xs text-slate-500">{{ hint }}</p>
  </div>
</template>

<script setup>
defineProps({
  label: String,
  value: [String, Number],
  hint: String,
  valueClass: { type: String, default: 'text-slate-900' }
})
</script>
