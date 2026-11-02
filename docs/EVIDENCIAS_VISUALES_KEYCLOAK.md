# Evidencias visuales de Keycloak y JWT

Fecha: 2 de noviembre de 2026. Base funcional: commit 65b5795 en avance-Cardenas. Se usó la instancia aislada de pruebas con datos ficticios y la CSP ya aplicada.

## Verificación en Brave

Se abrió la aplicación en localhost:5173, se siguió Continuar con Keycloak y se inició sesión con la cuenta ficticia audit-agendador. El navegador regresó a la SPA mostrando Carlos Mendoza y AGENDADOR. Se accedió al módulo Gestión de Citas y se consultó una agenda por profesional y fecha; la consulta mostró cero citas en ese filtro. Se cerró la sesión y se repitió correctamente el inicio de sesión.

Las capturas se limitaron al contenido de la página. Se dejaron fuera las barras del navegador y los avisos de depuración; el cursor y su halo no aparecen en los archivos finales. No se alteraron textos, datos o resultados visibles de la aplicación.

| Archivo | Evidencia y alcance |
|---|---|
| 01-formulario-keycloak.jpg | Formulario real del realm piedrazul, antes de introducir credenciales. |
| 02-agendador-autenticado.jpg | Panel real de la SPA después de autenticarse, con identidad y rol de agendador. |
| 03-configuracion-http.jpg | Vista del informe generado desde respuestas reales de Admin REST API: realm, cuatro roles, cliente público, PKCE y asignación del usuario ficticio. |
| 04-respuestas-200-401-403.jpg | Vista del mismo informe con cuatro peticiones HTTP repetidas: perfil autorizado 200; rol sin permiso administrativo 403; petición sin token 401; firma alterada 401. |

Los originales, el JSON saneado y el HTML del informe están en `evidencias-segundo-corte/capturas-keycloak-2026-11-02/`. Las figuras K1–K4 se incorporaron al Google Doc de arquitectura y se revisaron en su PDF exportado, páginas 47–48 de esa versión.

## Diferencias entre las evidencias

Las primeras dos imágenes son capturas de Keycloak y de la aplicación. Las dos últimas son capturas de un informe técnico automatizado, rotulado expresamente como tal. **No son capturas de Postman ni de la consola administrativa de Keycloak.** El JSON procede de consultas autenticadas realizadas para esta revisión; los tokens completos y las credenciales permanecen fuera del repositorio.

La consola administrativa mostró `Timeout when waiting for 3rd party check iframe message`. Durante el diagnóstico, la herramienta de navegador indicó que estaba resolviendo una respuesta de documento pausada. No se completó la captura visual de su pantalla de roles y no se atribuye a esta revisión una causa definitiva del error. No se desactivaron protecciones del navegador para obtener las imágenes.

## Límites

La navegación visual comprobada cubre el login, el retorno a la SPA y la consulta de agenda del agendador. No acredita todos los roles en interfaz, renovación automática de token, accesibilidad completa o producción. Las 25 comprobaciones HTTP previas y las 22 pruebas Java mantienen su evidencia separada. Esta revisión añade cuatro comprobaciones HTTP y las capturas; no modifica el comportamiento de la aplicación.
