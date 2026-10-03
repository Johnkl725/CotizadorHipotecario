package com.example.newcotizador.infrastructure.adapter.out.persistence;
import com.example.newcotizador.domain.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsername(String username);
}
