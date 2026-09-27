package com.example.newcotizador.dto;

import java.math.BigDecimal;
import java.util.List;

/** TEA y TEM se expresan como porcentajes, al igual que LTV y DSTI. */
public record SimulacionResponse(BigDecimal montoPrestamo, BigDecimal ltvPorcentaje,
        BigDecimal tea, BigDecimal tem, BigDecimal cuotaMensual, BigDecimal dstiPorcentaje,
        BigDecimal totalIntereses, boolean elegiblePreferencial, List<String> motivos) {
    public SimulacionResponse {
        motivos = List.copyOf(motivos);
    }
}
