package com.example.newcotizador.domain.port.out;

import com.example.newcotizador.domain.model.Empleado;

import java.util.Optional;

public interface EmpleadoRepositoryPort {
    Optional<Empleado> findById(Integer id);
    Optional<Empleado> findByCodigoMatricula(String codigoMatricula);
    Empleado save(Empleado empleado);
}
