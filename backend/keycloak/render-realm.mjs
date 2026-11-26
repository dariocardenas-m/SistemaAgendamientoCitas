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

await mkdir(new URL('cloud/', import.meta.url), { recursive: true })
await writeFile(new URL('cloud/piedrazul-realm.json', import.meta.url), JSON.stringify(realm, null, 2) + '\n')
