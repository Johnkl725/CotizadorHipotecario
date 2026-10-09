package com.example.newcotizador.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(schema = "Cotizador", name = "producto_hipotecario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoHipotecarioJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "producto_id")
    private Short productoId;
    
    @Column(name = "nombre_producto", nullable = false, length = 50)
    private String nombreProducto;
    
    @Column(name = "tasa_interes_referencial", nullable = false, precision = 5, scale = 2)
    private BigDecimal tasaInteresReferencial;
}
