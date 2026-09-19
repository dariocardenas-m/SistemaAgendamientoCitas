<template>
  <div class="flex min-h-[80vh] items-center justify-center px-4 py-10">
    <div class="card w-full max-w-lg p-8">
      <div class="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-brand text-2xl text-white">✚</div>
      <p class="mb-2 text-center text-xs font-semibold text-brand">Registro de paciente</p>
      <h1 class="text-center text-2xl font-extrabold">Registro de Paciente</h1>
      <p class="mb-6 text-center text-sm text-slate-500">Crea tu cuenta para agendar y gestionar tus citas médicas en línea</p>
      <form class="space-y-4" @submit.prevent="enviar">
        <label class="block text-xs font-bold uppercase text-slate-500">Nombre completo *
          <input v-model="form.nombreCompleto" class="input-field mt-1" placeholder="Ej: Laura Gómez Sánchez" required />
        </label>
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="block text-xs font-bold uppercase text-slate-500">Número de documento *
            <input v-model="form.documento" class="input-field mt-1" placeholder="Ej: 1061890123" required />
          </label>
          <label class="block text-xs font-bold uppercase text-slate-500">Teléfono de contacto *
            <input v-model="form.telefono" class="input-field mt-1" placeholder="Ej: 315 123 4567" required />
          </label>
        </div>
        <label class="block text-xs font-bold uppercase text-slate-500">Correo electrónico *
          <input v-model="form.email" type="email" class="input-field mt-1" placeholder="paciente@correo.com" required />
        </label>
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="block text-xs font-bold uppercase text-slate-500">Contraseña *
            <input v-model="form.password" type="password" class="input-field mt-1" minlength="6" required />
          </label>
          <label class="block text-xs font-bold uppercase text-slate-500">Confirmar contraseña *
            <input v-model="confirm" type="password" class="input-field mt-1" minlength="6" required />
          </label>
        </div>
        <p v-if="error" class="text-sm text-red-500">{{ error }}</p>
        <button class="btn-primary w-full" :disabled="cargando">Crear cuenta</button>
      </form>
      <p class="mt-6 text-center text-sm text-slate-500">
        ¿Ya tienes una cuenta registrada?
        <router-link to="/login" class="font-semibold text-brand">Iniciar sesión</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const auth = useAuthStore()
const router = useRouter()
const confirm = ref('')
const error = ref('')
const cargando = ref(false)
const form = reactive({
  nombreCompleto: '',
  documento: '',
  telefono: '',
  email: '',
  password: ''
})

async function enviar() {
  error.value = ''
  if (form.password !== confirm.value) {
    error.value = 'Las contraseñas no coinciden.'
    return
  }
  cargando.value = true
  try {
    await auth.registro(form)
    router.push('/agendar')
  } catch (e) {
    error.value = e.response?.data?.message || 'No fue posible completar el registro.'
  } finally {
    cargando.value = false
  }
}
</script>
