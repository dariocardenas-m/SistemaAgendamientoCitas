-- =============================================================================
-- Esquema PostgreSQL — Sistema de Agendamiento de Citas (Red Médica Piedra Azul)
-- Base recomendada: sistema_agendamiento_citas
--
-- Cómo usarlo en pgAdmin:
--   1. Conéctese al servidor PostgreSQL.
--   2. Cree la base de datos (si aún no existe): clic derecho en Databases → Create
--      → Database → Name: sistema_agendamiento_citas → Save.
--      Equivalente SQL (ejecutar conectado a la base "postgres"):
--        CREATE DATABASE sistema_agendamiento_citas WITH ENCODING 'UTF8';
--   3. Abra Query Tool sobre sistema_agendamiento_citas (no sobre "postgres").
--   4. File → Open → este archivo → Execute (F5).
--
-- El script no elimina tablas ni datos. CREATE TABLE IF NOT EXISTS permite
-- reejecutarlo de forma segura si las tablas ya existen.
-- Roles, estados y modalidades se persisten como VARCHAR (enums JPA), no como
-- catálogos. Los datos de arranque (cuentas, especialidades, médicos) los
-- inserta DataSeeder.java la primera vez que arranca la aplicación con la BD vacía.
-- =============================================================================

-- Usuarios de autenticación (paciente, agendador, administrador, médico/terapeuta).
CREATE TABLE IF NOT EXISTS usuarios (
    id              BIGSERIAL PRIMARY KEY,
    nombre_completo VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    rol             VARCHAR(50)  NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    foto_url        VARCHAR(255),
    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT chk_usuarios_rol CHECK (
        rol IN ('ADMINISTRADOR', 'MEDICO_TERAPISTA', 'PACIENTE', 'AGENDADOR')
    )
);

-- Perfil clínico asociado 1:1 a un usuario con rol PACIENTE.
CREATE TABLE IF NOT EXISTS pacientes (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL,
    documento       VARCHAR(255) NOT NULL,
    telefono        VARCHAR(255) NOT NULL,
    codigo          VARCHAR(255) NOT NULL,
    perfil_clinico  VARCHAR(255),
    CONSTRAINT uq_pacientes_usuario UNIQUE (usuario_id),
    CONSTRAINT uq_pacientes_documento UNIQUE (documento),
    CONSTRAINT uq_pacientes_codigo UNIQUE (codigo),
    CONSTRAINT fk_pacientes_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

-- Catálogo de especialidades médicas (un médico pertenece a una especialidad).
CREATE TABLE IF NOT EXISTS especialidades (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(255) NOT NULL,
    descripcion VARCHAR(500),
    icono       VARCHAR(255),
    color       VARCHAR(255),
    CONSTRAINT uq_especialidades_nombre UNIQUE (nombre)
);

-- Profesionales. La disponibilidad vive en esta tabla (días, horario e intervalo).
-- usuario_id es opcional: el seeder crea médicos sin cuenta de usuario.
CREATE TABLE IF NOT EXISTS medicos (
    id                 BIGSERIAL PRIMARY KEY,
    usuario_id         BIGINT,
    nombre_completo    VARCHAR(255) NOT NULL,
    especialidad_id    BIGINT       NOT NULL,
    consultorio        VARCHAR(255),
    foto_url           VARCHAR(255),
    anios_experiencia  INTEGER,
    rating             DOUBLE PRECISION,
    descripcion        VARCHAR(800),
    dias_atencion      VARCHAR(255) NOT NULL,
    hora_apertura      TIME         NOT NULL,
    hora_cierre        TIME         NOT NULL,
    intervalo_minutos  INTEGER      NOT NULL,
    version            BIGINT, -- bloqueo optimista JPA (@Version)
    CONSTRAINT uq_medicos_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_medicos_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_medicos_especialidad
        FOREIGN KEY (especialidad_id) REFERENCES especialidades (id),
    CONSTRAINT chk_medicos_horario CHECK (hora_cierre > hora_apertura),
    CONSTRAINT chk_medicos_intervalo CHECK (intervalo_minutos IN (15, 30, 45, 60))
);

-- Citas. No hay UNIQUE (medico, fecha, hora) porque una cita CANCELADA libera la franja.
CREATE TABLE IF NOT EXISTS citas (
    id                BIGSERIAL PRIMARY KEY,
    codigo            VARCHAR(255) NOT NULL,
    paciente_id       BIGINT       NOT NULL,
    medico_id         BIGINT       NOT NULL,
    fecha             DATE         NOT NULL,
    hora              TIME         NOT NULL,
    duracion_minutos  INTEGER      NOT NULL,
    modalidad         VARCHAR(50)  NOT NULL DEFAULT 'PRESENCIAL',
    motivo            VARCHAR(500),
    estado            VARCHAR(50)  NOT NULL DEFAULT 'CONFIRMADA',
    creada_en         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_citas_codigo UNIQUE (codigo),
    CONSTRAINT fk_citas_paciente
        FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_citas_medico
        FOREIGN KEY (medico_id) REFERENCES medicos (id),
    CONSTRAINT chk_citas_modalidad CHECK (modalidad IN ('PRESENCIAL', 'TELEMEDICINA')),
    CONSTRAINT chk_citas_estado CHECK (estado IN ('CONFIRMADA', 'CANCELADA', 'ATENDIDA')),
    CONSTRAINT chk_citas_duracion CHECK (duracion_minutos IN (15, 30, 45, 60))
);

-- Parámetro global: semanas hacia adelante en las que se puede agendar.
CREATE TABLE IF NOT EXISTS configuracion_global (
    id               BIGSERIAL PRIMARY KEY,
    ventana_semanas  INTEGER NOT NULL DEFAULT 4,
    CONSTRAINT chk_configuracion_ventana CHECK (ventana_semanas BETWEEN 1 AND 12)
);

-- Consultas frecuentes del repositorio JPA.
CREATE INDEX IF NOT EXISTS idx_medicos_especialidad_id ON medicos (especialidad_id);
CREATE INDEX IF NOT EXISTS idx_citas_paciente_id ON citas (paciente_id);
CREATE INDEX IF NOT EXISTS idx_citas_medico_fecha ON citas (medico_id, fecha);
CREATE INDEX IF NOT EXISTS idx_usuarios_rol ON usuarios (rol);
