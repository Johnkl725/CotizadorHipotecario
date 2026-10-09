package com.example.newcotizador.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(schema = "Cotizador", name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cliente_id")
    private Integer clienteId;
    
    @Column(name = "tipo_documento", nullable = false, length = 3)
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.CHAR)
    private String tipoDocumento;
    
    @Column(name = "numero_documento", nullable = false, length = 15)
    private String numeroDocumento;
    
    @Column(name = "nombres", nullable = false, length = 50)
    private String nombres;
    
    @Column(name = "apellidos", nullable = false, length = 50)
    private String apellidos;
    
    @Column(name = "email", length = 80)
    private String email;
    
    @Column(name = "ingreso_mensual_neto", nullable = false, precision = 12, scale = 2)
    private BigDecimal ingresoMensualNeto;
}
