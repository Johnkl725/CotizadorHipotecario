package com.example.newcotizador.domain.port.out;
import com.example.newcotizador.domain.model.Cliente;
import java.util.List;
import java.util.Optional;
public interface ClienteRepositoryPort {
    Optional<Cliente> findById(Integer id);
    List<Cliente> buscar(String query);
}
