package com.example.newcotizador.infrastructure.adapter.out.persistence;

import com.example.newcotizador.domain.model.Cotizacion;
import com.example.newcotizador.domain.model.EstadoCotizacion;
import com.example.newcotizador.domain.port.out.CotizacionRepositoryPort;
import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.CotizacionJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CotizacionPersistenceAdapter implements CotizacionRepositoryPort {

    private final CotizacionRepository repository;

    @Override
    public Cotizacion saveAndFlush(Cotizacion cotizacion) {
        CotizacionJpaEntity entity = Mapper.toEntity(cotizacion);
        CotizacionJpaEntity saved = repository.saveAndFlush(entity);
        return Mapper.toDomain(saved);
    }

    @Override
    public void flush() {
        repository.flush();
    }

    @Override
    public Page<Cotizacion> findByEjecutivoUsername(String username, Pageable pageable) {
        return repository.findByEjecutivoUsername(username, pageable).map(Mapper::toDomain);
    }

    @Override
    public Page<Cotizacion> findByEstado(EstadoCotizacion estado, Pageable pageable) {
        return repository.findByEstado(estado, pageable).map(Mapper::toDomain);
    }

    @Override
    public Optional<Cotizacion> findById(Integer id) {
        return repository.findById(id).map(Mapper::toDomain);
    }
}
