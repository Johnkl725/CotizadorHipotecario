package com.example.newcotizador.domain.port.out;

import com.example.newcotizador.domain.model.AuditoriaCotizacion;

public interface AuditoriaRepositoryPort {
    AuditoriaCotizacion save(AuditoriaCotizacion auditoria);
}
