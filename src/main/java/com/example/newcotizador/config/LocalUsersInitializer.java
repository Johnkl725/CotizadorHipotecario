package com.example.newcotizador.config;

import com.example.newcotizador.domain.model.Empleado;
import com.example.newcotizador.domain.port.out.EmpleadoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicitly enabled local bootstrap. Never creates accounts with default passwords. */
@Component @Profile("local") @RequiredArgsConstructor
public class LocalUsersInitializer implements ApplicationRunner {
    private final EmpleadoRepositoryPort empleados;
    private final PasswordEncoder encoder;
    private final Environment environment;
    
    @Override @Transactional
    public void run(ApplicationArguments args) {
        crear("ejecutivo", "Carlos", "Ejecutivo", "EJECUTIVO_COMERCIAL", "BOOTSTRAP_EJECUTIVO_PASSWORD");
        crear("aprobador", "Laura", "Evaluadora", "GESTOR_RIESGOS", "BOOTSTRAP_APROBADOR_PASSWORD");
    }
    
    private void crear(String matricula, String nombres, String apellidos, String rol, String key) {
        String password = environment.getProperty(key);
        if (password == null || password.isBlank()) return;
        if (password.length() < 12 || password.length() > 64) throw new IllegalArgumentException(key + " debe tener entre 12 y 64 caracteres.");
        
        var existing = empleados.findByCodigoMatricula(matricula);
        if (existing.isPresent()) {
            if (!existing.get().getRolPrincipal().equals(rol) || !encoder.matches(password, existing.get().getPasswordHash()))
                throw new IllegalStateException("Las credenciales locales no coinciden con la cuenta " + matricula
                    + ". Restaura artifacts/private/access.xml original o configura las claves correctas. No se cambió la cuenta existente.");
            return;
        }
        
        Empleado emp = new Empleado(); 
        emp.setCodigoMatricula(matricula); 
        emp.setNombres(nombres);
        emp.setApellidos(apellidos);
        emp.setRolPrincipal(rol); 
        emp.setPasswordHash(encoder.encode(password));
        empleados.save(emp);
    }
}
