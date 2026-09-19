<template>
  <div v-if="cita" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
    <div class="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-3xl border border-brand/30 bg-white p-8 shadow-xl">
      <div class="text-center">
        <div class="mx-auto mb-3 flex h-16 w-16 items-center justify-center rounded-full bg-brand text-3xl text-white">✓</div>
        <p class="mb-2 inline-flex rounded-full bg-brand-light px-3 py-1 text-xs font-semibold text-brand">Confirmación de agendamiento</p>
        <h2 class="text-2xl font-extrabold">Cita agendada correctamente</h2>
        <p class="mt-2 text-sm text-slate-500">Tu reserva ha sido confirmada en el sistema. Presenta tu documento de identidad en recepción 15 minutos antes.</p>
      </div>
      <div class="mt-6 rounded-2xl border border-slate-100 p-5">
        <div class="mb-4 flex items-center justify-between">
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-400">Comprobante de reserva médica</p>
          <span class="rounded-full border px-3 py-1 text-xs font-bold">{{ cita.codigo }}</span>
        </div>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div>
            <p class="text-slate-400">Paciente</p>
            <p class="font-bold">{{ cita.pacienteNombre }}</p>
            <p class="text-xs">Doc: {{ cita.pacienteDocumento }}</p>
          </div>
          <div>
            <p class="text-slate-400">Médico asignado</p>
            <p class="font-bold">{{ cita.medicoNombre }}</p>
            <p class="text-xs text-brand">{{ cita.especialidad }}</p>
          </div>
          <div>
            <p class="text-slate-400">Fecha programada</p>
            <p class="font-bold">{{ formatearFecha(cita.fecha) }}</p>
          </div>
          <div>
            <p class="text-slate-400">Hora de consulta</p>
            <p class="font-bold">{{ cita.hora?.slice(0,5) }} ({{ cita.duracionMinutos }} min)</p>
          </div>
          <div>
            <p class="text-slate-400">Estado de la cita</p>
            <p class="font-semibold text-brand">● Confirmada</p>
          </div>
          <div>
            <p class="text-slate-400">Modalidad</p>
            <p class="font-bold">{{ cita.modalidad === 'TELEMEDICINA' ? 'Telemedicina' : 'Presencial' }}</p>
          </div>
        </div>
      </div>
      <div class="mt-6 flex flex-col gap-3 sm:flex-row">
        <button class="btn-primary flex-1" @click="$emit('verCitas')">Ver mis citas</button>
        <button class="btn-ghost flex-1" @click="$emit('inicio')">Volver al inicio</button>
      </div>
      <button class="mt-4 w-full text-center text-xs text-slate-500 hover:text-brand" @click="imprimir">Imprimir comprobante / Guardar PDF</button>
    </div>
  </div>
</template>

<script setup>
defineProps({ cita: Object })
defineEmits(['verCitas', 'inicio'])

function formatearFecha(f) {
  if (!f) return ''
  return new Date(f + 'T00:00:00').toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' })
}

function imprimir() {
  window.print()
}
</script>
