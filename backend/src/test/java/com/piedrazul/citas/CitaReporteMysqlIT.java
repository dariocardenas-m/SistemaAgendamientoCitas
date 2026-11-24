package com.piedrazul.citas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración contra MySQL real. Se ejecuta en CI con un servicio MySQL aislado;
 * localmente se omite salvo que MYSQL_TEST_URL apunte a una base dedicada de pruebas.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${MYSQL_TEST_URL:jdbc:h2:mem:mysql_it_fallback;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${MYSQL_TEST_USERNAME:sa}",
        "spring.datasource.password=${MYSQL_TEST_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = "jdbc:mysql:.*")
class CitaReporteMysqlIT {

    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void guardaCancelacionEnMysqlYLaIncluyeEnElReporteMensual() throws Exception {
        String pacienteToken = login("juan.perez@email.com", "paciente123");
        Long medicoId = buscarMedicoId("Laura");
        LocalDate fecha = siguienteHabil(LocalDate.now(ZONA_COLOMBIA).plusDays(2));
        Long citaId = reservar(pacienteToken, medicoId, fecha);

        mockMvc.perform(patch("/api/citas/" + citaId + "/cancelar")
                        .header("Authorization", "Bearer " + pacienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificacion\":\"Prueba de persistencia MySQL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.justificacionCancelacion").value("Prueba de persistencia MySQL"));

        mockMvc.perform(get("/api/citas/mias").header("Authorization", "Bearer " + pacienteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].justificacionCancelacion").value("Prueba de persistencia MySQL"));

        String agendadorToken = login("carlos.mendoza@piedrazul.com", "agendador123");
        MvcResult resultado = mockMvc.perform(get("/api/citas/reporte-anual")
                        .param("anio", Integer.toString(fecha.getYear()))
                        .header("Authorization", "Bearer " + agendadorToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode reporte = objectMapper.readTree(resultado.getResponse().getContentAsString());
        JsonNode serieMedico = null;
        for (JsonNode serie : reporte.path("medicos")) {
            if (serie.path("medicoId").asLong() == medicoId) {
                serieMedico = serie;
                break;
            }
        }
        assertTrue(serieMedico != null, "El reporte debe conservar la serie del médico consultado");
        assertEquals(1, serieMedico.path("meses").get(fecha.getMonthValue() - 1).path("canceladas").asLong());
    }

    private String login(String email, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).path("token").asText();
    }

    private Long buscarMedicoId(String fragmentoNombre) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/medicos"))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode medico : objectMapper.readTree(resultado.getResponse().getContentAsString())) {
            if (medico.path("nombreCompleto").asText().contains(fragmentoNombre)) {
                return medico.path("id").asLong();
            }
        }
        throw new IllegalStateException("No se encontró el profesional de prueba: " + fragmentoNombre);
    }

    private Long reservar(String token, Long medicoId, LocalDate fecha) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/citas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"medicoId":%d,"fecha":"%s","hora":"08:00","modalidad":"PRESENCIAL","motivo":"Integración MySQL"}
                                """.formatted(medicoId, fecha)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).path("id").asLong();
    }

    private LocalDate siguienteHabil(LocalDate fecha) {
        LocalDate resultado = fecha;
        while (resultado.getDayOfWeek() == DayOfWeek.SATURDAY || resultado.getDayOfWeek() == DayOfWeek.SUNDAY) {
            resultado = resultado.plusDays(1);
        }
        return resultado;
    }
}
