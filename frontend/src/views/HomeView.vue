<template>
  <div>
    <section class="mx-auto max-w-7xl px-4 py-8">
      <div class="overflow-hidden rounded-3xl bg-gradient-to-br from-[#043c3a] via-[#0b5c56] to-[#00A887] p-8 text-white shadow-card md:p-12">
        <span class="inline-flex rounded-full bg-white/15 px-3 py-1 text-xs font-semibold">Red Médica Piedra Azul</span>
        <h1 class="mt-5 max-w-3xl text-4xl font-extrabold leading-tight md:text-5xl">Sistema de Agendamiento de Citas Médicas</h1>
        <p class="mt-4 max-w-2xl text-sm text-emerald-50 md:text-base">
          Gestiona y agenda tus citas médicas de manera sencilla, rápida y segura. Diseñado para optimizar la atención de pacientes, la consulta de agendadores y la parametrización médica.
        </p>
        <div class="mt-6 flex flex-wrap gap-3">
          <router-link to="/login" class="rounded-full bg-white/15 px-5 py-3 text-sm font-semibold hover:bg-white/25">Iniciar Sesión</router-link>
          <router-link to="/registro" class="rounded-full bg-white px-5 py-3 text-sm font-semibold text-brand-dark">Registrarse como Paciente</router-link>
          <router-link to="/login" class="rounded-full bg-brand px-5 py-3 text-sm font-semibold">Solicitar una cita</router-link>
        </div>
      </div>

      <h2 class="mt-12 text-center text-2xl font-extrabold">Módulos del Sistema y Roles Integrados</h2>
      <p class="mb-8 text-center text-sm text-slate-500">Plataforma diseñada para garantizar alta usabilidad, seguridad y parametrización médica flexible.</p>
      <div class="grid gap-5 md:grid-cols-3">
        <article class="card p-6">
          <p class="text-xs font-bold uppercase tracking-wide text-brand">Módulo Paciente</p>
          <h3 class="mt-2 text-lg font-bold">Agendamiento Autónomo</h3>
          <p class="mt-2 text-sm text-slate-500">Flujo intuitivo de 5 pasos para consultar especialidades, médicos disponibles, fechas hábiles y franjas horarias libres.</p>
          <ul class="mt-4 space-y-1 text-sm text-slate-600">
            <li>✓ Registro y autenticación de pacientes</li>
            <li>✓ Selección por especialidad y médico</li>
            <li>✓ Comprobante de cita con código único</li>
          </ul>
          <router-link to="/login" class="mt-5 inline-block text-sm font-semibold text-brand">Iniciar sesión para acceder →</router-link>
        </article>
        <article class="card p-6">
          <p class="text-xs font-bold uppercase tracking-wide text-blue-600">Módulo Agendador</p>
          <h3 class="mt-2 text-lg font-bold">Consulta y Gestión de Citas</h3>
          <p class="mt-2 text-sm text-slate-500">Módulo operativo para secretaría y agendadores de citas. Consulta agendas por profesional y fecha en tiempo real.</p>
          <ul class="mt-4 space-y-1 text-sm text-slate-600">
            <li>✓ Filtro por profesional, especialidad y fecha</li>
            <li>✓ Conteo total y desglose de citas</li>
            <li>✓ Tabla detallada con estados de cita</li>
          </ul>
          <router-link to="/login" class="mt-5 inline-block text-sm font-semibold text-blue-600">Iniciar sesión para acceder →</router-link>
        </article>
        <article class="card p-6">
          <p class="text-xs font-bold uppercase tracking-wide text-violet-600">Módulo Administrador</p>
          <h3 class="mt-2 text-lg font-bold">Parametrización del Sistema</h3>
          <p class="mt-2 text-sm text-slate-500">Control total de semanas disponibles, días de atención, horarios de apertura y cierre, e intervalos entre consultas (15 a 60 min).</p>
          <ul class="mt-4 space-y-1 text-sm text-slate-600">
            <li>✓ Ventana de tiempo (1 a 12 semanas)</li>
            <li>✓ Configuración individual por profesional</li>
            <li>✓ Simulador de disponibilidad en vivo</li>
          </ul>
          <router-link to="/login" class="mt-5 inline-block text-sm font-semibold text-violet-600">Iniciar sesión para acceder →</router-link>
        </article>
      </div>

      <div class="mt-10 card p-6">
        <div class="mb-4 flex items-center justify-between">
          <div>
            <h3 class="text-lg font-bold">Equipo Médico y Profesionales Disponibles</h3>
            <p class="text-sm text-slate-500">Especialistas registrados en el sistema de salud</p>
          </div>
          <router-link to="/agendar" class="text-sm font-semibold text-brand">Agendar con un profesional →</router-link>
        </div>
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <div v-for="m in medicos.slice(0,4)" :key="m.id" class="flex items-center gap-3 rounded-2xl border border-slate-100 p-3">
            <div class="flex h-12 w-12 items-center justify-center rounded-full bg-brand-light font-bold text-brand">
              {{ iniciales(m.nombreCompleto) }}
            </div>
            <div>
              <p class="text-sm font-bold">{{ m.nombreCompleto }}</p>
              <p class="text-xs text-slate-500">{{ m.especialidad }} · {{ m.aniosExperiencia }} años</p>
              <p class="text-[11px] text-slate-400">{{ m.consultorio }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'

const medicos = ref([])

onMounted(async () => {
  const { data } = await api.get('/medicos')
  medicos.value = data
})

function iniciales(nombre) {
  return nombre.split(' ').filter(Boolean).slice(-2).map(p => p[0]).join('').slice(0, 2)
}

</script>
