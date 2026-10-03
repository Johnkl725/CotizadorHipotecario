package com.example.newcotizador.application.service;

import com.example.newcotizador.config.PoliticaProperties;
import com.example.newcotizador.dto.SimulacionRequest;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CalculoServiceTest {
    private final CalculoService service = new CalculoService(new PoliticaProperties(
            bd("9"), bd("10"), 700, bd("40"), 360));

    private static BigDecimal bd(String value) { return new BigDecimal(value); }
    private static SimulacionRequest request(String initial, String income, String debt, Integer score, int term) {
        return new SimulacionRequest(bd("300000"), bd(initial), term, bd(income), bd(debt), score);
    }

    @Test void zeroInterestDividesPrincipalAndIncludesDebtInDsti() {
        var result = service.simularConTea(request("60000", "5000", "1000", 700, 240), BigDecimal.ZERO);
        assertThat(result.cuotaMensual()).isEqualByComparingTo("1000.00");
        assertThat(result.totalIntereses()).isEqualByComparingTo("0.00");
        assertThat(result.ltvPorcentaje()).isEqualByComparingTo("80.00");
        assertThat(result.dstiPorcentaje()).isEqualByComparingTo("40.00");
        assertThat(result.elegiblePreferencial()).isTrue();
    }

    @Test void frenchPaymentAmortizesLoanAndMonthlyRateRecoversAnnualRate() {
        var result = service.simular(request("60000", "9000", "500", 750, 240));
        BigDecimal monthly = result.tem().divide(bd("100"));
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal annual = BigDecimal.ONE.add(monthly).pow(12, mc).subtract(BigDecimal.ONE).multiply(bd("100"));
        assertThat(annual.subtract(bd("9")).abs()).isLessThan(bd("0.000000001"));
        BigDecimal balance = result.montoPrestamo();
        for (int i = 0; i < 240; i++) balance = balance.multiply(BigDecimal.ONE.add(monthly), mc).subtract(result.cuotaMensual());
        // Two-decimal installments can leave a small final-installment adjustment.
        assertThat(balance.abs()).isLessThan(bd("4"));
        // Independently verified with Python decimal at precision 40.
        assertThat(result.cuotaMensual()).isEqualByComparingTo("2105.43");
    }

    @Test void eligibilityIsConservativeAtBoundariesAndUnknownScore() {
        assertThat(service.simularConTea(request("60000", "5000", "1000.01", 700, 240), BigDecimal.ZERO).elegiblePreferencial()).isFalse();
        assertThat(service.simularConTea(request("60000", "5000", "1000", 699, 240), BigDecimal.ZERO).elegiblePreferencial()).isFalse();
        assertThat(service.simular(request("60000", "9000", "0", null)).elegiblePreferencial()).isFalse();
    }

    private static SimulacionRequest request(String initial, String income, String debt, Integer score) {
        return request(initial, income, debt, score, 240);
    }

    @Test void rejectsInvalidInputsEvenOutsideControllerValidation() {
        for (String initial : new String[]{"-1", "29999.99", "300000", "300001"})
            assertThatIllegalArgumentException().isThrownBy(() -> service.simular(request(initial, "9000", "0", 750)));
        for (int term : new int[]{0, 361, Integer.MAX_VALUE})
            assertThatIllegalArgumentException().isThrownBy(() -> service.simular(request("60000", "9000", "0", 750, term)));
        assertThatIllegalArgumentException().isThrownBy(() -> service.simular(request("60000", "0", "0", 750)));
        assertThatIllegalArgumentException().isThrownBy(() -> service.simularConTea(request("60000", "9000", "0", 750), bd("-1")));
        assertThatIllegalArgumentException().isThrownBy(() -> service.simularConTea(request("60000", "9000", "0", 750), bd("100.01")));
    }

    @Test void sixtyConcurrentCalculationsAreDeterministic() throws Exception {
        var input = request("60000", "9000", "500", 750);
        var expected = service.simular(input);
        ExecutorService pool = Executors.newFixedThreadPool(60);
        CountDownLatch ready = new CountDownLatch(60);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var futures = new ArrayList<Future<?>>();
            for (int i = 0; i < 60; i++) futures.add(pool.submit(() -> {
                ready.countDown();
                try { if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Start timeout"); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new RuntimeException(ex); }
                for (int j = 0; j < 20; j++) assertThat(service.simular(input)).isEqualTo(expected);
            }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) future.get(20, TimeUnit.SECONDS);
        } finally { start.countDown(); pool.shutdownNow(); }
    }
}
