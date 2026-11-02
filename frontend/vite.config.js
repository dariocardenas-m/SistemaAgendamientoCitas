import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode, command, isPreview }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  const identityOrigin = env.VITE_KEYCLOAK_URL
    ? new URL(env.VITE_KEYCLOAK_URL).origin
    : ''
  if (identityOrigin && !/^https?:\/\//.test(identityOrigin)) {
    throw new Error('VITE_KEYCLOAK_URL debe usar HTTP o HTTPS')
  }
  const development = command === 'serve' && !isPreview
  const connectSources = ["'self'", identityOrigin]
  if (development) connectSources.push('ws://localhost:5173', 'ws://127.0.0.1:5173')
  const policy = [
    "default-src 'self'",
    "script-src 'self'",
    // Vue usa estilos dinámicos y Vite inserta estilos durante HMR.
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    "font-src 'self' https://fonts.gstatic.com",
    "img-src 'self' data: blob:",
    `connect-src ${connectSources.filter(Boolean).join(' ')}`,
    `frame-src ${identityOrigin || "'none'"}`,
    "frame-ancestors 'none'",
    "object-src 'none'",
    "base-uri 'self'",
    `form-action 'self'${identityOrigin ? ` ${identityOrigin}` : ''}`
  ].join('; ')
  const headers = {
    'Content-Security-Policy': policy,
    'X-Frame-Options': 'DENY',
    'X-Content-Type-Options': 'nosniff',
    'Referrer-Policy': 'strict-origin-when-cross-origin'
  }
  return {
    plugins: [vue()],
    server: {
      headers,
      port: 5173,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    preview: { headers }
  }
})
