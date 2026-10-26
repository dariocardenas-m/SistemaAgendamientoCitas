package com.piedrazul.citas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CitaExportacionHistorialTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenPaciente;
    private String tokenAgendador;
    private String tokenLaura;
    private Long medicoLauraId;
    private Long medicoCarlosId;
    private LocalDate fechaHabil;

    @BeforeEach
    void setUp() throws Exception {
        tokenPaciente = login("juan.perez@email.com", "paciente123");
        tokenAgendador = login("carlos.mendoza@piedrazul.com", "agendador123");
        tokenLaura = login("laura.martinez@piedrazul.com", "medico123");
        medicoLauraId = medicoId("Laura");
        medicoCarlosId = medicoId("Carlos");
        fechaHabil = siguienteDiaHabil();
    }

    @Test
    void agendadorExportaAgendaCsvEnMemoria() throws Exception {
        reservar(fechaHabil, LocalTime.of(8, 0));

        mockMvc.perform(get("/api/citas/exportar")
                        .header("Authorization", "Bearer " + tokenAgendador)
                        .param("medicoId", medicoLauraId.toString())
                        .param("fecha", fechaHabil.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Paciente")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Juan")));
    }

    @Test
    void pacienteNoPuedeExportarNiConsultarHistorial() throws Exception {
        mockMvc.perform(get("/api/citas/exportar")
                        .header("Authorization", "Bearer " + tokenPaciente)
                        .param("medicoId", medicoLauraId.toString())
                        .param("fecha", fechaHabil.toString()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/citas/historial")
                        .header("Authorization", "Bearer " + tokenPaciente)
                        .param("cedula", "1061789234"))
                .andExpect(status().isForbidden());
    }

    @Test
    void historialValidaCedulaYNoExponeDatosDeContacto() throws Exception {
        reservar(fechaHabil, LocalTime.of(8, 0));

        mockMvc.perform(get("/api/citas/historial")
                        .header("Authorization", "Bearer " + tokenAgendador)
                        .param("cedula", "1061789234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fecha").value(fechaHabil.toString()))
                .andExpect(jsonPath("$[0].medico", org.hamcrest.Matchers.containsString("Laura")))
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$[0].telefono").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist());

        mockMvc.perform(get("/api/citas/historial")
                        .header("Authorization", "Bearer " + tokenAgendador)
                        .param("cedula", "1061789234' OR '1'='1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void medicoSoloPuedeExportarSuPropiaAgenda() throws Exception {
        reservar(fechaHabil, LocalTime.of(8, 0));

        mockMvc.perform(get("/api/citas/exportar")
                        .header("Authorization", "Bearer " + tokenLaura)
                        .param("medicoId", medicoLauraId.toString())
                        .param("fecha", fechaHabil.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/citas/exportar")
                        .header("Authorization", "Bearer " + tokenLaura)
                        .param("medicoId", medicoCarlosId.toString())
                        .param("fecha", fechaHabil.toString()))
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

    private Long medicoId(String nombre) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/medicos")).andExpect(status().isOk()).andReturn();
        for (JsonNode medico : objectMapper.readTree(result.getResponse().getContentAsString())) {
            if (medico.get("nombreCompleto").asText().contains(nombre)) {
                return medico.get("id").asLong();
            }
        }
        throw new IllegalStateException("Médico de prueba no encontrado");
    }

    private void reservar(LocalDate fecha, LocalTime hora) throws Exception {
        mockMvc.perform(post("/api/citas")
                        .header("Authorization", "Bearer " + tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "medicoId": %d, "fecha": "%s", "hora": "%s",
                                  "modalidad": "PRESENCIAL", "motivo": "Control" }
                                """.formatted(medicoLauraId, fecha, hora)))
                .andExpect(status().isOk());
    }

    private LocalDate siguienteDiaHabil() {
        LocalDate fecha = LocalDate.now().plusDays(1);
        while (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            fecha = fecha.plusDays(1);
        }
        return fecha;
    }
}
