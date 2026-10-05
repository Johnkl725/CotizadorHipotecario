package com.example.newcotizador.infrastructure.adapter.out.persistence;

import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.AuditoriaCotizacionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaCotizacionRepository extends JpaRepository<AuditoriaCotizacionJpaEntity, Integer> {
}
