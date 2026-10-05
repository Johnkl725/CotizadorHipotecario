package com.example.newcotizador.infrastructure.adapter.out.persistence;

import com.example.newcotizador.domain.model.Usuario;
import com.example.newcotizador.domain.port.out.UsuarioRepositoryPort;
import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.UsuarioJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioRepository repository;

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return repository.findByUsername(username).map(Mapper::toDomain);
    }

    @Override
    public Usuario save(Usuario usuario) {
        return Mapper.toDomain(repository.save(Mapper.toEntity(usuario)));
    }
}
