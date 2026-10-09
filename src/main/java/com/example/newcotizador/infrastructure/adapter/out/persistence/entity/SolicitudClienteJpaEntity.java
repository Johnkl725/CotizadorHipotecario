package com.example.newcotizador.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(schema = "Cotizador", name = "solicitud_cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudClienteJpaEntity {

    @EmbeddedId
    private SolicitudClienteId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("solicitudId")
    @JoinColumn(name = "solicitud_id")
    private SolicitudCreditoJpaEntity solicitud;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("clienteId")
    @JoinColumn(name = "cliente_id")
    private ClienteJpaEntity cliente;
    
    @Column(name = "tipo_participacion", nullable = false, length = 20)
    private String tipoParticipacion;

    @Column(name = "ingreso_evaluado", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal ingresoEvaluado;
}
