<template>
  <div class="mx-auto max-w-6xl px-4 py-8">
    <div class="card mb-6 p-6">
      <span class="rounded-full bg-violet-50 px-3 py-1 text-xs font-semibold text-violet-700">Administración del sistema</span>
      <h1 class="mt-3 text-2xl font-extrabold">Configuración de Agendamiento</h1>
      <p class="text-sm text-slate-500">Defina la ventana de anticipación y la disponibilidad de cada médico o terapista. Estos parámetros alimentan las franjas del agendamiento autónomo.</p>
    </div>

    <div class="card mb-6 p-6">
      <h2 class="font-bold">Ventana de tiempo global</h2>
      <p class="mb-4 text-sm text-slate-500">Cantidad de semanas hacia adelante en las que los pacientes pueden reservar (1 a 12).</p>
      <div class="flex items-center gap-4">
        <input v-model.number="ventana" type="range" min="1" max="12" class="flex-1" />
        <span class="w-24 rounded-2xl bg-brand-light py-2 text-center font-bold text-brand">{{ ventana }} sem.</span>
        <button class="btn-primary" @click="guardarVentana">Guardar</button>
      </div>
      <p v-if="msgVentana" class="mt-2 text-sm text-brand">{{ msgVentana }}</p>
    </div>

    <div class="card overflow-x-auto p-6">
      <h2 class="mb-4 font-bold">Parametrización por profesional</h2>
      <table class="min-w-full text-left text-sm">
        <thead class="text-xs uppercase text-slate-400">
          <tr>
            <th class="px-2 py-2">Profesional</th>
            <th class="px-2 py-2">Días de atención</th>
            <th class="px-2 py-2">Apertura</th>
            <th class="px-2 py-2">Cierre</th>
            <th class="px-2 py-2">Intervalo</th>
            <th class="px-2 py-2"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in medicos" :key="m.id" class="border-t align-top">
            <td class="px-2 py-3">
              <p class="font-semibold">{{ m.nombreCompleto }}</p>
              <p class="text-xs text-slate-400">{{ m.especialidad }}</p>
            </td>
            <td class="px-2 py-3">
              <label v-for="d in dias" :key="d.id" class="mr-2 inline-flex items-center gap-1 text-xs">
                <input type="checkbox" :checked="m.diasAtencion.includes(d.id)" @change="toggleDia(m, d.id)" />
                {{ d.label }}
              </label>
            </td>
            <td class="px-2 py-3"><input v-model="m.horaApertura" type="time" class="input-field !py-2" /></td>
            <td class="px-2 py-3"><input v-model="m.horaCierre" type="time" class="input-field !py-2" /></td>
            <td class="px-2 py-3">
              <select v-model.number="m.intervaloMinutos" class="input-field !py-2">
                <option :value="15">15 min</option>
                <option :value="30">30 min</option>
                <option :value="45">45 min</option>
                <option :value="60">60 min</option>
              </select>
            </td>
            <td class="px-2 py-3">
              <button class="text-sm font-semibold text-brand" @click="guardarMedico(m)">Aplicar</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="msgMedico" class="mt-3 text-sm text-brand">{{ msgMedico }}</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'

const ventana = ref(8)
const medicos = ref([])
const msgVentana = ref('')
const msgMedico = ref('')
const dias = [
  { id: 'LUN', label: 'Lun' },
  { id: 'MAR', label: 'Mar' },
  { id: 'MIE', label: 'Mié' },
  { id: 'JUE', label: 'Jue' },
  { id: 'VIE', label: 'Vie' },
  { id: 'SAB', label: 'Sáb' },
  { id: 'DOM', label: 'Dom' }
]

onMounted(async () => {
  const cfg = await api.get('/configuracion')
  ventana.value = cfg.data.ventanaSemanas
  const { data } = await api.get('/medicos')
  medicos.value = data.map(m => ({
    ...m,
    horaApertura: (m.horaApertura || '08:00:00').slice(0, 5),
    horaCierre: (m.horaCierre || '12:00:00').slice(0, 5)
  }))
})

function toggleDia(m, id) {
  if (m.diasAtencion.includes(id)) {
    m.diasAtencion = m.diasAtencion.filter(d => d !== id)
  } else {
    m.diasAtencion = [...m.diasAtencion, id]
  }
}

async function guardarVentana() {
  await api.put('/configuracion', { ventanaSemanas: ventana.value })
  msgVentana.value = 'Ventana de tiempo actualizada correctamente.'
}

async function guardarMedico(m) {
  await api.put(`/medicos/${m.id}/configuracion`, {
    diasAtencion: m.diasAtencion,
    horaApertura: m.horaApertura.length === 5 ? m.horaApertura + ':00' : m.horaApertura,
    horaCierre: m.horaCierre.length === 5 ? m.horaCierre + ':00' : m.horaCierre,
    intervaloMinutos: m.intervaloMinutos
  })
  msgMedico.value = `Disponibilidad de ${m.nombreCompleto} aplicada a la generación de franjas.`
}
</script>
