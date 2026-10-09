package com.example.newcotizador.domain.port.out;
import com.example.newcotizador.domain.model.ProductoHipotecario;
import java.util.List;
import java.util.Optional;
public interface ProductoRepositoryPort {
    Optional<ProductoHipotecario> findById(Integer id);
    List<ProductoHipotecario> findAll();
}
