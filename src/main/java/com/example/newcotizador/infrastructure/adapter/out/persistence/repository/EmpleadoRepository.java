package com.example.newcotizador.infrastructure.adapter.out.persistence.repository;

import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.EmpleadoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpleadoRepository extends JpaRepository<EmpleadoJpaEntity, Integer> {
    java.util.Optional<EmpleadoJpaEntity> findByCodigoMatricula(String codigoMatricula);
}
