package com.example.newcotizador.infrastructure.adapter.out.persistence;

import com.example.newcotizador.domain.model.*;
import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.*;

import java.util.stream.Collectors;

public class Mapper {
    public static Cliente toDomain(ClienteJpaEntity e) {
        return Cliente.builder().clienteId(e.getClienteId()).tipoDocumento(e.getTipoDocumento())
            .numeroDocumento(e.getNumeroDocumento()).nombres(e.getNombres()).apellidos(e.getApellidos())
            .email(e.getEmail()).ingresoMensualNeto(e.getIngresoMensualNeto()).build();
    }


    public static Empleado toDomain(EmpleadoJpaEntity entity) {
        if (entity == null) return null;
        return Empleado.builder()
                .empleadoId(entity.getEmpleadoId())
                .nombres(entity.getNombres())
                .apellidos(entity.getApellidos())
                .codigoMatricula(entity.getCodigoMatricula())
                .rolPrincipal(entity.getRolPrincipal())
                .passwordHash(entity.getPasswordHash())
                .build();
    }

    public static EmpleadoJpaEntity toEntity(Empleado domain) {
        if (domain == null) return null;
        EmpleadoJpaEntity entity = new EmpleadoJpaEntity();
        entity.setEmpleadoId(domain.getEmpleadoId());
        entity.setNombres(domain.getNombres());
        entity.setApellidos(domain.getApellidos());
        entity.setCodigoMatricula(domain.getCodigoMatricula());
        entity.setRolPrincipal(domain.getRolPrincipal());
        entity.setPasswordHash(domain.getPasswordHash());
        return entity;
    }

    public static ProductoHipotecario toDomain(ProductoHipotecarioJpaEntity entity) {
        if (entity == null) return null;
        return ProductoHipotecario.builder()
                .productoId(entity.getProductoId())
                .nombreProducto(entity.getNombreProducto())
                .tasaInteresReferencial(entity.getTasaInteresReferencial())
                .build();
    }

    public static ProductoHipotecarioJpaEntity toEntity(ProductoHipotecario domain) {
        if (domain == null) return null;
        ProductoHipotecarioJpaEntity entity = new ProductoHipotecarioJpaEntity();
        entity.setProductoId(domain.getProductoId());
        entity.setNombreProducto(domain.getNombreProducto());
        entity.setTasaInteresReferencial(domain.getTasaInteresReferencial());
        return entity;
    }

    public static SolicitudCredito toDomain(SolicitudCreditoJpaEntity entity) {
        if (entity == null) return null;
        SolicitudCredito domain = SolicitudCredito.builder()
                .solicitudId(entity.getSolicitudId())
                .numeroExpediente(entity.getNumeroExpediente())
                .producto(toDomain(entity.getProducto()))
                .ejecutivo(toDomain(entity.getEjecutivo()))
                .gestorRiesgo(toDomain(entity.getGestorRiesgo()))
                .montoSolicitado(entity.getMontoSolicitado())
                .moneda(entity.getMoneda())
                .plazoMeses(entity.getPlazoMeses())
                .estadoActual(entity.getEstadoActual())
                .fechaCreacion(entity.getFechaCreacion())
                .version(entity.getVersion())
                .build();

        domain.getProducto().setTasaInteresReferencial(entity.getTasaAplicada());

        if (entity.getParticipantes() != null) {
            domain.setParticipantes(entity.getParticipantes().stream()
                    .map(Mapper::toDomain)
                    .collect(Collectors.toList()));
        }

        if (entity.getInmuebles() != null) {
            domain.setInmuebles(entity.getInmuebles().stream()
                    .map(Mapper::toDomain)
                    .collect(Collectors.toList()));
        }

        if (entity.getHistorial() != null) {
            domain.setHistorial(entity.getHistorial().stream()
                    .map(Mapper::toDomain)
                    .collect(Collectors.toList()));
        }

        return domain;
    }

    public static SolicitudCreditoJpaEntity toEntity(SolicitudCredito domain) {
        if (domain == null) return null;
        SolicitudCreditoJpaEntity entity = new SolicitudCreditoJpaEntity();
        entity.setSolicitudId(domain.getSolicitudId());
        entity.setNumeroExpediente(domain.getNumeroExpediente());
        entity.setProducto(toEntity(domain.getProducto()));
        entity.setEjecutivo(toEntity(domain.getEjecutivo()));
        entity.setGestorRiesgo(toEntity(domain.getGestorRiesgo()));
        entity.setMontoSolicitado(domain.getMontoSolicitado());
        entity.setTasaAplicada(domain.getProducto().getTasaInteresReferencial());
        entity.setMoneda(domain.getMoneda());
        entity.setPlazoMeses(domain.getPlazoMeses());
        entity.setEstadoActual(domain.getEstadoActual());
        entity.setFechaCreacion(domain.getFechaCreacion());
        entity.setVersion(domain.getVersion());

        if (domain.getParticipantes() != null) {
            entity.setParticipantes(domain.getParticipantes().stream()
                    .map(p -> {
                        SolicitudClienteJpaEntity pEnt = toEntity(p);
                        pEnt.setSolicitud(entity);
                        return pEnt;
                    })
                    .collect(Collectors.toList()));
        }

        if (domain.getInmuebles() != null) {
            entity.setInmuebles(domain.getInmuebles().stream()
                    .map(i -> {
                        InmuebleGarantiaJpaEntity iEnt = toEntity(i);
                        iEnt.setSolicitud(entity);
                        return iEnt;
                    })
                    .collect(Collectors.toList()));
        }

        if (domain.getHistorial() != null) {
            entity.setHistorial(domain.getHistorial().stream()
                    .map(h -> {
                        SolicitudHistorialEstadoJpaEntity hEnt = toEntity(h);
                        hEnt.setSolicitud(entity);
                        return hEnt;
                    })
                    .collect(Collectors.toList()));
        }

        return entity;
    }

    public static Participante toDomain(SolicitudClienteJpaEntity entity) {
        if (entity == null) return null;
        Cliente c = new Cliente();
        if (entity.getCliente() != null) {
            c.setClienteId(entity.getCliente().getClienteId());
            c.setNumeroDocumento(entity.getCliente().getNumeroDocumento());
            c.setIngresoMensualNeto(entity.getIngresoEvaluado());
            c.setNombres(entity.getCliente().getNombres());
            c.setApellidos(entity.getCliente().getApellidos());
            c.setTipoDocumento(entity.getCliente().getTipoDocumento());
            c.setEmail(entity.getCliente().getEmail());
        }
        return Participante.builder()
                .tipoParticipacion(entity.getTipoParticipacion())
                .cliente(c)
                .build();
    }

    public static SolicitudClienteJpaEntity toEntity(Participante domain) {
        if (domain == null) return null;
        SolicitudClienteJpaEntity entity = new SolicitudClienteJpaEntity();
        
        SolicitudClienteId id = new SolicitudClienteId();
        if (domain.getCliente() != null) {
            id.setClienteId(domain.getCliente().getClienteId());
            ClienteJpaEntity c = new ClienteJpaEntity();
            c.setClienteId(domain.getCliente().getClienteId());
            c.setNumeroDocumento(domain.getCliente().getNumeroDocumento());
            c.setIngresoMensualNeto(domain.getCliente().getIngresoMensualNeto());
            c.setNombres(domain.getCliente().getNombres());
            c.setApellidos(domain.getCliente().getApellidos());
            c.setTipoDocumento(domain.getCliente().getTipoDocumento());
            c.setEmail(domain.getCliente().getEmail());
            entity.setCliente(c);
        }
        entity.setId(id);
        
        entity.setTipoParticipacion(domain.getTipoParticipacion());
        entity.setIngresoEvaluado(domain.getCliente().getIngresoMensualNeto());
        return entity;
    }

    public static Inmueble toDomain(InmuebleGarantiaJpaEntity entity) {
        if (entity == null) return null;
        return Inmueble.builder()
                .inmuebleId(entity.getInmuebleId())
                .tipoInmueble(entity.getTipoInmueble())
                .direccion(entity.getDireccion())
                .valorComercial(entity.getValorComercial())
                .valorTasacion(entity.getValorTasacion())
                .partidaRegistral(entity.getPartidaRegistral())
                .build();
    }

    public static InmuebleGarantiaJpaEntity toEntity(Inmueble domain) {
        if (domain == null) return null;
        InmuebleGarantiaJpaEntity entity = new InmuebleGarantiaJpaEntity();
        entity.setInmuebleId(domain.getInmuebleId());
        entity.setTipoInmueble(domain.getTipoInmueble());
        entity.setDireccion(domain.getDireccion());
        entity.setValorComercial(domain.getValorComercial());
        entity.setValorTasacion(domain.getValorTasacion());
        entity.setPartidaRegistral(domain.getPartidaRegistral());
        return entity;
    }

    public static HistorialEstado toDomain(SolicitudHistorialEstadoJpaEntity entity) {
        if (entity == null) return null;
        return HistorialEstado.builder()
                .historialId(entity.getHistorialId())
                .estadoAnterior(entity.getEstadoAnterior())
                .estadoNuevo(entity.getEstadoNuevo())
                .fechaCambio(entity.getFechaCambio())
                .comentario(entity.getComentario())
                .empleadoId(entity.getEmpleado() != null ? entity.getEmpleado().getEmpleadoId() : null)
                .build();
    }

    public static SolicitudHistorialEstadoJpaEntity toEntity(HistorialEstado domain) {
        if (domain == null) return null;
        SolicitudHistorialEstadoJpaEntity entity = new SolicitudHistorialEstadoJpaEntity();
        entity.setHistorialId(domain.getHistorialId());
        entity.setEstadoAnterior(domain.getEstadoAnterior());
        entity.setEstadoNuevo(domain.getEstadoNuevo());
        entity.setFechaCambio(domain.getFechaCambio());
        entity.setComentario(domain.getComentario());
        if (domain.getEmpleadoId() != null) {
            EmpleadoJpaEntity emp = new EmpleadoJpaEntity();
            emp.setEmpleadoId(domain.getEmpleadoId());
            entity.setEmpleado(emp);
        }
        return entity;
    }
}
