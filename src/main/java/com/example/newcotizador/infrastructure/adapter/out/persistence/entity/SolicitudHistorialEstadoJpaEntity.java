package com.example.newcotizador.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(schema = "Cotizador", name = "solicitud_historial_estado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudHistorialEstadoJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historial_id")
    private Integer historialId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudCreditoJpaEntity solicitud;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private EmpleadoJpaEntity empleado;
    
    @Column(name = "estado_anterior", length = 30)
    private String estadoAnterior;
    
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private String estadoNuevo;
    
    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime fechaCambio;
    
    @Column(name = "comentario", length = 255)
    private String comentario;
}
