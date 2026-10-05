package com.example.newcotizador.domain.model;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 @Getter @Setter @NoArgsConstructor
public class Cliente {
          private Integer id;
     private String dni;
     private String nombres;
     private String apellidos;
     private Integer scoreCrediticio;
     private BigDecimal ingresosMensuales;
}
