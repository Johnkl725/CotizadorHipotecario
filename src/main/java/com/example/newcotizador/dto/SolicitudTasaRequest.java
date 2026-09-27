package com.example.newcotizador.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record SolicitudTasaRequest(
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal teaPreferencial,
    @NotNull @Min(0) Long version
) {}
