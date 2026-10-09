package com.example.newcotizador.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inmueble {
    private Integer inmuebleId;
    private Integer solicitudId;
    private String tipoInmueble;
    private String direccion;
    private String partidaRegistral;
    private BigDecimal valorComercial;
    private BigDecimal valorTasacion;
}
