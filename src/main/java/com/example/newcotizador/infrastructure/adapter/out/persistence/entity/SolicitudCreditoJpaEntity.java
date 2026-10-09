package com.example.newcotizador.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(schema = "Cotizador", name = "solicitud_credito")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudCreditoJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "solicitud_id")
    private Integer solicitudId;
    
    @Column(name = "numero_expediente", nullable = false, length = 20, unique = true)
    private String numeroExpediente;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoHipotecarioJpaEntity producto;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejecutivo_id", nullable = false)
    private EmpleadoJpaEntity ejecutivo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gestor_riesgo_id")
    private EmpleadoJpaEntity gestorRiesgo;
    
    @Column(name = "monto_solicitado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoSolicitado;

    @Column(name = "tasa_aplicada", nullable = false, precision = 5, scale = 2)
    private BigDecimal tasaAplicada;
    
    @Column(name = "moneda", nullable = false, length = 3)
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.CHAR)
    private String moneda;
    
    @Column(name = "plazo_meses", nullable = false)
    private Short plazoMeses;
    
    @Column(name = "estado_actual", nullable = false, length = 30)
    private String estadoActual;
    
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("tipoParticipacion DESC")
    private List<SolicitudClienteJpaEntity> participantes = new ArrayList<>();
    
    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InmuebleGarantiaJpaEntity> inmuebles = new ArrayList<>();
    
    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("historialId ASC")
    private List<SolicitudHistorialEstadoJpaEntity> historial = new ArrayList<>();
}
