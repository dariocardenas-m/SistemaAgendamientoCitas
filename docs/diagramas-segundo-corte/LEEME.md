# Diagramas de PiedraAzul - segunda entrega

Fecha de revisión: 1 de noviembre de 2026.

Repositorio: https://github.com/dariocardenas-m/SistemaAgendamientoCitas

Rama revisada: `avance-Cardenas`.
Commit base: `7a5b57f5a030b2d236928be96e1e4ff1870a2372`, más las correcciones verificadas de la rama de revisión `codex/segunda-entrega-seguridad-pruebas`.

Cada figura tiene una fuente `.puml`, una imagen `.png` y una versión vectorial `.svg`.
Las imágenes se generaron con PlantUML 1.2026.8; los tres C4 usan su biblioteca estándar C4.

## Correspondencia con el documento compartido

| Figura | Archivo | Sección y propósito |
|---|---|---|
| 1 | 01_c4_contexto | C4 nivel 1: cuatro actores, sistema de agendamiento y proveedor externo Keycloak. |
| 2 | 02_c4_contenedores | C4 nivel 2: SPA Vue, API Spring Boot, MySQL y relaciones OIDC/REST/JDBC. |
| 3 | 03_c4_componentes_backend | C4 nivel 3: seguridad, perfil, citas, CSV, catálogos y repositorios del backend. |
| 4 | 04_vista_logica_clases | Vista lógica: entidades JPA y multiplicidades. |
| 5 | 05_vista_procesos_login_oidc | Vista de procesos: Authorization Code con PKCE, JWT, perfil local y errores. |
| 6 | 06_vista_procesos_csv_historial | Vista de procesos: autorización, CSV, historial y respuestas de error. |
| 7 | 07_vista_desarrollo_paquetes | Vista de desarrollo: organización del código y dependencias seleccionadas. |
| 8 | 08_vista_fisica_despliegue_local | Vista física: configuración local y puertos 5173, 8080, 8081 y 3307. |
| 9 | 09_vista_escenarios_casos_uso | Vista +1: actores y casos de uso HU-18 a HU-22. |

Se actualizaron los espacios de arquitectura del documento existente:
https://docs.google.com/document/d/1My4xHymRjkCn2dBd-PxHgA55EkNoyUUQuEAKSpOf4Bw/edit?tab=t.0

C4 y 4+1 son modelos complementarios. C4 organiza el alcance del sistema; 4+1 reúne perspectivas de estructura, comportamiento, código, despliegue y escenarios. No existe un diagrama UML único llamado "4+1". La vista de procesos contiene dos secuencias.

## Fuentes verificadas

- `README.md`, `backend/pom.xml` y `frontend/package.json`.
- Perfiles `application-mysql.yml`, `application-keycloak.yml` y realm de `backend/keycloak`.
- Entidades `Usuario`, `Paciente`, `Medico`, `Cita`, `Especialidad`, `ConfiguracionGlobal` y enum `Rol`.
- `CitaController`, `CitaService`, `CitaCsvExportService`, `AuthService`, `SeguridadUtil`, repositorios y configuración de seguridad.
- `frontend/src/main.js`, router, `authStore`, `identityProvider`, cliente API y configuración Vite.

## Decisiones y límites

1. MySQL es persistencia interna en C4 contenedores; no se presenta como otro sistema externo en contexto.
2. Keycloak es proveedor externo de identidad. La API valida el JWT con las claves obtenidas por discovery/JWKS; no consulta al proveedor por cada operación de negocio.
3. Los cuatro actores tienen responsabilidades diferentes. Un paciente no recibe acceso al historial administrativo de otra persona.
4. La API se mantiene como monolito modular por paquetes. Los diagramas no afirman aislamiento estricto de módulos ni un despliegue de microservicios.
5. El esquema físico representa los puertos predeterminados de desarrollo, con el proxy de Vite para `/api`. La auditoría usó una instancia MySQL separada en 3317 en lugar de 3307; no se evaluó producción.
6. El perfil Keycloak incorpora `JwtAuthFilter` restringido a `!keycloak`, audiencia personalizada `piedrazul-api` y rechazo de tokens con cero o varios roles de aplicación. La reactivación comprueba solapamientos bajo bloqueo.
7. Pasaron 22 pruebas automatizadas y el build del frontend. Tres casos de seguridad usan `JwtDecoder` simulado y H2; por separado pasaron 25 comprobaciones HTTP con Keycloak 26.8.1 y MySQL 8.0.46 reales. El recorrido visual completo de la SPA sigue pendiente.
8. Se ejecutó OWASP ZAP 2.16.0. El informe documenta dos alertas medias de cabeceras del frontend de desarrollo, validadas por HTTP. Siguen abiertas; no se demostró explotación. Ver `docs/VALIDACION_SEGUNDO_CORTE.md` y sus evidencias en la rama de revisión.

Antes de entregar, contrastar las figuras con el commit definitivo y actualizar lo que cambie. La fecha de portada no confirma por sí sola el plazo oficial de entrega.

## Regenerar

Con Java y PlantUML disponibles, desde esta carpeta:

```powershell
java -jar RUTA_AL_ARCHIVO_PLANTUML.jar -charset UTF-8 -tpng *.puml
java -jar RUTA_AL_ARCHIVO_PLANTUML.jar -charset UTF-8 -tsvg *.puml
```

Para modelos que utilicen Graphviz, debe estar disponible la dependencia indicada por la distribución de PlantUML utilizada. También puede abrirse cada fuente en un editor PlantUML compatible con la biblioteca estándar C4.
