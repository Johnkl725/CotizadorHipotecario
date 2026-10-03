package com.example.newcotizador.application.service;

import com.example.newcotizador.config.PoliticaProperties;
import com.example.newcotizador.dto.SimulacionRequest;
import com.example.newcotizador.dto.SimulacionResponse;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalculoService {
    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal LIMIT = new BigDecimal("999999999.99");
    private static final BigDecimal TWELVE = new BigDecimal("12");
    private final PoliticaProperties politica;

    public SimulacionResponse simular(SimulacionRequest request) {
        return simularConTea(request, politica.teaBase());
    }

    public SimulacionResponse simularConTea(SimulacionRequest r, BigDecimal tea) {
        validar(r, tea);
        BigDecimal principal = r.valorInmueble().subtract(r.cuotaInicial());
        BigDecimal monthly = tea.signum() == 0 ? BigDecimal.ZERO
                : raizDoce(BigDecimal.ONE.add(tea.divide(HUNDRED, MC))).subtract(BigDecimal.ONE);
        BigDecimal periods = BigDecimal.valueOf(r.plazoMeses());
        BigDecimal payment;
        if (monthly.signum() == 0) {
            payment = principal.divide(periods, MC);
        } else {
            BigDecimal growth = BigDecimal.ONE.add(monthly).pow(r.plazoMeses(), MC);
            payment = principal.multiply(monthly, MC).multiply(growth, MC)
                    .divide(growth.subtract(BigDecimal.ONE), MC);
        }
        BigDecimal roundedPayment = dinero(payment);
        BigDecimal ltv = principal.multiply(HUNDRED).divide(r.valorInmueble(), MC);
        // La capacidad de pago incluye la cuota a cobrar, además de la deuda existente.
        BigDecimal dsti = roundedPayment.add(r.deudasMensuales()).multiply(HUNDRED)
                .divide(r.ingresosMensuales(), MC);
        if (dsti.compareTo(new BigDecimal("9999999.99")) > 0)
            throw new IllegalArgumentException("La relación deuda/ingreso excede el rango admitido. Revisa los ingresos y deudas.");
        var motivos = new ArrayList<String>();
        if (r.scoreCrediticio() == null) motivos.add("Se requiere score crediticio para solicitar una tasa preferencial.");
        else if (r.scoreCrediticio() < politica.scoreMinimo()) motivos.add("Score inferior al mínimo de " + politica.scoreMinimo() + ".");
        if (dsti.compareTo(politica.dstiMaximo()) > 0) motivos.add("DSTI superior al máximo de " + politica.dstiMaximo() + "%.");
        return new SimulacionResponse(dinero(principal), dinero(ltv), dinero(tea),
                monthly.multiply(HUNDRED).setScale(12, RoundingMode.HALF_UP), roundedPayment,
                dinero(dsti), dinero(payment.multiply(periods, MC).subtract(principal)),
                motivos.isEmpty(), motivos);
    }

    private void validar(SimulacionRequest r, BigDecimal tea) {
        if (r == null) throw new IllegalArgumentException("La simulación es obligatoria.");
        monto(r.valorInmueble(), true, "valor del inmueble");
        monto(r.cuotaInicial(), false, "cuota inicial");
        monto(r.ingresosMensuales(), true, "ingresos mensuales");
        monto(r.deudasMensuales(), false, "deudas mensuales");
        if (r.plazoMeses() == null || r.plazoMeses() < 1 || r.plazoMeses() > politica.plazoMaximoMeses())
            throw new IllegalArgumentException("Plazo fuera del rango permitido.");
        if (r.scoreCrediticio() != null && (r.scoreCrediticio() < 0 || r.scoreCrediticio() > 999))
            throw new IllegalArgumentException("El score debe estar entre 0 y 999.");
        if (tea == null || tea.signum() < 0 || tea.compareTo(HUNDRED) > 0 || tea.scale() > 2)
            throw new IllegalArgumentException("La TEA debe estar entre 0 y 100 y tener hasta dos decimales.");
        if (r.cuotaInicial().compareTo(r.valorInmueble()) >= 0)
            throw new IllegalArgumentException("La cuota inicial debe ser menor al valor del inmueble.");
        if (r.cuotaInicial().multiply(HUNDRED).compareTo(r.valorInmueble().multiply(politica.cuotaInicialMinimaPorcentaje())) < 0)
            throw new IllegalArgumentException("La cuota inicial mínima es " + politica.cuotaInicialMinimaPorcentaje() + "%.");
    }

    private static void monto(BigDecimal value, boolean positive, String label) {
        if (value == null || value.signum() < 0 || (positive && value.signum() == 0)
                || value.compareTo(LIMIT) > 0 || value.scale() > 2 || value.scale() < -9)
            throw new IllegalArgumentException("Importe inválido para " + label + ".");
    }

    private static BigDecimal dinero(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Newton acotado: el radicando está en [1,2], sin conversión a coma flotante. */
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
