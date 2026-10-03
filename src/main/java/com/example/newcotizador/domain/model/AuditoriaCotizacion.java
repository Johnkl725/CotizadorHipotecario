package com.example.newcotizador.domain.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter @Setter @NoArgsConstructor
public class AuditoriaCotizacion {
             private Integer id;
            private Cotizacion cotizacion;
        private String accion; // ej: "TASA_PREFERENCIAL_SOLICITADA", "TASA_APROBADA", "COTIZACION_CLONADA"
        private BigDecimal teaAnterior;
        private BigDecimal teaNueva;
        private String usuarioResponsable;
        private LocalDateTime fechaEvento;
}
