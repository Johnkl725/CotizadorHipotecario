package com.example.newcotizador.infrastructure.adapter.out.persistence;

import com.example.newcotizador.domain.model.Empleado;
import com.example.newcotizador.domain.port.out.EmpleadoRepositoryPort;
import com.example.newcotizador.infrastructure.adapter.out.persistence.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmpleadoPersistenceAdapter implements EmpleadoRepositoryPort {

    private final EmpleadoRepository repository;

    @Override
    public Optional<Empleado> findById(Integer id) {
        return repository.findById(id).map(Mapper::toDomain);
    }
    
    @Override
    public Optional<Empleado> findByCodigoMatricula(String codigoMatricula) {
        return repository.findByCodigoMatricula(codigoMatricula).map(Mapper::toDomain);
    }
    
    @Override
    public Empleado save(Empleado empleado) {
        return Mapper.toDomain(repository.save(Mapper.toEntity(empleado)));
    }
}
