package com.example.newcotizador.controller;
import com.example.newcotizador.dto.*;
import com.example.newcotizador.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api") @RequiredArgsConstructor @PreAuthorize("hasRole('EJECUTIVO')")
public class CotizacionController {
    private final CalculoService calculo;
    private final CotizacionService cotizaciones;
    @PostMapping("/simulaciones") public SimulacionResponse simular(@Valid @RequestBody SimulacionRequest request) { return calculo.simular(request); }
    @PostMapping("/cotizaciones") @ResponseStatus(HttpStatus.CREATED)
    public CotizacionResponse crear(@Valid @RequestBody CrearCotizacionRequest request, Authentication auth) { return cotizaciones.crear(request, auth.getName()); }
    @GetMapping("/cotizaciones") public PaginaResponse<CotizacionResponse> listar(Authentication auth,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) { return cotizaciones.propias(auth.getName(), page, size); }
    @PostMapping("/cotizaciones/{id}/solicitud") public CotizacionResponse solicitar(@PathVariable Integer id,
        @Valid @RequestBody SolicitudTasaRequest request, Authentication auth) { return cotizaciones.solicitar(id, request, auth.getName()); }
}
