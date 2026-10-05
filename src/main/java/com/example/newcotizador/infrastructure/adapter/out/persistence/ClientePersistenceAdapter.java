package com.example.newcotizador.infrastructure.adapter.out.persistence;

import com.example.newcotizador.domain.model.Cliente;
import com.example.newcotizador.domain.port.out.ClienteRepositoryPort;
import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.ClienteJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClientePersistenceAdapter implements ClienteRepositoryPort {

    private final ClienteRepository repository;

    @Override
    public Optional<Cliente> findByDni(String dni) {
        return repository.findByDni(dni).map(Mapper::toDomain);
    }

    @Override
    public Cliente save(Cliente cliente) {
        ClienteJpaEntity entity = Mapper.toEntity(cliente);
        ClienteJpaEntity saved = repository.save(entity);
        return Mapper.toDomain(saved);
    }
}
