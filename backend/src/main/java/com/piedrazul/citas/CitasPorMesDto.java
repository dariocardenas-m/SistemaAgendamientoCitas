package com.piedrazul.citas;

public record CitasPorMesDto(
        int mes,
        long confirmadas,
        long atendidas,
        long canceladas) {
}
