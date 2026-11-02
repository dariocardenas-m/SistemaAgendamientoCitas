# Matriz de pruebas y alcance del cierre

Fecha: 2 de noviembre de 2026. Base: `jors-desarrollo` 44e5646, incluyendo las mejoras de interfaz y el historial de los tres integrantes. Las pruebas añadidas no modifican las reglas de negocio.

## Resultado reproducible

Desde backend, con Java 17: `mvn test`.

El POM incluye ahora `*Test` y `*IT` en Surefire por defecto y genera el reporte JaCoCo al terminar. Resultado de esta revisión: **57 pruebas, cero fallos, cero errores, cero omitidas**. El informe HTML se encuentra en `backend/target/site/jacoco/index.html`; los contadores XML y CSV están junto a él. Los reportes se generan al ejecutar, no se versiona target.

Cobertura de líneas de las 54 clases del backend analizadas por JaCoCo: **948/1171 = 80,96 %**. Ramas: **173/306 = 56,54 %**. No son porcentajes de cumplimiento de requisitos, cobertura frontend ni cobertura de todos los caminos posibles.

## Trazabilidad por servicio

| Servicio | Evidencia | Tipo y alcance |
|---|---|---|
| CitaService | CitaServiceEditarTest, CitaServiceReactivarTest, CitaEditarIT, CitaExportacionHistorialTest | Unitarias con Mockito e integración: edición, acceso a cita ajena, conflictos, reactivación, consulta, historial y permisos. No todas sus ramas están cubiertas. |
| CitaCsvExportService | CitaCsvExportServiceTest y CitaExportacionHistorialTest | Unitarias: agenda vacía, BOM/UTF-8, comillas, comas, saltos de línea y prefijos de fórmulas; integración con exportación filtrada. |
| AuthService | AuthServiceTest | Ocho casos unitarios: login válido, desconocido, clave errónea, inactivo, registro cifrado y normalizado, duplicados y contexto de identidad. Es la autenticación local conservada, distinta del acceso OIDC. |
| PerfilBasicoKeycloakService | PerfilesKeycloakServiceTest | Creación y vinculación; rechazo sin JWT, rol incorrecto, correo no verificado e identidad deshabilitada. |
| PerfilPacienteKeycloakService | PerfilesKeycloakServiceTest | Roles, correo verificado, evitar apropiación de cuenta por correo, documento ocupado, idempotencia y creación de perfil. |
| JwtService | JwtServiceTest | Firma/claims locales, rechazo con otra clave, vencimiento y formato inválido. No sustituye la validación real de tokens de Keycloak. |

## Entidades y persistencia

Las entidades JPA Usuario, Paciente, Medico, Especialidad, Cita y ConfiguracionGlobal son principalmente datos y relaciones. `DominioPersistenciaIT` verifica con H2 sus contratos de persistencia: recorrido completo de relaciones y proyección de CitaDto, correo único, documento único, una cuenta por perfil paciente, especialidad única, médico con especialidad obligatoria, código de cita único y ventana no nula. Son **ocho pruebas de integración**, no pruebas unitarias de getters/setters ni una validación exhaustiva de todas las restricciones de MySQL.

Las clases auxiliares y DTO no se declaran cubiertas completamente. El reporte conserva las líneas y ramas no recorridas para una revisión posterior.

## Seguridad, integración externa e interfaz

- `KeycloakAuthenticationIT`: JwtDecoder simulado y H2. No requiere Keycloak instalado para ejecutar esta suite.
- `KeycloakJwtAuthenticationConverterTest` y `KeycloakRealmImportTest`: rol de aplicación y configuración de audiencia.
- La matriz anterior de 25 casos HTTP con Keycloak y MySQL reales se conserva por separado. No se suman esos casos a las 57 pruebas JUnit.
- `docs/postman`: ejecución de once peticiones y dieciséis aserciones Newman contra Keycloak real para gestionar un usuario y sus roles. Sin secretos en Git.
- La compilación frontend incluye MisCitasView de Jors. Las capturas y la revisión de interfaz se conservan en `docs/evidencias-cierre`.

## Límites

No se acredita cobertura del 100 %, conformidad WCAG completa, prueba de carga, seguridad absoluta ni validación del entorno productivo. Permanece como deuda el hallazgo ZAP 10055 de estilos en línea. El vídeo y su URL corresponden al equipo.
