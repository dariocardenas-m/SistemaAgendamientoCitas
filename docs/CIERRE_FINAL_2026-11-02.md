# Cierre técnico de la segunda entrega

El cierre incorpora `jors-desarrollo` (44e5646) sobre la versión validada 616fb3c, conserva sus autores y añade pruebas, trazabilidad y una corrección del arranque OIDC. La versión de entrega se publica en `avance-Cardenas`; no se modifican main ni deployment.

## Comprobado

- `mvn test` con Java 17: **57 pruebas**, cero fallos, errores y omitidas. Surefire incluye Test e IT por defecto.
- JaCoCo: **80,96 % de líneas del backend** (948/1171), 56,54 % de ramas (173/306), sobre 54 clases. La matriz distingue unitarias Mockito, integración H2 y comprobaciones externas; no declara 100 %.
- `npm run build`: correcto con la pantalla MisCitasView de Jors y el cambio de inicialización OIDC.
- Navegador real, paciente ficticio: consulta, filtros, vacío, error de red con Reintentar, cancelación confirmada y botón temporalmente deshabilitado. La cita CITA-2026-1385 creada para la revisión terminó Cancelada.
- Se reprodujo que el router conservaba en la URL parámetros de retorno OIDC; al cargar el router después de initializeIdentityProvider, el mismo acceso terminó en `/mis-citas` sin code/state/iss.
- Colección Postman ejecutada mediante Newman contra Keycloak real: **11 peticiones, 16 aserciones, cero fallos**. Crea/consulta usuario ficticio, asigna/revoca PACIENTE, elimina el usuario, confirma 404 y verifica 401 sin autenticación. Secretos fuera del repositorio; entorno público vacío.
- Se conserva la evidencia previa de 25 comprobaciones HTTP Keycloak/MySQL reales y el reescaneo ZAP. Las 25 comprobaciones no se suman a JUnit ni a Newman.

## Fuentes

- [Matriz de pruebas y alcance](MATRIZ_PRUEBAS_CIERRE.md).
- [Resumen de ejecución y cobertura](evidencias-cierre/resumen-cierre.json).
- [Registro JUnit](evidencias-cierre/resultado-junit-2026-11-02.txt).
- [Contadores JaCoCo por clase](evidencias-cierre/jacoco-backend.csv).
- [Colección y guía Postman/Newman](postman/LEEME.md).
- [Reescaneo ZAP](REESCANEO_ZAP_2026-11-02.md).
- [Documento de arquitectura](https://docs.google.com/document/d/1My4xHymRjkCn2dBd-PxHgA55EkNoyUUQuEAKSpOf4Bw/edit?tab=t.0).

## Configuración y límites

El frontend normal está en localhost:5173, la API en 8080, Keycloak en 8081 y la base de auditoría MySQL separada en 3317. El ensayo inicial en 5174 recibió el rechazo CORS esperado al intentar modificar datos desde un origen no permitido; la prueba se repitió en 5173 sin ampliar CORS. Se retiró el redirect temporal de Keycloak para 5174.

La revisión no acredita conformidad WCAG completa, todas las pantallas/roles, carga ni producción. Los contratos JPA se comprobaron en H2; no son una certificación de todas las particularidades de MySQL. Permanece documentado el hallazgo medio ZAP 10055 por estilos en línea. No se inventaron participantes ni mediciones: la evaluación de usabilidad sigue siendo Gestalt de ocho capturas y seis principios.

Para completar la entrega administrativa, el equipo debe grabar y publicar el video de 14 minutos sin acelerarlo, incorporar su URL al documento y comprobar el acceso del profesor. Sebastián dispone de 6 minutos: arquitectura2, pruebas1, Git/Jira1 y GoF2. La colección ejecutada es evidencia de gestión por la API de Keycloak; no se presenta como captura de la interfaz de Postman.
