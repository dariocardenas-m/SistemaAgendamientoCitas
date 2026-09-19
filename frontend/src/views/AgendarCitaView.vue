<template>
  <div class="mx-auto max-w-6xl px-4 py-8">
    <div class="card mb-6 flex flex-col gap-4 p-6 md:flex-row md:items-center md:justify-between">
      <div>
        <span class="rounded-full bg-brand-light px-3 py-1 text-xs font-semibold text-brand">Reserva de citas médicas</span>
        <h1 class="mt-3 text-2xl font-extrabold">Agendar Cita Médica</h1>
        <p class="text-sm text-slate-500">Sigue los 5 pasos para reservar tu turno con el especialista de tu preferencia.</p>
      </div>
      <div class="rounded-2xl border px-4 py-3 text-sm">
        <p class="text-xs text-slate-400">Paciente</p>
        <p class="font-bold">{{ auth.usuario?.nombreCompleto }}</p>
        <p class="text-xs text-slate-500">CC {{ auth.usuario?.documento }}</p>
      </div>
    </div>

    <StepWizard :current="store.paso" @goto="irPaso" />

    <div class="card p-6">
      <!-- Paso 1 -->
      <div v-if="store.paso === 1">
        <h2 class="text-lg font-bold">Paso 1: Selecciona la Especialidad Médica</h2>
        <p class="mb-5 text-sm text-slate-500">Elige el área de atención requerida para ver los médicos y terapeutas disponibles.</p>
        <div class="grid gap-4 md:grid-cols-3">
          <button
            v-for="e in store.especialidades"
            :key="e.id"
            class="rounded-2xl border p-5 text-left transition"
            :class="store.especialidadId === e.id ? 'border-brand bg-brand-light ring-1 ring-brand' : 'border-slate-100 hover:border-brand/40'"
            @click="store.especialidadId = e.id"
          >
            <div class="mb-3 flex items-center justify-between">
              <span class="text-xl">✚</span>
              <span v-if="store.especialidadId === e.id" class="text-brand">✓</span>
            </div>
            <p class="font-bold">{{ e.nombre }}</p>
            <p class="mt-1 text-sm text-slate-500">{{ e.descripcion }}</p>
            <p class="mt-3 text-xs font-semibold text-brand">{{ e.profesionalesDisponibles }} profesional{{ e.profesionalesDisponibles === 1 ? '' : 'es' }} disponible{{ e.profesionalesDisponibles === 1 ? '' : 's' }}</p>
          </button>
        </div>
        <div class="mt-6 flex justify-end">
          <button class="btn-primary" :disabled="!store.especialidadId" @click="siguienteDesdeEspecialidad">Continuar al Paso 2 →</button>
        </div>
      </div>

      <!-- Paso 2 -->
      <div v-else-if="store.paso === 2">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="text-lg font-bold">Paso 2: Selecciona el Profesional</h2>
          <button class="text-sm font-semibold text-brand" @click="store.paso = 1">Cambiar especialidad</button>
        </div>
        <div class="grid gap-4 md:grid-cols-2">
          <button
            v-for="m in store.medicos"
            :key="m.id"
            class="rounded-2xl border p-5 text-left"
            :class="store.medico?.id === m.id ? 'border-brand bg-brand-light ring-1 ring-brand' : 'border-slate-100 hover:border-brand/40'"
            @click="store.medico = m"
          >
            <p class="text-xs font-semibold text-brand">{{ m.especialidad }}</p>
            <p class="text-lg font-extrabold">{{ m.nombreCompleto }}</p>
            <p class="text-xs text-slate-500">★ {{ m.rating }} · {{ m.aniosExperiencia }} años exp.</p>
            <p class="mt-2 text-sm text-slate-600">{{ m.descripcion }}</p>
            <div class="mt-4 flex items-center justify-between text-xs">
              <span>{{ m.consultorio }}</span>
              <span class="rounded-full bg-white px-3 py-1 font-semibold text-brand">Turnos de {{ m.intervaloMinutos }} min</span>
            </div>
          </button>
        </div>
        <div class="mt-6 flex justify-between">
          <button class="btn-ghost" @click="store.paso = 1">← Anterior</button>
          <button class="btn-primary" :disabled="!store.medico" @click="siguienteDesdeMedico">Continuar al Paso 3 →</button>
        </div>
      </div>

      <!-- Paso 3 -->
      <div v-else-if="store.paso === 3">
        <h2 class="text-lg font-bold">Paso 3: Selecciona una fecha hábil</h2>
        <p class="mb-4 text-sm text-slate-500">Solo se muestran días de atención del profesional dentro de la ventana configurada por el administrador.</p>
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-4 md:grid-cols-7">
          <button
            v-for="f in store.fechasHabiles"
            :key="f"
            class="rounded-2xl border p-3 text-center text-sm"
            :class="store.fecha === f ? 'border-brand bg-brand text-white' : 'border-slate-100 hover:border-brand/40'"
            @click="store.fecha = f"
          >
            <p class="text-[10px] uppercase opacity-80">{{ diaSemana(f) }}</p>
            <p class="font-bold">{{ f.slice(8) }}</p>
            <p class="text-[10px]">{{ mes(f) }}</p>
          </button>
        </div>
        <div class="mt-6 flex justify-between">
          <button class="btn-ghost" @click="store.paso = 2">← Anterior</button>
          <button class="btn-primary" :disabled="!store.fecha" @click="siguienteDesdeFecha">Continuar al Paso 4 →</button>
        </div>
      </div>

      <!-- Paso 4 -->
      <div v-else-if="store.paso === 4">
        <h2 class="text-lg font-bold">Paso 4: Franjas horarias</h2>
        <p class="mb-4 text-sm text-slate-500">Las franjas libres se distinguen de las ocupadas para evitar reservas en horarios no disponibles.</p>
        <div class="mb-4 flex gap-4 text-xs">
          <span class="flex items-center gap-1"><span class="h-3 w-3 rounded bg-brand"></span> Disponible</span>
          <span class="flex items-center gap-1"><span class="h-3 w-3 rounded bg-slate-200"></span> Ocupada</span>
        </div>
        <div class="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
          <button
            v-for="h in todasLasFranjas"
            :key="h"
            class="rounded-xl border px-3 py-3 text-sm font-semibold"
            :disabled="store.disponibilidad.ocupadas.includes(h) || !store.disponibilidad.libres.includes(h)"
            :class="slotClass(h)"
            @click="store.hora = h"
          >
            {{ h.slice(0,5) }}
          </button>
        </div>
        <div class="mt-6 flex justify-between">
          <button class="btn-ghost" @click="store.paso = 3">← Anterior</button>
          <button class="btn-primary" :disabled="!store.hora" @click="store.paso = 5">Continuar al Paso 5 →</button>
        </div>
      </div>

      <!-- Paso 5 -->
      <div v-else>
        <h2 class="text-lg font-bold">Paso 5: Resumen y Confirmación de Cita</h2>
        <p class="mb-5 text-sm text-slate-500">Verifica todos los datos antes de registrar la cita en el sistema.</p>
        <div class="rounded-2xl border border-brand/20 bg-brand-light/40 p-5">
          <div class="grid gap-4 md:grid-cols-2">
            <div class="rounded-2xl bg-white p-4">
              <p class="text-[11px] font-semibold uppercase text-slate-400">Profesional asignado</p>
              <p class="font-bold">{{ store.medico?.nombreCompleto }}</p>
              <p class="text-sm text-brand">{{ store.medico?.especialidad }}</p>
              <p class="text-xs text-slate-500">{{ store.medico?.consultorio }}</p>
            </div>
            <div class="rounded-2xl bg-white p-4">
              <p class="text-[11px] font-semibold uppercase text-slate-400">Datos del paciente</p>
              <p class="font-bold">{{ auth.usuario?.nombreCompleto }}</p>
              <p class="text-xs">Doc: {{ auth.usuario?.documento }}</p>
              <p class="text-xs">{{ auth.usuario?.email }}</p>
            </div>
          </div>
          <div class="mt-4 grid gap-3 rounded-2xl bg-white p-4 text-sm md:grid-cols-3">
            <div><p class="text-slate-400">Fecha agendada</p><p class="font-bold">{{ formatear(store.fecha) }}</p></div>
            <div><p class="text-slate-400">Hora de consulta</p><p class="font-bold text-brand">{{ store.hora?.slice(0,5) }} ({{ store.medico?.intervaloMinutos }} minutos)</p></div>
            <div><p class="text-slate-400">Modalidad</p><p class="font-bold">{{ store.modalidad === 'TELEMEDICINA' ? 'Telemedicina' : 'Presencial' }}</p></div>
          </div>
          <p class="mt-4 text-xs font-bold uppercase text-slate-500">Modalidad de consulta</p>
          <div class="mt-2 grid grid-cols-2 gap-2">
            <button class="rounded-xl px-4 py-3 text-sm font-semibold" :class="store.modalidad==='PRESENCIAL' ? 'bg-brand text-white' : 'bg-white'" @click="store.modalidad='PRESENCIAL'">Presencial</button>
            <button class="rounded-xl px-4 py-3 text-sm font-semibold" :class="store.modalidad==='TELEMEDICINA' ? 'bg-brand text-white' : 'bg-white'" @click="store.modalidad='TELEMEDICINA'">Telemedicina</button>
          </div>
          <label class="mt-4 block text-xs font-bold uppercase text-slate-500">Motivo o notas de la consulta (opcional)
            <textarea v-model="store.motivo" class="input-field mt-1" rows="2" placeholder="Ej: Control de rutina, dolor lumbar, revisión de exámenes..."></textarea>
          </label>
        </div>
        <p v-if="error" class="mt-3 text-sm text-red-600">{{ error }}</p>
        <div class="mt-6 flex justify-between">
          <button class="btn-ghost" @click="store.paso = 4">← Anterior</button>
          <button class="btn-primary" :disabled="enviando" @click="confirmar">Confirmar Cita</button>
        </div>
      </div>
    </div>

    <ComprobanteModal
      :cita="store.citaConfirmada"
      @ver-citas="router.push('/mis-citas')"
      @inicio="router.push('/')"
    />

    <div v-if="store.conflictoReserva" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4" role="alertdialog" aria-modal="true" aria-labelledby="conflicto-title">
      <div class="w-full max-w-md rounded-3xl bg-white p-6 shadow-xl">
        <div class="flex h-12 w-12 items-center justify-center rounded-full bg-amber-100 text-xl text-amber-700">!</div>
        <h2 id="conflicto-title" class="mt-4 text-lg font-extrabold">Horario ya no disponible</h2>
        <p class="mt-2 text-sm text-slate-600">{{ store.conflictoReserva }}</p>
        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-ghost" @click="store.cerrarConflictoReserva()">Cerrar</button>
          <button class="btn-primary" @click="elegirOtroHorario">Elegir otro horario</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import StepWizard from '../components/StepWizard.vue'
import ComprobanteModal from '../components/ComprobanteModal.vue'
import { useAuthStore } from '../stores/authStore'
import { useCitasStore } from '../stores/citasStore'

const auth = useAuthStore()
const store = useCitasStore()
const router = useRouter()
const error = ref('')
const enviando = ref(false)

const todasLasFranjas = computed(() => {
  const set = new Set([...(store.disponibilidad.libres || []), ...(store.disponibilidad.ocupadas || [])])
  return [...set].sort()
})

onMounted(async () => {
  store.resetFlujo()
  await store.cargarEspecialidades()
})

function irPaso(n) {
  if (n < store.paso) store.paso = n
}

async function siguienteDesdeEspecialidad() {
  await store.cargarMedicos(store.especialidadId)
  store.medico = null
  store.paso = 2
}

async function siguienteDesdeMedico() {
  await store.cargarFechas(store.medico.id)
  store.fecha = ''
  store.paso = 3
}

async function siguienteDesdeFecha() {
  await store.cargarDisponibilidad(store.medico.id, store.fecha)
  store.hora = ''
  store.paso = 4
}

function slotClass(h) {
  if (store.disponibilidad.ocupadas.includes(h) || !store.disponibilidad.libres.includes(h)) {
    return 'border-slate-100 bg-slate-100 text-slate-400 cursor-not-allowed'
  }
  if (store.hora === h) return 'border-brand bg-brand text-white'
  return 'border-brand/30 bg-white text-brand hover:bg-brand-light'
}

function diaSemana(f) {
  return new Date(f + 'T00:00:00').toLocaleDateString('es-CO', { weekday: 'short' })
}
function mes(f) {
  return new Date(f + 'T00:00:00').toLocaleDateString('es-CO', { month: 'short' })
}
function formatear(f) {
  return new Date(f + 'T00:00:00').toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' })
}

async function confirmar() {
  error.value = ''
  enviando.value = true
  try {
    await store.confirmar()
  } catch (e) {
    if (!e.esConflictoReserva) {
      error.value = e.response?.data?.message || 'No fue posible confirmar la cita.'
    }
  } finally {
    enviando.value = false
  }
}

async function elegirOtroHorario() {
  store.cerrarConflictoReserva()
  await store.cargarDisponibilidad(store.medico.id, store.fecha)
  store.hora = ''
  store.paso = 4
}
</script>
