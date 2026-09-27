package com.example.newcotizador.dto;
import jakarta.validation.constraints.*;
public record DecisionRequest(@NotNull Boolean aprobar, @NotBlank @Size(max = 500) String comentario,
                              @NotNull @Min(0) Long version) {}
