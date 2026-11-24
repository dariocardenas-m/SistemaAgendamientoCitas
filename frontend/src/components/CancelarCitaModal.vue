<template>
  <div v-if="cita" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4" @click.self="cerrar">
    <section role="dialog" aria-modal="true" aria-labelledby="cancelar-titulo" class="card w-full max-w-lg p-6">
      <div class="mb-5">
        <p class="text-xs font-bold uppercase tracking-wide text-rose-500">Cancelar cita</p>
        <h2 id="cancelar-titulo" class="mt-1 text-xl font-extrabold">Confirma el motivo</h2>
        <p class="mt-2 text-sm text-slate-600">
          Cita con {{ cita.medicoNombre }} el {{ cita.fecha }} a las {{ cita.hora?.slice(0, 5) }}.
          Al cancelar se liberará la franja y la cita seguirá visible en tu historial.
        </p>
      </div>
      <label class="mb-2 block text-sm font-semibold" for="justificacion-cancelacion">Justificación</label>
      <textarea id="justificacion-cancelacion" v-model="justificacion" class="input-field min-h-28 resize-y" maxlength="500" placeholder="Cuéntanos por qué necesitas cancelar" />
      <div class="mt-1 flex justify-between text-xs text-slate-400">
        <span>Obligatoria, máximo 500 caracteres.</span><span>{{ justificacion.length }}/500</span>
      </div>
      <p v-if="error" role="alert" class="mt-3 text-sm text-rose-600">{{ error }}</p>
      <div class="mt-6 flex justify-end gap-2">
        <button class="btn-ghost" :disabled="guardando" @click="cerrar">Volver</button>
        <button class="rounded-2xl bg-rose-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-rose-700 disabled:opacity-60" :disabled="guardando || !justificacion.trim()" @click="confirmar">
          {{ guardando ? 'Cancelando…' : 'Confirmar cancelación' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '../services/api'

const props = defineProps({ cita: { type: Object, default: null } })
const emit = defineEmits(['cerrar', 'guardada'])
const justificacion = ref('')
const error = ref('')
const guardando = ref(false)

watch(() => props.cita, () => {
  justificacion.value = ''
  error.value = ''
  guardando.value = false
})

function cerrar() {
  if (!guardando.value) emit('cerrar')
}

async function confirmar() {
  const texto = justificacion.value.trim()
  if (!texto) {
    error.value = 'Escribe una justificación para continuar.'
    return
  }
  if (texto.length > 500) {
    error.value = 'La justificación no puede superar 500 caracteres.'
    return
  }
  guardando.value = true
  error.value = ''
  try {
    await api.patch(`/citas/${props.cita.id}/cancelar`, { justificacion: texto })
    emit('guardada')
  } catch (e) {
    error.value = e.response?.data?.message || 'No fue posible cancelar la cita.'
  } finally {
    guardando.value = false
  }
}
</script>
