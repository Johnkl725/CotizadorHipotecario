package com.example.newcotizador.infrastructure.adapter.out.persistence;
import com.example.newcotizador.domain.model.Cliente;
import com.example.newcotizador.domain.port.out.ClienteRepositoryPort;
import com.example.newcotizador.infrastructure.adapter.out.persistence.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import java.util.*;
@Component @RequiredArgsConstructor
public class ClientePersistenceAdapter implements ClienteRepositoryPort {
    private final ClienteRepository repository;
    public Optional<Cliente> findById(Integer id) { return repository.findById(id).map(Mapper::toDomain); }
    public List<Cliente> buscar(String query) { return repository.buscar(query, PageRequest.of(0,20)).stream().map(Mapper::toDomain).toList(); }
}
