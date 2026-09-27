package com.example.newcotizador.config;

import com.example.newcotizador.entity.Usuario;
import com.example.newcotizador.repository.UsuarioRepository;
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
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final Environment environment;
    @Override @Transactional
    public void run(ApplicationArguments args) {
        crear("ejecutivo", "EJECUTIVO", "BOOTSTRAP_EJECUTIVO_PASSWORD");
        crear("aprobador", "APROBADOR", "BOOTSTRAP_APROBADOR_PASSWORD");
    }
    private void crear(String username, String rol, String key) {
        String password = environment.getProperty(key);
        if (password == null || password.isBlank()) return;
        if (password.length() < 12 || password.length() > 64) throw new IllegalArgumentException(key + " debe tener entre 12 y 64 caracteres.");
        var existing = usuarios.findByUsername(username);
        if (existing.isPresent()) {
            if (!existing.get().getRol().equals(rol) || !encoder.matches(password, existing.get().getPasswordHash()))
                throw new IllegalStateException("Las credenciales locales no coinciden con la cuenta " + username
                    + ". Restaura artifacts/private/access.xml original o configura las claves correctas. No se cambió la cuenta existente.");
            return;
        }
        Usuario user = new Usuario(); user.setUsername(username); user.setRol(rol); user.setPasswordHash(encoder.encode(password));
        usuarios.save(user);
    }
}
