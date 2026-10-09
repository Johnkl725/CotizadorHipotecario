package com.example.newcotizador.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(schema = "Cotizador", name = "empleado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpleadoJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "empleado_id")
    private Integer empleadoId;
    
    @Column(name = "codigo_matricula", nullable = false, length = 10, unique = true)
    private String codigoMatricula;
    
    @Column(name = "nombres", nullable = false, length = 50)
    private String nombres;
    
    @Column(name = "apellidos", nullable = false, length = 50)
    private String apellidos;
    
    @Column(name = "rol_principal", nullable = false, length = 30)
    private String rolPrincipal;
    @jakarta.persistence.Column(name = "password_hash")
    private String passwordHash;
}
