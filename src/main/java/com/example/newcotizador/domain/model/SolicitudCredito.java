package com.example.newcotizador.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCredito {
    private Integer solicitudId;
    private String numeroExpediente;
    private ProductoHipotecario producto;
    private Empleado ejecutivo;
    private Empleado gestorRiesgo;
    private BigDecimal montoSolicitado;
    private String moneda;
    private Short plazoMeses;
    private String estadoActual;
    private LocalDateTime fechaCreacion;
    private Long version;
    
    @Builder.Default
    private List<Participante> participantes = new ArrayList<>();
    
    @Builder.Default
    private List<Inmueble> inmuebles = new ArrayList<>();
    
    @Builder.Default
    private List<HistorialEstado> historial = new ArrayList<>();

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");
    
    // HU-02: Co-evaluación Crediticia
    public BigDecimal evaluarCapacidadPago() {
        if (participantes == null || participantes.isEmpty()) return BigDecimal.ZERO;
        return participantes.stream()
                .map(p -> p.getCliente().getIngresoMensualNeto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // HU-03: Ajuste de LTV por Tasación
    public BigDecimal calcularValorGarantia() {
        if (inmuebles == null || inmuebles.isEmpty()) return BigDecimal.ZERO;
        return inmuebles.stream()
                .map(i -> {
                    BigDecimal comercial = i.getValorComercial();
                    BigDecimal tasacion = i.getValorTasacion();
                    if (tasacion != null && tasacion.compareTo(comercial) < 0) {
                        return tasacion;
                    }
                    return comercial;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularLtv() {
        BigDecimal garantia = calcularValorGarantia();
        if (garantia.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return this.montoSolicitado.multiply(HUNDRED).divide(garantia, 2, RoundingMode.HALF_UP);
    }

    // HU-01: Simulación con Producto
    public BigDecimal simularCuotaMensual() {
        if (producto == null || producto.getTasaInteresReferencial() == null) return BigDecimal.ZERO;
        BigDecimal tea = producto.getTasaInteresReferencial();
        
        BigDecimal monthly = tea.signum() == 0 ? BigDecimal.ZERO
                : raizDoce(BigDecimal.ONE.add(tea.divide(HUNDRED, MC))).subtract(BigDecimal.ONE);
        
        BigDecimal periods = BigDecimal.valueOf(this.plazoMeses);
        BigDecimal payment;
        
        if (monthly.signum() == 0) {
            payment = this.montoSolicitado.divide(periods, MC);
        } else {
            BigDecimal growth = BigDecimal.ONE.add(monthly).pow(this.plazoMeses, MC);
            payment = this.montoSolicitado.multiply(monthly, MC).multiply(growth, MC)
                    .divide(growth.subtract(BigDecimal.ONE), MC);
        }
        return payment.setScale(2, RoundingMode.HALF_UP);
    }
    
    public BigDecimal calcularDsti() {
        BigDecimal ingresos = evaluarCapacidadPago();
        if (ingresos.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal("999.99");
        BigDecimal cuota = simularCuotaMensual();
        // Nota: Si los clientes tuvieran deudas en su modelo, se sumarían a la cuota aquí.
        return cuota.multiply(HUNDRED).divide(ingresos, 2, RoundingMode.HALF_UP);
    }

    // HU-04: Máquina de Estados
    public void registrar(Empleado ejecutivo) {
        if (!"EJECUTIVO_COMERCIAL".equals(ejecutivo.getRolPrincipal())) {
            throw new IllegalArgumentException("Solo un Ejecutivo Comercial puede registrar.");
        }
        this.estadoActual = "REGISTRADO";
        this.ejecutivo = ejecutivo;
        agregarHistorial(ejecutivo, null, "REGISTRADO", "Solicitud ingresada por ejecutivo");
    }

    public void enviarAEvaluacion(Empleado gestor) {
        if (!"REGISTRADO".equals(this.estadoActual)) {
            throw new IllegalStateException("Solo solicitudes REGISTRADAS pueden ir a evaluación.");
        }
        if (!"GESTOR_RIESGOS".equals(gestor.getRolPrincipal())) {
            throw new IllegalArgumentException("Solo un Gestor de Riesgos puede evaluar.");
        }
        String anterior = this.estadoActual;
        this.estadoActual = "EN_EVALUACION";
        this.gestorRiesgo = gestor;
        agregarHistorial(gestor, anterior, "EN_EVALUACION", "Inicia evaluación de riesgos");
    }

    public void aprobar(Empleado gestor, String comentario) {
        validarComentario(comentario);
        if (!"GESTOR_RIESGOS".equals(gestor.getRolPrincipal())) throw new IllegalArgumentException("Rol no autorizado.");
        if (!"EN_EVALUACION".equals(this.estadoActual)) {
            throw new IllegalStateException("La solicitud debe estar EN_EVALUACION.");
        }
        if (this.gestorRiesgo == null || !this.gestorRiesgo.getEmpleadoId().equals(gestor.getEmpleadoId())) {
            throw new IllegalArgumentException("Solo el gestor asignado puede aprobar.");
        }
        
        // Reglas de Riesgo Hard-coded de negocio
        if (calcularValorGarantia().signum() <= 0 || montoSolicitado.multiply(HUNDRED).compareTo(calcularValorGarantia().multiply(new BigDecimal("90"))) > 0) {
            throw new IllegalStateException("El LTV no puede superar el 90%.");
        }
        if (evaluarCapacidadPago().signum() <= 0 || simularCuotaMensual().multiply(HUNDRED).compareTo(evaluarCapacidadPago().multiply(new BigDecimal("40"))) > 0) {
            throw new IllegalStateException("El DSTI consolidado no puede superar el 40%.");
        }

        String anterior = this.estadoActual;
        this.estadoActual = "APROBADO";
        agregarHistorial(gestor, anterior, "APROBADO", comentario);
    }

    public void rechazar(Empleado gestor, String comentario) {
        validarComentario(comentario);
        if (!"GESTOR_RIESGOS".equals(gestor.getRolPrincipal()) || gestorRiesgo == null
            || !gestor.getEmpleadoId().equals(gestorRiesgo.getEmpleadoId()))
            throw new IllegalArgumentException("Solo el gestor asignado puede decidir.");
        if (!"EN_EVALUACION".equals(estadoActual)) throw new IllegalStateException("La solicitud debe estar EN_EVALUACION.");
        estadoActual = "RECHAZADO";
        agregarHistorial(gestor, "EN_EVALUACION", "RECHAZADO", comentario);
    }
    private static void validarComentario(String comentario) {
        if (comentario == null || comentario.isBlank() || comentario.length() > 255)
            throw new IllegalArgumentException("Se requiere un fundamento de hasta 255 caracteres.");
    }

    private void agregarHistorial(Empleado emp, String anterior, String nuevo, String comentario) {
        this.historial.add(HistorialEstado.builder()
                .empleadoId(emp.getEmpleadoId())
                .estadoAnterior(anterior)
                .estadoNuevo(nuevo)
                .fechaCambio(LocalDateTime.now())
                .comentario(comentario)
                .build());
    }

    private static BigDecimal raizDoce(BigDecimal value) {
        BigDecimal current = BigDecimal.ONE;
        for (int iteration = 0; iteration < 64; iteration++) {
            BigDecimal next = current.multiply(new BigDecimal("11"), MC)
                    .add(value.divide(current.pow(11, MC), MC), MC).divide(TWELVE, MC);
            if (next.subtract(current).abs().compareTo(new BigDecimal("1E-32")) <= 0) return next;
            current = next;
        }
        throw new IllegalStateException("La conversión de tasa no convergió.");
    }
}
