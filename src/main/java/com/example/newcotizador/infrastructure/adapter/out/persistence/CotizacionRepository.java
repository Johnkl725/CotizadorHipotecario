package com.example.newcotizador.infrastructure.adapter.out.persistence;
import com.example.newcotizador.domain.model.*;
import java.util.Optional;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
public interface CotizacionRepository extends JpaRepository<Cotizacion, Integer> {
    @EntityGraph(attributePaths = {"cliente", "ejecutivo", "aprobador"})
    Page<Cotizacion> findByEjecutivoUsername(String username, Pageable pageable);
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    @EntityGraph(attributePaths = {"cliente", "ejecutivo", "aprobador"})
    Page<Cotizacion> findByEstado(EstadoCotizacion estado, Pageable pageable);
    @Override @EntityGraph(attributePaths = {"cliente", "ejecutivo", "aprobador"})
    Optional<Cotizacion> findById(Integer id);
}
