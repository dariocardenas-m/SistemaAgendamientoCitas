# Gestión de usuarios y roles de Keycloak

La colección `Gestion_Keycloak.postman_collection.json` es compatible con Postman. Se ejecutó con Newman (ejecutor de colecciones Postman) contra el realm local real `piedrazul`, el 2 de noviembre de 2026: **11 peticiones y 16 aserciones, cero fallos**. El registro original está en `resultado-newman-2026-11-02.txt`.

El recorrido obtiene un token de una cuenta de servicio administrativa ya configurada, crea un usuario ficticio, lo consulta, asigna PACIENTE, comprueba el rol, lo revoca, comprueba su retirada, elimina únicamente ese usuario y confirma 404. También comprueba 401 al consultar administración sin token. El usuario creado para esta ejecución quedó eliminado.

## Reproducir en Postman

1. Importar la colección y `Entorno_SIN_SECRETOS.postman_environment.json`.
2. Seleccionar el entorno y configurar su `client_secret` de forma privada. El archivo del repositorio contiene el valor vacío deliberadamente; no sustituirlo en Git por una credencial real.
3. Configurar el cliente administrativo en el realm de pruebas con permisos mínimos de consulta y gestión de usuarios. La revisión utilizó la cuenta de servicio existente `piedrazul-postman-admin`, con query-users, view-users, manage-users y view-realm. No copiar credenciales de auditoría a producción.
4. Ejecutar las once peticiones en orden desde Runner. Es una colección para un entorno de demostración con autorización de administración, no para ejecutarla contra cuentas reales.
5. Mostrar el resumen de peticiones/aserciones y las respuestas de creación, asignación y revocación. Ocultar el secreto, Authorization y la respuesta que contiene el access_token al grabar.

La evidencia es una ejecución real de la colección mediante Newman. No se presenta el registro como una captura de la aplicación gráfica de Postman ni como prueba de una interfaz administrativa propia.
