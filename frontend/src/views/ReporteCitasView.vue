<template>
  <div class="mx-auto max-w-7xl px-4 py-8">
    <header class="card mb-6 flex flex-col gap-4 p-6 md:flex-row md:items-end md:justify-between">
      <div>
        <span class="rounded-full bg-brand-light px-3 py-1 text-xs font-semibold text-brand">Análisis de actividad</span>
        <h1 class="mt-3 text-2xl font-extrabold text-brand">Citas por mes y profesional</h1>
        <p class="mt-1 max-w-2xl text-sm text-slate-500">Compara la actividad del año seleccionado. Las citas confirmadas, atendidas y canceladas se identifican por separado.</p>
      </div>
      <form class="flex flex-wrap items-end gap-3" @submit.prevent="consultar">
        <label class="block text-sm font-semibold" for="anio-reporte">
          Año
          <input id="anio-reporte" v-model.number="anio" class="input-field mt-1 min-w-32" type="number" min="1900" max="2100" required />
        </label>
        <button class="btn-primary" type="submit" :disabled="cargando">{{ cargando ? 'Consultando…' : 'Generar reporte' }}</button>
      </form>
    </header>

    <div class="card mb-6 flex flex-wrap items-center justify-between gap-4 p-4">
      <div class="flex rounded-full bg-slate-100 p-1" role="group" aria-label="Periodo del reporte">
        <button class="rounded-full px-4 py-2 text-sm font-semibold transition" :class="periodo==='anual' ? 'bg-white text-brand shadow-sm' : 'text-slate-500'" @click="periodo='anual'">Anual</button>
        <button class="rounded-full px-4 py-2 text-sm font-semibold transition" :class="periodo==='mensual' ? 'bg-white text-brand shadow-sm' : 'text-slate-500'" @click="periodo='mensual'">Mensual</button>
      </div>
      <label v-if="periodo==='mensual'" class="flex items-center gap-2 text-sm font-semibold">
        Mes
        <select v-model.number="mesSeleccionado" class="input-field !w-auto !py-2">
          <option v-for="mes in meses" :key="mes.numero" :value="mes.numero">{{ mes.nombreCompleto }}</option>
        </select>
      </label>
    </div>

    <section class="card p-5" aria-labelledby="grafico-titulo">
      <div class="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 id="grafico-titulo" class="font-bold">{{ periodo==='anual' ? 'Actividad mensual' : `Actividad de ${nombreMesSeleccionado}` }} · {{ reporte?.anio ?? anio }}</h2>
          <p class="text-xs text-slate-500">{{ alcance }} {{ periodo==='mensual' ? 'Las tres series de estado se comparan en el mismo gráfico.' : '' }}</p>
        </div>
        <label v-if="periodo==='anual'" class="flex items-center gap-2 text-sm font-semibold">
          Mostrar
          <select v-model="estadoSeleccionado" class="input-field !w-auto !py-2">
            <option value="total">Todas las citas</option>
            <option value="confirmadas">Confirmadas</option>
            <option value="atendidas">Atendidas</option>
            <option value="canceladas">Canceladas</option>
          </select>
        </label>
      </div>

      <div v-if="error" role="alert" class="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">{{ error }}</div>
      <div v-else-if="cargando" class="flex min-h-72 items-center justify-center text-sm text-slate-500" aria-live="polite">Cargando datos del reporte…</div>
      <div v-else-if="!reporte || reporte.medicos.length === 0" class="flex min-h-72 flex-col items-center justify-center text-center">
        <span class="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-xl text-slate-500">—</span>
        <p class="font-semibold">No hay citas para {{ reporte?.anio ?? anio }}</p>
        <p class="mt-1 text-sm text-slate-500">Prueba con otro año. El sistema no completa meses sin registros con datos ficticios.</p>
      </div>
      <div v-else class="overflow-x-auto">
        <svg class="h-auto min-w-[760px] w-full" viewBox="0 0 1200 470" role="img" :aria-label="descripcionGrafico">
          <g v-for="tick in ticks" :key="`tick-${tick.value}`">
            <line :x1="plot.left" :x2="plot.right" :y1="tick.y" :y2="tick.y" stroke="#e2e8f0" stroke-width="1" />
            <text :x="plot.left - 12" :y="tick.y + 4" text-anchor="end" fill="#64748b" font-size="12">{{ tick.value }}</text>
          </g>
          <line :x1="plot.left" :x2="plot.left" :y1="plot.top" :y2="plot.bottom" stroke="#94a3b8" />
          <line :x1="plot.left" :x2="plot.right" :y1="plot.bottom" :y2="plot.bottom" stroke="#94a3b8" />
          <g v-for="(categoria, categoriaIndex) in categoriasGrafico" :key="categoria.key">
            <text :x="xCategoria(categoriaIndex)" :y="plot.bottom + 24" text-anchor="middle" fill="#475569" font-size="12">{{ categoria.label }}</text>
            <rect
              v-for="(serie, serieIndex) in seriesGrafico"
              :key="`${categoria.key}-${serie.key}`"
              :x="xBarra(categoriaIndex, serieIndex)"
              :y="yBarra(valorGrafico(categoria, serie))"
              :width="anchoBarra"
              :height="altoBarra(valorGrafico(categoria, serie))"
              :fill="serie.color"
              rx="2"
            >
              <title>{{ tituloBarra(categoria, serie) }}</title>
            </rect>
          </g>
        </svg>

        <div class="mt-4 flex flex-wrap gap-x-5 gap-y-2 border-t border-slate-100 pt-4" :aria-label="periodo==='anual' ? 'Leyenda de profesionales' : 'Leyenda de estados de cita'">
          <span v-for="serie in seriesGrafico" :key="serie.key" class="inline-flex items-center gap-2 text-xs text-slate-600">
            <span class="h-3 w-3 rounded-sm" :style="{ backgroundColor: serie.color }" aria-hidden="true"></span>
            {{ serie.label }}
          </span>
        </div>
        <p class="mt-3 text-xs text-slate-400">Pasa el cursor sobre una barra para ver su detalle. Los meses o profesionales sin citas se muestran en cero.</p>
      </div>
    </section>

    <section v-if="reporte?.medicos.length" class="card mt-6 overflow-hidden">
      <div class="border-b border-slate-100 p-5">
        <h2 class="font-bold">{{ periodo==='anual' ? 'Resumen del año' : `Resumen de ${nombreMesSeleccionado}` }}</h2>
        <p class="text-sm text-slate-500">Totales {{ periodo==='anual' ? 'del año' : 'del mes' }} por profesional y estado.</p>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full text-left text-sm">
          <thead class="bg-slate-50 text-xs uppercase text-slate-500">
            <tr><th class="px-5 py-3">Profesional</th><th class="px-5 py-3 text-right">Confirmadas</th><th class="px-5 py-3 text-right">Atendidas</th><th class="px-5 py-3 text-right">Canceladas</th><th class="px-5 py-3 text-right">Total</th></tr>
          </thead>
          <tbody>
            <tr v-for="medico in reporte.medicos" :key="medico.medicoId" class="border-t border-slate-100">
              <th scope="row" class="px-5 py-3 font-semibold">{{ medico.medicoNombre }}</th>
              <td class="px-5 py-3 text-right">{{ totalPeriodo(medico, 'confirmadas') }}</td>
              <td class="px-5 py-3 text-right">{{ totalPeriodo(medico, 'atendidas') }}</td>
              <td class="px-5 py-3 text-right">{{ totalPeriodo(medico, 'canceladas') }}</td>
              <td class="px-5 py-3 text-right font-bold">{{ totalPeriodo(medico, 'total') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <details v-if="reporte?.medicos.length && periodo==='anual'" class="card mt-4 overflow-hidden">
      <summary class="cursor-pointer px-5 py-4 text-sm font-semibold text-brand">Ver detalle mensual y valores en tabla</summary>
      <div class="overflow-x-auto border-t border-slate-100">
        <table class="min-w-full text-left text-sm">
          <thead class="bg-slate-50 text-xs uppercase text-slate-500">
            <tr><th class="px-5 py-3">Mes</th><th class="px-5 py-3">Profesional</th><th class="px-5 py-3 text-right">Confirmadas</th><th class="px-5 py-3 text-right">Atendidas</th><th class="px-5 py-3 text-right">Canceladas</th><th class="px-5 py-3 text-right">Total</th></tr>
          </thead>
          <tbody>
            <template v-for="mes in meses" :key="`detalle-${mes.numero}`">
              <tr v-for="medico in reporte.medicos" :key="`${mes.numero}-${medico.medicoId}`" class="border-t border-slate-100">
                <td class="px-5 py-3">{{ mes.nombre }}</td>
                <th scope="row" class="px-5 py-3 font-semibold">{{ medico.medicoNombre }}</th>
                <td class="px-5 py-3 text-right">{{ medico.meses[mes.numero - 1].confirmadas }}</td>
                <td class="px-5 py-3 text-right">{{ medico.meses[mes.numero - 1].atendidas }}</td>
                <td class="px-5 py-3 text-right">{{ medico.meses[mes.numero - 1].canceladas }}</td>
                <td class="px-5 py-3 text-right font-bold">{{ totalMes(medico.meses[mes.numero - 1]) }}</td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </details>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import { useAuthStore } from '../stores/authStore'

const auth = useAuthStore()
const anio = ref(new Date().getFullYear())
const periodo = ref('anual')
const mesSeleccionado = ref(new Date().getMonth() + 1)
const reporte = ref(null)
const cargando = ref(false)
const error = ref('')
const estadoSeleccionado = ref('total')
const requestId = ref(0)

const meses = [
  { numero: 1, nombre: 'Ene', nombreCompleto: 'Enero' }, { numero: 2, nombre: 'Feb', nombreCompleto: 'Febrero' }, { numero: 3, nombre: 'Mar', nombreCompleto: 'Marzo' },
  { numero: 4, nombre: 'Abr', nombreCompleto: 'Abril' }, { numero: 5, nombre: 'May', nombreCompleto: 'Mayo' }, { numero: 6, nombre: 'Jun', nombreCompleto: 'Junio' },
  { numero: 7, nombre: 'Jul', nombreCompleto: 'Julio' }, { numero: 8, nombre: 'Ago', nombreCompleto: 'Agosto' }, { numero: 9, nombre: 'Sep', nombreCompleto: 'Septiembre' },
  { numero: 10, nombre: 'Oct', nombreCompleto: 'Octubre' }, { numero: 11, nombre: 'Nov', nombreCompleto: 'Noviembre' }, { numero: 12, nombre: 'Dic', nombreCompleto: 'Diciembre' }
]
const colors = ['#0f766e', '#e4572e', '#55a630', '#c43ab5', '#2563eb', '#b7791f', '#6b46c1', '#0891b2']
const statusSeries = [
  { key: 'confirmadas', label: 'Confirmadas', color: '#0f766e' },
  { key: 'atendidas', label: 'Atendidas', color: '#2563eb' },
  { key: 'canceladas', label: 'Canceladas', color: '#e4572e' }
]
const plot = { left: 58, right: 1180, top: 24, bottom: 404 }
const alcance = computed(() => auth.rol === 'MEDICO_TERAPISTA'
  ? 'Se muestran únicamente las citas asociadas a tu perfil profesional.'
  : 'Vista consolidada de todos los médicos y terapistas.')
const etiquetaEstado = computed(() => ({ total: 'citas', confirmadas: 'confirmadas', atendidas: 'atendidas', canceladas: 'canceladas' })[estadoSeleccionado.value])
const nombreMesSeleccionado = computed(() => meses[mesSeleccionado.value - 1]?.nombreCompleto || '')
const categoriasGrafico = computed(() => periodo.value === 'anual'
  ? meses.map(mes => ({ key: mes.numero, label: mes.nombre, mes }))
  : (reporte.value?.medicos || []).map(medico => ({ key: medico.medicoId, label: medico.medicoNombre, medico })))
const seriesGrafico = computed(() => periodo.value === 'anual'
  ? (reporte.value?.medicos || []).map((medico, index) => ({ key: medico.medicoId, label: medico.medicoNombre, medico, color: colorMedico(index) }))
  : statusSeries)
const valoresVisibles = computed(() => categoriasGrafico.value.flatMap(categoria => seriesGrafico.value.map(serie => valorGrafico(categoria, serie))))
const maximo = computed(() => Math.max(1, ...valoresVisibles.value))
const altoPlot = plot.bottom - plot.top
const anchoGrupo = computed(() => (plot.right - plot.left) / Math.max(categoriasGrafico.value.length, 1))
const anchoBarra = computed(() => Math.max(2, Math.min(22, (anchoGrupo.value * 0.82) / Math.max(seriesGrafico.value.length, 1) * 0.64)))
const ticks = computed(() => [0, Math.ceil(maximo.value / 2), maximo.value].filter((v, i, arr) => arr.indexOf(v) === i).map(value => ({
  value,
  y: plot.bottom - (value / maximo.value) * altoPlot
})))
const descripcionGrafico = computed(() => periodo.value === 'anual'
  ? `Gráfico anual de ${etiquetaEstado.value} por mes y profesional para el año ${reporte.value?.anio ?? anio.value}.`
  : `Gráfico mensual de citas confirmadas, atendidas y canceladas por profesional para ${nombreMesSeleccionado.value} de ${reporte.value?.anio ?? anio.value}.`)

onMounted(consultar)

async function consultar() {
  const id = ++requestId.value
  error.value = ''
  cargando.value = false
  if (!Number.isInteger(anio.value) || anio.value < 1900 || anio.value > 2100) {
    reporte.value = null
    error.value = 'Ingresa un año entre 1900 y 2100.'
    return
  }
  cargando.value = true
  try {
    const { data } = await api.get('/citas/reporte-anual', { params: { anio: anio.value } })
    if (id === requestId.value) reporte.value = data
  } catch (e) {
    if (id === requestId.value) {
      reporte.value = null
      error.value = e.response?.data?.message || 'No fue posible generar el reporte. Intenta nuevamente.'
    }
  } finally {
    if (id === requestId.value) cargando.value = false
  }
}

function valorGrafico(categoria, serie) {
  if (periodo.value === 'anual') {
    const mes = serie.medico.meses[categoria.mes.numero - 1]
    return estadoSeleccionado.value === 'total'
      ? mes.confirmadas + mes.atendidas + mes.canceladas
      : mes[estadoSeleccionado.value]
  }
  const mes = categoria.medico.meses[mesSeleccionado.value - 1]
  return mes[serie.key]
}
function totalPeriodo(medico, estado) {
  const datos = periodo.value === 'anual' ? medico.meses : [medico.meses[mesSeleccionado.value - 1]]
  return datos.reduce((suma, mes) => suma + (estado === 'total' ? mes.confirmadas + mes.atendidas + mes.canceladas : mes[estado]), 0)
}
function totalMes(mes) {
  return mes.confirmadas + mes.atendidas + mes.canceladas
}
function colorMedico(index) {
  return colors[index % colors.length]
}
function xCategoria(index) {
  return plot.left + anchoGrupo.value * (index + 0.5)
}
function xBarra(categoriaIndex, serieIndex) {
  const cantidad = Math.max(seriesGrafico.value.length, 1)
  const grupo = anchoGrupo.value * 0.82
  const inicio = xCategoria(categoriaIndex) - grupo / 2
  return inicio + (grupo / cantidad) * serieIndex + ((grupo / cantidad) - anchoBarra.value) / 2
}
function yBarra(value) {
  return plot.bottom - (value / maximo.value) * altoPlot
}
function altoBarra(value) {
  return value === 0 ? 0 : Math.max(1, (value / maximo.value) * altoPlot)
}
function tituloBarra(categoria, serie) {
  if (periodo.value === 'anual') {
    const mes = serie.medico.meses[categoria.mes.numero - 1]
    return `${serie.label} · ${categoria.mes.nombreCompleto}: ${valorGrafico(categoria, serie)} ${etiquetaEstado.value}. ${mes.confirmadas} confirmadas, ${mes.atendidas} atendidas y ${mes.canceladas} canceladas.`
  }
  return `${categoria.medico.medicoNombre} · ${serie.label}: ${valorGrafico(categoria, serie)} citas en ${nombreMesSeleccionado.value}.`
}
</script>
