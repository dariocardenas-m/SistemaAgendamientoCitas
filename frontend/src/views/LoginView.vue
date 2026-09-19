VC<template>
  <div class="flex min-h-[80vh] items-center justify-center px-4 py-10">
    <div class="card w-full max-w-md p-8">
      <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-brand text-2xl text-white">✚</div>
      <h1 class="text-center text-2xl font-extrabold">Iniciar Sesión</h1>
      <p class="mb-6 text-center text-sm text-slate-500">Acceso seguro al Sistema de Agendamiento de Citas Médicas</p>

      <form class="space-y-4" @submit.prevent="enviar">
        <label class="block text-xs font-bold uppercase tracking-wide text-slate-500">Correo electrónico
          <input v-model="email" type="email" class="input-field mt-1" placeholder="ejemplo: juan.perez@email.com" required />
        </label>
        <label class="block text-xs font-bold uppercase tracking-wide text-slate-500">Contraseña
          <input v-model="password" type="password" class="input-field mt-1" required />
        </label>
        <p v-if="error" class="text-sm text-red-500">{{ error }}</p>
        <button class="btn-primary w-full" :disabled="cargando">Iniciar Sesión</button>
      </form>
      <p class="mt-6 text-center text-sm text-slate-500">
        ¿No tienes una cuenta de paciente aún?
        <router-link to="/registro" class="font-semibold text-brand">Registrarse aquí</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const email = ref('')
const password = ref('')
const error = ref('')
const cargando = ref(false)

async function irDestino() {
  const dest = route.query.redirect || auth.homePorRol()
  await router.push(dest)
}

async function enviar() {
  error.value = ''
  cargando.value = true
  try {
    await auth.login(email.value, password.value)
    await irDestino()
  } catch (e) {
    error.value = e.response?.data?.message || 'Credenciales incorrectas.'
  } finally {
    cargando.value = false
  }
}
</script>
