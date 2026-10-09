package com.example.newcotizador.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SolicitudCreditoTest {

    private ProductoHipotecario mivivienda;
    private Empleado ejecutivo;
    private Empleado gestor;
    private Cliente titular;
    private Cliente coSolicitante;

    private static BigDecimal bd(String val) { return new BigDecimal(val); }

    @BeforeEach
    void setup() {
        mivivienda = ProductoHipotecario.builder().productoId((short) 1).nombreProducto("MiVivienda").tasaInteresReferencial(bd("7.0")).build();
        ejecutivo = Empleado.builder().empleadoId(1).rolPrincipal("EJECUTIVO_COMERCIAL").build();
        gestor = Empleado.builder().empleadoId(2).rolPrincipal("GESTOR_RIESGOS").build();
        titular = Cliente.builder().clienteId(1).ingresoMensualNeto(bd("3000")).build();
        coSolicitante = Cliente.builder().clienteId(2).ingresoMensualNeto(bd("2500")).build();
    }

    @Test
    void evaluarCapacidadPago_sumaIngresosTitularYCoSolicitante() {
        SolicitudCredito solicitud = SolicitudCredito.builder()
                .participantes(List.of(
                        Participante.builder().cliente(titular).tipoParticipacion("TITULAR").build(),
                        Participante.builder().cliente(coSolicitante).tipoParticipacion("CO_SOLICITANTE").build()
                )).build();
        
        assertThat(solicitud.evaluarCapacidadPago()).isEqualByComparingTo("5500");
    }

    @Test
    void calcularLtv_usaValorTasacionSiEsMenorQueComercial() {
        SolicitudCredito solicitud = SolicitudCredito.builder()
                .montoSolicitado(bd("80000"))
                .inmuebles(List.of(
                        Inmueble.builder().valorComercial(bd("100000")).valorTasacion(bd("90000")).build() // Usa 90k
                )).build();
        
        // 80,000 / 90,000 = 88.89%
        assertThat(solicitud.calcularLtv()).isEqualByComparingTo("88.89");
    }

    @Test
    void flujoEstados_validaRolesYReglasRiesgo() {
        SolicitudCredito solicitud = SolicitudCredito.builder()
                .montoSolicitado(bd("80000")) // LTV = 88.89% (con inmueble de 90k)
                .plazoMeses((short) 240)
                .producto(mivivienda) // Tasa 7% -> Cuota aprox ~620
                .participantes(List.of(
                        Participante.builder().cliente(titular).tipoParticipacion("TITULAR").build() // Ingreso 3000
                ))
                .inmuebles(List.of(
                        Inmueble.builder().valorComercial(bd("100000")).valorTasacion(bd("90000")).build()
                ))
                .build();
        
        // 1. Registro
        solicitud.registrar(ejecutivo);
        assertThat(solicitud.getEstadoActual()).isEqualTo("REGISTRADO");
        assertThat(solicitud.getHistorial()).hasSize(1);
        
        // 2. Envío a Evaluación
        solicitud.enviarAEvaluacion(gestor);
        assertThat(solicitud.getEstadoActual()).isEqualTo("EN_EVALUACION");
        assertThat(solicitud.getGestorRiesgo().getEmpleadoId()).isEqualTo(2);

        // 3. Aprobación (Cumple reglas: LTV <= 90%, DSTI <= 40%)
        // Cuota: 620.24. Ingreso: 3000. DSTI = 20.67%
        solicitud.aprobar(gestor, "Todo en orden");
        assertThat(solicitud.getEstadoActual()).isEqualTo("APROBADO");
    }

    @Test
    void rechazaAprobacionSiDstiExcedeLimite() {
        // Reducimos el ingreso para que exceda el 40% de DSTI
        Cliente clientePobre = Cliente.builder().clienteId(1).ingresoMensualNeto(bd("1500")).build();
        
        SolicitudCredito solicitud = SolicitudCredito.builder()
                .montoSolicitado(bd("80000"))
                .plazoMeses((short) 240)
                .producto(mivivienda)
                .participantes(List.of(Participante.builder().cliente(clientePobre).tipoParticipacion("TITULAR").build()))
                .inmuebles(List.of(Inmueble.builder().valorComercial(bd("100000")).build()))
                .build();
        
        solicitud.registrar(ejecutivo);
        solicitud.enviarAEvaluacion(gestor);
        
        assertThatIllegalStateException()
                .isThrownBy(() -> solicitud.aprobar(gestor, "Aprobado"))
                .withMessageContaining("DSTI consolidado no puede superar el 40%");
    }
}
