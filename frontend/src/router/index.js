import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { guest: true } },
  { path: '/registro', name: 'registro', component: () => import('../views/RegisterView.vue'), meta: { guest: true } },
  {
    path: '/agendar',
    name: 'agendar',
    component: () => import('../views/AgendarCitaView.vue'),
    meta: { auth: true, roles: ['PACIENTE', 'ADMINISTRADOR'] }
  },
  {
    path: '/mis-citas',
    name: 'mis-citas',
    component: () => import('../views/MisCitasView.vue'),
    meta: { auth: true, roles: ['PACIENTE'] }
  },
  {
    path: '/agendador',
    name: 'agendador',
    component: () => import('../views/AgendadorView.vue'),
    meta: { auth: true, roles: ['AGENDADOR', 'ADMINISTRADOR'] }
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../views/AdminConfigView.vue'),
    meta: { auth: true, roles: ['ADMINISTRADOR'] }
  },
  {
    path: '/admin/configuracion',
    name: 'admin-configuracion',
    component: () => import('../views/AdminConfigView.vue'),
    meta: { auth: true, roles: ['ADMINISTRADOR'] }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && auth.isAuthenticated) {
    return auth.homePorRol()
  }
  if (to.meta.roles && !to.meta.roles.includes(auth.rol)) {
    return auth.homePorRol()
  }
  return true
})

export default router
