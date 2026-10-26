import Keycloak from 'keycloak-js'

const provider = import.meta.env.VITE_AUTH_PROVIDER || 'local'
const isKeycloak = provider === 'keycloak'
const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID
}
const isConfigured = Object.values(keycloakConfig).every(Boolean)

const keycloak = isKeycloak && isConfigured
  ? new Keycloak(keycloakConfig)
  : null

async function accessToken() {
  if (!isKeycloak) return localStorage.getItem('pa_token') || ''
  if (!keycloak?.authenticated) return ''
  await keycloak.updateToken(30)
  return keycloak.token || ''
}

export { accessToken, isKeycloak, isConfigured, keycloak }
