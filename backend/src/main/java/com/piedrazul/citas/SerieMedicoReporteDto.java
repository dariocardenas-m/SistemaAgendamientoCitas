package com.piedrazul.citas;

import java.util.List;

public record SerieMedicoReporteDto(
        Long medicoId,
        String medicoNombre,
        List<CitasPorMesDto> meses) {
}
