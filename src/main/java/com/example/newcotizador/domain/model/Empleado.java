package com.example.newcotizador.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Empleado {
    private Integer empleadoId;
    private String codigoMatricula;
    private String nombres;
    private String apellidos;
    private String rolPrincipal;
    @lombok.ToString.Exclude
    private String passwordHash;
}
