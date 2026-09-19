<template>
  <div class="mx-auto max-w-7xl px-4 py-8">
    <div class="card mb-6 flex items-start justify-between p-6">
      <div>
        <span class="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">Gestión operativa de citas</span>
        <h1 class="mt-3 text-2xl font-extrabold">Consulta de Citas Programadas</h1>
        <p class="text-sm text-slate-500">Módulo del Agendador para verificación de agendas médicas, pacientes citados y ocupación diaria.</p>
      </div>
      <span class="rounded-full border px-3 py-1 text-xs font-semibold">Rol Activo: Agendador de Citas</span>
    </div>

    <div class="card mb-6 p-5">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
        <p class="text-xs font-bold uppercase text-slate-400">Criterios de consulta de agenda</p>
        <div class="flex gap-2 text-xs">
          <button class="rounded-full bg-slate-100 px-3 py-1" @click="atajoLaura">Dra. Laura (26 Ago)</button>
          <button class="rounded-full bg-slate-100 px-3 py-1" @click="atajoCarlos">Dr. Carlos (27 Ago)</button>
        </div>
      </div>
      <div class="grid gap-3 md:grid-cols-[1fr_220px_auto]">
        <select v-model="medicoId" class="input-field">
          <option disabled value="">Profesional médico o terapista *</option>
          <option v-for="m in medicos" :key="m.id" :value="m.id">{{ m.nombreCompleto }} — {{ m.especialidad }}</option>
        </select>
        <input v-model="fecha" type="date" class="input-field" />
        <button class="btn-primary !bg-blue-600 hover:!bg-blue-700" @click="consultar">Consultar</button>
      </div>
    </div>

    <div v-if="consultado" class="mb-6 grid gap-4 md:grid-cols-4">
      <MetricCard label="Total de citas" :value="citas.length" hint="Citas registradas" value-class="text-blue-700" />
      <MetricCard label="Confirmadas" :value="conteo('CONFIRMADA')" hint="Turnos activos" value-class="text-brand" />
      <MetricCard label="Canceladas" :value="conteo('CANCELADA')" hint="Liberadas" value-class="text-rose-500" />
      <div class="card flex items-center gap-3 p-5">
        <div class="flex h-12 w-12 items-center justify-center rounded-full bg-brand-light font-bold text-brand">
          {{ profesional?.nombreCompleto?.split(' ').pop()?.[0] }}
        </div>
        <div>
          <p class="text-xs text-slate-400">Profesional</p>
          <p class="font-bold">{{ profesional?.nombreCompleto }}</p>
          <p class="text-xs text-slate-500">{{ profesional?.especialidad }}</p>
        </div>
      </div>
    </div>

    <div v-if="consultado" class="card overflow-x-auto p-4">
      <div class="mb-3 flex items-center justify-between">
        <div>
          <h3 class="font-bold">Citas programadas</h3>
          <p class="text-xs text-slate-500">Fecha: {{ fecha }} · Profesional: {{ profesional?.nombreCompleto }}</p>
        </div>
        <div class="flex gap-2">
          <input v-model="busqueda" class="input-field !py-2" placeholder="Buscar paciente o cédula..." />
          <select v-model="filtroEstado" class="input-field !py-2">
            <option value="">Todos los estados</option>
            <option value="CONFIRMADA">Confirmada</option>
            <option value="CANCELADA">Cancelada</option>
            <option value="ATENDIDA">Atendida</option>
          </select>
        </div>
      </div>
      <table class="min-w-full text-left text-sm">
        <thead class="text-xs uppercase text-slate-400">
          <tr>
            <th class="px-3 py-2">Hora</th>
            <th class="px-3 py-2">Paciente</th>
            <th class="px-3 py-2">Documento</th>
            <th class="px-3 py-2">Teléfono</th>
            <th class="px-3 py-2">Motivo / Notas</th>
            <th class="px-3 py-2">Estado</th>
            <th class="px-3 py-2">Acción operativa</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in visibles" :key="c.id" class="border-t">
            <td class="px-3 py-3 font-bold">{{ c.hora?.slice(0,5) }}</td>
            <td class="px-3 py-3">
              <p class="font-semibold">{{ c.pacienteNombre }}</p>
              <p class="text-[11px] text-slate-400">{{ c.codigo }}</p>
            </td>
            <td class="px-3 py-3">{{ c.pacienteDocumento }}</td>
            <td class="px-3 py-3">{{ c.pacienteTelefono }}</td>
            <td class="px-3 py-3">{{ c.motivo }}</td>
            <td class="px-3 py-3">
              <span class="rounded-full px-2 py-1 text-xs font-semibold" :class="c.estado==='CANCELADA' ? 'bg-rose-50 text-rose-600' : 'bg-emerald-50 text-brand'">{{ etiqueta(c.estado) }}</span>
            </td>
            <td class="px-3 py-3">
              <template v-if="c.estado==='CONFIRMADA'">
                <button class="mr-3 text-xs font-semibold text-brand" @click="accion(c, 'atender')">Atendido</button>
                <button class="text-xs font-semibold text-rose-500" @click="accion(c, 'cancelar')">Cancelar</button>
              </template>
              <button v-else-if="c.estado==='CANCELADA'" class="text-xs font-semibold text-slate-500" @click="accion(c, 'reactivar')">Reactivar</button>
              <span v-else class="text-xs text-slate-400">—</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import MetricCard from '../components/MetricCard.vue'

const medicos = ref([])
const medicoId = ref('')
const fecha = ref('')
const citas = ref([])
const consultado = ref(false)
const busqueda = ref('')
const filtroEstado = ref('')

const profesional = computed(() => medicos.value.find(m => m.id === Number(medicoId.value)))
const visibles = computed(() => citas.value.filter(c => {
  const q = busqueda.value.toLowerCase()
  const match = !q || c.pacienteNombre.toLowerCase().includes(q) || c.pacienteDocumento.includes(q) || c.codigo.toLowerCase().includes(q)
  const est = !filtroEstado.value || c.estado === filtroEstado.value
  return match && est
}))

onMounted(async () => {
  const { data } = await api.get('/medicos')
  medicos.value = data
})

function conteo(est) {
  return citas.value.filter(c => c.estado === est).length
}
function etiqueta(e) {
  return ({ CONFIRMADA: 'Confirmada', CANCELADA: 'Cancelada', ATENDIDA: 'Atendida' })[e] || e
}

async function consultar() {
  if (!medicoId.value || !fecha.value) return
  const { data } = await api.get('/citas/consulta', { params: { medicoId: medicoId.value, fecha: fecha.value } })
  citas.value = data
  consultado.value = true
}

function atajoLaura() {
  const m = medicos.value.find(x => x.nombreCompleto.includes('Laura'))
  if (m) medicoId.value = m.id
  fecha.value = '2026-08-26'
  consultar()
}
function atajoCarlos() {
  const m = medicos.value.find(x => x.nombreCompleto.includes('Carlos Gómez'))
  if (m) medicoId.value = m.id
  fecha.value = '2026-08-27'
  consultar()
}

async function accion(c, tipo) {
  await api.patch(`/citas/${c.id}/${tipo}`)
  await consultar()
}
</script>
