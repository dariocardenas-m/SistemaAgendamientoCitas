El Sistema de Agendamiento de Citas Médicas de la Red Piedra Azul es un monolito modular (Spring Boot 3 + Vue 3) para registro de pacientes, reserva autónoma, consulta de agenda y parametrización de disponibilidad.

## Cómo ejecutar

### Backend (Java 17, puerto 8080)
Desde `backend/` (requiere Maven instalado):

```bash
mvn spring-boot:run
```

La consola H2 está desactivada por defecto. Para habilitarla solo en desarrollo, inicia el backend con `H2_CONSOLE_ENABLED=true`; URL: http://localhost:8080/h2-console (JDBC `jdbc:h2:mem:piedrazul`, usuario `sa`).

### Frontend (Node 18+, puerto 5173)
Desde `frontend/`:

```bash
npm install
npm run dev
```

Abra http://localhost:5173

### Persistencia con MySQL 8

El perfil predeterminado usa H2 en memoria para desarrollo y pruebas. Para conservar los datos al reiniciar, activa el perfil `mysql`. MySQL debe estar iniciado localmente en el puerto `3307` (o ajusta `DB_URL` si usas otro puerto). Conecta la aplicación con un usuario limitado a la base, como `piedrazul_app`, no con `root`.

1. En MySQL crea la base de datos (o deja que el usuario configurado la cree si tiene permisos):

```sql
CREATE DATABASE piedrazul CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'piedrazul_app'@'localhost' IDENTIFIED WITH caching_sha2_password BY 'DEFINE_UNA_CLAVE_LOCAL';
GRANT ALL PRIVILEGES ON piedrazul.* TO 'piedrazul_app'@'localhost';
```

2. En CMD, desde `backend/`, configura las credenciales para esa terminal y ejecuta Spring Boot:

```cmd
set "DB_USERNAME=piedrazul_app"
set "DB_PASSWORD=TU_CLAVE_DE_MYSQL"
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

También puedes definir `DB_URL` si MySQL usa otro host, puerto o nombre de base. El esquema se actualiza con Hibernate (`ddl-auto: update`) y los datos de demostración se insertan solo cuando la tabla de usuarios está vacía. No subas credenciales ni archivos `.env` a GitHub.

Para el perfil local, `APP_JWT_SECRET` permite reemplazar la clave de desarrollo del token. Define una clave propia fuera del repositorio antes de desplegar el sistema.

### Autenticación con Keycloak

El perfil local conserva el inicio de sesión propio para desarrollo. El perfil `keycloak` valida access tokens JWT emitidos por Keycloak y aplica roles de realm. La configuración inicial del realm, sus cuatro roles y el cliente OIDC público están en `backend/keycloak/piedrazul-realm.json`. El cliente usa Authorization Code con PKCE S256; no habilita el flujo implícito ni el envío de contraseñas.

1. Descarga y descomprime Keycloak 26.x. Copia `backend/keycloak/piedrazul-realm.json` a la carpeta `data/import` de Keycloak.
2. En CMD, desde la carpeta de Keycloak, define credenciales locales de administrador e inicia el servidor:

```cmd
set "KC_BOOTSTRAP_ADMIN_USERNAME=admin"
set "KC_BOOTSTRAP_ADMIN_PASSWORD=CAMBIA_ESTA_CLAVE"
bin\kc.bat start-dev --http-port 8081 --import-realm
```

3. Crea usuarios en el realm `piedrazul` desde la consola de administración o la Admin REST API de Keycloak. Asigna a cada cuenta un solo rol de aplicación (`PACIENTE`, `AGENDADOR`, `MEDICO_TERAPISTA` o `ADMINISTRADOR`) y verifica el correo.
4. Copia `frontend/.env.example` a `frontend/.env.local` y cambia `VITE_AUTH_PROVIDER=local` por `VITE_AUTH_PROVIDER=keycloak`.
5. En otra terminal CMD, inicia el backend con MySQL y Keycloak:

```cmd
set "DB_USERNAME=piedrazul_app"
set "DB_PASSWORD=TU_CLAVE_DE_MYSQL"
set "KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/piedrazul"
set "APP_REQUIRE_HTTPS=false"
set "SPRING_PROFILES_ACTIVE=mysql,keycloak"
mvn spring-boot:run
```

`APP_REQUIRE_HTTPS=false` es solo para desarrollo local por HTTP; en despliegue real debe usarse HTTPS. La web inicia sesión en Keycloak y envía el access token al API. El API comprueba emisor, firma, expiración y audiencia `piedrazul-api`; solo acepta un rol del sistema por token. Los perfiles demo existentes se vinculan a la identidad por correo verificado y luego quedan asociados al `sub` de Keycloak.

Para un paciente nuevo, crea en Keycloak el usuario con rol `PACIENTE`, obtén un token con Postman mediante Authorization Code + PKCE y llama `POST /api/keycloak/perfil-paciente` con ese bearer y JSON `{ "nombreCompleto": "Nombre", "documento": "12345678", "telefono": "3001234567" }`. Para crear un perfil local de `ADMINISTRADOR` o `AGENDADOR`, llama `POST /api/keycloak/perfil-basico` con su token y JSON `{ "nombreCompleto": "Nombre" }`. El rol y el correo se obtienen del JWT, no del cuerpo. Para un médico nuevo también se necesita un registro profesional `Medico`; por ahora vincula usuarios Keycloak a los profesionales demo existentes por correo.

En Postman configura OAuth 2.0 Authorization Code (With PKCE): client `piedrazul-app`, Authorization URL `http://localhost:8081/realms/piedrazul/protocol/openid-connect/auth`, Access Token URL `http://localhost:8081/realms/piedrazul/protocol/openid-connect/token`, callback `https://oauth.pstmn.io/v1/browser-callback`, scope `openid` y challenge `SHA-256`.

Para administrar cuentas con la Admin REST API desde Postman, crea en la consola de Keycloak un cliente confidencial `piedrazul-postman-admin` con Service Account habilitada y asígnale los roles de `realm-management` `query-users`, `view-users`, `manage-users` y `view-realm`. Guarda el client secret solo en un entorno privado de Postman. Obtén un token con `client_credentials` en `POST http://localhost:8081/realms/piedrazul/protocol/openid-connect/token`; crea usuarios con `POST /admin/realms/piedrazul/users`, busca el usuario y el rol por sus endpoints `GET`, y asigna el rol con `POST /admin/realms/piedrazul/users/{userId}/role-mappings/realm` usando el JSON del rol. No guardes tokens, claves ni client secrets en la colección o el repositorio.

## Cuentas locales demo

El perfil local crea estas cuentas al iniciar una base vacía:

- Paciente: `juan.perez@email.com` (contraseña `paciente123`)
- Agendador: `carlos.mendoza@piedrazul.com` (contraseña `agendador123`)
- Administrador: `admin@piedrazul.com` (contraseña `admin123`)
- Médicos: `laura.martinez@piedrazul.com`, `carlos.gomez@piedrazul.com`, `ana.rodriguez@piedrazul.com`, `maria.torres@piedrazul.com`, `andres.molina@piedrazul.com` y `lucia.herrera@piedrazul.com` (contraseña `medico123`)

Son credenciales de demostración; cámbialas antes de exponer el sistema. En el perfil Keycloak, las identidades y roles se administran en Keycloak; las contraseñas locales no autentican.

La reserva de citas usa bloqueo pesimista (`PESSIMISTIC_WRITE`) sobre el médico y las citas del día; un choque de franja responde HTTP 409.
