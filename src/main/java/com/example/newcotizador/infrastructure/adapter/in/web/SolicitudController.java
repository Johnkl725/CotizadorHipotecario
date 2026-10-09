package com.example.newcotizador.infrastructure.adapter.in.web;
import com.example.newcotizador.domain.model.Pagina;
import com.example.newcotizador.domain.port.in.SolicitudUseCase;
import com.example.newcotizador.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api/solicitudes") @RequiredArgsConstructor @Validated
public class SolicitudController {
    private final SolicitudUseCase useCase;
    @PostMapping("/registrar") @ResponseStatus(HttpStatus.CREATED)
    public SolicitudView registrar(Authentication auth, @Valid @RequestBody RegistroSolicitudRequest request) {
        return SolicitudView.from(useCase.registrarSolicitud(auth.getName(), request.toDomain()));
    }
    @GetMapping
    public Pagina<SolicitudView> listar(Authentication auth, @RequestParam(defaultValue="") String estado,
        @RequestParam(defaultValue="") @Size(max=100) String q,
        @RequestParam(defaultValue="0") @Min(0) int page, @RequestParam(defaultValue="12") @Min(1) @Max(50) int size) {
        var result = useCase.listar(auth.getName(),estado,q,page,size);
        return new Pagina<>(result.content().stream().map(SolicitudView::from).toList(),result.totalElements(),result.totalPages(),result.number());
    }
    @GetMapping("/{id}")
    public SolicitudView detalle(Authentication auth, @PathVariable @Min(1) Integer id) {
        return SolicitudView.from(useCase.consultar(auth.getName(),id));
    }
    @PostMapping("/{id}/evaluar")
    public SolicitudView evaluar(Authentication auth, @PathVariable @Min(1) Integer id, @Valid @RequestBody VersionRequest request) {
        return SolicitudView.from(useCase.enviarAEvaluacion(auth.getName(),id,request.version()));
    }
    @PostMapping("/{id}/aprobar")
    public SolicitudView aprobar(Authentication auth, @PathVariable @Min(1) Integer id, @Valid @RequestBody DecisionSolicitudRequest request) {
        return SolicitudView.from(useCase.decidir(auth.getName(),id,request.version(),true,request.comentario().trim()));
    }
    @PostMapping("/{id}/rechazar")
    public SolicitudView rechazar(Authentication auth, @PathVariable @Min(1) Integer id, @Valid @RequestBody DecisionSolicitudRequest request) {
        return SolicitudView.from(useCase.decidir(auth.getName(),id,request.version(),false,request.comentario().trim()));
    }
}
