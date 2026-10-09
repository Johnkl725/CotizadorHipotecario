package com.example.newcotizador.dto;
import jakarta.validation.constraints.*;
public record VersionRequest(@NotNull @Min(0) Long version) {}
