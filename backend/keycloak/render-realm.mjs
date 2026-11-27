import { mkdir, readFile, writeFile } from 'node:fs/promises'

const realm = JSON.parse(await readFile(new URL('piedrazul-realm.json', import.meta.url), 'utf8'))
const webClient = realm.clients.find(client => client.clientId === 'piedrazul-app')
if (!webClient) throw new Error('El realm no contiene el cliente piedrazul-app.')

const appOrigin = '${PIEDRAZUL_WEB_ORIGIN}'
const localOrigin = 'http://localhost:5173'
realm.sslRequired = 'all'
webClient.redirectUris = webClient.redirectUris.map(uri => uri.replace(localOrigin, appOrigin))
webClient.webOrigins = [appOrigin]
webClient.attributes['post.logout.redirect.uris'] = `${appOrigin}/`

if (process.env.PIEDRAZUL_IMPORT_DEMO_USERS === 'true') {
  realm.resetPasswordAllowed = false
  realm.bruteForceProtected = true
  realm.users = [
    demoUser('admin', 'Sofía', 'Alarcón', 'ADMINISTRADOR', 'DEMO_ADMIN_PASSWORD'),
    demoUser('carlos.mendoza', 'Carlos', 'Mendoza', 'AGENDADOR', 'DEMO_AGENDADOR_PASSWORD'),
    demoUser('juan.perez', 'Juan', 'Pérez', 'PACIENTE', 'DEMO_PATIENT_PASSWORD'),
    demoUser('laura.martinez', 'Laura', 'Martínez', 'MEDICO_TERAPISTA', 'DEMO_MEDICO_PASSWORD')
  ]
}

await mkdir(new URL('cloud/', import.meta.url), { recursive: true })
await writeFile(new URL('cloud/piedrazul-realm.json', import.meta.url), JSON.stringify(realm, null, 2) + '\n')

function demoUser(username, firstName, lastName, role, passwordVariable) {
  return {
    username,
    email: `${username}@example.com`,
    firstName,
    lastName,
    enabled: true,
    emailVerified: true,
    realmRoles: [role],
    credentials: [{ type: 'password', value: '${' + passwordVariable + '}', temporary: false }]
  }
}
