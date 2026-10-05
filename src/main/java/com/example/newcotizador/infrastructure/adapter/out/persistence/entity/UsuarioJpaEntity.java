package com.example.newcotizador.infrastructure.adapter.out.persistence.entity;

import com.example.newcotizador.domain.model.*;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(schema = "Cotizador", name = "Usuarios")
@Getter @Setter @NoArgsConstructor
public class UsuarioJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario") private Integer id;
    @Column(nullable = false, unique = true, length = 50) private String username;
    @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
    @Column(nullable = false, length = 20) private String rol;
}
