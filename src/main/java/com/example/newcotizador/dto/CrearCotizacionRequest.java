package com.example.newcotizador.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CrearCotizacionRequest(
    @NotBlank @Pattern(regexp = "[0-9]{8}") String dni,
    @NotBlank @Size(max = 100) String nombres,
    @NotBlank @Size(max = 100) String apellidos,
    @NotNull @DecimalMin("0.01") @DecimalMax("999999999.99") @Digits(integer = 9, fraction = 2) BigDecimal valorInmueble,
    @NotNull @DecimalMin("0") @Digits(integer = 9, fraction = 2) BigDecimal cuotaInicial,
    @NotNull @Min(1) @Max(360) Integer plazoMeses,
    @NotNull @DecimalMin("0.01") @Digits(integer = 9, fraction = 2) BigDecimal ingresosMensuales,
    @NotNull @DecimalMin("0") @Digits(integer = 9, fraction = 2) BigDecimal deudasMensuales,
    @Min(0) @Max(999) Integer scoreCrediticio
) {
    public SimulacionRequest simulacion() {
        return new SimulacionRequest(valorInmueble, cuotaInicial, plazoMeses, ingresosMensuales, deudasMensuales, scoreCrediticio);
    }
}
