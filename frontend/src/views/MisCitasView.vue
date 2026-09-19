<template>
  <div class="mx-auto max-w-6xl px-4 py-8">
    <div class="card mb-6 flex flex-col gap-4 p-6 md:flex-row md:items-center md:justify-between">
      <div>
        <h1 class="text-2xl font-extrabold text-brand">Mis Citas Médicas</h1>
        <p class="text-sm text-slate-500">Historial y gestión de todas tus consultas programadas en el sistema.</p>
      </div>
      <router-link to="/agendar" class="btn-primary">Agendar Nueva Cita</router-link>
    </div>

    <div class="card mb-6 p-5">
      <div class="mb-4 flex items-center justify-between">
        <p class="text-xs font-bold uppercase tracking-wide text-slate-400">Filtros de búsqueda</p>
        <div class="flex rounded-full bg-slate-100 p-1 text-xs font-semibold">
          <button class="rounded-full px-3 py-1" :class="vista==='tarjetas' && 'bg-white shadow'" @click="vista='tarjetas'">Tarjetas</button>
          <button class="rounded-full px-3 py-1" :class="vista==='tabla' && 'bg-white shadow'" @click="vista='tabla'">Tabla</button>
        </div>
      </div>
      <div class="grid gap-3 md:grid-cols-3">
        <select v-model="filtroEstado" class="input-field">
          <option value="">Todos los estados</option>
          <option value="CONFIRMADA">Confirmada</option>
          <option value="CANCELADA">Cancelada</option>
          <option value="ATENDIDA">Atendida</option>
        </select>
        <select v-model="filtroEsp" class="input-field">
          <option value="">Todas las especialidades</option>
          <option v-for="e in especialidades" :key="e" :value="e">{{ e }}</option>
        </select>
        <input v-model="filtroFecha" type="date" class="input-field" />
      </div>
    </div>

    <div v-if="vista==='tarjetas'" class="grid gap-4 md:grid-cols-2">
      <article v-for="c in filtradas" :key="c.id" class="card p-5">
        <div class="mb-3 flex items-center justify-between">
          <span class="text-xs font-bold text-slate-400">{{ c.codigo }}</span>
          <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="badge(c.estado)">{{ etiqueta(c.estado) }}</span>
        </div>
        <p class="text-xs font-bold uppercase text-brand">{{ c.especialidad }}</p>
        <p class="text-lg font-extrabold">{{ c.medicoNombre }}</p>
        <p class="text-xs text-slate-500">Modalidad: {{ c.modalidad === 'TELEMEDICINA' ? 'Telemedicina' : 'Presencial' }}</p>
        <div class="mt-3 grid grid-cols-2 gap-2 rounded-xl bg-slate-50 p-3 text-sm">
          <div><p class="text-xs text-slate-400">Fecha</p>{{ formatear(c.fecha) }}</div>
          <div><p class="text-xs text-slate-400">Hora</p>{{ c.hora?.slice(0,5) }} ({{ c.duracionMinutos }} min)</div>
        </div>
        <p class="mt-3 rounded-xl border border-brand/20 px-3 py-2 text-sm">Motivo: {{ c.motivo || 'Sin notas' }}</p>
        <div class="mt-4 flex justify-between text-sm">
          <button class="font-semibold text-brand" @click="seleccionada = c">Ver Comprobante</button>
          <button v-if="c.estado==='CONFIRMADA'" class="font-semibold text-rose-500" @click="cancelar(c)">Cancelar Cita</button>
        </div>
      </article>
    </div>

    <div v-else class="card overflow-x-auto">
      <table class="min-w-full text-left text-sm">
        <thead class="bg-slate-50 text-xs uppercase text-slate-400">
          <tr>
            <th class="px-4 py-3">Código</th>
            <th class="px-4 py-3">Especialidad</th>
            <th class="px-4 py-3">Médico</th>
            <th class="px-4 py-3">Fecha</th>
            <th class="px-4 py-3">Hora</th>
            <th class="px-4 py-3">Estado</th>
            <th class="px-4 py-3"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in filtradas" :key="c.id" class="border-t">
            <td class="px-4 py-3">{{ c.codigo }}</td>
            <td class="px-4 py-3">{{ c.especialidad }}</td>
            <td class="px-4 py-3">{{ c.medicoNombre }}</td>
            <td class="px-4 py-3">{{ c.fecha }}</td>
            <td class="px-4 py-3">{{ c.hora?.slice(0,5) }}</td>
            <td class="px-4 py-3"><span class="rounded-full px-2 py-1 text-xs" :class="badge(c.estado)">{{ etiqueta(c.estado) }}</span></td>
            <td class="px-4 py-3 text-right">
              <button class="mr-3 text-brand" @click="seleccionada = c">Comprobante</button>
              <button v-if="c.estado==='CONFIRMADA'" class="text-rose-500" @click="cancelar(c)">Cancelar</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <ComprobanteModal :cita="seleccionada" @ver-citas="seleccionada=null" @inicio="seleccionada=null" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import { useCitasStore } from '../stores/citasStore'
import ComprobanteModal from '../components/ComprobanteModal.vue'

const store = useCitasStore()
const vista = ref('tarjetas')
const filtroEstado = ref('')
const filtroEsp = ref('')
const filtroFecha = ref('')
const seleccionada = ref(null)

onMounted(() => store.cargarMisCitas())

const especialidades = computed(() => [...new Set(store.misCitas.map(c => c.especialidad))])

const filtradas = computed(() => store.misCitas.filter(c => {
  if (filtroEstado.value && c.estado !== filtroEstado.value) return false
  if (filtroEsp.value && c.especialidad !== filtroEsp.value) return false
  if (filtroFecha.value && c.fecha !== filtroFecha.value) return false
  return true
}))

function badge(e) {
  if (e === 'CANCELADA') return 'bg-rose-50 text-rose-600'
  if (e === 'ATENDIDA') return 'bg-slate-100 text-slate-600'
  return 'bg-emerald-50 text-brand'
}
function etiqueta(e) {
  return ({ CONFIRMADA: 'Confirmada', CANCELADA: 'Cancelada', ATENDIDA: 'Atendida' })[e] || e
}
function formatear(f) {
  return new Date(f + 'T00:00:00').toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' })
}
async function cancelar(c) {
  if (!confirm('¿Desea cancelar esta cita?')) return
  await api.patch(`/citas/${c.id}/cancelar`)
  await store.cargarMisCitas()
}
</script>
