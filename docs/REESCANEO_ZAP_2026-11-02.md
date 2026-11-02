# Corrección y reescaneo de ZAP

Fecha: 2 de noviembre de 2026 (Colombia). Complementa el informe inicial del 1 de noviembre; sus evidencias se conservan.

## Cambio realizado

`frontend/vite.config.js` entrega `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff` y `Referrer-Policy` en desarrollo y preview. La CSP limita scripts al propio origen, prohíbe objetos y la incrustación de la aplicación en marcos, y permite el origen de Keycloak obtenido de `VITE_KEYCLOAK_URL`. Permite Google Fonts y los recursos de imagen usados por la aplicación; WebSocket de HMR se permite únicamente en desarrollo.

No se usa `unsafe-inline` ni `unsafe-eval` para scripts. Los estilos sí conservan `unsafe-inline`, por los estilos dinámicos de Vue y la inserción de CSS en desarrollo. Esta excepción es una limitación explícita, no una CSP completamente endurecida.

## Resultado del reescaneo

ZAP 2.16.0, sesión nueva y vacía. Spider y escaneo activo terminados; cola pasiva en cero. Contexto local del frontend y API, escaneo activo sobre `/login` sin recursión, modo protect y datos ficticios. No autenticado; Keycloak y otras aplicaciones excluidos. El escaneo se ejecutó entre 05:42:08 y 05:42:18 UTC, equivalentes a 00:42:08–00:42:18 del 2 de noviembre en Colombia.

| Hallazgo inicial | Resultado posterior |
|---|---|
| 10020: Missing Anti-clickjacking Header | No reaparece. Respuestas con DENY y frame-ancestors none |
| 10038: Content Security Policy Header Not Set | No reaparece. CSP presente y en modo de aplicación |
| 10021: X-Content-Type-Options Header Missing | No reaparece. nosniff presente |

Los dos primeros hallazgos se consideran corregidos **en el alcance local reescaneado**. No se extiende esta conclusión a un servidor de producción.

ZAP registra ahora 20 instancias en cuatro tipos: nueve advertencias medias `10055 CSP: style-src unsafe-inline`, nueve informativas de aplicación moderna, una informativa de comentarios y una advertencia baja de antigüedad del escáner. La advertencia media de estilos **sigue abierta**. No se ocultó ni se excluyó su regla. La siguiente mejora requiere sustituir el permiso general por nonces o hashes y comprobar estilos dinámicos, HMR y navegación con un navegador.

## Comprobaciones de regresión

- Diez comprobaciones HTTP de cabeceras: cinco rutas en desarrollo y las mismas cinco en preview, todas aprobadas; recursos JS/CSS referenciados accesibles con HTTP 200.
- Compilación del frontend correcta.
- Veinticinco comprobaciones HTTP con Keycloak real y MySQL real aprobadas de nuevo, incluyendo cuatro roles, permisos, CSV, historial y conflictos de reactivación.
- Estas comprobaciones no ejecutan JavaScript en un navegador: queda pendiente comprobar visualmente la aplicación y detectar eventuales infracciones de CSP durante navegación, login y renovación de sesión.
- No se volvió a ejecutar la suite Java: el cambio solo afecta las cabeceras del servidor frontend. Sus 22 pruebas aprobadas corresponden a la revisión anterior.

Evidencias en `evidencias-segundo-corte/reescaneo-2026-11-02/`: informe ZAP original posterior, JSON de alertas y resumen, cabeceras antes/después y matriz de integración real. Las evidencias anteriores mantienen los hallazgos iniciales.

## Producción

Vite dev y preview no sustituyen al servidor productivo. Al desplegar `dist/`, reproducir estas cabeceras en el proxy o servidor real, configurar el origen correcto de Keycloak y repetir las comprobaciones. No se ha probado ese despliegue. No se garantiza ausencia global de vulnerabilidades ni conformidad completa OWASP.

Fuentes: [cabeceras de Vite](https://vite.dev/config/server-options#server-headers), [cabeceras de preview](https://vite.dev/config/preview-options#preview-headers), [regla 10038 de ZAP](https://www.zaproxy.org/docs/alerts/10038/).
