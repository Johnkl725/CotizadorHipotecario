package com.example.newcotizador.application.service;

import com.example.newcotizador.config.PoliticaProperties;
import com.example.newcotizador.dto.*;
import com.example.newcotizador.domain.model.*;
import com.example.newcotizador.exception.BusinessException;
import com.example.newcotizador.infrastructure.adapter.out.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class CotizacionService {
    private final CotizacionRepository cotizaciones;
    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;
    private final CalculoService calculo;
    private final PoliticaProperties politica;
    private final AuditoriaCotizacionRepository auditoria; // HU 3

    @Transactional(timeout = 10)
    public CotizacionResponse crear(CrearCotizacionRequest request, String username) {
        SimulacionResponse resultado = calculo.simular(request.simulacion());
        Cliente cliente = clientes.findByDni(request.dni()).orElseGet(() -> {
            Cliente nuevo = new Cliente();
            nuevo.setDni(request.dni()); nuevo.setNombres(request.nombres().strip()); nuevo.setApellidos(request.apellidos().strip());
            nuevo.setIngresosMensuales(request.ingresosMensuales()); nuevo.setScoreCrediticio(request.scoreCrediticio());
            return clientes.saveAndFlush(nuevo);
        });
        if (!cliente.getNombres().equalsIgnoreCase(request.nombres().strip()) || !cliente.getApellidos().equalsIgnoreCase(request.apellidos().strip())) {
            throw new BusinessException(HttpStatus.CONFLICT, "El DNI ya está registrado con otros nombres. Verifica la identidad del cliente.");
        }
        Cotizacion c = new Cotizacion();
        c.setCliente(cliente); c.setEjecutivo(usuario(username));
        c.setValorInmueble(request.valorInmueble()); c.setCuotaInicial(request.cuotaInicial()); c.setPlazoMeses(request.plazoMeses());
        c.setMontoPrestamo(resultado.montoPrestamo()); c.setLtvPorcentaje(resultado.ltvPorcentaje());
        c.setTeaCalculada(resultado.tea()); c.setCuotaMensualEstimada(resultado.cuotaMensual());
        c.setIngresosMensuales(request.ingresosMensuales()); c.setDeudasMensuales(request.deudasMensuales());
        c.setScoreCrediticio(request.scoreCrediticio()); c.setDstiPorcentaje(resultado.dstiPorcentaje());
        c.setEstado(EstadoCotizacion.BORRADOR); c.setFechaCreacion(LocalDateTime.now(ZoneOffset.UTC));
        return respuesta(cotizaciones.saveAndFlush(c));
    }

    // HU 1: Clonar Cotización Iterativa
    @Transactional(timeout = 10)
    public CotizacionResponse clonar(Integer idOriginal, String username) {
        Cotizacion original = buscar(idOriginal);
        if (!original.getEjecutivo().getUsername().equals(username)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "No puedes clonar cotizaciones de otros.");
        }
        Cotizacion clone = new Cotizacion();
        clone.setCliente(original.getCliente()); clone.setEjecutivo(original.getEjecutivo());
        clone.setValorInmueble(original.getValorInmueble()); clone.setCuotaInicial(original.getCuotaInicial());
        clone.setPlazoMeses(original.getPlazoMeses()); clone.setMontoPrestamo(original.getMontoPrestamo());
        clone.setLtvPorcentaje(original.getLtvPorcentaje()); clone.setTeaCalculada(original.getTeaCalculada());
        clone.setCuotaMensualEstimada(original.getCuotaMensualEstimada()); clone.setIngresosMensuales(original.getIngresosMensuales());
        clone.setDeudasMensuales(original.getDeudasMensuales()); clone.setScoreCrediticio(original.getScoreCrediticio());
        clone.setDstiPorcentaje(original.getDstiPorcentaje()); clone.setEstado(EstadoCotizacion.BORRADOR);
        clone.setFechaCreacion(LocalDateTime.now(ZoneOffset.UTC));
        Cotizacion guardada = cotizaciones.saveAndFlush(clone);
        guardarAuditoria(guardada, "COTIZACION_CLONADA", null, username);
        return respuesta(guardada);
    }

    @Transactional(readOnly = true, timeout = 5)
    public PaginaResponse<CotizacionResponse> propias(String username, int page, int size) {
        return PaginaResponse.of(cotizaciones.findByEjecutivoUsername(username, paginacion(page, size)).map(this::respuesta));
    }
    @Transactional(readOnly = true, timeout = 5)
    public PaginaResponse<CotizacionResponse> bandeja(EstadoCotizacion estado, int page, int size) {
        if (estado == EstadoCotizacion.BORRADOR) throw new IllegalArgumentException("Los borradores son privados del ejecutivo.");
        return PaginaResponse.of(cotizaciones.findByEstado(estado, paginacion(page, size)).map(this::respuesta));
    }
    
    @Transactional(timeout = 10)
    public CotizacionResponse solicitar(Integer id, SolicitudTasaRequest request, String username) {
        Cotizacion c = buscar(id);
        if (!c.getEjecutivo().getUsername().equals(username)) throw new BusinessException(HttpStatus.NOT_FOUND, "No se encontró la cotización.");
        validarVersion(c, request.version());
        if (c.getEstado() != EstadoCotizacion.BORRADOR) throw new BusinessException(HttpStatus.CONFLICT, "Solo puedes solicitar una tasa para un borrador.");
        List<String> motivos = motivos(c);
        if (!motivos.isEmpty()) throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, String.join(" ", motivos));
        if (request.teaPreferencial().compareTo(c.getTeaCalculada()) >= 0) throw new IllegalArgumentException("La tasa preferencial debe ser menor que la TEA original.");
        c.setTeaPreferencialSolicitada(request.teaPreferencial()); c.setEstado(EstadoCotizacion.PENDIENTE_APROBACION);
        cotizaciones.flush();
        guardarAuditoria(c, "TASA_PREFERENCIAL_SOLICITADA", request.teaPreferencial(), username); // HU 3
        return respuesta(c);
    }

    @Transactional(timeout = 10)
    public CotizacionResponse decidir(Integer id, DecisionRequest request, String username) {
        Cotizacion c = buscar(id);
        validarVersion(c, request.version());
        if (c.getEstado() != EstadoCotizacion.PENDIENTE_APROBACION) throw new BusinessException(HttpStatus.CONFLICT, "La solicitud ya no está pendiente.");
        if (c.getEjecutivo().getUsername().equals(username)) throw new BusinessException(HttpStatus.FORBIDDEN, "No puedes decidir sobre tu propia cotización.");
        if (request.aprobar()) {
            SimulacionResponse resultado = calculo.simularConTea(new SimulacionRequest(c.getValorInmueble(), c.getCuotaInicial(),
                c.getPlazoMeses(), c.getIngresosMensuales(), c.getDeudasMensuales(), c.getScoreCrediticio()), c.getTeaPreferencialSolicitada());
            c.setCuotaMensualEstimada(resultado.cuotaMensual()); c.setDstiPorcentaje(resultado.dstiPorcentaje());
            c.setEstado(EstadoCotizacion.APROBADA);
            guardarAuditoria(c, "TASA_PREFERENCIAL_APROBADA", c.getTeaPreferencialSolicitada(), username); // HU 3
        } else {
            c.setEstado(EstadoCotizacion.RECHAZADA);
            guardarAuditoria(c, "TASA_PREFERENCIAL_RECHAZADA", null, username); // HU 3
        }
        c.setAprobador(usuario(username)); c.setComentarioDecision(request.comentario().strip());
        c.setFechaDecision(LocalDateTime.now(ZoneOffset.UTC));
        cotizaciones.flush();
        return respuesta(c);
    }
    
    // Método auxiliar para Auditoría (HU 3)
    private void guardarAuditoria(Cotizacion c, String accion, BigDecimal teaNueva, String usuarioRes) {
        AuditoriaCotizacion aud = new AuditoriaCotizacion();
        aud.setCotizacion(c); aud.setAccion(accion); aud.setTeaAnterior(c.getTeaCalculada());
        aud.setTeaNueva(teaNueva); aud.setUsuarioResponsable(usuarioRes);
        aud.setFechaEvento(LocalDateTime.now(ZoneOffset.UTC));
        auditoria.save(aud);
    }

    private Cotizacion buscar(Integer id) { return cotizaciones.findById(id).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "No se encontró la cotización.")); }
    private Usuario usuario(String username) { return usuarios.findByUsername(username).orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Inicia sesión nuevamente.")); }
    private void validarVersion(Cotizacion c, Long version) { if (!c.getVersion().equals(version)) throw new BusinessException(HttpStatus.CONFLICT, "La cotización cambió. Actualiza la lista antes de continuar."); }
    private Pageable paginacion(int page, int size) {
        if (page < 0 || page > 10000 || size < 1 || size > 50) throw new IllegalArgumentException("Página o tamaño fuera de rango (máximo 50 registros).");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaCreacion", "id"));
    }
    private List<String> motivos(Cotizacion c) {
        List<String> motivos = new ArrayList<>();
        if (c.getScoreCrediticio() == null || c.getScoreCrediticio() < politica.scoreMinimo()) motivos.add("Score insuficiente o sin historial para solicitar una tasa preferencial.");
        if (c.getDstiPorcentaje() == null || c.getIngresosMensuales() == null || c.getDeudasMensuales() == null
            || c.getCuotaMensualEstimada().add(c.getDeudasMensuales()).multiply(new BigDecimal("100"))
                .compareTo(c.getIngresosMensuales().multiply(politica.dstiMaximo())) > 0)
            motivos.add("El DSTI supera el límite de la política o no está disponible.");
        if (c.getIngresosMensuales() == null || c.getIngresosMensuales().signum() <= 0 || c.getDeudasMensuales() == null) motivos.add("El perfil financiero está incompleto.");
        return List.copyOf(motivos);
    }
    private CotizacionResponse respuesta(Cotizacion c) {
        List<String> motivos = motivos(c);
        return new CotizacionResponse(c.getId(), c.getVersion(), c.getCliente().getDni(), c.getCliente().getNombres(), c.getCliente().getApellidos(),
            c.getEjecutivo().getUsername(), c.getValorInmueble(), c.getCuotaInicial(), c.getMontoPrestamo(), c.getPlazoMeses(), c.getLtvPorcentaje(),
            c.getTeaCalculada(), c.getTeaPreferencialSolicitada(), c.getCuotaMensualEstimada(), c.getIngresosMensuales(), c.getDeudasMensuales(),
            c.getScoreCrediticio(), c.getDstiPorcentaje(), c.getEstado().name(), c.getFechaCreacion(), c.getComentarioDecision(),
            c.getAprobador() == null ? null : c.getAprobador().getUsername(), motivos.isEmpty(), motivos);
    }
}
