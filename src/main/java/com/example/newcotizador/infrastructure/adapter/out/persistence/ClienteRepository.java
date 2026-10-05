package com.example.newcotizador.infrastructure.adapter.out.persistence;
import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.ClienteJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ClienteRepository extends JpaRepository<ClienteJpaEntity, Integer> {
    Optional<ClienteJpaEntity> findByDni(String dni);
}
