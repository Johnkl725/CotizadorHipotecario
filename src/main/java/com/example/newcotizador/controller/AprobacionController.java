package com.example.newcotizador.controller;
import com.example.newcotizador.dto.*;
import com.example.newcotizador.entity.EstadoCotizacion;
import com.example.newcotizador.service.CotizacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/aprobaciones") @RequiredArgsConstructor @PreAuthorize("hasRole('APROBADOR')")
public class AprobacionController {
    private final CotizacionService cotizaciones;
    @GetMapping public PaginaResponse<CotizacionResponse> listar(@RequestParam(defaultValue = "PENDIENTE_APROBACION") EstadoCotizacion estado,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) { return cotizaciones.bandeja(estado, page, size); }
    @PostMapping("/{id}/decision") public CotizacionResponse decidir(@PathVariable Integer id,
        @Valid @RequestBody DecisionRequest request, Authentication auth) { return cotizaciones.decidir(id, request, auth.getName()); }
}
