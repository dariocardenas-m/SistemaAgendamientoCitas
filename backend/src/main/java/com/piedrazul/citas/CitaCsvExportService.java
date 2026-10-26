package com.piedrazul.citas;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class CitaCsvExportService {

    private static final String ENCABEZADO = "Código,Hora,Paciente,Documento,Estado,Modalidad,Duración (minutos)\r\n";

    /**
     * Genera el archivo en memoria. No se persiste ningún archivo CSV en el servidor,
     * eliminando rutas manipulables y exposición de archivos temporales.
     */
    public byte[] generar(List<CitaDto> citas) {
        StringBuilder csv = new StringBuilder("\uFEFF").append(ENCABEZADO);
        for (CitaDto cita : citas) {
            csv.append(campo(cita.getCodigo())).append(',')
                    .append(campo(cita.getHora() == null ? "" : cita.getHora().toString())).append(',')
                    .append(campo(cita.getPacienteNombre())).append(',')
                    .append(campo(cita.getPacienteDocumento())).append(',')
                    .append(campo(cita.getEstado() == null ? "" : cita.getEstado().name())).append(',')
                    .append(campo(cita.getModalidad() == null ? "" : cita.getModalidad().name())).append(',')
                    .append(campo(cita.getDuracionMinutos() == null ? "" : cita.getDuracionMinutos().toString()))
                    .append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String campo(String valor) {
        String seguro = valor == null ? "" : valor.replace("\"", "\"\"");
        // Evita que una hoja de cálculo interprete datos controlados por usuarios como fórmulas.
        if (!seguro.isEmpty() && "=+-@".indexOf(seguro.charAt(0)) >= 0) {
            seguro = "'" + seguro;
        }
        return '"' + seguro + '"';
    }
}
