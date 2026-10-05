package com.example.newcotizador.domain.port.in;

import com.example.newcotizador.dto.CotizacionResponse;
import com.example.newcotizador.dto.CrearCotizacionRequest;
import com.example.newcotizador.dto.DecisionRequest;
import com.example.newcotizador.dto.PaginaResponse;
import com.example.newcotizador.dto.SolicitudTasaRequest;
import com.example.newcotizador.domain.model.EstadoCotizacion;

public interface CotizacionUseCase {
    CotizacionResponse crear(CrearCotizacionRequest request, String username);
    CotizacionResponse clonar(Integer idOriginal, String username);
    PaginaResponse<CotizacionResponse> propias(String username, int page, int size);
    PaginaResponse<CotizacionResponse> bandeja(EstadoCotizacion estado, int page, int size);
    CotizacionResponse solicitar(Integer id, SolicitudTasaRequest request, String username);
    CotizacionResponse decidir(Integer id, DecisionRequest request, String username);
}
