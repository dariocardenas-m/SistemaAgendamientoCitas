import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../services/api'

export const useCitasStore = defineStore('citas', () => {
  const especialidades = ref([])
  const medicos = ref([])
  const paso = ref(1)
  const especialidadId = ref(null)
  const medico = ref(null)
  const fecha = ref('')
  const hora = ref('')
  const modalidad = ref('PRESENCIAL')
  const motivo = ref('')
  const disponibilidad = ref({ libres: [], ocupadas: [], intervaloMinutos: 30 })
  const fechasHabiles = ref([])
  const citaConfirmada = ref(null)
  const conflictoReserva = ref('')
  const misCitas = ref([])

  async function cargarEspecialidades() {
    const { data } = await api.get('/especialidades')
    especialidades.value = data
  }

  async function cargarMedicos(idEsp) {
    const { data } = await api.get(`/especialidades/${idEsp}/medicos`)
    medicos.value = data
  }

  async function cargarFechas(medicoId) {
    const { data } = await api.get('/citas/fechas-habiles', { params: { medicoId } })
    fechasHabiles.value = data
  }

  async function cargarDisponibilidad(medicoId, fechaSel) {
    const { data } = await api.get('/citas/disponibilidad', { params: { medicoId, fecha: fechaSel } })
    disponibilidad.value = data
  }

  async function confirmar() {
    try {
      const { data } = await api.post('/citas', {
        medicoId: medico.value.id,
        fecha: fecha.value,
        hora: hora.value,
        modalidad: modalidad.value,
        motivo: motivo.value
      })
      citaConfirmada.value = data
      return data
    } catch (error) {
      if (error.esConflictoReserva) {
        conflictoReserva.value = 'La franja horaria seleccionada acaba de ser ocupada por otro usuario. Por favor, elige otro horario.'
      }
      throw error
    }
  }

  async function cargarMisCitas() {
    const { data } = await api.get('/citas/mias')
    misCitas.value = data
  }

  function resetFlujo() {
    paso.value = 1
    especialidadId.value = null
    medico.value = null
    fecha.value = ''
    hora.value = ''
    modalidad.value = 'PRESENCIAL'
    motivo.value = ''
    citaConfirmada.value = null
    conflictoReserva.value = ''
  }

  function cerrarConflictoReserva() {
    conflictoReserva.value = ''
  }

  return {
    especialidades, medicos, paso, especialidadId, medico, fecha, hora, modalidad, motivo,
    disponibilidad, fechasHabiles, citaConfirmada, conflictoReserva, misCitas,
    cargarEspecialidades, cargarMedicos, cargarFechas, cargarDisponibilidad, confirmar, cargarMisCitas, resetFlujo, cerrarConflictoReserva
  }
})
