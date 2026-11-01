<template>
  <div class="min-h-screen bg-page" :class="`role-theme-${auth.rol || 'anon'}`">
    <Navbar />
    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import Navbar from './components/Navbar.vue'
import { useAuthStore } from './stores/authStore'

const auth = useAuthStore()
</script>
