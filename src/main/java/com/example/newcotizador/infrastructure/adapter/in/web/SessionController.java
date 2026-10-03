package com.example.newcotizador.infrastructure.adapter.in.web;
import com.example.newcotizador.config.PoliticaProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class SessionController {
    private final PoliticaProperties politica;
    @GetMapping("/session") public Map<String,String> session(Authentication auth, CsrfToken csrf) {
        return Map.of("username", auth.getName(), "role", auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""),
            "csrfToken", csrf.getToken(), "csrfHeader", csrf.getHeaderName());
    }
    @GetMapping("/politica") public PoliticaProperties politica() { return politica; }
}
