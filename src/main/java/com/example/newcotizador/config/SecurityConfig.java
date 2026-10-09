package com.example.newcotizador.config;
import com.example.newcotizador.domain.port.out.EmpleadoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration @EnableMethodSecurity @RequiredArgsConstructor
public class SecurityConfig {
    private final EmpleadoRepositoryPort empleados;
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService userDetailsService() {
        return username -> empleados.findByCodigoMatricula(username)
            .map(e -> User.withUsername(e.getCodigoMatricula()).password(e.getPasswordHash()).roles(e.getRolPrincipal()).build())
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
    }
    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/csrf","/api/login","/error","/actuator/health","/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET,"/","/login","/simulador","/aprobaciones","/index.html","/*.js","/*.css","/*.svg","/*.ico","/assets/**").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/solicitudes/registrar").hasRole("EJECUTIVO_COMERCIAL")
                .requestMatchers(HttpMethod.POST,"/api/solicitudes/*/evaluar","/api/solicitudes/*/aprobar","/api/solicitudes/*/rechazar").hasRole("GESTOR_RIESGOS")
                .requestMatchers("/api/clientes").hasRole("EJECUTIVO_COMERCIAL")
                .requestMatchers("/api/**").hasAnyRole("EJECUTIVO_COMERCIAL","GESTOR_RIESGOS")
                .anyRequest().denyAll())
            .formLogin(f -> f.loginProcessingUrl("/api/login")
                .successHandler((req,res,auth) -> { res.setContentType("application/json"); res.getWriter().write("{\"authenticated\":true}"); })
                .failureHandler((req,res,ex) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Credenciales incorrectas.\"}"); }))
            .logout(l -> l.logoutUrl("/api/logout").invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req,res,auth) -> res.setStatus(204)))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req,res,ex) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Inicia sesion para continuar.\"}"); })
                .accessDeniedHandler((req,res,ex) -> { res.setStatus(403); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Operacion no autorizada o token CSRF vencido.\"}"); }))
            .requestCache(c -> c.disable())
            .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")));
        return http.build();
    }
}
