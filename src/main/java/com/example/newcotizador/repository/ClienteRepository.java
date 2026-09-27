package com.example.newcotizador.repository;
import com.example.newcotizador.entity.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findByDni(String dni);
}
