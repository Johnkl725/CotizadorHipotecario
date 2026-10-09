package com.example.newcotizador.infrastructure.adapter.in.web;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class WebController {
    @GetMapping({"/","/login","/simulador","/aprobaciones"})
    String app() { return "forward:/index.html"; }
}
