package com.example.newcotizador.infrastructure.adapter.out.persistence.entity;

import com.example.newcotizador.domain.model.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(schema = "Cotizador", name = "Clientes")
@Getter @Setter @NoArgsConstructor
public class ClienteJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente") private Integer id;
    @Column(nullable = false, unique = true, length = 8) private String dni;
    @Column(nullable = false, length = 100) private String nombres;
    @Column(nullable = false, length = 100) private String apellidos;
    @Column(name = "score_crediticio") private Integer scoreCrediticio;
    @Column(name = "ingresos_mensuales", precision = 18, scale = 2) private BigDecimal ingresosMensuales;
}
