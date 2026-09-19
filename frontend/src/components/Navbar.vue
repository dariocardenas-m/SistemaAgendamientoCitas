<template>
  <nav class="sticky top-0 z-40 border-b border-slate-100 bg-white/90 backdrop-blur">
    <div class="mx-auto flex max-w-7xl items-center justify-between px-4 py-3">
      <router-link to="/" class="flex items-center gap-3">
        <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-brand text-white">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6l4 2M9 12a9 9 0 1118 0 9 9 0 01-18 0z" />
          </svg>
        </span>
        <span>
          <span class="block text-sm font-bold text-slate-900">Sistema de Agendamiento</span>
          <span class="block text-xs text-slate-500">Citas Médicas · Salud Unicauca</span>
        </span>
      </router-link>

      <div v-if="auth.isAuthenticated" class="hidden items-center gap-2 md:flex">
        <router-link v-if="auth.rol === 'PACIENTE'" to="/" class="rounded-full px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50">Inicio</router-link>
        <router-link v-if="auth.rol === 'PACIENTE'" to="/agendar" class="rounded-full px-3 py-2 text-sm font-semibold" :class="linkClass('/agendar')">Agendar Cita</router-link>
        <router-link v-if="auth.rol === 'PACIENTE'" to="/mis-citas" class="rounded-full px-3 py-2 text-sm font-semibold" :class="linkClass('/mis-citas')">Mis Citas</router-link>
        <router-link v-if="['AGENDADOR','ADMINISTRADOR'].includes(auth.rol)" to="/agendador" class="rounded-full px-3 py-2 text-sm font-semibold" :class="linkClass('/agendador')">Consulta de Citas</router-link>
        <router-link v-if="auth.rol === 'ADMINISTRADOR'" to="/admin" class="rounded-full px-3 py-2 text-sm font-semibold" :class="linkClass('/admin')">Parametrización</router-link>
      </div>

      <div class="flex items-center gap-2">
        <template v-if="!auth.isAuthenticated">
          <router-link to="/login" class="hidden rounded-full px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 sm:inline">Iniciar Sesión</router-link>
          <router-link to="/registro" class="btn-primary !rounded-full !py-2">Registrarse</router-link>
        </template>
        <template v-else>
          <span class="hidden rounded-full border border-brand/30 bg-brand-light px-3 py-1 text-xs font-semibold text-brand sm:inline">
            Rol: {{ etiquetaRol }}
          </span>
          <div class="hidden text-right sm:block">
            <p class="text-sm font-semibold leading-tight">{{ auth.usuario.nombreCompleto }}</p>
            <p class="text-xs text-slate-500">{{ auth.usuario.email }}</p>
          </div>
          <button class="rounded-full p-2 text-slate-500 hover:bg-slate-100" title="Cerrar sesión" @click="salir">
            ↗
          </button>
        </template>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const etiquetaRol = computed(() => {
  const map = { PACIENTE: 'Paciente', AGENDADOR: 'Agendador', ADMINISTRADOR: 'Administrador', MEDICO_TERAPISTA: 'Médico' }
  return map[auth.rol] || auth.rol
})

function linkClass(path) {
  return route.path.startsWith(path) ? 'bg-brand text-white' : 'text-slate-600 hover:bg-slate-50'
}

function salir() {
  auth.logout()
  router.push('/')
}
</script>
