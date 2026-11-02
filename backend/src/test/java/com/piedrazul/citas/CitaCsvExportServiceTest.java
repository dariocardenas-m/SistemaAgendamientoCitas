package com.piedrazul.citas;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.piedrazul.usuarios.Usuario;
import com.piedrazul.pacientes.Paciente;
import com.piedrazul.medicos.Medico;
import com.piedrazul.especialidades.Especialidad;
import static org.junit.jupiter.api.Assertions.*;

class CitaCsvExportServiceTest {
    private final CitaCsvExportService service=new CitaCsvExportService();
    private CitaDto dto(String nombre){
        Usuario usuario=new Usuario();usuario.setNombreCompleto(nombre);
        Paciente paciente=new Paciente();paciente.setUsuario(usuario);
        Medico medico=new Medico();medico.setEspecialidad(new Especialidad());
        Cita cita=new Cita();cita.setPaciente(paciente);cita.setMedico(medico);
        return CitaDto.from(cita);
    }
    @Test void agendaVaciaSigueSiendoCsvConBomYEncabezado(){
        String csv=new String(service.generar(List.of()),StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFFCódigo,Fecha,Hora,Profesional,Paciente,Documento"));assertEquals(1,csv.lines().count());
    }
    @Test void conservaTildesComillasComasYSaltosDentroDelCampo(){
        CitaDto cita=dto("María, \"Pérez\"\nSegunda línea");
        String csv=new String(service.generar(List.of(cita)),StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"María, \"\"Pérez\"\"\nSegunda línea\""));assertFalse(csv.contains("null"));
    }
    @Test void neutralizaPrefijosDeFormulaSinCambiarElDatoOriginal(){
        for(String prefix:List.of("=","+","-","@")){
            String dato=prefix+"SUM(1,2)";CitaDto cita=dto(dato);
            String csv=new String(service.generar(List.of(cita)),StandardCharsets.UTF_8);
            assertTrue(csv.contains("\"'"+dato+"\""));assertEquals(dato,cita.getPacienteNombre());
        }
    }
}
