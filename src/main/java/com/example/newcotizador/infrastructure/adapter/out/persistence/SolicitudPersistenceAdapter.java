package com.example.newcotizador.infrastructure.adapter.out.persistence;

import com.example.newcotizador.domain.model.*;
import com.example.newcotizador.domain.port.out.SolicitudRepositoryPort;
import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.*;
import com.example.newcotizador.infrastructure.adapter.out.persistence.repository.SolicitudCreditoRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Component @RequiredArgsConstructor @Transactional(readOnly=true)
public class SolicitudPersistenceAdapter implements SolicitudRepositoryPort {
    private final SolicitudCreditoRepository repository;
    private final EntityManager entityManager;

    @Override @Transactional
    public SolicitudCredito save(SolicitudCredito s) {
        SolicitudCreditoJpaEntity entity;
        if (s.getSolicitudId() == null) {
            entity = Mapper.toEntity(s);
            entity.setProducto(entityManager.getReference(ProductoHipotecarioJpaEntity.class, s.getProducto().getProductoId()));
            entity.setEjecutivo(entityManager.getReference(EmpleadoJpaEntity.class, s.getEjecutivo().getEmpleadoId()));
            entity.getParticipantes().forEach(p -> p.setCliente(entityManager.getReference(ClienteJpaEntity.class, p.getCliente().getClienteId())));
            entity.getHistorial().forEach(h -> h.setEmpleado(entityManager.getReference(EmpleadoJpaEntity.class, h.getEmpleado().getEmpleadoId())));
        } else {
            entity = repository.findById(s.getSolicitudId()).orElseThrow();
            if (!entity.getVersion().equals(s.getVersion()))
                throw new ObjectOptimisticLockingFailureException(SolicitudCreditoJpaEntity.class, s.getSolicitudId());
            // Mutate the managed aggregate. Do not replace/delete persisted children.
            entity.setEstadoActual(s.getEstadoActual());
            if (s.getGestorRiesgo() != null)
                entity.setGestorRiesgo(entityManager.getReference(EmpleadoJpaEntity.class, s.getGestorRiesgo().getEmpleadoId()));
            s.getHistorial().stream().filter(h -> h.getHistorialId() == null).forEach(h -> {
                var entry = Mapper.toEntity(h);
                entry.setSolicitud(entity);
                entry.setEmpleado(entityManager.getReference(EmpleadoJpaEntity.class,h.getEmpleadoId()));
                entity.getHistorial().add(entry);
            });
        }
        return Mapper.toDomain(repository.saveAndFlush(entity));
    }
    @Override public Optional<SolicitudCredito> findById(Integer id) { return repository.findById(id).map(Mapper::toDomain); }
    @Override public Pagina<SolicitudCredito> buscar(Integer ejecutivoId,String estado,String query,int page,int size) {
        var result = repository.buscar(ejecutivoId,estado,query,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"fechaCreacion","solicitudId")));
        return new Pagina<>(result.getContent().stream().map(Mapper::toDomain).toList(),result.getTotalElements(),result.getTotalPages(),result.getNumber());
    }
}
