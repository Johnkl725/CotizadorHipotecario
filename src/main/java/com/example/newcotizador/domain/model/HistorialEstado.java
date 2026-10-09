package com.example.newcotizador.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEstado {
    private Integer historialId;
    private Integer solicitudId;
    private Integer empleadoId;
    private String estadoAnterior;
    private String estadoNuevo;
    private LocalDateTime fechaCambio;
    private String comentario;
}
