import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/authStore'
import './style.css'

async function startApp() {
  const app = createApp(App)
  const pinia = createPinia()
  app.use(pinia)
  await useAuthStore(pinia).initializeIdentityProvider()
  app.use(router)
  app.mount('#app')
}

startApp()
