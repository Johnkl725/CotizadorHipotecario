package com.example.newcotizador.config;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Valores iniciales ilustrativos; requieren aprobación comercial antes de producción. */
@Validated
@ConfigurationProperties(prefix = "cotizador.politica")
public record PoliticaProperties(
        @DefaultValue("9.00") @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal teaBase,
        @DefaultValue("10.00") @NotNull @DecimalMin("0") @DecimalMax("99.99") BigDecimal cuotaInicialMinimaPorcentaje,
        @DefaultValue("700") @Min(0) @Max(999) int scoreMinimo,
        @DefaultValue("40.00") @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal dstiMaximo,
        @DefaultValue("360") @Min(1) @Max(360) int plazoMaximoMeses) {
}
