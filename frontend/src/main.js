import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { useAuthStore } from './stores/authStore'
import './style.css'

async function startApp() {
  const app = createApp(App)
  const pinia = createPinia()
  app.use(pinia)
  await useAuthStore(pinia).initializeIdentityProvider()
  // Keycloak debe consumir y retirar el fragmento OIDC antes de que Vue Router
  // tome la URL inicial; de lo contrario puede reintroducir code/state en la ruta.
  const { default: router } = await import('./router')
  app.use(router)
  app.mount('#app')
}

startApp()
