package com.example.newcotizador.dto;
import com.example.newcotizador.domain.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SolicitudView(Integer solicitudId, String numeroExpediente, ProductoHipotecario producto,
    EmpleadoView ejecutivo, EmpleadoView gestorRiesgo, BigDecimal montoSolicitado, String moneda,
    Short plazoMeses, String estadoActual, LocalDateTime fechaCreacion, Long version,
    List<Participante> participantes, List<Inmueble> inmuebles, List<HistorialEstado> historial,
    BigDecimal cuotaMensual, BigDecimal ltv, BigDecimal dsti) {
    public record EmpleadoView(Integer empleadoId, String codigoMatricula, String nombres, String apellidos, String rolPrincipal) {
        public static EmpleadoView from(Empleado e) {
            return e == null ? null : new EmpleadoView(e.getEmpleadoId(),e.getCodigoMatricula(),e.getNombres(),e.getApellidos(),e.getRolPrincipal());
        }
    }
    public static SolicitudView from(SolicitudCredito s) {
        return new SolicitudView(s.getSolicitudId(),s.getNumeroExpediente(),s.getProducto(),
            EmpleadoView.from(s.getEjecutivo()),EmpleadoView.from(s.getGestorRiesgo()),s.getMontoSolicitado(),
            s.getMoneda(),s.getPlazoMeses(),s.getEstadoActual(),s.getFechaCreacion(),s.getVersion(),
            s.getParticipantes(),s.getInmuebles(),s.getHistorial(),s.simularCuotaMensual(),s.calcularLtv(),s.calcularDsti());
    }
}
