package com.example.newcotizador.infrastructure.adapter.in.web;
import com.example.newcotizador.domain.port.in.SolicitudUseCase;
import com.example.newcotizador.domain.model.*;
import com.example.newcotizador.dto.SolicitudView.EmpleadoView;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api") @RequiredArgsConstructor @Validated
public class SessionController {
    private final SolicitudUseCase useCase;
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) {
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }
    @GetMapping("/session") public EmpleadoView session(Authentication auth) {
        return EmpleadoView.from(useCase.empleadoActual(auth.getName()));
    }
    @GetMapping("/productos") public List<ProductoHipotecario> productos(Authentication auth) {
        return useCase.productos(auth.getName());
    }
    @GetMapping("/clientes") public List<Cliente> clientes(Authentication auth,
        @RequestParam(defaultValue="") @Size(max=100) String q) {
        return useCase.clientes(auth.getName(),q);
    }
}
