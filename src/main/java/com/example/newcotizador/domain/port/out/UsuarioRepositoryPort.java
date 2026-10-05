package com.example.newcotizador.domain.port.out;

import com.example.newcotizador.domain.model.Usuario;
import java.util.Optional;

public interface UsuarioRepositoryPort {
    Optional<Usuario> findByUsername(String username);
    Usuario save(Usuario usuario);
}
