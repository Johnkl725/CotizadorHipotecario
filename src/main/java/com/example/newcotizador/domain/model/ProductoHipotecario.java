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
public class ProductoHipotecario {
    private Short productoId;
    private String nombreProducto;
    private BigDecimal tasaInteresReferencial;
}
