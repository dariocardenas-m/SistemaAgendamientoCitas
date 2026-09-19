import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import api from '../services/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('pa_token') || '')
  const usuario = ref(JSON.parse(localStorage.getItem('pa_user') || 'null'))

  const isAuthenticated = computed(() => Boolean(token.value && usuario.value))
  const rol = computed(() => usuario.value?.rol || null)

  function persist(auth) {
    token.value = auth.token
    usuario.value = auth.usuario
    localStorage.setItem('pa_token', auth.token)
    localStorage.setItem('pa_user', JSON.stringify(auth.usuario))
  }

  async function login(email, password) {
    const { data } = await api.post('/auth/login', { email, password })
    persist(data)
    return data.usuario
  }

  async function registro(payload) {
    const { data } = await api.post('/auth/registro', payload)
    persist(data)
    return data.usuario
  }

  function logout() {
    token.value = ''
    usuario.value = null
    localStorage.removeItem('pa_token')
    localStorage.removeItem('pa_user')
  }

  function homePorRol() {
    if (rol.value === 'AGENDADOR') return '/agendador'
    if (rol.value === 'ADMINISTRADOR') return '/admin'
    if (rol.value === 'PACIENTE') return '/agendar'
    return '/'
  }

  return { token, usuario, isAuthenticated, rol, login, registro, logout, homePorRol }
})
