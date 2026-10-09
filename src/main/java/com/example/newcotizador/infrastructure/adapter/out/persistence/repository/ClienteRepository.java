package com.example.newcotizador.infrastructure.adapter.out.persistence.repository;
import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.ClienteJpaEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.util.List;
public interface ClienteRepository extends JpaRepository<ClienteJpaEntity, Integer> {
    @Query("select c from ClienteJpaEntity c where :query = '' or lower(concat(c.nombres, ' ', c.apellidos)) like lower(concat('%', :query, '%')) or c.numeroDocumento like concat('%', :query, '%') order by c.apellidos, c.clienteId")
    List<ClienteJpaEntity> buscar(String query, Pageable page);
}
