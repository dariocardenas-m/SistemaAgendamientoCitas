<template>
  <div class="mx-auto max-w-7xl px-4 py-8">
    <div class="card mb-6 flex items-start justify-between p-6">
      <div>
        <span class="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">Gestión operativa de citas</span>
        <h1 class="mt-3 text-2xl font-extrabold">Consulta de Citas Programadas</h1>
        <p class="text-sm text-slate-500">Módulo del Agendador para verificación de agendas médicas, pacientes citados y ocupación diaria.</p>
      </div>
      <span class="rounded-full border px-3 py-1 text-xs font-semibold">Rol activo: {{ etiquetaRol }}</span>
    </div>

    <div class="card mb-6 p-5">
      <p class="mb-4 text-xs font-bold uppercase text-slate-400">Criterios de consulta de agenda</p>
      <div class="grid gap-3 md:grid-cols-[1fr_220px_auto]">
        <select v-model="medicoId" class="input-field" :disabled="esMedico">
          <option disabled value="">Profesional médico o terapista *</option>
          <option v-for="m in medicos" :key="m.id" :value="m.id">{{ m.nombreCompleto }} — {{ m.especialidad }}</option>
        </select>
        <input v-model="fecha" type="date" class="input-field" />
        <button class="btn-primary !bg-blue-600 hover:!bg-blue-700" @click="consultar">Consultar</button>
      </div>
      <p v-if="error" class="mt-3 text-sm text-rose-600">{{ error }}</p>
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
          <button class="btn-ghost !py-2 !text-xs" @click="exportar">Exportar CSV</button>
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
                <button class="mr-3 text-xs font-semibold text-blue-600" @click="editar = c">Editar</button>
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

    <div class="card mt-6 p-5">
      <div class="mb-4">
        <h2 class="font-bold">Historial de citas por cédula</h2>
        <p class="text-sm text-slate-500">Consulta el historial autorizado del paciente sin exponer datos clínicos o de contacto innecesarios.</p>
      </div>
      <form class="grid gap-3 md:grid-cols-[1fr_auto]" @submit.prevent="consultarHistorial">
        <input v-model.trim="cedula" class="input-field" inputmode="numeric" maxlength="20" pattern="[0-9]{6,20}" placeholder="Cédula del paciente" required />
        <button class="btn-primary !bg-blue-600 hover:!bg-blue-700" :disabled="consultandoHistorial">Consultar historial</button>
      </form>
      <p v-if="errorHistorial" class="mt-3 text-sm text-rose-600">{{ errorHistorial }}</p>
      <div v-if="historialConsultado" class="mt-5 overflow-x-auto">
        <p v-if="historial.length === 0" class="text-sm text-slate-500">No hay citas registradas para esta cédula.</p>
        <table v-else class="min-w-full text-left text-sm">
          <thead class="text-xs uppercase text-slate-400">
            <tr><th class="px-3 py-2">Fecha</th><th class="px-3 py-2">Profesional</th><th class="px-3 py-2">Especialidad</th><th class="px-3 py-2">Estado</th></tr>
          </thead>
          <tbody>
            <tr v-for="(cita, index) in historial" :key="`${cita.fecha}-${cita.medico}-${index}`" class="border-t">
              <td class="px-3 py-3">{{ cita.fecha }}</td><td class="px-3 py-3 font-semibold">{{ cita.medico }}</td><td class="px-3 py-3">{{ cita.especialidad }}</td><td class="px-3 py-3">{{ etiqueta(cita.estado) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <EditarCitaModal
      :cita="editar"
      permitir-cambiar-medico
      @cerrar="editar = null"
      @guardada="onEditada"
    />
    <ComprobanteModal
      :cita="comprobante"
      actualizada
      @ver-citas="comprobante = null"
      @inicio="comprobante = null"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import MetricCard from '../components/MetricCard.vue'
import EditarCitaModal from '../components/EditarCitaModal.vue'
import ComprobanteModal from '../components/ComprobanteModal.vue'
import { useAuthStore } from '../stores/authStore'

const auth = useAuthStore()
const medicos = ref([])
const medicoId = ref('')
const fecha = ref('')
const citas = ref([])
const consultado = ref(false)
const busqueda = ref('')
const filtroEstado = ref('')
const editar = ref(null)
const comprobante = ref(null)
const error = ref('')
const cedula = ref('')
const historial = ref([])
const historialConsultado = ref(false)
const consultandoHistorial = ref(false)
const errorHistorial = ref('')

const profesional = computed(() => medicos.value.find(m => m.id === Number(medicoId.value)))
const esMedico = computed(() => auth.rol === 'MEDICO_TERAPISTA')
const etiquetaRol = computed(() => esMedico.value ? 'Médico o terapista' : auth.rol === 'ADMINISTRADOR' ? 'Administrador' : 'Agendador de citas')
const visibles = computed(() => citas.value.filter(c => {
  const q = busqueda.value.toLowerCase()
  const match = !q || c.pacienteNombre.toLowerCase().includes(q) || c.pacienteDocumento.includes(q) || c.codigo.toLowerCase().includes(q)
  const est = !filtroEstado.value || c.estado === filtroEstado.value
  return match && est
}))

onMounted(async () => {
  const { data } = await api.get('/medicos')
  if (esMedico.value) {
    const { data: perfil } = await api.get('/medicos/mi-perfil')
    medicos.value = [perfil]
    medicoId.value = perfil.id
  } else {
    medicos.value = data
  }
})

function conteo(est) {
  return citas.value.filter(c => c.estado === est).length
}
function etiqueta(e) {
  return ({ CONFIRMADA: 'Confirmada', CANCELADA: 'Cancelada', ATENDIDA: 'Atendida' })[e] || e
}

async function consultar() {
  if (!medicoId.value || !fecha.value) return
  error.value = ''
  try {
    const { data } = await api.get('/citas/consulta', { params: { medicoId: medicoId.value, fecha: fecha.value } })
    citas.value = data
    consultado.value = true
  } catch (e) {
    error.value = e.response?.data?.message || 'No fue posible consultar la agenda.'
  }
}

async function exportar() {
  if (!medicoId.value || !fecha.value) return
  error.value = ''
  try {
    const response = await api.get('/citas/exportar', { params: { medicoId: medicoId.value, fecha: fecha.value }, responseType: 'blob' })
    const enlace = document.createElement('a')
    enlace.href = URL.createObjectURL(response.data)
    enlace.download = `agenda-${fecha.value}-medico-${medicoId.value}.csv`
    enlace.click()
    URL.revokeObjectURL(enlace.href)
  } catch (e) {
    error.value = 'No fue posible exportar la agenda solicitada.'
  }
}

async function consultarHistorial() {
  if (!/^\d{6,20}$/.test(cedula.value)) {
    errorHistorial.value = 'Ingresa una cédula válida de 6 a 20 dígitos.'
    return
  }
  consultandoHistorial.value = true
  errorHistorial.value = ''
  try {
    const { data } = await api.get('/citas/historial', { params: { cedula: cedula.value } })
    historial.value = data
    historialConsultado.value = true
  } catch (e) {
    historial.value = []
    historialConsultado.value = false
    errorHistorial.value = e.response?.data?.message || 'No fue posible consultar el historial.'
  } finally {
    consultandoHistorial.value = false
  }
}

async function accion(c, tipo) {
  await api.patch(`/citas/${c.id}/${tipo}`)
  await consultar()
}

async function onEditada(actualizada) {
  editar.value = null
  comprobante.value = actualizada
  await consultar()
}
</script>
