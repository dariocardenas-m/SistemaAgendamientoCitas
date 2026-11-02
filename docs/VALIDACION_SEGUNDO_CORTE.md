# Validación del segundo corte

Fecha: 1 de noviembre de 2026. Base: rama `avance-Cardenas`, commit `7a5b57f5a030b2d236928be96e1e4ff1870a2372`.

## Resultado comprobado

- 22 pruebas automatizadas aprobadas: 0 fallos, 0 errores y 0 omitidas. Incluyen las clases `*Test` y `*IT`.
- 25 comprobaciones HTTP aprobadas con Keycloak real y la aplicación conectada a MySQL real. Se probaron los cuatro roles de aplicación, alta del perfil de un paciente, permisos, propiedad, CSV, historial, reserva, cancelación y reactivación.
- Frontend compilado después de incorporar el cambio de colores por rol del commit base.
- Paquete ejecutable del backend generado correctamente. La primera operación de empaquetado encontró el JAR abierto por el backend de auditoría; se detuvo ese proceso y el empaquetado posterior finalizó correctamente.

Estos resultados no equivalen a cobertura total del código ni a una certificación de seguridad. La prueba OIDC se automatizó mediante HTTP con Authorization Code y PKCE S256; no constituye una grabación del recorrido visual de la SPA.

## Entorno de auditoría

Se usó una instalación local aislada, con datos ficticios del `DataSeeder` y cuentas de auditoría. La base habitual del equipo no fue modificada.

| Componente | Configuración comprobada |
|---|---|
| Backend | Java 17, Spring Boot 3.3.4, perfiles `mysql,keycloak`, puerto 8080 |
| Frontend | Vue 3 y Vite, puerto 5173 |
| Identidad | Keycloak 26.8.1 sobre Java 25, realm `piedrazul`, puerto 8081 |
| Datos de la aplicación | MySQL 8.0.46, base separada `piedrazul_auditoria`, puerto 3317 |
| Escáner | OWASP ZAP 2.16.0, instancia local separada, puerto 8090 |

El README del proyecto conserva el puerto MySQL predeterminado 3307; la auditoría lo sustituyó mediante `DB_URL` para aislar los datos. Keycloak utilizó su almacenamiento de desarrollo, separado de MySQL de la aplicación. Todos los servicios de auditoría se limitaron al equipo local.

## Fallos reproducidos y correcciones

1. **Filtro JWT local activo con Keycloak.** El filtro anterior podía borrar la autenticación aceptada por el Resource Server. Se restringió `JwtAuthFilter` al perfil `!keycloak`. La regresión inicial fallaba y sus tres casos pasan después del ajuste.
2. **Audiencia de Keycloak ausente.** El realm configuraba `included.client.audience` con un destinatario que no quedaba resuelto en la importación. Un token real contenía `aud: account` y la API respondía 401. Se usó `included.custom.audience: piedrazul-api`, conservando la validación de firma, emisor, expiración y audiencia en el backend. El acceso real de los cuatro roles pasó a responder 200. Una prueba del archivo de importación evita referencias de audiencia sin resolver.
3. **Roles de aplicación ambiguos.** Un token con `ADMINISTRADOR` y `PACIENTE` era rechazado por `/api/auth/me`, pero podía consultar `/api/configuracion`. El conversor ahora exige exactamente un rol de aplicación antes de crear la autenticación. Los roles técnicos de Keycloak no cuentan como roles de negocio. Un token sin rol de aplicación o con varios recibe 401; una identidad válida con rol único pero sin permiso recibe 403.
4. **Doble ocupación al reactivar.** Se canceló una cita, otro paciente reservó la franja y la reactivación de la primera respondía 200. Ahora se bloquea primero el profesional y luego las citas del día, igual que al reservar, y se comprueba el solapamiento excluyendo la propia cita. El conflicto responde 409. Una cita adyacente que no solapa sigue permitida.

Las pruebas de regresión de audiencia, roles y solapamiento se ejecutaron antes y después de corregir los fallos. Los resultados HTTP anteriores y posteriores se conservan por separado en las evidencias.

5. **CSV incompleto frente a HU-18.** El archivo incluía hora y paciente, pero omitía fecha y profesional, exigidos por el criterio de aceptación del documento. Se agregaron ambas columnas y se reforzó la prueba de exportación con una cita real de la prueba. Se conservan UTF-8, BOM, comillas, neutralización de fórmulas y generación en memoria. La matriz HTTP comprueba las columnas y la cantidad de filas frente a la consulta de agenda.

## Pruebas automatizadas

Desde `backend`, usando Java 17:

```powershell
mvn "-Dtest=*Test,*IT" test
```

La selección explícita incluye las clases `*IT`, que no forman parte de la selección predeterminada de Surefire en este proyecto. Los casos de `KeycloakAuthenticationIT` utilizan `JwtDecoder` simulado y H2; la matriz de 25 casos es una validación distinta contra el proveedor y la base reales.

Pruebas añadidas: `KeycloakAuthenticationIT`, `KeycloakRealmImportTest`, `KeycloakJwtAuthenticationConverterTest` y `CitaServiceReactivarTest`.

No se midió un porcentaje de cobertura. Tampoco se ejecutaron en esta revisión una prueba de carga, una validación exhaustiva de concurrencia o un recorrido visual completo de la SPA. Los escenarios de tokens expirados y de emisor incorrecto requieren evidencia específica adicional; no se infieren de los dos casos negativos de ausencia de token y firma alterada.

## Informe breve de OWASP ZAP

El spider y el escaneo activo finalizaron el 1 de noviembre de 2026; la cola pasiva quedó vacía. El contexto incluyó `http://localhost:5173/` y `http://localhost:8080/api/`. El escaneo activo se limitó a `/login`, sin recursión, con un máximo de tres minutos. Se excluyeron Keycloak, la API del propio ZAP, otros equipos y otros servicios.

El escaneo web fue **sin autenticar**. Las operaciones protegidas se verificaron aparte mediante la matriz HTTP con tokens reales. Por ello no se afirma que ZAP haya explorado todos los flujos privados ni detectado los cuatro fallos anteriores.

### Hallazgo 1: protección contra clickjacking ausente

- Alerta ZAP `10020`, **Missing Anti-clickjacking Header**; riesgo medio, confianza media; CWE-1021.
- Se observaron diez instancias en respuestas del frontend. Se verificaron manualmente HTTP 200 de `/` y `/login`, sin `X-Frame-Options` ni `Content-Security-Policy`.
- Mapeo adoptado: **A05:2021 - Security Misconfiguration**.
- La ausencia del control permite considerar un riesgo de incrustación de la interfaz en marcos no autorizados. No se demostró un ataque completo de clickjacking.
- Recomendación: aplicar una política `frame-ancestors` y, como defensa adicional compatible, `X-Frame-Options` en el servidor o proxy que entregue el frontend. Debe verificarse el comportamiento de los flujos OIDC después del cambio.
- Estado: **abierto**. No se modificaron cabeceras ni se realizó un reescaneo posterior a una corrección.

### Hallazgo 2: Content Security Policy ausente

- Alerta ZAP `10038`, **Content Security Policy (CSP) Header Not Set**; riesgo medio, confianza alta.
- Se observaron diez instancias. La comprobación manual confirmó que `/` y `/login` no enviaban la cabecera CSP.
- Mapeo adoptado: **A05:2021 - Security Misconfiguration**.
- Falta una política que limite los orígenes de recursos y la ejecución de contenido. Esto debilita la defensa en profundidad, pero **no demuestra por sí solo una vulnerabilidad XSS**.
- Recomendación: definir la política con los recursos reales de Vue/Vite y Keycloak, evaluar primero los reportes y probar login, renovación de token y navegación antes de aplicarla en producción.
- Estado: **abierto**. No se afirma que la ausencia esté corregida.

Son dos tipos de alerta de ZAP dentro de una misma categoría OWASP; no se presentan como dos categorías diferentes. Los hallazgos corresponden al servidor Vite de desarrollo; deben revisarse en el servidor o proxy de producción antes de extrapolarlos al despliegue final.

### Otros resultados y límites

ZAP registró 45 alertas de instancia, agrupadas en seis tipos. También informó ausencia de `X-Content-Type-Options`, características de aplicación moderna, comentarios sospechosos y una advertencia de antigüedad de ZAP. Las alertas informativas y la antigüedad de la herramienta no se contabilizan como vulnerabilidades confirmadas de la aplicación. No se encontró ni se demostró inyección SQL o XSS en el alcance recorrido; esto no acredita su ausencia en toda la aplicación.

Evidencias: `evidencias-segundo-corte/integracion-real-final.json`, `resumen-pruebas.json`, `resumen-zap.json`, `alertas-zap.json`, `validacion-manual-cabeceras.json` y `reporte-original-zap.html`. No contienen tokens, client secrets o contraseñas.

## Patrones GoF comprobados

**Strategy:** `PasswordEncoder` es la abstracción; `BCryptPasswordEncoder`, la estrategia concreta; `PasswordConfig` selecciona la implementación que consumen `AuthService`, el seeder y los servicios de perfiles. La dependencia de la interfaz concentra la elección del algoritmo. Cambiar de algoritmo requiere planificar la compatibilidad de los hashes existentes. En el modo Keycloak, las contraseñas de acceso se verifican en el proveedor externo.

**Chain of Responsibility:** Spring Security organiza filtros mediante `FilterChainProxy` y `SecurityFilterChain`. En el perfil Keycloak, `BearerTokenAuthenticationFilter` realiza la autenticación y `AuthorizationFilter` aplica las reglas de rutas. Los filtros pueden rechazar una solicitud o continuar la cadena. El filtro propio `JwtAuthFilter` pertenece solo al modo local. Se documenta un patrón aplicado a través del framework, no una cadena creada íntegramente por el equipo.

Repository, DTO, Service Layer e inyección de dependencias mantienen su clasificación propia; no se cuentan como patrones GoF.

## Pendientes de cierre de entrega

- Integrar la rama de revisión en el commit que el equipo vaya a entregar y actualizar los enlaces del documento y el video.
- Incorporar o revisar evidencias visuales del recorrido de la SPA y de la gestión de cuentas si se requieren para la sustentación.
- Añadir el enlace real del video y confirmar el acceso del profesor. El protocolo suma 14 minutos, sin acelerar la grabación.
- Completar con resultados reales de participantes cualquier evidencia de usabilidad aún pendiente. No se realizaron sesiones con participantes durante esta auditoría.

## Referencias técnicas

- [Configuración de protocol mappers de Keycloak](https://www.keycloak.org/admin-api/protocol-mappers).
- [Inicio de Keycloak mediante distribución ZIP](https://www.keycloak.org/getting-started/getting-started-zip).
- [API de OWASP ZAP](https://www.zaproxy.org/docs/api/).
- [OWASP Top 10:2021, A05](https://top10.owasp.org/2021/A05_2021-Security_Misconfiguration/).

## Actualización del 2 de noviembre
Los dos hallazgos originales ya no aparecen en el reescaneo local posterior a la corrección. Permanece una advertencia media de estilos en línea. Ver [informe posterior](REESCANEO_ZAP_2026-11-02.md) y sus evidencias; el estado abierto descrito arriba corresponde al análisis inicial.

