package com.example.newcotizador.domain.port.in;
import com.example.newcotizador.domain.model.*;
import java.util.List;
public interface SolicitudUseCase {
    Empleado empleadoActual(String matricula);
    List<ProductoHipotecario> productos(String matricula);
    List<Cliente> clientes(String matricula, String query);
    SolicitudCredito registrarSolicitud(String matricula, SolicitudCredito solicitud);
    SolicitudCredito consultar(String matricula, Integer solicitudId);
    Pagina<SolicitudCredito> listar(String matricula, String estado, String query, int page, int size);
    SolicitudCredito enviarAEvaluacion(String matricula, Integer solicitudId, Long version);
    SolicitudCredito decidir(String matricula, Integer solicitudId, Long version, boolean aprobar, String comentario);
}
