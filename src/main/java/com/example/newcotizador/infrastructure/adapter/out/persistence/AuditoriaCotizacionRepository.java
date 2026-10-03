package com.example.newcotizador.infrastructure.adapter.out.persistence;

import com.example.newcotizador.domain.model.AuditoriaCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaCotizacionRepository extends JpaRepository<AuditoriaCotizacion, Integer> {
}
