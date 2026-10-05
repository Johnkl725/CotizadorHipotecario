package com.example.newcotizador.domain.port.out;

import com.example.newcotizador.domain.model.Cliente;
import java.util.Optional;

public interface ClienteRepositoryPort {
    Optional<Cliente> findByDni(String dni);
    Cliente save(Cliente cliente);
}
