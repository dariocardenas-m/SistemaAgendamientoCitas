<template>
  <div class="card mb-6 grid grid-cols-5 gap-2 p-3">
    <button
      v-for="s in steps"
      :key="s.n"
      type="button"
      class="rounded-2xl px-2 py-3 text-center transition"
      :class="props.current === s.n ? 'bg-brand-light ring-1 ring-brand' : 'text-slate-400'"
      @click="$emit('goto', s.n)"
    >
      <div
        class="mx-auto mb-1 flex h-8 w-8 items-center justify-center rounded-full text-sm font-bold"
        :class="props.current >= s.n ? 'bg-brand text-white' : 'bg-slate-100 text-slate-400'"
      >
        <span v-if="props.current > s.n">✓</span>
        <span v-else>{{ s.n }}</span>
      </div>
      <p class="text-xs font-semibold">{{ s.title }}</p>
      <p class="text-[10px] text-slate-400">{{ s.sub }}</p>
    </button>
  </div>
</template>

<script setup>
const props = defineProps({
  current: { type: Number, required: true }
})
defineEmits(['goto'])

const steps = [
  { n: 1, title: 'Especialidad', sub: 'Área Médica' },
  { n: 2, title: 'Profesional', sub: 'Médico / Terapista' },
  { n: 3, title: 'Fecha', sub: 'Calendario Hábil' },
  { n: 4, title: 'Horario', sub: 'Franjas Libres' },
  { n: 5, title: 'Resumen', sub: 'Confirmación' }
]
</script>
