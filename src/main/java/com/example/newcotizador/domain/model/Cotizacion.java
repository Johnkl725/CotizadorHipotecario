package com.example.newcotizador.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(schema = "Cotizador", name = "Cotizaciones")
@Getter @Setter @NoArgsConstructor
public class Cotizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cotizacion") private Integer id;
    @Version @Column(nullable = false) private Long version;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_cliente") private Cliente cliente;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_ejecutivo") private Usuario ejecutivo;
    @Column(name = "valor_inmueble", precision = 18, scale = 2, nullable = false) private BigDecimal valorInmueble;
    @Column(name = "cuota_inicial", precision = 18, scale = 2, nullable = false) private BigDecimal cuotaInicial;
    @Column(name = "monto_prestamo", precision = 18, scale = 2, nullable = false) private BigDecimal montoPrestamo;
    @Column(name = "plazo_meses", nullable = false) private Integer plazoMeses;
    @Column(name = "ltv_porcentaje", precision = 5, scale = 2, nullable = false) private BigDecimal ltvPorcentaje;
    @Column(name = "tea_calculada", precision = 5, scale = 2, nullable = false) private BigDecimal teaCalculada;
    @Column(name = "tea_preferencial_solicitada", precision = 5, scale = 2) private BigDecimal teaPreferencialSolicitada;
    @Column(name = "cuota_mensual_estimada", precision = 18, scale = 2, nullable = false) private BigDecimal cuotaMensualEstimada;
    @Column(name = "ingresos_mensuales", precision = 18, scale = 2) private BigDecimal ingresosMensuales;
    @Column(name = "deudas_mensuales", precision = 18, scale = 2) private BigDecimal deudasMensuales;
    @Column(name = "score_crediticio") private Integer scoreCrediticio;
    @Column(name = "dsti_porcentaje", precision = 9, scale = 2) private BigDecimal dstiPorcentaje;
    @Enumerated(EnumType.STRING) @Column(length = 20) private EstadoCotizacion estado;
    @Column(name = "fecha_creacion", columnDefinition = "datetime") private LocalDateTime fechaCreacion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_aprobador") private Usuario aprobador;
    @Column(name = "comentario_decision", length = 500, columnDefinition = "nvarchar(500)") private String comentarioDecision;
    @Column(name = "fecha_decision", columnDefinition = "datetime2") private LocalDateTime fechaDecision;
}
