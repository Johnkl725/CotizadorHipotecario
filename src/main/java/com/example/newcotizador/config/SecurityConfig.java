package com.example.newcotizador.config;

import com.example.newcotizador.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration @EnableMethodSecurity @RequiredArgsConstructor
public class SecurityConfig {
    private final UsuarioRepository usuarios;
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService userDetailsService() {
        return username -> usuarios.findByUsername(username)
            .map(u -> User.withUsername(u.getUsername()).password(u.getPasswordHash()).roles(u.getRol()).build())
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RequestMatcher api = request -> request.getServletPath().startsWith("/api/");
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error", "/actuator/health", "/actuator/health/**").permitAll()
            .requestMatchers("/actuator/**").denyAll()
            .requestMatchers("/api/aprobaciones/**", "/aprobaciones").hasRole("APROBADOR")
            .requestMatchers("/api/simulaciones", "/api/cotizaciones/**", "/simulador").hasRole("EJECUTIVO")
            .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor((request, response, e) -> jsonError(response, 401, "Tu sesión expiró. Inicia sesión nuevamente."), api)
                .accessDeniedHandler((request, response, e) -> {
                    if (api.matches(request)) jsonError(response, 403, "No tienes acceso o el token de seguridad expiró. Actualiza la página.");
                    else response.sendError(403);
                }))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; form-action 'self'; base-uri 'self'"))
                .frameOptions(frame -> frame.deny()));
        return http.build();
    }
    private static void jsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\",\"fieldErrors\":{}}");
    }
}
