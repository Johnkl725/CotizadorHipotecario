package com.example.newcotizador.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(schema = "Cotizador", name = "inmueble_garantia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InmuebleGarantiaJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inmueble_id")
    private Integer inmuebleId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudCreditoJpaEntity solicitud;
    
    @Column(name = "tipo_inmueble", nullable = false, length = 30)
    private String tipoInmueble;
    
    @Column(name = "direccion", nullable = false, length = 150)
    private String direccion;
    
    @Column(name = "partida_registral", length = 30)
    private String partidaRegistral;
    
    @Column(name = "valor_comercial", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorComercial;
    
    @Column(name = "valor_tasacion", precision = 12, scale = 2)
    private BigDecimal valorTasacion;
}
