package com.example.newcotizador.infrastructure.adapter.in.web;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class WebController {
    @GetMapping("/") String home(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_APROBADOR"))
            ? "redirect:/aprobaciones" : "redirect:/simulador";
    }
    @GetMapping("/login") String login() { return "login"; }
    @GetMapping("/simulador") String simulador() { return "simulador"; }
    @GetMapping("/aprobaciones") String aprobaciones() { return "aprobaciones"; }
}
