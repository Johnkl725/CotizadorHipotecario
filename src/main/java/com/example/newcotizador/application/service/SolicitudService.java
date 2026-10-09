package com.example.newcotizador.application.service;

import com.example.newcotizador.domain.model.*;
import com.example.newcotizador.domain.port.in.SolicitudUseCase;
import com.example.newcotizador.domain.port.out.*;
import com.example.newcotizador.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolicitudService implements SolicitudUseCase {
    private final SolicitudRepositoryPort solicitudes;
    private final EmpleadoRepositoryPort empleados;
    private final ProductoRepositoryPort productos;
    private final ClienteRepositoryPort clientes;

    @Override public Empleado empleadoActual(String matricula) {
        Empleado e = empleados.findByCodigoMatricula(matricula)
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "La cuenta ya no está disponible."));
        if (!Set.of("EJECUTIVO_COMERCIAL", "GESTOR_RIESGOS").contains(e.getRolPrincipal()))
            throw new BusinessException(HttpStatus.FORBIDDEN, "El rol no tiene acceso a créditos hipotecarios.");
        return e;
    }
    private Empleado exigirRol(String matricula, String rol) {
        Empleado e = empleadoActual(matricula);
        if (!rol.equals(e.getRolPrincipal())) throw new BusinessException(HttpStatus.FORBIDDEN, "No tienes acceso a esta operación.");
        return e;
    }
    @Override public List<ProductoHipotecario> productos(String matricula) {
        empleadoActual(matricula);
        return productos.findAll();
    }
    @Override public List<Cliente> clientes(String matricula, String query) {
        exigirRol(matricula, "EJECUTIVO_COMERCIAL");
        return clientes.buscar(query.trim());
    }
    @Override public SolicitudCredito consultar(String matricula, Integer id) {
        Empleado e = empleadoActual(matricula);
        SolicitudCredito s = solicitudes.findById(id)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Solicitud no encontrada."));
        if ("EJECUTIVO_COMERCIAL".equals(e.getRolPrincipal()) && !e.getEmpleadoId().equals(s.getEjecutivo().getEmpleadoId()))
            throw new BusinessException(HttpStatus.NOT_FOUND, "Solicitud no encontrada.");
        return s;
    }
    @Override public Pagina<SolicitudCredito> listar(String matricula, String estado, String query, int page, int size) {
        Empleado e = empleadoActual(matricula);
        if (page < 0 || size < 1 || size > 50) throw new IllegalArgumentException("Paginación inválida.");
        if (!estado.isEmpty() && !Set.of("REGISTRADO", "EN_EVALUACION", "APROBADO", "RECHAZADO").contains(estado))
            throw new IllegalArgumentException("Estado inválido.");
        return solicitudes.buscar("EJECUTIVO_COMERCIAL".equals(e.getRolPrincipal()) ? e.getEmpleadoId() : null,
            estado, query.trim(), page, size);
    }
    @Override @Transactional
    public SolicitudCredito registrarSolicitud(String matricula, SolicitudCredito s) {
        Empleado e = exigirRol(matricula, "EJECUTIVO_COMERCIAL");
        s.setProducto(productos.findById(s.getProducto().getProductoId().intValue())
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado.")));
        if (s.getProducto().getTasaInteresReferencial().signum() < 0 || s.getProducto().getTasaInteresReferencial().compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("La tasa del producto no es válida.");
        Set<Integer> ids = new HashSet<>();
        long titulares = s.getParticipantes().stream().filter(p -> "TITULAR".equals(p.getTipoParticipacion())).count();
        if (titulares != 1) throw new IllegalArgumentException("Se requiere exactamente un titular.");
        s.getParticipantes().forEach(p -> {
            if (!ids.add(p.getCliente().getClienteId())) throw new IllegalArgumentException("No se pueden repetir participantes.");
            p.setCliente(clientes.findById(p.getCliente().getClienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado.")));
            if (p.getCliente().getIngresoMensualNeto().signum() <= 0) throw new IllegalArgumentException("El cliente debe tener ingresos válidos.");
        });
        if (s.getMontoSolicitado().compareTo(s.calcularValorGarantia()) > 0)
            throw new IllegalArgumentException("El préstamo no puede superar el valor de las garantías.");
        s.setSolicitudId(null); s.setVersion(null); s.setGestorRiesgo(null);
        s.setNumeroExpediente("HIP-" + UUID.randomUUID().toString().replace("-", "").substring(0,16).toUpperCase(Locale.ROOT));
        s.setMoneda("PEN"); s.setFechaCreacion(LocalDateTime.now());
        s.setHistorial(new ArrayList<>());
        s.registrar(e);
        return solicitudes.save(s);
    }
    private void verificarVersion(SolicitudCredito s, Long version) {
        if (!Objects.equals(s.getVersion(), version)) throw new BusinessException(HttpStatus.CONFLICT,
            "El expediente cambió. Actualiza su detalle antes de continuar.");
    }
    @Override @Transactional
    public SolicitudCredito enviarAEvaluacion(String matricula, Integer id, Long version) {
        Empleado gestor = exigirRol(matricula, "GESTOR_RIESGOS");
        SolicitudCredito s = consultar(matricula, id);
        verificarVersion(s, version);
        s.enviarAEvaluacion(gestor);
        return solicitudes.save(s);
    }
    @Override @Transactional
    public SolicitudCredito decidir(String matricula, Integer id, Long version, boolean aprobar, String comentario) {
        Empleado gestor = exigirRol(matricula, "GESTOR_RIESGOS");
        SolicitudCredito s = consultar(matricula, id);
        verificarVersion(s, version);
        if (s.getGestorRiesgo() == null || !gestor.getEmpleadoId().equals(s.getGestorRiesgo().getEmpleadoId()))
            throw new BusinessException(HttpStatus.FORBIDDEN, "Solo el gestor asignado puede decidir.");
        if (aprobar) s.aprobar(gestor, comentario); else s.rechazar(gestor, comentario);
        return solicitudes.save(s);
    }
}
