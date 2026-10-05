package com.example.newcotizador.infrastructure.adapter.out.persistence;

import com.example.newcotizador.domain.model.AuditoriaCotizacion;
import com.example.newcotizador.domain.port.out.AuditoriaRepositoryPort;
import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.AuditoriaCotizacionJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuditoriaPersistenceAdapter implements AuditoriaRepositoryPort {

    private final AuditoriaCotizacionRepository repository;

    @Override
    public AuditoriaCotizacion save(AuditoriaCotizacion auditoria) {
        AuditoriaCotizacionJpaEntity entity = Mapper.toEntity(auditoria);
        AuditoriaCotizacionJpaEntity saved = repository.save(entity);
        return Mapper.toDomain(saved);
    }
}
