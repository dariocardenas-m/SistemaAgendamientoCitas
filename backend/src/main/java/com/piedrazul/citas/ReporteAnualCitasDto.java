package com.piedrazul.citas;

import java.util.List;

public record ReporteAnualCitasDto(
        int anio,
        List<SerieMedicoReporteDto> medicos) {
}
