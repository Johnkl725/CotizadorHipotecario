package com.example.newcotizador.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CotizacionTest {
    private final PoliticaRiesgo politica = new PoliticaRiesgo(
            bd("9"), bd("10"), 700, bd("40"), 360);

    private static BigDecimal bd(String value) { return new BigDecimal(value); }

    private Cotizacion createCotizacion(String initial, String income, String debt, Integer score, int term) {
        Cotizacion c = new Cotizacion();
        c.setValorInmueble(bd("300000"));
        c.setCuotaInicial(bd(initial));
        c.setIngresosMensuales(bd(income));
        c.setDeudasMensuales(bd(debt));
        c.setScoreCrediticio(score);
        c.setPlazoMeses(term);
        return c;
    }

    @Test void zeroInterestDividesPrincipalAndIncludesDebtInDsti() {
        Cotizacion c = createCotizacion("60000", "5000", "1000", 700, 240);
        c.simularConTea(politica, BigDecimal.ZERO);
        assertThat(c.getCuotaMensualEstimada()).isEqualByComparingTo("1000.00");
        assertThat(c.getLtvPorcentaje()).isEqualByComparingTo("80.00");
        assertThat(c.getDstiPorcentaje()).isEqualByComparingTo("40.00");
    }

    @Test void frenchPaymentAmortizesLoanAndMonthlyRateRecoversAnnualRate() {
        Cotizacion c = createCotizacion("60000", "9000", "500", 750, 240);
        c.simular(politica);
        assertThat(c.getCuotaMensualEstimada()).isEqualByComparingTo("2105.43");
    }

    @Test void rejectsInvalidInputsEvenOutsideControllerValidation() {
        for (String initial : new String[]{"-1", "29999.99", "300000", "300001"}) {
            Cotizacion c = createCotizacion(initial, "9000", "0", 750, 240);
            assertThatIllegalArgumentException().isThrownBy(() -> c.simular(politica));
        }
        for (int term : new int[]{0, 361, Integer.MAX_VALUE}) {
            Cotizacion c = createCotizacion("60000", "9000", "0", 750, term);
            assertThatIllegalArgumentException().isThrownBy(() -> c.simular(politica));
        }
    }
}
