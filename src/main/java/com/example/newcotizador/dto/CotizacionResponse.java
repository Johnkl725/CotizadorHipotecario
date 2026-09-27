package com.example.newcotizador.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record CotizacionResponse(
    Integer id, Long version, String dni, String nombres, String apellidos, String ejecutivo,
    BigDecimal valorInmueble, BigDecimal cuotaInicial, BigDecimal montoPrestamo, Integer plazoMeses,
    BigDecimal ltvPorcentaje, BigDecimal teaCalculada, BigDecimal teaPreferencialSolicitada,
    BigDecimal cuotaMensualEstimada, BigDecimal ingresosMensuales, BigDecimal deudasMensuales,
    Integer scoreCrediticio, BigDecimal dstiPorcentaje, String estado, LocalDateTime fechaCreacion,
    String comentarioDecision, String aprobador, boolean elegiblePreferencial, List<String> motivos
) {}
