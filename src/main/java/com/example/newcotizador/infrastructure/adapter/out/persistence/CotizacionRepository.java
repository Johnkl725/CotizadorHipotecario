package com.example.newcotizador.infrastructure.adapter.out.persistence;
import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.CotizacionJpaEntity;
import com.example.newcotizador.domain.model.EstadoCotizacion;
import java.util.Optional;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
public interface CotizacionRepository extends JpaRepository<CotizacionJpaEntity, Integer> {
    @EntityGraph(attributePaths = {"cliente", "ejecutivo", "aprobador"})
    Page<CotizacionJpaEntity> findByEjecutivoUsername(String username, Pageable pageable);
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    @EntityGraph(attributePaths = {"cliente", "ejecutivo", "aprobador"})
    Page<CotizacionJpaEntity> findByEstado(EstadoCotizacion estado, Pageable pageable);
    @Override @EntityGraph(attributePaths = {"cliente", "ejecutivo", "aprobador"})
    Optional<CotizacionJpaEntity> findById(Integer id);
}
