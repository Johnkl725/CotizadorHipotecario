package com.example.newcotizador.infrastructure.adapter.out.persistence;
import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.UsuarioJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UsuarioRepository extends JpaRepository<UsuarioJpaEntity, Integer> {
    Optional<UsuarioJpaEntity> findByUsername(String username);
}
