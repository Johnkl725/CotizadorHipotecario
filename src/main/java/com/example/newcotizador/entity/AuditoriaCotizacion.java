package com.example.newcotizador.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "Cotizador", name = "Auditoria_Cotizaciones")
@Getter @Setter @NoArgsConstructor
public class AuditoriaCotizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cotizacion", nullable = false)
    private Cotizacion cotizacion;

    @Column(name = "accion", length = 100, nullable = false)
    private String accion; // ej: "TASA_PREFERENCIAL_SOLICITADA", "TASA_APROBADA", "COTIZACION_CLONADA"

    @Column(name = "tea_anterior", precision = 5, scale = 2)
    private BigDecimal teaAnterior;

    @Column(name = "tea_nueva", precision = 5, scale = 2)
    private BigDecimal teaNueva;

    @Column(name = "usuario_responsable", length = 50)
    private String usuarioResponsable;

    @Column(name = "fecha_evento", columnDefinition = "datetime", nullable = false)
    private LocalDateTime fechaEvento;
}
