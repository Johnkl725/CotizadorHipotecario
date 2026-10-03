package com.example.newcotizador.infrastructure.adapter.out.persistence;
import com.example.newcotizador.domain.model.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findByDni(String dni);
}
