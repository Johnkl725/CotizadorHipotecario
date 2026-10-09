package com.example.newcotizador.dto;
import jakarta.validation.constraints.*;
public record DecisionSolicitudRequest(@NotNull @Min(0) Long version,
    @NotBlank @Size(max=255) String comentario) {}
