El Sistema de Agendamiento de Citas Médicas de la Red Piedra Azul es un monolito modular (Spring Boot 3 + Vue 3) para registro de pacientes, reserva autónoma, consulta de agenda y parametrización de disponibilidad.

## Cómo ejecutar

### Backend (Java 17, puerto 8080)
Desde `backend/`:

```bash
.\mvnw.cmd spring-boot:run
```

Consola H2: http://localhost:8080/h2-console (JDBC `jdbc:h2:mem:piedrazul`, usuario `sa`).

### Frontend (Node 18+, puerto 5173)
Desde `frontend/`:

```bash
npm install
npm run dev
```

Abra http://localhost:5173

## Cuentas demo (contraseña `123456`)
- Paciente: `paciente@demo.com` (Juan Pérez)
- Agendador: `agendador@demo.com` (Carlos Mendoza)
- Administrador: `admin@demo.com` (Dra. Sofía Alarcón)

La reserva de citas usa bloqueo pesimista (`PESSIMISTIC_WRITE`) sobre el médico y las citas del día; un choque de franja responde HTTP 409.
