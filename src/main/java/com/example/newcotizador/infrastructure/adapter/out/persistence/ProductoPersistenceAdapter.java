package com.example.newcotizador.infrastructure.adapter.out.persistence;

import com.example.newcotizador.domain.model.ProductoHipotecario;
import com.example.newcotizador.domain.port.out.ProductoRepositoryPort;
import com.example.newcotizador.infrastructure.adapter.out.persistence.repository.ProductoHipotecarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductoPersistenceAdapter implements ProductoRepositoryPort {

    private final ProductoHipotecarioRepository repository;

    @Override
    public java.util.List<ProductoHipotecario> findAll() {
        return repository.findAll(org.springframework.data.domain.Sort.by("productoId")).stream().map(Mapper::toDomain).toList();
    }
    @Override
    public Optional<ProductoHipotecario> findById(Integer id) {
        return repository.findById(id.shortValue()).map(Mapper::toDomain);
    }
}
