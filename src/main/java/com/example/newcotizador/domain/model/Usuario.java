package com.example.newcotizador.domain.model;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 @Getter @Setter @NoArgsConstructor
public class Usuario {
          private Integer id;
     private String username;
     private String passwordHash;
     private String rol;
}
