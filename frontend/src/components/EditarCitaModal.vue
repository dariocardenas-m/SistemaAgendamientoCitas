<template>
  <div v-if="cita" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
    <div class="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-3xl bg-white p-6 shadow-xl">
      <p class="text-xs font-semibold uppercase tracking-wide text-brand">Reprogramar cita</p>
      <h2 class="mt-1 text-xl font-extrabold">Editar cita {{ cita.codigo }}</h2>
      <p class="mt-1 text-sm text-slate-500">
        {{ permitirCambiarMedico ? 'Puede cambiar médico, fecha, hora, modalidad y motivo.' : 'Puede reprogramar fecha, hora, modalidad y motivo de su cita confirmada.' }}
      </p>

      <div v-if="permitirCambiarMedico" class="mt-4">
        <label class="text-xs font-bold uppercase text-slate-500">Médico
          <select v-model="form.medicoId" class="input-field mt-1" @change="onMedicoChange">
            <option disabled value="">Seleccione profesional</option>
            <option v-for="m in medicos" :key="m.id" :value="m.id">{{ m.nombreCompleto }} — {{ m.especialidad }}</option>
          </select>
        </label>
      </div>
      <p v-else class="mt-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm">
        <span class="text-xs uppercase text-slate-400">Profesional</span><br>
        <span class="font-bold">{{ cita.medicoNombre }}</span>
        <span class="text-slate-500"> · {{ cita.especialidad }}</span>
      </p>

      <p class="mt-4 text-xs font-bold uppercase text-slate-500">Fecha hábil</p>
      <p v-if="cargandoDisponibilidad" class="mt-2 text-sm text-slate-500">Cargando disponibilidad…</p>
      <div class="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-4 md:grid-cols-6">
        <button
          v-for="f in store.fechasHabiles"
          :key="f"
          type="button"
          class="rounded-2xl border p-2 text-center text-xs"
          :class="form.fecha === f ? 'border-brand bg-brand text-white' : 'border-slate-100 hover:border-brand/40'"
          @click="seleccionarFecha(f)"
        >
          <p class="uppercase opacity-80">{{ diaSemana(f) }}</p>
          <p class="font-bold">{{ f.slice(8) }} {{ mes(f) }}</p>
        </button>
      </div>

      <p class="mt-4 text-xs font-bold uppercase text-slate-500">Franja horaria</p>
      <div class="mt-2 mb-2 flex gap-4 text-xs">
        <span class="flex items-center gap-1"><span class="h-3 w-3 rounded bg-brand"></span> Disponible</span>
        <span class="flex items-center gap-1"><span class="h-3 w-3 rounded bg-slate-200"></span> Ocupada</span>
      </div>
      <div class="grid grid-cols-3 gap-2 sm:grid-cols-4 md:grid-cols-6">
        <button
          v-for="h in todasLasFranjas"
          :key="h"
          type="button"
          class="rounded-xl border px-2 py-2 text-sm font-semibold"
          :disabled="ocupada(h) || cargandoDisponibilidad"
          :class="slotClass(h)"
          @click="form.hora = h"
        >
          {{ h.slice(0,5) }}
        </button>
      </div>

      <p class="mt-4 text-xs font-bold uppercase text-slate-500">Modalidad</p>
      <div class="mt-2 grid grid-cols-2 gap-2">
        <button type="button" class="rounded-xl px-4 py-3 text-sm font-semibold" :class="form.modalidad==='PRESENCIAL' ? 'bg-brand text-white' : 'bg-slate-50'" @click="form.modalidad='PRESENCIAL'">Presencial</button>
        <button type="button" class="rounded-xl px-4 py-3 text-sm font-semibold" :class="form.modalidad==='TELEMEDICINA' ? 'bg-brand text-white' : 'bg-slate-50'" @click="form.modalidad='TELEMEDICINA'">Telemedicina</button>
      </div>

      <label class="mt-4 block text-xs font-bold uppercase text-slate-500">Motivo o notas
        <textarea v-model="form.motivo" class="input-field mt-1" rows="2" maxlength="500" placeholder="Motivo de la consulta"></textarea>
      </label>

      <p v-if="error" class="mt-3 text-sm text-red-600">{{ error }}</p>
      <div class="mt-6 flex justify-end gap-3">
        <button class="btn-ghost" type="button" @click="$emit('cerrar')">Cancelar</button>
        <button class="btn-primary" type="button" :disabled="enviando || cargandoDisponibilidad || !form.medicoId || !form.fecha || !form.hora" @click="guardar">Guardar cambios</button>
      </div>
    </div>

    <div v-if="store.conflictoReserva" class="fixed inset-0 z-[60] flex items-center justify-center bg-slate-900/40 p-4" role="alertdialog" aria-modal="true">
      <div class="w-full max-w-md rounded-3xl bg-white p-6 shadow-xl">
        <div class="flex h-12 w-12 items-center justify-center rounded-full bg-amber-100 text-xl text-amber-700">!</div>
        <h3 class="mt-4 text-lg font-extrabold">Horario ya no disponible</h3>
        <p class="mt-2 text-sm text-slate-600">{{ store.conflictoReserva }}</p>
        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-ghost" type="button" @click="store.cerrarConflictoReserva()">Cerrar</button>
          <button class="btn-primary" type="button" @click="elegirOtroHorario">Elegir otro horario</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import api from '../services/api'
import { useCitasStore } from '../stores/citasStore'

const props = defineProps({
  cita: { type: Object, default: null },
  permitirCambiarMedico: { type: Boolean, default: false }
})
const emit = defineEmits(['cerrar', 'guardada'])

const store = useCitasStore()
const medicos = ref([])
const error = ref('')
const enviando = ref(false)
const cargandoDisponibilidad = ref(false)
const form = reactive({
  medicoId: '',
  fecha: '',
  hora: '',
  modalidad: 'PRESENCIAL',
  motivo: ''
})

const todasLasFranjas = computed(() => {
  const set = new Set([...(store.disponibilidad.libres || []), ...(store.disponibilidad.ocupadas || [])])
  return [...set].sort()
})

watch(() => props.cita, async (c) => {
  if (!c) return
  error.value = ''
  cargandoDisponibilidad.value = true
  form.medicoId = c.medicoId
  form.fecha = c.fecha
  form.hora = normalizarHora(c.hora)
  form.modalidad = c.modalidad || 'PRESENCIAL'
  form.motivo = c.motivo || ''
  try {
    if (props.permitirCambiarMedico) {
      const { data } = await api.get('/medicos')
      medicos.value = data
    }
    await store.cargarFechas(form.medicoId)
    await store.cargarDisponibilidad(form.medicoId, form.fecha, c.id)
  } catch (e) {
    error.value = e.response?.data?.message || 'No fue posible cargar la disponibilidad de la cita.'
  } finally {
    cargandoDisponibilidad.value = false
  }
}, { immediate: true })

function normalizarHora(h) {
  if (!h) return ''
  return h.length === 5 ? `${h}:00` : h
}

function ocupada(h) {
  return store.disponibilidad.ocupadas.includes(h) || !store.disponibilidad.libres.includes(h)
}

function slotClass(h) {
  if (ocupada(h)) return 'border-slate-100 bg-slate-100 text-slate-400 cursor-not-allowed'
  if (form.hora === h) return 'border-brand bg-brand text-white'
  return 'border-brand/30 bg-white text-brand hover:bg-brand-light'
}

async function onMedicoChange() {
  form.fecha = ''
  form.hora = ''
  cargandoDisponibilidad.value = true
  error.value = ''
  try {
    await store.cargarFechas(form.medicoId)
  } catch (e) {
    error.value = e.response?.data?.message || 'No fue posible cargar las fechas del profesional.'
  } finally {
    cargandoDisponibilidad.value = false
  }
}

async function seleccionarFecha(f) {
  form.fecha = f
  form.hora = ''
  cargandoDisponibilidad.value = true
  error.value = ''
  try {
    await store.cargarDisponibilidad(form.medicoId, f, props.cita?.id)
  } catch (e) {
    error.value = e.response?.data?.message || 'No fue posible cargar los horarios disponibles.'
  } finally {
    cargandoDisponibilidad.value = false
  }
}

async function guardar() {
  error.value = ''
  enviando.value = true
  try {
    const actualizada = await store.editarCita(props.cita.id, {
      medicoId: Number(form.medicoId),
      fecha: form.fecha,
      hora: form.hora,
      modalidad: form.modalidad,
      motivo: form.motivo
    })
    emit('guardada', actualizada)
  } catch (e) {
    if (!e.esConflictoReserva) {
      error.value = e.response?.data?.message || 'No fue posible actualizar la cita.'
    }
  } finally {
    enviando.value = false
  }
}

async function elegirOtroHorario() {
  store.cerrarConflictoReserva()
  form.hora = ''
  if (form.medicoId && form.fecha) {
    await store.cargarDisponibilidad(form.medicoId, form.fecha, props.cita?.id)
  }
}

function diaSemana(f) {
  return new Date(f + 'T00:00:00').toLocaleDateString('es-CO', { weekday: 'short' })
}
function mes(f) {
  return new Date(f + 'T00:00:00').toLocaleDateString('es-CO', { month: 'short' })
}
</script>
