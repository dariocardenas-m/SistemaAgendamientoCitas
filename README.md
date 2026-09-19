# Sistema de Agendamiento de Citas Médicas - Piedra Azul

## Software III - Universidad del Cauca

---

## Descripción

El **Sistema de Agendamiento de Citas Médicas de la Red Piedra Azul** es una plataforma web orientada a la gestión y agendamiento de citas médicas.

El proyecto busca facilitar la interacción entre pacientes, agendadores, médicos, terapeutas y administradores, permitiendo gestionar de manera organizada la información relacionada con profesionales, pacientes, especialidades, disponibilidad y citas médicas.

La plataforma permite que los pacientes consulten la disponibilidad de los profesionales y realicen el agendamiento de sus citas de manera autónoma. Asimismo, proporciona herramientas para que los agendadores puedan consultar y gestionar citas, y para que los administradores puedan configurar los parámetros relacionados con la disponibilidad y el funcionamiento del sistema.

---

## Objetivo

Desarrollar una plataforma web que permita gestionar de forma centralizada el proceso de agendamiento de citas médicas, reduciendo la dependencia de procesos manuales y facilitando la consulta de disponibilidad y gestión de citas.

El sistema busca mantener la información organizada, controlar el acceso de acuerdo con los roles de usuario y garantizar que el proceso de agendamiento respete la disponibilidad definida para cada profesional.

---

## Funcionalidades principales

### Gestión de usuarios

- Registro de usuarios.
- Inicio de sesión.
- Gestión de roles.
- Control de acceso según el tipo de usuario.

### Gestión de pacientes

- Registro y consulta de información de pacientes.
- Asociación de pacientes con sus citas médicas.

### Gestión de médicos y terapeutas

- Registro y consulta de profesionales.
- Asociación de profesionales con sus especialidades.
- Gestión de disponibilidad.

### Gestión de especialidades

- Administración del catálogo de especialidades.
- Asociación de especialidades con médicos y terapeutas.

### Agendamiento de citas

- Consulta de profesionales disponibles.
- Consulta de fechas y franjas horarias.
- Agendamiento autónomo de citas por parte del paciente.
- Agendamiento de citas por parte del personal encargado.
- Validación de disponibilidad.
- Consulta de citas registradas.

### Gestión de disponibilidad

- Configuración de días de atención.
- Configuración de franjas horarias.
- Definición del intervalo entre citas.
- Configuración de parámetros para el agendamiento autónomo.

---

## Arquitectura

El sistema se desarrolla utilizando una **arquitectura de monolito modular**, en la cual las diferentes funcionalidades del sistema se organizan en módulos independientes dentro de una misma aplicación.

La arquitectura busca mantener una separación clara de responsabilidades, facilitando el mantenimiento, evolución y comprensión del sistema.

Los principales módulos son:

- **Usuarios**
- **Pacientes**
- **Médicos**
- **Especialidades**
- **Citas**

La aplicación está compuesta principalmente por:

- **Frontend:** Vue 3
- **Backend:** Spring Boot 3
- **Lenguaje:** Java
- **Persistencia:** Base de datos SQL

El frontend proporciona la interfaz de interacción con los usuarios, mientras que el backend concentra las reglas de negocio y la gestión de la información mediante una API REST.

---

## Roles del sistema

### Paciente

Puede consultar la disponibilidad de los profesionales y realizar el agendamiento de sus propias citas.

### Agendador de citas

Puede consultar y gestionar las citas asociadas a los profesionales, facilitando el proceso de agendamiento para los pacientes.

### Administrador

Tiene responsabilidades relacionadas con la administración de usuarios, roles y configuración de los parámetros que determinan la disponibilidad del sistema.

### Médico o terapeuta

Se encuentra asociado a una especialidad y cuenta con una disponibilidad definida para la atención de pacientes.

---

## Modelo de arquitectura

La arquitectura del proyecto se analiza mediante el modelo **C4** y las **4+1 vistas arquitectónicas**, permitiendo representar el sistema desde diferentes niveles de abstracción.

### Modelo C4

Se contemplan los siguientes niveles:

- **Nivel 1 - Contexto:** representa los actores que interactúan con el sistema Piedra Azul.
- **Nivel 2 - Contenedores:** representa el frontend, el backend como monolito modular y la base de datos.
- **Nivel 3 - Componentes:** representa los módulos principales que conforman el monolito.
- **Nivel 4 - Código:** presenta el detalle interno de uno de los módulos del sistema.

### Modelo 4+1

El diseño arquitectónico también contempla:

- Vista lógica.
- Vista de desarrollo.
- Vista de procesos.
- Vista física.
- Escenarios y casos de uso.

---

## Tecnologías

| Tecnología | Uso |
|------------|-----|
| Vue 3 | Desarrollo del frontend |
| Spring Boot 3 | Desarrollo del backend |
| Java | Lenguaje principal del backend |
| API REST | Comunicación entre frontend y backend |
| SQL | Persistencia de información |
| PlantUML | Modelado de arquitectura |
| C4 | Modelado arquitectónico |
| 4+1 | Vistas arquitectónicas |

---

## Contexto del proyecto

El proyecto corresponde a una evolución del sistema de agendamiento de citas médicas desarrollado previamente.

En esta etapa se busca adaptar la solución hacia una **arquitectura de monolito modular**, manteniendo una separación clara entre las responsabilidades del sistema y utilizando una interfaz web desarrollada con Vue.

La evolución arquitectónica permite estudiar y aplicar conceptos relacionados con modularidad, separación de responsabilidades, diseño de software y modelado arquitectónico.

---

## Integrantes

**Elaborado por:**

- **Jors Eduar Solarte Castillo**
- **Dario Alexander Cardenas**
- **Sebastian Ruiz**

---

## Información académica

**Universidad del Cauca**  
**Software IIII**  
**Periodo académico: 2026-2**

---

## Piedra Azul

**Sistema de Agendamiento de Citas Médicas**

Plataforma web para la gestión de pacientes, profesionales, disponibilidad y citas médicas, desarrollada bajo una arquitectura de **monolito modular**.
