package com.piedrazul.citas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedrazul.usuarios.RegistroPacienteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CitaEditarIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenPaciente;
    private Long medicoId;
    private LocalDate fechaHabil;

    @BeforeEach
    void setUp() throws Exception {
        tokenPaciente = login("juan.perez@email.com", "paciente123");
        medicoId = medicoLauraId();
        fechaHabil = siguienteHabil(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    }

    @Test
    void editarCitaValidaDevuelve200() throws Exception {
        Long citaId = reservar(tokenPaciente, medicoId, fechaHabil, LocalTime.of(8, 0), "Chequeo");

        mockMvc.perform(put("/api/citas/" + citaId)
                        .header("Authorization", "Bearer " + tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEdicion(medicoId, fechaHabil, LocalTime.of(9, 0), "TELEMEDICINA", "Control actualizado")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hora").value("09:00:00"))
                .andExpect(jsonPath("$.modalidad").value("TELEMEDICINA"))
                .andExpect(jsonPath("$.motivo").value("Control actualizado"));
    }

    @Test
    void fechasHabilesSePuedenConsultarSinSesion() throws Exception {
        mockMvc.perform(get("/api/citas/fechas-habiles").param("medicoId", medicoId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void editarConHorarioOcupadoDevuelve409() throws Exception {
        Long citaId = reservar(tokenPaciente, medicoId, fechaHabil, LocalTime.of(8, 0), "Primera");
        reservar(tokenPaciente, medicoId, fechaHabil, LocalTime.of(9, 0), "Ocupada");

        mockMvc.perform(put("/api/citas/" + citaId)
                        .header("Authorization", "Bearer " + tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEdicion(medicoId, fechaHabil, LocalTime.of(9, 0), "PRESENCIAL", "Choque")))
                .andExpect(status().isConflict());
    }

    @Test
    void editarCitaCanceladaDevuelve400() throws Exception {
        Long citaId = reservar(tokenPaciente, medicoId, fechaHabil, LocalTime.of(10, 0), "A cancelar");
        mockMvc.perform(patch("/api/citas/" + citaId + "/cancelar")
                        .header("Authorization", "Bearer " + tokenPaciente))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/citas/" + citaId)
                        .header("Authorization", "Bearer " + tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEdicion(medicoId, fechaHabil, LocalTime.of(10, 30), "PRESENCIAL", "No debe pasar")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editarPorPacienteQueNoEsDuenoDevuelve403() throws Exception {
        Long citaId = reservar(tokenPaciente, medicoId, fechaHabil, LocalTime.of(11, 0), "De Juan");
        String tokenOtro = registrarPaciente("Ana López", "1099001122", "ana.otro@email.com");

        mockMvc.perform(put("/api/citas/" + citaId)
                        .header("Authorization", "Bearer " + tokenOtro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEdicion(medicoId, fechaHabil, LocalTime.of(11, 30), "PRESENCIAL", "Intento ajeno")))
                .andExpect(status().isForbidden());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String registrarPaciente(String nombre, String documento, String email) throws Exception {
        RegistroPacienteRequest req = new RegistroPacienteRequest();
        req.setNombreCompleto(nombre);
        req.setDocumento(documento);
        req.setTelefono("3100000000");
        req.setEmail(email);
        req.setPassword("paciente123");
        MvcResult result = mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private Long medicoLauraId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/medicos")).andExpect(status().isOk()).andReturn();
        for (JsonNode n : objectMapper.readTree(result.getResponse().getContentAsString())) {
            if (n.get("nombreCompleto").asText().contains("Laura")) {
                return n.get("id").asLong();
            }
        }
        throw new IllegalStateException("Médico de prueba no encontrado");
    }

    private Long reservar(String token, Long medico, LocalDate fecha, LocalTime hora, String motivo) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/citas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicoId": %d,
                                  "fecha": "%s",
                                  "hora": "%s",
                                  "modalidad": "PRESENCIAL",
                                  "motivo": "%s"
                                }
                                """.formatted(medico, fecha, hora, motivo)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String cuerpoEdicion(Long medico, LocalDate fecha, LocalTime hora, String modalidad, String motivo) {
        return """
                {
                  "medicoId": %d,
                  "fecha": "%s",
                  "hora": "%s",
                  "modalidad": "%s",
                  "motivo": "%s"
                }
                """.formatted(medico, fecha, hora, modalidad, motivo);
    }

    private LocalDate siguienteHabil(DayOfWeek desde, DayOfWeek hasta) {
        LocalDate d = LocalDate.now().plusDays(1);
        while (d.getDayOfWeek().getValue() < desde.getValue() || d.getDayOfWeek().getValue() > hasta.getValue()) {
            d = d.plusDays(1);
        }
        return d;
    }
}
