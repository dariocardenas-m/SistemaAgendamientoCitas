package com.piedrazul;
import com.piedrazul.citas.*;
import com.piedrazul.medicos.*;
import com.piedrazul.usuarios.*;
import com.piedrazul.pacientes.*;
import com.piedrazul.especialidades.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:pruebas;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.h2.console.enabled=false"
})
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
    @Autowired CitaRepository citas; @Autowired MedicoRepository medicos;
    @Autowired EspecialidadRepository especialidades; @Autowired UsuarioRepository usuarios;
    @Autowired PacienteRepository pacientes; @Autowired CitaService service;
    static final AtomicInteger SEQ=new AtomicInteger();
    String pacienteToken, otroToken, adminToken, agendadorToken;
    Medico medico; Paciente paciente;
    @BeforeEach void setup() throws Exception {
        pacienteToken=login("juan.perez@email.com","paciente123");
        adminToken=login("admin@piedrazul.com","admin123");
        agendadorToken=login("carlos.mendoza@piedrazul.com","agendador123");
        int n=SEQ.incrementAndGet();
        String data=mapper.writeValueAsString(Map.of("nombreCompleto","Paciente prueba","email","p"+n+"@example.test",
            "password","test12345","documento",String.valueOf(90000000+n),"telefono","3000000000"));
        var result=mvc.perform(post("/api/auth/registro").contentType("application/json").content(data)).andExpect(status().isOk()).andReturn();
        otroToken=mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        paciente=pacientes.findByUsuario(usuarios.findByEmailIgnoreCase("juan.perez@email.com").orElseThrow()).orElseThrow();
        medico=Fixtures.medico(); medico.setId(null); medico.setEspecialidad(especialidades.findAll().get(0));
        medico=medicos.saveAndFlush(medico);
    }
    String login(String email,String password) throws Exception {
        var r=mvc.perform(post("/api/auth/login").contentType("application/json").content(mapper.writeValueAsString(Map.of("email",email,"password",password))))
            .andExpect(status().isOk()).andReturn(); return mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }
    String body(LocalDate fecha,LocalTime hora) throws Exception {
        return mapper.writeValueAsString(Map.of("medicoId",medico.getId(),"fecha",fecha.toString(),"hora",hora.toString()));
    }
    long reservar() throws Exception {
        var r=mvc.perform(post("/api/citas").header("Authorization","Bearer "+pacienteToken).contentType("application/json")
            .content(body(LocalDate.now().plusDays(1),LocalTime.of(8,0)))).andExpect(status().isOk()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void registroPersisteHashBCrypt() {
        Usuario u=usuarios.findAll().stream().filter(x->x.getEmail().endsWith("@example.test")).findFirst().orElseThrow();
        assertThat(u.getPasswordHash()).startsWith("$2").isNotEqualTo("test12345");
    }
    @Test void loginIncorrectoDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"nadie@example.test\",\"password\":\"incorrecta\"}"))
            .andExpect(status().isUnauthorized());
    }
    @Test void sinTokenNoAccedeACitas() throws Exception { mvc.perform(get("/api/citas/mias")).andExpect(status().isForbidden()); }
    @Test void tokenInvalidoNoAccedeACitas() throws Exception {
        mvc.perform(get("/api/citas/mias").header("Authorization","Bearer invalido")).andExpect(status().isForbidden());
    }
    @Test void pacienteNoConfiguraVentana() throws Exception {
        mvc.perform(put("/api/configuracion").header("Authorization","Bearer "+pacienteToken).contentType("application/json").content("{\"ventanaSemanas\":4}"))
            .andExpect(status().isForbidden());
    }
    @Test void pacienteNoConfiguraProfesional() throws Exception {
        mvc.perform(put("/api/medicos/"+medico.getId()+"/configuracion").header("Authorization","Bearer "+pacienteToken).contentType("application/json")
            .content("{\"diasAtencion\":[\"LUN\"],\"horaApertura\":\"08:00\",\"horaCierre\":\"12:00\",\"intervaloMinutos\":30}"))
            .andExpect(status().isForbidden());
    }
    @Test void reservaDuplicadaDevuelve409() throws Exception {
        reservar(); mvc.perform(post("/api/citas").header("Authorization","Bearer "+otroToken).contentType("application/json")
            .content(body(LocalDate.now().plusDays(1),LocalTime.of(8,0)))).andExpect(status().isConflict());
    }
    @Test void cancelacionLiberaFranja() throws Exception {
        long id=reservar(); mvc.perform(patch("/api/citas/"+id+"/cancelar").header("Authorization","Bearer "+pacienteToken)).andExpect(status().isOk());
        mvc.perform(post("/api/citas").header("Authorization","Bearer "+otroToken).contentType("application/json")
            .content(body(LocalDate.now().plusDays(1),LocalTime.of(8,0)))).andExpect(status().isOk());
    }
    @Test void pacienteNoCancelaCitaAjena() throws Exception {
        long id=reservar(); mvc.perform(patch("/api/citas/"+id+"/cancelar").header("Authorization","Bearer "+otroToken)).andExpect(status().isForbidden());
    }
    @Test void agendaAgendadorFiltraFechaYMedico() throws Exception {
        reservar(); mvc.perform(get("/api/citas/consulta").header("Authorization","Bearer "+agendadorToken)
            .param("medicoId",medico.getId().toString()).param("fecha",LocalDate.now().plusDays(1).toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void datosObligatoriosDevuelven400() throws Exception {
        mvc.perform(post("/api/citas").header("Authorization","Bearer "+pacienteToken).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
    }
    @Test void dosTransaccionesSimultaneasSoloConfirmanUnaReserva() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
        Callable<Boolean> tarea=()->{start.await(); var r=Fixtures.reserva(LocalDate.now().plusDays(1),LocalTime.of(9,0)); r.setMedicoId(medico.getId());
            try { service.reservar(paciente.getUsuario(),r); return true; }
            catch(com.piedrazul.config.ConflictoReservaException e) {return false;} };
        try { Future<Boolean> a=pool.submit(tarea), b=pool.submit(tarea); start.countDown();
            assertThat(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(citas.findByMedicoIdAndFechaOrderByHoraAsc(medico.getId(),LocalDate.now().plusDays(1))).hasSize(1);
        } finally {pool.shutdownNow();}
    }
}
