# Evaluación de usabilidad: inspección Gestalt de prototipos

Fecha: 2 de noviembre de 2026. Técnica: inspección visual cualitativa de los prototipos incorporados al documento de arquitectura. La consigna admite técnicas Gestalt como alternativa de evaluación. Se revisaron ocho capturas: acceso, profesional, fecha/horario, citas del paciente y paneles de agendador, administrador, paciente y médico.

Fuente: Google Doc `Documento_Arquitectura_Segundo_Corte`, exportado antes de esta revisión como `Documento_Arquitectura_reescaneo_2026-11-02.pdf`, páginas 10–14 de esa versión. Las páginas pueden cambiar al editar el documento. Las capturas fueron inspeccionadas; no se ejecutó un estudio con participantes ni se midieron tiempos, satisfacción o éxito de tareas.

| Principio | Evidencia observada | Resultado y recomendación |
|---|---|---|
| Proximidad | En acceso, agenda e historial, campos y acciones relacionados aparecen próximos; en agenda la fecha se reconoce principalmente por su formato. | Parcial: añadir una etiqueta visible Fecha y conservar la agrupación. |
| Semejanza | Consultar y Consultar historial mantienen forma y ubicación consistentes entre roles; el rol se identifica con texto y color. | Favorable: conservar las convenciones al ampliar módulos. |
| Figura y fondo | Las tarjetas se separan del fondo y las acciones principales usan color; la parametrización y las franjas contienen texto pequeño y tonos claros. | Parcial: medir contraste y verificar legibilidad en tamaños reales. No se acredita WCAG mediante capturas. |
| Continuidad | El flujo de reserva presenta cinco pasos, paso activo y controles Anterior/Continuar. | Favorable: conservar los pasos y la descripción de la selección. |
| Cierre | La selección de profesional usa borde y marca. Las capturas iniciales de agenda e historial no muestran descarga terminada, vacío o error. | Parcial: ampliar los prototipos con esos estados. No se concluye que falten en el código. |
| Región común | Agenda e historial están en tarjetas distintas; administración separa ventana global y configuración por profesional. | Favorable: mantener la separación al añadir acciones. |

Resultado cualitativo: tres criterios favorables y tres parciales. Las recomendaciones son mejoras propuestas, no cambios implementados ni problemas demostrados mediante sesiones con usuarios. La prioridad es la etiqueta de fecha, la legibilidad secundaria y los estados de retroalimentación. Se retiraron del documento las dos tablas de participantes simulados.

Límites y seguimiento: comprobar navegación real, estados de carga/error/vacío, teclado, móvil, contraste y los efectos de CSP. Posteriormente, validar con personas representativas y datos ficticios. Los resultados de esas sesiones se registrarán solo cuando ocurran. Esta inspección no prueba accesibilidad completa ni el rendimiento de usuarios reales.
