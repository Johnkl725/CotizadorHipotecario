package com.example.newcotizador.domain.port.out;
import com.example.newcotizador.domain.model.*;
import java.util.Optional;
public interface SolicitudRepositoryPort {
    SolicitudCredito save(SolicitudCredito solicitud);
    Optional<SolicitudCredito> findById(Integer id);
    Pagina<SolicitudCredito> buscar(Integer ejecutivoId, String estado, String query, int page, int size);
}
