package com.example.newcotizador.infrastructure.adapter.out.persistence.repository;

import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.ProductoHipotecarioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoHipotecarioRepository extends JpaRepository<ProductoHipotecarioJpaEntity, Short> {
}
