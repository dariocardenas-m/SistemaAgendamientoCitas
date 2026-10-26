import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import api from '../services/api'
import { isConfigured, isKeycloak, keycloak } from '../services/identityProvider'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('pa_token') || '')
  const usuario = ref(JSON.parse(localStorage.getItem('pa_user') || 'null'))
  const errorIdentidad = ref('')

  const isAuthenticated = computed(() => isKeycloak
    ? Boolean(keycloak?.authenticated && usuario.value)
    : Boolean(token.value && usuario.value))
  const rol = computed(() => usuario.value?.rol || null)

  function persist(auth) {
    token.value = auth.token
    usuario.value = auth.usuario
    localStorage.setItem('pa_token', auth.token)
    localStorage.setItem('pa_user', JSON.stringify(auth.usuario))
  }

  async function login(email, password) {
    if (isKeycloak) {
      if (!keycloak) throw new Error('Falta configurar la conexión con Keycloak en el archivo .env.local.')
      const redirect = new URLSearchParams(window.location.search).get('redirect') || '/'
      const redirectUri = `${window.location.origin}/login?redirect=${encodeURIComponent(redirect)}`
      await keycloak.login({ loginHint: email || undefined, redirectUri })
      return null
    }
    const { data } = await api.post('/auth/login', { email, password })
    persist(data)
    return data.usuario
  }

  async function registro(payload) {
    if (isKeycloak) {
      throw new Error('En este modo, los usuarios y roles se administran en Keycloak y los perfiles con Postman.')
    }
    const { data } = await api.post('/auth/registro', payload)
    persist(data)
    return data.usuario
  }

  function logout() {
    if (isKeycloak && keycloak?.authenticated) {
      usuario.value = null
      errorIdentidad.value = ''
      return keycloak.logout({ redirectUri: window.location.origin })
    }
    token.value = ''
    usuario.value = null
    localStorage.removeItem('pa_token')
    localStorage.removeItem('pa_user')
  }

  function homePorRol() {
    if (rol.value === 'AGENDADOR') return '/agendador'
    if (rol.value === 'ADMINISTRADOR') return '/admin'
    if (rol.value === 'MEDICO_TERAPISTA') return '/agendador'
    if (rol.value === 'PACIENTE') return '/agendar'
    return '/'
  }

  async function initializeIdentityProvider() {
    if (!isKeycloak) return
    if (!isConfigured) {
      errorIdentidad.value = 'Falta configurar VITE_KEYCLOAK_URL, VITE_KEYCLOAK_REALM y VITE_KEYCLOAK_CLIENT_ID.'
      return
    }
    try {
      await keycloak.init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false })
      if (keycloak.authenticated) {
        const { data } = await api.get('/auth/me')
        usuario.value = data
        errorIdentidad.value = ''
      }
    } catch (error) {
      usuario.value = null
      errorIdentidad.value = error.response?.data?.message
        || 'No se pudo validar la sesión o vincular el perfil local con Keycloak.'
    }
  }

  return {
    token, usuario, isAuthenticated, rol, errorIdentidad,
    usesKeycloak: isKeycloak, login, registro, logout, homePorRol,
    initializeIdentityProvider
  }
})
