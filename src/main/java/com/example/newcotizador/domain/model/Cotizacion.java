package com.example.newcotizador.domain.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 @Getter @Setter @NoArgsConstructor
public class Cotizacion {
          private Integer id;
      private Long version;
      private Cliente cliente;
      private Usuario ejecutivo;
     private BigDecimal valorInmueble;
     private BigDecimal cuotaInicial;
     private BigDecimal montoPrestamo;
     private Integer plazoMeses;
     private BigDecimal ltvPorcentaje;
     private BigDecimal teaCalculada;
     private BigDecimal teaPreferencialSolicitada;
     private BigDecimal cuotaMensualEstimada;
     private BigDecimal ingresosMensuales;
     private BigDecimal deudasMensuales;
     private Integer scoreCrediticio;
     private BigDecimal dstiPorcentaje;
      private EstadoCotizacion estado;
     private LocalDateTime fechaCreacion;
      private Usuario aprobador;
    private String comentarioDecision;
     private LocalDateTime fechaDecision;
    private static final java.math.MathContext MC = new java.math.MathContext(34, java.math.RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal LIMIT = new BigDecimal("999999999.99");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    public void simular(PoliticaRiesgo politica) {
        simularConTea(politica, politica.teaBase());
    }

    public void simularConTea(PoliticaRiesgo politica, BigDecimal tea) {
        validar(politica, tea);
        this.montoPrestamo = this.valorInmueble.subtract(this.cuotaInicial);
        this.teaCalculada = tea;
        
        BigDecimal monthly = tea.signum() == 0 ? BigDecimal.ZERO
                : raizDoce(BigDecimal.ONE.add(tea.divide(HUNDRED, MC))).subtract(BigDecimal.ONE);
        BigDecimal periods = BigDecimal.valueOf(this.plazoMeses);
        BigDecimal payment;
        if (monthly.signum() == 0) {
            payment = this.montoPrestamo.divide(periods, MC);
        } else {
            BigDecimal growth = BigDecimal.ONE.add(monthly).pow(this.plazoMeses, MC);
            payment = this.montoPrestamo.multiply(monthly, MC).multiply(growth, MC)
                    .divide(growth.subtract(BigDecimal.ONE), MC);
        }
        this.cuotaMensualEstimada = dinero(payment);
        this.ltvPorcentaje = dinero(this.montoPrestamo.multiply(HUNDRED).divide(this.valorInmueble, MC));
        
        // Capacidad de pago (DSTI)
        BigDecimal dsti = this.cuotaMensualEstimada.add(this.deudasMensuales).multiply(HUNDRED)
                .divide(this.ingresosMensuales, MC);
        if (dsti.compareTo(new BigDecimal("9999999.99")) > 0)
            throw new IllegalArgumentException("La relación deuda/ingreso excede el rango admitido. Revisa los ingresos y deudas.");
        this.dstiPorcentaje = dinero(dsti);
    }

    private void validar(PoliticaRiesgo politica, BigDecimal tea) {
        monto(this.valorInmueble, true, "valor del inmueble");
        monto(this.cuotaInicial, false, "cuota inicial");
        monto(this.ingresosMensuales, true, "ingresos mensuales");
        monto(this.deudasMensuales, false, "deudas mensuales");
        if (this.plazoMeses == null || this.plazoMeses < 1 || this.plazoMeses > politica.plazoMaximoMeses())
            throw new IllegalArgumentException("Plazo fuera del rango permitido.");
        if (this.scoreCrediticio != null && (this.scoreCrediticio < 0 || this.scoreCrediticio > 999))
            throw new IllegalArgumentException("El score debe estar entre 0 y 999.");
        if (tea == null || tea.signum() < 0 || tea.compareTo(HUNDRED) > 0 || tea.scale() > 2)
            throw new IllegalArgumentException("La TEA debe estar entre 0 y 100 y tener hasta dos decimales.");
        if (this.cuotaInicial.compareTo(this.valorInmueble) >= 0)
            throw new IllegalArgumentException("La cuota inicial debe ser menor al valor del inmueble.");
        if (this.cuotaInicial.multiply(HUNDRED).compareTo(this.valorInmueble.multiply(politica.cuotaInicialMinimaPorcentaje())) < 0)
            throw new IllegalArgumentException("La cuota inicial mínima es " + politica.cuotaInicialMinimaPorcentaje() + "%.");
    }

    private static void monto(BigDecimal value, boolean positive, String label) {
        if (value == null || value.signum() < 0 || (positive && value.signum() == 0)
                || value.compareTo(LIMIT) > 0 || value.scale() > 2 || value.scale() < -9)
            throw new IllegalArgumentException("Importe inválido para " + label + ".");
    }

    private static BigDecimal dinero(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
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
