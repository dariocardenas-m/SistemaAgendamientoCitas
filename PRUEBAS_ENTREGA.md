# Pruebas del primer corte — PiedraAzul

La rama `tests-first-sprint` parte de `deployment` (`934e05b`) e incorpora las pruebas y correcciones del primer corte sin modificar la rama de despliegue. La aplicación mantiene PostgreSQL para su ejecución normal; solo `ApiIntegrationTest` usa una base H2 aislada durante la prueba.

## Ejecución verificada

Backend (JDK 22, Maven):

```powershell
cd backend
mvn test
```

Frontend (Node.js, pnpm):

```powershell
cd frontend
pnpm install --frozen-lockfile
pnpm test
pnpm build
```

## Resultados del 28/09/2026

| Suite | Casos | Aprobados | Fallidos |
|---|---:|---:|---:|
| `CitaServiceTest` | 29 | 29 | 0 |
| `AuthServiceTest` | 8 | 8 | 0 |
| `ConfiguracionTest` | 20 | 20 | 0 |
| `JwtServiceTest` | 4 | 4 | 0 |
| `DominioTest` | 7 | 7 | 0 |
| `ApiIntegrationTest` | 12 | 12 | 0 |
| `stores.test.js` | 15 | 15 | 0 |
| **Total** | **95** | **95** | **0** |

`pnpm build` también terminó correctamente. JaCoCo informó 714 de 792 líneas (90,2 %) y 89 de 118 ramas (75,4 %) cubiertas en el backend. La cobertura es una medida de ejecución, no una garantía de corrección.

Las pruebas cubren reglas de disponibilidad y reserva, conflictos, cancelación, autenticación, JWT, configuración, DTO/entidades y estado del frontend. La suite de integración incluye una prueba concurrente con dos solicitudes sobre la misma franja: solo una debe confirmarse.

Los informes reproducibles quedan en `backend/target/surefire-reports` y `backend/target/site/jacoco/index.html` después de ejecutar `mvn test`.

## Alcance y límites

- Las 12 pruebas de integración usan H2 con esquema generado para pruebas; **no** validan una instalación PostgreSQL real ni el script `db/postgresql/schema.sql`.
- Tres pruebas de regresión que fallaban en el clon local fueron retiradas por el equipo antes de esta integración. No se incluyen en los 95 casos aprobados y no se debe afirmar que esos tres escenarios estén verificados por esta suite.
- El frontend tiene pruebas de stores y compilación exitosa; esto no reemplaza una prueba de uso con participantes.
- El video, el tablero Jira y los commits publicados de todos los integrantes son evidencias de entrega separadas de estas pruebas.
