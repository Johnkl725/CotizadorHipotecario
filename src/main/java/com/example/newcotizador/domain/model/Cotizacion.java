package com.example.newcotizador.domain.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 @Getter @Setter @NoArgsConstructor
public class Cotizacion {
          private Integer id;
      private Long version;
      private Cliente cliente;
      private Usuario ejecutivo;
     private BigDecimal valorInmueble;
     private BigDecimal cuotaInicial;
     private BigDecimal montoPrestamo;
     private Integer plazoMeses;
     private BigDecimal ltvPorcentaje;
     private BigDecimal teaCalculada;
     private BigDecimal teaPreferencialSolicitada;
     private BigDecimal cuotaMensualEstimada;
     private BigDecimal ingresosMensuales;
     private BigDecimal deudasMensuales;
     private Integer scoreCrediticio;
     private BigDecimal dstiPorcentaje;
      private EstadoCotizacion estado;
     private LocalDateTime fechaCreacion;
      private Usuario aprobador;
    private String comentarioDecision;
     private LocalDateTime fechaDecision;
}
