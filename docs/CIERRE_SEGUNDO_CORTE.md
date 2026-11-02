# Estado de entrega, 2 de noviembre de 2026

El código validado quedó integrado mediante avance rápido en `avance-Cardenas`, commit `dd779a1`, sin reescribir la historia ni modificar los clones previos. Este archivo y la evaluación Gestalt agregan documentación posterior a ese código.

## Evidencias disponibles

- Arquitectura C4 y 4+1: nueve diagramas, fuentes PlantUML, PNG y SVG en `diagramas-segundo-corte/`.
- Pruebas: 22 automatizadas aprobadas el 1 de noviembre; 25 comprobaciones HTTP con Keycloak y MySQL reales repetidas y aprobadas el 2 de noviembre. La selección Java requiere incluir `*Test,*IT` explícitamente.
- Seguridad: informe inicial y reescaneo en `REESCANEO_ZAP_2026-11-02.md`, con archivos antes/después. Las dos alertas originales no reaparecen en el entorno local; sigue abierta la advertencia media de estilos en línea.
- Usabilidad: inspección Gestalt real de ocho capturas y seis criterios en `EVALUACION_GESTALT_SEGUNDO_CORTE.md`. Sin participantes ni mediciones inventadas.
- Documento nativo: https://docs.google.com/document/d/1My4xHymRjkCn2dBd-PxHgA55EkNoyUUQuEAKSpOf4Bw/edit?tab=t.0 . Contiene prototipos y planificación proporcionados por el equipo; los resultados técnicos se contrastaron con el código.

## Actividad del equipo al grabar

Agregar la URL real de YouTube al documento y comprobar que el profesor pueda abrirlo. El protocolo suma 14 minutos sin acelerar la grabación: historias/prototipos/usabilidad 2; escenarios de seguridad 1; C4/4+1 2; pruebas 1; Git/Jira 1; GoF 2; demostración 5. La distribución entre integrantes se acuerda con el equipo.

Para usabilidad, explicar la inspección Gestalt y sus hallazgos; no mencionar un estudio con cinco participantes ni los tiempos de las tablas retiradas. Para ZAP, mostrar antes/después e indicar la advertencia residual. Mostrar `avance-Cardenas` y los commits reales; no atribuir autorías de otros integrantes.

## Límites declarados

La entrega conserva como seguimiento la revisión visual interactiva con la CSP, el endurecimiento de estilos en línea y la validación del servidor productivo. No se certifica cobertura total del dominio, ausencia de vulnerabilidades o pruebas de carga. Jira no fue modificado para cerrar tareas automáticamente. Las sesiones con participantes se recomiendan como siguiente validación, no como actividad ya ejecutada.

Actualización visual del 2 de noviembre: login y navegación del agendador comprobados en Brave con la CSP aplicada. Se incorporaron cuatro capturas K1–K4 al documento; dos son de la aplicación y dos de un informe de consultas HTTP reales. No se completó la consola administrativa ni se usó Postman. Ver `EVIDENCIAS_VISUALES_KEYCLOAK.md` para el alcance y los originales.
