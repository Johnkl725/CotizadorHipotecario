package com.example.newcotizador.domain.port.out;

import com.example.newcotizador.domain.model.Cotizacion;
import com.example.newcotizador.domain.model.EstadoCotizacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface CotizacionRepositoryPort {
    Cotizacion saveAndFlush(Cotizacion cotizacion);
    void flush();
    Page<Cotizacion> findByEjecutivoUsername(String username, Pageable pageable);
    Page<Cotizacion> findByEstado(EstadoCotizacion estado, Pageable pageable);
    Optional<Cotizacion> findById(Integer id);
}
