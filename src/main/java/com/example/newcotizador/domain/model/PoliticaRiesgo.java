package com.example.newcotizador.domain.model;

import java.math.BigDecimal;

public record PoliticaRiesgo(
        BigDecimal teaBase,
        BigDecimal cuotaInicialMinimaPorcentaje,
        int scoreMinimo,
        BigDecimal dstiMaximo,
        int plazoMaximoMeses
) {}
