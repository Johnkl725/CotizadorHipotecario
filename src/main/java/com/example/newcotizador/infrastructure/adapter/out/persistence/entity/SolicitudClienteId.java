package com.example.newcotizador.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudClienteId implements Serializable {
    @Column(name = "solicitud_id")
    private Integer solicitudId;

    @Column(name = "cliente_id")
    private Integer clienteId;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SolicitudClienteId that = (SolicitudClienteId) o;
        return Objects.equals(solicitudId, that.solicitudId) && Objects.equals(clienteId, that.clienteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(solicitudId, clienteId);
    }
}
