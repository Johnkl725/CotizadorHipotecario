package com.example.newcotizador.infrastructure.adapter.out.persistence;

import com.example.newcotizador.domain.model.*;
import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.*;

public class Mapper {
    public static Usuario toDomain(UsuarioJpaEntity entity) {
        if (entity == null) return null;
        Usuario domain = new Usuario();
        domain.setId(entity.getId());
        domain.setUsername(entity.getUsername());
        domain.setPasswordHash(entity.getPasswordHash());
        domain.setRol(entity.getRol());
        return domain;
    }

    public static UsuarioJpaEntity toEntity(Usuario domain) {
        if (domain == null) return null;
        UsuarioJpaEntity entity = new UsuarioJpaEntity();
        entity.setId(domain.getId());
        entity.setUsername(domain.getUsername());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setRol(domain.getRol());
        return entity;
    }

    public static Cliente toDomain(ClienteJpaEntity entity) {
        if (entity == null) return null;
        Cliente domain = new Cliente();
        domain.setId(entity.getId());
        domain.setDni(entity.getDni());
        domain.setNombres(entity.getNombres());
        domain.setApellidos(entity.getApellidos());
        domain.setScoreCrediticio(entity.getScoreCrediticio());
        domain.setIngresosMensuales(entity.getIngresosMensuales());
        return domain;
    }

    public static ClienteJpaEntity toEntity(Cliente domain) {
        if (domain == null) return null;
        ClienteJpaEntity entity = new ClienteJpaEntity();
        entity.setId(domain.getId());
        entity.setDni(domain.getDni());
        entity.setNombres(domain.getNombres());
        entity.setApellidos(domain.getApellidos());
        entity.setScoreCrediticio(domain.getScoreCrediticio());
        entity.setIngresosMensuales(domain.getIngresosMensuales());
        return entity;
    }

    public static Cotizacion toDomain(CotizacionJpaEntity entity) {
        if (entity == null) return null;
        Cotizacion domain = new Cotizacion();
        domain.setId(entity.getId());
        domain.setVersion(entity.getVersion());
        domain.setCliente(toDomain(entity.getCliente()));
        domain.setEjecutivo(toDomain(entity.getEjecutivo()));
        domain.setValorInmueble(entity.getValorInmueble());
        domain.setCuotaInicial(entity.getCuotaInicial());
        domain.setMontoPrestamo(entity.getMontoPrestamo());
        domain.setPlazoMeses(entity.getPlazoMeses());
        domain.setLtvPorcentaje(entity.getLtvPorcentaje());
        domain.setTeaCalculada(entity.getTeaCalculada());
        domain.setTeaPreferencialSolicitada(entity.getTeaPreferencialSolicitada());
        domain.setCuotaMensualEstimada(entity.getCuotaMensualEstimada());
        domain.setIngresosMensuales(entity.getIngresosMensuales());
        domain.setDeudasMensuales(entity.getDeudasMensuales());
        domain.setScoreCrediticio(entity.getScoreCrediticio());
        domain.setDstiPorcentaje(entity.getDstiPorcentaje());
        domain.setEstado(entity.getEstado());
        domain.setFechaCreacion(entity.getFechaCreacion());
        domain.setAprobador(toDomain(entity.getAprobador()));
        domain.setComentarioDecision(entity.getComentarioDecision());
        domain.setFechaDecision(entity.getFechaDecision());
        return domain;
    }

    public static CotizacionJpaEntity toEntity(Cotizacion domain) {
        if (domain == null) return null;
        CotizacionJpaEntity entity = new CotizacionJpaEntity();
        entity.setId(domain.getId());
        entity.setVersion(domain.getVersion());
        entity.setCliente(toEntity(domain.getCliente()));
        entity.setEjecutivo(toEntity(domain.getEjecutivo()));
        entity.setValorInmueble(domain.getValorInmueble());
        entity.setCuotaInicial(domain.getCuotaInicial());
        entity.setMontoPrestamo(domain.getMontoPrestamo());
        entity.setPlazoMeses(domain.getPlazoMeses());
        entity.setLtvPorcentaje(domain.getLtvPorcentaje());
        entity.setTeaCalculada(domain.getTeaCalculada());
        entity.setTeaPreferencialSolicitada(domain.getTeaPreferencialSolicitada());
        entity.setCuotaMensualEstimada(domain.getCuotaMensualEstimada());
        entity.setIngresosMensuales(domain.getIngresosMensuales());
        entity.setDeudasMensuales(domain.getDeudasMensuales());
        entity.setScoreCrediticio(domain.getScoreCrediticio());
        entity.setDstiPorcentaje(domain.getDstiPorcentaje());
        entity.setEstado(domain.getEstado());
        entity.setFechaCreacion(domain.getFechaCreacion());
        entity.setAprobador(toEntity(domain.getAprobador()));
        entity.setComentarioDecision(domain.getComentarioDecision());
        entity.setFechaDecision(domain.getFechaDecision());
        return entity;
    }

    public static AuditoriaCotizacion toDomain(AuditoriaCotizacionJpaEntity entity) {
        if (entity == null) return null;
        AuditoriaCotizacion domain = new AuditoriaCotizacion();
        domain.setId(entity.getId());
        domain.setCotizacion(toDomain(entity.getCotizacion()));
        domain.setAccion(entity.getAccion());
        domain.setTeaAnterior(entity.getTeaAnterior());
        domain.setTeaNueva(entity.getTeaNueva());
        domain.setUsuarioResponsable(entity.getUsuarioResponsable());
        domain.setFechaEvento(entity.getFechaEvento());
        return domain;
    }

    public static AuditoriaCotizacionJpaEntity toEntity(AuditoriaCotizacion domain) {
        if (domain == null) return null;
        AuditoriaCotizacionJpaEntity entity = new AuditoriaCotizacionJpaEntity();
        entity.setId(domain.getId());
        entity.setCotizacion(toEntity(domain.getCotizacion()));
        entity.setAccion(domain.getAccion());
        entity.setTeaAnterior(domain.getTeaAnterior());
        entity.setTeaNueva(domain.getTeaNueva());
        entity.setUsuarioResponsable(domain.getUsuarioResponsable());
        entity.setFechaEvento(domain.getFechaEvento());
        return entity;
    }
}
